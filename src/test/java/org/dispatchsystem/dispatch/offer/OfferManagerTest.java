package org.dispatchsystem.dispatch.offer;

import org.dispatchsystem.common.events.DriverAssignedEvent;
import org.dispatchsystem.common.events.DriverOfferCreatedEvent;
import org.dispatchsystem.common.events.NoDriversAvailableEvent;
import org.dispatchsystem.common.exceptions.BusinessRuleViolationException;
import org.dispatchsystem.driver.domain.AvailabilityStatus;
import org.dispatchsystem.driver.domain.Driver;
import org.dispatchsystem.driver.domain.DriverBookingType;
import org.dispatchsystem.driver.repository.DriverRepository;
import org.dispatchsystem.ride.domain.BookingType;
import org.dispatchsystem.ride.domain.OfferStatus;
import org.dispatchsystem.ride.domain.Ride;
import org.dispatchsystem.ride.domain.RideOffer;
import org.dispatchsystem.ride.domain.RentalPlan;
import org.dispatchsystem.ride.domain.RideStatus;
import org.dispatchsystem.ride.repository.RideOfferRepository;
import org.dispatchsystem.ride.repository.RideRepository;
import org.dispatchsystem.ride.service.DispatchAuditService;
import org.dispatchsystem.ride.service.RideStateMachine;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OfferManagerTest {

    private RideOfferRepository offerRepository;
    private RideRepository rideRepository;
    private DriverRepository driverRepository;
    private ApplicationEventPublisher applicationEventPublisher;
    private RideStateMachine rideStateMachine;
    private DispatchAuditService dispatchAuditService;
    private OfferManager offerManager;
    private long nextDriverId;

    @BeforeEach
    void setUp() {
        nextDriverId = 1L;
        offerRepository = mock(RideOfferRepository.class);
        rideRepository = mock(RideRepository.class);
        driverRepository = mock(DriverRepository.class);
        applicationEventPublisher = mock(ApplicationEventPublisher.class);
        rideStateMachine = new RideStateMachine(applicationEventPublisher);
        dispatchAuditService = mock(DispatchAuditService.class);
        offerManager = new OfferManager(
                offerRepository,
                rideRepository,
                driverRepository,
                applicationEventPublisher,
                rideStateMachine,
                dispatchAuditService
        );

        when(offerRepository.save(any(RideOffer.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(rideRepository.save(any(Ride.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(driverRepository.save(any(Driver.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(driverRepository.updateDriver(anyLong(), any(AvailabilityStatus.class), any(AvailabilityStatus.class))).thenReturn(1);
    }

    @AfterEach
    void tearDown() {
        ScheduledExecutorService scheduler =
                (ScheduledExecutorService) ReflectionTestUtils.getField(offerManager, "scheduler");
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    @Test
    void handleDriverResponseReturnsNoActiveOfferForUnknownDriver() {
        Ride ride = createRide(101L, RideStatus.DISPATCHING);
        Driver offeredDriver = createDriver("offered@dispatchx.dev");
        Driver otherDriver = createDriver("other@dispatchx.dev");

        offerManager.startOfferFlow(ride, List.of(offeredDriver, otherDriver));

        when(offerRepository.findTopByDriver_EmailAndRide_IdOrderBySentAtDesc(otherDriver.getEmail(), ride.getId()))
                .thenReturn(Optional.empty());

        DriverResponded response = offerManager.handleDriverResponse(ride.getId(), otherDriver.getEmail(), "ACCEPT");

        assertEquals(OfferStatusState.NO_ACTIVE_OFFER, response.getOfferStatus());
        assertEquals("No active offer for this ride", response.getMessage());
    }

    @Test
    void handleDriverResponseRejectMovesOfferToNextDriver() {
        Ride ride = createRide(102L, RideStatus.DISPATCHING);
        Driver firstDriver = createDriver("first@dispatchx.dev");
        Driver secondDriver = createDriver("second@dispatchx.dev");

        offerManager.startOfferFlow(ride, List.of(firstDriver, secondDriver));
        DriverResponded response = offerManager.handleDriverResponse(ride.getId(), firstDriver.getEmail(), "REJECT");

        ArgumentCaptor<RideOffer> offerCaptor = ArgumentCaptor.forClass(RideOffer.class);
        verify(offerRepository, times(3)).save(offerCaptor.capture());
        List<RideOffer> savedOffers = offerCaptor.getAllValues();

        assertEquals(OfferStatusState.REJECTED, response.getOfferStatus());
        assertEquals("Ride is Rejected by the driver", response.getMessage());
        assertEquals(OfferStatus.REJECTED, savedOffers.get(1).getStatus());
        assertNotNull(savedOffers.get(1).getRespondedAt());
        assertEquals(AvailabilityStatus.AVAILABLE, firstDriver.getAvailabilityStatus());
        assertEquals(secondDriver.getEmail(), savedOffers.get(2).getDriver().getEmail());
        assertEquals(OfferStatus.PENDING, savedOffers.get(2).getStatus());
        verify(driverRepository).save(firstDriver);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(applicationEventPublisher, times(2)).publishEvent(eventCaptor.capture());
        List<Object> publishedEvents = eventCaptor.getAllValues();
        assertInstanceOf(DriverOfferCreatedEvent.class, publishedEvents.get(0));
        assertInstanceOf(DriverOfferCreatedEvent.class, publishedEvents.get(1));
    }

    @Test
    void handleOfferTimeoutExpiresCurrentOfferAndMovesToNextDriver() {
        Ride ride = createRide(103L, RideStatus.DISPATCHING);
        Driver firstDriver = createDriver("first-timeout@dispatchx.dev");
        Driver secondDriver = createDriver("second-timeout@dispatchx.dev");

        offerManager.startOfferFlow(ride, List.of(firstDriver, secondDriver));

        @SuppressWarnings("unchecked")
        Map<Long, OfferFlowState> activeFlows =
                (ConcurrentHashMap<Long, OfferFlowState>) ReflectionTestUtils.getField(offerManager, "activeFlows");
        assertNotNull(activeFlows);

        OfferFlowState state = activeFlows.get(ride.getId());
        assertNotNull(state);

        ReflectionTestUtils.invokeMethod(offerManager, "handleOfferTimeout", state);

        ArgumentCaptor<RideOffer> offerCaptor = ArgumentCaptor.forClass(RideOffer.class);
        verify(offerRepository, times(3)).save(offerCaptor.capture());
        List<RideOffer> savedOffers = offerCaptor.getAllValues();

        assertEquals(OfferStatus.EXPIRED, savedOffers.get(1).getStatus());
        assertNotNull(savedOffers.get(1).getRespondedAt());
        assertEquals(AvailabilityStatus.AVAILABLE, firstDriver.getAvailabilityStatus());
        assertEquals(secondDriver.getEmail(), savedOffers.get(2).getDriver().getEmail());
        assertEquals(OfferStatus.PENDING, savedOffers.get(2).getStatus());
        verify(driverRepository).save(firstDriver);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(applicationEventPublisher, times(2)).publishEvent(eventCaptor.capture());
        List<Object> publishedEvents = eventCaptor.getAllValues();
        assertInstanceOf(DriverOfferCreatedEvent.class, publishedEvents.get(0));
        assertInstanceOf(DriverOfferCreatedEvent.class, publishedEvents.get(1));
    }

    @Test
    void handleDriverResponseAcceptAssignsRideAndPublishesEvent() {
        Ride ride = createRide(104L, RideStatus.DISPATCHING);
        Driver driver = createDriver("accept@dispatchx.dev");

        offerManager.startOfferFlow(ride, List.of(driver));
        DriverResponded response = offerManager.handleDriverResponse(ride.getId(), driver.getEmail(), "ACCEPT");

        assertEquals(OfferStatusState.SUCCESS, response.getOfferStatus());
        assertEquals("Ride is Accepted by the driver", response.getMessage());
        assertSame(driver, ride.getDriver());
        assertEquals(RideStatus.DRIVER_ASSIGNED, ride.getStatus());
        assertEquals(AvailabilityStatus.UNAVAILABLE, driver.getAvailabilityStatus());

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(applicationEventPublisher, times(3)).publishEvent(eventCaptor.capture());
        List<Object> publishedEvents = eventCaptor.getAllValues();
        assertInstanceOf(DriverOfferCreatedEvent.class, publishedEvents.get(0));
        assertInstanceOf(DriverAssignedEvent.class, publishedEvents.get(2));
    }

    @Test
    void noMoreDriversCancelsRideAndPublishesEvents() {
        Ride ride = createRide(105L, RideStatus.DISPATCHING);

        offerManager.startOfferFlow(ride, List.of());

        assertEquals(RideStatus.CANCELLED, ride.getStatus());

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(applicationEventPublisher, times(2)).publishEvent(eventCaptor.capture());
        List<Object> publishedEvents = eventCaptor.getAllValues();
        assertInstanceOf(NoDriversAvailableEvent.class, publishedEvents.get(1));
    }

    @Test
    void cancelRideFlowStopsTimeoutFromProgressingToNextDriver() {
        Ride ride = createRide(106L, RideStatus.DISPATCHING);
        Driver firstDriver = createDriver("cancel-first@dispatchx.dev");
        Driver secondDriver = createDriver("cancel-second@dispatchx.dev");

        offerManager.startOfferFlow(ride, List.of(firstDriver, secondDriver));

        @SuppressWarnings("unchecked")
        Map<Long, OfferFlowState> activeFlows =
                (ConcurrentHashMap<Long, OfferFlowState>) ReflectionTestUtils.getField(offerManager, "activeFlows");
        assertNotNull(activeFlows);

        OfferFlowState state = activeFlows.get(ride.getId());
        assertNotNull(state);

        offerManager.cancelRideFlow(ride.getId());
        ReflectionTestUtils.invokeMethod(offerManager, "handleOfferTimeout", state);

        assertFalse(activeFlows.containsKey(ride.getId()));
        assertEquals(OfferStatus.CANCELLED, state.getCurrentOffer().getStatus());
        assertNotNull(state.getCurrentOffer().getRespondedAt());
        verify(offerRepository, times(2)).save(any(RideOffer.class));
    }

    @Test
    void cancelRideFlowWithoutActiveFlowIsNoOp() {
        offerManager.cancelRideFlow(999L);

        verify(offerRepository, never()).save(any(RideOffer.class));
    }

    @Test
    void handleDriverResponseReturnsRideCancelledWhenOfferWasCancelled() {
        Ride ride = createRide(108L, RideStatus.CANCELLED);
        Driver driver = createDriver("cancelled@dispatchx.dev");
        RideOffer cancelledOffer = createStoredOffer(ride, driver, OfferStatus.CANCELLED);

        when(offerRepository.findTopByDriver_EmailAndRide_IdOrderBySentAtDesc(driver.getEmail(), ride.getId()))
                .thenReturn(Optional.of(cancelledOffer));

        DriverResponded response = offerManager.handleDriverResponse(ride.getId(), driver.getEmail(), "ACCEPT");

        assertEquals(OfferStatusState.RIDE_CANCELLED, response.getOfferStatus());
        assertEquals("Ride was cancelled", response.getMessage());
    }

    @Test
    void handleDriverResponseReturnsOfferExpiredWhenOfferAlreadyExpired() {
        Ride ride = createRide(109L, RideStatus.DISPATCHING);
        Driver driver = createDriver("expired@dispatchx.dev");
        RideOffer expiredOffer = createStoredOffer(ride, driver, OfferStatus.EXPIRED);

        when(offerRepository.findTopByDriver_EmailAndRide_IdOrderBySentAtDesc(driver.getEmail(), ride.getId()))
                .thenReturn(Optional.of(expiredOffer));

        DriverResponded response = offerManager.handleDriverResponse(ride.getId(), driver.getEmail(), "ACCEPT");

        assertEquals(OfferStatusState.OFFER_EXPIRED, response.getOfferStatus());
        assertEquals("Offer already expired", response.getMessage());
    }

    @Test
    void handleDriverResponseRejectsUnsupportedResponses() {
        Ride ride = createRide(110L, RideStatus.DISPATCHING);
        Driver driver = createDriver("unsupported@dispatchx.dev");

        offerManager.startOfferFlow(ride, List.of(driver));

        BusinessRuleViolationException exception = assertThrows(
                BusinessRuleViolationException.class,
                () -> offerManager.handleDriverResponse(ride.getId(), driver.getEmail(), "MAYBE")
        );

        assertEquals("Unsupported driver response: MAYBE", exception.getMessage());
    }

    @Test
    void reservationFailureSkipsFirstDriverAndOffersSecondDriver() {
        Ride ride = createRide(107L, RideStatus.DISPATCHING);
        Driver firstDriver = createDriver("reserve-fail-first@dispatchx.dev");
        Driver secondDriver = createDriver("reserve-fail-second@dispatchx.dev");

        when(driverRepository.updateDriver(firstDriver.getId(), AvailabilityStatus.AVAILABLE, AvailabilityStatus.RESERVED))
                .thenReturn(0);
        when(driverRepository.updateDriver(secondDriver.getId(), AvailabilityStatus.AVAILABLE, AvailabilityStatus.RESERVED))
                .thenReturn(1);

        offerManager.startOfferFlow(ride, List.of(firstDriver, secondDriver));

        ArgumentCaptor<RideOffer> offerCaptor = ArgumentCaptor.forClass(RideOffer.class);
        verify(offerRepository, times(3)).save(offerCaptor.capture());
        List<RideOffer> savedOffers = offerCaptor.getAllValues();

        assertEquals(firstDriver.getEmail(), savedOffers.get(0).getDriver().getEmail());
        assertEquals(OfferStatus.EXPIRED, savedOffers.get(1).getStatus());
        assertNotNull(savedOffers.get(1).getRespondedAt());
        assertEquals(secondDriver.getEmail(), savedOffers.get(2).getDriver().getEmail());
        assertEquals(OfferStatus.PENDING, savedOffers.get(2).getStatus());
        verify(driverRepository, never()).save(firstDriver);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(applicationEventPublisher, times(1)).publishEvent(eventCaptor.capture());
        assertInstanceOf(DriverOfferCreatedEvent.class, eventCaptor.getValue());
    }

    private Ride createRide(Long rideId, RideStatus status) {
        Ride ride = new Ride();
        ride.setId(rideId);
        ride.setStatus(status);
        ride.setBookingType(BookingType.TRIP);
        ride.setRentalPlan(RentalPlan.NONE);
        return ride;
    }

    private Driver createDriver(String email) {
        Driver driver = new Driver();
        driver.setId(nextDriverId++);
        driver.setEmail(email);
        driver.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
        driver.setSupportedBookingTypes(new java.util.HashSet<>(Set.of(DriverBookingType.TRIP)));
        return driver;
    }

    private RideOffer createStoredOffer(Ride ride, Driver driver, OfferStatus status) {
        RideOffer offer = new RideOffer();
        offer.setRide(ride);
        offer.setDriver(driver);
        offer.setStatus(status);
        offer.setSentAt(LocalDateTime.now().minusSeconds(30));
        offer.setRespondedAt(LocalDateTime.now());
        return offer;
    }
}

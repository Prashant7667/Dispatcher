package org.dispatchsystem.dispatch.offer;

import org.dispatchsystem.common.events.DriverAssignedEvent;
import org.dispatchsystem.common.events.NoDriversAvailableEvent;
import org.dispatchsystem.common.events.RideCancelledEvent;
import org.dispatchsystem.common.exceptions.BusinessRuleViolationException;
import org.dispatchsystem.driver.domain.AvailabilityStatus;
import org.dispatchsystem.driver.domain.Driver;
import org.dispatchsystem.driver.repository.DriverRepository;
import org.dispatchsystem.ride.domain.OfferStatus;
import org.dispatchsystem.ride.domain.Ride;
import org.dispatchsystem.ride.domain.RideOffer;
import org.dispatchsystem.ride.domain.RideStatus;
import org.dispatchsystem.ride.repository.RideOfferRepository;
import org.dispatchsystem.ride.repository.RideRepository;
import org.dispatchsystem.ride.service.RideBroadcastService;
import org.dispatchsystem.ride.service.RideStateMachine;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OfferManagerTest {

    private RideOfferRepository offerRepository;
    private RideBroadcastService broadcastService;
    private RideRepository rideRepository;
    private DriverRepository driverRepository;
    private ApplicationEventPublisher applicationEventPublisher;
    private RideStateMachine rideStateMachine;
    private OfferManager offerManager;

    @BeforeEach
    void setUp() {
        offerRepository = mock(RideOfferRepository.class);
        broadcastService = mock(RideBroadcastService.class);
        rideRepository = mock(RideRepository.class);
        driverRepository = mock(DriverRepository.class);
        applicationEventPublisher = mock(ApplicationEventPublisher.class);
        rideStateMachine = new RideStateMachine(applicationEventPublisher);
        offerManager = new OfferManager(
                offerRepository,
                broadcastService,
                rideRepository,
                driverRepository,
                applicationEventPublisher,
                rideStateMachine
        );

        when(offerRepository.save(any(RideOffer.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(rideRepository.save(any(Ride.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(driverRepository.save(any(Driver.class))).thenAnswer(invocation -> invocation.getArgument(0));
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
    void handleDriverResponseRejectsUnauthorizedDriver() {
        Ride ride = createRide(101L, RideStatus.DISPATCHING);
        Driver offeredDriver = createDriver("offered@dispatchx.dev");
        Driver otherDriver = createDriver("other@dispatchx.dev");

        offerManager.startOfferFlow(ride, List.of(offeredDriver, otherDriver));

        BusinessRuleViolationException exception = assertThrows(
                BusinessRuleViolationException.class,
                () -> offerManager.handleDriverResponse(ride.getId(), otherDriver.getEmail(), "ACCEPT")
        );

        assertEquals("This driver is not authorized to respond to the current offer", exception.getMessage());
    }

    @Test
    void handleDriverResponseRejectMovesOfferToNextDriver() {
        Ride ride = createRide(102L, RideStatus.DISPATCHING);
        Driver firstDriver = createDriver("first@dispatchx.dev");
        Driver secondDriver = createDriver("second@dispatchx.dev");

        offerManager.startOfferFlow(ride, List.of(firstDriver, secondDriver));
        offerManager.handleDriverResponse(ride.getId(), firstDriver.getEmail(), "REJECT");

        ArgumentCaptor<RideOffer> offerCaptor = ArgumentCaptor.forClass(RideOffer.class);
        verify(offerRepository, times(3)).save(offerCaptor.capture());
        List<RideOffer> savedOffers = offerCaptor.getAllValues();

        assertEquals(OfferStatus.REJECTED, savedOffers.get(1).getStatus());
        assertEquals(secondDriver.getEmail(), savedOffers.get(2).getDriver().getEmail());
        assertEquals(OfferStatus.PENDING, savedOffers.get(2).getStatus());
        verify(broadcastService).sendOfferToDriver(firstDriver, ride);
        verify(broadcastService).sendOfferToDriver(secondDriver, ride);
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
        assertEquals(secondDriver.getEmail(), savedOffers.get(2).getDriver().getEmail());
        assertEquals(OfferStatus.PENDING, savedOffers.get(2).getStatus());
        verify(broadcastService).sendOfferToDriver(secondDriver, ride);
    }

    @Test
    void handleDriverResponseAcceptAssignsRideAndPublishesEvent() {
        Ride ride = createRide(104L, RideStatus.DISPATCHING);
        Driver driver = createDriver("accept@dispatchx.dev");

        offerManager.startOfferFlow(ride, List.of(driver));
        offerManager.handleDriverResponse(ride.getId(), driver.getEmail(), "ACCEPT");

        assertSame(driver, ride.getDriver());
        assertEquals(RideStatus.DRIVER_ASSIGNED, ride.getStatus());
        assertEquals(AvailabilityStatus.UNAVAILABLE, driver.getAvailabilityStatus());

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(applicationEventPublisher, times(2)).publishEvent(eventCaptor.capture());
        List<Object> publishedEvents = eventCaptor.getAllValues();
        assertInstanceOf(DriverAssignedEvent.class, publishedEvents.get(1));
    }

    @Test
    void noMoreDriversCancelsRideAndPublishesEvents() {
        Ride ride = createRide(105L, RideStatus.DISPATCHING);

        offerManager.startOfferFlow(ride, List.of());

        assertEquals(RideStatus.CANCELLED, ride.getStatus());

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(applicationEventPublisher, times(3)).publishEvent(eventCaptor.capture());
        List<Object> publishedEvents = eventCaptor.getAllValues();
        assertInstanceOf(NoDriversAvailableEvent.class, publishedEvents.get(1));
        assertInstanceOf(RideCancelledEvent.class, publishedEvents.get(2));
    }

    private Ride createRide(Long rideId, RideStatus status) {
        Ride ride = new Ride();
        ride.setId(rideId);
        ride.setStatus(status);
        return ride;
    }

    private Driver createDriver(String email) {
        Driver driver = new Driver();
        driver.setEmail(email);
        driver.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
        return driver;
    }
}

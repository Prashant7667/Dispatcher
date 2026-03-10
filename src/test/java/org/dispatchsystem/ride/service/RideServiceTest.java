package org.dispatchsystem.ride.service;

import org.dispatchsystem.common.events.RideStatusChangedEvent;
import org.dispatchsystem.common.exceptions.BusinessRuleViolationException;
import org.dispatchsystem.driver.domain.AvailabilityStatus;
import org.dispatchsystem.driver.domain.Driver;
import org.dispatchsystem.driver.repository.DriverRepository;
import org.dispatchsystem.driver.service.DriverService;
import org.dispatchsystem.dispatch.orchestrator.DispatchOrchestrator;
import org.dispatchsystem.ride.domain.Ride;
import org.dispatchsystem.ride.domain.RideStatus;
import org.dispatchsystem.ride.repository.RideRepository;
import org.dispatchsystem.user.repository.UserRepository;
import org.dispatchsystem.user.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RideServiceTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void assignedDriverCanProgressRideThroughFullLifecycle() {
        RideRepository rideRepository = mock(RideRepository.class);
        DriverRepository driverRepository = mock(DriverRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        DriverService driverService = mock(DriverService.class);
        UserService userService = mock(UserService.class);
        DispatchOrchestrator dispatchOrchestrator = mock(DispatchOrchestrator.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

        RideStateMachine rideStateMachine = new RideStateMachine(eventPublisher);
        RideService rideService = new RideService(
                rideRepository,
                driverRepository,
                userRepository,
                driverService,
                userService,
                dispatchOrchestrator,
                eventPublisher,
                rideStateMachine
        );

        Driver driver = new Driver();
        driver.setEmail("driver@dispatchx.dev");
        driver.setAvailabilityStatus(AvailabilityStatus.UNAVAILABLE);

        Ride ride = new Ride();
        ride.setId(200L);
        ride.setStatus(RideStatus.DRIVER_ASSIGNED);
        ride.setDriver(driver);

        when(rideRepository.findById(ride.getId())).thenReturn(Optional.of(ride));
        when(rideRepository.save(any(Ride.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(driverRepository.save(any(Driver.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(driver.getEmail(), null)
        );

        rideService.markDriverEnRoute(ride.getId());
        assertEquals(RideStatus.DRIVER_EN_ROUTE, ride.getStatus());

        rideService.markDriverArrived(ride.getId());
        assertEquals(RideStatus.DRIVER_ARRIVED, ride.getStatus());

        rideService.startRide(ride.getId());
        assertEquals(RideStatus.IN_PROGRESS, ride.getStatus());

        rideService.completeRide(ride.getId());
        assertEquals(RideStatus.COMPLETED, ride.getStatus());
        assertEquals(AvailabilityStatus.AVAILABLE, driver.getAvailabilityStatus());

        verify(driverRepository).save(driver);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, times(4)).publishEvent(eventCaptor.capture());
        List<Object> publishedEvents = eventCaptor.getAllValues();
        assertInstanceOf(RideStatusChangedEvent.class, publishedEvents.get(0));
        assertInstanceOf(RideStatusChangedEvent.class, publishedEvents.get(1));
        assertInstanceOf(RideStatusChangedEvent.class, publishedEvents.get(2));
        assertInstanceOf(RideStatusChangedEvent.class, publishedEvents.get(3));
    }

    @Test
    void nonAssignedDriverCannotUpdateRideLifecycle() {
        RideRepository rideRepository = mock(RideRepository.class);
        DriverRepository driverRepository = mock(DriverRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        DriverService driverService = mock(DriverService.class);
        UserService userService = mock(UserService.class);
        DispatchOrchestrator dispatchOrchestrator = mock(DispatchOrchestrator.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

        RideService rideService = new RideService(
                rideRepository,
                driverRepository,
                userRepository,
                driverService,
                userService,
                dispatchOrchestrator,
                eventPublisher,
                new RideStateMachine(eventPublisher)
        );

        Driver assignedDriver = new Driver();
        assignedDriver.setEmail("assigned@dispatchx.dev");

        Ride ride = new Ride();
        ride.setId(201L);
        ride.setStatus(RideStatus.DRIVER_ASSIGNED);
        ride.setDriver(assignedDriver);

        when(rideRepository.findById(ride.getId())).thenReturn(Optional.of(ride));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("other@dispatchx.dev", null)
        );

        BusinessRuleViolationException exception = assertThrows(
                BusinessRuleViolationException.class,
                () -> rideService.markDriverEnRoute(ride.getId())
        );

        assertEquals("You are not authorised to process this ride", exception.getMessage());
    }
}

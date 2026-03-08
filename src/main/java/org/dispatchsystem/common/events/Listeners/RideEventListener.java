package org.dispatchsystem.common.events.Listeners;

import lombok.extern.slf4j.Slf4j;
import org.dispatchsystem.common.events.DriverAssignedEvent;
import org.dispatchsystem.common.events.NoDriversAvailableEvent;
import org.dispatchsystem.common.events.RideRequestedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RideEventListener {
    @EventListener
    public void onDriverAssigned(DriverAssignedEvent event) {
        log.info("✅ Driver {} assigned to ride {}",
                event.getDriver().getName(), event.getRide().getId());
        // Future: send push notification to rider
        // Future: send notification to driver
        // Future: log to analytics
    }
    @EventListener
    public void onRideRequested(RideRequestedEvent event) {
        log.info("🆕 New ride requested: {}", event.getRide().getId());
    }
    @EventListener
    public void onNoDrivers(NoDriversAvailableEvent event) {
        log.info("❌ No drivers available for ride {}", event.getRide().getId());
        // Future: notify rider, suggest retry
    }
}

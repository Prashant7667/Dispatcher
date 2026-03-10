package org.dispatchsystem.common.config;

import org.dispatchsystem.ride.service.RideSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final RideSocketHandler rideSocketHandler;
    private final WebSocketAuthInterceptor authInterceptor;

    public WebSocketConfig(RideSocketHandler rideSocketHandler,
                           WebSocketAuthInterceptor authInterceptor) {
        this.rideSocketHandler = rideSocketHandler;
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(rideSocketHandler, "/ws/rides")
                .addInterceptors(authInterceptor)
                .setAllowedOrigins("*");
    }
}

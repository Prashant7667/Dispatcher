package org.dispatchsystem.common.config;

import org.dispatchsystem.driver.controller.DriverSocketHandler;
import org.dispatchsystem.user.controller.UserSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final DriverSocketHandler driverSocketHandler;
    private final UserSocketHandler userSocketHandler;
    private final WebSocketAuthInterceptor authInterceptor;

    public WebSocketConfig(DriverSocketHandler driverSocketHandler, UserSocketHandler userSocketHandler,
                           WebSocketAuthInterceptor authInterceptor) {
        this.driverSocketHandler = driverSocketHandler;
        this.userSocketHandler = userSocketHandler;
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(driverSocketHandler,"/ws/drivers")
                .addHandler(userSocketHandler,"/ws/users")
                .addInterceptors(authInterceptor)
                .setAllowedOrigins("*");
    }
}

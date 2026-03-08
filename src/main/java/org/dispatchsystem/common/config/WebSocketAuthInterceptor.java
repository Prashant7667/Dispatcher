package org.dispatchsystem.common.config;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    private final JwtUtils jwtUtils;

    public WebSocketAuthInterceptor(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {

        String query=request.getURI().getQuery();
        if(query==null || ! query.startsWith("token=")){
            return false;
        }
        String token = query.substring(6);

        try {
            String email = jwtUtils.validateTokenAndGetEmail(token);
            String role = jwtUtils.getRoleFromToken(token);

            attributes.put("email", email);
            attributes.put("role", role);

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {}
}

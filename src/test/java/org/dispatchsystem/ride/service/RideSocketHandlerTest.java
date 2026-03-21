package org.dispatchsystem.ride.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dispatchsystem.dispatch.offer.DriverOfferResponse;
import org.dispatchsystem.dispatch.offer.DriverResponded;
import org.dispatchsystem.dispatch.offer.OfferManager;
import org.dispatchsystem.dispatch.offer.OfferStatusState;
import org.dispatchsystem.driver.service.DriverSessionRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RideSocketHandlerTest {

    private DriverSessionRegistry registry;
    private OfferManager offerManager;
    private ObjectMapper objectMapper;
    private RideSocketHandler rideSocketHandler;
    private WebSocketSession session;

    @BeforeEach
    void setUp() {
        registry = mock(DriverSessionRegistry.class);
        offerManager = mock(OfferManager.class);
        objectMapper = new ObjectMapper();
        rideSocketHandler = new RideSocketHandler(registry, offerManager, objectMapper);
        session = mock(WebSocketSession.class);

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("email", "driver@dispatchx.dev");
        attributes.put("role", "DRIVER");

        when(session.getAttributes()).thenReturn(attributes);
    }

    @Test
    void handleTextMessageSendsOfferManagerResponseBackToDriver() throws Exception {
        DriverResponded expectedResponse = new DriverResponded(
                OfferStatusState.SUCCESS,
                42L,
                "Ride accepted by driver"
        );
        when(offerManager.handleDriverResponse(42L, "driver@dispatchx.dev", "ACCEPT"))
                .thenReturn(expectedResponse);

        rideSocketHandler.handleTextMessage(session, new TextMessage(
                """
                {"rideId":42,"message":"ACCEPT"}
                """
        ));

        ArgumentCaptor<TextMessage> messageCaptor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session).sendMessage(messageCaptor.capture());
        assertEquals(objectMapper.writeValueAsString(expectedResponse), messageCaptor.getValue().getPayload());
    }

    @Test
    void handleTextMessageSendsErrorPayloadWhenRequestCannotBeProcessed() throws Exception {
        when(offerManager.handleDriverResponse(42L, "driver@dispatchx.dev", "ACCEPT"))
                .thenThrow(new RuntimeException("boom"));

        rideSocketHandler.handleTextMessage(session, new TextMessage(
                """
                {"rideId":42,"message":"ACCEPT"}
                """
        ));

        DriverResponded errorPayload = new DriverResponded(
                OfferStatusState.ERROR,
                null,
                "Unable to process driver response"
        );
        ArgumentCaptor<TextMessage> messageCaptor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session).sendMessage(messageCaptor.capture());
        assertEquals(objectMapper.writeValueAsString(errorPayload), messageCaptor.getValue().getPayload());
    }
}

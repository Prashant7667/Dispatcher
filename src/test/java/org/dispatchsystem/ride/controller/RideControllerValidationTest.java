package org.dispatchsystem.ride.controller;

import org.dispatchsystem.common.exceptions.GlobalExceptionHandler;
import org.dispatchsystem.ride.service.RideService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.mock;

class RideControllerValidationTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        RideService rideService = mock(RideService.class);
        RideController controller = new RideController(rideService);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void requestRideRejectsMissingRequestedVehicleClass() throws Exception {
        String payload = """
                {
                  "startLongitude": 77.5946,
                  "startLatitude": 12.9716,
                  "endLongitude": 77.6200,
                  "endLatitude": 12.9900,
                  "bookingType": "TRIP",
                  "fare": 250.0,
                  "requiredLuggageCapacity": 15
                }
                """;

        mockMvc.perform(post("/rides/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("requestedVehicleClass: requestedVehicleClass is required")));
    }

    @Test
    void requestRideRejectsNegativeLuggageCapacity() throws Exception {
        String payload = """
                {
                  "startLongitude": 77.5946,
                  "startLatitude": 12.9716,
                  "endLongitude": 77.6200,
                  "endLatitude": 12.9900,
                  "bookingType": "TRIP",
                  "fare": 250.0,
                  "requestedVehicleClass": "SEDAN",
                  "requiredLuggageCapacity": -1
                }
                """;

        mockMvc.perform(post("/rides/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("requiredLuggageCapacity: requiredLuggageCapacity must be zero or positive")));
    }

    @Test
    void updateRideRejectsMissingRequestedVehicleClass() throws Exception {
        String payload = """
                {
                  "startLongitude": 77.5946,
                  "startLatitude": 12.9716,
                  "endLongitude": 77.6200,
                  "endLatitude": 12.9900,
                  "bookingType": "TRIP",
                  "fare": 250.0,
                  "requiredLuggageCapacity": 10
                }
                """;

        mockMvc.perform(put("/rides/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("requestedVehicleClass: requestedVehicleClass is required")));
    }
}

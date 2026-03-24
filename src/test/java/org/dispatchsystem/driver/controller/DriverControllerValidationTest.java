package org.dispatchsystem.driver.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dispatchsystem.common.exceptions.GlobalExceptionHandler;
import org.dispatchsystem.driver.service.DriverService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DriverControllerValidationTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        DriverService driverService = mock(DriverService.class);
        DriverController controller = new DriverController(driverService);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void createDriverRejectsMissingVehicleDetails() throws Exception {
        String payload = """
                {
                  "name": "Driver One",
                  "email": "driver1@dispatch.dev",
                  "password": "secret",
                  "phoneNumber": "9999999999",
                  "latitude": 12.9716,
                  "longitude": 77.5946
                }
                """;

        mockMvc.perform(post("/drivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("vehicleDetails: vehicleDetails is required")));
    }

    @Test
    void createDriverRejectsMissingNestedVehicleDetailsFields() throws Exception {
        String payload = """
                {
                  "name": "Driver Two",
                  "email": "driver2@dispatch.dev",
                  "password": "secret",
                  "phoneNumber": "9999999998",
                  "latitude": 12.9716,
                  "longitude": 77.5946,
                  "vehicleDetails": {
                    "seatCapacity": 4,
                    "luggageCapacityKg": 20,
                    "city": "Bengaluru",
                    "zone": "South",
                    "vehicleMake": "Hyundai",
                    "vehicleModel": "i20",
                    "vehicleColor": "White",
                    "licensePlate": "KA01AB1234",
                    "vehicleYear": 2024
                  }
                }
                """;

        mockMvc.perform(post("/drivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("vehicleDetails.vehicleClass: vehicleClass is required")));
    }
}

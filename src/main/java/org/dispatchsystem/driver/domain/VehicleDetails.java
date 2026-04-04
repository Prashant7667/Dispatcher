package org.dispatchsystem.driver.domain;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class VehicleDetails {
    @Enumerated(EnumType.STRING)
    @NotNull(message = "vehicleClass is required")
    private VehicleClass vehicleClass;
    @NotNull(message = "seatCapacity is required")
    @Positive(message = "seatCapacity must be positive")
    private Integer seatCapacity;
    @NotNull(message = "luggageCapacityKg is required")
    @PositiveOrZero(message = "luggageCapacityKg must be zero or positive")
    private Integer luggageCapacityKg;
    @NotBlank(message = "city is required")
    private String city;
    @NotBlank(message = "zone is required")
    private String zone;
    @NotBlank(message = "vehicleMake is required")
    private String vehicleMake;
    @NotBlank(message = "vehicleModel is required")
    private String vehicleModel;
    @NotBlank(message = "vehicleColor is required")
    private String vehicleColor;
    @NotBlank(message = "licensePlate is required")
    private String licensePlate;
    @NotNull(message = "vehicleYear is required")
    private Integer vehicleYear;
}

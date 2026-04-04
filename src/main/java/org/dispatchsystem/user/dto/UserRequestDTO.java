package org.dispatchsystem.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.dispatchsystem.user.domain.UserRole;

@Data
public class UserRequestDTO {
    @NotBlank(message = "name is required")
    private String name;
    @NotBlank(message = "email is required")
    private String email;
    @NotBlank(message = "password is required")
    private String password;
    @NotBlank(message = "phoneNumber is required")
    private String phoneNumber;
    private UserRole role;
}

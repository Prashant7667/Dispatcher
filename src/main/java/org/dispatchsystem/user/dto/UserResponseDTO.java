package org.dispatchsystem.user.dto;

import lombok.Data;
import org.dispatchsystem.user.domain.UserRole;

@Data
public class UserResponseDTO {
    private Long id;
    private String name;
    private String email;
    private String phoneNumber;
    private UserRole role;
}

package org.dispatchsystem.user.dto;

import lombok.Data;

@Data
public class UserUpdateDTO {
    private String name;
    private String password;
    private String phoneNumber;
}

package com.financemanagerai.user_service.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUserRequest {
    private String firstName;
    private String lastName;

    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;
}

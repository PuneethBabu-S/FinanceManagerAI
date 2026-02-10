package com.financemanagerai.user_service.dto;

import com.financemanagerai.user_service.model.Role;
import lombok.Builder;
import lombok.Data;

import java.util.Set;
import java.util.UUID;

@Data
@Builder
public class UserResponse {
    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
    private Set<Role> roles;
}

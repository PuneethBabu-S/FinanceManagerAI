package com.financemanagerai.user_service.controller;

import com.financemanagerai.user_service.dto.UserLoginDTO;
import com.financemanagerai.user_service.dto.UserRequestDTO;
import com.financemanagerai.user_service.dto.UserResponseDTO;
import com.financemanagerai.user_service.entity.User;
import com.financemanagerai.user_service.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponseDTO> register(@Valid @RequestBody UserRequestDTO userDto) {
        User savedUser = userService.registerUser(userDto.toEntity());
        return ResponseEntity.ok(UserResponseDTO.from(savedUser));
    }

    @GetMapping("/{username}")
    public ResponseEntity<UserResponseDTO> getUserByUsername(@PathVariable String username) {
        return userService.findByUsername(username)
                .map(UserResponseDTO::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<UserResponseDTO>> getAllUsers() {
        List<UserResponseDTO> users = userService.getAllUsers()
                .stream()
                .map(UserResponseDTO::from)
                .toList();
        return ResponseEntity.ok(users);
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@Valid @RequestBody UserLoginDTO loginDto) {
        String token = userService.login(loginDto.getUsername(), loginDto.getPassword());
        Map<String, String> response = new HashMap<>();
        response.put("token", token);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> getCurrentUser(Authentication authentication) {
        String username = authentication.getName();
        return userService.findByUsername(username)
                .map(UserResponseDTO::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/promote/{username}")
    public ResponseEntity<UserResponseDTO> promoteToAdmin(@PathVariable String username, Authentication authentication) {
        User promoted = userService.promoteToAdmin(username, authentication);
        return ResponseEntity.ok(UserResponseDTO.from(promoted));
    }

    @DeleteMapping("/delete/{username}")
    public ResponseEntity<String> deleteUser(@PathVariable String username, Authentication authentication) {
        userService.deleteUser(username, authentication);
        return ResponseEntity.ok("User deleted successfully");
    }
}
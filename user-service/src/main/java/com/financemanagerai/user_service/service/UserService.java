package com.financemanagerai.user_service.service;

import com.financemanagerai.user_service.entity.Role;
import com.financemanagerai.user_service.entity.User;
import com.financemanagerai.user_service.repository.UserRepository;
import com.financemanagerai.user_service.security.JwtUtil;
import jakarta.transaction.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuditService auditService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil, AuditService auditService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.auditService = auditService;
    }

    public User registerUser(User user) {
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists");
        }
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public String login(String username, String rawPassword) {
        return userRepository.findByUsernameAndActiveTrue(username)
                .filter(user -> passwordEncoder.matches(rawPassword, user.getPassword()))
                .map(user -> jwtUtil.generateToken(user.getUsername(), user.getRole().name()))
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));
    }

    public User promoteToAdmin(String username, Authentication authentication) {
        User user = userRepository.findByUsernameAndActiveTrue(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole() == Role.ADMIN) {
            throw new RuntimeException("User is already an admin");
        }

        user.setRole(Role.ADMIN);
        User updated = userRepository.save(user);
        auditService.logAction("PROMOTE_USER", authentication.getName(), username);
        return updated;
    }

    @Transactional
    public void deleteUser(String username, Authentication authentication) {
        String requester = authentication.getName();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));

        User user = userRepository.findByUsernameAndActiveTrue(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (isAdmin || requester.equals(username)) {
            user.setActive(false);
            userRepository.save(user);
            auditService.logAction("DELETE_USER", requester, username);
        } else {
            throw new AccessDeniedException("You can only delete your own account");
        }
    }

}
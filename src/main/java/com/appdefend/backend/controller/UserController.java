package com.appdefend.backend.controller;

import com.appdefend.backend.dto.AuthDtos.CreateUserRequest;
import com.appdefend.backend.dto.AuthDtos.UpdateUserRequest;
import com.appdefend.backend.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "User administration APIs")
public class UserController {
    private final AuthService authService;

    public UserController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('USER_READ')")
    @Operation(summary = "List users", description = "Returns all platform users with their assigned roles, permissions and views.")
    public List<?> listUsers() {
        return authService.findAllUsers().stream().map(authService::toSummary).toList();
    }

    @PostMapping
    @PreAuthorize("hasAuthority('USER_WRITE')")
    @Operation(summary = "Create user", description = "Creates a platform user and assigns one or more roles.")
    public Object createUser(@Valid @RequestBody CreateUserRequest request) {
        return authService.toSummary(authService.createUser(request));
    }

    @PutMapping("/{userId}")
    @PreAuthorize("hasAuthority('USER_WRITE')")
    @Operation(summary = "Update user", description = "Updates a user profile and replaces role assignments.")
    public Object updateUser(@PathVariable Long userId, @Valid @RequestBody UpdateUserRequest request) {
        return authService.toSummary(authService.updateUser(userId, request));
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasAuthority('USER_WRITE')")
    @Operation(summary = "Delete user", description = "Deletes a user and related role mappings.")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        authService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }
}

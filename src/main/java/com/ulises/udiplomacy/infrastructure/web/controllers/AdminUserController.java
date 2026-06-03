package com.ulises.udiplomacy.infrastructure.web.controllers;

import com.ulises.udiplomacy.application.port.input.DeleteUserUseCase;
import com.ulises.udiplomacy.application.port.input.ListUsersUseCase;
import com.ulises.udiplomacy.application.port.input.UpdateUserRoleUseCase;
import com.ulises.udiplomacy.infrastructure.web.dto.request.UpdateRoleRequest;
import com.ulises.udiplomacy.infrastructure.web.dto.response.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {
    private final ListUsersUseCase listUsersUseCase;
    private final UpdateUserRoleUseCase updateUserRoleUseCase;
    private final DeleteUserUseCase deleteUserUseCase;

    public AdminUserController(ListUsersUseCase listUsersUseCase,
                               UpdateUserRoleUseCase updateUserRoleUseCase,
                               DeleteUserUseCase deleteUserUseCase) {
        this.listUsersUseCase = listUsersUseCase;
        this.updateUserRoleUseCase = updateUserRoleUseCase;
        this.deleteUserUseCase = deleteUserUseCase;
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> listUsers() {
        var users = listUsersUseCase.execute();
        return ResponseEntity.ok(users.stream().map(UserResponse::from).toList());
    }

    @PutMapping("/{userId}/role")
    public ResponseEntity<Void> updateRole(@PathVariable String userId,
                                            @Valid @RequestBody UpdateRoleRequest request) {
        updateUserRoleUseCase.execute(userId, request.role());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable String userId,
                                            Authentication auth) {
        deleteUserUseCase.execute(userId, auth.getName());
        return ResponseEntity.noContent().build();
    }
}

package com.ulises.udiplomacy.infrastructure.web.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateRoleRequest(@NotBlank String role) {}

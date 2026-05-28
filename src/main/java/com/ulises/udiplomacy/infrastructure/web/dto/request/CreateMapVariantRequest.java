package com.ulises.udiplomacy.infrastructure.web.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateMapVariantRequest(
        @NotBlank String name,
        @NotBlank String mapJson,
        String svgContent
) {}

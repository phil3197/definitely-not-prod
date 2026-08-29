package com.definitelynotprod.controller.dto;

import java.time.Instant;
import java.util.List;

public record RegistryResponse(
        Instant loadedAt,
        int definitionCount,
        int endpointCount,
        List<RegistrySourceResponse> sources,
        Instant generatedAt
) {
}

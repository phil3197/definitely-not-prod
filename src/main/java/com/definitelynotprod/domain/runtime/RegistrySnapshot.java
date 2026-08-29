package com.definitelynotprod.domain.runtime;

import java.time.Instant;
import java.util.List;

public record RegistrySnapshot(
        Instant loadedAt,
        List<LoadedEndpointDefinition> endpoints,
        List<LoadedDefinitionSource> sources
) {

    public static RegistrySnapshot empty() {
        return new RegistrySnapshot(Instant.EPOCH, List.of(), List.of());
    }
}

package com.definitelynotprod.domain.runtime;

import com.definitelynotprod.domain.definition.EndpointDefinition;

public record LoadedEndpointDefinition(
        LoadedDefinitionSource source,
        String fullPath,
        String normalizedMethod,
        int order,
        EndpointDefinition definition
) {
}

package com.definitelynotprod.domain.runtime;

import java.nio.file.Path;

public record LoadedDefinitionSource(
        Path file,
        String apiName,
        String version
) {
}

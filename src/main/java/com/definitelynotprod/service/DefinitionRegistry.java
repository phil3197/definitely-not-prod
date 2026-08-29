package com.definitelynotprod.service;

import com.definitelynotprod.domain.definition.EndpointDefinition;
import com.definitelynotprod.domain.definition.MockDefinitionFile;
import com.definitelynotprod.domain.runtime.LoadedDefinitionSource;
import com.definitelynotprod.domain.runtime.LoadedEndpointDefinition;
import com.definitelynotprod.domain.runtime.RegistrySnapshot;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class DefinitionRegistry {

    private final DefinitionLoader definitionLoader;
    private final DefinitionValidator definitionValidator;
    private final AtomicReference<RegistrySnapshot> snapshotReference = new AtomicReference<>(RegistrySnapshot.empty());

    public DefinitionRegistry(DefinitionLoader definitionLoader, DefinitionValidator definitionValidator) {
        this.definitionLoader = definitionLoader;
        this.definitionValidator = definitionValidator;
    }

    @PostConstruct
    public void initialize() {
        reload();
    }

    public RegistrySnapshot currentSnapshot() {
        return snapshotReference.get();
    }

    public synchronized RegistrySnapshot reload() {
        List<DefinitionLoader.LoadedDefinitionFile> loadedFiles = definitionLoader.loadAll();
        definitionValidator.validate(loadedFiles);

        List<LoadedEndpointDefinition> endpoints = new ArrayList<>();
        List<LoadedDefinitionSource> sources = new ArrayList<>();
        int order = 0;

        for (DefinitionLoader.LoadedDefinitionFile loadedFile : loadedFiles) {
            MockDefinitionFile definition = loadedFile.definition();
            LoadedDefinitionSource source = new LoadedDefinitionSource(loadedFile.path(), definition.getApiName(), definition.getVersion());
            sources.add(source);
            for (EndpointDefinition endpoint : definition.getEndpoints()) {
                if (!endpoint.isEnabled()) {
                    continue;
                }
                endpoints.add(new LoadedEndpointDefinition(
                        source,
                        normalizePath(definition.getBasePath(), endpoint.getPath()),
                        endpoint.getMethod().toUpperCase(Locale.ROOT),
                        order++,
                        endpoint
                ));
            }
        }

        endpoints.sort(Comparator
                .comparingInt((LoadedEndpointDefinition endpoint) -> endpoint.definition().getPriority()).reversed()
                .thenComparingInt(LoadedEndpointDefinition::order));

        RegistrySnapshot snapshot = new RegistrySnapshot(Instant.now(), List.copyOf(endpoints), List.copyOf(sources));
        snapshotReference.set(snapshot);
        return snapshot;
    }

    private String normalizePath(String basePath, String endpointPath) {
        String combined = trimTrailingSlash(basePath) + ensureLeadingSlash(endpointPath);
        return combined.replaceAll("//+", "/");
    }

    private String trimTrailingSlash(String value) {
        if (value.length() > 1 && value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }

    private String ensureLeadingSlash(String value) {
        return value.startsWith("/") ? value : "/" + value;
    }
}

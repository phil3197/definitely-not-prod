package com.definitelynotprod.controller;

import com.definitelynotprod.controller.dto.RegistryResponse;
import com.definitelynotprod.controller.dto.RegistrySourceResponse;
import com.definitelynotprod.domain.runtime.RegistrySnapshot;
import com.definitelynotprod.service.DefinitionRegistry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final DefinitionRegistry definitionRegistry;

    public AdminController(DefinitionRegistry definitionRegistry) {
        this.definitionRegistry = definitionRegistry;
    }

    @GetMapping("/registry")
    public RegistryResponse registry() {
        return snapshotResponse(definitionRegistry.currentSnapshot());
    }

    @PostMapping("/reload")
    public RegistryResponse reload() {
        return snapshotResponse(definitionRegistry.reload());
    }

    private RegistryResponse snapshotResponse(RegistrySnapshot snapshot) {
        return new RegistryResponse(
                snapshot.loadedAt(),
                snapshot.sources().size(),
                snapshot.endpoints().size(),
                snapshot.sources().stream()
                        .map(source -> new RegistrySourceResponse(
                                source.file().toString(),
                                source.apiName(),
                                source.version()))
                        .toList(),
                Instant.now()
        );
    }
}

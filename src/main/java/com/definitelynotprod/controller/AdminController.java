package com.definitelynotprod.controller;

import com.definitelynotprod.domain.runtime.RegistrySnapshot;
import com.definitelynotprod.service.DefinitionRegistry;
import com.definitelynotprod.service.ReloadService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final ReloadService reloadService;
    private final DefinitionRegistry definitionRegistry;

    public AdminController(ReloadService reloadService, DefinitionRegistry definitionRegistry) {
        this.reloadService = reloadService;
        this.definitionRegistry = definitionRegistry;
    }

    @GetMapping("/registry")
    public Map<String, Object> registry() {
        return snapshotResponse(definitionRegistry.currentSnapshot());
    }

    @PostMapping("/reload")
    public Map<String, Object> reload() {
        return snapshotResponse(reloadService.reload());
    }

    private Map<String, Object> snapshotResponse(RegistrySnapshot snapshot) {
        return Map.of(
                "loadedAt", snapshot.loadedAt(),
                "definitionCount", snapshot.sources().size(),
                "endpointCount", snapshot.endpoints().size(),
                "sources", snapshot.sources().stream()
                        .map(source -> Map.of(
                                "file", source.file().toString(),
                                "apiName", source.apiName(),
                                "version", source.version()))
                        .toList(),
                "generatedAt", Instant.now()
        );
    }
}

package com.definitelynotprod.service;

import com.definitelynotprod.domain.runtime.RegistrySnapshot;
import org.springframework.stereotype.Service;

@Service
public class ReloadService {

    private final DefinitionRegistry definitionRegistry;

    public ReloadService(DefinitionRegistry definitionRegistry) {
        this.definitionRegistry = definitionRegistry;
    }

    public RegistrySnapshot reload() {
        return definitionRegistry.reload();
    }
}

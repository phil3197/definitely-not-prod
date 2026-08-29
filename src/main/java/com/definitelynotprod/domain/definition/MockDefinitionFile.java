package com.definitelynotprod.domain.definition;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class MockDefinitionFile {

    @NotBlank
    private String apiName;

    @NotBlank
    private String version;

    @NotBlank
    private String basePath;

    private String description;

    @Valid
    @NotEmpty
    private List<EndpointDefinition> endpoints;

    public String getApiName() {
        return apiName;
    }

    public void setApiName(String apiName) {
        this.apiName = apiName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<EndpointDefinition> getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(List<EndpointDefinition> endpoints) {
        this.endpoints = endpoints;
    }
}

package com.definitelynotprod.service;

import com.definitelynotprod.domain.definition.EndpointDefinition;
import com.definitelynotprod.domain.definition.MockDefinitionFile;
import com.definitelynotprod.domain.definition.ResponseDefinition;
import com.definitelynotprod.exception.DefinitionLoadException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefinitionValidatorTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    private final DefinitionValidator definitionValidator = new DefinitionValidator(validator);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldRejectDuplicateEnabledMatchSignatures() throws Exception {
        MockDefinitionFile definition = new MockDefinitionFile();
        definition.setApiName("customer-api");
        definition.setVersion("v1");
        definition.setBasePath("/api/customers");
        definition.setEndpoints(List.of(
                endpoint("first", true),
                endpoint("second", true)
        ));

        List<DefinitionLoader.LoadedDefinitionFile> loadedFiles = List.of(
                new DefinitionLoader.LoadedDefinitionFile(Path.of("definitions/example.json"), definition)
        );

        assertThatThrownBy(() -> definitionValidator.validate(loadedFiles))
                .isInstanceOf(DefinitionLoadException.class)
                .hasMessageContaining("duplicates an existing enabled mock signature");
    }

    @Test
    void shouldAllowDisabledDuplicateMatchSignature() throws Exception {
        MockDefinitionFile definition = new MockDefinitionFile();
        definition.setApiName("customer-api");
        definition.setVersion("v1");
        definition.setBasePath("/api/customers");
        definition.setEndpoints(List.of(
                endpoint("first", true),
                endpoint("second", false)
        ));

        List<DefinitionLoader.LoadedDefinitionFile> loadedFiles = List.of(
                new DefinitionLoader.LoadedDefinitionFile(Path.of("definitions/example.json"), definition)
        );

        assertThatCode(() -> definitionValidator.validate(loadedFiles))
                .doesNotThrowAnyException();
    }

    private EndpointDefinition endpoint(String name, boolean enabled) throws Exception {
        EndpointDefinition endpoint = new EndpointDefinition();
        endpoint.setName(name);
        endpoint.setEnabled(enabled);
        endpoint.setMethod("GET");
        endpoint.setPath("/42");
        endpoint.setQueryParams(Map.of("status", "active"));
        endpoint.setHeaders(Map.of("X-Env", "dev"));
        endpoint.setRequestBodyMatch(objectMapper.readTree("{\"name\":\"Grace Hopper\"}"));

        ResponseDefinition response = new ResponseDefinition();
        response.setStatus(200);
        endpoint.setResponse(response);
        return endpoint;
    }
}

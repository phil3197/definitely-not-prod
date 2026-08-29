package com.definitelynotprod.service;

import com.definitelynotprod.domain.definition.EndpointDefinition;
import com.definitelynotprod.domain.definition.ResponseDefinition;
import com.definitelynotprod.domain.runtime.LoadedDefinitionSource;
import com.definitelynotprod.domain.runtime.LoadedEndpointDefinition;
import com.definitelynotprod.domain.runtime.MatchStatus;
import com.definitelynotprod.domain.runtime.RegistrySnapshot;
import org.junit.jupiter.api.Test;
import org.springframework.util.LinkedMultiValueMap;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RequestMatcherTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RequestMatcher requestMatcher = new RequestMatcher(objectMapper);

    @Test
    void shouldMatchHighestPriorityEndpoint() throws Exception {
        LoadedEndpointDefinition low = endpoint("low", 1, "GET", Map.of("x-env", "dev"), null);
        LoadedEndpointDefinition high = endpoint("high", 10, "GET", Map.of("x-env", "dev"), null);
        RegistrySnapshot snapshot = new RegistrySnapshot(Instant.now(), List.of(high, low), List.of());

        var result = requestMatcher.match(snapshot, "GET", "/api/test", new LinkedMultiValueMap<>(), Map.of("x-env", "dev"), null);

        assertThat(result.status()).isEqualTo(MatchStatus.MATCHED);
        assertThat(result.endpoint().definition().getName()).isEqualTo("high");
    }

    @Test
    void shouldReturnMethodNotAllowedWhenOnlyPathMatches() {
        RegistrySnapshot snapshot = new RegistrySnapshot(Instant.now(), List.of(endpoint("only-post", 1, "POST", null, null)), List.of());

        var result = requestMatcher.match(snapshot, "GET", "/api/test", new LinkedMultiValueMap<>(), Map.of(), null);

        assertThat(result.status()).isEqualTo(MatchStatus.METHOD_NOT_ALLOWED);
    }

    @Test
    void shouldReturnInvalidJsonWhenBodyMatchingConfigured() {
        RegistrySnapshot snapshot = new RegistrySnapshot(Instant.now(), List.of(endpoint("body", 1, "POST", null, object("{\"name\":\"Grace\"}"))), List.of());

        var result = requestMatcher.match(snapshot, "POST", "/api/test", new LinkedMultiValueMap<>(), Map.of(), "{invalid");

        assertThat(result.status()).isEqualTo(MatchStatus.INVALID_JSON_BODY);
    }

    @Test
    void shouldReturnNotFoundWhenMethodMatchesButHeaderDoesNot() {
        RegistrySnapshot snapshot = new RegistrySnapshot(Instant.now(), List.of(endpoint("get-secure", 1, "GET", Map.of("x-env", "dev"), null)), List.of());

        var result = requestMatcher.match(snapshot, "GET", "/api/test", new LinkedMultiValueMap<>(), Map.of(), null);

        assertThat(result.status()).isEqualTo(MatchStatus.NOT_FOUND);
    }

    private LoadedEndpointDefinition endpoint(String name, int priority, String method, Map<String, String> headers, JsonNode bodyMatch) {
        EndpointDefinition endpointDefinition = new EndpointDefinition();
        endpointDefinition.setName(name);
        endpointDefinition.setEnabled(true);
        endpointDefinition.setPriority(priority);
        endpointDefinition.setMethod(method);
        endpointDefinition.setPath("/test");
        endpointDefinition.setHeaders(headers);
        endpointDefinition.setRequestBodyMatch(bodyMatch);
        ResponseDefinition responseDefinition = new ResponseDefinition();
        responseDefinition.setStatus(200);
        endpointDefinition.setResponse(responseDefinition);
        return new LoadedEndpointDefinition(new LoadedDefinitionSource(Path.of("test.json"), "api", "v1"), "/api/test", method, 0, endpointDefinition);
    }

    private JsonNode object(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

package com.definitelynotprod.service;

import com.definitelynotprod.domain.definition.EndpointDefinition;
import com.definitelynotprod.domain.runtime.LoadedEndpointDefinition;
import com.definitelynotprod.domain.runtime.MatchResult;
import com.definitelynotprod.domain.runtime.RegistrySnapshot;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class RequestMatcher {

    private final ObjectMapper objectMapper;

    public RequestMatcher(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public MatchResult match(RegistrySnapshot snapshot,
                             String method,
                             String path,
                             MultiValueMap<String, String> queryParams,
                             Map<String, String> headers,
                             String requestBody) {
        boolean pathMatched = false;
        boolean methodMatched = false;
        boolean invalidJsonDetected = false;

        List<LoadedEndpointDefinition> endpoints = snapshot.endpoints();
        for (LoadedEndpointDefinition endpoint : endpoints) {
            if (!endpoint.fullPath().equals(path)) {
                continue;
            }
            pathMatched = true;

            if (!endpoint.normalizedMethod().equals(method.toUpperCase(Locale.ROOT))) {
                continue;
            }
            methodMatched = true;

            if (!matchesQueryParams(endpoint.definition(), queryParams)) {
                continue;
            }
            if (!matchesHeaders(endpoint.definition(), headers)) {
                continue;
            }

            BodyMatchOutcome bodyMatchOutcome = matchesBody(endpoint.definition(), requestBody);
            if (bodyMatchOutcome == BodyMatchOutcome.INVALID_JSON) {
                invalidJsonDetected = true;
                continue;
            }
            if (bodyMatchOutcome == BodyMatchOutcome.NO_MATCH) {
                continue;
            }

            return MatchResult.matched(endpoint);
        }

        if (invalidJsonDetected) {
            return MatchResult.invalidJsonBody();
        }
        if (pathMatched && !methodMatched) {
            return MatchResult.methodNotAllowed();
        }
        return MatchResult.notFound();
    }

    private boolean matchesQueryParams(EndpointDefinition definition, MultiValueMap<String, String> queryParams) {
        if (definition.getQueryParams() == null || definition.getQueryParams().isEmpty()) {
            return true;
        }
        for (Map.Entry<String, String> entry : definition.getQueryParams().entrySet()) {
            String actual = queryParams.getFirst(entry.getKey());
            if (!entry.getValue().equals(actual)) {
                return false;
            }
        }
        return true;
    }

    private boolean matchesHeaders(EndpointDefinition definition, Map<String, String> headers) {
        if (definition.getHeaders() == null || definition.getHeaders().isEmpty()) {
            return true;
        }
        for (Map.Entry<String, String> entry : definition.getHeaders().entrySet()) {
            String actual = headers.get(entry.getKey().toLowerCase(Locale.ROOT));
            if (!entry.getValue().equals(actual)) {
                return false;
            }
        }
        return true;
    }

    private BodyMatchOutcome matchesBody(EndpointDefinition definition, String requestBody) {
        JsonNode expected = definition.getRequestBodyMatch();
        if (expected == null) {
            return BodyMatchOutcome.MATCH;
        }
        if (requestBody == null || requestBody.isBlank()) {
            return BodyMatchOutcome.NO_MATCH;
        }
        try {
            JsonNode actual = objectMapper.readTree(requestBody);
            return expected.equals(actual) ? BodyMatchOutcome.MATCH : BodyMatchOutcome.NO_MATCH;
        } catch (JacksonException e) {
            return BodyMatchOutcome.INVALID_JSON;
        }
    }

    private enum BodyMatchOutcome {
        MATCH,
        NO_MATCH,
        INVALID_JSON
    }
}

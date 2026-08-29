package com.definitelynotprod.service;

import com.definitelynotprod.domain.definition.EndpointDefinition;
import com.definitelynotprod.domain.definition.MockDefinitionFile;
import com.definitelynotprod.exception.DefinitionLoadException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import tools.jackson.databind.JsonNode;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Component
public class DefinitionValidator {

    private final Validator validator;

    public DefinitionValidator(Validator validator) {
        this.validator = validator;
    }

    public void validate(List<DefinitionLoader.LoadedDefinitionFile> loadedFiles) {
        List<String> errors = new ArrayList<>();
        Set<String> uniqueEndpointKeys = new HashSet<>();
        Set<String> uniqueMatchSignatures = new HashSet<>();

        for (DefinitionLoader.LoadedDefinitionFile loadedFile : loadedFiles) {
            validateBean(loadedFile.path(), loadedFile.definition(), errors);
            validateBusinessRules(loadedFile.path(), loadedFile.definition(), errors, uniqueEndpointKeys, uniqueMatchSignatures);
        }

        if (!errors.isEmpty()) {
            throw new DefinitionLoadException("Definition validation failed:\n - " + String.join("\n - ", errors));
        }
    }

    private void validateBean(Path path, MockDefinitionFile definition, List<String> errors) {
        for (ConstraintViolation<MockDefinitionFile> violation : validator.validate(definition)) {
            errors.add(path + " " + violation.getPropertyPath() + " " + violation.getMessage());
        }
    }

    private void validateBusinessRules(Path path,
                                       MockDefinitionFile definition,
                                       List<String> errors,
                                       Set<String> uniqueEndpointKeys,
                                       Set<String> uniqueMatchSignatures) {
        if (!definition.getBasePath().startsWith("/")) {
            errors.add(path + " basePath must start with '/'");
        }

        for (EndpointDefinition endpoint : definition.getEndpoints()) {
            if (!endpoint.getPath().startsWith("/")) {
                errors.add(path + " endpoint '" + endpoint.getName() + "' path must start with '/'");
            }

            try {
                HttpMethod.valueOf(endpoint.getMethod().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                errors.add(path + " endpoint '" + endpoint.getName() + "' has unsupported method '" + endpoint.getMethod() + "'");
            }

            if (StringUtils.hasText(endpoint.getResponse().getContentType())) {
                try {
                    MediaType.parseMediaType(endpoint.getResponse().getContentType());
                } catch (IllegalArgumentException e) {
                    errors.add(path + " endpoint '" + endpoint.getName() + "' has invalid response contentType '" + endpoint.getResponse().getContentType() + "'");
                }
            }

            String endpointKey = definition.getApiName() + "|" + definition.getVersion() + "|" + endpoint.getName();
            if (!uniqueEndpointKeys.add(endpointKey)) {
                errors.add(path + " duplicate endpoint name detected for api/version/name: " + endpointKey);
            }

            if (endpoint.isEnabled()) {
                String matchSignature = buildMatchSignature(definition, endpoint);
                if (!uniqueMatchSignatures.add(matchSignature)) {
                    errors.add(path + " endpoint '" + endpoint.getName() + "' duplicates an existing enabled mock signature: " + matchSignature);
                }
            }
        }
    }

    private String buildMatchSignature(MockDefinitionFile definition, EndpointDefinition endpoint) {
        return String.join("|",
                endpoint.getMethod().toUpperCase(Locale.ROOT),
                normalizePath(definition.getBasePath(), endpoint.getPath()),
                canonicalizeMap(endpoint.getQueryParams()),
                canonicalizeHeaders(endpoint.getHeaders()),
                canonicalizeBody(endpoint.getRequestBodyMatch()));
    }

    private String normalizePath(String basePath, String endpointPath) {
        String normalizedBasePath = basePath.length() > 1 && basePath.endsWith("/")
                ? basePath.substring(0, basePath.length() - 1)
                : basePath;
        String normalizedEndpointPath = endpointPath.startsWith("/") ? endpointPath : "/" + endpointPath;
        return (normalizedBasePath + normalizedEndpointPath).replaceAll("//+", "/");
    }

    private String canonicalizeHeaders(Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) {
            return "";
        }
        return headers.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().toLowerCase(Locale.ROOT)))
                .map(entry -> entry.getKey().toLowerCase(Locale.ROOT) + "=" + Objects.toString(entry.getValue(), ""))
                .reduce((left, right) -> left + "&" + right)
                .orElse("");
    }

    private String canonicalizeMap(Map<String, String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return values.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + Objects.toString(entry.getValue(), ""))
                .reduce((left, right) -> left + "&" + right)
                .orElse("");
    }

    private String canonicalizeBody(JsonNode body) {
        return body == null ? "" : body.toString();
    }
}

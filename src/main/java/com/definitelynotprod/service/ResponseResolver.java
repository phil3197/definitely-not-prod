package com.definitelynotprod.service;

import com.definitelynotprod.domain.definition.ResponseDefinition;
import com.definitelynotprod.domain.runtime.LoadedEndpointDefinition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ResponseResolver {

    public ResponseEntity<Object> resolve(LoadedEndpointDefinition endpoint) {
        applyDelay(endpoint.definition().getDelayMs());

        ResponseDefinition response = endpoint.definition().getResponse();
        HttpHeaders headers = new HttpHeaders();
        if (response.getHeaders() != null) {
            response.getHeaders().forEach(headers::add);
        }
        if (StringUtils.hasText(response.getContentType())) {
            headers.setContentType(MediaType.parseMediaType(response.getContentType()));
        } else if (response.getBody() != null) {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }

        return ResponseEntity.status(HttpStatusCode.valueOf(response.getStatus()))
                .headers(headers)
                .body(response.getBody());
    }

    private void applyDelay(long delayMs) {
        if (delayMs <= 0) {
            return;
        }
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while applying mock delay", e);
        }
    }
}

package com.definitelynotprod.controller;

import com.definitelynotprod.domain.runtime.MatchResult;
import com.definitelynotprod.domain.runtime.MatchStatus;
import com.definitelynotprod.service.DefinitionRegistry;
import com.definitelynotprod.service.RequestMatcher;
import com.definitelynotprod.service.ResponseResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@RestController
public class MockDispatcherController {

    private static final Logger log = LoggerFactory.getLogger(MockDispatcherController.class);

    private final DefinitionRegistry definitionRegistry;
    private final RequestMatcher requestMatcher;
    private final ResponseResolver responseResolver;

    public MockDispatcherController(DefinitionRegistry definitionRegistry,
                                    RequestMatcher requestMatcher,
                                    ResponseResolver responseResolver) {
        this.definitionRegistry = definitionRegistry;
        this.requestMatcher = requestMatcher;
        this.responseResolver = responseResolver;
    }

    @RequestMapping("/**")
    public ResponseEntity<Object> dispatch(HttpServletRequest request,
                                           @RequestBody(required = false) String requestBody) {
        MatchResult result = requestMatcher.match(
                definitionRegistry.currentSnapshot(),
                request.getMethod(),
                request.getRequestURI(),
                copyQueryParams(request),
                copyHeaders(request),
                requestBody
        );

        if (result.status() == MatchStatus.MATCHED) {
            log.info("Matched mock api={} version={} endpoint={} method={} path={}",
                    result.endpoint().source().apiName(),
                    result.endpoint().source().version(),
                    result.endpoint().definition().getName(),
                    result.endpoint().normalizedMethod(),
                    result.endpoint().fullPath());
            return responseResolver.resolve(result.endpoint());
        }
        if (result.status() == MatchStatus.METHOD_NOT_ALLOWED) {
            return ResponseEntity.status(405).body(Map.of("error", "Method Not Allowed"));
        }
        if (result.status() == MatchStatus.INVALID_JSON_BODY) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid JSON request body"));
        }
        return ResponseEntity.status(404).body(Map.of("error", "No mock matched"));
    }

    private MultiValueMap<String, String> copyQueryParams(HttpServletRequest request) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        request.getParameterMap().forEach((key, values) -> {
            if (values != null) {
                for (String value : values) {
                    params.add(key, value);
                }
            }
        });
        return params;
    }

    private Map<String, String> copyHeaders(HttpServletRequest request) {
        Enumeration<String> names = request.getHeaderNames();
        if (names == null) {
            return Collections.emptyMap();
        }
        Map<String, String> headers = new LinkedHashMap<>();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            headers.put(name.toLowerCase(Locale.ROOT), request.getHeader(name));
        }
        return headers;
    }
}

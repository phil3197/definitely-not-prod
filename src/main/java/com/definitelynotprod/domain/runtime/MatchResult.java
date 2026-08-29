package com.definitelynotprod.domain.runtime;

public record MatchResult(
        MatchStatus status,
        LoadedEndpointDefinition endpoint
) {

    public static MatchResult matched(LoadedEndpointDefinition endpoint) {
        return new MatchResult(MatchStatus.MATCHED, endpoint);
    }

    public static MatchResult notFound() {
        return new MatchResult(MatchStatus.NOT_FOUND, null);
    }

    public static MatchResult methodNotAllowed() {
        return new MatchResult(MatchStatus.METHOD_NOT_ALLOWED, null);
    }

    public static MatchResult invalidJsonBody() {
        return new MatchResult(MatchStatus.INVALID_JSON_BODY, null);
    }
}

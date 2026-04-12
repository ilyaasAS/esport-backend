package org.example.web.dto;

import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builds the canonical JSON error body used by {@link org.example.web.advice.GlobalExceptionHandler}
 * and security entry points so all API error responses share the same schema.
 */
public final class StandardApiErrorBody {

    private StandardApiErrorBody() {
    }

    public static Map<String, Object> toMap(
            HttpStatus status,
            String errorCode,
            String message,
            String path,
            Map<String, String> details
    ) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("errorCode", errorCode);
        body.put("message", message);
        body.put("path", path);
        if (details != null && !details.isEmpty()) {
            body.put("details", details);
        }
        return body;
    }
}

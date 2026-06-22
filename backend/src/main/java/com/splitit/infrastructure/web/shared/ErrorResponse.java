package com.splitit.infrastructure.web.shared;

import java.util.List;

/** Uniform error body: {status, message, errors[]}. errors holds field-validation messages. */
public record ErrorResponse(int status, String message, List<String> errors) {

    public static ErrorResponse of(int status, String message) {
        return new ErrorResponse(status, message, List.of());
    }

    public static ErrorResponse of(int status, String message, List<String> errors) {
        return new ErrorResponse(status, message, errors);
    }
}

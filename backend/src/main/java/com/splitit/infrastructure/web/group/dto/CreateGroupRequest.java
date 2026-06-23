package com.splitit.infrastructure.web.group.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateGroupRequest(

        @NotBlank(message = "must not be blank")
        @Size(max = 100, message = "must be at most 100 characters")
        String name,

        @Size(max = 500, message = "must be at most 500 characters")
        String description
) {
}

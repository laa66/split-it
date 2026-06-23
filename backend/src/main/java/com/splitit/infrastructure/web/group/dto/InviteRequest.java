package com.splitit.infrastructure.web.group.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InviteRequest(

        @NotBlank(message = "must not be blank")
        @Email(message = "must be a valid email")
        @Size(max = 255, message = "must be at most 255 characters")
        String email
) {
}

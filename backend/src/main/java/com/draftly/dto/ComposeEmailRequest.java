package com.draftly.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ComposeEmailRequest {

    private String userName;

    @NotBlank(message = "userEmail is required")
    @Email(message = "userEmail must be a valid email address")
    private String userEmail;

    private String recipient;

    @NotBlank(message = "prompt is required")
    private String prompt;

    private String tone;

    private String context;

    private String desiredLength;
}

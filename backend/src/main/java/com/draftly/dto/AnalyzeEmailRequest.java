package com.draftly.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AnalyzeEmailRequest {

    private String userName;

    @NotBlank(message = "userEmail is required")
    @Email(message = "userEmail must be a valid email address")
    private String userEmail;

    private String gmailMessageId;

    private String gmailThreadId;

    @NotBlank(message = "sender is required")
    private String sender;

    private String senderName;

    private String senderEmail;

    @NotEmpty(message = "recipients must contain at least one email address")
    private List<String> recipients;

    @NotBlank(message = "subject is required")
    private String subject;

    @NotBlank(message = "body is required")
    private String body;

    private String threadHistory;
}

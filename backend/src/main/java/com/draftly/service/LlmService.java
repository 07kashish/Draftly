package com.draftly.service;

public interface LlmService {

    String generateReply(
            String userName,
            String sender,
            String senderName,
            String senderEmail,
            String subject,
            String body,
            String threadHistory,
            String category,
            String tone,
            String urgency,
            String strategy,
            String userToneProfileContext
    );

    default String generateComposeEmail(
            String userName,
            String recipient,
            String prompt,
            String tone,
            String context,
            String desiredLength,
            String userToneProfileContext
    ) {
        throw new RuntimeException("Compose email generation is unavailable");
    }
}

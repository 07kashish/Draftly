package com.draftly.service;

import com.draftly.dto.ComposeEmailRequest;
import com.draftly.dto.ComposeEmailResponse;
import com.draftly.entity.User;
import com.draftly.entity.UserToneProfile;
import com.draftly.config.OpenRouterConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComposeEmailService {

    private final UserService userService;
    private final LlmService llmService;
    private final ToneProfileService toneProfileService;
    private final OpenRouterConfig openRouterConfig;

    public ComposeEmailResponse compose(ComposeEmailRequest request) {
        if (!StringUtils.hasText(request.getPrompt())) {
            throw new IllegalArgumentException("prompt is required");
        }

        User user = userService.findOrCreateUser(request.getUserName(), request.getUserEmail());
        UserToneProfile profile = toneProfileService.findByUserEmail(user.getEmail()).orElse(null);
        String toneContext = toneProfileService.buildGenerationContext(profile);
        String tone = valueOrDefault(request.getTone(), valueOrDefault(profile == null ? null : profile.getPreferredTone(), "PROFESSIONAL"));
        String strategy = buildStrategy(request);
        boolean openRouterEnabled = StringUtils.hasText(openRouterConfig.getApiKey());
        log.info("Compose Mode: OpenRouter enabled = {}", openRouterEnabled);

        try {
            ComposeEmailResponse response = parseLlmComposeResponse(llmService.generateComposeEmail(
                    user.getName(),
                    request.getRecipient(),
                    request.getPrompt(),
                    tone,
                    request.getContext(),
                    request.getDesiredLength(),
                    toneContext
            ), tone, strategy);

            if (!isValidComposeResponse(response, request)) {
                log.warn("Compose Mode: OpenRouter response failed validation, using fallback");
                log.info("Compose Mode: using fallback = true");
                return fallbackCompose(user, request, profile, tone, strategy);
            }

            log.info("Compose Mode: using fallback = false");
            return response;
        } catch (Exception exception) {
            log.warn("OpenRouter compose failed, using fallback: {}", exception.getMessage());
            log.info("Compose Mode: using fallback = true");
            return fallbackCompose(user, request, profile, tone, strategy);
        }
    }

    private ComposeEmailResponse parseLlmComposeResponse(String content, String tone, String strategy) {
        String subject = "";
        String body = content == null ? "" : content.trim();
        String[] lines = body.split("\\R");

        if (lines.length > 0 && lines[0].toLowerCase(Locale.ROOT).startsWith("subject:")) {
            subject = lines[0].substring("subject:".length()).trim();
            body = body.substring(lines[0].length()).trim();
            if (body.toLowerCase(Locale.ROOT).startsWith("body:")) {
                body = body.substring("body:".length()).trim();
            }
        }

        if (!StringUtils.hasText(subject)) {
            subject = "Follow Up";
        }

        return ComposeEmailResponse.builder()
                .subject(subject)
                .draft(body)
                .tone(tone)
                .strategy(strategy)
                .build();
    }

    private ComposeEmailResponse fallbackCompose(
            User user,
            ComposeEmailRequest request,
            UserToneProfile profile,
            String tone,
            String strategy
    ) {
        String subject = buildFallbackSubject(request.getPrompt());
        String greeting = greetingFor(request, profile);
        String signOff = valueOrDefault(profile == null ? null : profile.getPreferredSignOff(), "Best regards");
        String body = buildFallbackBody(request, greeting, signOff, user.getName());

        return ComposeEmailResponse.builder()
                .subject(subject)
                .draft(body)
                .tone(tone)
                .strategy(strategy)
                .build();
    }

    private String buildFallbackSubject(String prompt) {
        String lower = prompt.toLowerCase(Locale.ROOT);
        if (lower.contains("deposit") && lower.contains("extension")) {
            return "Request for Extension of Deposit Deadline";
        }
        if (mentionsDeadlineExtension(lower)) {
            return "Request for Extension of Deadline";
        }
        if (lower.contains("meeting")) {
            return "Meeting Request";
        }
        if (lower.contains("follow up") || lower.contains("following up")) {
            return "Follow Up";
        }
        if (lower.contains("thank")) {
            return "Thank You";
        }
        return "Email Request";
    }

    private String buildFallbackBody(ComposeEmailRequest request, String greeting, String signOff, String userName) {
        String lower = request.getPrompt().toLowerCase(Locale.ROOT);
        if (lower.contains("deposit") && lower.contains("extension")) {
            return """
                    %s,

                    I hope you are doing well.

                    I am writing to kindly request an extension for the deposit payment deadline. The deposit amount is significant, and I would appreciate some additional time to arrange the required amount.

                    Thank you for your understanding and consideration. I look forward to your response.

                    %s,
                    %s
                    """.formatted(greeting, signOff, userName).trim();
        }

        if (mentionsDeadlineExtension(lower)) {
            return """
                    %s,

                    I hope you are doing well.

                    I am writing to kindly request an extension for the deadline. I would appreciate some additional time to complete the required process properly.

                    Thank you for your understanding and consideration. I look forward to your response.

                    %s,
                    %s
                    """.formatted(greeting, signOff, userName).trim();
        }

        return """
                %s,

                I hope you are doing well.

                %s

                Thank you for your time and consideration.

                %s,
                %s
                """.formatted(greeting, sentenceFromPrompt(request.getPrompt()), signOff, userName).trim();
    }

    private String sentenceFromPrompt(String prompt) {
        String clean = prompt.trim();
        clean = clean.replaceFirst("(?i)^write an email\\s*", "");
        clean = clean.replaceFirst("(?i)^to\\s+", "");
        clean = clean.replaceFirst("(?i)^for\\s+", "");
        clean = clean.replaceAll("[.]+$", "").trim();

        if (!StringUtils.hasText(clean)) {
            return "I am writing to share this message and would appreciate your response when convenient.";
        }

        if (mentionsDeadlineExtension(clean.toLowerCase(Locale.ROOT))) {
            return "I am writing to kindly request an extension for the deadline and would appreciate some additional time.";
        }

        return "I am writing regarding " + clean.substring(0, 1).toLowerCase(Locale.ROOT) + clean.substring(1) + ".";
    }

    private boolean isValidComposeResponse(ComposeEmailResponse response, ComposeEmailRequest request) {
        if (response == null || !StringUtils.hasText(response.getSubject()) || !StringUtils.hasText(response.getDraft())) {
            return false;
        }

        String draft = response.getDraft();
        String lowerDraft = draft.toLowerCase(Locale.ROOT);
        if (draft.lines().filter(StringUtils::hasText).count() < 4) {
            return false;
        }
        if (lowerDraft.contains("writing to for") || lowerDraft.contains("i am writing to for")) {
            return false;
        }
        return mentionsMainIntent(response, request.getPrompt());
    }

    private boolean mentionsMainIntent(ComposeEmailResponse response, String prompt) {
        String lowerPrompt = prompt.toLowerCase(Locale.ROOT);
        String combined = (response.getSubject() + " " + response.getDraft()).toLowerCase(Locale.ROOT);

        if (lowerPrompt.contains("deposit")) {
            return combined.contains("deposit") && combined.contains("deadline")
                    && (combined.contains("extension") || combined.contains("additional time") || combined.contains("more time"));
        }
        if (mentionsDeadlineExtension(lowerPrompt)) {
            return combined.contains("deadline")
                    && (combined.contains("extension") || combined.contains("additional time") || combined.contains("more time"));
        }
        if (lowerPrompt.contains("meeting")) {
            return combined.contains("meeting");
        }
        if (lowerPrompt.contains("thank")) {
            return combined.contains("thank");
        }

        return true;
    }

    private boolean mentionsDeadlineExtension(String lowerText) {
        return (lowerText.contains("deadline") || lowerText.contains("due date"))
                && (lowerText.contains("extension") || lowerText.contains("extend") || lowerText.contains("extending")
                || lowerText.contains("more time") || lowerText.contains("additional time"));
    }

    private String greetingFor(ComposeEmailRequest request, UserToneProfile profile) {
        String preferredGreeting = valueOrDefault(profile == null ? null : profile.getPreferredGreeting(), "Dear");
        String recipient = request.getRecipient();
        if (StringUtils.hasText(recipient) && recipient.contains("@")) {
            String localPart = recipient.substring(0, recipient.indexOf("@"));
            if (localPart.toLowerCase(Locale.ROOT).contains("admission")) {
                return preferredGreeting + " Admissions Team";
            }
        }
        return preferredGreeting + " Team";
    }

    private String buildStrategy(ComposeEmailRequest request) {
        return "Write a polished %s email from the user's prompt without inventing unsupported facts."
                .formatted(valueOrDefault(request.getTone(), "professional").toLowerCase(Locale.ROOT));
    }

    private String valueOrDefault(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }
}

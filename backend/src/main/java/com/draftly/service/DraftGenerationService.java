package com.draftly.service;

import com.draftly.entity.Email;
import com.draftly.entity.User;
import com.draftly.entity.UserToneProfile;
import com.draftly.enums.EmailCategory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class DraftGenerationService {

    private static final Pattern EMAIL_GREETING_PATTERN = Pattern.compile(
            "(?im)^\\s*(hi|hello|dear)\\s+[^\\r\\n,]{1,80},\\s*$"
    );

    private final LlmService llmService;
    private final ToneProfileService toneProfileService;

    @Autowired
    public DraftGenerationService(LlmService llmService, ToneProfileService toneProfileService) {
        this.llmService = llmService;
        this.toneProfileService = toneProfileService;
    }

    public DraftGenerationService(LlmService llmService) {
        this.llmService = llmService;
        this.toneProfileService = null;
    }

    public String generateDraft(User user, Email email, String strategy) {
        UserToneProfile profile = toneProfileService == null
                ? null
                : toneProfileService.findByUserEmail(user.getEmail()).orElse(null);
        return generateDraft(user, email, strategy, profile);
    }

    public String generateDraft(User user, Email email, String strategy, UserToneProfile profile) {
        try {
            return sanitizeGeneratedReply(llmService.generateReply(
                    user.getName(),
                    email.getSender(),
                    email.getSenderName(),
                    email.getSenderEmail(),
                    email.getSubject(),
                    email.getBody(),
                    email.getThreadHistory(),
                    email.getCategory().name(),
                    email.getTone().name(),
                    email.getUrgency().name(),
                    strategy,
                    buildGenerationContext(profile)
            ));
        } catch (Exception exception) {
            log.warn("OpenRouter failed, using mock fallback: {}", exception.getMessage());
            return generateMockDraft(user, email, strategy, profile);
        }
    }

    String sanitizeGeneratedReply(String generatedReply) {
        if (!hasText(generatedReply)) {
            return generatedReply;
        }

        String cleaned = generatedReply
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .trim();

        Matcher greetingMatcher = EMAIL_GREETING_PATTERN.matcher(cleaned);
        if (greetingMatcher.find() && containsAnalysisPreamble(cleaned.substring(0, greetingMatcher.start()))) {
            cleaned = cleaned.substring(greetingMatcher.start()).trim();
        }

        cleaned = cleaned.replaceFirst("(?is)^\\s*(final\\s+reply|email\\s+reply|draft|response)\\s*:\\s*", "").trim();
        cleaned = cleaned.replaceFirst("(?is)^\\s*body\\s*:\\s*", "").trim();

        return cleaned;
    }

    private boolean containsAnalysisPreamble(String preamble) {
        if (!hasText(preamble)) {
            return false;
        }

        String lower = preamble.toLowerCase(Locale.ROOT);
        return lower.contains("sender's main intent")
                || lower.contains("sender main intent")
                || lower.contains("questions to answer")
                || lower.contains("reply strategy")
                || lower.contains("clean name:")
                || lower.contains("user name:");
    }

    private String generateMockDraft(User user, Email email, String strategy, UserToneProfile profile) {
        String senderName = extractSenderName(email);
        log.info("[Draftly Sender] finalGreetingName: {}", hasText(senderName) ? senderName : "");
        String userName = user.getName();

        return switch (email.getCategory()) {
            case DECISION_REQUEST -> decisionRequestDraft(senderName, userName, email, profile);
            case CONFIRMATION_REQUEST -> formattedDraft(
                    profile,
                    senderName,
                    userName,
                    "Thank you for checking in. I am reviewing this and will confirm shortly. I appreciate your patience."
            );
            case MEETING_REQUEST -> formattedDraft(
                    profile,
                    senderName,
                    userName,
                    meetingRequestDraftBody(email)
            );
            case FOLLOW_UP -> formattedDraft(
                    profile,
                    senderName,
                    userName,
                    "Thanks for following up. I appreciate the reminder and will review this as soon as possible. I'll get back to you with an update shortly."
            );
            case COMPLAINT -> formattedDraft(
                    profile,
                    senderName,
                    userName,
                    "Thank you for bringing this to my attention. I'm sorry for the inconvenience and understand your concern. I'll look into this and follow up with the next steps."
            );
            case INFORMATIONAL -> formattedDraft(
                    profile,
                    senderName,
                    userName,
                    "Thank you for sharing this update. I've noted the information and will keep it in mind for the next steps."
            );
            case PERSONAL -> formattedDraft(
                    profile,
                    senderName,
                    userName,
                    "Thanks for your message. It was good to hear from you, and I appreciate you reaching out."
            );
            case PROFESSIONAL, OTHER -> defaultDraft(senderName, userName, strategy, profile);
        };
    }

    private String decisionRequestDraft(String senderName, String userName, Email email, UserToneProfile profile) {
        String text = ((email.getSubject() == null ? "" : email.getSubject()) + " "
                + (email.getBody() == null ? "" : email.getBody())).toLowerCase();

        if (text.contains("university") || text.contains("acceptance") || text.contains("offer")) {
            return formattedDraft(
                    profile,
                    senderName,
                    userName,
                    "Thank you for following up regarding the university acceptance.\n\nI am currently reviewing the offer and the next steps, and I will confirm my final decision shortly. I appreciate your patience."
            );
        }

        return formattedDraft(
                profile,
                senderName,
                userName,
                "Thank you for following up.\n\nI am currently reviewing this and will confirm my decision shortly. I appreciate your patience."
        );
    }

    private String meetingRequestDraftBody(Email email) {
        String text = ((email.getSubject() == null ? "" : email.getSubject()) + " "
                + (email.getBody() == null ? "" : email.getBody()) + " "
                + (email.getThreadHistory() == null ? "" : email.getThreadHistory())).toLowerCase();

        if (containsAny(text, "when you are ready", "let me know when", "are you ready")) {
            return "Thank you for understanding.\n\nI am reviewing my availability and will confirm a suitable time for the meeting shortly. Please feel free to share a few time slots that work best for you.";
        }

        if (containsAny(text, "when can we", "can we have a meeting", "meeting", "schedule")) {
            return "Thank you for following up about the meeting.\n\nI will check my availability so we can schedule a meeting at a suitable time. Please feel free to share a few time slots that work best for you.";
        }

        return "Thank you for reaching out about scheduling.\n\nI will review my availability and confirm a suitable time shortly. Please feel free to share a few time slots that work best for you.";
    }

    private String defaultDraft(String senderName, String userName, String strategy, UserToneProfile profile) {
        return formattedDraft(
                profile,
                senderName,
                userName,
                "Thank you for your email. I appreciate you reaching out. " + strategy
        );
    }

    private String formattedDraft(UserToneProfile profile, String senderName, String userName, String body) {
        String greeting = profile != null && hasText(profile.getPreferredGreeting())
                ? profile.getPreferredGreeting()
                : "Hi";
        String signOff = profile != null && hasText(profile.getPreferredSignOff())
                ? profile.getPreferredSignOff()
                : "Best regards";
        String normalizedSignOff = signOff.endsWith(",") ? signOff : signOff + ",";

        String greetingLine = hasText(senderName)
                ? "%s %s,".formatted(greeting, senderName)
                : "%s,".formatted(greeting);

        return """
                %s

                %s

                %s
                %s
                """.formatted(greetingLine, body, normalizedSignOff, userName);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private boolean containsAny(String text, String... values) {
        for (String value : values) {
            if (text.contains(value)) {
                return true;
            }
        }
        return false;
    }

    private String buildGenerationContext(UserToneProfile profile) {
        if (toneProfileService != null) {
            return toneProfileService.buildGenerationContext(profile);
        }
        if (profile == null) {
            return "User writing preferences: No learned tone profile yet. Use a professional concise default.";
        }
        return """
                User writing preferences:
                - Preferred tone: %s
                - Preferred greeting: %s
                - Preferred sign-off: %s
                - Average reply length: %s words
                - Common phrases: %s
                """.formatted(
                hasText(profile.getPreferredTone()) ? profile.getPreferredTone() : "PROFESSIONAL",
                hasText(profile.getPreferredGreeting()) ? profile.getPreferredGreeting() : "Hi",
                hasText(profile.getPreferredSignOff()) ? profile.getPreferredSignOff() : "Best regards",
                profile.getAverageReplyLength() == null ? "unknown" : profile.getAverageReplyLength(),
                hasText(profile.getCommonPhrases()) ? profile.getCommonPhrases() : "none"
        );
    }

    private String extractSenderName(Email email) {
        if (email == null) {
            return "";
        }

        String displayName = cleanDisplayName(email.getSenderName());
        if (hasText(displayName)) {
            return displayName;
        }

        String senderEmail = hasText(email.getSenderEmail()) ? email.getSenderEmail() : extractEmail(email.getSender());
        if (hasText(senderEmail)) {
            return cleanEmailLocalPart(senderEmail);
        }

        return cleanDisplayName(email.getSender());
    }

    private String extractEmail(String value) {
        if (!hasText(value)) {
            return "";
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(value);
        return matcher.find() ? matcher.group() : "";
    }

    private String cleanDisplayName(String value) {
        if (!hasText(value)) {
            return "";
        }
        String trimmed = value.trim();
        String displayName = trimmed;
        int emailStart = trimmed.indexOf('<');
        if (emailStart > 0) {
            displayName = trimmed.substring(0, emailStart).replace("\"", "").trim();
        } else if (trimmed.contains("@")) {
            return cleanEmailLocalPart(trimmed);
        }

        return displayName
                .replace("\"", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String cleanEmailLocalPart(String email) {
        if (!hasText(email) || !email.contains("@")) {
            return "";
        }
        String localPart = email.trim().split("@")[0];
        String cleaned = localPart
                .replaceAll("\\d+", " ")
                .replaceAll("[._+-]+", " ")
                .replaceAll("[^A-Za-z\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (cleaned.isBlank()) {
            return "";
        }

        String[] parts = cleaned.split("\\s+");
        String firstName = splitMergedLastName(parts[0]);
        return Character.toUpperCase(firstName.charAt(0)) + firstName.substring(1).toLowerCase();
    }

    private String splitMergedLastName(String name) {
        String lower = name.toLowerCase();
        String[] likelyMergedLastNames = {"jain", "shah", "patel", "singh", "kumar", "gupta", "sharma"};
        for (String lastName : likelyMergedLastNames) {
            if (lower.endsWith(lastName) && lower.length() > lastName.length() + 2) {
                return lower.substring(0, lower.length() - lastName.length());
            }
        }
        return lower;
    }
}

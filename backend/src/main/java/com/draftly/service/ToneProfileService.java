package com.draftly.service;

import com.draftly.dto.UserToneProfileResponse;
import com.draftly.entity.Draft;
import com.draftly.entity.User;
import com.draftly.entity.UserToneProfile;
import com.draftly.enums.EmailCategory;
import com.draftly.exception.ResourceNotFoundException;
import com.draftly.repository.UserToneProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ToneProfileService {

    private static final List<String> TRACKED_PHRASES = List.of(
            "Thank you for following up",
            "I appreciate your patience",
            "Please let me know",
            "I will confirm",
            "Thank you for understanding",
            "I am reviewing",
            "I will get back to you"
    );
    private static final Pattern WORD_PATTERN = Pattern.compile("\\b[\\p{L}\\p{N}']+\\b");

    private final UserToneProfileRepository toneProfileRepository;

    @Transactional(readOnly = true)
    public Optional<UserToneProfile> findByUserEmail(String email) {
        return toneProfileRepository.findByUserEmail(email);
    }

    @Transactional(readOnly = true)
    public UserToneProfileResponse getProfile(String email) {
        return getProfileEntity(email)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Tone profile not found for user: " + email));
    }

    @Transactional(readOnly = true)
    public Optional<UserToneProfile> getProfileEntity(String email) {
        return toneProfileRepository.findByUserEmail(email);
    }

    @Transactional
    public UserToneProfile getOrCreateProfile(String userEmail) {
        return toneProfileRepository.findByUserEmail(userEmail)
                .orElseGet(() -> toneProfileRepository.save(UserToneProfile.builder()
                        .userEmail(userEmail)
                        .approvedDraftCount(0)
                        .averageReplyLength(0)
                        .preferredTone("PROFESSIONAL")
                        .preferredGreeting("Hi")
                        .preferredSignOff("Best regards")
                        .category(EmailCategory.OTHER)
                        .build()));
    }

    @Transactional
    public void resetProfile(String email) {
        toneProfileRepository.deleteByUserEmail(email);
    }

    @Transactional
    public UserToneProfile learnFromApprovedDraft(Draft draft) {
        return updateFromApprovedDraft(draft.getUser(), draft.getContent());
    }

    @Transactional
    public UserToneProfile updateFromApprovedDraft(String userEmail, String draftContent) {
        UserToneProfile profile = getOrCreateProfile(userEmail);
        return updateProfileSignals(profile, draftContent);
    }

    @Transactional
    public UserToneProfile updateFromApprovedDraft(User user, String draftContent) {
        UserToneProfile profile = toneProfileRepository.findByUserEmail(user.getEmail())
                .orElseGet(() -> UserToneProfile.builder()
                        .user(user)
                        .userEmail(user.getEmail())
                        .category(EmailCategory.OTHER)
                        .approvedDraftCount(0)
                        .averageReplyLength(0)
                        .build());
        profile.setUser(user);
        profile.setUserEmail(user.getEmail());
        return updateProfileSignals(profile, draftContent);
    }

    private UserToneProfile updateProfileSignals(UserToneProfile profile, String content) {
        int previousCount = safeCount(profile.getApprovedDraftCount());
        int wordCount = countWords(content);
        int previousAverage = profile.getAverageReplyLength() == null ? 0 : profile.getAverageReplyLength();
        int newAverage = previousCount == 0
                ? wordCount
                : Math.round(((previousAverage * previousCount) + wordCount) / (float) (previousCount + 1));

        profile.setAverageReplyLength(newAverage);
        profile.setApprovedDraftCount(previousCount + 1);
        profile.setPreferredTone(detectPreferredTone(content, wordCount));

        String greeting = detectGreeting(content);
        if (StringUtils.hasText(greeting)) {
            profile.setPreferredGreeting(greeting);
        }

        String signOff = detectSignOff(content);
        if (StringUtils.hasText(signOff)) {
            profile.setPreferredSignOff(signOff);
        }

        profile.setCommonPhrases(mergeCommonPhrases(profile.getCommonPhrases(), content));
        profile.setUsesContractions(content != null && content.matches("(?is).*\\b(I'm|I'll|I've|I'd|can't|won't|don't)\\b.*"));
        return toneProfileRepository.save(profile);
    }

    public String buildGenerationContext(UserToneProfile profile) {
        if (profile == null) {
            return """
                    User writing preferences:
                    - No learned tone profile yet. Use a professional concise default.
                    """;
        }

        return """
                User writing preferences:
                - Preferred tone: %s
                - Preferred greeting: %s
                - Preferred sign-off: %s
                - Average reply length: %s words
                - Common phrases: %s

                Rules:
                - Follow the user's writing style if available.
                - Do not copy phrases unnaturally.
                - Keep the reply relevant to the email intent.
                - Do not ignore the email intent/action items.
                - Do not invent decisions, commitments, dates, money, availability, or facts.
                """.formatted(
                valueOrDefault(profile.getPreferredTone(), "PROFESSIONAL"),
                valueOrDefault(profile.getPreferredGreeting(), "Hi"),
                valueOrDefault(profile.getPreferredSignOff(), "Best regards"),
                profile.getAverageReplyLength() == null ? "unknown" : profile.getAverageReplyLength(),
                valueOrDefault(profile.getCommonPhrases(), "none")
        );
    }

    @Transactional(readOnly = true)
    public String buildToneContext(String userEmail) {
        return buildGenerationContext(toneProfileRepository.findByUserEmail(userEmail).orElse(null));
    }

    public UserToneProfileResponse toResponse(UserToneProfile profile) {
        return UserToneProfileResponse.builder()
                .userEmail(valueOrDefault(profile.getUserEmail(), profile.getUser() == null ? null : profile.getUser().getEmail()))
                .preferredTone(profile.getPreferredTone())
                .preferredGreeting(profile.getPreferredGreeting())
                .preferredSignOff(profile.getPreferredSignOff())
                .averageReplyLength(profile.getAverageReplyLength())
                .approvedDraftCount(profile.getApprovedDraftCount())
                .commonPhrases(profile.getCommonPhrases())
                .build();
    }

    private int countWords(String content) {
        if (!StringUtils.hasText(content)) {
            return 0;
        }

        Matcher matcher = WORD_PATTERN.matcher(content);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private String detectGreeting(String content) {
        for (String line : lines(content)) {
            String trimmed = stripPunctuation(line);
            String lower = trimmed.toLowerCase(Locale.ROOT);
            if (lower.startsWith("hi ")) {
                return "Hi";
            }
            if (lower.equals("hi")) {
                return "Hi";
            }
            if (lower.startsWith("hello ")) {
                return "Hello";
            }
            if (lower.equals("hello")) {
                return "Hello";
            }
            if (lower.startsWith("dear ")) {
                return "Dear";
            }
        }
        return null;
    }

    private String detectSignOff(String content) {
        List<String> lines = lines(content);
        for (int i = lines.size() - 1; i >= 0; i--) {
            String trimmed = stripPunctuation(lines.get(i));
            String lower = trimmed.toLowerCase(Locale.ROOT);
            if (lower.equals("best regards")) {
                return "Best regards";
            }
            if (lower.equals("regards")) {
                return "Regards";
            }
            if (lower.equals("thank you")) {
                return "Thank you";
            }
            if (lower.equals("sincerely")) {
                return "Sincerely";
            }
            if (lower.equals("best")) {
                return "Best";
            }
        }
        return null;
    }

    private String detectPreferredTone(String content, int wordCount) {
        String lower = content == null ? "" : content.toLowerCase(Locale.ROOT);
        if (lower.contains("dear ") && lower.contains("sincerely")) {
            return "FORMAL";
        }
        if (lower.contains("i appreciate") || lower.contains("thank you") || lower.contains("kindly")) {
            return "POLITE_PROFESSIONAL";
        }
        if (wordCount > 0 && wordCount <= 55 && (lower.contains("please") || lower.contains("thank"))) {
            return "CONCISE_PROFESSIONAL";
        }
        return "PROFESSIONAL";
    }

    private String mergeCommonPhrases(String existing, String content) {
        Set<String> phrases = new LinkedHashSet<>();
        if (StringUtils.hasText(existing)) {
            for (String phrase : existing.split(";")) {
                if (StringUtils.hasText(phrase)) {
                    phrases.add(phrase.trim());
                }
            }
        }

        String lower = content == null ? "" : content.toLowerCase(Locale.ROOT);
        for (String phrase : TRACKED_PHRASES) {
            if (lower.contains(phrase.toLowerCase(Locale.ROOT))) {
                phrases.add(phrase);
            }
        }

        return String.join("; ", phrases);
    }

    private List<String> lines(String content) {
        if (!StringUtils.hasText(content)) {
            return List.of();
        }
        return content.lines()
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

    private String stripPunctuation(String value) {
        return value == null ? "" : value.trim().replaceAll("[,.;:]+$", "").trim();
    }

    private int safeCount(Integer count) {
        return count == null ? 0 : count;
    }

    private String valueOrDefault(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }
}

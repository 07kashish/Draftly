package com.draftly.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class EmailActionItemService {

    public String detectActionContext(String subject, String body, String threadHistory) {
        String text = combine(subject, body, threadHistory);
        List<String> questions = new ArrayList<>();
        List<String> actions = new ArrayList<>();
        List<String> needs = new ArrayList<>();

        if (containsAny(text, "let me know when", "when you are ready", "are you ready")) {
            questions.add("sender asks when the user will be ready");
            actions.add("address readiness without claiming the user is already ready");
            needs.add("readiness");
        }
        if (containsAny(text, "when can we", "can we have a meeting", "have a meeting", "schedule a meeting", "share your availability", "availability", "calendar")) {
            questions.add("sender asks about meeting scheduling");
            actions.add("ask for available slots or say the user will confirm a suitable time shortly");
            needs.add("availability");
        }
        if (containsAny(text, "please confirm", "can you confirm")) {
            questions.add("sender asks for confirmation");
            actions.add("confirm only if the needed information is known");
            needs.add("confirmation");
        }
        if (containsAny(text, "what is your decision", "final decision", "decision on it", "do you accept", "do you agree", "should we proceed")) {
            questions.add("sender asks for a decision");
            actions.add("do not invent acceptance, rejection, agreement, or a commitment");
            needs.add("decision");
        }
        if (containsAny(text, "send me", "can you provide")) {
            questions.add("sender requests information or a document");
            actions.add("acknowledge the requested item and say the user will share it when available");
            needs.add("follow-up");
        }

        if (questions.isEmpty() && actions.isEmpty()) {
            return "Detected action items: none explicit. Identify the sender's main intent from the email and answer it directly.";
        }

        return """
                Detected action items:
                - Questions/requests: %s
                - Reply needs: %s
                - Required handling: %s
                """.formatted(
                String.join("; ", questions),
                String.join(", ", distinct(needs)),
                String.join("; ", distinct(actions))
        ).trim();
    }

    private String combine(String subject, String body, String threadHistory) {
        return ((subject == null ? "" : subject) + " "
                + (body == null ? "" : body) + " "
                + (threadHistory == null ? "" : threadHistory))
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
    }

    private boolean containsAny(String text, String... phrases) {
        for (String phrase : phrases) {
            if (StringUtils.hasText(phrase) && text.contains(phrase)) {
                return true;
            }
        }
        return false;
    }

    private List<String> distinct(List<String> values) {
        return values.stream().distinct().toList();
    }
}

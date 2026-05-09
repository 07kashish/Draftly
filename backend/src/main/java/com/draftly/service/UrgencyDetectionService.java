package com.draftly.service;

import com.draftly.enums.EmailUrgency;
import org.springframework.stereotype.Service;

@Service
public class UrgencyDetectionService {

    public EmailUrgency detect(String subject, String body) {
        String text = ((subject == null ? "" : subject) + " " + (body == null ? "" : body)).toLowerCase();

        if (containsAny(text,
                "urgent",
                "asap",
                "immediately",
                "deadline",
                "today",
                "critical",
                "emergency",
                "as soon as possible")) {
            return EmailUrgency.HIGH;
        }
        if (containsAny(text,
                "waiting on your reply",
                "decision",
                "acceptance",
                "confirm",
                "can we",
                "could we",
                "can you",
                "could you",
                "please",
                "schedule",
                "meeting",
                "call",
                "available",
                "tomorrow",
                "follow up",
                "respond",
                "reply",
                "request",
                "need your input")) {
            return EmailUrgency.MEDIUM;
        }
        return EmailUrgency.LOW;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}

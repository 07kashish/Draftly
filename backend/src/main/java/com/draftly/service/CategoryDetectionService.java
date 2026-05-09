package com.draftly.service;

import com.draftly.enums.EmailCategory;
import org.springframework.stereotype.Service;

@Service
public class CategoryDetectionService {

    public EmailCategory detect(String subject, String body) {
        String text = combine(subject, body);

        if (containsAny(text,
                "what is your decision",
                "waiting on your reply",
                "confirm your decision",
                "offer acceptance",
                "do you accept",
                "would you like to proceed",
                "final decision",
                "decision on it")
                || (text.contains("acceptance") && containsAny(text, "decision", "reply", "confirm"))) {
            return EmailCategory.DECISION_REQUEST;
        }
        if (containsAny(text,
                "meeting",
                "schedule",
                "call",
                "calendar",
                "available",
                "availability",
                "when can we",
                "can we have a meeting",
                "share your availability")) {
            return EmailCategory.MEETING_REQUEST;
        }
        if (containsAny(text,
                "please confirm",
                "can you confirm",
                "let us know",
                "let me know when",
                "are you available",
                "when you are ready",
                "should we proceed",
                "do you agree")) {
            return EmailCategory.CONFIRMATION_REQUEST;
        }
        if (containsAny(text, "follow up", "checking in", "reminder")) {
            return EmailCategory.FOLLOW_UP;
        }
        if (containsAny(text, "issue", "problem", "unhappy", "complaint", "disappointed")) {
            return EmailCategory.COMPLAINT;
        }
        if (containsAny(text, "fyi", "information", "update", "sharing")) {
            return EmailCategory.INFORMATIONAL;
        }
        if (containsAny(text, "friend", "family", "casual", "personal")) {
            return EmailCategory.PERSONAL;
        }
        if (text.length() > 0) {
            return EmailCategory.PROFESSIONAL;
        }
        return EmailCategory.OTHER;
    }

    private String combine(String subject, String body) {
        return ((subject == null ? "" : subject) + " " + (body == null ? "" : body)).trim().toLowerCase();
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

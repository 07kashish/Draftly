package com.draftly.service;

import com.draftly.enums.EmailTone;
import org.springframework.stereotype.Service;

@Service
public class ToneDetectionService {

    public EmailTone detect(String subject, String body) {
        String text = ((subject == null ? "" : subject) + " " + (body == null ? "" : body)).toLowerCase();

        if (containsAny(text, "complaint", "issue", "problem", "unhappy", "disappointed")) {
            return EmailTone.APOLOGETIC;
        }
        if (containsAny(text, "sir", "madam", "official", "kindly")) {
            return EmailTone.FORMAL;
        }
        if (containsAny(text, "thanks", "hope you are well", "appreciate")) {
            return EmailTone.FRIENDLY;
        }
        if (text.length() < 120) {
            return EmailTone.CONCISE;
        }
        return EmailTone.PROFESSIONAL;
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

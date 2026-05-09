package com.draftly.service;

import com.draftly.enums.EmailCategory;
import com.draftly.enums.EmailUrgency;
import org.springframework.stereotype.Service;

@Service
public class ReplyStrategyService {

    public String createStrategy(EmailCategory category, EmailUrgency urgency) {
        return createStrategy(category, urgency, "");
    }

    public String createStrategy(EmailCategory category, EmailUrgency urgency, String actionContext) {
        String urgencyInstruction = switch (urgency) {
            case HIGH -> "Respond quickly, acknowledge urgency, and give a clear next step.";
            case MEDIUM -> "Answer the request clearly and suggest the next step.";
            case LOW -> "Keep the reply helpful, polite, and concise.";
        };

        String categoryInstruction = switch (category) {
            case DECISION_REQUEST -> "Sender's main intent: decision request. Questions that must be answered: the requested decision or next step. Do not invent acceptance or rejection. If the user's actual decision is unknown, say they are reviewing it and will confirm shortly.";
            case CONFIRMATION_REQUEST -> "Sender's main intent: confirmation request. Questions that must be answered: what can be confirmed and what still needs checking. Confirm only when enough information exists; otherwise say the user will confirm shortly or ask for details.";
            case MEETING_REQUEST -> "Sender's main intent: meeting scheduling. Questions that must be answered: readiness, availability, and meeting timing when mentioned. Acknowledge the sender, address readiness if mentioned, ask for availability or say the user will confirm a suitable time shortly. Do not invent a specific date or time unless user context includes it. Do not give a generic acknowledgement only.";
            case FOLLOW_UP -> "Acknowledge the follow-up and provide a useful status update.";
            case COMPLAINT -> "Show empathy, apologize where appropriate, and offer a resolution path.";
            case INFORMATIONAL -> "Thank the sender and acknowledge the update.";
            case PERSONAL -> "Reply warmly while staying clear and respectful.";
            case PROFESSIONAL -> "Maintain a professional tone and answer the main point.";
            case OTHER -> "Acknowledge the message and ask for clarification if needed.";
        };

        String normalizedActionContext = actionContext == null || actionContext.isBlank() ? "" : " " + actionContext;
        return categoryInstruction + " " + urgencyInstruction + normalizedActionContext;
    }
}

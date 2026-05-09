package com.draftly.service;

import com.draftly.entity.Email;
import com.draftly.entity.User;
import com.draftly.enums.EmailCategory;
import com.draftly.enums.EmailTone;
import com.draftly.enums.EmailUrgency;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DraftGenerationServiceTest {

    private final LlmService failingLlmService = (userName, sender, senderName, senderEmail, subject, body, threadHistory, category, tone, urgency, strategy, userToneProfileContext) -> {
        throw new RuntimeException("LLM unavailable");
    };

    private final LlmService leakingAnalysisLlmService = (userName, sender, senderName, senderEmail, subject, body, threadHistory, category, tone, urgency, strategy, userToneProfileContext) -> """
            'sender' is elixirjain@gmail.com -> clean name: Elixir
            User name: Kashish Jain
            Sender's main intent: meeting scheduling
            Questions to answer: readiness, availability, meeting timing
            Reply strategy: acknowledge, address readiness without claiming readiness, ask for availability or confirm shortly

            Hi Elixir,

            I'm reviewing the university response and will let you know when I'm ready. Could you share your availability for a meeting in the coming days? I'll confirm a suitable time shortly.

            Best regards,
            Kashish Jain
            """;

    @Test
    void draftGenerationReturnsFallbackDraftWhenLlmFails() {
        DraftGenerationService service = new DraftGenerationService(failingLlmService);

        User user = User.builder()
                .name("Kashish Jain")
                .email("kashish@example.com")
                .build();
        Email email = Email.builder()
                .sender("client@example.com")
                .subject("Meeting follow up")
                .body("Can we schedule a meeting tomorrow?")
                .threadHistory("")
                .category(EmailCategory.MEETING_REQUEST)
                .tone(EmailTone.CONCISE)
                .urgency(EmailUrgency.MEDIUM)
                .build();

        String draft = service.generateDraft(user, email, "Confirm availability.");

        assertThat(draft).isNotBlank();
        assertThat(draft).contains("Kashish Jain");
    }

    @Test
    void decisionRequestFallbackAddressesDecisionWithoutInventingAnswer() {
        DraftGenerationService service = new DraftGenerationService(failingLlmService);

        User user = User.builder()
                .name("Kashish Jain")
                .email("kashish@example.com")
                .build();
        Email email = Email.builder()
                .sender("elixirjain@gmail.com")
                .subject("Update on university reply")
                .body("Dear Kashish, We have been waiting on your reply for university acceptance what is your decision on it. Best regards, Elixir")
                .threadHistory("")
                .category(EmailCategory.DECISION_REQUEST)
                .tone(EmailTone.PROFESSIONAL)
                .urgency(EmailUrgency.MEDIUM)
                .build();

        String draft = service.generateDraft(user, email, "Address the requested decision.");

        assertThat(draft).contains("Hi Elixir,");
        assertThat(draft).contains("decision", "reviewing", "confirm", "shortly");
        assertThat(draft).doesNotContain("I've noted the information");
        assertThat(draft).doesNotContain("I accept", "I decline");
    }

    @Test
    void fallbackGreetingUsesSenderDisplayNameBeforeEmail() {
        DraftGenerationService service = new DraftGenerationService(failingLlmService);

        User user = User.builder().name("Kashish Jain").email("kashish@example.com").build();
        Email email = Email.builder()
                .sender("Kashish Jain <elixirjain@gmail.com>")
                .senderName("Kashish Jain")
                .senderEmail("elixirjain@gmail.com")
                .subject("Meeting request")
                .body("Can we schedule a meeting?")
                .category(EmailCategory.MEETING_REQUEST)
                .tone(EmailTone.PROFESSIONAL)
                .urgency(EmailUrgency.MEDIUM)
                .build();

        String draft = service.generateDraft(user, email, "Address meeting scheduling.");

        assertThat(draft).startsWith("Hi Kashish Jain,");
        assertThat(draft).doesNotStartWith("Hi Elixir,");
    }

    @Test
    void fallbackGreetingUsesEmailLocalPartWhenDisplayNameMissing() {
        DraftGenerationService service = new DraftGenerationService(failingLlmService);

        User user = User.builder().name("Kashish Jain").email("kashish@example.com").build();
        Email email = Email.builder()
                .sender("elixirjain@gmail.com")
                .senderEmail("elixirjain@gmail.com")
                .subject("Meeting request")
                .body("Can we schedule a meeting?")
                .category(EmailCategory.MEETING_REQUEST)
                .tone(EmailTone.PROFESSIONAL)
                .urgency(EmailUrgency.MEDIUM)
                .build();

        String draft = service.generateDraft(user, email, "Address meeting scheduling.");

        assertThat(draft).startsWith("Hi Elixir,");
    }

    @Test
    void fallbackGreetingOmitsNameWhenSenderNameAndEmailMissing() {
        DraftGenerationService service = new DraftGenerationService(failingLlmService);

        User user = User.builder().name("Kashish Jain").email("kashish@example.com").build();
        Email email = Email.builder()
                .sender("")
                .subject("Meeting request")
                .body("Can we schedule a meeting?")
                .category(EmailCategory.MEETING_REQUEST)
                .tone(EmailTone.PROFESSIONAL)
                .urgency(EmailUrgency.MEDIUM)
                .build();

        String draft = service.generateDraft(user, email, "Address meeting scheduling.");

        assertThat(draft).startsWith("Hi,");
    }

    @Test
    void informationalFallbackCanAcknowledgeUpdate() {
        DraftGenerationService service = new DraftGenerationService(failingLlmService);

        User user = User.builder().name("Kashish Jain").email("kashish@example.com").build();
        Email email = Email.builder()
                .sender("updates@example.com")
                .subject("Project update")
                .body("FYI, sharing the latest information.")
                .category(EmailCategory.INFORMATIONAL)
                .tone(EmailTone.CONCISE)
                .urgency(EmailUrgency.LOW)
                .build();

        String draft = service.generateDraft(user, email, "Thank the sender and acknowledge the update.");

        assertThat(draft).contains("Thank you for sharing this update");
    }

    @Test
    void meetingRequestFallbackAddressesScheduling() {
        DraftGenerationService service = new DraftGenerationService(failingLlmService);

        User user = User.builder().name("Kashish Jain").email("kashish@example.com").build();
        Email email = Email.builder()
                .sender("client@example.com")
                .subject("Meeting request")
                .body("Can we schedule a meeting tomorrow?")
                .category(EmailCategory.MEETING_REQUEST)
                .tone(EmailTone.CONCISE)
                .urgency(EmailUrgency.MEDIUM)
                .build();

        String draft = service.generateDraft(user, email, "Confirm availability and suggest scheduling details.");

        assertThat(draft).contains("schedule a meeting");
    }

    @Test
    void complaintFallbackAcknowledgesIssue() {
        DraftGenerationService service = new DraftGenerationService(failingLlmService);

        User user = User.builder().name("Kashish Jain").email("kashish@example.com").build();
        Email email = Email.builder()
                .sender("client@example.com")
                .subject("Complaint")
                .body("I am unhappy and disappointed with this issue.")
                .category(EmailCategory.COMPLAINT)
                .tone(EmailTone.APOLOGETIC)
                .urgency(EmailUrgency.MEDIUM)
                .build();

        String draft = service.generateDraft(user, email, "Show empathy and offer a resolution path.");

        assertThat(draft).contains("sorry", "concern");
    }

    @Test
    void draftGenerationStripsLeakedLlmAnalysisBeforeEmailBody() {
        DraftGenerationService service = new DraftGenerationService(leakingAnalysisLlmService);

        User user = User.builder().name("Kashish Jain").email("kashish@example.com").build();
        Email email = Email.builder()
                .sender("elixirjain@gmail.com")
                .subject("Meeting request")
                .body("Let me know when you are ready so we can schedule a meeting.")
                .category(EmailCategory.MEETING_REQUEST)
                .tone(EmailTone.PROFESSIONAL)
                .urgency(EmailUrgency.MEDIUM)
                .build();

        String draft = service.generateDraft(user, email, "Address readiness and meeting availability.");

        assertThat(draft).startsWith("Hi Elixir,");
        assertThat(draft).doesNotContain("Sender's main intent");
        assertThat(draft).doesNotContain("Questions to answer");
        assertThat(draft).doesNotContain("Reply strategy");
        assertThat(draft).contains("Best regards,", "Kashish Jain");
    }
}

package com.draftly.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.draftly.repository.EmailRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EmailControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmailRepository emailRepository;

    @Test
    void analyzeEmailReturnsExpectedResponseFields() throws Exception {
        String requestBody = """
                {
                  "userName": "Kashish Jain",
                  "userEmail": "kashish@example.com",
                  "sender": "client@example.com",
                  "recipients": ["kashish@example.com"],
                  "subject": "Meeting follow up",
                  "body": "Hi Kashish, can we schedule a meeting tomorrow?",
                  "threadHistory": "We had previously discussed the project timeline."
                }
                """;

        mockMvc.perform(post("/api/emails/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.emailId", not(nullValue())))
                .andExpect(jsonPath("$.draftId", not(nullValue())))
                .andExpect(jsonPath("$.category", not(nullValue())))
                .andExpect(jsonPath("$.tone", not(nullValue())))
                .andExpect(jsonPath("$.urgency", not(nullValue())))
                .andExpect(jsonPath("$.strategy", not(nullValue())))
                .andExpect(jsonPath("$.draft", not(nullValue())));
    }

    @Test
    void analyzeEmailAcceptsOptionalGmailThreadMetadata() throws Exception {
        String requestBody = """
                {
                  "userName": "Kashish Jain",
                  "userEmail": "gmail-metadata@example.com",
                  "gmailMessageId": "msg-api-id-123",
                  "gmailThreadId": "thread-api-id-456",
                  "sender": "client@example.com",
                  "recipients": ["gmail-metadata@example.com"],
                  "subject": "Threaded reply",
                  "body": "Please reply in this thread.",
                  "threadHistory": ""
                }
                """;

        String content = mockMvc.perform(post("/api/emails/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.emailId", not(nullValue())))
                .andExpect(jsonPath("$.draftId", not(nullValue())))
                .andExpect(jsonPath("$.draft", not(nullValue())))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(content);
        com.draftly.entity.Email email = emailRepository.findById(java.util.UUID.fromString(json.get("emailId").asText()))
                .orElseThrow();
        org.assertj.core.api.Assertions.assertThat(email.getGmailMessageId()).isEqualTo("msg-api-id-123");
        org.assertj.core.api.Assertions.assertThat(email.getGmailThreadId()).isEqualTo("thread-api-id-456");
    }

    @Test
    void composeEmailWithValidPromptReturnsSubjectAndDraft() throws Exception {
        String requestBody = """
                {
                  "userName": "Kashish Jain",
                  "userEmail": "compose-valid@example.com",
                  "recipient": "admissions@example.com",
                  "prompt": "Write an email asking for an extension in the deposit deadline because the deposit amount is high and I need more time to arrange it.",
                  "tone": "PROFESSIONAL",
                  "context": "I received an offer for MSc Engineering Management.",
                  "desiredLength": "MEDIUM"
                }
                """;

        mockMvc.perform(post("/api/emails/compose")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subject", not(nullValue())))
                .andExpect(jsonPath("$.subject", containsString("Deposit")))
                .andExpect(jsonPath("$.draft", not(nullValue())))
                .andExpect(jsonPath("$.draft", containsString("extension")))
                .andExpect(jsonPath("$.tone").value("PROFESSIONAL"))
                .andExpect(jsonPath("$.strategy", not(nullValue())));
    }

    @Test
    void composeEmailForDeadlineExtensionIsPolishedAndSpecific() throws Exception {
        String requestBody = """
                {
                  "userName": "Kashish Jain",
                  "userEmail": "compose-deadline@example.com",
                  "prompt": "Write an email for extending deadline.",
                  "tone": "PROFESSIONAL",
                  "desiredLength": "MEDIUM"
                }
                """;

        String content = mockMvc.perform(post("/api/emails/compose")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(content);
        String subject = json.get("subject").asText();
        String draft = json.get("draft").asText();
        String combined = (subject + " " + draft).toLowerCase();

        org.assertj.core.api.Assertions.assertThat(combined).contains("deadline");
        org.assertj.core.api.Assertions.assertThat(combined).contains("extension");
        org.assertj.core.api.Assertions.assertThat(draft.toLowerCase())
                .containsAnyOf("request an extension", "request additional time", "additional time");
        org.assertj.core.api.Assertions.assertThat(draft).doesNotContain("writing to for");
        org.assertj.core.api.Assertions.assertThat(draft).contains("Dear");
        org.assertj.core.api.Assertions.assertThat(draft).contains("Best regards,");
        org.assertj.core.api.Assertions.assertThat(draft.lines().filter(line -> !line.isBlank()).count()).isGreaterThanOrEqualTo(4);
    }

    @Test
    void composeEmailForNtuDepositDeadlineIncludesProvidedDetails() throws Exception {
        String requestBody = """
                {
                  "userName": "Kashish Jain",
                  "userEmail": "compose-ntu@example.com",
                  "recipient": "admissions@ntu.ac.uk",
                  "prompt": "Write an email to NTU asking for extension in deposit deadline because the deposit amount is high and I need more time to arrange it.",
                  "tone": "POLITE",
                  "desiredLength": "MEDIUM"
                }
                """;

        String content = mockMvc.perform(post("/api/emails/compose")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(content);
        String subject = json.get("subject").asText().toLowerCase();
        String draft = json.get("draft").asText().toLowerCase();
        String combined = subject + " " + draft;

        org.assertj.core.api.Assertions.assertThat(subject).contains("deposit").contains("deadline");
        org.assertj.core.api.Assertions.assertThat(combined).contains("extension");
        org.assertj.core.api.Assertions.assertThat(combined).contains("deposit");
        org.assertj.core.api.Assertions.assertThat(combined).contains("deadline");
        org.assertj.core.api.Assertions.assertThat(combined).containsAnyOf("more time", "additional time");
        org.assertj.core.api.Assertions.assertThat(draft).doesNotContain("writing to for");
        org.assertj.core.api.Assertions.assertThat(draft).contains("thank");
    }

    @Test
    void composeEmailWithBlankPromptReturnsValidationError() throws Exception {
        String requestBody = """
                {
                  "userEmail": "compose-blank@example.com",
                  "prompt": " "
                }
                """;

        mockMvc.perform(post("/api/emails/compose")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    void composeEmailUsesToneProfileInFallback() throws Exception {
        String email = "compose-tone@example.com";
        String draftId = generateDraft(email, "Formal reply", "Please acknowledge.");
        approveEditedDraft(draftId, """
                Dear Team,

                Thank you for sharing this update.

                Sincerely,
                Kashish Jain
                """);

        String requestBody = """
                {
                  "userName": "Kashish Jain",
                  "userEmail": "%s",
                  "recipient": "team@example.com",
                  "prompt": "Write an email following up on the project timeline.",
                  "desiredLength": "SHORT"
                }
                """.formatted(email);

        mockMvc.perform(post("/api/emails/compose")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.draft", containsString("Dear Team,")))
                .andExpect(jsonPath("$.draft", containsString("Sincerely,")));
    }

    @Test
    void analyzeUniversityDecisionEmailReturnsUsefulDecisionDraft() throws Exception {
        String requestBody = """
                {
                  "userName": "Kashish Jain",
                  "userEmail": "kashish@example.com",
                  "sender": "elixirjain@gmail.com",
                  "recipients": ["kashish@example.com"],
                  "subject": "Update on university reply",
                  "body": "Dear Kashish, We have been waiting on your reply for university acceptance what is your decision on it. Best regards, Elixir",
                  "threadHistory": ""
                }
                """;

        mockMvc.perform(post("/api/emails/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.emailId", not(nullValue())))
                .andExpect(jsonPath("$.draftId", not(nullValue())))
                .andExpect(jsonPath("$.category").value("DECISION_REQUEST"))
                .andExpect(jsonPath("$.urgency").value("MEDIUM"))
                .andExpect(jsonPath("$.strategy", containsString("decision")))
                .andExpect(jsonPath("$.draft", containsString("decision")))
                .andExpect(jsonPath("$.draft", containsString("reviewing")))
                .andExpect(jsonPath("$.draft", containsString("confirm")))
                .andExpect(jsonPath("$.draft", not(containsString("I've noted the information"))))
                .andExpect(jsonPath("$.draft", not(containsString("I accept"))))
                .andExpect(jsonPath("$.draft", not(containsString("I decline"))));
    }

    @Test
    void meetingReadinessEmailProducesSpecificMeetingDraft() throws Exception {
        String requestBody = """
                {
                  "userName": "Kashish Jain",
                  "userEmail": "meeting-readiness@example.com",
                  "sender": "elixirjain@gmail.com",
                  "recipients": ["meeting-readiness@example.com"],
                  "subject": "Meeting follow up",
                  "body": "hi kashish, i totally understand it. let me know when you are ready and also when can we have a meeting to go on with let me know. best regards elixir",
                  "threadHistory": ""
                }
                """;

        mockMvc.perform(post("/api/emails/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.category").value("MEETING_REQUEST"))
                .andExpect(jsonPath("$.urgency", not(nullValue())))
                .andExpect(jsonPath("$.strategy", containsString("meeting")))
                .andExpect(jsonPath("$.strategy", containsString("availability")))
                .andExpect(jsonPath("$.strategy", containsString("readiness")))
                .andExpect(jsonPath("$.draft", containsString("meeting")))
                .andExpect(jsonPath("$.draft", containsString("availability")))
                .andExpect(jsonPath("$.draft", containsString("suitable time")))
                .andExpect(jsonPath("$.draft", not(containsString("Thank you for reaching out. I'd be happy to schedule a meeting."))))
                .andExpect(jsonPath("$.draft", not(containsString("tomorrow"))))
                .andExpect(jsonPath("$.draft", not(containsString("Monday"))));
    }

    @Test
    void conciseUniversityDecisionRequestDoesNotInventDecision() throws Exception {
        String requestBody = """
                {
                  "userName": "Kashish Jain",
                  "userEmail": "decision-short@example.com",
                  "sender": "admissions@example.com",
                  "recipients": ["decision-short@example.com"],
                  "subject": "University acceptance",
                  "body": "Please let me know your final decision on the university acceptance.",
                  "threadHistory": ""
                }
                """;

        mockMvc.perform(post("/api/emails/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.category").value("DECISION_REQUEST"))
                .andExpect(jsonPath("$.draft", containsString("decision")))
                .andExpect(jsonPath("$.draft", containsString("reviewing")))
                .andExpect(jsonPath("$.draft", containsString("confirm")))
                .andExpect(jsonPath("$.draft", not(containsString("I accept"))))
                .andExpect(jsonPath("$.draft", not(containsString("I decline"))));
    }

    @Test
    void approvingEditedDraftCreatesToneProfileFromFinalContent() throws Exception {
        String email = "tone-create@example.com";
        String draftId = generateDraft(email, "Project follow up", "Please confirm the project timeline.");
        String editedDraft = """
                Hi Elixir,

                Thank you for following up. I appreciate your patience and will confirm the project timeline shortly. Please let me know if there is anything else you need.

                Best regards,
                Kashish Jain
                """;

        mockMvc.perform(patch("/api/drafts/{draftId}", draftId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "%s"
                                }
                                """.formatted(escapeJson(editedDraft))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/drafts/{draftId}/approve", draftId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        mockMvc.perform(get("/api/users/{email}/tone-profile", email))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userEmail").value(email))
                .andExpect(jsonPath("$.preferredTone").value("POLITE_PROFESSIONAL"))
                .andExpect(jsonPath("$.preferredGreeting").value("Hi"))
                .andExpect(jsonPath("$.preferredSignOff").value("Best regards"))
                .andExpect(jsonPath("$.approvedDraftCount").value(1))
                .andExpect(jsonPath("$.averageReplyLength", not(nullValue())))
                .andExpect(jsonPath("$.commonPhrases", containsString("Thank you for following up")))
                .andExpect(jsonPath("$.commonPhrases", containsString("I appreciate your patience")))
                .andExpect(jsonPath("$.commonPhrases", containsString("Please let me know")));
    }

    @Test
    void approvingMultipleDraftsUpdatesRollingAverageLength() throws Exception {
        String email = "tone-average@example.com";
        String firstDraftId = generateDraft(email, "First", "Please confirm.");
        String secondDraftId = generateDraft(email, "Second", "Please confirm again.");

        approveEditedDraft(firstDraftId, """
                Hi Client,

                Thank you.

                Regards,
                Kashish Jain
                """);
        approveEditedDraft(secondDraftId, """
                Hi Client,

                Thank you for following up. I appreciate your patience and will confirm this shortly.

                Regards,
                Kashish Jain
                """);

        mockMvc.perform(get("/api/users/{email}/tone-profile", email))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approvedDraftCount").value(2))
                .andExpect(jsonPath("$.averageReplyLength").value(13));
    }

    @Test
    void analyzeEmailUsesLearnedToneProfileInFallbackDraft() throws Exception {
        String email = "tone-use@example.com";
        String draftId = generateDraft(email, "Formal reply", "Please acknowledge this update.");
        approveEditedDraft(draftId, """
                Dear Team,

                Thank you for sharing this update. I will review it carefully.

                Sincerely,
                Kashish Jain
                """);

        String requestBody = """
                {
                  "userName": "Kashish Jain",
                  "userEmail": "%s",
                  "sender": "client@example.com",
                  "recipients": ["%s"],
                  "subject": "Meeting request",
                  "body": "Can we schedule a meeting tomorrow?",
                  "threadHistory": ""
                }
                """.formatted(email, email);

        mockMvc.perform(post("/api/emails/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.draft", containsString("Dear Client,")))
                .andExpect(jsonPath("$.draft", containsString("Sincerely,")));
    }

    @Test
    void toneProfileCanBeReset() throws Exception {
        String email = "tone-reset@example.com";
        String draftId = generateDraft(email, "Reset", "Please confirm.");
        approveEditedDraft(draftId, """
                Hi Client,

                Thank you for following up.

                Best regards,
                Kashish Jain
                """);

        mockMvc.perform(delete("/api/users/{email}/tone-profile", email))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Tone profile reset successfully"));

        mockMvc.perform(get("/api/users/{email}/tone-profile", email))
                .andExpect(status().isNotFound());
    }

    private String generateDraft(String email, String subject, String body) throws Exception {
        String requestBody = """
                {
                  "userName": "Kashish Jain",
                  "userEmail": "%s",
                  "sender": "client@example.com",
                  "recipients": ["%s"],
                  "subject": "%s",
                  "body": "%s",
                  "threadHistory": ""
                }
                """.formatted(email, email, subject, body);

        String content = mockMvc.perform(post("/api/emails/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode json = objectMapper.readTree(content);
        return json.get("draftId").asText();
    }

    private void approveEditedDraft(String draftId, String editedDraft) throws Exception {
        mockMvc.perform(patch("/api/drafts/{draftId}", draftId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "%s"
                                }
                                """.formatted(escapeJson(editedDraft))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/drafts/{draftId}/approve", draftId))
                .andExpect(status().isOk());
    }

    private String escapeJson(String value) throws Exception {
        String json = objectMapper.writeValueAsString(value);
        return json.substring(1, json.length() - 1);
    }
}

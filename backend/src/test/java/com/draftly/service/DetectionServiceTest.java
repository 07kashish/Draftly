package com.draftly.service;

import com.draftly.enums.EmailCategory;
import com.draftly.enums.EmailTone;
import com.draftly.enums.EmailUrgency;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DetectionServiceTest {

    private final CategoryDetectionService categoryDetectionService = new CategoryDetectionService();
    private final ToneDetectionService toneDetectionService = new ToneDetectionService();
    private final UrgencyDetectionService urgencyDetectionService = new UrgencyDetectionService();

    @Test
    void universityDecisionEmailDetectsDecisionRequestAndMediumUrgency() {
        String subject = "Update on university reply";
        String body = "Dear Kashish, We have been waiting on your reply for university acceptance what is your decision on it. Best regards, Elixir";

        assertThat(categoryDetectionService.detect(subject, body)).isEqualTo(EmailCategory.DECISION_REQUEST);
        assertThat(urgencyDetectionService.detect(subject, body)).isEqualTo(EmailUrgency.MEDIUM);
    }

    @Test
    void confirmationEmailDetectsConfirmationRequest() {
        EmailCategory category = categoryDetectionService.detect(
                "Please confirm",
                "Can you confirm whether we should proceed?"
        );

        assertThat(category).isEqualTo(EmailCategory.CONFIRMATION_REQUEST);
    }

    @Test
    void meetingRequestEmailDetectsMeetingRequest() {
        EmailCategory category = categoryDetectionService.detect(
                "Meeting follow up",
                "Hi, can we schedule a call tomorrow?"
        );

        assertThat(category).isEqualTo(EmailCategory.MEETING_REQUEST);
    }

    @Test
    void complaintEmailDetectsComplaintAndApologeticTone() {
        String body = "I am unhappy and disappointed with this issue.";

        assertThat(categoryDetectionService.detect("Complaint", body)).isEqualTo(EmailCategory.COMPLAINT);
        assertThat(toneDetectionService.detect("Complaint", body)).isEqualTo(EmailTone.APOLOGETIC);
    }

    @Test
    void followUpEmailDetectsFollowUp() {
        EmailCategory category = categoryDetectionService.detect(
                "Checking in",
                "Just following up on the project timeline."
        );

        assertThat(category).isEqualTo(EmailCategory.FOLLOW_UP);
    }

    @Test
    void informationalEmailDetectsInformational() {
        EmailCategory category = categoryDetectionService.detect(
                "Project update",
                "FYI, sharing the latest information about the project."
        );

        assertThat(category).isEqualTo(EmailCategory.INFORMATIONAL);
    }

    @Test
    void emptyAndShortEmailDoesNotCrash() {
        assertThat(categoryDetectionService.detect(null, null)).isEqualTo(EmailCategory.OTHER);
        assertThat(toneDetectionService.detect("", "")).isEqualTo(EmailTone.CONCISE);
        assertThat(urgencyDetectionService.detect("", "")).isEqualTo(EmailUrgency.LOW);
    }

    @Test
    void urgentWordsDetectHighUrgency() {
        EmailUrgency urgency = urgencyDetectionService.detect(
                "Urgent deadline",
                "Please respond ASAP. This is critical."
        );

        assertThat(urgency).isEqualTo(EmailUrgency.HIGH);
    }
}

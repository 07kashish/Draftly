package com.draftly.dto;

import com.draftly.enums.DraftStatus;
import com.draftly.enums.EmailCategory;
import com.draftly.enums.EmailTone;
import com.draftly.enums.EmailUrgency;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class DraftResponse {
    private UUID id;
    private UUID emailId;
    private String userEmail;
    private String sender;
    private String subject;
    private String content;
    private DraftStatus status;
    private String strategy;
    private String aiModel;
    private Integer retryCount;
    private EmailCategory category;
    private EmailTone tone;
    private EmailUrgency urgency;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

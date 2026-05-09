package com.draftly.dto;

import com.draftly.enums.EmailCategory;
import com.draftly.enums.EmailTone;
import com.draftly.enums.EmailUrgency;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class AnalyzeEmailResponse {
    private UUID emailId;
    private UUID draftId;
    private EmailCategory category;
    private EmailTone tone;
    private EmailUrgency urgency;
    private String strategy;
    private String draft;
}

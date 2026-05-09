package com.draftly.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserToneProfileResponse {

    private String userEmail;

    private String preferredTone;

    private String preferredGreeting;

    private String preferredSignOff;

    private Integer averageReplyLength;

    private Integer approvedDraftCount;

    private String commonPhrases;
}

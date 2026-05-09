package com.draftly.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ComposeEmailResponse {

    private String subject;

    private String draft;

    private String tone;

    private String strategy;
}

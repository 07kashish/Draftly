package com.draftly.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateDraftRequest {

    @NotBlank(message = "content is required")
    private String content;
}

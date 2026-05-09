package com.draftly.controller;

import com.draftly.dto.AnalyzeEmailRequest;
import com.draftly.dto.AnalyzeEmailResponse;
import com.draftly.dto.ComposeEmailRequest;
import com.draftly.dto.ComposeEmailResponse;
import com.draftly.service.ComposeEmailService;
import com.draftly.service.EmailAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/emails")
@RequiredArgsConstructor
@Tag(name = "Emails", description = "Analyze email context and generate reply drafts")
public class EmailController {

    private final EmailAnalysisService emailAnalysisService;
    private final ComposeEmailService composeEmailService;

    @PostMapping("/analyze")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Analyze email and generate AI draft")
    public AnalyzeEmailResponse analyzeEmail(@Valid @RequestBody AnalyzeEmailRequest request) {
        return emailAnalysisService.analyze(request);
    }

    @PostMapping("/compose")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Generate a full email from a natural-language prompt")
    public ComposeEmailResponse composeEmail(@Valid @RequestBody ComposeEmailRequest request) {
        return composeEmailService.compose(request);
    }
}

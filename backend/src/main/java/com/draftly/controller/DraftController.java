package com.draftly.controller;

import com.draftly.dto.DraftResponse;
import com.draftly.dto.UpdateDraftRequest;
import com.draftly.service.DraftService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/drafts")
@RequiredArgsConstructor
@Tag(name = "Drafts", description = "View, update, approve, reject, and regenerate AI reply drafts")
public class DraftController {

    private final DraftService draftService;

    @GetMapping("/{draftId}")
    @Operation(summary = "Get draft by ID")
    public DraftResponse getDraft(@PathVariable UUID draftId) {
        return draftService.getDraft(draftId);
    }

    @PatchMapping("/{draftId}")
    @Operation(summary = "Update draft content")
    public DraftResponse updateDraft(@PathVariable UUID draftId, @Valid @RequestBody UpdateDraftRequest request) {
        return draftService.updateDraft(draftId, request.getContent());
    }

    @PostMapping("/{draftId}/approve")
    @Operation(summary = "Approve draft")
    public DraftResponse approveDraft(@PathVariable UUID draftId) {
        return draftService.approveDraft(draftId);
    }

    @PostMapping("/{draftId}/reject")
    @Operation(summary = "Reject draft")
    public DraftResponse rejectDraft(@PathVariable UUID draftId) {
        return draftService.rejectDraft(draftId);
    }

    @PostMapping("/{draftId}/regenerate")
    @Operation(summary = "Regenerate draft")
    public DraftResponse regenerateDraft(@PathVariable UUID draftId) {
        return draftService.regenerateDraft(draftId);
    }
}

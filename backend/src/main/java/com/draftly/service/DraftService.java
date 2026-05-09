package com.draftly.service;

import com.draftly.dto.DraftResponse;
import com.draftly.entity.Draft;
import com.draftly.entity.User;
import com.draftly.enums.DraftStatus;
import com.draftly.exception.ResourceNotFoundException;
import com.draftly.repository.DraftRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DraftService {

    private final DraftRepository draftRepository;
    private final UserService userService;
    private final DraftGenerationService draftGenerationService;
    private final ReplyStrategyService replyStrategyService;
    private final ToneProfileService toneProfileService;

    @Transactional(readOnly = true)
    public DraftResponse getDraft(UUID draftId) {
        return toResponse(findDraft(draftId));
    }

    @Transactional
    public DraftResponse updateDraft(UUID draftId, String content) {
        Draft draft = findDraft(draftId);
        draft.setContent(content);
        return toResponse(draftRepository.save(draft));
    }

    @Transactional
    public DraftResponse approveDraft(UUID draftId) {
        Draft draft = findDraft(draftId);
        draft.setStatus(DraftStatus.APPROVED);
        Draft savedDraft = draftRepository.save(draft);
        toneProfileService.learnFromApprovedDraft(savedDraft);
        return toResponse(savedDraft);
    }

    @Transactional
    public DraftResponse rejectDraft(UUID draftId) {
        return updateStatus(draftId, DraftStatus.REJECTED);
    }

    @Transactional
    public DraftResponse regenerateDraft(UUID draftId) {
        Draft draft = findDraft(draftId);
        String strategy = replyStrategyService.createStrategy(draft.getEmail().getCategory(), draft.getEmail().getUrgency());
        String content = draftGenerationService.generateDraft(draft.getUser(), draft.getEmail(), strategy);

        draft.setStrategy(strategy);
        draft.setContent(content);
        draft.setStatus(DraftStatus.PENDING);
        draft.setRetryCount((draft.getRetryCount() == null ? 0 : draft.getRetryCount()) + 1);
        return toResponse(draftRepository.save(draft));
    }

    @Transactional(readOnly = true)
    public List<DraftResponse> getDraftsForUser(String email) {
        return draftRepository.findByUserEmailOrderByCreatedAtDesc(email)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private DraftResponse updateStatus(UUID draftId, DraftStatus status) {
        Draft draft = findDraft(draftId);
        draft.setStatus(status);
        return toResponse(draftRepository.save(draft));
    }

    private Draft findDraft(UUID draftId) {
        return draftRepository.findById(draftId)
                .orElseThrow(() -> new ResourceNotFoundException("Draft not found with id: " + draftId));
    }

    private DraftResponse toResponse(Draft draft) {
        return DraftResponse.builder()
                .id(draft.getId())
                .emailId(draft.getEmail().getId())
                .userEmail(draft.getUser().getEmail())
                .sender(draft.getEmail().getSender())
                .subject(draft.getEmail().getSubject())
                .content(draft.getContent())
                .status(draft.getStatus())
                .strategy(draft.getStrategy())
                .aiModel(draft.getAiModel())
                .retryCount(draft.getRetryCount())
                .category(draft.getEmail().getCategory())
                .tone(draft.getEmail().getTone())
                .urgency(draft.getEmail().getUrgency())
                .createdAt(draft.getCreatedAt())
                .updatedAt(draft.getUpdatedAt())
                .build();
    }
}

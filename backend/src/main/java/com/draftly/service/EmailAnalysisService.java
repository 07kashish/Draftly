package com.draftly.service;

import com.draftly.dto.AnalyzeEmailRequest;
import com.draftly.dto.AnalyzeEmailResponse;
import com.draftly.entity.Draft;
import com.draftly.entity.Email;
import com.draftly.entity.User;
import com.draftly.entity.UserToneProfile;
import com.draftly.enums.DraftStatus;
import com.draftly.enums.EmailCategory;
import com.draftly.enums.EmailTone;
import com.draftly.enums.EmailUrgency;
import com.draftly.repository.DraftRepository;
import com.draftly.repository.EmailRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailAnalysisService {

    private final UserService userService;
    private final CategoryDetectionService categoryDetectionService;
    private final ToneDetectionService toneDetectionService;
    private final UrgencyDetectionService urgencyDetectionService;
    private final ReplyStrategyService replyStrategyService;
    private final DraftGenerationService draftGenerationService;
    private final EmailRepository emailRepository;
    private final DraftRepository draftRepository;
    private final ToneProfileService toneProfileService;
    private final EmailActionItemService emailActionItemService;

    @Transactional
    public AnalyzeEmailResponse analyze(AnalyzeEmailRequest request) {
        User user = userService.findOrCreateUser(request.getUserName(), request.getUserEmail());
        log.info("[Draftly Sender] displayName: {}", request.getSenderName() == null ? "" : request.getSenderName());
        log.info("[Draftly Sender] email: {}", request.getSenderEmail() == null ? "" : request.getSenderEmail());

        EmailCategory category = categoryDetectionService.detect(request.getSubject(), request.getBody());
        EmailTone tone = toneDetectionService.detect(request.getSubject(), request.getBody());
        EmailUrgency urgency = urgencyDetectionService.detect(request.getSubject(), request.getBody());
        String actionContext = emailActionItemService.detectActionContext(
                request.getSubject(),
                request.getBody(),
                request.getThreadHistory()
        );
        String strategy = replyStrategyService.createStrategy(category, urgency, actionContext);

        Email email = emailRepository.save(Email.builder()
                .user(user)
                .gmailMessageId(request.getGmailMessageId())
                .gmailThreadId(request.getGmailThreadId())
                .sender(request.getSender())
                .senderName(request.getSenderName())
                .senderEmail(request.getSenderEmail())
                .recipients(String.join(",", request.getRecipients()))
                .subject(request.getSubject())
                .body(request.getBody())
                .threadHistory(request.getThreadHistory())
                .category(category)
                .tone(tone)
                .urgency(urgency)
                .build());

        UserToneProfile toneProfile = toneProfileService.findByUserEmail(user.getEmail()).orElse(null);
        String draftContent = draftGenerationService.generateDraft(user, email, strategy, toneProfile);
        Draft draft = draftRepository.save(Draft.builder()
                .user(user)
                .email(email)
                .content(draftContent)
                .status(DraftStatus.PENDING)
                .strategy(strategy)
                .aiModel("mock-rule-based-v1")
                .retryCount(0)
                .build());

        return AnalyzeEmailResponse.builder()
                .emailId(email.getId())
                .draftId(draft.getId())
                .category(category)
                .tone(tone)
                .urgency(urgency)
                .strategy(strategy)
                .draft(draftContent)
                .build();
    }
}

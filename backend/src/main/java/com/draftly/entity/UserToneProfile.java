package com.draftly.entity;

import com.draftly.enums.EmailCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_tone_profiles")
public class UserToneProfile {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private String userEmail;

    @Enumerated(EnumType.STRING)
    private EmailCategory category;

    @Column(name = "avg_length")
    private Integer averageReplyLength;

    private Integer approvedDraftCount;

    private Boolean usesContractions;

    private String preferredTone;

    private String preferredGreeting;

    @Column(name = "preferred_closing")
    private String preferredSignOff;

    @Column(columnDefinition = "TEXT")
    private String commonPhrases;

    @Column(columnDefinition = "TEXT")
    private String sampleEmailsJson;

    @Column(columnDefinition = "TEXT")
    private String styleNotes;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

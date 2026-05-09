package com.draftly.entity;

import com.draftly.enums.EmailCategory;
import com.draftly.enums.EmailTone;
import com.draftly.enums.EmailUrgency;
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
@Table(name = "emails")
public class Email {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String gmailMessageId;

    private String gmailThreadId;

    @Column(nullable = false)
    private String sender;

    private String senderName;

    private String senderEmail;

    @Column(nullable = false)
    private String recipients;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(columnDefinition = "TEXT")
    private String threadHistory;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmailCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmailTone tone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmailUrgency urgency;

    private LocalDateTime receivedAt;

    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (receivedAt == null) {
            receivedAt = LocalDateTime.now();
        }
        createdAt = LocalDateTime.now();
    }
}

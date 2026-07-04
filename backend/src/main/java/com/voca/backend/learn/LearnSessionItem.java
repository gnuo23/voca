package com.voca.backend.learn;

import com.voca.backend.vocab.VocabItem;
import com.voca.backend.review.ReviewQuality;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(name = "learn_session_items", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"session_id", "vocab_item_id"})
})
public class LearnSessionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private LearnSession session;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vocab_item_id", nullable = false)
    private VocabItem vocabItem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LearnItemStage stage = LearnItemStage.NEW;

    @Column(name = "correct_streak", nullable = false)
    private int correctStreak;

    @Column(name = "total_attempts", nullable = false)
    private int totalAttempts;

    @Column(name = "correct_attempts", nullable = false)
    private int correctAttempts;

    @Column(name = "incorrect_attempts", nullable = false)
    private int incorrectAttempts;

    @Column(name = "last_answered_at")
    private LocalDateTime lastAnsweredAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_quality_override")
    private ReviewQuality reviewQualityOverride;

    @Column(name = "review_applied_at")
    private LocalDateTime reviewAppliedAt;

    @Column(nullable = false)
    private int priority;

    // --- Getters & Setters ---

    public Long getId() { return id; }

    public LearnSession getSession() { return session; }
    public void setSession(LearnSession session) { this.session = session; }

    public VocabItem getVocabItem() { return vocabItem; }
    public void setVocabItem(VocabItem vocabItem) { this.vocabItem = vocabItem; }

    public LearnItemStage getStage() { return stage; }
    public void setStage(LearnItemStage stage) { this.stage = stage; }

    public int getCorrectStreak() { return correctStreak; }
    public void setCorrectStreak(int correctStreak) { this.correctStreak = correctStreak; }

    public int getTotalAttempts() { return totalAttempts; }
    public void setTotalAttempts(int totalAttempts) { this.totalAttempts = totalAttempts; }
    public void incrementTotalAttempts() { this.totalAttempts++; }

    public int getCorrectAttempts() { return correctAttempts; }
    public void setCorrectAttempts(int correctAttempts) { this.correctAttempts = correctAttempts; }
    public void incrementCorrectAttempts() { this.correctAttempts++; }

    public int getIncorrectAttempts() { return incorrectAttempts; }
    public void setIncorrectAttempts(int incorrectAttempts) { this.incorrectAttempts = incorrectAttempts; }
    public void incrementIncorrectAttempts() { this.incorrectAttempts++; }

    public LocalDateTime getLastAnsweredAt() { return lastAnsweredAt; }
    public void setLastAnsweredAt(LocalDateTime lastAnsweredAt) { this.lastAnsweredAt = lastAnsweredAt; }

    public ReviewQuality getReviewQualityOverride() { return reviewQualityOverride; }
    public void setReviewQualityOverride(ReviewQuality reviewQualityOverride) { this.reviewQualityOverride = reviewQualityOverride; }

    public LocalDateTime getReviewAppliedAt() { return reviewAppliedAt; }
    public void setReviewAppliedAt(LocalDateTime reviewAppliedAt) { this.reviewAppliedAt = reviewAppliedAt; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }
}

package com.voca.backend.vocab;

import com.voca.backend.user.User;
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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_progress")
public class UserProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vocab_item_id", nullable = false)
    private VocabItem vocabItem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VocabProgressStatus status = VocabProgressStatus.NEW;

    @Column(name = "known_count", nullable = false)
    private int knownCount;

    @Column(name = "unknown_count", nullable = false)
    private int unknownCount;

    @Column(name = "difficult_count", nullable = false)
    private int difficultCount;

    @Column(name = "correct_count", nullable = false)
    private int correctCount;

    @Column(name = "wrong_count", nullable = false)
    private int wrongCount;

    @Column(name = "streak_correct_count", nullable = false)
    private int streakCorrectCount;

    @Column(name = "ease_factor", nullable = false)
    private double easeFactor = 2.5;

    @Column(name = "interval_days", nullable = false)
    private int intervalDays;

    @Column(name = "repetition_count", nullable = false)
    private int repetitionCount;

    @Column(name = "lapse_count", nullable = false)
    private int lapseCount;

    @Column(name = "last_quality")
    private String lastQuality;

    @Column(name = "last_response_time_ms")
    private Integer lastResponseTimeMs;

    @Column(name = "last_marked_at", nullable = false)
    private LocalDateTime lastMarkedAt;

    @Column(name = "last_reviewed_at")
    private LocalDateTime lastReviewedAt;

    @Column(name = "next_review_at")
    private LocalDateTime nextReviewAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (lastMarkedAt == null) {
            lastMarkedAt = now;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public VocabItem getVocabItem() {
        return vocabItem;
    }

    public void setVocabItem(VocabItem vocabItem) {
        this.vocabItem = vocabItem;
    }

    public VocabProgressStatus getStatus() {
        return status;
    }

    public void setStatus(VocabProgressStatus status) {
        this.status = status;
    }

    public void mark(VocabMarkAction action) {
        status = switch (action) {
            case KNOWN -> VocabProgressStatus.REVIEW;
            case UNKNOWN -> VocabProgressStatus.LEARNING;
            case DIFFICULT -> VocabProgressStatus.DIFFICULT;
        };

        switch (action) {
            case KNOWN -> knownCount++;
            case UNKNOWN -> unknownCount++;
            case DIFFICULT -> difficultCount++;
        }

        lastMarkedAt = LocalDateTime.now();
    }

    public int getKnownCount() {
        return knownCount;
    }

    public int getUnknownCount() {
        return unknownCount;
    }

    public int getDifficultCount() {
        return difficultCount;
    }

    public void incrementKnownCount() {
        knownCount++;
    }

    public void incrementUnknownCount() {
        unknownCount++;
    }

    public void incrementDifficultCount() {
        difficultCount++;
    }

    public int getCorrectCount() {
        return correctCount;
    }

    public void setCorrectCount(int correctCount) {
        this.correctCount = correctCount;
    }

    public int getWrongCount() {
        return wrongCount;
    }

    public void setWrongCount(int wrongCount) {
        this.wrongCount = wrongCount;
    }

    public int getStreakCorrectCount() {
        return streakCorrectCount;
    }

    public void setStreakCorrectCount(int streakCorrectCount) {
        this.streakCorrectCount = streakCorrectCount;
    }

    public double getEaseFactor() {
        return easeFactor;
    }

    public void setEaseFactor(double easeFactor) {
        this.easeFactor = easeFactor;
    }

    public int getIntervalDays() {
        return intervalDays;
    }

    public void setIntervalDays(int intervalDays) {
        this.intervalDays = intervalDays;
    }

    public int getRepetitionCount() {
        return repetitionCount;
    }

    public void setRepetitionCount(int repetitionCount) {
        this.repetitionCount = repetitionCount;
    }

    public int getLapseCount() {
        return lapseCount;
    }

    public void setLapseCount(int lapseCount) {
        this.lapseCount = lapseCount;
    }

    public String getLastQuality() {
        return lastQuality;
    }

    public void setLastQuality(String lastQuality) {
        this.lastQuality = lastQuality;
    }

    public Integer getLastResponseTimeMs() {
        return lastResponseTimeMs;
    }

    public void setLastResponseTimeMs(Integer lastResponseTimeMs) {
        this.lastResponseTimeMs = lastResponseTimeMs;
    }

    public LocalDateTime getLastMarkedAt() {
        return lastMarkedAt;
    }

    public void setLastMarkedAt(LocalDateTime lastMarkedAt) {
        this.lastMarkedAt = lastMarkedAt;
    }

    public LocalDateTime getLastReviewedAt() {
        return lastReviewedAt;
    }

    public void setLastReviewedAt(LocalDateTime lastReviewedAt) {
        this.lastReviewedAt = lastReviewedAt;
    }

    public LocalDateTime getNextReviewAt() {
        return nextReviewAt;
    }

    public void setNextReviewAt(LocalDateTime nextReviewAt) {
        this.nextReviewAt = nextReviewAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}

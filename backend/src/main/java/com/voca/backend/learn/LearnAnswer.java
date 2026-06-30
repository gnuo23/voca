package com.voca.backend.learn;

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
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "learn_answers")
public class LearnAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private LearnSession session;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_item_id", nullable = false)
    private LearnSessionItem sessionItem;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false)
    private LearnQuestionType questionType;

    @Column(nullable = false, length = 1000)
    private String prompt;

    @Column(name = "user_answer", length = 1000)
    private String userAnswer;

    @Column(name = "correct_answer", nullable = false, length = 1000)
    private String correctAnswer;

    @Column(nullable = false)
    private boolean correct;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GradeVerdict verdict = GradeVerdict.INCORRECT;

    @Column(name = "similarity_score", nullable = false)
    private double similarityScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage_before")
    private LearnItemStage stageBefore;

    @Column(name = "review_snapshot_exists")
    private Boolean reviewSnapshotExists;

    @Column(name = "review_snapshot_status")
    private String reviewSnapshotStatus;

    @Column(name = "review_snapshot_known_count")
    private Integer reviewSnapshotKnownCount;

    @Column(name = "review_snapshot_unknown_count")
    private Integer reviewSnapshotUnknownCount;

    @Column(name = "review_snapshot_difficult_count")
    private Integer reviewSnapshotDifficultCount;

    @Column(name = "review_snapshot_correct_count")
    private Integer reviewSnapshotCorrectCount;

    @Column(name = "review_snapshot_wrong_count")
    private Integer reviewSnapshotWrongCount;

    @Column(name = "review_snapshot_streak_correct_count")
    private Integer reviewSnapshotStreakCorrectCount;

    @Column(name = "review_snapshot_ease_factor")
    private Double reviewSnapshotEaseFactor;

    @Column(name = "review_snapshot_interval_days")
    private Integer reviewSnapshotIntervalDays;

    @Column(name = "review_snapshot_repetition_count")
    private Integer reviewSnapshotRepetitionCount;

    @Column(name = "review_snapshot_lapse_count")
    private Integer reviewSnapshotLapseCount;

    @Column(name = "review_snapshot_last_quality")
    private String reviewSnapshotLastQuality;

    @Column(name = "review_snapshot_last_response_time_ms")
    private Integer reviewSnapshotLastResponseTimeMs;

    @Column(name = "review_snapshot_last_marked_at")
    private LocalDateTime reviewSnapshotLastMarkedAt;

    @Column(name = "review_snapshot_last_reviewed_at")
    private LocalDateTime reviewSnapshotLastReviewedAt;

    @Column(name = "review_snapshot_next_review_at")
    private LocalDateTime reviewSnapshotNextReviewAt;

    @Column(name = "response_time_ms")
    private Long responseTimeMs;

    @Column(name = "answered_at", nullable = false)
    private LocalDateTime answeredAt;

    @PrePersist
    void onCreate() {
        if (answeredAt == null) {
            answeredAt = LocalDateTime.now();
        }
    }

    // --- Getters & Setters ---

    public Long getId() { return id; }

    public LearnSession getSession() { return session; }
    public void setSession(LearnSession session) { this.session = session; }

    public LearnSessionItem getSessionItem() { return sessionItem; }
    public void setSessionItem(LearnSessionItem sessionItem) { this.sessionItem = sessionItem; }

    public LearnQuestionType getQuestionType() { return questionType; }
    public void setQuestionType(LearnQuestionType questionType) { this.questionType = questionType; }

    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }

    public String getUserAnswer() { return userAnswer; }
    public void setUserAnswer(String userAnswer) { this.userAnswer = userAnswer; }

    public String getCorrectAnswer() { return correctAnswer; }
    public void setCorrectAnswer(String correctAnswer) { this.correctAnswer = correctAnswer; }

    public boolean isCorrect() { return correct; }
    public void setCorrect(boolean correct) { this.correct = correct; }

    public GradeVerdict getVerdict() { return verdict; }
    public void setVerdict(GradeVerdict verdict) { this.verdict = verdict; }

    public double getSimilarityScore() { return similarityScore; }
    public void setSimilarityScore(double similarityScore) { this.similarityScore = similarityScore; }

    public LearnItemStage getStageBefore() { return stageBefore; }
    public void setStageBefore(LearnItemStage stageBefore) { this.stageBefore = stageBefore; }

    public Boolean getReviewSnapshotExists() { return reviewSnapshotExists; }
    public void setReviewSnapshotExists(Boolean reviewSnapshotExists) { this.reviewSnapshotExists = reviewSnapshotExists; }

    public String getReviewSnapshotStatus() { return reviewSnapshotStatus; }
    public void setReviewSnapshotStatus(String reviewSnapshotStatus) { this.reviewSnapshotStatus = reviewSnapshotStatus; }

    public Integer getReviewSnapshotKnownCount() { return reviewSnapshotKnownCount; }
    public void setReviewSnapshotKnownCount(Integer reviewSnapshotKnownCount) { this.reviewSnapshotKnownCount = reviewSnapshotKnownCount; }

    public Integer getReviewSnapshotUnknownCount() { return reviewSnapshotUnknownCount; }
    public void setReviewSnapshotUnknownCount(Integer reviewSnapshotUnknownCount) { this.reviewSnapshotUnknownCount = reviewSnapshotUnknownCount; }

    public Integer getReviewSnapshotDifficultCount() { return reviewSnapshotDifficultCount; }
    public void setReviewSnapshotDifficultCount(Integer reviewSnapshotDifficultCount) { this.reviewSnapshotDifficultCount = reviewSnapshotDifficultCount; }

    public Integer getReviewSnapshotCorrectCount() { return reviewSnapshotCorrectCount; }
    public void setReviewSnapshotCorrectCount(Integer reviewSnapshotCorrectCount) { this.reviewSnapshotCorrectCount = reviewSnapshotCorrectCount; }

    public Integer getReviewSnapshotWrongCount() { return reviewSnapshotWrongCount; }
    public void setReviewSnapshotWrongCount(Integer reviewSnapshotWrongCount) { this.reviewSnapshotWrongCount = reviewSnapshotWrongCount; }

    public Integer getReviewSnapshotStreakCorrectCount() { return reviewSnapshotStreakCorrectCount; }
    public void setReviewSnapshotStreakCorrectCount(Integer reviewSnapshotStreakCorrectCount) { this.reviewSnapshotStreakCorrectCount = reviewSnapshotStreakCorrectCount; }

    public Double getReviewSnapshotEaseFactor() { return reviewSnapshotEaseFactor; }
    public void setReviewSnapshotEaseFactor(Double reviewSnapshotEaseFactor) { this.reviewSnapshotEaseFactor = reviewSnapshotEaseFactor; }

    public Integer getReviewSnapshotIntervalDays() { return reviewSnapshotIntervalDays; }
    public void setReviewSnapshotIntervalDays(Integer reviewSnapshotIntervalDays) { this.reviewSnapshotIntervalDays = reviewSnapshotIntervalDays; }

    public Integer getReviewSnapshotRepetitionCount() { return reviewSnapshotRepetitionCount; }
    public void setReviewSnapshotRepetitionCount(Integer reviewSnapshotRepetitionCount) { this.reviewSnapshotRepetitionCount = reviewSnapshotRepetitionCount; }

    public Integer getReviewSnapshotLapseCount() { return reviewSnapshotLapseCount; }
    public void setReviewSnapshotLapseCount(Integer reviewSnapshotLapseCount) { this.reviewSnapshotLapseCount = reviewSnapshotLapseCount; }

    public String getReviewSnapshotLastQuality() { return reviewSnapshotLastQuality; }
    public void setReviewSnapshotLastQuality(String reviewSnapshotLastQuality) { this.reviewSnapshotLastQuality = reviewSnapshotLastQuality; }

    public Integer getReviewSnapshotLastResponseTimeMs() { return reviewSnapshotLastResponseTimeMs; }
    public void setReviewSnapshotLastResponseTimeMs(Integer reviewSnapshotLastResponseTimeMs) { this.reviewSnapshotLastResponseTimeMs = reviewSnapshotLastResponseTimeMs; }

    public LocalDateTime getReviewSnapshotLastMarkedAt() { return reviewSnapshotLastMarkedAt; }
    public void setReviewSnapshotLastMarkedAt(LocalDateTime reviewSnapshotLastMarkedAt) { this.reviewSnapshotLastMarkedAt = reviewSnapshotLastMarkedAt; }

    public LocalDateTime getReviewSnapshotLastReviewedAt() { return reviewSnapshotLastReviewedAt; }
    public void setReviewSnapshotLastReviewedAt(LocalDateTime reviewSnapshotLastReviewedAt) { this.reviewSnapshotLastReviewedAt = reviewSnapshotLastReviewedAt; }

    public LocalDateTime getReviewSnapshotNextReviewAt() { return reviewSnapshotNextReviewAt; }
    public void setReviewSnapshotNextReviewAt(LocalDateTime reviewSnapshotNextReviewAt) { this.reviewSnapshotNextReviewAt = reviewSnapshotNextReviewAt; }

    public Long getResponseTimeMs() { return responseTimeMs; }
    public void setResponseTimeMs(Long responseTimeMs) { this.responseTimeMs = responseTimeMs; }

    public LocalDateTime getAnsweredAt() { return answeredAt; }
    public void setAnsweredAt(LocalDateTime answeredAt) { this.answeredAt = answeredAt; }
}

package com.voca.backend.learn;

import com.voca.backend.deck.Deck;
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
@Table(name = "learn_sessions")
public class LearnSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "deck_id", nullable = false)
    private Deck deck;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LearnSessionScope scope = LearnSessionScope.ALL;

    @Column(name = "total_terms", nullable = false)
    private int totalTerms;

    @Column(name = "mastered_terms", nullable = false)
    private int masteredTerms;

    @Column(name = "total_answers", nullable = false)
    private int totalAnswers;

    @Column(name = "correct_answers", nullable = false)
    private int correctAnswers;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LearnSessionStatus status = LearnSessionStatus.IN_PROGRESS;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "duration_ms")
    private long durationMs;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (startedAt == null) {
            startedAt = now;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // --- Getters & Setters ---

    public Long getId() { return id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Deck getDeck() { return deck; }
    public void setDeck(Deck deck) { this.deck = deck; }

    public LearnSessionScope getScope() { return scope; }
    public void setScope(LearnSessionScope scope) { this.scope = scope; }

    public int getTotalTerms() { return totalTerms; }
    public void setTotalTerms(int totalTerms) { this.totalTerms = totalTerms; }

    public int getMasteredTerms() { return masteredTerms; }
    public void setMasteredTerms(int masteredTerms) { this.masteredTerms = masteredTerms; }
    public void incrementMasteredTerms() { this.masteredTerms++; }

    public int getTotalAnswers() { return totalAnswers; }
    public void setTotalAnswers(int totalAnswers) { this.totalAnswers = totalAnswers; }
    public void incrementTotalAnswers() { this.totalAnswers++; }

    public int getCorrectAnswers() { return correctAnswers; }
    public void setCorrectAnswers(int correctAnswers) { this.correctAnswers = correctAnswers; }
    public void incrementCorrectAnswers() { this.correctAnswers++; }

    public LearnSessionStatus getStatus() { return status; }
    public void setStatus(LearnSessionStatus status) { this.status = status; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public long getDurationMs() { return durationMs; }
    public void setDurationMs(long durationMs) { this.durationMs = durationMs; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}

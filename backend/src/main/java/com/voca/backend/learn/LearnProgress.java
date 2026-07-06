package com.voca.backend.learn;

import com.voca.backend.user.User;
import com.voca.backend.vocab.VocabItem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(name = "learn_progress", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "vocab_item_id"})
})
public class LearnProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vocab_item_id", nullable = false)
    private VocabItem vocabItem;

    @Column(nullable = false)
    private boolean mastered;

    @Column(name = "correct_attempts", nullable = false)
    private int correctAttempts;

    @Column(name = "incorrect_attempts", nullable = false)
    private int incorrectAttempts;

    @Column(name = "last_practiced_at")
    private LocalDateTime lastPracticedAt;

    @Column(name = "mastered_at")
    private LocalDateTime masteredAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
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

    public Long getId() { return id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public VocabItem getVocabItem() { return vocabItem; }
    public void setVocabItem(VocabItem vocabItem) { this.vocabItem = vocabItem; }

    public boolean isMastered() { return mastered; }
    public void setMastered(boolean mastered) { this.mastered = mastered; }

    public int getCorrectAttempts() { return correctAttempts; }
    public void setCorrectAttempts(int correctAttempts) { this.correctAttempts = correctAttempts; }

    public int getIncorrectAttempts() { return incorrectAttempts; }
    public void setIncorrectAttempts(int incorrectAttempts) { this.incorrectAttempts = incorrectAttempts; }

    public LocalDateTime getLastPracticedAt() { return lastPracticedAt; }
    public void setLastPracticedAt(LocalDateTime lastPracticedAt) { this.lastPracticedAt = lastPracticedAt; }

    public LocalDateTime getMasteredAt() { return masteredAt; }
    public void setMasteredAt(LocalDateTime masteredAt) { this.masteredAt = masteredAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}

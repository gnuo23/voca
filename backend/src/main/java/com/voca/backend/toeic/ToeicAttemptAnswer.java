package com.voca.backend.toeic;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "toeic_attempt_answers")
public class ToeicAttemptAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attempt_id", nullable = false)
    private ToeicAttempt attempt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private ToeicQuestion question;

    @Column(name = "selected_label", length = 4)
    private String selectedLabel;

    @Column(nullable = false)
    private boolean correct;

    @Column(name = "answered_at", nullable = false)
    private LocalDateTime answeredAt;

    @PrePersist
    void onCreate() {
        answeredAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public ToeicAttempt getAttempt() {
        return attempt;
    }

    public void setAttempt(ToeicAttempt attempt) {
        this.attempt = attempt;
    }

    public ToeicQuestion getQuestion() {
        return question;
    }

    public void setQuestion(ToeicQuestion question) {
        this.question = question;
    }

    public String getSelectedLabel() {
        return selectedLabel;
    }

    public void setSelectedLabel(String selectedLabel) {
        this.selectedLabel = selectedLabel;
    }

    public boolean isCorrect() {
        return correct;
    }

    public void setCorrect(boolean correct) {
        this.correct = correct;
    }

    public LocalDateTime getAnsweredAt() {
        return answeredAt;
    }
}

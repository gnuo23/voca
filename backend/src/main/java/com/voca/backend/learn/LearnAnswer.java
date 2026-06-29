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

    public Long getResponseTimeMs() { return responseTimeMs; }
    public void setResponseTimeMs(Long responseTimeMs) { this.responseTimeMs = responseTimeMs; }

    public LocalDateTime getAnsweredAt() { return answeredAt; }
    public void setAnsweredAt(LocalDateTime answeredAt) { this.answeredAt = answeredAt; }
}

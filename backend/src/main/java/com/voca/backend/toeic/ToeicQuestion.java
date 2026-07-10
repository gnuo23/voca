package com.voca.backend.toeic;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "toeic_questions")
public class ToeicQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private ToeicQuestionGroup group;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "test_id", nullable = false)
    private ToeicTest test;

    @Column(name = "question_part", nullable = false, length = 40)
    private String questionPart;

    @Column(name = "question_number", nullable = false)
    private int questionNumber;

    @Column(name = "question_text", columnDefinition = "text")
    private String questionText;

    @Column(name = "correct_answer_label", nullable = false, length = 4)
    private String correctAnswerLabel;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("answerOrder asc")
    private List<ToeicAnswer> answers = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public List<ToeicAnswer> getAnswers() {
        return answers;
    }

    public ToeicQuestionGroup getGroup() {
        return group;
    }

    public void setGroup(ToeicQuestionGroup group) {
        this.group = group;
    }

    public ToeicTest getTest() {
        return test;
    }

    public void setTest(ToeicTest test) {
        this.test = test;
    }

    public String getQuestionPart() {
        return questionPart;
    }

    public void setQuestionPart(String questionPart) {
        this.questionPart = questionPart;
    }

    public int getQuestionNumber() {
        return questionNumber;
    }

    public void setQuestionNumber(int questionNumber) {
        this.questionNumber = questionNumber;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public String getCorrectAnswerLabel() {
        return correctAnswerLabel;
    }

    public void setCorrectAnswerLabel(String correctAnswerLabel) {
        this.correctAnswerLabel = correctAnswerLabel;
    }
}

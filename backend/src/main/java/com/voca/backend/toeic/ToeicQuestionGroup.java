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
@Table(name = "toeic_question_groups")
public class ToeicQuestionGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "test_id", nullable = false)
    private ToeicTest test;

    @Column(name = "question_part", nullable = false, length = 40)
    private String questionPart;

    @Column(name = "group_order", nullable = false)
    private int groupOrder;

    @Column(name = "passage_text", columnDefinition = "text")
    private String passageText;

    @Column(name = "audio_transcript", columnDefinition = "text")
    private String audioTranscript;

    @Column(name = "audio_transcript_html", columnDefinition = "text")
    private String audioTranscriptHtml;

    @Column(name = "explanation_html", columnDefinition = "text")
    private String explanationHtml;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder asc")
    private List<ToeicGroupMedia> media = new ArrayList<>();

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("questionNumber asc")
    private List<ToeicQuestion> questions = new ArrayList<>();

    public Long getId() {
        return id;
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

    public int getGroupOrder() {
        return groupOrder;
    }

    public void setGroupOrder(int groupOrder) {
        this.groupOrder = groupOrder;
    }

    public String getPassageText() {
        return passageText;
    }

    public void setPassageText(String passageText) {
        this.passageText = passageText;
    }

    public String getAudioTranscript() {
        return audioTranscript;
    }

    public void setAudioTranscript(String audioTranscript) {
        this.audioTranscript = audioTranscript;
    }

    public String getAudioTranscriptHtml() {
        return audioTranscriptHtml;
    }

    public void setAudioTranscriptHtml(String audioTranscriptHtml) {
        this.audioTranscriptHtml = audioTranscriptHtml;
    }

    public String getExplanationHtml() {
        return explanationHtml;
    }

    public void setExplanationHtml(String explanationHtml) {
        this.explanationHtml = explanationHtml;
    }

    public List<ToeicGroupMedia> getMedia() {
        return media;
    }

    public List<ToeicQuestion> getQuestions() {
        return questions;
    }
}

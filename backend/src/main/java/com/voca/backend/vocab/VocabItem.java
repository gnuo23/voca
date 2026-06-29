package com.voca.backend.vocab;

import com.voca.backend.deck.Deck;
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

import java.time.LocalDateTime;

@Entity
@Table(name = "vocab_items")
public class VocabItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "deck_id", nullable = false)
    private Deck deck;

    @Column(nullable = false)
    private String word;

    @Column(name = "normalized_word", nullable = false)
    private String normalizedWord;

    @Column(name = "part_of_speech")
    private String partOfSpeech;

    @Column(name = "meaning_vi")
    private String meaningVi;

    @Column
    private String ipa;

    @Column(name = "pronunciation_hint")
    private String pronunciationHint;

    @Column(name = "example_en")
    private String exampleEn;

    @Column(name = "example_vi")
    private String exampleVi;

    @Column
    private String topic;

    @Column
    private String level;

    @Column(length = 2000)
    private String synonyms;

    @Column(length = 2000)
    private String antonyms;

    @Column(length = 2000)
    private String collocations;

    @Column(name = "enriched_at")
    private LocalDateTime enrichedAt;

    @Column(name = "audio_url", length = 1000)
    private String audioUrl;

    @Column(name = "audio_us_url", length = 1000)
    private String audioUsUrl;

    @Column(name = "audio_uk_url", length = 1000)
    private String audioUkUrl;

    @Column(name = "audio_accent")
    private String audioAccent;

    @Column(name = "audio_source")
    private String audioSource;

    @Column(name = "audio_refreshed_at")
    private LocalDateTime audioRefreshedAt;

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

    public Long getId() {
        return id;
    }

    public Deck getDeck() {
        return deck;
    }

    public void setDeck(Deck deck) {
        this.deck = deck;
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public String getNormalizedWord() {
        return normalizedWord;
    }

    public void setNormalizedWord(String normalizedWord) {
        this.normalizedWord = normalizedWord;
    }

    public String getPartOfSpeech() {
        return partOfSpeech;
    }

    public void setPartOfSpeech(String partOfSpeech) {
        this.partOfSpeech = partOfSpeech;
    }

    public String getMeaningVi() {
        return meaningVi;
    }

    public void setMeaningVi(String meaningVi) {
        this.meaningVi = meaningVi;
    }

    public String getIpa() {
        return ipa;
    }

    public void setIpa(String ipa) {
        this.ipa = ipa;
    }

    public String getPronunciationHint() {
        return pronunciationHint;
    }

    public void setPronunciationHint(String pronunciationHint) {
        this.pronunciationHint = pronunciationHint;
    }

    public String getExampleEn() {
        return exampleEn;
    }

    public void setExampleEn(String exampleEn) {
        this.exampleEn = exampleEn;
    }

    public String getExampleVi() {
        return exampleVi;
    }

    public void setExampleVi(String exampleVi) {
        this.exampleVi = exampleVi;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getSynonyms() {
        return synonyms;
    }

    public void setSynonyms(String synonyms) {
        this.synonyms = synonyms;
    }

    public String getAntonyms() {
        return antonyms;
    }

    public void setAntonyms(String antonyms) {
        this.antonyms = antonyms;
    }

    public String getCollocations() {
        return collocations;
    }

    public void setCollocations(String collocations) {
        this.collocations = collocations;
    }

    public LocalDateTime getEnrichedAt() {
        return enrichedAt;
    }

    public void setEnrichedAt(LocalDateTime enrichedAt) {
        this.enrichedAt = enrichedAt;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public void setAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
    }

    public String getAudioUsUrl() {
        return audioUsUrl;
    }

    public void setAudioUsUrl(String audioUsUrl) {
        this.audioUsUrl = audioUsUrl;
    }

    public String getAudioUkUrl() {
        return audioUkUrl;
    }

    public void setAudioUkUrl(String audioUkUrl) {
        this.audioUkUrl = audioUkUrl;
    }

    public String getAudioAccent() {
        return audioAccent;
    }

    public void setAudioAccent(String audioAccent) {
        this.audioAccent = audioAccent;
    }

    public String getAudioSource() {
        return audioSource;
    }

    public void setAudioSource(String audioSource) {
        this.audioSource = audioSource;
    }

    public LocalDateTime getAudioRefreshedAt() {
        return audioRefreshedAt;
    }

    public void setAudioRefreshedAt(LocalDateTime audioRefreshedAt) {
        this.audioRefreshedAt = audioRefreshedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}

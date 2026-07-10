package com.voca.backend.toeic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Service
public class ToeicImportService {

    private static final Logger log = LoggerFactory.getLogger(ToeicImportService.class);
    private static final String[] LABELS = {"A", "B", "C", "D"};

    private final ToeicTestRepository testRepository;
    private final ObjectMapper objectMapper;

    public ToeicImportService(ToeicTestRepository testRepository, ObjectMapper objectMapper) {
        this.testRepository = testRepository;
        this.objectMapper = objectMapper;
    }

    public int importDirectory(Path importDir) {
        if (!Files.isDirectory(importDir)) {
            log.warn("TOEIC import dir not found: {}", importDir.toAbsolutePath());
            return 0;
        }
        List<Path> files = findFullJsonFiles(importDir);
        int imported = 0;
        for (Path file : files) {
            try {
                if (importFile(file)) {
                    imported++;
                }
            } catch (Exception e) {
                log.error("Failed to import TOEIC file {}: {}", file, e.getMessage());
            }
        }
        log.info("TOEIC import complete: {} new tests from {} files", imported, files.size());
        return imported;
    }

    private List<Path> findFullJsonFiles(Path importDir) {
        try (Stream<Path> stream = Files.walk(importDir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith("-full.json"))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            log.error("Failed to walk TOEIC import dir {}: {}", importDir, e.getMessage());
            return List.of();
        }
    }

    private boolean importFile(Path file) throws IOException {
        JsonNode root = readJson(file);
        JsonNode testNode = root.path("test");
        String slug = testNode.path("slug").asText(null);
        if (slug == null || slug.isBlank()) {
            log.warn("Skipping TOEIC file with no slug: {}", file);
            return false;
        }
        if (testRepository.existsBySlug(slug)) {
            return false;
        }

        ToeicTest test = new ToeicTest();
        test.setSlug(slug);
        test.setTestName(text(testNode, "testName", slug));
        test.setCollectionName(text(testNode, "collectionName", null));
        test.setTestNumber(testNode.path("testNumber").isMissingNode() ? null : testNode.path("testNumber").asInt());
        test.setTotalQuestions(testNode.path("totalQuestions").asInt(0));
        int duration = testNode.path("duration").asInt(120);
        test.setDurationMinutes(duration <= 0 ? 120 : duration);

        for (JsonNode groupNode : root.path("questionGroups")) {
            ToeicQuestionGroup group = buildGroup(groupNode, test);
            test.getQuestionGroups().add(group);
        }

        testRepository.save(test);
        log.info("Imported TOEIC test '{}' ({} groups)", slug, test.getQuestionGroups().size());
        return true;
    }

    private ToeicQuestionGroup buildGroup(JsonNode groupNode, ToeicTest test) {
        ToeicQuestionGroup group = new ToeicQuestionGroup();
        group.setTest(test);
        group.setQuestionPart(groupNode.path("questionPart").asText(""));
        group.setGroupOrder(groupNode.path("questionGroupOrder").asInt(0));
        group.setPassageText(text(groupNode, "passageText", null));
        group.setAudioTranscript(text(groupNode, "audioTranscript", null));
        group.setAudioTranscriptHtml(text(groupNode, "audioTranscript", null));
        group.setExplanationHtml(text(groupNode, "explanation", null));

        int mediaOrder = 1;
        for (JsonNode fileNode : groupNode.path("files")) {
            ToeicGroupMedia media = new ToeicGroupMedia();
            media.setGroup(group);
            media.setUrl(fileNode.path("url").asText(""));
            media.setFileType(fileNode.path("fileType").asText("UNKNOWN"));
            media.setDisplayOrder(fileNode.path("displayOrder").asInt(mediaOrder));
            group.getMedia().add(media);
            mediaOrder++;
        }

        for (JsonNode questionNode : groupNode.path("questions")) {
            group.getQuestions().add(buildQuestion(questionNode, group, test));
        }
        return group;
    }

    private ToeicQuestion buildQuestion(JsonNode questionNode, ToeicQuestionGroup group, ToeicTest test) {
        ToeicQuestion question = new ToeicQuestion();
        question.setGroup(group);
        question.setTest(test);
        question.setQuestionPart(group.getQuestionPart());
        question.setQuestionNumber(questionNode.path("questionNumber").asInt(0));
        question.setQuestionText(text(questionNode, "questionText", null));

        String correctLabel = questionNode.path("correctAnswerLabel").asText(null);
        JsonNode answersNode = questionNode.path("answers");
        int order = 0;
        for (JsonNode answerNode : answersNode) {
            int answerOrder = answerNode.path("answerOrder").isMissingNode()
                    ? order + 1
                    : answerNode.path("answerOrder").asInt();
            String label = labelForOrder(answerOrder);
            boolean isCorrect = answerNode.path("isCorrect").asBoolean(false);

            ToeicAnswer answer = new ToeicAnswer();
            answer.setQuestion(question);
            answer.setAnswerLabel(label);
            answer.setContent(text(answerNode, "content", null));
            answer.setAnswerOrder(answerOrder);
            answer.setCorrect(isCorrect);
            question.getAnswers().add(answer);

            if (isCorrect && (correctLabel == null || correctLabel.isBlank())) {
                correctLabel = label;
            }
            order++;
        }
        question.setCorrectAnswerLabel(correctLabel == null ? "" : correctLabel);
        return question;
    }

    private JsonNode readJson(Path file) throws IOException {
        byte[] bytes = Files.readAllBytes(file);
        String content = new String(bytes, StandardCharsets.UTF_8);
        if (!content.isEmpty() && content.charAt(0) == '﻿') {
            content = content.substring(1);
        }
        return objectMapper.readTree(content);
    }

    private String labelForOrder(int answerOrder) {
        int index = answerOrder - 1;
        if (index >= 0 && index < LABELS.length) {
            return LABELS[index];
        }
        return String.valueOf(answerOrder);
    }

    private String text(JsonNode node, String field, String fallback) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return fallback;
        }
        String text = value.asText();
        return text == null || text.isEmpty() ? fallback : text;
    }
}

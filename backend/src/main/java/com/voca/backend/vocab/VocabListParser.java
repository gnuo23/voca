package com.voca.backend.vocab;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class VocabListParser {

    private static final Pattern POS_PREFIX = Pattern.compile("^\\(([^)]+)\\)\\s*(.*)$");
    private static final Pattern DASH_SEPARATOR = Pattern.compile("\\s+-\\s+");

    public List<ParsedVocabLine> parse(String rawText) {
        String[] lines = rawText.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
        List<ParsedVocabLine> items = new ArrayList<>();

        for (int index = 0; index < lines.length; index++) {
            int lineNumber = index + 1;
            String line = lines[index].trim();
            if (line.isEmpty()) {
                continue;
            }

            items.add(parseLine(lineNumber, line));
        }

        return items;
    }

    private ParsedVocabLine parseLine(int lineNumber, String line) {
        if (line.contains("|")) {
            return parsePipe(lineNumber, line);
        }

        SplitLine splitLine = splitLine(line);
        if (splitLine == null) {
            return valid(lineNumber, line, null, null);
        }

        if (splitLine.word().isBlank()) {
            return error(lineNumber, "Missing word");
        }

        if (splitLine.meaning().isBlank()) {
            return error(lineNumber, "Missing meaning");
        }

        MeaningParts parts = parseMeaning(splitLine.meaning());
        if (parts.meaning() == null || parts.meaning().isBlank()) {
            return error(lineNumber, "Missing meaning");
        }

        return valid(lineNumber, splitLine.word(), parts.partOfSpeech(), parts.meaning());
    }

    private ParsedVocabLine parsePipe(int lineNumber, String line) {
        String[] parts = line.split("\\|", -1);
        if (parts.length != 3) {
            return error(lineNumber, "Pipe format must be: word | pos | meaning");
        }

        String word = parts[0].trim();
        String pos = normalizeNullable(parts[1]);
        String meaning = parts[2].trim();

        if (word.isBlank()) {
            return error(lineNumber, "Missing word");
        }

        if (meaning.isBlank()) {
            return error(lineNumber, "Missing meaning");
        }

        return valid(lineNumber, word, pos, meaning);
    }

    private SplitLine splitLine(String line) {
        int semicolon = line.indexOf(';');
        if (semicolon >= 0) {
            return new SplitLine(line.substring(0, semicolon).trim(), line.substring(semicolon + 1).trim());
        }

        int colon = line.indexOf(':');
        if (colon >= 0) {
            return new SplitLine(line.substring(0, colon).trim(), line.substring(colon + 1).trim());
        }

        Matcher dashMatcher = DASH_SEPARATOR.matcher(line);
        if (dashMatcher.find()) {
            return new SplitLine(line.substring(0, dashMatcher.start()).trim(), line.substring(dashMatcher.end()).trim());
        }

        return null;
    }

    private MeaningParts parseMeaning(String meaning) {
        Matcher matcher = POS_PREFIX.matcher(meaning);
        if (matcher.matches()) {
            return new MeaningParts(normalizeNullable(matcher.group(1)), matcher.group(2).trim());
        }

        return new MeaningParts(null, meaning.trim());
    }

    private ParsedVocabLine valid(int lineNumber, String word, String partOfSpeech, String meaning) {
        String normalizedWord = word.trim().replaceAll("\\s+", " ");
        if (normalizedWord.length() > 255) {
            return error(lineNumber, "Word is too long");
        }

        String normalizedMeaning = normalizeNullable(meaning);
        if (normalizedMeaning != null && normalizedMeaning.length() > 1000) {
            return error(lineNumber, "Meaning is too long");
        }

        String normalizedPos = normalizeNullable(partOfSpeech);
        if (normalizedPos != null && normalizedPos.length() > 80) {
            return error(lineNumber, "Part of speech is too long");
        }

        return new ParsedVocabLine(lineNumber, normalizedWord, normalizedPos, normalizedMeaning, null);
    }

    private ParsedVocabLine error(int lineNumber, String message) {
        return new ParsedVocabLine(lineNumber, null, null, null, message);
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim().replaceAll("\\s+", " ");
        return normalized.isBlank() ? null : normalized;
    }

    private record SplitLine(String word, String meaning) {
    }

    private record MeaningParts(String partOfSpeech, String meaning) {
    }
}

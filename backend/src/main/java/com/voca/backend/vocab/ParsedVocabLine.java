package com.voca.backend.vocab;

public record ParsedVocabLine(
        Integer lineNumber,
        String word,
        String partOfSpeech,
        String meaningVi,
        String error
) {

    public boolean isValid() {
        return error == null;
    }
}

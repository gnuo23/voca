package com.voca.backend.vocab;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class VocabListParserTest {

    private final VocabListParser parser = new VocabListParser();

    @Test
    void parsesSupportedFormats() {
        String rawText = """
                absent ; (adj) vắng mặt, không có mặt
                accumulate ; tích lũy, gom góp
                real estate ; (n) bất động sản
                take off - cất cánh
                mortgage: khoản vay mua nhà
                lease | n | hợp đồng thuê
                standalone
                """;

        List<ParsedVocabLine> items = parser.parse(rawText);

        assertThat(items).hasSize(7);
        assertThat(items.get(0)).extracting(
                ParsedVocabLine::word,
                ParsedVocabLine::partOfSpeech,
                ParsedVocabLine::meaningVi,
                ParsedVocabLine::error
        ).containsExactly("absent", "adj", "vắng mặt, không có mặt", null);
        assertThat(items.get(1).word()).isEqualTo("accumulate");
        assertThat(items.get(1).partOfSpeech()).isNull();
        assertThat(items.get(2).word()).isEqualTo("real estate");
        assertThat(items.get(2).partOfSpeech()).isEqualTo("n");
        assertThat(items.get(3).meaningVi()).isEqualTo("cất cánh");
        assertThat(items.get(4).meaningVi()).isEqualTo("khoản vay mua nhà");
        assertThat(items.get(5).partOfSpeech()).isEqualTo("n");
        assertThat(items.get(6).word()).isEqualTo("standalone");
        assertThat(items.get(6).meaningVi()).isNull();
    }

    @Test
    void reportsInvalidLinesWithLineNumbers() {
        String rawText = """
                ; missing word
                broken | pipe
                word ; (adj)
                """;

        List<ParsedVocabLine> items = parser.parse(rawText);

        assertThat(items).hasSize(3);
        assertThat(items.get(0).lineNumber()).isEqualTo(1);
        assertThat(items.get(0).error()).isEqualTo("Missing word");
        assertThat(items.get(1).lineNumber()).isEqualTo(2);
        assertThat(items.get(1).error()).isEqualTo("Pipe format must be: word | pos | meaning");
        assertThat(items.get(2).lineNumber()).isEqualTo(3);
        assertThat(items.get(2).error()).isEqualTo("Missing meaning");
    }
}

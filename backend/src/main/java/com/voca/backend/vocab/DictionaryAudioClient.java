package com.voca.backend.vocab;

import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class DictionaryAudioClient {

    private final RestClient restClient;

    public DictionaryAudioClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder
                .baseUrl("https://api.dictionaryapi.dev/api/v2")
                .build();
    }

    @SuppressWarnings("unchecked")
    public AudioLookupResult lookup(String word) {
        List<Map<String, Object>> entries = restClient.get()
                .uri("/entries/en/{word}", word)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                    throw new AudioNotFoundException();
                })
                .body(List.class);

        if (entries == null || entries.isEmpty()) {
            return AudioLookupResult.empty();
        }

        String audioUsUrl = null;
        String audioUkUrl = null;
        String firstAudioUrl = null;

        for (Map<String, Object> entry : entries) {
            Object phoneticsValue = entry.get("phonetics");
            if (!(phoneticsValue instanceof List<?> phonetics)) {
                continue;
            }

            for (Object value : phonetics) {
                if (!(value instanceof Map<?, ?> phonetic)) {
                    continue;
                }

                String audio = asText(phonetic.get("audio"));
                if (audio == null || audio.isBlank()) {
                    continue;
                }

                String normalizedAudio = normalizeAudioUrl(audio);
                if (firstAudioUrl == null) {
                    firstAudioUrl = normalizedAudio;
                }

                String sourceUrl = asText(phonetic.get("sourceUrl"));
                String licenseName = null;
                Object licenseValue = phonetic.get("license");
                if (licenseValue instanceof Map<?, ?> license) {
                    licenseName = asText(license.get("name"));
                }
                String searchable = ((sourceUrl == null ? "" : sourceUrl) + " " + normalizedAudio + " " + (licenseName == null ? "" : licenseName))
                        .toLowerCase();

                if (audioUsUrl == null && (searchable.contains("-us.") || searchable.contains("_us.") || searchable.contains("us_pron") || searchable.contains("us-"))) {
                    audioUsUrl = normalizedAudio;
                }
                if (audioUkUrl == null && (searchable.contains("-uk.") || searchable.contains("_uk.") || searchable.contains("uk_pron") || searchable.contains("uk-"))){
                    audioUkUrl = normalizedAudio;
                }
            }
        }

        String audioUrl = audioUsUrl != null ? audioUsUrl : firstAudioUrl;
        String accent = audioUrl == null ? null : (audioUrl.equals(audioUsUrl) ? "US" : audioUrl.equals(audioUkUrl) ? "UK" : "DEFAULT");
        return new AudioLookupResult(audioUrl, audioUsUrl, audioUkUrl, accent);
    }

    private String asText(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }

    private String normalizeAudioUrl(String audio) {
        if (audio.startsWith("//")) {
            return "https:" + audio;
        }
        return audio;
    }

    public record AudioLookupResult(String audioUrl, String audioUsUrl, String audioUkUrl, String audioAccent) {
        static AudioLookupResult empty() {
            return new AudioLookupResult(null, null, null, null);
        }
    }

    static class AudioNotFoundException extends RuntimeException {
    }
}

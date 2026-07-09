# ETS 2026 Test 7

Raw and mapped public data fetched from ChamTOEIC for local import/rendering.

## Main File

- `ets-2026-test-7-full.json`: canonical full JSON for app import/rendering. It contains source
  metadata, test overview, counts, complete question groups, answer keys, media URLs, transcripts,
  explanations, and a flat media list.

## Supporting Files

- `overview.json`: public test metadata.
- `questions.json`: public questions payload without answer keys.
- `questions-show-answers.json`: public `show_answers=true` payload with answer keys, transcripts,
  and explanations.
- `answer-key.json`: extracted answer key for all 200 questions.
- `questions-with-answer-key.json`: questions with `answers[].isCorrect` filled.
- `questions-with-answer-key-and-media.json`: questions with answer keys and media mapped into each
  question.
- `questions-complete-mapped.json`: questions with answer keys, media, transcripts, and explanations
  mapped into each question.
- `media-links.json`, `media-links.csv`, `media-links.txt`: extracted audio/image URL lists.
- `question-media-map.json`: flat per-question media map.
- `transcript-map.json`: flat per-question transcript/explanation map.
- `sample-one-question-per-part.json`: one representative question from each TOEIC part.

## Counts

- Question groups: 103
- Questions: 200
- Answer choices: 775
- Correct answers: 200
- Media files: 90
- Audio files: 54
- Image files: 36
- Questions with media: 170
- Questions with audio: 100
- Questions with image: 91
- Questions with audio transcript: 100
- Questions with group explanation: 200

## Refresh

From the repository root:

```powershell
powershell -ExecutionPolicy Bypass -File imports\chamtoeic\ets-2026-test-7\fetch.ps1
powershell -ExecutionPolicy Bypass -File imports\chamtoeic\ets-2026-test-7\submit-one-answer.ps1
powershell -ExecutionPolicy Bypass -File imports\chamtoeic\ets-2026-test-7\map-media-to-questions.ps1
powershell -ExecutionPolicy Bypass -File imports\chamtoeic\ets-2026-test-7\fetch-transcripts-and-map.ps1
powershell -ExecutionPolicy Bypass -File imports\chamtoeic\ets-2026-test-7\build-full-json.ps1
```

# ETS 2026 Test 1

Raw public data fetched from ChamTOEIC for local import experiments.

## Source

- Page: https://chamtoeic.edu.vn/tests/ets-2026-test-1
- Overview API: https://chamtoeic.edu.vn/api/v1/question-bank-service/test-sets/accessible/ets-2026-test-1/overview
- Questions API: https://chamtoeic.edu.vn/api/v1/question-bank-service/test-sets/accessible/ets-2026-test-1/questions

## Files

- `ets-2026-test-1-full.json`: canonical full JSON for app import/rendering. It contains source
  metadata, test overview, counts, complete question groups, answer keys, media URLs, transcripts,
  explanations, and a flat media list.
- `overview.json`: public test metadata.
- `questions.json`: public question groups, questions, answers, and remote media URLs.
- `summary.json`: generated local stats for quick inspection.
- `submit-one-answer-request.json`: anonymous public submit payload with only the first question answered.
- `submit-one-answer-response.json`: public submit response containing answer details.
- `answer-key.json`: extracted correct answer key for all 200 questions.
- `questions-with-answer-key.json`: original questions payload with `answers[].isCorrect` filled and
  question-level `correctAnswerId`, `correctAnswerOrder`, `correctAnswerLabel`, and `tags` added.
- `sample-one-question-per-part.json`: one representative question from each TOEIC part for checking
  whether the data is enough to render a test UI.
- `media-links.json`: extracted audio/image URLs with part, group order, file IDs, HTTP status, and
  content metadata.
- `media-links.csv`: the same media URL list in CSV form.
- `media-links.txt`: flat media URL list for quick copy/paste.
- `questions-with-answer-key-and-media.json`: original question payload with answer keys and group
  media copied into each question as `mediaFiles`, `audioUrls`, and `imageUrls`.
- `question-media-map.json`: flat per-question media map for UI/debugging.
- `map-media-to-questions.ps1`: regenerates the two question-level media mapping files.
- `questions-show-answers.json`: raw public `show_answers=true` payload containing answer keys,
  group explanations, and listening transcripts.
- `questions-complete-mapped.json`: full mapped payload for rendering. Each question includes answer
  key fields, media fields, `audioTranscriptHtml`, `audioTranscriptText`, `groupExplanationHtml`, and
  `groupExplanationText`.
- `transcript-map.json`: flat per-question transcript/explanation map.
- `fetch-transcripts-and-map.ps1`: fetches the `show_answers=true` payload and regenerates the
  complete mapped transcript files.
- `build-full-json.ps1`: builds `ets-2026-test-1-full.json` from the generated artifacts.

## Notes

The public questions payload does not include answer keys or explanations. A public anonymous submit
request returns correct answer IDs for all 200 questions, so `answer-key.json` and
`questions-with-answer-key.json` include the answer key.

The public submit response still does not include explanations. In the generated answer key,
`explanation` is currently `null` for all 200 questions.

Media files are referenced by remote URLs in `questions.json`; they have not been downloaded into
this repo. The extracted media list currently has 92 URLs: 54 audio files and 38 image files. All
92 returned HTTP OK when checked.

Media is originally stored at question-group level. For easier rendering, the mapped files copy the
same group media onto every question in that group. Part 5 has no media, so those questions have
empty `mediaFiles`, `audioUrls`, and `imageUrls`.

Transcripts and explanations are also stored at question-group level in the public `show_answers=true`
payload. The complete mapped files copy the group transcript/explanation onto every question in that
group. Current transcript counts: 54 groups with audio transcripts, mapped to 100 listening
questions. All 103 groups have explanations, mapped to all 200 questions.

## Refresh

From the repository root:

```powershell
$dir = "imports\chamtoeic\ets-2026-test-1"
Invoke-WebRequest -Uri "https://chamtoeic.edu.vn/api/v1/question-bank-service/test-sets/accessible/ets-2026-test-1/overview" -UseBasicParsing -Headers @{Accept="application/json"} -OutFile "$dir\overview.json"
Invoke-WebRequest -Uri "https://chamtoeic.edu.vn/api/v1/question-bank-service/test-sets/accessible/ets-2026-test-1/questions" -UseBasicParsing -Headers @{Accept="application/json"} -OutFile "$dir\questions.json"
```

Or run the maintained scripts:

```powershell
powershell -ExecutionPolicy Bypass -File imports\chamtoeic\ets-2026-test-1\fetch.ps1
powershell -ExecutionPolicy Bypass -File imports\chamtoeic\ets-2026-test-1\submit-one-answer.ps1
powershell -ExecutionPolicy Bypass -File imports\chamtoeic\ets-2026-test-1\fetch-transcripts-and-map.ps1
powershell -ExecutionPolicy Bypass -File imports\chamtoeic\ets-2026-test-1\build-full-json.ps1
```

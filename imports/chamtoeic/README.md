# ChamTOEIC Imports

Fetched and mapped public TOEIC test data from ChamTOEIC for local import/rendering.

## Validation Status

- Generated at: `2026-07-09T20:52:34Z`
- Total tests: `29`
- Total questions: `5800`
- Correct answers: `5800`
- Media files: `2656` (`1566` audio, `1090` image)
- Failed media links: `0`
- Mapping validation failures: `0`

Validation checks compare each `*-full.json` against `questions-show-answers.json` and `media-links.json`:

- each question has exactly one correct answer and `correctAnswerId` matches the raw source
- question-level `mediaFiles` match the source question group media
- every mapped media URL exists in `media-links.json`
- `audioTranscriptHtml` matches the source group transcript
- `groupExplanationHtml` matches the source group explanation
- media URLs return HTTP OK

## Collection Totals

| Collection | Tests | Questions | Media | Validation failures |
|---|---:|---:|---:|---:|
| ETS 2023 | 9 | 1800 | 828 | 0 |
| ETS 2024 | 10 | 2000 | 912 | 0 |
| ETS 2026 | 10 | 2000 | 916 | 0 |

## Test Files

| Collection | Test | Slug | Full JSON | Media | Audio transcript questions | Explanation questions | Validation |
|---|---:|---|---|---:|---:|---:|---:|
| ETS 2023 | 2 | `ets2023-test-2` | `ets2023-test-2/ets2023-test-2-full.json` | 92 | 100 | 200 | 0 |
| ETS 2023 | 3 | `ets-2023-test-3` | `ets-2023-test-3/ets-2023-test-3-full.json` | 92 | 100 | 197 | 0 |
| ETS 2023 | 4 | `ets-2023-test-4` | `ets-2023-test-4/ets-2023-test-4-full.json` | 91 | 104 | 197 | 0 |
| ETS 2023 | 5 | `ets-2023-test-5` | `ets-2023-test-5/ets-2023-test-5-full.json` | 93 | 100 | 100 | 0 |
| ETS 2023 | 6 | `ets-2023-test-6` | `ets-2023-test-6/ets-2023-test-6-full.json` | 92 | 100 | 194 | 0 |
| ETS 2023 | 7 | `ets-2023-test-7` | `ets-2023-test-7/ets-2023-test-7-full.json` | 92 | 100 | 194 | 0 |
| ETS 2023 | 8 | `ets-2023-test-8` | `ets-2023-test-8/ets-2023-test-8-full.json` | 92 | 101 | 128 | 0 |
| ETS 2023 | 9 | `ets-2023-test-9` | `ets-2023-test-9/ets-2023-test-9-full.json` | 92 | 100 | 199 | 0 |
| ETS 2023 | 10 | `ets-2023-test-10` | `ets-2023-test-10/ets-2023-test-10-full.json` | 92 | 100 | 194 | 0 |
| ETS 2024 | 1 | `ets-2024-test1` | `ets-2024-test1/ets-2024-test1-full.json` | 92 | 100 | 200 | 0 |
| ETS 2024 | 2 | `ets2024-test-2` | `ets2024-test-2/ets2024-test-2-full.json` | 92 | 100 | 200 | 0 |
| ETS 2024 | 3 | `ets2024-test-3` | `ets2024-test-3/ets2024-test-3-full.json` | 92 | 100 | 200 | 0 |
| ETS 2024 | 4 | `ets2024-test-4` | `ets2024-test-4/ets2024-test-4-full.json` | 92 | 100 | 200 | 0 |
| ETS 2024 | 5 | `ets-2024-test-5` | `ets-2024-test-5/ets-2024-test-5-full.json` | 92 | 100 | 200 | 0 |
| ETS 2024 | 6 | `ets-2024-test-6` | `ets-2024-test-6/ets-2024-test-6-full.json` | 92 | 100 | 200 | 0 |
| ETS 2024 | 7 | `ets-2024-test-7` | `ets-2024-test-7/ets-2024-test-7-full.json` | 92 | 100 | 200 | 0 |
| ETS 2024 | 8 | `ets-2024-test-8` | `ets-2024-test-8/ets-2024-test-8-full.json` | 87 | 100 | 200 | 0 |
| ETS 2024 | 9 | `ets-2024-test-9` | `ets-2024-test-9/ets-2024-test-9-full.json` | 89 | 100 | 200 | 0 |
| ETS 2024 | 10 | `ets2024-test-10` | `ets2024-test-10/ets2024-test-10-full.json` | 92 | 100 | 200 | 0 |
| ETS 2026 | 1 | `ets-2026-test-1` | `ets-2026-test-1/ets-2026-test-1-full.json` | 92 | 100 | 200 | 0 |
| ETS 2026 | 2 | `ets-2026-test-2` | `ets-2026-test-2/ets-2026-test-2-full.json` | 92 | 100 | 200 | 0 |
| ETS 2026 | 3 | `ets-2026-test-3` | `ets-2026-test-3/ets-2026-test-3-full.json` | 92 | 100 | 200 | 0 |
| ETS 2026 | 4 | `ets-2026-test-4` | `ets-2026-test-4/ets-2026-test-4-full.json` | 92 | 100 | 200 | 0 |
| ETS 2026 | 5 | `ets-2026-test-5` | `ets-2026-test-5/ets-2026-test-5-full.json` | 92 | 100 | 199 | 0 |
| ETS 2026 | 6 | `ets-2026-test-6` | `ets-2026-test-6/ets-2026-test-6-full.json` | 91 | 100 | 200 | 0 |
| ETS 2026 | 7 | `ets-2026-test-7` | `ets-2026-test-7/ets-2026-test-7-full.json` | 90 | 100 | 200 | 0 |
| ETS 2026 | 8 | `ets-2026-test-8` | `ets-2026-test-8/ets-2026-test-8-full.json` | 91 | 100 | 200 | 0 |
| ETS 2026 | 9 | `ets-2026-test-9` | `ets-2026-test-9/ets-2026-test-9-full.json` | 92 | 100 | 200 | 0 |
| ETS 2026 | 10 | `ets-2026-test-10` | `ets-2026-test-10/ets-2026-test-10-full.json` | 92 | 100 | 200 | 0 |

## Summary Files

- `validation-summary.json`: global validation result for every imported test.
- `validation-summary.csv`: CSV form of the global validation result.
- `ets-2023-summary.json` / `ets-2023-summary.csv`: ETS 2023 collection summary.
- `ets-2024-summary.json` / `ets-2024-summary.csv`: ETS 2024 collection summary.

## Notes

- ETS 2023 starts here from test 2 because that was the requested range.
- Slugs are not fully consistent across years. Use the folder names in this README instead of guessing URL slugs.
- Some ETS 2023 tests have fewer explanation texts than questions because the public source omits or leaves blank some group explanations.
- Media is stored at group level in the source and copied to each question in mapped/full JSON files.

"use client";

import { useMemo, useState } from "react";
import { Braces, Check, CircleHelp, Eye, ListChecks, Send, X } from "lucide-react";
import {
  answerQuizQuestion,
  createQuizAttempt,
  createQuizImportAttempt,
  createManualQuizAttempt,
  generateQuiz,
  getQuizResult,
  previewQuizImport,
  QuizAnswer,
  QuizAttempt,
  ManualQuizPayload,
  QuizImportPreview,
  QuizQuestion,
  QuizQuestionType,
  QuizResult
} from "@/lib/api";

type QuizPanelProps = {
  token: string;
  deckId: string;
  totalWords: number;
  refreshDeck: () => void;
};

export function QuizPanel({ token, deckId, totalWords, refreshDeck }: QuizPanelProps) {
  const manualSample = `{
  "questionTypes": ["CLOZE_CHOICE"],
  "limit": 20,
  "vocabPairs": [
    { "word": "absent", "meaning": "vang mat" },
    { "word": "accumulate", "meaning": "tich luy" }
  ]
}`;
  const bulkSample = `absent -- He was ____ from class yesterday.
accumulate -- Dust can accumulate on the shelf.
adhere -- Please adhere to the safety rules.
adjacent -- The hotel is adjacent to the station.`;
  const [attempt, setAttempt] = useState<QuizAttempt | null>(null);
  const [answers, setAnswers] = useState<Record<number, QuizAnswer>>({});
  const [selectedAnswers, setSelectedAnswers] = useState<Record<number, string>>({});
  const [matchingPairs, setMatchingPairs] = useState<Record<number, Record<string, string>>>({});
  const [currentIndex, setCurrentIndex] = useState(0);
  const [questionStartedAt, setQuestionStartedAt] = useState(Date.now());
  const [result, setResult] = useState<QuizResult | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState("");
  const [showManualJson, setShowManualJson] = useState(false);
  const [manualJson, setManualJson] = useState(manualSample);
  const [showBulkImport, setShowBulkImport] = useState(false);
  const [bulkText, setBulkText] = useState("");
  const [bulkPreview, setBulkPreview] = useState<QuizImportPreview | null>(null);
  const [bulkTypes, setBulkTypes] = useState<QuizQuestionType[]>(["CLOZE_CHOICE", "CHOOSE_MEANING"]);
  const [bulkLimit, setBulkLimit] = useState(10);

  const currentQuestion = useMemo(() => {
    if (!attempt || attempt.questions.length === 0) {
      return null;
    }
    return attempt.questions[Math.min(currentIndex, attempt.questions.length - 1)];
  }, [attempt, currentIndex]);

  function resetRunState() {
    setResult(null);
    setAnswers({});
    setSelectedAnswers({});
    setMatchingPairs({});
    setCurrentIndex(0);
    setQuestionStartedAt(Date.now());
  }

  async function startQuiz() {
    setIsLoading(true);
    setError("");
    resetRunState();

    try {
      const generated = await generateQuiz(token, deckId);
      const nextAttempt = await createQuizAttempt(
        token,
        deckId,
        generated.questions.map((question) => question.id)
      );
      setAttempt(nextAttempt);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not start quiz");
    } finally {
      setIsLoading(false);
    }
  }

  async function startManualQuiz() {
    setIsLoading(true);
    setError("");
    resetRunState();

    try {
      const payload = JSON.parse(manualJson) as ManualQuizPayload;
      const nextAttempt = await createManualQuizAttempt(token, deckId, payload);
      setAttempt(nextAttempt);
    } catch (err) {
      if (err instanceof SyntaxError) {
        setError("Manual quiz JSON is invalid");
      } else {
        setError(err instanceof Error ? err.message : "Could not start manual quiz");
      }
    } finally {
      setIsLoading(false);
    }
  }

  async function previewBulkQuiz() {
    setIsLoading(true);
    setError("");

    try {
      const nextPreview = await previewQuizImport(token, deckId, {
        rawText: bulkText,
        questionTypes: bulkTypes
      });
      setBulkPreview(nextPreview);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not preview quiz import");
    } finally {
      setIsLoading(false);
    }
  }

  async function startBulkQuiz() {
    setIsLoading(true);
    setError("");
    resetRunState();

    try {
      const nextAttempt = await createQuizImportAttempt(token, deckId, {
        rawText: bulkText,
        questionTypes: bulkTypes,
        limit: bulkLimit
      });
      setAttempt(nextAttempt);
      setShowBulkImport(false);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not start imported quiz");
    } finally {
      setIsLoading(false);
    }
  }

  async function submitAnswer(question: QuizQuestion) {
    const answer = question.type === "MATCHING"
      ? JSON.stringify(matchingPairs[question.id] ?? {})
      : selectedAnswers[question.id]?.trim() ?? "";
    if (!answer) {
      setError("Choose or enter an answer first");
      return;
    }
    if (question.type === "MATCHING") {
      const options = matchingOptions(question);
      const pairs = matchingPairs[question.id] ?? {};
      if (!options || options.words.some((word) => !pairs[word])) {
        setError("Match every word before submitting");
        return;
      }
    }
    if (!attempt) {
      return;
    }

    setIsLoading(true);
    setError("");
    try {
      const response = await answerQuizQuestion(token, attempt.id, question.id, answer, Date.now() - questionStartedAt);
      setAnswers((current) => ({ ...current, [question.id]: response }));
      await refreshDeck();
      if (Object.keys(answers).length + 1 >= attempt.totalQuestions) {
        const nextResult = await getQuizResult(token, attempt.id);
        setResult(nextResult);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not submit answer");
    } finally {
      setIsLoading(false);
    }
  }

  async function loadResult() {
    if (!attempt) {
      return;
    }
    setIsLoading(true);
    setError("");
    try {
      setResult(await getQuizResult(token, attempt.id));
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not load result");
    } finally {
      setIsLoading(false);
    }
  }

  const answeredCount = Object.keys(answers).length;
  const currentAnswer = currentQuestion ? answers[currentQuestion.id] : null;

  return (
    <section className="card quiz-card">
      <div className="section-heading">
        <div>
          <h2>Quiz</h2>
          <p>{attempt ? `${answeredCount}/${attempt.totalQuestions} answered` : "One 4-option question per word"}</p>
        </div>
        <div className="button-row">
          <button className="button" type="button" onClick={startQuiz} disabled={isLoading || totalWords < 4}>
            <ListChecks size={18} aria-hidden="true" />
            {attempt ? "New Quiz" : "Start Quiz"}
          </button>
          <button
            className="button secondary-button"
            type="button"
            onClick={() => setShowManualJson((current) => !current)}
            disabled={isLoading || totalWords < 2}
          >
            <Braces size={18} aria-hidden="true" />
            Manual JSON
          </button>
          <button
            className="button secondary-button"
            type="button"
            onClick={() => setShowBulkImport((current) => !current)}
            disabled={isLoading || totalWords < 4}
          >
            <Eye size={18} aria-hidden="true" />
            Bulk Quiz
          </button>
        </div>
      </div>

      {error && <p className="form-error">{error}</p>}
      {totalWords < 4 && <p className="form-muted">Need at least 4 vocabulary items with meanings for 4 answer choices.</p>}

      {showManualJson && (
        <div className="quiz-manual-panel">
          <div className="field">
            <label htmlFor="manual-quiz-json">Quiz JSON</label>
            <textarea
              id="manual-quiz-json"
              className="quiz-json-input"
              value={manualJson}
              onChange={(event) => setManualJson(event.target.value)}
              spellCheck={false}
            />
          </div>
          <div className="button-row">
            <button className="button" type="button" onClick={startManualQuiz} disabled={isLoading || totalWords < 2}>
              <Braces size={18} aria-hidden="true" />
              Start Manual Quiz
            </button>
            <button className="button secondary-button" type="button" onClick={() => setManualJson(manualSample)} disabled={isLoading}>
              Reset Sample
            </button>
          </div>
        </div>
      )}

      {showBulkImport && (
        <div className="quiz-manual-panel">
          <div className="field">
            <label htmlFor="bulk-quiz-text">Bulk quiz lines</label>
            <textarea
              id="bulk-quiz-text"
              className="quiz-json-input"
              value={bulkText}
              onChange={(event) => {
                setBulkText(event.target.value);
                setBulkPreview(null);
              }}
              placeholder={bulkSample}
              spellCheck={false}
            />
          </div>

          <div className="quiz-type-row">
            <label>
              Questions
              <input
                className="quiz-count-input"
                type="number"
                min={1}
                max={100}
                value={bulkLimit}
                onChange={(event) => setBulkLimit(Math.max(1, Number(event.target.value) || 1))}
              />
            </label>
            <label>
              <input
                type="checkbox"
                checked={bulkTypes.includes("CLOZE_CHOICE")}
                onChange={() => setBulkTypes((current) => toggleQuizType(current, "CLOZE_CHOICE"))}
              />
              Fill blank
            </label>
            <label>
              <input
                type="checkbox"
                checked={bulkTypes.includes("CHOOSE_MEANING")}
                onChange={() => setBulkTypes((current) => toggleQuizType(current, "CHOOSE_MEANING"))}
              />
              Choose meaning
            </label>
            <label>
              <input
                type="checkbox"
                checked={bulkTypes.includes("MATCHING")}
                onChange={() => setBulkTypes((current) => toggleQuizType(current, "MATCHING"))}
              />
              Matching
            </label>
          </div>

          <div className="button-row">
            <button className="button" type="button" onClick={previewBulkQuiz} disabled={isLoading || bulkText.trim().length === 0 || bulkTypes.length === 0}>
              <Eye size={18} aria-hidden="true" />
              Preview
            </button>
            <button
              className="button"
              type="button"
              onClick={startBulkQuiz}
              disabled={isLoading || !bulkPreview || bulkPreview.validCount === 0 || bulkTypes.length === 0}
            >
              <ListChecks size={18} aria-hidden="true" />
              Start {bulkLimit} Quiz
            </button>
            <button className="button secondary-button" type="button" onClick={() => setBulkText(bulkSample)} disabled={isLoading}>
              Reset Sample
            </button>
          </div>

          {bulkPreview && (
            <div className="preview-wrap">
              <p className="form-muted">
                {bulkPreview.validCount} ready, {bulkPreview.skippedCount} skipped, {bulkPreview.errorCount} error{bulkPreview.errorCount === 1 ? "" : "s"}.
              </p>
              <table className="preview-table">
                <thead>
                  <tr>
                    <th>Line</th>
                    <th>Word</th>
                    <th>Sentence</th>
                    <th>Status</th>
                  </tr>
                </thead>
                <tbody>
                  {bulkPreview.items.map((item) => (
                    <tr key={`${item.lineNumber}-${item.word ?? "line"}`}>
                      <td>{item.lineNumber}</td>
                      <td>{item.word || "-"}</td>
                      <td>{item.prompt || "-"}</td>
                      <td>
                        <span className={`status-pill ${item.status === "OK" ? "ok" : item.status === "ERROR" ? "bad" : "neutral"}`}>
                          {item.status}
                        </span>
                        {item.message && <span className="status-message">{item.message}</span>}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {currentQuestion && (
        <div className="quiz-runner">
          <div className="quiz-progress-row">
            <span>Question {currentIndex + 1} / {attempt?.totalQuestions}</span>
            <span className="status-pill neutral">{questionLabel(currentQuestion.type)}</span>
          </div>

          <h3>{currentQuestion.prompt}</h3>

          {isChoiceQuestion(currentQuestion) ? (
            <div className="quiz-options">
              {choiceOptions(currentQuestion).map((option, index) => (
                <button
                  key={option}
                  className={`quiz-option ${selectedAnswers[currentQuestion.id] === option ? "selected" : ""}`}
                  type="button"
                  onClick={() => setSelectedAnswers((current) => ({ ...current, [currentQuestion.id]: option }))}
                  disabled={Boolean(currentAnswer)}
                >
                  <span className="quiz-option-letter">{String.fromCharCode(65 + index)}</span>
                  {option}
                </button>
              ))}
            </div>
          ) : currentQuestion.type === "MATCHING" ? (
            <div className="matching-grid">
              {(matchingOptions(currentQuestion)?.words ?? []).map((word) => (
                <div key={word} className="matching-row">
                  <strong>{word}</strong>
                  <select
                    value={matchingPairs[currentQuestion.id]?.[word] ?? ""}
                    onChange={(event) =>
                      setMatchingPairs((current) => ({
                        ...current,
                        [currentQuestion.id]: {
                          ...(current[currentQuestion.id] ?? {}),
                          [word]: event.target.value
                        }
                      }))
                    }
                    disabled={Boolean(currentAnswer)}
                  >
                    <option value="">Choose meaning</option>
                    {(matchingOptions(currentQuestion)?.meanings ?? []).map((meaning) => (
                      <option key={meaning} value={meaning}>{meaning}</option>
                    ))}
                  </select>
                </div>
              ))}
            </div>
          ) : (
            <div className="field">
              <label htmlFor={`quiz-answer-${currentQuestion.id}`}>Answer</label>
              <input
                id={`quiz-answer-${currentQuestion.id}`}
                value={selectedAnswers[currentQuestion.id] ?? ""}
                onChange={(event) =>
                  setSelectedAnswers((current) => ({ ...current, [currentQuestion.id]: event.target.value }))
                }
                disabled={Boolean(currentAnswer)}
              />
            </div>
          )}

          {currentAnswer && (
            <div className={`quiz-feedback ${currentAnswer.correct ? "correct" : "incorrect"}`}>
              <strong>
                {currentAnswer.correct ? <Check size={18} aria-hidden="true" /> : <X size={18} aria-hidden="true" />}
                {currentAnswer.correct ? "Correct" : "Incorrect"}
              </strong>
              <p>{currentAnswer.explanation}</p>
              {!currentAnswer.correct && currentQuestion.type !== "MATCHING" && <p>Correct answer: {currentAnswer.correctAnswer}</p>}
            </div>
          )}

          <div className="button-row">
            {!currentAnswer ? (
              <button className="button" type="button" onClick={() => submitAnswer(currentQuestion)} disabled={isLoading}>
                <Send size={18} aria-hidden="true" />
                Submit
              </button>
            ) : (
              <button
                className="button"
                type="button"
                onClick={() => {
                  setCurrentIndex((index) => Math.min(index + 1, (attempt?.totalQuestions ?? 1) - 1));
                  setQuestionStartedAt(Date.now());
                }}
                disabled={currentIndex + 1 >= (attempt?.totalQuestions ?? 0)}
              >
                Next
              </button>
            )}
            {attempt && answeredCount > 0 && (
              <button className="button secondary-button" type="button" onClick={loadResult} disabled={isLoading}>
                <CircleHelp size={18} aria-hidden="true" />
                Result
              </button>
            )}
          </div>
        </div>
      )}

      {result && (
        <div className="quiz-result">
          <div>
            <span className="result-score">{result.scorePercent}%</span>
            <p>{result.correctCount}/{result.totalQuestions} correct</p>
          </div>
          <div className="quiz-answer-list">
            {result.answers.map((answer) => (
              <div key={answer.questionId} className="quiz-answer-row">
                <span className={`status-pill ${answer.correct ? "ok" : "neutral"}`}>
                  {answer.correct ? "Correct" : "Review"}
                </span>
                <p>{answer.explanation}</p>
              </div>
            ))}
          </div>
        </div>
      )}
    </section>
  );
}

function questionLabel(type: string) {
  if (type === "CHOOSE_MEANING") return "Choose meaning";
  if (type === "CLOZE_CHOICE") return "Fill blank";
  if (type === "TRUE_FALSE") return "True / False";
  if (type === "MATCHING") return "Matching";
  return "Type answer";
}

function isChoiceQuestion(question: QuizQuestion) {
  return question.type === "CHOOSE_MEANING" || question.type === "CLOZE_CHOICE" || question.type === "TRUE_FALSE";
}

function choiceOptions(question: QuizQuestion) {
  return Array.isArray(question.options) ? question.options : [];
}

function matchingOptions(question: QuizQuestion) {
  if (question.type !== "MATCHING" || Array.isArray(question.options)) {
    return null;
  }
  return question.options;
}

function toggleQuizType(current: QuizQuestionType[], type: QuizQuestionType) {
  if (current.includes(type)) {
    return current.filter((item) => item !== type);
  }
  return [...current, type];
}

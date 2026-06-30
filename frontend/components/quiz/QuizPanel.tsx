"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { Check, ChevronRight, CircleHelp, Eye, ListChecks, Send, Volume2, X } from "lucide-react";
import {
  answerQuizQuestion,
  createQuizAttempt,
  createQuizImportAttempt,
  generateQuiz,
  getQuizResult,
  previewQuizImport,
  QuizAnswer,
  QuizAttempt,
  QuizImportPreview,
  QuizQuestion,
  QuizQuestionType,
  QuizResult
} from "@/lib/api";
import { computeDiff, DiffSegment } from "@/lib/diffUtil";

type QuizPanelProps = {
  token: string;
  deckId: string;
  totalWords: number;
  refreshDeck: () => void;
};

function playEnglishPronunciation(text: string) {
  if (typeof window === "undefined" || !("speechSynthesis" in window)) return;
  window.speechSynthesis.cancel();
  const utterance = new SpeechSynthesisUtterance(text);
  utterance.lang = "en-US";
  utterance.rate = 0.92;
  window.speechSynthesis.speak(utterance);
}

function PronounceButton({ text, className = "" }: { text: string; className?: string }) {
  return (
    <button
      type="button"
      className={`learn-audio-btn ${className}`}
      onClick={(event) => {
        event.stopPropagation();
        playEnglishPronunciation(text);
      }}
      aria-label={`Nghe phát âm ${text}`}
      title={`Nghe phát âm ${text}`}
    >
      <Volume2 size={18} aria-hidden="true" />
    </button>
  );
}

function DiffDisplay({ segments }: { segments: DiffSegment[] }) {
  return (
    <span className="learn-diff-text">
      {segments.map((seg, i) => (
        <span key={i} className={`learn-diff-${seg.type}`}>
          {seg.text}
        </span>
      ))}
    </span>
  );
}

export function QuizPanel({ token, deckId, totalWords, refreshDeck }: QuizPanelProps) {
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
  const [showBulkImport, setShowBulkImport] = useState(false);
  const [bulkText, setBulkText] = useState("");
  const [bulkPreview, setBulkPreview] = useState<QuizImportPreview | null>(null);
  const [bulkTypes, setBulkTypes] = useState<QuizQuestionType[]>(["CLOZE_CHOICE", "CHOOSE_MEANING"]);
  const [bulkLimit, setBulkLimit] = useState(10);
  const autoAdvanceTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  const currentQuestion = useMemo(() => {
    if (!attempt || attempt.questions.length === 0) {
      return null;
    }
    return attempt.questions[Math.min(currentIndex, attempt.questions.length - 1)];
  }, [attempt, currentIndex]);

  const currentAnswer = currentQuestion ? answers[currentQuestion.id] : null;
  const answeredCount = Object.keys(answers).length;

  // Mirror the backend's question-generation math so the user knows how many
  // questions they'll actually get: each valid word yields one question per
  // non-matching type, plus one matching question per group of 2-5 words.
  const bulkEstimate = useMemo(() => {
    if (!bulkPreview) {
      return null;
    }
    const validWords = bulkPreview.validCount;
    if (validWords === 0) {
      return { total: 0, capped: 0, validWords: 0 };
    }
    const nonMatchingTypes = bulkTypes.filter((type) => type !== "MATCHING").length;
    let matchingGroups = 0;
    if (bulkTypes.includes("MATCHING")) {
      for (let i = 0; i < validWords; i += 5) {
        if (Math.min(5, validWords - i) >= 2) {
          matchingGroups += 1;
        }
      }
    }
    const total = validWords * nonMatchingTypes + matchingGroups;
    const capped = Math.min(total, Math.max(1, bulkLimit));
    return { total, capped, validWords };
  }, [bulkPreview, bulkTypes, bulkLimit]);

  const rawQuestionCount = bulkEstimate?.total ?? 0;
  const estimatedQuestionCount = bulkEstimate?.capped ?? 0;

  function clearAutoAdvance() {
    if (autoAdvanceTimer.current) {
      clearTimeout(autoAdvanceTimer.current);
      autoAdvanceTimer.current = null;
    }
  }

  function resetRunState() {
    clearAutoAdvance();
    setResult(null);
    setAnswers({});
    setSelectedAnswers({});
    setMatchingPairs({});
    setCurrentIndex(0);
    setQuestionStartedAt(Date.now());
  }

  const goNext = useCallback(() => {
    clearAutoAdvance();
    setCurrentIndex((index) => Math.min(index + 1, (attempt?.totalQuestions ?? 1) - 1));
    setQuestionStartedAt(Date.now());
  }, [attempt]);

  useEffect(() => {
    return () => clearAutoAdvance();
  }, []);

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
      setError(err instanceof Error ? err.message : "Không thể bắt đầu quiz");
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
      setError(err instanceof Error ? err.message : "Không thể xem trước quiz");
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
      setError(err instanceof Error ? err.message : "Không thể bắt đầu quiz nhập nhanh");
    } finally {
      setIsLoading(false);
    }
  }

  const submitAnswer = useCallback(
    async (question: QuizQuestion) => {
      const answer = question.type === "MATCHING"
        ? JSON.stringify(matchingPairs[question.id] ?? {})
        : selectedAnswers[question.id]?.trim() ?? "";
      if (!answer) {
        setError("Hãy chọn hoặc nhập đáp án trước");
        return;
      }
      if (question.type === "MATCHING") {
        const options = matchingOptions(question);
        const pairs = matchingPairs[question.id] ?? {};
        if (!options || options.words.some((word) => !pairs[word])) {
          setError("Hãy nối tất cả các từ trước khi gửi");
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
        const isLast = Object.keys(answers).length + 1 >= attempt.totalQuestions;
        if (isLast) {
          const nextResult = await getQuizResult(token, attempt.id);
          setResult(nextResult);
        } else if (response.correct) {
          clearAutoAdvance();
          autoAdvanceTimer.current = setTimeout(() => goNext(), 1200);
        }
      } catch (err) {
        setError(err instanceof Error ? err.message : "Không thể gửi đáp án");
      } finally {
        setIsLoading(false);
      }
    },
    [attempt, answers, matchingPairs, selectedAnswers, questionStartedAt, token, refreshDeck, goNext]
  );

  async function loadResult() {
    if (!attempt) {
      return;
    }
    setIsLoading(true);
    setError("");
    try {
      setResult(await getQuizResult(token, attempt.id));
    } catch (err) {
      setError(err instanceof Error ? err.message : "Không thể tải kết quả");
    } finally {
      setIsLoading(false);
    }
  }

  // Keyboard shortcuts: 1-9 to pick a choice, Enter to submit / advance.
  useEffect(() => {
    if (!currentQuestion || result) {
      return;
    }
    const handleKeyDown = (event: KeyboardEvent) => {
      const target = event.target as HTMLElement;
      const isInput = target.tagName === "INPUT" || target.tagName === "TEXTAREA" || target.tagName === "SELECT";

      if (event.key === "Enter") {
        if (currentAnswer) {
          event.preventDefault();
          if (currentIndex + 1 < (attempt?.totalQuestions ?? 0)) {
            goNext();
          }
        } else if (!isInput || target.tagName === "INPUT") {
          event.preventDefault();
          void submitAnswer(currentQuestion);
        }
        return;
      }

      if (!currentAnswer && !isInput && isChoiceQuestion(currentQuestion)) {
        const options = choiceOptions(currentQuestion);
        const num = parseInt(event.key, 10);
        if (num >= 1 && num <= options.length) {
          event.preventDefault();
          setSelectedAnswers((current) => ({ ...current, [currentQuestion.id]: options[num - 1] }));
        }
      }
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [currentQuestion, currentAnswer, currentIndex, attempt, result, goNext, submitAnswer]);

  const promptAudioText =
    currentQuestion && (currentQuestion.type === "CLOZE_CHOICE" || currentQuestion.type === "CHOOSE_MEANING")
      ? currentQuestion.prompt
      : null;

  const writtenDiff =
    currentQuestion &&
    currentAnswer &&
    !currentAnswer.correct &&
    currentQuestion.type === "FILL_IN_BLANK" &&
    currentAnswer.answer &&
    currentAnswer.correctAnswer
      ? computeDiff(currentAnswer.answer, currentAnswer.correctAnswer)
      : null;

  return (
    <section className="card quiz-card">
      <div className="section-heading">
        <div>
          <h2>Quiz</h2>
          <p>{attempt ? `Đã trả lời ${answeredCount}/${attempt.totalQuestions}` : "Mỗi từ một câu trắc nghiệm 4 lựa chọn"}</p>
        </div>
        <div className="button-row">
          <button className="button" type="button" onClick={startQuiz} disabled={isLoading || totalWords < 4}>
            <ListChecks size={18} aria-hidden="true" />
            {attempt ? "Quiz mới" : "Bắt đầu Quiz"}
          </button>
          <button
            className="button secondary-button"
            type="button"
            onClick={() => setShowBulkImport((current) => !current)}
            disabled={isLoading || totalWords < 4}
          >
            <Eye size={18} aria-hidden="true" />
            Quiz nhập nhanh
          </button>
        </div>
      </div>

      {error && <p className="form-error">{error}</p>}
      {totalWords < 4 && <p className="form-muted">Cần ít nhất 4 từ vựng có nghĩa để tạo 4 lựa chọn đáp án.</p>}

      {showBulkImport && (
        <div className="quiz-manual-panel">
          <div className="quiz-bulk-steps">
            <span className={`quiz-bulk-step ${bulkPreview ? "done" : "active"}`}>1 · Nhập &amp; xem trước</span>
            <ChevronRight size={14} aria-hidden="true" />
            <span className={`quiz-bulk-step ${bulkPreview ? "active" : ""}`}>2 · Xác nhận &amp; bắt đầu</span>
          </div>

          <div className="field">
            <label htmlFor="bulk-quiz-text">Danh sách từ &amp; câu ví dụ</label>
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
            <p className="form-muted">Mỗi dòng: <code>từ -- câu ví dụ</code> (từ phải có sẵn trong bộ thẻ này).</p>
          </div>

          <div className="quiz-type-row">
            <label>
              Giới hạn số câu
              <input
                className="quiz-count-input"
                type="number"
                min={1}
                max={100}
                value={bulkLimit}
                onChange={(event) => {
                  setBulkLimit(Math.max(1, Number(event.target.value) || 1));
                  setBulkPreview(null);
                }}
              />
            </label>
            <label>
              <input
                type="checkbox"
                checked={bulkTypes.includes("CLOZE_CHOICE")}
                onChange={() => {
                  setBulkTypes((current) => toggleQuizType(current, "CLOZE_CHOICE"));
                  setBulkPreview(null);
                }}
              />
              Điền từ
            </label>
            <label>
              <input
                type="checkbox"
                checked={bulkTypes.includes("CHOOSE_MEANING")}
                onChange={() => {
                  setBulkTypes((current) => toggleQuizType(current, "CHOOSE_MEANING"));
                  setBulkPreview(null);
                }}
              />
              Chọn nghĩa
            </label>
            <label>
              <input
                type="checkbox"
                checked={bulkTypes.includes("MATCHING")}
                onChange={() => {
                  setBulkTypes((current) => toggleQuizType(current, "MATCHING"));
                  setBulkPreview(null);
                }}
              />
              Nối từ
            </label>
          </div>

          {bulkTypes.length === 0 && <p className="form-muted">Chọn ít nhất một dạng câu hỏi.</p>}

          <div className="button-row">
            <button className="button" type="button" onClick={previewBulkQuiz} disabled={isLoading || bulkText.trim().length === 0 || bulkTypes.length === 0}>
              <Eye size={18} aria-hidden="true" />
              Xem trước
            </button>
            <button className="button secondary-button" type="button" onClick={() => { setBulkText(bulkSample); setBulkPreview(null); }} disabled={isLoading}>
              Dùng mẫu
            </button>
          </div>

          {bulkPreview && (
            <div className="preview-wrap">
              <div className="quiz-bulk-summary">
                <p className="form-muted">
                  {bulkPreview.validCount} từ hợp lệ · {bulkPreview.skippedCount} bỏ qua · {bulkPreview.errorCount} lỗi
                </p>
                {bulkPreview.validCount > 0 && (
                  <p className="quiz-bulk-estimate">
                    Sẽ tạo khoảng <strong>{estimatedQuestionCount}</strong> câu hỏi
                    {estimatedQuestionCount >= bulkLimit && rawQuestionCount > bulkLimit ? ` (giới hạn ${bulkLimit})` : ""}.
                  </p>
                )}
              </div>

              <table className="preview-table">
                <thead>
                  <tr>
                    <th>Dòng</th>
                    <th>Từ</th>
                    <th>Câu</th>
                    <th>Trạng thái</th>
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
                          {item.status === "OK" ? "Hợp lệ" : item.status === "ERROR" ? "Lỗi" : "Bỏ qua"}
                        </span>
                        {item.message && <span className="status-message">{item.message}</span>}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>

              {bulkPreview.validCount > 0 ? (
                <>
                  <p className="form-muted quiz-bulk-warning">
                    Lưu ý: các câu hỏi quiz cũ của những từ này sẽ được thay bằng câu mới.
                  </p>
                  <div className="button-row">
                    <button className="button" type="button" onClick={startBulkQuiz} disabled={isLoading}>
                      <ListChecks size={18} aria-hidden="true" />
                      Xác nhận &amp; bắt đầu
                    </button>
                  </div>
                </>
              ) : (
                <p className="form-error">Không có từ nào hợp lệ. Hãy chỉnh lại danh sách rồi xem trước lần nữa.</p>
              )}
            </div>
          )}
        </div>
      )}

      {currentQuestion && !result && (
        <div className="quiz-runner">
          <div className="quiz-progress-row">
            <span>Câu {currentIndex + 1} / {attempt?.totalQuestions}</span>
            <span className="status-pill neutral">{questionLabel(currentQuestion.type)}</span>
          </div>

          <div className="learn-prompt-row">
            <h3>{currentQuestion.prompt}</h3>
            {promptAudioText && <PronounceButton text={promptAudioText} className="learn-prompt-audio-btn" />}
          </div>

          {isChoiceQuestion(currentQuestion) ? (
            <div className="quiz-options">
              {choiceOptions(currentQuestion).map((option, index) => {
                let optionClass = "quiz-option";
                if (selectedAnswers[currentQuestion.id] === option && !currentAnswer) {
                  optionClass += " selected";
                }
                if (currentAnswer) {
                  if (option === currentAnswer.correctAnswer) {
                    optionClass += " correct-highlight";
                  } else if (option === selectedAnswers[currentQuestion.id] && !currentAnswer.correct) {
                    optionClass += " wrong-highlight";
                  }
                }
                return (
                  <button
                    key={option}
                    className={optionClass}
                    type="button"
                    onClick={() => setSelectedAnswers((current) => ({ ...current, [currentQuestion.id]: option }))}
                    disabled={Boolean(currentAnswer)}
                  >
                    <span className="quiz-option-letter">{index + 1}</span>
                    {option}
                  </button>
                );
              })}
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
                    <option value="">Chọn nghĩa</option>
                    {(matchingOptions(currentQuestion)?.meanings ?? []).map((meaning) => (
                      <option key={meaning} value={meaning}>{meaning}</option>
                    ))}
                  </select>
                </div>
              ))}
            </div>
          ) : (
            <div className="field">
              <label htmlFor={`quiz-answer-${currentQuestion.id}`}>Đáp án</label>
              <input
                id={`quiz-answer-${currentQuestion.id}`}
                value={selectedAnswers[currentQuestion.id] ?? ""}
                onChange={(event) =>
                  setSelectedAnswers((current) => ({ ...current, [currentQuestion.id]: event.target.value }))
                }
                disabled={Boolean(currentAnswer)}
                autoComplete="off"
              />
            </div>
          )}

          {currentAnswer && (
            <div className={`quiz-feedback ${currentAnswer.correct ? "correct" : "incorrect"}`}>
              <strong>
                {currentAnswer.correct ? <Check size={18} aria-hidden="true" /> : <X size={18} aria-hidden="true" />}
                {currentAnswer.correct ? "Chính xác!" : "Sai rồi"}
              </strong>
              {currentAnswer.explanation && <p>{currentAnswer.explanation}</p>}
              {writtenDiff ? (
                <div className="learn-diff-row">
                  <div className="learn-diff-column">
                    <span className="learn-diff-label">Câu trả lời của bạn</span>
                    <DiffDisplay segments={writtenDiff.userDiff} />
                  </div>
                  <div className="learn-diff-column">
                    <span className="learn-diff-label">Đáp án đúng</span>
                    <DiffDisplay segments={writtenDiff.correctDiff} />
                  </div>
                </div>
              ) : (
                !currentAnswer.correct && currentQuestion.type !== "MATCHING" && <p>Đáp án đúng: {currentAnswer.correctAnswer}</p>
              )}
            </div>
          )}

          <div className="button-row">
            {!currentAnswer ? (
              <button className="button" type="button" onClick={() => submitAnswer(currentQuestion)} disabled={isLoading}>
                <Send size={18} aria-hidden="true" />
                Gửi
              </button>
            ) : (
              <button
                className="button"
                type="button"
                onClick={goNext}
                disabled={currentIndex + 1 >= (attempt?.totalQuestions ?? 0)}
              >
                Tiếp tục
                <ChevronRight size={18} aria-hidden="true" />
              </button>
            )}
            {attempt && answeredCount > 0 && (
              <button className="button secondary-button" type="button" onClick={loadResult} disabled={isLoading}>
                <CircleHelp size={18} aria-hidden="true" />
                Kết quả
              </button>
            )}
          </div>

          <div className="learn-kbd-hint">
            <kbd>1</kbd>–<kbd>4</kbd> chọn đáp án · <kbd>Enter</kbd> gửi / tiếp tục
          </div>
        </div>
      )}

      {result && (
        <div className="quiz-result">
          <div>
            <span className="result-score">{result.scorePercent}%</span>
            <p>{result.correctCount}/{result.totalQuestions} câu đúng</p>
          </div>
          <div className="quiz-answer-list">
            {result.answers.map((answer) => (
              <div key={answer.questionId} className="quiz-answer-row">
                <span className={`status-pill ${answer.correct ? "ok" : "neutral"}`}>
                  {answer.correct ? "Đúng" : "Xem lại"}
                </span>
                {!answer.correct && <p>Đáp án đúng: {answer.correctAnswer}</p>}
                {answer.explanation && <p>{answer.explanation}</p>}
              </div>
            ))}
          </div>
        </div>
      )}
    </section>
  );
}

function questionLabel(type: string) {
  if (type === "CHOOSE_MEANING") return "Chọn nghĩa";
  if (type === "CLOZE_CHOICE") return "Điền từ";
  if (type === "TRUE_FALSE") return "Đúng / Sai";
  if (type === "MATCHING") return "Nối từ";
  return "Viết đáp án";
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

"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { Check, ChevronRight, CircleHelp, Eye, ListChecks, Save, Send, Volume2, X } from "lucide-react";
import {
  answerQuizQuestion,
  getQuizResult,
  previewQuizImport,
  QuizAnswer,
  QuizAttempt,
  QuizImportPreview,
  QuizQuestion,
  QuizResult,
  saveQuizImport,
  startQuiz
} from "@/lib/api";

type QuizPanelProps = {
  token: string;
  deckId: string;
  totalWords: number;
  savedQuestionCount: number;
  canManageQuestions?: boolean;
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

export function QuizPanel({ token, deckId, totalWords, savedQuestionCount, canManageQuestions = true, refreshDeck }: QuizPanelProps) {
  const [attempt, setAttempt] = useState<QuizAttempt | null>(null);
  const [answers, setAnswers] = useState<Record<number, QuizAnswer>>({});
  const [selectedAnswers, setSelectedAnswers] = useState<Record<number, string>>({});
  const [currentIndex, setCurrentIndex] = useState(0);
  const [questionStartedAt, setQuestionStartedAt] = useState(Date.now());
  const [result, setResult] = useState<QuizResult | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState("");
  const [showBulkImport, setShowBulkImport] = useState(false);
  const [bulkText, setBulkText] = useState("");
  const [bulkPreview, setBulkPreview] = useState<QuizImportPreview | null>(null);
  const [questionLimit, setQuestionLimit] = useState<number>(Math.max(1, savedQuestionCount));
  const autoAdvanceTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    setQuestionLimit((current) => {
      if (savedQuestionCount <= 0) return 1;
      if (current > savedQuestionCount) return savedQuestionCount;
      if (current < 1) return 1;
      return current;
    });
  }, [savedQuestionCount]);

  const currentQuestion = useMemo(() => {
    if (!attempt || attempt.questions.length === 0) {
      return null;
    }
    return attempt.questions[Math.min(currentIndex, attempt.questions.length - 1)];
  }, [attempt, currentIndex]);

  const currentAnswer = currentQuestion ? answers[currentQuestion.id] : null;
  const answeredCount = Object.keys(answers).length;

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

  async function beginQuiz() {
    setIsLoading(true);
    setError("");
    resetRunState();
    try {
      const safeLimit = Math.max(1, Math.min(questionLimit || 1, Math.max(1, savedQuestionCount)));
      const nextAttempt = await startQuiz(token, deckId, safeLimit);
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
      const nextPreview = await previewQuizImport(token, deckId, { rawText: bulkText });
      setBulkPreview(nextPreview);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Không thể xem trước quiz");
    } finally {
      setIsLoading(false);
    }
  }

  async function saveBulkQuiz() {
    setIsLoading(true);
    setError("");
    try {
      const saved = await saveQuizImport(token, deckId, { rawText: bulkText });
      setBulkPreview(saved);
      setShowBulkImport(false);
      setBulkText("");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Không thể lưu quiz");
    } finally {
      setIsLoading(false);
    }
  }

  const submitAnswer = useCallback(
    async (question: QuizQuestion) => {
      const answer = selectedAnswers[question.id]?.trim() ?? "";
      if (!answer) {
        setError("Hãy chọn đáp án trước");
        return;
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
    [attempt, answers, selectedAnswers, questionStartedAt, token, refreshDeck, goNext]
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
        } else if (!isInput) {
          event.preventDefault();
          void submitAnswer(currentQuestion);
        }
        return;
      }

      if (!currentAnswer && !isInput) {
        const options = currentQuestion.options;
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

  return (
    <section className="card quiz-card">
      <div className="section-heading">
        <div>
          <h2>Quiz</h2>
          <p>{attempt ? `Đã trả lời ${answeredCount}/${attempt.totalQuestions}` : `Trắc nghiệm 4 lựa chọn · ${savedQuestionCount} câu hỏi đã lưu`}</p>
        </div>
        {canManageQuestions && (
          <div className="button-row">
            <button
              className="button secondary-button"
              type="button"
              onClick={() => setShowBulkImport((current) => !current)}
              disabled={isLoading || totalWords < 4}
            >
              <Eye size={18} aria-hidden="true" />
              {showBulkImport ? "Đóng" : "Thêm câu hỏi"}
            </button>
          </div>
        )}
      </div>

      {error && <p className="form-error">{error}</p>}
      {totalWords < 4 && <p className="form-muted">Cần ít nhất 4 từ vựng trong bộ thẻ để tạo 4 lựa chọn đáp án.</p>}

      {!attempt && !showBulkImport && totalWords >= 4 && (
        <div className="quiz-start-form">
          {savedQuestionCount === 0 ? (
            <div className="quiz-empty-state">
              <ListChecks size={28} aria-hidden="true" />
              <h3>Chưa có câu hỏi quiz</h3>
              <p>
                {canManageQuestions
                  ? <>Hãy thêm danh sách câu hỏi <code>từ -- câu hỏi</code> để bắt đầu.</>
                  : "Deck lớp này chưa có câu hỏi quiz. Chủ lớp cần thêm câu hỏi trước."}
              </p>
              {canManageQuestions && (
                <button
                  className="button"
                  type="button"
                  onClick={() => setShowBulkImport(true)}
                >
                  <Eye size={18} aria-hidden="true" />
                  Thêm câu hỏi ngay
                </button>
              )}
            </div>
          ) : (
            <>
              <div className="quiz-start-row">
                <label className="quiz-start-label" htmlFor="quiz-count">Số câu trong lượt này</label>
                <div className="quiz-start-controls">
                  <input
                    id="quiz-count"
                    type="number"
                    min={1}
                    max={savedQuestionCount}
                    value={questionLimit}
                    onChange={(event) => {
                      const value = Number(event.target.value) || 1;
                      setQuestionLimit(Math.max(1, Math.min(value, savedQuestionCount)));
                    }}
                    className="quiz-count-input"
                  />
                  <span className="form-muted">/ {savedQuestionCount}</span>
                  <button
                    className="button"
                    type="button"
                    onClick={beginQuiz}
                    disabled={isLoading || savedQuestionCount === 0}
                  >
                    <ListChecks size={18} aria-hidden="true" />
                    Bắt đầu Quiz
                  </button>
                </div>
              </div>
              <p className="form-muted">Mỗi lần chơi, hệ thống xáo lại thứ tự câu và 3 đáp án sai.</p>
            </>
          )}
        </div>
      )}

      {attempt && !result && (
        <div className="button-row quiz-new-row">
          <button className="button secondary-button" type="button" onClick={beginQuiz} disabled={isLoading}>
            <ListChecks size={18} aria-hidden="true" />
            Quiz mới
          </button>
        </div>
      )}

      {showBulkImport && canManageQuestions && (
        <div className="quiz-manual-panel">
          <div className="field">
            <label htmlFor="bulk-quiz-text">Danh sách câu hỏi</label>
            <textarea
              id="bulk-quiz-text"
              className="quiz-json-input"
              value={bulkText}
              onChange={(event) => {
                setBulkText(event.target.value);
                setBulkPreview(null);
              }}
              placeholder={`fingernail -- She broke a ...... while opening the box.\nabsent -- He was ...... from class yesterday.`}
              spellCheck={false}
            />
            <p className="form-muted">
              Mỗi dòng: <code>từ -- câu hỏi</code>. Từ phải có trong bộ thẻ này; hệ thống sẽ ghép câu hỏi với từ đó.
            </p>
          </div>

          <div className="button-row">
            <button
              className="button"
              type="button"
              onClick={previewBulkQuiz}
              disabled={isLoading || bulkText.trim().length === 0}
            >
              <Eye size={18} aria-hidden="true" />
              Xem trước
            </button>
          </div>

          {bulkPreview && (
            <div className="preview-wrap">
              <div className="quiz-bulk-summary">
                <p className="form-muted">
                  {bulkPreview.validCount} câu hợp lệ · {bulkPreview.skippedCount} bỏ qua · {bulkPreview.errorCount} lỗi
                </p>
              </div>

              <table className="preview-table">
                <thead>
                  <tr>
                    <th>Dòng</th>
                    <th>Từ</th>
                    <th>Câu hỏi</th>
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
                    Lưu ý: câu hỏi cũ của những từ này sẽ được thay bằng câu mới.
                  </p>
                  <div className="button-row">
                    <button className="button" type="button" onClick={saveBulkQuiz} disabled={isLoading}>
                      <Save size={18} aria-hidden="true" />
                      Lưu câu hỏi
                    </button>
                  </div>
                </>
              ) : (
                <p className="form-error">Không có câu hỏi nào hợp lệ. Hãy chỉnh lại danh sách rồi xem trước lần nữa.</p>
              )}
            </div>
          )}
        </div>
      )}

      {currentQuestion && !result && (
        <div className="quiz-runner">
          <div className="quiz-progress-row">
            <span>Câu {currentIndex + 1} / {attempt?.totalQuestions}</span>
            <span className="status-pill neutral">Chọn từ phù hợp</span>
          </div>

          <div className="learn-prompt-row">
            <h3>{currentQuestion.prompt}</h3>
            <PronounceButton text={currentQuestion.prompt} className="learn-prompt-audio-btn" />
          </div>

          <div className="quiz-options">
            {currentQuestion.options.map((option, index) => {
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

          {currentAnswer && (
            <div className={`quiz-feedback ${currentAnswer.correct ? "correct" : "incorrect"}`}>
              <strong>
                {currentAnswer.correct ? <Check size={18} aria-hidden="true" /> : <X size={18} aria-hidden="true" />}
                {currentAnswer.correct ? "Chính xác!" : "Sai rồi"}
              </strong>
              {currentAnswer.explanation && <p>{currentAnswer.explanation}</p>}
              {!currentAnswer.correct && <p>Đáp án đúng: {currentAnswer.correctAnswer}</p>}
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

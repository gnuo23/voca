"use client";

import { useState, useEffect, useRef, useCallback } from "react";
import { Check, X, Send, ChevronRight, HelpCircle, AlertTriangle, Volume2, Frown, Meh, Smile, Zap } from "lucide-react";
import { LearnQuestion as LearnQuestionData, LearnAnswer, LearnVerdict, ReviewQuality } from "@/lib/api";
import { computeDiff, DiffSegment } from "@/lib/diffUtil";

type LearnQuestionProps = {
  question: LearnQuestionData;
  onSubmit: (answer: string) => Promise<LearnAnswer | null>;
  onNext: () => void;
  onOverride?: (sessionItemId: number) => Promise<LearnAnswer | null>;
  onQuality?: (sessionItemId: number, quality: ReviewQuality) => Promise<void>;
  isLoading: boolean;
  showWrittenHint: boolean;
};

/** Resolve the feedback class for the question card border/bg */
function feedbackCardClass(feedback: LearnAnswer | null): string {
  if (!feedback) return "";
  const verdict = feedback.verdict ?? (feedback.correct ? "CORRECT" : "INCORRECT");
  if (verdict === "CORRECT") return "is-correct";
  if (verdict === "CLOSE") return "is-close";
  return "is-incorrect";
}

function quotedPromptValue(prompt: string): string | null {
  const match = prompt.match(/[:"“]\s*["“]?([^"”]+)["”]?\.?$/);
  return match?.[1]?.trim() || null;
}

function isMeaningPrompt(question: LearnQuestionData): boolean {
  const prompt = question.trueFalseStatement ?? question.prompt;
  return /meaning:/i.test(prompt) || /for this meaning/i.test(prompt);
}

/** Get the question type label for display */
function questionTypeLabel(question: LearnQuestionData): string {
  if (question.questionType === "MCQ") return isMeaningPrompt(question) ? "Định nghĩa" : "Từ vựng";
  if (question.questionType === "WRITTEN") return isMeaningPrompt(question) ? "Định nghĩa" : "Từ vựng";
  const type = question.questionType;
  if (type === "TRUE_FALSE") return "Đúng / Sai";
  return "Viết đáp án";
}

function displayQuestionPrompt(question: LearnQuestionData): string {
  if (question.trueFalseStatement) return question.trueFalseStatement;
  return quotedPromptValue(question.prompt) ?? question.prompt;
}

function normalizeLetters(value: string | null | undefined): string {
  return (value ?? "")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
    .replace(/[^a-z]/g, "");
}

function hasSharedLetter(userAnswer: string | null | undefined, correctAnswer: string | null | undefined): boolean {
  const userLetters = new Set(normalizeLetters(userAnswer).split(""));
  return normalizeLetters(correctAnswer)
    .split("")
    .some((letter) => userLetters.has(letter));
}

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

/** Render diff segments as spans with appropriate CSS classes */
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

export function LearnQuestion({
  question,
  onSubmit,
  onNext,
  onOverride,
  onQuality,
  isLoading,
  showWrittenHint,
}: LearnQuestionProps) {
  const [answer, setAnswer] = useState("");
  const [feedback, setFeedback] = useState<LearnAnswer | null>(null);
  const [selectedOption, setSelectedOption] = useState<string>("");
  const [submitting, setSubmitting] = useState(false);
  const [overriding, setOverriding] = useState(false);
  const [pickedQuality, setPickedQuality] = useState<ReviewQuality | null>(null);
  const autoAdvanceTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    setAnswer("");
    setFeedback(null);
    setSelectedOption("");
    setSubmitting(false);
    setOverriding(false);
    setPickedQuality(null);
    if (autoAdvanceTimer.current) {
      clearTimeout(autoAdvanceTimer.current);
      autoAdvanceTimer.current = null;
    }
    if (question.questionType === "WRITTEN") {
      setTimeout(() => inputRef.current?.focus(), 50);
    }
  }, [question]);

  // Cleanup timer on unmount
  useEffect(() => {
    return () => {
      if (autoAdvanceTimer.current) {
        clearTimeout(autoAdvanceTimer.current);
      }
    };
  }, []);

  const handleSubmit = useCallback(
    async (value: string) => {
      if (submitting || feedback || isLoading) return;
      setSubmitting(true);
      setSelectedOption(value);
      try {
        const result = await onSubmit(value);
        if (result) {
          setFeedback(result);
        }
      } finally {
        setSubmitting(false);
      }
    },
    [submitting, feedback, isLoading, onSubmit]
  );

  const handleDontKnow = useCallback(() => {
    if (submitting || feedback || isLoading) return;
    setAnswer("");
    handleSubmit("");
  }, [submitting, feedback, isLoading, handleSubmit]);

  const handleOverride = useCallback(async () => {
    if (!feedback || !onOverride || !question.sessionItemId || overriding) return;
    setOverriding(true);
    try {
      const result = await onOverride(question.sessionItemId);
      if (result) {
        setFeedback(result);
        // Auto-advance after override accepted
        autoAdvanceTimer.current = setTimeout(() => {
          onNext();
        }, 1200);
      }
    } finally {
      setOverriding(false);
    }
  }, [feedback, onOverride, question.sessionItemId, overriding, onNext]);

  const handleAdvance = useCallback(() => {
    if (!feedback) return;
    if (autoAdvanceTimer.current) {
      clearTimeout(autoAdvanceTimer.current);
      autoAdvanceTimer.current = null;
    }
    onNext();
  }, [feedback, onNext]);

  const handlePickQuality = useCallback(
    async (quality: ReviewQuality) => {
      if (!feedback || !question.sessionItemId || pickedQuality) return;
      setPickedQuality(quality);
      try {
        if (onQuality) {
          await onQuality(question.sessionItemId, quality);
        }
      } catch {
        // Non-fatal: scheduling adjustment is a best-effort tweak
      }
      onNext();
    },
    [feedback, question.sessionItemId, pickedQuality, onQuality, onNext]
  );

  // Keyboard handler
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      // Don't capture if user is typing in an input
      const target = e.target as HTMLElement;
      const isInput = target.tagName === "INPUT" || target.tagName === "TEXTAREA";

      if (e.key === "Enter") {
        if (feedback) {
          e.preventDefault();
          handleAdvance();
        } else if (question.questionType === "WRITTEN" && isInput) {
          e.preventDefault();
          handleSubmit(answer);
        }
      }

      // Ctrl+Enter or Alt+D for "Don't know" (Written only, no feedback yet)
      if (!feedback && question.questionType === "WRITTEN") {
        if ((e.ctrlKey && e.key === "Enter") || (e.altKey && e.key === "d")) {
          e.preventDefault();
          handleDontKnow();
        }
      }

      // Number keys 1-4 for MCQ / TRUE_FALSE options
      if (
        !feedback &&
        !isInput &&
        (question.questionType === "MCQ" || question.questionType === "TRUE_FALSE") &&
        question.options
      ) {
        const num = parseInt(e.key, 10);
        if (num >= 1 && num <= question.options.length) {
          e.preventDefault();
          handleSubmit(question.options[num - 1]);
        }
      }
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [feedback, question.questionType, question.options, answer, handleAdvance, handleSubmit, handleDontKnow]);

  const cardClass = feedbackCardClass(feedback);
  const verdict: LearnVerdict | null = feedback
    ? (feedback.verdict ?? (feedback.correct ? "CORRECT" : "INCORRECT"))
    : null;

  const displayPrompt = displayQuestionPrompt(question);
  const promptAudioText =
    question.questionType !== "TRUE_FALSE" && !isMeaningPrompt(question) && question.word
      ? question.word
      : null;

  // Compute diff for CLOSE / INCORRECT written answers
  const showSimpleWrittenAnswer = Boolean(
    feedback &&
    question.questionType === "WRITTEN" &&
    verdict !== "CORRECT" &&
    (!normalizeLetters(feedback.userAnswer) || !hasSharedLetter(feedback.userAnswer, feedback.correctAnswer))
  );

  const showDiff =
    feedback &&
    question.questionType === "WRITTEN" &&
    verdict !== "CORRECT" &&
    !showSimpleWrittenAnswer &&
    feedback.userAnswer &&
    feedback.correctAnswer;

  const diff = showDiff
    ? computeDiff(feedback.userAnswer, feedback.correctAnswer)
    : null;

  const similarityPercent =
    feedback && typeof feedback.similarityScore === "number"
      ? Math.round(feedback.similarityScore * 100)
      : null;

  return (
    <div className="learn-q-container">
      <div className={`learn-question-card ${cardClass}`}>
        <span className="learn-q-type-label">{questionTypeLabel(question)}</span>
        <div className="learn-prompt-row">
          <div className="learn-prompt">{displayPrompt}</div>
          {promptAudioText && <PronounceButton text={promptAudioText} className="learn-prompt-audio-btn" />}
        </div>

        {/* Answer Section */}
        <div className="learn-answer-section">
          {/* Section header */}
          {(question.questionType === "MCQ" || question.questionType === "TRUE_FALSE") && (
            <div className="learn-answer-header">
              <span className="learn-answer-title">
                {question.questionType === "TRUE_FALSE" ? "Chọn đúng hay sai" : "Chọn đáp án đúng"}
              </span>
              {feedback && !feedback.correct && (
                <span className="learn-retry-badge">Hãy thử lại lần nữa</span>
              )}
              {feedback && feedback.correct && (
                <span className="learn-correct-badge">Chính xác!</span>
              )}
            </div>
          )}

          {question.questionType === "WRITTEN" && !feedback && (
            <div className="learn-answer-header">
              <span className="learn-answer-title">Nhập đáp án của bạn</span>
            </div>
          )}

          {/* MCQ or TRUE_FALSE */}
          {(question.questionType === "MCQ" || question.questionType === "TRUE_FALSE") &&
            question.options && (
              <>
                <div className="learn-options-grid-2col">
                  {question.options.map((option, idx) => {
                    let optionClass = "learn-option-card";
                    const isDisabled = !!feedback || submitting || isLoading;
                    const showOptionAudio = question.questionType === "MCQ" && isMeaningPrompt(question);
                    if (feedback) {
                      if (option === feedback.correctAnswer) {
                        optionClass += " correct-highlight";
                      } else if (option === selectedOption && !feedback.correct) {
                        optionClass += " wrong-highlight";
                      }
                    }
                    if (isDisabled) {
                      optionClass += " is-disabled";
                    }
                    return (
                      <div
                        key={option}
                        className={optionClass}
                      >
                        <button
                          type="button"
                          className="learn-option-select"
                          onClick={() => handleSubmit(option)}
                          disabled={isDisabled}
                        >
                          <span className="learn-option-number">{idx + 1}</span>
                          <span className="learn-option-text">{option}</span>
                        </button>
                        {showOptionAudio && <PronounceButton text={option} className="learn-option-audio-btn" />}
                      </div>
                    );
                  })}
                </div>
                {!feedback && (
                  <div className="learn-choice-dont-know-row">
                      <button
                        type="button"
                        className="learn-choice-dont-know-btn"
                        onClick={handleDontKnow}
                        disabled={submitting || isLoading}
                      >
                        <HelpCircle size={16} aria-hidden="true" />
                        Bạn không biết
                      </button>
                  </div>
                )}
              </>
            )}

          {/* WRITTEN — input + submit + don't know */}
          {question.questionType === "WRITTEN" && !feedback && (
            <div className="learn-written-field">
              <input
                ref={inputRef}
                type="text"
                className="learn-written-input"
                value={answer}
                onChange={(e) => setAnswer(e.target.value)}
                placeholder="Nhập đáp án…"
                disabled={submitting || isLoading}
                autoComplete="off"
              />
              {showWrittenHint && question.hint && (
                <div className="learn-written-hint">
                  Hint: <span>{question.hint}</span>
                </div>
              )}
              <div className="learn-written-actions">
                <button
                  type="button"
                  className="button"
                  onClick={() => handleSubmit(answer)}
                  disabled={submitting || isLoading}
                >
                  <Send size={16} />
                </button>
                <button
                  type="button"
                  className="learn-dont-know-btn"
                  onClick={handleDontKnow}
                  disabled={submitting || isLoading}
                >
                  <HelpCircle size={16} />
                  Không biết
                </button>
              </div>
              <div className="learn-kbd-hint">
                <kbd>Enter</kbd> gửi · <kbd>Ctrl+Enter</kbd> không biết
              </div>
            </div>
          )}

          {/* Feedback */}
          {feedback && (
            <div
              className={`learn-feedback ${
                verdict === "CORRECT" ? "correct" : verdict === "CLOSE" ? "close" : "incorrect"
              }`}
            >
              <span className={`learn-feedback-icon ${verdict === "CLOSE" ? "close-icon" : ""}`}>
                {verdict === "CORRECT" ? (
                  <Check size={18} />
                ) : verdict === "CLOSE" ? (
                  <AlertTriangle size={18} />
                ) : (
                  <X size={18} />
                )}
              </span>
              {verdict === "CORRECT"
                ? "Chính xác!"
                : verdict === "CLOSE"
                  ? "Gần đúng!"
                  : "Sai rồi"}

              {/* Similarity badge for CLOSE */}
              {verdict === "CLOSE" && similarityPercent !== null && (
                <span className="learn-similarity-badge">{similarityPercent}%</span>
              )}
            </div>
          )}

          {/* Diff display for CLOSE / INCORRECT written */}
          {feedback && diff && (
            <div className="learn-diff-row">
              {feedback.userAnswer && (
                <div className="learn-diff-column">
                  <span className="learn-diff-label">Câu trả lời của bạn</span>
                  <DiffDisplay segments={diff.userDiff} />
                </div>
              )}
              <div className="learn-diff-column">
                <span className="learn-diff-label">Đáp án đúng</span>
                <DiffDisplay segments={diff.correctDiff} />
              </div>
            </div>
          )}

          {/* Non-written incorrect: just show correct answer */}
          {feedback &&
            !feedback.correct &&
            (question.questionType !== "WRITTEN" || showSimpleWrittenAnswer) &&
            verdict !== "CLOSE" && (
              <div className="learn-feedback-answer">
                Đáp án đúng: {feedback.correctAnswer}
              </div>
          )}

          {/* Quizlet-style manual override for written answers the system marked wrong */}
          {feedback && question.questionType === "WRITTEN" && verdict !== "CORRECT" && !showSimpleWrittenAnswer && onOverride && (
            <div className="learn-override-row">
              <button
                type="button"
                className="learn-override-btn accept"
                onClick={handleOverride}
                disabled={overriding}
              >
                <Check size={16} />
                {overriding ? "Đang cập nhật…" : "Tôi đã đúng"}
              </button>
              <button
                type="button"
                className="learn-override-btn reject"
                onClick={handleAdvance}
              >
                <X size={16} />
                Đánh dấu sai
              </button>
            </div>
          )}

          {/* Vocab details — show once feedback is present so the learner can lock the memory */}
          {feedback && (feedback.vocab || question.vocab) && (
            <VocabDetails
              word={feedback.correctAnswer || question.word || ""}
              vocab={feedback.vocab ?? question.vocab}
            />
          )}

          {/* Quality picker — shown for CORRECT/CLOSE to let user self-report recall difficulty */}
          {feedback && verdict !== "INCORRECT" && onQuality && question.sessionItemId && (
            <div className="learn-quality-row" role="group" aria-label="Bạn nhớ từ này thế nào?">
              <span className="learn-quality-label">Bạn nhớ từ này thế nào?</span>
              <div className="learn-quality-buttons">
                <button
                  type="button"
                  className={`learn-quality-btn quality-again ${pickedQuality === "AGAIN" ? "is-picked" : ""}`}
                  onClick={() => handlePickQuality("AGAIN")}
                  disabled={Boolean(pickedQuality)}
                  title="Quên hẳn — cho ôn lại sớm"
                >
                  <Frown size={16} aria-hidden="true" />
                  Quên
                </button>
                <button
                  type="button"
                  className={`learn-quality-btn quality-hard ${pickedQuality === "HARD" ? "is-picked" : ""}`}
                  onClick={() => handlePickQuality("HARD")}
                  disabled={Boolean(pickedQuality)}
                  title="Nhớ mờ — cho ôn lại sớm hơn bình thường"
                >
                  <Meh size={16} aria-hidden="true" />
                  Mờ
                </button>
                <button
                  type="button"
                  className={`learn-quality-btn quality-good ${pickedQuality === "GOOD" ? "is-picked" : ""}`}
                  onClick={() => handlePickQuality("GOOD")}
                  disabled={Boolean(pickedQuality)}
                  title="Nhớ tốt — lịch ôn bình thường"
                >
                  <Smile size={16} aria-hidden="true" />
                  Nhớ
                </button>
                <button
                  type="button"
                  className={`learn-quality-btn quality-easy ${pickedQuality === "EASY" ? "is-picked" : ""}`}
                  onClick={() => handlePickQuality("EASY")}
                  disabled={Boolean(pickedQuality)}
                  title="Nhớ ngay — giãn cách xa hơn"
                >
                  <Zap size={16} aria-hidden="true" />
                  Dễ
                </button>
              </div>
            </div>
          )}

          {/* Fallback continue button for INCORRECT (quality already inferred as AGAIN server-side) */}
          {feedback && verdict === "INCORRECT" && (
            <div className="learn-action-row">
              <button type="button" className="button learn-continue-btn" onClick={handleAdvance}>
                Tiếp tục <ChevronRight size={16} />
              </button>
            </div>
          )}

          {/* Continue button also shown when quality picker is not wired */}
          {feedback && verdict !== "INCORRECT" && !onQuality && (
            <div className="learn-action-row">
              <button type="button" className="button learn-continue-btn" onClick={handleAdvance}>
                Tiếp tục <ChevronRight size={16} />
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

function VocabDetails({ word, vocab }: { word: string; vocab: { ipa: string | null; meaningVi: string | null; partOfSpeech: string | null; exampleEn: string | null; exampleVi: string | null } | null }) {
  if (!vocab) return null;
  const { ipa, meaningVi, partOfSpeech, exampleEn, exampleVi } = vocab;
  if (!ipa && !meaningVi && !exampleEn && !exampleVi) return null;

  const highlighted = exampleEn ? highlightWord(exampleEn, word) : null;

  return (
    <div className="learn-vocab-details">
      <div className="learn-vocab-head">
        <span className="learn-vocab-word">{word}</span>
        {partOfSpeech && <span className="learn-vocab-pos">{partOfSpeech}</span>}
        {ipa && <span className="learn-vocab-ipa">/{ipa.replace(/^\/|\/$/g, "")}/</span>}
        {word && <PronounceButton text={word} className="learn-vocab-audio-btn" />}
      </div>
      {meaningVi && <p className="learn-vocab-meaning">{meaningVi}</p>}
      {highlighted && (
        <div className="learn-vocab-example">
          <span className="learn-vocab-example-label">Ví dụ</span>
          <p className="learn-vocab-example-en">{highlighted}</p>
          {exampleVi && <p className="learn-vocab-example-vi">{exampleVi}</p>}
        </div>
      )}
    </div>
  );
}

function highlightWord(sentence: string, word: string) {
  if (!word) return sentence;
  const escaped = word.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
  const parts = sentence.split(new RegExp(`\\b(${escaped})\\b`, "gi"));
  return parts.map((part, idx) =>
    part.toLowerCase() === word.toLowerCase() ? <strong key={idx} className="learn-vocab-example-hit">{part}</strong> : <span key={idx}>{part}</span>
  );
}

"use client";

import Link from "next/link";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { CalendarClock, Check, RotateCcw, Send, Volume2, X } from "lucide-react";
import { AppShell } from "@/components/AppShell";
import { getStoredToken, getTodayReview, getVocabAudio, ReviewItem, submitReviewAnswer, submitReviewResult, VocabAudio } from "@/lib/api";

type ReviewFeedback = "correct" | "pendingIncorrect" | "incorrect";
type ReviewDirection = "EN_TO_VI" | "VI_TO_EN";

const CORRECT_AUTO_ADVANCE_MS = 700;

function pickAudioUrl(audio: VocabAudio | null) {
  return audio?.audioUrl ?? audio?.audioUsUrl ?? audio?.audioUkUrl ?? null;
}

async function playAudioUrl(audioUrl: string) {
  const audio = new Audio(audioUrl);
  await audio.play();
}

async function playEnglishPronunciation(text: string) {
  if (typeof window === "undefined" || !("speechSynthesis" in window)) {
    throw new Error("Browser speech is not available");
  }

  window.speechSynthesis.cancel();
  const utterance = new SpeechSynthesisUtterance(text);
  utterance.lang = "en-US";
  utterance.rate = 0.92;

  await new Promise<void>((resolve, reject) => {
    utterance.onend = () => resolve();
    utterance.onerror = (event) => reject(new Error(event.error || "Browser speech failed"));
    window.speechSynthesis.speak(utterance);
  });
}

async function playPronunciation(text: string, audio: VocabAudio | null) {
  const audioUrl = pickAudioUrl(audio);
  if (audioUrl) {
    try {
      await playAudioUrl(audioUrl);
      return;
    } catch {
      // Fall back to browser speech below.
    }
  }

  await playEnglishPronunciation(text);
}

export default function ReviewPage() {
  const router = useRouter();
  const [token, setToken] = useState("");
  const [items, setItems] = useState<ReviewItem[]>([]);
  const [currentIndex, setCurrentIndex] = useState(0);
  const [startedAt, setStartedAt] = useState(Date.now());
  const [responseTimeMs, setResponseTimeMs] = useState<number | null>(null);
  const [reviewed, setReviewed] = useState(0);
  const [direction, setDirection] = useState<ReviewDirection>("EN_TO_VI");
  const [answer, setAnswer] = useState("");
  const [feedback, setFeedback] = useState<ReviewFeedback | null>(null);
  const [currentAudio, setCurrentAudio] = useState<VocabAudio | null>(null);
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const answerInputRef = useRef<HTMLInputElement | null>(null);
  const autoAdvanceTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    const storedToken = getStoredToken();
    if (!storedToken) {
      router.push("/login");
      return;
    }
    setToken(storedToken);
    getTodayReview(storedToken)
      .then((response) => {
        setItems(response.items);
        setStartedAt(Date.now());
      })
      .catch((err) => setError(err instanceof Error ? err.message : "Could not load review list"))
      .finally(() => setIsLoading(false));
  }, [router]);

  const currentCard = useMemo(() => items[currentIndex] ?? null, [items, currentIndex]);

  useEffect(() => {
    let cancelled = false;
    setCurrentAudio(null);

    if (!token || !currentCard) {
      return () => {
        cancelled = true;
      };
    }

    getVocabAudio(token, currentCard.vocabId)
      .then((audio) => {
        if (!cancelled) {
          setCurrentAudio(audio);
        }
      })
      .catch(() => {
        if (!cancelled) {
          setCurrentAudio(null);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [currentCard?.vocabId, token]);

  const clearAutoAdvance = useCallback(() => {
    if (autoAdvanceTimer.current) {
      clearTimeout(autoAdvanceTimer.current);
      autoAdvanceTimer.current = null;
    }
  }, []);

  const nextCard = useCallback(() => {
    clearAutoAdvance();
    setReviewed((value) => value + 1);
    setCurrentIndex((index) => index + 1);
    setStartedAt(Date.now());
    setResponseTimeMs(null);
    setAnswer("");
    setFeedback(null);
  }, [clearAutoAdvance]);

  const restart = useCallback((nextDirection = direction) => {
    clearAutoAdvance();
    setDirection(nextDirection);
    setCurrentIndex(0);
    setReviewed(0);
    setStartedAt(Date.now());
    setResponseTimeMs(null);
    setAnswer("");
    setFeedback(null);
    setError("");
  }, [clearAutoAdvance, direction]);

  useEffect(() => {
    return () => clearAutoAdvance();
  }, [clearAutoAdvance]);

  useEffect(() => {
    if (!currentCard || feedback !== null || isLoading) {
      return;
    }
    answerInputRef.current?.focus();
  }, [currentCard?.vocabId, feedback, isLoading]);

  const submit = useCallback(async () => {
    if (!currentCard) {
      return;
    }
    const submitted = answer.trim();
    if (!submitted) {
      setError(direction === "VI_TO_EN" ? "Enter the English word first" : "Enter the meaning first");
      return;
    }
    const correct = direction === "VI_TO_EN"
      ? isEnglishWordCorrect(submitted, currentCard.word)
      : isMeaningCorrect(submitted, currentCard.meaningVi ?? "");
    const elapsedMs = Date.now() - startedAt;
    setResponseTimeMs(elapsedMs);
    setError("");
    if (!correct) {
      setFeedback("pendingIncorrect");
      return;
    }
    setIsLoading(true);
    try {
      await submitReviewAnswer(
        token,
        currentCard.vocabId,
        true,
        elapsedMs,
        direction === "VI_TO_EN" ? "VIETNAMESE_TO_ENGLISH" : "FLASHCARD"
      );
      setFeedback("correct");
      clearAutoAdvance();
      autoAdvanceTimer.current = setTimeout(nextCard, CORRECT_AUTO_ADVANCE_MS);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not submit review");
    } finally {
      setIsLoading(false);
    }
  }, [answer, clearAutoAdvance, currentCard, direction, nextCard, startedAt, token]);

  const reveal = useCallback(async () => {
    if (!currentCard) {
      return;
    }
    setError("");
    setResponseTimeMs(Date.now() - startedAt);
    setFeedback("pendingIncorrect");
  }, [currentCard, startedAt]);

  const playCurrentAudio = useCallback(() => {
    if (!currentCard || (direction === "VI_TO_EN" && feedback === null)) {
      return;
    }
    void playPronunciation(currentCard.word, currentAudio).catch(() => undefined);
  }, [currentAudio, currentCard, direction, feedback]);

  const markPending = useCallback(async (correct: boolean) => {
    if (!currentCard) {
      return;
    }
    setIsLoading(true);
    try {
      await submitReviewResult(
        token,
        currentCard.vocabId,
        correct ? "GOOD" : "AGAIN",
        responseTimeMs ?? Date.now() - startedAt,
        direction === "VI_TO_EN" ? "VIETNAMESE_TO_ENGLISH" : "FLASHCARD"
      );
      setFeedback(correct ? "correct" : "incorrect");
      clearAutoAdvance();
      if (correct) {
        autoAdvanceTimer.current = setTimeout(nextCard, CORRECT_AUTO_ADVANCE_MS);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not submit review");
    } finally {
      setIsLoading(false);
    }
  }, [clearAutoAdvance, currentCard, direction, nextCard, responseTimeMs, startedAt, token]);

  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.isComposing || isLoading || !currentCard) {
        return;
      }

      if ((event.ctrlKey && event.key === "Enter") || (event.altKey && event.key.toLowerCase() === "d")) {
        event.preventDefault();
        if (feedback === null) {
          void reveal();
          return;
        }
        if (feedback === "pendingIncorrect") {
          void markPending(false);
        }
        return;
      }

      if (event.code === "ShiftLeft" && !event.repeat) {
        event.preventDefault();
        playCurrentAudio();
        return;
      }

      if (event.key === "Enter") {
        event.preventDefault();
        if (feedback === null) {
          void submit();
          return;
        }
        if (feedback === "correct" || feedback === "incorrect") {
          nextCard();
        }
      }

      if (feedback === "pendingIncorrect") {
        if (event.key === "ArrowUp") {
          event.preventDefault();
          void markPending(true);
        }
        if (event.key === "ArrowDown") {
          event.preventDefault();
          void markPending(false);
        }
      }
    };

    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [currentCard, feedback, isLoading, markPending, nextCard, playCurrentAudio, reveal, submit]);

  const complete = !isLoading && currentIndex >= items.length;

  return (
    <AppShell>
      <header className="topbar">
        <div>
          <h1>Review</h1>
          <p>
            {items.length === 0 && isLoading
              ? "Loading cards"
              : `${direction === "VI_TO_EN" ? "Việt → Anh" : "Anh → Việt"} · ${Math.min(currentIndex + 1, items.length)} / ${items.length || 0}`}
          </p>
        </div>
        <div className="button-row">
          <Link className="button secondary-button" href="/review/schedule">
            <CalendarClock size={18} aria-hidden="true" />
            Schedule
          </Link>
          <button
            className="button secondary-button"
            type="button"
            onClick={() => restart()}
            disabled={items.length === 0}
          >
            <RotateCcw size={18} aria-hidden="true" />
            Restart
          </button>
        </div>
      </header>

      {error && <p className="form-error">{error}</p>}

      <section className="card review-mode-picker" aria-label="Review direction">
        <div className="review-mode-switch" role="group" aria-label="Chọn chiều ôn tập">
          <button
            className="button secondary-button"
            type="button"
            aria-pressed={direction === "EN_TO_VI"}
            onClick={() => restart("EN_TO_VI")}
          >
            Anh → Việt
          </button>
          <button
            className="button secondary-button"
            type="button"
            aria-pressed={direction === "VI_TO_EN"}
            onClick={() => restart("VI_TO_EN")}
          >
            Việt → Anh
          </button>
        </div>
        <p className="review-mode-note">
          {direction === "VI_TO_EN"
            ? "Luyện theo các từ đang đến hạn, không làm thay đổi lịch hay chỉ số ôn tập."
            : "Kết quả trả lời sẽ cập nhật lịch ôn tập."}
        </p>
      </section>

      {currentCard && (
        <section className="review-stage">
          <article className="review-card">
            <span className="status-pill neutral">{currentCard.status}</span>
            <div className="review-word-row">
              <h2>{direction === "VI_TO_EN" ? currentCard.meaningVi || "-" : currentCard.word}</h2>
              {(direction === "EN_TO_VI" || feedback !== null) && (
                <button
                  type="button"
                  className="learn-audio-btn review-audio-btn"
                  onClick={playCurrentAudio}
                  aria-label={`Nghe phát âm ${currentCard.word}`}
                  title={`Nghe phát âm ${currentCard.word} (Left Shift)`}
                >
                  <Volume2 size={18} aria-hidden="true" />
                </button>
              )}
            </div>
            <p>{currentCard.partOfSpeech || "Vocabulary"}</p>
            {feedback === null ? (
              <div className="field review-answer-field">
                <label htmlFor="review-answer">
                  {direction === "VI_TO_EN" ? "English word" : "Meaning"}
                </label>
                <input
                  ref={answerInputRef}
                  id="review-answer"
                  type="text"
                  value={answer}
                  onChange={(event) => setAnswer(event.target.value)}
                  autoComplete="off"
                  autoCorrect="off"
                  autoCapitalize="none"
                  spellCheck={false}
                  data-lpignore="true"
                  data-1p-ignore="true"
                  data-bwignore="true"
                  data-form-type="other"
                  autoFocus
                />
              </div>
            ) : (
              <div className={`quiz-feedback ${feedback === "correct" ? "correct" : "incorrect"}`}>
                <strong>
                  {feedback === "correct" ? <Check size={18} aria-hidden="true" /> : <X size={18} aria-hidden="true" />}
                  {feedback === "correct" ? "Correct" : feedback === "pendingIncorrect" ? "Check your answer" : "Needs review"}
                </strong>
                {feedback === "pendingIncorrect" && answer.trim() && <p>Your answer: {answer.trim()}</p>}
                <p>
                  Correct answer: {direction === "VI_TO_EN" ? currentCard.word : currentCard.meaningVi || "-"}
                </p>
                {direction === "VI_TO_EN" && <p>Meaning: {currentCard.meaningVi || "-"}</p>}
                {currentCard.exampleEn && <p>{currentCard.exampleEn}</p>}
                {currentCard.exampleVi && <p>{currentCard.exampleVi}</p>}
              </div>
            )}
          </article>

          <div className="review-actions">
            {feedback === null ? (
              <button className="button" type="button" onClick={submit} disabled={isLoading}>
                <Send size={18} aria-hidden="true" />
                Check
              </button>
            ) : feedback === "pendingIncorrect" ? (
              <button className="button" type="button" onClick={() => markPending(true)} disabled={isLoading}>
                <Check size={18} aria-hidden="true" />
                Tôi đã trả lời đúng
              </button>
            ) : (
              <button className="button" type="button" onClick={nextCard}>
                <Check size={18} aria-hidden="true" />
                Next
              </button>
            )}
            {feedback === "pendingIncorrect" ? (
              <button className="button secondary-button" type="button" onClick={() => markPending(false)} disabled={isLoading}>
                <X size={18} aria-hidden="true" />
                Đánh dấu sai
              </button>
            ) : (
              <button className="button secondary-button" type="button" onClick={reveal} disabled={feedback !== null || isLoading}>
                <X size={18} aria-hidden="true" />
                Reveal
              </button>
            )}
          </div>
        </section>
      )}

      {complete && (
        <section className="card review-summary">
          <h2>Review complete</h2>
          <div className="metric">{reviewed}</div>
          <p>cards reviewed</p>
        </section>
      )}
    </AppShell>
  );
}

function isMeaningCorrect(answer: string, meaning: string) {
  const normalizedAnswer = normalizeAnswer(answer);
  const accepted = meaning
    .split(/[,;/\n]/)
    .map(normalizeAnswer)
    .filter(Boolean);
  return normalizeAnswer(meaning) === normalizedAnswer || accepted.includes(normalizedAnswer);
}

function isEnglishWordCorrect(answer: string, word: string) {
  return normalizeAnswer(answer) === normalizeAnswer(word);
}

function normalizeAnswer(value: string) {
  return value
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .trim()
    .replace(/\s+/g, " ")
    .toLowerCase();
}

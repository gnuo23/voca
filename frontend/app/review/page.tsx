"use client";

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { CalendarClock, Check, RotateCcw, Send, X } from "lucide-react";
import { AppShell } from "@/components/AppShell";
import { getStoredToken, getTodayReview, ReviewItem, submitReviewAnswer } from "@/lib/api";

export default function ReviewPage() {
  const router = useRouter();
  const [token, setToken] = useState("");
  const [items, setItems] = useState<ReviewItem[]>([]);
  const [currentIndex, setCurrentIndex] = useState(0);
  const [startedAt, setStartedAt] = useState(Date.now());
  const [reviewed, setReviewed] = useState(0);
  const [answer, setAnswer] = useState("");
  const [feedback, setFeedback] = useState<boolean | null>(null);
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(true);

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

  async function submit() {
    if (!currentCard) {
      return;
    }
    const submitted = answer.trim();
    if (!submitted) {
      setError("Enter the meaning first");
      return;
    }
    const correct = isMeaningCorrect(submitted, currentCard.meaningVi ?? "");
    setError("");
    setIsLoading(true);
    try {
      await submitReviewAnswer(token, currentCard.vocabId, correct, Date.now() - startedAt);
      setFeedback(correct);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not submit review");
    } finally {
      setIsLoading(false);
    }
  }

  async function reveal() {
    if (!currentCard) {
      return;
    }
    setError("");
    setIsLoading(true);
    try {
      await submitReviewAnswer(token, currentCard.vocabId, false, Date.now() - startedAt);
      setFeedback(false);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not submit review");
    } finally {
      setIsLoading(false);
    }
  }

  function nextCard() {
    setReviewed((value) => value + 1);
    setCurrentIndex((index) => index + 1);
    setStartedAt(Date.now());
    setAnswer("");
    setFeedback(null);
  }

  const complete = !isLoading && currentIndex >= items.length;

  return (
    <AppShell>
      <header className="topbar">
        <div>
          <h1>Review</h1>
          <p>{items.length === 0 && isLoading ? "Loading cards" : `${Math.min(currentIndex + 1, items.length)} / ${items.length || 0}`}</p>
        </div>
        <div className="button-row">
          <Link className="button secondary-button" href="/review/schedule">
            <CalendarClock size={18} aria-hidden="true" />
            Schedule
          </Link>
          <button
            className="button secondary-button"
            type="button"
            onClick={() => {
              setCurrentIndex(0);
              setReviewed(0);
              setStartedAt(Date.now());
              setAnswer("");
              setFeedback(null);
            }}
            disabled={items.length === 0}
          >
            <RotateCcw size={18} aria-hidden="true" />
            Restart
          </button>
        </div>
      </header>

      {error && <p className="form-error">{error}</p>}

      {currentCard && (
        <section className="review-stage">
          <article className="review-card">
            <span className="status-pill neutral">{currentCard.status}</span>
            <h2>{currentCard.word}</h2>
            <p>{currentCard.partOfSpeech || "Vocabulary"}</p>
            {feedback === null ? (
              <div className="field review-answer-field">
                <label htmlFor="review-answer">Meaning</label>
                <input
                  id="review-answer"
                  value={answer}
                  onChange={(event) => setAnswer(event.target.value)}
                  onKeyDown={(event) => {
                    if (event.key === "Enter") {
                      void submit();
                    }
                  }}
                />
              </div>
            ) : (
              <div className={`quiz-feedback ${feedback ? "correct" : "incorrect"}`}>
                <strong>
                  {feedback ? <Check size={18} aria-hidden="true" /> : <X size={18} aria-hidden="true" />}
                  {feedback ? "Correct" : "Needs review"}
                </strong>
                <p>Correct answer: {currentCard.meaningVi || "-"}</p>
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
            ) : (
              <button className="button" type="button" onClick={nextCard}>
                <Check size={18} aria-hidden="true" />
                Next
              </button>
            )}
            <button className="button secondary-button" type="button" onClick={reveal} disabled={feedback !== null || isLoading}>
              <X size={18} aria-hidden="true" />
              Reveal
            </button>
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

function normalizeAnswer(value: string) {
  return value
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .trim()
    .replace(/\s+/g, " ")
    .toLowerCase();
}

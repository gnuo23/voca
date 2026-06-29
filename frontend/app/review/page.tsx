"use client";

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { CalendarClock, Check, Flame, RotateCcw, Smile, X } from "lucide-react";
import { AppShell } from "@/components/AppShell";
import { getStoredToken, getTodayReview, ReviewItem, ReviewQuality, submitReviewResult } from "@/lib/api";

export default function ReviewPage() {
  const router = useRouter();
  const [token, setToken] = useState("");
  const [items, setItems] = useState<ReviewItem[]>([]);
  const [currentIndex, setCurrentIndex] = useState(0);
  const [startedAt, setStartedAt] = useState(Date.now());
  const [reviewed, setReviewed] = useState(0);
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

  async function submit(quality: ReviewQuality) {
    if (!currentCard) {
      return;
    }
    setError("");
    try {
      await submitReviewResult(token, currentCard.vocabId, quality, Date.now() - startedAt);
      setReviewed((value) => value + 1);
      setCurrentIndex((index) => index + 1);
      setStartedAt(Date.now());
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not submit review");
    }
  }

  const complete = !isLoading && currentIndex >= items.length;

  return (
    <AppShell>
      <header className="topbar">
        <div>
          <h1>Review</h1>
          <p>{isLoading ? "Loading cards" : `${Math.min(currentIndex + 1, items.length)} / ${items.length || 0}`}</p>
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
            <div className="review-meaning">{currentCard.meaningVi || "-"}</div>
            {currentCard.exampleEn && <p>{currentCard.exampleEn}</p>}
            {currentCard.exampleVi && <p>{currentCard.exampleVi}</p>}
          </article>

          <div className="review-actions">
            <button className="button danger-button" type="button" onClick={() => submit("AGAIN")}>
              <X size={18} aria-hidden="true" />
              Again
            </button>
            <button className="button warning-button" type="button" onClick={() => submit("HARD")}>
              <Flame size={18} aria-hidden="true" />
              Hard
            </button>
            <button className="button" type="button" onClick={() => submit("GOOD")}>
              <Check size={18} aria-hidden="true" />
              Good
            </button>
            <button className="button" type="button" onClick={() => submit("EASY")}>
              <Smile size={18} aria-hidden="true" />
              Easy
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

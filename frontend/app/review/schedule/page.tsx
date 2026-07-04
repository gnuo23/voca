"use client";

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { CalendarClock, RefreshCw, Repeat2 } from "lucide-react";
import { AppShell } from "@/components/AppShell";
import {
  Deck,
  getReviewSchedule,
  getStoredToken,
  listStudyDecks,
  ReviewScheduleBucket,
  ReviewScheduleItem,
  ReviewScheduleResponse,
  VocabProgressStatus
} from "@/lib/api";

const STATUS_OPTIONS: Array<VocabProgressStatus | ""> = ["", "NEW", "LEARNING", "REVIEW", "DIFFICULT", "MASTERED"];

const BUCKET_LABELS: Record<ReviewScheduleBucket, string> = {
  NEW: "New",
  OVERDUE: "Overdue",
  DUE_NOW: "Now",
  TODAY: "Today",
  TOMORROW: "Tomorrow",
  THIS_WEEK: "This week",
  LATER: "Later"
};

export default function ReviewSchedulePage() {
  const router = useRouter();
  const [token, setToken] = useState("");
  const [decks, setDecks] = useState<Deck[]>([]);
  const [deckId, setDeckId] = useState("");
  const [status, setStatus] = useState<VocabProgressStatus | "">("");
  const [schedule, setSchedule] = useState<ReviewScheduleResponse | null>(null);
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const storedToken = getStoredToken();
    if (!storedToken) {
      router.push("/login");
      return;
    }
    setToken(storedToken);
    listStudyDecks(storedToken)
      .then(setDecks)
      .catch(() => setDecks([]));
  }, [router]);

  useEffect(() => {
    if (!token) {
      return;
    }
    loadSchedule(token, deckId, status);
  }, [token, deckId, status]);

  function loadSchedule(authToken = token, selectedDeckId = deckId, selectedStatus = status) {
    setIsLoading(true);
    setError("");
    getReviewSchedule(authToken, { deckId: selectedDeckId, status: selectedStatus, limit: 300 })
      .then(setSchedule)
      .catch((err) => setError(err instanceof Error ? err.message : "Could not load review schedule"))
      .finally(() => setIsLoading(false));
  }

  const items = useMemo(() => schedule?.items ?? [], [schedule]);

  return (
    <AppShell>
      <header className="topbar">
        <div>
          <h1>Review Schedule</h1>
          <p>{isLoading ? "Loading schedule" : `${schedule?.totalItems ?? 0} scheduled words`}</p>
        </div>
        <div className="button-row">
          <Link className="button secondary-button" href="/review">
            <Repeat2 size={18} aria-hidden="true" />
            Review
          </Link>
          <button className="button secondary-button" type="button" onClick={() => loadSchedule()} disabled={isLoading}>
            <RefreshCw size={18} aria-hidden="true" />
            Refresh
          </button>
        </div>
      </header>

      {error && <p className="form-error">{error}</p>}

      <section className="card schedule-controls">
        <label className="field">
          <span>Deck</span>
          <select value={deckId} onChange={(event) => setDeckId(event.target.value)}>
            <option value="">All decks</option>
            {decks.map((deck) => (
              <option key={deck.id} value={deck.id}>
                {deck.name}
              </option>
            ))}
          </select>
        </label>

        <label className="field">
          <span>Status</span>
          <select value={status} onChange={(event) => setStatus(event.target.value as VocabProgressStatus | "")}>
            {STATUS_OPTIONS.map((option) => (
              <option key={option || "ALL"} value={option}>
                {option ? option.replaceAll("_", " ") : "All statuses"}
              </option>
            ))}
          </select>
        </label>
      </section>

      <section className="grid schedule-summary" aria-label="Review schedule metrics">
        <article className="card">
          <CalendarClock size={22} aria-hidden="true" />
          <div className="metric">{schedule?.dueNow ?? 0}</div>
          <p>Due now</p>
        </article>
        <article className="card">
          <CalendarClock size={22} aria-hidden="true" />
          <div className="metric">{schedule?.upcoming ?? 0}</div>
          <p>Upcoming</p>
        </article>
        <article className="card">
          <CalendarClock size={22} aria-hidden="true" />
          <div className="metric">{schedule?.overdue ?? 0}</div>
          <p>Overdue</p>
        </article>
        <article className="card">
          <CalendarClock size={22} aria-hidden="true" />
          <div className="metric">{schedule?.newItems ?? 0}</div>
          <p>New words</p>
        </article>
      </section>

      <section className="card">
        <div className="section-heading">
          <div>
            <h2>Upcoming Words</h2>
            <p>{items.length} visible</p>
          </div>
        </div>

        <div className="preview-wrap">
          <table className="preview-table schedule-table">
            <thead>
              <tr>
                <th>When</th>
                <th>Word</th>
                <th>Meaning</th>
                <th>Deck</th>
                <th>Status</th>
                <th>Marks</th>
                <th>Next review</th>
              </tr>
            </thead>
            <tbody>
              {items.map((item) => (
                <tr key={item.vocabId}>
                  <td>
                    <span className={`status-pill ${bucketClass(item.bucket)}`}>{BUCKET_LABELS[item.bucket]}</span>
                    <span className="status-message">{relativeTime(item)}</span>
                  </td>
                  <td className="schedule-word">
                    <strong>{item.word}</strong>
                    {item.partOfSpeech && <span>{item.partOfSpeech}</span>}
                  </td>
                  <td>{item.meaningVi || "-"}</td>
                  <td>{item.deckName}</td>
                  <td>
                    <span className={`status-pill ${statusClass(item.status)}`}>{item.status}</span>
                  </td>
                  <td>
                    {item.correctCount}/{item.wrongCount}/{item.lapseCount}
                    <span className="status-message">rep {item.repetitionCount}</span>
                  </td>
                  <td>{absoluteTime(item.nextReviewAt)}</td>
                </tr>
              ))}
              {!isLoading && items.length === 0 && (
                <tr>
                  <td colSpan={7}>No scheduled words.</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </section>
    </AppShell>
  );
}

function bucketClass(bucket: ReviewScheduleBucket) {
  if (bucket === "OVERDUE") {
    return "bad";
  }
  if (bucket === "DUE_NOW" || bucket === "NEW") {
    return "ok";
  }
  return "neutral";
}

function statusClass(status: VocabProgressStatus) {
  if (status === "MASTERED" || status === "REVIEW") {
    return "ok";
  }
  if (status === "DIFFICULT") {
    return "bad";
  }
  return "neutral";
}

function relativeTime(item: ReviewScheduleItem) {
  if (item.bucket === "NEW") {
    return "ready";
  }
  if (item.minutesUntilReview <= 0) {
    return "ready";
  }
  if (item.minutesUntilReview < 60) {
    return `${item.minutesUntilReview} min`;
  }
  if (item.minutesUntilReview < 24 * 60) {
    return `${Math.round(item.minutesUntilReview / 60)} hr`;
  }
  return `${Math.round(item.minutesUntilReview / 1440)} days`;
}

function absoluteTime(value: string | null) {
  if (!value) {
    return "Ready";
  }
  const date = new Date(`${value}Z`);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return new Intl.DateTimeFormat("en", {
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit"
  }).format(date);
}

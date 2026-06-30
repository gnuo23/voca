"use client";

import { useEffect, useMemo, useState } from "react";
import { Gauge, RefreshCw, Trophy } from "lucide-react";
import { listDeckVocab, VocabItem } from "@/lib/api";

type MatchGamePanelProps = {
  token: string;
  deckId: string;
  totalWords: number;
};

type MatchCard = {
  id: string;
  pairId: number;
  label: string;
  side: "word" | "meaning";
};

export function MatchGamePanel({ token, deckId, totalWords }: MatchGamePanelProps) {
  const [cards, setCards] = useState<MatchCard[]>([]);
  const [selectedIds, setSelectedIds] = useState<string[]>([]);
  const [matchedPairIds, setMatchedPairIds] = useState<Set<number>>(new Set());
  const [attempts, setAttempts] = useState(0);
  const [startedAt, setStartedAt] = useState<number | null>(null);
  const [elapsedMs, setElapsedMs] = useState(0);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState("");

  const complete = cards.length > 0 && matchedPairIds.size === cards.length / 2;

  useEffect(() => {
    if (!startedAt || complete) {
      return;
    }
    const timer = window.setInterval(() => setElapsedMs(Date.now() - startedAt), 250);
    return () => window.clearInterval(timer);
  }, [complete, startedAt]);

  async function startGame() {
    setIsLoading(true);
    setError("");
    setSelectedIds([]);
    setMatchedPairIds(new Set());
    setAttempts(0);
    setElapsedMs(0);

    try {
      const vocab = await listDeckVocab(token, deckId);
      const playable = shuffle(vocab.filter((item) => item.meaningVi?.trim())).slice(0, 6);
      if (playable.length < 2) {
        throw new Error("Need at least 2 vocabulary items with meanings.");
      }
      setCards(buildCards(playable));
      setStartedAt(Date.now());
    } catch (err) {
      setCards([]);
      setStartedAt(null);
      setError(err instanceof Error ? err.message : "Could not start match game");
    } finally {
      setIsLoading(false);
    }
  }

  function choose(card: MatchCard) {
    if (matchedPairIds.has(card.pairId) || selectedIds.includes(card.id) || selectedIds.length >= 2 || complete) {
      return;
    }

    const nextSelected = [...selectedIds, card.id];
    setSelectedIds(nextSelected);
    if (nextSelected.length !== 2) {
      return;
    }

    setAttempts((value) => value + 1);
    const [first, second] = nextSelected.map((id) => cards.find((item) => item.id === id));
    if (first && second && first.pairId === second.pairId && first.side !== second.side) {
      setMatchedPairIds((current) => new Set(current).add(first.pairId));
      window.setTimeout(() => setSelectedIds([]), 250);
      return;
    }
    window.setTimeout(() => setSelectedIds([]), 700);
  }

  const elapsedLabel = useMemo(() => formatElapsed(elapsedMs), [elapsedMs]);

  return (
    <section className="card match-card">
      <div className="section-heading">
        <div>
          <h2>Match</h2>
          <p>{cards.length ? `${matchedPairIds.size}/${cards.length / 2} pairs · ${attempts} attempts` : "Timed word and meaning matching"}</p>
        </div>
        <div className="button-row">
          <span className="status">
            <Gauge size={16} aria-hidden="true" />
            {elapsedLabel}
          </span>
          <button className="button" type="button" onClick={startGame} disabled={isLoading || totalWords < 2}>
            <RefreshCw size={18} aria-hidden="true" />
            {cards.length ? "Restart" : "Start Match"}
          </button>
        </div>
      </div>

      {error && <p className="form-error">{error}</p>}
      {totalWords < 2 && <p className="form-muted">Need at least 2 vocabulary items with meanings.</p>}

      {cards.length > 0 && (
        <div className="match-grid">
          {cards.map((card) => {
            const selected = selectedIds.includes(card.id);
            const matched = matchedPairIds.has(card.pairId);
            return (
              <button
                key={card.id}
                className={`match-tile ${selected ? "selected" : ""} ${matched ? "matched" : ""}`}
                type="button"
                onClick={() => choose(card)}
                disabled={matched}
              >
                {card.label}
              </button>
            );
          })}
        </div>
      )}

      {complete && (
        <div className="quiz-feedback correct">
          <strong>
            <Trophy size={18} aria-hidden="true" />
            Complete
          </strong>
          <p>{elapsedLabel} · {attempts} attempts</p>
        </div>
      )}
    </section>
  );
}

function buildCards(items: VocabItem[]) {
  return shuffle(
    items.flatMap((item) => [
      { id: `${item.id}-word`, pairId: item.id, label: item.word, side: "word" as const },
      { id: `${item.id}-meaning`, pairId: item.id, label: item.meaningVi ?? "", side: "meaning" as const }
    ])
  );
}

function shuffle<T>(items: T[]) {
  const copy = [...items];
  for (let index = copy.length - 1; index > 0; index -= 1) {
    const swapIndex = Math.floor(Math.random() * (index + 1));
    [copy[index], copy[swapIndex]] = [copy[swapIndex], copy[index]];
  }
  return copy;
}

function formatElapsed(ms: number) {
  const totalSeconds = Math.floor(ms / 1000);
  const minutes = Math.floor(totalSeconds / 60).toString().padStart(2, "0");
  const seconds = (totalSeconds % 60).toString().padStart(2, "0");
  return `${minutes}:${seconds}`;
}

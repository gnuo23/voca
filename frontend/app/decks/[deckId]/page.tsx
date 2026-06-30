"use client";

import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { Trash2 } from "lucide-react";
import { AppShell } from "@/components/AppShell";
import { DeckForm } from "@/components/decks/DeckForm";
import { LearnPanel } from "@/components/learn/LearnPanel";
import { MatchGamePanel } from "@/components/match/MatchGamePanel";
import { QuizPanel } from "@/components/quiz/QuizPanel";
import { VocabImportPanel } from "@/components/vocab/VocabImportPanel";
import { VocabStudyPanel } from "@/components/vocab/VocabStudyPanel";
import { Deck, deleteDeck, getDeck, getStoredToken, updateDeck } from "@/lib/api";

export default function DeckDetailPage() {
  const params = useParams<{ deckId: string }>();
  const router = useRouter();
  const [token, setToken] = useState("");
  const [deck, setDeck] = useState<Deck | null>(null);
  const [saved, setSaved] = useState(false);
  const [error, setError] = useState("");
  const [vocabRefreshKey, setVocabRefreshKey] = useState(0);

  useEffect(() => {
    const storedToken = getStoredToken();
    if (!storedToken) {
      router.push("/login");
      return;
    }

    setToken(storedToken);
    getDeck(storedToken, params.deckId)
      .then(setDeck)
      .catch(() => router.push("/decks"));
  }, [params.deckId, router]);

  async function handleDelete() {
    setError("");
    try {
      await deleteDeck(token, params.deckId);
      router.push("/decks");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not delete deck");
    }
  }

  async function refreshDeck() {
    if (!token) {
      return;
    }

    const refreshed = await getDeck(token, params.deckId);
    setDeck(refreshed);
  }

  async function handleImported() {
    await refreshDeck();
    setVocabRefreshKey((value) => value + 1);
  }

  return (
    <AppShell>
      <header className="topbar">
        <div>
          <h1>{deck?.name ?? "Deck Detail"}</h1>
          <p>{deck ? `${deck.totalWords} words · ${deck.learnedWords} learned · ${deck.dueWords} due` : "Loading deck"}</p>
        </div>
        <button className="button danger-button" type="button" onClick={handleDelete} disabled={!deck}>
          <Trash2 size={18} aria-hidden="true" />
          Delete
        </button>
      </header>

      <section className="grid">
        <article className="card">
          <h2>Progress</h2>
          <div className="metric">{deck?.totalWords ?? 0}</div>
          <p>Total words</p>
        </article>
        <article className="card">
          <h2>Learned</h2>
          <div className="metric">{deck?.learnedWords ?? 0}</div>
          <p>Placeholder</p>
        </article>
        <article className="card">
          <h2>Due</h2>
          <div className="metric">{deck?.dueWords ?? 0}</div>
          <p>Placeholder</p>
        </article>
      </section>

      <section className="card profile-card deck-edit-card">
        <h2>Edit deck</h2>
        {deck && (
          <DeckForm
            initialDeck={deck}
            submitLabel="Save deck"
            onSubmit={async (payload) => {
              const updated = await updateDeck(token, params.deckId, payload);
              setDeck(updated);
              setSaved(true);
            }}
          />
        )}
        {saved && <p className="form-success">Deck saved.</p>}
        {error && <p className="form-error">{error}</p>}
      </section>

      {deck && token && (
        <VocabStudyPanel
          token={token}
          deckId={params.deckId}
          refreshDeck={refreshDeck}
          refreshKey={vocabRefreshKey}
        />
      )}

      {deck && token && (
        <LearnPanel
          token={token}
          deckId={params.deckId}
          totalWords={deck.totalWords}
          refreshDeck={refreshDeck}
        />
      )}

      {deck && token && (
        <QuizPanel
          token={token}
          deckId={params.deckId}
          totalWords={deck.totalWords}
          refreshDeck={refreshDeck}
        />
      )}

      {deck && token && (
        <MatchGamePanel
          token={token}
          deckId={params.deckId}
          totalWords={deck.totalWords}
        />
      )}

      {deck && token && (
        <VocabImportPanel
          token={token}
          deckId={params.deckId}
          onImported={handleImported}
        />
      )}
    </AppShell>
  );
}

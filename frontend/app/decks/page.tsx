"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { Plus, Layers } from "lucide-react";
import { AppShell } from "@/components/AppShell";
import { Deck, getStoredToken, listDecks } from "@/lib/api";

export default function DecksPage() {
  const router = useRouter();
  const [decks, setDecks] = useState<Deck[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const token = getStoredToken();
    if (!token) {
      router.push("/login");
      return;
    }

    listDecks(token)
      .then(setDecks)
      .catch(() => router.push("/login"))
      .finally(() => setIsLoading(false));
  }, [router]);

  return (
    <AppShell>
      <header className="topbar">
        <div>
          <h1>My Decks</h1>
          <p>{isLoading ? "Loading decks" : `${decks.length} deck${decks.length === 1 ? "" : "s"}`}</p>
        </div>
        <Link className="button" href="/decks/new">
          <Plus size={18} aria-hidden="true" />
          New deck
        </Link>
      </header>

      {decks.length === 0 && !isLoading ? (
        <section className="empty-state">
          <Layers size={28} aria-hidden="true" />
          <h2>No decks yet</h2>
          <p>Create your first vocabulary deck.</p>
          <Link className="button" href="/decks/new">
            <Plus size={18} aria-hidden="true" />
            New deck
          </Link>
        </section>
      ) : (
        <section className="deck-list" aria-label="Vocabulary decks">
          {decks.map((deck) => (
            <Link className="card deck-card" key={deck.id} href={`/decks/${deck.id}`}>
              <div>
                <h2>{deck.name}</h2>
                <p>{deck.description || "No description"}</p>
              </div>
              <div className="progress-row">
                <span>{deck.totalWords} words</span>
                <span>{deck.learnedWords} learned</span>
                <span>{deck.dueWords} due</span>
              </div>
            </Link>
          ))}
        </section>
      )}
    </AppShell>
  );
}

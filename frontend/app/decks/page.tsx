"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { Plus, Layers } from "lucide-react";
import { AppShell } from "@/components/AppShell";
import { DeckImportFromCodePanel } from "@/components/decks/DeckImportFromCodePanel";
import { Deck, getStoredToken, listDecks } from "@/lib/api";

export default function DecksPage() {
  const router = useRouter();
  const [token, setToken] = useState("");
  const [decks, setDecks] = useState<Deck[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const storedToken = getStoredToken();
    if (!storedToken) {
      router.push("/login");
      return;
    }

    setToken(storedToken);
    listDecks(storedToken)
      .then(setDecks)
      .catch(() => router.push("/login"))
      .finally(() => setIsLoading(false));
  }, [router]);

  async function refreshDecks() {
    if (!token) return;
    try {
      setDecks(await listDecks(token));
    } catch {
      // ignore - the import flow will navigate away on success
    }
  }

  return (
    <AppShell>
      <header className="topbar">
        <div>
          <h1>Bộ thẻ của tôi</h1>
          <p>{isLoading ? "Đang tải…" : `${decks.length} deck`}</p>
        </div>
        <div className="button-row">
          {token && (
            <DeckImportFromCodePanel token={token} onImported={refreshDecks} />
          )}
          <Link className="button" href="/decks/new">
            <Plus size={18} aria-hidden="true" />
            Deck mới
          </Link>
        </div>
      </header>

      {decks.length === 0 && !isLoading ? (
        <section className="empty-state">
          <Layers size={28} aria-hidden="true" />
          <h2>Chưa có deck nào</h2>
          <p>Tạo bộ thẻ đầu tiên hoặc nhập mã chia sẻ từ người khác.</p>
          <div className="button-row">
            <Link className="button" href="/decks/new">
              <Plus size={18} aria-hidden="true" />
              Tạo deck mới
            </Link>
            {token && (
              <DeckImportFromCodePanel token={token} onImported={refreshDecks} />
            )}
          </div>
        </section>
      ) : (
        <section className="deck-list" aria-label="Vocabulary decks">
          {decks.map((deck) => (
            <Link className="card deck-card" key={deck.id} href={`/decks/${deck.id}`}>
              <div>
                <h2>{deck.name}</h2>
                <p>{deck.description || "Chưa có mô tả"}</p>
              </div>
              <div className="progress-row">
                <span>{deck.totalWords} từ</span>
                <span>{deck.learnedWords} đã thuộc</span>
                <span>{deck.dueWords} cần ôn</span>
              </div>
            </Link>
          ))}
        </section>
      )}
    </AppShell>
  );
}

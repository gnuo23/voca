"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { AppShell } from "@/components/AppShell";
import { DeckForm } from "@/components/decks/DeckForm";
import { createDeck, getStoredToken } from "@/lib/api";

export default function NewDeckPage() {
  const router = useRouter();
  const [token, setToken] = useState("");

  useEffect(() => {
    const storedToken = getStoredToken();
    if (!storedToken) {
      router.push("/login");
      return;
    }

    setToken(storedToken);
  }, [router]);

  return (
    <AppShell>
      <header className="topbar">
        <div>
          <h1>New Deck</h1>
          <p>Create a vocabulary collection.</p>
        </div>
      </header>

      <section className="card profile-card">
        <DeckForm
          submitLabel="Create deck"
          onSubmit={async (payload) => {
            const deck = await createDeck(token, payload);
            router.push(`/mydeck/${deck.id}`);
          }}
        />
      </section>
    </AppShell>
  );
}

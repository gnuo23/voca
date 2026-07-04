"use client";

import { useParams } from "next/navigation";
import { DeckDetailController } from "@/components/decks/DeckDetailController";

export default function MyDeckDetailPage() {
  const params = useParams<{ deckId: string }>();

  return (
    <DeckDetailController
      deckId={params.deckId}
      learnHref={`/mydeck/${params.deckId}/learn`}
      notFoundHref="/decks"
    />
  );
}

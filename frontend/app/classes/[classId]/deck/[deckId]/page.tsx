"use client";

import { useParams } from "next/navigation";
import { DeckDetailController } from "@/components/decks/DeckDetailController";

export default function ClassDeckDetailPage() {
  const params = useParams<{ classId: string; deckId: string }>();

  return (
    <DeckDetailController
      deckId={params.deckId}
      classId={params.classId}
      learnHref={`/classes/${params.classId}/deck/${params.deckId}/learn`}
      notFoundHref={`/classes/${params.classId}`}
    />
  );
}

"use client";

import { useParams } from "next/navigation";
import { LearnPageController } from "@/components/learn/LearnPageController";

export default function MyDeckLearnPage() {
  const params = useParams<{ deckId: string }>();

  return (
    <LearnPageController
      deckId={params.deckId}
      backHref={`/mydeck/${params.deckId}`}
    />
  );
}

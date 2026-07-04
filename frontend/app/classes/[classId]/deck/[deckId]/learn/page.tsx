"use client";

import { useParams } from "next/navigation";
import { LearnPageController } from "@/components/learn/LearnPageController";

export default function ClassDeckLearnPage() {
  const params = useParams<{ classId: string; deckId: string }>();

  return (
    <LearnPageController
      deckId={params.deckId}
      classId={params.classId}
      backHref={`/classes/${params.classId}/deck/${params.deckId}`}
    />
  );
}

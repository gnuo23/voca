"use client";

import { FormEvent, useState } from "react";
import { Save } from "lucide-react";
import { Deck, DeckPayload } from "@/lib/api";

type DeckFormProps = {
  initialDeck?: Deck;
  submitLabel: string;
  onSubmit: (payload: DeckPayload) => Promise<void>;
};

export function DeckForm({ initialDeck, submitLabel, onSubmit }: DeckFormProps) {
  const [error, setError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    setIsSubmitting(true);

    const formData = new FormData(event.currentTarget);

    try {
      await onSubmit({
        name: String(formData.get("name")),
        description: String(formData.get("description"))
      });
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not save deck");
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <form onSubmit={handleSubmit}>
      <div className="field">
        <label htmlFor="name">Deck name</label>
        <input id="name" name="name" defaultValue={initialDeck?.name ?? ""} maxLength={160} required />
      </div>
      <div className="field">
        <label htmlFor="description">Description</label>
        <textarea
          id="description"
          name="description"
          defaultValue={initialDeck?.description ?? ""}
          maxLength={1000}
          rows={4}
        />
      </div>
      {error && <p className="form-error">{error}</p>}
      <button className="button" type="submit" disabled={isSubmitting}>
        <Save size={18} aria-hidden="true" />
        {isSubmitting ? "Saving" : submitLabel}
      </button>
    </form>
  );
}

"use client";

import { useEffect, useMemo, useState } from "react";
import { Check, Flame, Pencil, RotateCcw, Sparkles, Trash2, Volume1, Volume2, X } from "lucide-react";
import {
  deleteVocabItem,
  enrichDeck,
  EnrichmentJob,
  getEnrichmentJob,
  getVocabAudio,
  listDeckVocab,
  markVocabItem,
  updateVocabItem,
  VocabAudio,
  VocabItem,
  VocabItemPayload,
  VocabMarkAction
} from "@/lib/api";

type VocabStudyPanelProps = {
  token: string;
  deckId: string;
  canEdit?: boolean;
  refreshDeck: () => void;
  refreshKey: number;
};

const emptyForm: VocabItemPayload = {
  word: "",
  partOfSpeech: "",
  meaningVi: ""
};

export function VocabStudyPanel({ token, deckId, canEdit = true, refreshDeck, refreshKey }: VocabStudyPanelProps) {
  const [items, setItems] = useState<VocabItem[]>([]);
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [editing, setEditing] = useState<VocabItem | null>(null);
  const [form, setForm] = useState<VocabItemPayload>(emptyForm);
  const [studyOpen, setStudyOpen] = useState(false);
  const [cardIndex, setCardIndex] = useState(0);
  const [isFlipped, setIsFlipped] = useState(false);
  const [enrichmentJob, setEnrichmentJob] = useState<EnrichmentJob | null>(null);

  useEffect(() => {
    loadItems();
  }, [deckId, refreshKey]);

  useEffect(() => {
    if (!enrichmentJob || enrichmentJob.status === "DONE" || enrichmentJob.status === "FAILED") {
      return;
    }

    const timer = window.setInterval(async () => {
      try {
        const job = await getEnrichmentJob(token, enrichmentJob.id);
        setEnrichmentJob(job);
        if (job.status === "DONE") {
          await loadItems();
          refreshDeck();
        }
      } catch (err) {
        setError(err instanceof Error ? err.message : "Could not refresh AI job");
      }
    }, 1200);

    return () => window.clearInterval(timer);
  }, [enrichmentJob, token]);

  const currentCard = useMemo(() => {
    if (items.length === 0) {
      return null;
    }
    return items[Math.min(cardIndex, items.length - 1)];
  }, [cardIndex, items]);

  async function loadItems() {
    setIsLoading(true);
    setError("");

    try {
      const result = await listDeckVocab(token, deckId);
      setItems(result);
      setCardIndex((index) => Math.min(index, Math.max(result.length - 1, 0)));
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not load vocabulary");
    } finally {
      setIsLoading(false);
    }
  }

  function startEdit(item: VocabItem) {
    if (!canEdit) {
      return;
    }
    setEditing(item);
    setForm({
      word: item.word,
      partOfSpeech: item.partOfSpeech ?? "",
      meaningVi: item.meaningVi ?? ""
    });
  }

  async function handleSaveEdit() {
    if (!editing) {
      return;
    }

    setError("");
    try {
      const updated = await updateVocabItem(token, editing.id, form);
      setItems((current) => current.map((item) => (item.id === updated.id ? updated : item)));
      setEditing(null);
      refreshDeck();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not update vocabulary item");
    }
  }

  async function handleDelete(item: VocabItem) {
    setError("");
    try {
      await deleteVocabItem(token, item.id);
      setItems((current) => current.filter((candidate) => candidate.id !== item.id));
      setEditing(null);
      refreshDeck();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not delete vocabulary item");
    }
  }

  async function handleMark(action: VocabMarkAction) {
    if (!currentCard) {
      return;
    }

    setError("");
    try {
      const updated = await markVocabItem(token, currentCard.id, action);
      setItems((current) => current.map((item) => (item.id === updated.id ? updated : item)));
      setIsFlipped(false);
      setCardIndex((index) => (items.length === 0 ? 0 : (index + 1) % items.length));
      refreshDeck();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not save progress");
    }
  }

  async function handleEnrichDeck() {
    if (!canEdit) {
      return;
    }
    setError("");
    try {
      const job = await enrichDeck(token, deckId);
      setEnrichmentJob(job);
      if (job.status === "DONE") {
        await loadItems();
        refreshDeck();
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not start AI enrichment");
    }
  }

  async function playPronunciation(item: VocabItem, slow: boolean, accent: "US" | "UK" | "DEFAULT" = "DEFAULT") {
    setError("");

    try {
      let audio: VocabAudio = {
        vocabId: item.id,
        word: item.word,
        audioUrl: item.audioUrl,
        audioUsUrl: item.audioUsUrl,
        audioUkUrl: item.audioUkUrl,
        audioAccent: item.audioAccent,
        audioSource: item.audioSource,
        audioRefreshedAt: item.audioRefreshedAt
      };

      if (!audio.audioRefreshedAt) {
        audio = await getVocabAudio(token, item.id);
        setItems((current) =>
          current.map((candidate) =>
            candidate.id === item.id
              ? {
                  ...candidate,
                  audioUrl: audio.audioUrl,
                  audioUsUrl: audio.audioUsUrl,
                  audioUkUrl: audio.audioUkUrl,
                  audioAccent: audio.audioAccent,
                  audioSource: audio.audioSource,
                  audioRefreshedAt: audio.audioRefreshedAt
                }
              : candidate
          )
        );
      }

      const audioUrl = pickAudioUrl(audio, accent);
      if (audioUrl) {
        await playAudioUrl(audioUrl, slow);
        return;
      }

      speakWithBrowser(item.word, slow, accent);
    } catch (err) {
      try {
        speakWithBrowser(item.word, slow, accent);
      } catch {
        setError(err instanceof Error ? err.message : "Could not play pronunciation");
      }
    }
  }

  const isEnriching = enrichmentJob?.status === "PENDING" || enrichmentJob?.status === "PROCESSING";

  return (
    <section className="card vocab-card">
      <div className="section-heading">
        <div>
          <h2>Vocabulary</h2>
          <p>{isLoading ? "Loading" : `${items.length} item${items.length === 1 ? "" : "s"}`}</p>
        </div>
        <div className="button-row">
          {canEdit && (
            <button
              className="button secondary-button"
              type="button"
              onClick={handleEnrichDeck}
              disabled={items.length === 0 || isEnriching}
            >
              <Sparkles size={18} aria-hidden="true" />
              {isEnriching ? "Generating" : "Generate AI"}
            </button>
          )}
          <button
            className="button"
            type="button"
            onClick={() => {
              setStudyOpen((open) => !open);
              setIsFlipped(false);
            }}
            disabled={items.length === 0}
          >
            <RotateCcw size={18} aria-hidden="true" />
            {studyOpen ? "Close Study" : "Study"}
          </button>
        </div>
      </div>

      {error && <p className="form-error">{error}</p>}
      {enrichmentJob && (
        <div className={`job-status ${enrichmentJob.status.toLowerCase()}`}>
          <span>{enrichmentJob.status}</span>
          <strong>{enrichmentJob.processedItems}/{enrichmentJob.totalItems}</strong>
          {enrichmentJob.errorMessage && <p>{enrichmentJob.errorMessage}</p>}
        </div>
      )}

      {studyOpen && currentCard && (
        <div className="study-panel">
          <button className="flashcard" type="button" onClick={() => setIsFlipped((value) => !value)}>
            <span className="flashcard-meta">
              {cardIndex + 1} / {items.length}
              <span className={`status-pill ${["REVIEW", "MASTERED"].includes(currentCard.progressStatus) ? "ok" : "neutral"}`}>
                {currentCard.progressStatus}
              </span>
            </span>
            <span className="flashcard-word">
              {isFlipped ? currentCard.meaningVi || "-" : currentCard.word}
            </span>
            <span className="flashcard-pos">
              {isFlipped ? currentCard.partOfSpeech || "-" : currentCard.partOfSpeech || "Vocabulary"}
            </span>
          </button>

          <div className="audio-actions">
            <button className="icon-text-button" type="button" onClick={() => playPronunciation(currentCard, false, "DEFAULT")}>
              <Volume2 size={18} aria-hidden="true" />
              Listen
            </button>
            <button className="icon-text-button" type="button" onClick={() => playPronunciation(currentCard, true, "DEFAULT")}>
              <Volume1 size={18} aria-hidden="true" />
              Slow
            </button>
            {(currentCard.audioUsUrl || currentCard.audioUkUrl) && (
              <>
                {currentCard.audioUsUrl && (
                  <button className="icon-text-button" type="button" onClick={() => playPronunciation(currentCard, false, "US")}>
                    US
                  </button>
                )}
                {currentCard.audioUkUrl && (
                  <button className="icon-text-button" type="button" onClick={() => playPronunciation(currentCard, false, "UK")}>
                    UK
                  </button>
                )}
              </>
            )}
          </div>

          <div className="study-actions">
            <button className="button danger-button" type="button" onClick={() => handleMark("UNKNOWN")}>
              <X size={18} aria-hidden="true" />
              Unknown
            </button>
            <button className="button warning-button" type="button" onClick={() => handleMark("DIFFICULT")}>
              <Flame size={18} aria-hidden="true" />
              Difficult
            </button>
            <button className="button" type="button" onClick={() => handleMark("KNOWN")}>
              <Check size={18} aria-hidden="true" />
              Known
            </button>
          </div>
        </div>
      )}

      {editing && canEdit && (
        <div className="edit-vocab-form">
          <div className="form-grid">
            <div className="field">
              <label htmlFor="edit-word">Word</label>
              <input
                id="edit-word"
                value={form.word}
                onChange={(event) => setForm((current) => ({ ...current, word: event.target.value }))}
              />
            </div>
            <div className="field">
              <label htmlFor="edit-pos">POS</label>
              <input
                id="edit-pos"
                value={form.partOfSpeech}
                onChange={(event) => setForm((current) => ({ ...current, partOfSpeech: event.target.value }))}
              />
            </div>
            <div className="field wide">
              <label htmlFor="edit-meaning">Meaning</label>
              <input
                id="edit-meaning"
                value={form.meaningVi}
                onChange={(event) => setForm((current) => ({ ...current, meaningVi: event.target.value }))}
              />
            </div>
          </div>
          <div className="button-row">
            <button className="button" type="button" onClick={handleSaveEdit} disabled={form.word.trim().length === 0}>
              <Check size={18} aria-hidden="true" />
              Save
            </button>
            <button className="button secondary-button" type="button" onClick={() => setEditing(null)}>
              <X size={18} aria-hidden="true" />
              Cancel
            </button>
          </div>
        </div>
      )}

      {items.length === 0 && !isLoading ? (
        <div className="empty-state">
          <h2>No vocabulary yet</h2>
          <p>{canEdit ? "Import a list below to start building this deck." : "Deck này chưa có từ vựng."}</p>
        </div>
      ) : (
        <div className="preview-wrap">
          <table className="preview-table vocab-table">
            <thead>
              <tr>
                <th>Word</th>
                <th>POS</th>
                <th>Meaning</th>
                <th>Status</th>
                <th>Marks</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {items.map((item) => (
                <tr key={item.id}>
                  <td>{item.word}</td>
                  <td>{item.partOfSpeech || "-"}</td>
                  <td>{item.meaningVi || "-"}</td>
                  <td>
                    <span className={`status-pill ${["REVIEW", "MASTERED"].includes(item.progressStatus) ? "ok" : "neutral"}`}>
                      {item.progressStatus}
                    </span>
                    {item.enrichedAt && <span className="status-message">AI enriched</span>}
                  </td>
                  <td>
                    <span>{item.knownCount}/{item.unknownCount}/{item.difficultCount}</span>
                    {item.ipa && <span className="status-message">{item.ipa}</span>}
                    {item.audioUrl && <span className="status-message">Audio ready</span>}
                  </td>
                  <td>
                    <div className="table-actions">
                      <button className="icon-button" type="button" onClick={() => playPronunciation(item, false, "DEFAULT")} aria-label={`Listen to ${item.word}`}>
                        <Volume2 size={17} aria-hidden="true" />
                      </button>
                      <button className="icon-button" type="button" onClick={() => playPronunciation(item, true, "DEFAULT")} aria-label={`Listen slowly to ${item.word}`}>
                        <Volume1 size={17} aria-hidden="true" />
                      </button>
                      {canEdit && (
                        <>
                          <button className="icon-button" type="button" onClick={() => startEdit(item)} aria-label={`Edit ${item.word}`}>
                            <Pencil size={17} aria-hidden="true" />
                          </button>
                          <button className="icon-button danger-icon" type="button" onClick={() => handleDelete(item)} aria-label={`Delete ${item.word}`}>
                            <Trash2 size={17} aria-hidden="true" />
                          </button>
                        </>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}

function pickAudioUrl(audio: VocabAudio, accent: "US" | "UK" | "DEFAULT") {
  if (accent === "US") {
    return audio.audioUsUrl ?? audio.audioUrl;
  }
  if (accent === "UK") {
    return audio.audioUkUrl ?? audio.audioUrl;
  }
  return audio.audioUrl ?? audio.audioUsUrl ?? audio.audioUkUrl;
}

async function playAudioUrl(audioUrl: string, slow: boolean) {
  const audio = new Audio(audioUrl);
  audio.playbackRate = slow ? 0.75 : 1;
  await audio.play();
}

function speakWithBrowser(word: string, slow: boolean, accent: "US" | "UK" | "DEFAULT") {
  if (typeof window === "undefined" || !("speechSynthesis" in window)) {
    throw new Error("Browser speech is not available");
  }

  window.speechSynthesis.cancel();
  const utterance = new SpeechSynthesisUtterance(word);
  utterance.lang = accent === "UK" ? "en-GB" : "en-US";
  utterance.rate = slow ? 0.72 : 1;
  window.speechSynthesis.speak(utterance);
}

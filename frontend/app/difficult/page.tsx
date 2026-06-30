"use client";

import { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { AlertTriangle, Check, RefreshCw, RotateCcw, Volume2, X } from "lucide-react";
import { AppShell } from "@/components/AppShell";
import {
  Deck,
  getReviewSchedule,
  getStoredToken,
  listDecks,
  listDeckVocab,
  ReviewScheduleItem,
  VocabItem
} from "@/lib/api";

type PracticeDirection = "EN_TO_VI" | "VI_TO_EN";

type PracticeQuestion = {
  item: ReviewScheduleItem;
  direction: PracticeDirection;
  prompt: string;
  promptAudio: string | null;
  correctAnswer: string;
  options: string[];
};

function playEnglishPronunciation(text: string) {
  if (typeof window === "undefined" || !("speechSynthesis" in window)) return;
  window.speechSynthesis.cancel();
  const utterance = new SpeechSynthesisUtterance(text);
  utterance.lang = "en-US";
  utterance.rate = 0.92;
  window.speechSynthesis.speak(utterance);
}

function PronounceButton({ text, className = "" }: { text: string; className?: string }) {
  return (
    <button
      type="button"
      className={`learn-audio-btn ${className}`}
      onClick={(event) => {
        event.stopPropagation();
        playEnglishPronunciation(text);
      }}
      aria-label={`Listen to ${text}`}
      title={`Listen to ${text}`}
    >
      <Volume2 size={18} aria-hidden="true" />
    </button>
  );
}

export default function DifficultPracticePage() {
  const router = useRouter();
  const [token, setToken] = useState("");
  const [decks, setDecks] = useState<Deck[]>([]);
  const [deckId, setDeckId] = useState("");
  const [difficultItems, setDifficultItems] = useState<ReviewScheduleItem[]>([]);
  const [vocabPool, setVocabPool] = useState<VocabItem[]>([]);
  const [question, setQuestion] = useState<PracticeQuestion | null>(null);
  const [answered, setAnswered] = useState<string | null>(null);
  const [correctCount, setCorrectCount] = useState(0);
  const [answeredCount, setAnsweredCount] = useState(0);
  const [error, setError] = useState("");
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
      .catch(() => setDecks([]));
  }, [router]);

  useEffect(() => {
    if (!token) return;
    void loadPracticeData(token, deckId);
  }, [token, deckId]);

  const selectedDeckName = useMemo(() => decks.find((deck) => String(deck.id) === deckId)?.name ?? "All decks", [decks, deckId]);

  async function loadPracticeData(authToken = token, selectedDeckId = deckId) {
    setIsLoading(true);
    setError("");
    try {
      const [schedule, loadedDecks] = await Promise.all([
        getReviewSchedule(authToken, { deckId: selectedDeckId, status: "DIFFICULT", limit: 500 }),
        decks.length > 0 ? Promise.resolve(decks) : listDecks(authToken)
      ]);
      if (decks.length === 0) {
        setDecks(loadedDecks);
      }
      const decksForPool = selectedDeckId
        ? loadedDecks.filter((deck) => String(deck.id) === selectedDeckId)
        : loadedDecks;
      const vocabGroups = await Promise.all(decksForPool.map((deck) => listDeckVocab(authToken, String(deck.id))));
      const pool = vocabGroups.flat().filter((item) => hasText(item.word) && hasText(item.meaningVi));
      setDifficultItems(schedule.items);
      setVocabPool(pool);
      setAnswered(null);
      setCorrectCount(0);
      setAnsweredCount(0);
      setQuestion(buildQuestion(schedule.items, pool));
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not load difficult words");
    } finally {
      setIsLoading(false);
    }
  }

  function answerOption(option: string) {
    if (!question || answered) return;
    setAnswered(option);
    setAnsweredCount((value) => value + 1);
    if (option === question.correctAnswer) {
      setCorrectCount((value) => value + 1);
    }
  }

  function nextQuestion() {
    setAnswered(null);
    setQuestion(buildQuestion(difficultItems, vocabPool, question?.item.vocabId));
  }

  const accuracy = answeredCount === 0 ? 0 : Math.round((correctCount * 100) / answeredCount);
  const hasEnoughData = difficultItems.length > 0 && vocabPool.length >= 2;

  return (
    <AppShell>
      <header className="topbar">
        <div>
          <h1>Difficult Practice</h1>
          <p>{isLoading ? "Loading" : `${difficultItems.length} difficult words · ${selectedDeckName}`}</p>
        </div>
        <div className="button-row">
          <label className="compact-field">
            <span>Deck</span>
            <select value={deckId} onChange={(event) => setDeckId(event.target.value)} disabled={isLoading}>
              <option value="">All decks</option>
              {decks.map((deck) => (
                <option key={deck.id} value={deck.id}>
                  {deck.name}
                </option>
              ))}
            </select>
          </label>
          <button className="button secondary-button" type="button" onClick={() => loadPracticeData()} disabled={isLoading}>
            <RefreshCw size={18} aria-hidden="true" />
            Refresh
          </button>
          <button
            className="button secondary-button"
            type="button"
            onClick={() => {
              setCorrectCount(0);
              setAnsweredCount(0);
              setAnswered(null);
              setQuestion(buildQuestion(difficultItems, vocabPool));
            }}
            disabled={!hasEnoughData}
          >
            <RotateCcw size={18} aria-hidden="true" />
            Restart
          </button>
        </div>
      </header>

      {error && <p className="form-error">{error}</p>}

      {!isLoading && !hasEnoughData && (
        <section className="empty-state difficult-empty">
          <AlertTriangle size={24} aria-hidden="true" />
          <h2>No difficult practice ready</h2>
          <p>{difficultItems.length === 0 ? "No difficult words found." : "Add at least one more word for answer choices."}</p>
        </section>
      )}

      {question && hasEnoughData && (
        <section className="difficult-practice">
          <div className="difficult-stats" aria-label="Practice stats">
            <span>{answeredCount} answered</span>
            <span>{correctCount} correct</span>
            <span>{accuracy}%</span>
          </div>

          <article className="difficult-card">
            <div className="difficult-card-top">
              <span className="status-pill bad">DIFFICULT</span>
              <span className="status-message">{question.direction === "EN_TO_VI" ? "English to Vietnamese" : "Vietnamese to English"}</span>
            </div>
            <div className="difficult-prompt-row">
              <h2>{question.prompt}</h2>
              {question.promptAudio && <PronounceButton text={question.promptAudio} />}
            </div>

            <div className="difficult-options">
              {question.options.map((option, index) => {
                const selected = answered === option;
                const isCorrect = option === question.correctAnswer;
                const optionClass = [
                  "difficult-option",
                  answered && isCorrect ? "correct" : "",
                  answered && selected && !isCorrect ? "wrong" : "",
                  answered ? "disabled" : ""
                ].filter(Boolean).join(" ");
                return (
                  <div key={`${option}-${index}`} className={optionClass}>
                    <button type="button" className="difficult-option-select" onClick={() => answerOption(option)} disabled={!!answered}>
                      <span className="quiz-option-letter">{index + 1}</span>
                      <span>{option}</span>
                    </button>
                    {question.direction === "VI_TO_EN" && <PronounceButton text={option} className="difficult-option-audio" />}
                  </div>
                );
              })}
            </div>

            {answered && (
              <div className={`quiz-feedback ${answered === question.correctAnswer ? "correct" : "incorrect"}`}>
                <strong>
                  {answered === question.correctAnswer ? <Check size={18} aria-hidden="true" /> : <X size={18} aria-hidden="true" />}
                  {answered === question.correctAnswer ? "Correct" : "Incorrect"}
                </strong>
                <p>Correct answer: {question.correctAnswer}</p>
                {question.direction === "EN_TO_VI" ? (
                  <p>{question.item.meaningVi || "-"}</p>
                ) : (
                  <p>{question.item.word}</p>
                )}
              </div>
            )}
          </article>

          <div className="review-actions">
            <button className="button" type="button" onClick={nextQuestion} disabled={!answered}>
              <Check size={18} aria-hidden="true" />
              Next
            </button>
          </div>
        </section>
      )}
    </AppShell>
  );
}

function buildQuestion(items: ReviewScheduleItem[], pool: VocabItem[], previousVocabId?: number): PracticeQuestion | null {
  const candidates = items.filter((item) => item.word && item.meaningVi);
  if (candidates.length === 0 || pool.length < 2) return null;

  const available = previousVocabId && candidates.length > 1
    ? candidates.filter((item) => item.vocabId !== previousVocabId)
    : candidates;
  const item = sample(available);
  const direction: PracticeDirection = Math.random() >= 0.5 ? "EN_TO_VI" : "VI_TO_EN";
  const correctAnswer = direction === "EN_TO_VI" ? item.meaningVi || "" : item.word;
  const prompt = direction === "EN_TO_VI" ? item.word : item.meaningVi || "";
  const distractors = pool
    .filter((vocab) => vocab.id !== item.vocabId)
    .map((vocab) => direction === "EN_TO_VI" ? vocab.meaningVi || "" : vocab.word)
    .filter((value) => hasText(value) && value !== correctAnswer);
  const options = shuffle(unique([correctAnswer, ...shuffle(distractors).slice(0, 3)]));
  if (options.length < 2) return null;

  return {
    item,
    direction,
    prompt,
    promptAudio: direction === "EN_TO_VI" ? item.word : null,
    correctAnswer,
    options
  };
}

function sample<T>(items: T[]): T {
  return items[Math.floor(Math.random() * items.length)];
}

function shuffle<T>(items: T[]): T[] {
  return [...items].sort(() => Math.random() - 0.5);
}

function unique(items: string[]): string[] {
  return Array.from(new Set(items));
}

function hasText(value: string | null | undefined): value is string {
  return Boolean(value && value.trim());
}

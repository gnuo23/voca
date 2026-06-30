"use client";

import { useEffect, useState, useCallback, useRef, useMemo } from "react";
import { useParams, useRouter } from "next/navigation";
import { X, Settings, ChevronDown } from "lucide-react";
import { LearnSetup } from "@/components/learn/LearnSetup";
import { LearnQuestion } from "@/components/learn/LearnQuestion";
import { LearnResult } from "@/components/learn/LearnResult";
import {
  Deck,
  getDeck,
  getStoredToken,
  LearnSession,
  LearnQuestion as LearnQuestionData,
  LearnAnswer,
  LearnSessionResult as LearnSessionResultData,
  LearnProgress,
  StartLearnOptions,
  startLearnSession,
  getNextLearnQuestion,
  submitLearnAnswer,
  getLearnSessionResult,
  overrideLearnAnswer,
} from "@/lib/api";

export default function LearnPage() {
  const params = useParams();
  const router = useRouter();
  const deckId = params.deckId as string;

  const [token, setToken] = useState<string | null>(null);
  const [deck, setDeck] = useState<Deck | null>(null);
  const [session, setSession] = useState<LearnSession | null>(null);
  const [question, setQuestion] = useState<LearnQuestionData | null>(null);
  const [questionTurn, setQuestionTurn] = useState(0);
  const [result, setResult] = useState<LearnSessionResultData | null>(null);
  const [progress, setProgress] = useState<LearnProgress | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const questionStartedAt = useRef<number>(0);

  // Auth + fetch deck on mount
  useEffect(() => {
    const t = getStoredToken();
    if (!t) {
      router.push("/login");
      return;
    }
    setToken(t);
    getDeck(t, deckId)
      .then(setDeck)
      .catch((err) => setError(err.message || "Failed to load deck"));
  }, [deckId, router]);

  // Record when a new question is displayed
  useEffect(() => {
    if (question) {
      questionStartedAt.current = Date.now();
    }
  }, [question]);

  // Escape key to go back
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape") {
        router.push(`/decks/${deckId}`);
      }
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [deckId, router]);

  const handleStart = useCallback(
    async (options: StartLearnOptions) => {
      if (!token) return;
      setIsLoading(true);
      setError(null);
      try {
        const newSession = await startLearnSession(token, deckId, options);
        setSession(newSession);
        const firstQuestion = await getNextLearnQuestion(token, newSession.id);
        setQuestion(firstQuestion);
        setQuestionTurn((current) => current + 1);
        setProgress(firstQuestion.progress);
      } catch (err: unknown) {
        setError(err instanceof Error ? err.message : "Failed to start session");
      } finally {
        setIsLoading(false);
      }
    },
    [token, deckId]
  );

  const handleSubmitAnswer = useCallback(
    async (answer: string): Promise<LearnAnswer | null> => {
      if (!token || !session || !question || question.sessionItemId === null) return null;
      setError(null);
      try {
        const responseTimeMs = Date.now() - questionStartedAt.current;
        const result = await submitLearnAnswer(
          token,
          session.id,
          question.sessionItemId,
          answer,
          question.questionType!,
          responseTimeMs,
          question.questionToken
        );
        setProgress(result.progress);

        // If no remaining terms, fetch result
        if (result.progress.remainingTerms === 0) {
          try {
            const sessionResult = await getLearnSessionResult(token, session.id);
            setResult(sessionResult);
          } catch (err: unknown) {
            setError(err instanceof Error ? err.message : "Failed to load results");
          }
        }

        return result;
      } catch (err: unknown) {
        setError(err instanceof Error ? err.message : "Failed to submit answer");
        return null;
      }
    },
    [token, session, question]
  );

  const handleNext = useCallback(async () => {
    if (!token || !session) return;
    // If result is already set, don't fetch next question
    if (result) return;
    setIsLoading(true);
    setError(null);
    try {
      const nextQuestion = await getNextLearnQuestion(token, session.id);
      setQuestion(nextQuestion);
      setQuestionTurn((current) => current + 1);
      setProgress(nextQuestion.progress);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "Failed to load next question");
    } finally {
      setIsLoading(false);
    }
  }, [token, session, result]);

  const handleRestart = useCallback(() => {
    setSession(null);
    setQuestion(null);
    setQuestionTurn(0);
    setResult(null);
    setProgress(null);
    setError(null);
  }, []);

  const handleBack = useCallback(() => {
    router.push(`/decks/${deckId}`);
  }, [deckId, router]);

  const handleOverride = useCallback(
    async (sessionItemId: number): Promise<LearnAnswer | null> => {
      if (!token || !session) return null;
      setError(null);
      try {
        const overrideResult = await overrideLearnAnswer(token, session.id, sessionItemId);
        setProgress(overrideResult.progress);

        // Check if session is complete after override
        if (overrideResult.progress.remainingTerms === 0) {
          try {
            const sessionResult = await getLearnSessionResult(token, session.id);
            setResult(sessionResult);
          } catch (err: unknown) {
            setError(err instanceof Error ? err.message : "Failed to load results");
          }
        }

        return overrideResult;
      } catch (err: unknown) {
        setError(err instanceof Error ? err.message : "Failed to override answer");
        return null;
      }
    },
    [token, session]
  );

  // Build segmented progress bar data
  const segments = useMemo(() => {
    if (!progress) return [];
    const total = progress.totalTerms;
    if (total === 0) return [];

    // Maximum visible segments (for large decks, we group)
    const MAX_SEGMENTS = 8;
    const segmentCount = Math.min(total, MAX_SEGMENTS);
    const termsPerSegment = total / segmentCount;

    const result: Array<{ filled: boolean; active: boolean }> = [];
    for (let i = 0; i < segmentCount; i++) {
      const segEnd = Math.round((i + 1) * termsPerSegment);
      const filled = progress.masteredTerms >= segEnd;
      const active = !filled && progress.masteredTerms >= Math.round(i * termsPerSegment);
      result.push({ filled, active });
    }
    return result;
  }, [progress]);

  return (
    <div className="learn-fullscreen">
      <div className="learn-topbar">
        <div className="learn-topbar-left">
          <span className="learn-mode-label">
            <ChevronDown size={14} />
            Học
          </span>
        </div>

        {session && (
          <div className="learn-segmented-progress">
            {segments.map((seg, i) => (
              <div
                key={i}
                className={`learn-segment ${seg.filled ? "filled" : seg.active ? "active" : ""}`}
              />
            ))}
            {progress && (
              <span className="learn-segment-counter">{progress.totalTerms}</span>
            )}
          </div>
        )}

        <div className="learn-topbar-right">
          <button
            type="button"
            className="learn-topbar-icon-btn"
            aria-label="Settings"
          >
            <Settings size={18} />
          </button>
          <button
            type="button"
            className="learn-topbar-icon-btn"
            onClick={handleBack}
            aria-label="Close"
          >
            <X size={18} />
          </button>
        </div>
      </div>

      {error && <div className="form-error">{error}</div>}

      <div className="learn-body">
        {!session && deck && (
          <LearnSetup
            deckName={deck.name}
            totalWords={deck.totalWords}
            learnedWords={deck.learnedWords}
            onStart={handleStart}
            isLoading={isLoading}
          />
        )}

        {session && question && !result && (
          <LearnQuestion
            key={questionTurn}
            question={question}
            onSubmit={handleSubmitAnswer}
            onNext={handleNext}
            onOverride={handleOverride}
            isLoading={isLoading}
          />
        )}

        {result && (
          <LearnResult
            result={result}
            onRestart={handleRestart}
            onBack={handleBack}
          />
        )}
      </div>
    </div>
  );
}

"use client";

import { useEffect, useState, useCallback, useRef, useMemo } from "react";
import { useRouter } from "next/navigation";
import { X, Settings, ChevronDown, RotateCcw } from "lucide-react";
import { LearnSetup } from "@/components/learn/LearnSetup";
import { LearnQuestion } from "@/components/learn/LearnQuestion";
import { LearnResult } from "@/components/learn/LearnResult";
import { clearStoredLearnState, readStoredLearnState, writeStoredLearnState } from "@/lib/learnStorage";
import {
  Deck,
  getClassDeck,
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
  adjustLearnQuality,
  resetLearnProgress,
  ReviewQuality,
  getActiveLearnSession,
} from "@/lib/api";

type LearnPageControllerProps = {
  deckId: string;
  classId?: string;
  backHref: string;
};

function isLearnSessionMissingError(error: unknown) {
  return error instanceof Error && error.message.toLowerCase().includes("learn session not found");
}

export function LearnPageController({ deckId, classId, backHref }: LearnPageControllerProps) {
  const router = useRouter();

  const [token, setToken] = useState<string | null>(null);
  const [deck, setDeck] = useState<Deck | null>(null);
  const [session, setSession] = useState<LearnSession | null>(null);
  const [question, setQuestion] = useState<LearnQuestionData | null>(null);
  const [questionTurn, setQuestionTurn] = useState(0);
  const [result, setResult] = useState<LearnSessionResultData | null>(null);
  const [progress, setProgress] = useState<LearnProgress | null>(null);
  const [showWrittenHint, setShowWrittenHint] = useState(true);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const questionStartedAt = useRef<number>(0);
  const restoredSessionRef = useRef(false);

  useEffect(() => {
    const t = getStoredToken();
    if (!t) {
      router.push("/login");
      return;
    }
    setToken(t);
    const loadDeck = classId ? getClassDeck(t, classId, deckId) : getDeck(t, deckId);
    loadDeck
      .then(setDeck)
      .catch((err) => setError(err.message || "Failed to load deck"));
  }, [classId, deckId, router]);

  useEffect(() => {
    if (!token || restoredSessionRef.current) return;
    restoredSessionRef.current = true;
    let cancelled = false;

    const resumeSession = async (
      activeSession: LearnSession,
      preferences: { questionTurn: number; showWrittenHint: boolean }
    ) => {
      const nextQuestion = await getNextLearnQuestion(token, activeSession.id);
      if (cancelled) return;

      setSession(activeSession);
      setShowWrittenHint(preferences.showWrittenHint);

      if (!nextQuestion.sessionItemId) {
        clearStoredLearnState(deckId);
        const sessionResult = await getLearnSessionResult(token, activeSession.id);
        if (!cancelled) {
          setResult(sessionResult);
          setQuestion(null);
          setProgress(nextQuestion.progress);
        }
        return;
      }

      setQuestion(nextQuestion);
      setProgress(nextQuestion.progress);
      setQuestionTurn(preferences.questionTurn);
      writeStoredLearnState(deckId, {
        session: activeSession,
        question: nextQuestion,
        questionTurn: preferences.questionTurn,
        progress: nextQuestion.progress,
        pendingNext: false,
        showWrittenHint: preferences.showWrittenHint,
      });
    };

    const resumeActiveSession = async (preferences?: { questionTurn?: number; showWrittenHint?: boolean }) => {
      const activeSession = await getActiveLearnSession(token, deckId);
      if (!activeSession || cancelled) return false;
      await resumeSession(activeSession, {
        questionTurn: preferences?.questionTurn ?? 1,
        showWrittenHint: preferences?.showWrittenHint ?? true,
      });
      return true;
    };

    const stored = readStoredLearnState(deckId);
    if (!stored) {
      setIsLoading(true);
      resumeActiveSession()
        .catch((err: unknown) => {
          if (!cancelled) {
            setError(err instanceof Error ? err.message : "Failed to resume session");
          }
        })
        .finally(() => {
          if (!cancelled) setIsLoading(false);
        });
      return () => {
        cancelled = true;
      };
    }

    setSession(stored.session);
    setProgress(stored.progress ?? stored.question?.progress ?? null);
    setQuestionTurn(stored.questionTurn);
    setShowWrittenHint(stored.showWrittenHint ?? true);

    if (stored.pendingNext) {
      setIsLoading(true);
      getNextLearnQuestion(token, stored.session.id)
        .then((nextQuestion) => {
          if (!nextQuestion.sessionItemId) {
            clearStoredLearnState(deckId);
            return getLearnSessionResult(token, stored.session.id).then(setResult);
          }
          setQuestion(nextQuestion);
          setProgress(nextQuestion.progress);
          setQuestionTurn((current) => current + 1);
          writeStoredLearnState(deckId, {
            session: stored.session,
            question: nextQuestion,
            questionTurn: stored.questionTurn + 1,
            progress: nextQuestion.progress,
            pendingNext: false,
            showWrittenHint: stored.showWrittenHint ?? true,
          });
        })
        .catch(async (err: unknown) => {
          clearStoredLearnState(deckId);
          try {
            const resumed = await resumeActiveSession({
              questionTurn: stored.questionTurn + 1,
              showWrittenHint: stored.showWrittenHint ?? true,
            });
            if (!resumed && !cancelled) {
              setSession(null);
              setQuestion(null);
              setProgress(null);
              setError(err instanceof Error ? err.message : "Failed to resume session");
            }
          } catch (fallbackErr: unknown) {
            if (!cancelled) {
              setSession(null);
              setQuestion(null);
              setProgress(null);
              setError(fallbackErr instanceof Error ? fallbackErr.message : "Failed to resume session");
            }
          }
        })
        .finally(() => {
          if (!cancelled) setIsLoading(false);
        });
      return () => {
        cancelled = true;
      };
    }

    if (stored.question?.sessionItemId) {
      setIsLoading(true);
      resumeActiveSession({
        questionTurn: stored.questionTurn,
        showWrittenHint: stored.showWrittenHint ?? true,
      })
        .then((resumed) => {
          if (!resumed && !cancelled) {
            clearStoredLearnState(deckId);
            setSession(null);
            setQuestion(null);
            setProgress(null);
            setQuestionTurn(0);
          }
        })
        .catch((err: unknown) => {
          if (!cancelled) {
            clearStoredLearnState(deckId);
            setSession(null);
            setQuestion(null);
            setProgress(null);
            setQuestionTurn(0);
            if (!isLearnSessionMissingError(err)) {
              setError(err instanceof Error ? err.message : "Failed to resume session");
            }
          }
        })
        .finally(() => {
          if (!cancelled) setIsLoading(false);
        });
      return () => {
        cancelled = true;
      };
    }

    clearStoredLearnState(deckId);
    setSession(null);
    return () => {
      cancelled = true;
    };
  }, [token, deckId]);

  useEffect(() => {
    if (question) {
      questionStartedAt.current = Date.now();
    }
  }, [question]);

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape") {
        router.push(backHref);
      }
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [backHref, router]);

  const handleStart = useCallback(
    async (options: StartLearnOptions, preferences: { showWrittenHint: boolean }) => {
      if (!token) return;
      setIsLoading(true);
      setError(null);
      try {
        setShowWrittenHint(preferences.showWrittenHint);
        const newSession = await startLearnSession(token, deckId, options);
        setSession(newSession);
        const firstQuestion = await getNextLearnQuestion(token, newSession.id);
        const nextQuestionTurn = 1;
        setQuestion(firstQuestion);
        setQuestionTurn(nextQuestionTurn);
        setProgress(firstQuestion.progress);
        writeStoredLearnState(deckId, {
          session: newSession,
          question: firstQuestion.sessionItemId ? firstQuestion : null,
          questionTurn: nextQuestionTurn,
          progress: firstQuestion.progress,
          pendingNext: false,
          showWrittenHint: preferences.showWrittenHint,
        });
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

        if (result.progress.remainingTerms === 0) {
          clearStoredLearnState(deckId);
          try {
            const sessionResult = await getLearnSessionResult(token, session.id);
            setResult(sessionResult);
          } catch (err: unknown) {
            setError(err instanceof Error ? err.message : "Failed to load results");
          }
        } else {
          writeStoredLearnState(deckId, {
            session,
            question: null,
            questionTurn,
            progress: result.progress,
            pendingNext: true,
            showWrittenHint,
          });
        }

        return result;
      } catch (err: unknown) {
        if (isLearnSessionMissingError(err)) {
          clearStoredLearnState(deckId);
          setSession(null);
          setQuestion(null);
          setProgress(null);
          setQuestionTurn(0);
          setResult(null);
        } else {
          setError(err instanceof Error ? err.message : "Failed to submit answer");
        }
        return null;
      }
    },
    [token, session, question, deckId, questionTurn, showWrittenHint]
  );

  const handleNext = useCallback(async () => {
    if (!token || !session) return;
    if (result) return;
    setIsLoading(true);
    setError(null);
    try {
      const nextQuestion = await getNextLearnQuestion(token, session.id);
      setQuestion(nextQuestion);
      setQuestionTurn((current) => current + 1);
      setProgress(nextQuestion.progress);
      if (nextQuestion.sessionItemId) {
        writeStoredLearnState(deckId, {
          session,
          question: nextQuestion,
          questionTurn: questionTurn + 1,
          progress: nextQuestion.progress,
          pendingNext: false,
          showWrittenHint,
        });
      } else {
        clearStoredLearnState(deckId);
      }
    } catch (err: unknown) {
      clearStoredLearnState(deckId);
      setSession(null);
      setQuestion(null);
      setProgress(null);
      setQuestionTurn(0);
      setResult(null);
      if (!isLearnSessionMissingError(err)) {
        setError(err instanceof Error ? err.message : "Failed to load next question");
      }
    } finally {
      setIsLoading(false);
    }
  }, [token, session, result, deckId, questionTurn, showWrittenHint]);

  const handleRestart = useCallback(() => {
    clearStoredLearnState(deckId);
    setSession(null);
    setQuestion(null);
    setQuestionTurn(0);
    setResult(null);
    setProgress(null);
    setError(null);
  }, [deckId]);

  const handleResetLearn = useCallback(async () => {
    if (!token || !deck) return;
    if (!confirm("Reset toàn bộ tiến độ học của deck này? Phiên Learn hiện tại cũng sẽ bắt đầu lại từ đầu.")) return;

    setIsLoading(true);
    setError(null);
    try {
      await resetLearnProgress(token, deckId);
      const updatedDeck = classId ? await getClassDeck(token, classId, deckId) : await getDeck(token, deckId);
      clearStoredLearnState(deckId);
      setDeck(updatedDeck);
      setSession(null);
      setQuestion(null);
      setQuestionTurn(0);
      setResult(null);
      setProgress(null);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "Failed to reset learn progress");
    } finally {
      setIsLoading(false);
    }
  }, [token, deck, deckId, classId]);

  const handleBack = useCallback(() => {
    router.push(backHref);
  }, [backHref, router]);

  const handleOverride = useCallback(
    async (sessionItemId: number): Promise<LearnAnswer | null> => {
      if (!token || !session) return null;
      setError(null);
      try {
        const overrideResult = await overrideLearnAnswer(token, session.id, sessionItemId);
        setProgress(overrideResult.progress);

        if (overrideResult.progress.remainingTerms === 0) {
          clearStoredLearnState(deckId);
          try {
            const sessionResult = await getLearnSessionResult(token, session.id);
            setResult(sessionResult);
          } catch (err: unknown) {
            setError(err instanceof Error ? err.message : "Failed to load results");
          }
        } else {
          writeStoredLearnState(deckId, {
            session,
            question: null,
            questionTurn,
            progress: overrideResult.progress,
            pendingNext: true,
            showWrittenHint,
          });
        }

        return overrideResult;
      } catch (err: unknown) {
        if (isLearnSessionMissingError(err)) {
          clearStoredLearnState(deckId);
          setSession(null);
          setQuestion(null);
          setProgress(null);
          setQuestionTurn(0);
          setResult(null);
        } else {
          setError(err instanceof Error ? err.message : "Failed to override answer");
        }
        return null;
      }
    },
    [token, session, deckId, questionTurn, showWrittenHint]
  );

  const handleQuality = useCallback(
    async (sessionItemId: number, quality: ReviewQuality) => {
      if (!token || !session) return;
      try {
        await adjustLearnQuality(token, session.id, sessionItemId, quality);
      } catch {
        // Non-fatal: scheduling adjustment is a best-effort tweak
      }
    },
    [token, session]
  );

  const segments = useMemo(() => {
    if (!progress) return [];
    const total = progress.totalTerms;
    if (total === 0) return [];

    const maxSegments = 8;
    const segmentCount = Math.min(total, maxSegments);
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
            onClick={handleResetLearn}
            disabled={!deck || isLoading}
            aria-label="Reset learn"
            title="Reset Learn"
          >
            <RotateCcw size={18} />
          </button>
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
            token={token}
            question={question}
            onSubmit={handleSubmitAnswer}
            onNext={handleNext}
            onOverride={handleOverride}
            onQuality={handleQuality}
            isLoading={isLoading}
            showWrittenHint={showWrittenHint}
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

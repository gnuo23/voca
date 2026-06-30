"use client";

import { useState } from "react";
import { BookOpenCheck, Check, ChevronRight, RotateCcw, Send, X } from "lucide-react";
import {
  getLearnSessionResult,
  getNextLearnQuestion,
  LearnAnswerDirection,
  LearnAnswer,
  LearnGoal,
  LearnGradingMode,
  LearnQuestionType,
  LearnQuestion,
  LearnSession,
  LearnSessionResult,
  LearnSessionScope,
  startLearnSession,
  submitLearnAnswer
} from "@/lib/api";

type LearnPanelProps = {
  token: string;
  deckId: string;
  totalWords: number;
  refreshDeck: () => void;
};

export function LearnPanel({ token, deckId, totalWords, refreshDeck }: LearnPanelProps) {
  const [scope, setScope] = useState<LearnSessionScope>("NOT_MASTERED");
  const [goal, setGoal] = useState<LearnGoal>("MASTER_ALL");
  const [answerDirection, setAnswerDirection] = useState<LearnAnswerDirection>("BOTH");
  const [gradingMode, setGradingMode] = useState<LearnGradingMode>("ACCENT_INSENSITIVE");
  const [questionTypes, setQuestionTypes] = useState<Record<LearnQuestionType, boolean>>({
    MCQ: true,
    TRUE_FALSE: false,
    WRITTEN: true
  });
  const [session, setSession] = useState<LearnSession | null>(null);
  const [question, setQuestion] = useState<LearnQuestion | null>(null);
  const [answer, setAnswer] = useState("");
  const [feedback, setFeedback] = useState<LearnAnswer | null>(null);
  const [result, setResult] = useState<LearnSessionResult | null>(null);
  const [startedAt, setStartedAt] = useState(Date.now());
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState("");

  async function start() {
    setIsLoading(true);
    setError("");
    setFeedback(null);
    setResult(null);
    setAnswer("");

    try {
      const enabledTypes = (Object.entries(questionTypes) as Array<[LearnQuestionType, boolean]>)
        .filter(([, enabled]) => enabled)
        .map(([type]) => type);
      const nextSession = await startLearnSession(token, deckId, {
        scope,
        goal,
        answerDirection,
        gradingMode,
        questionTypes: enabledTypes.length ? enabledTypes : ["MCQ"]
      });
      setSession(nextSession);
      const nextQuestion = await getNextLearnQuestion(token, nextSession.id);
      setQuestion(nextQuestion.sessionItemId ? nextQuestion : null);
      setStartedAt(Date.now());
      if (!nextQuestion.sessionItemId) {
        setResult(await getLearnSessionResult(token, nextSession.id));
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not start learn session");
    } finally {
      setIsLoading(false);
    }
  }

  async function submit(selectedAnswer?: string) {
    if (!session || !question?.sessionItemId || !question.questionType) {
      return;
    }
    const finalAnswer = (selectedAnswer ?? answer).trim();
    if (!finalAnswer) {
      setError("Choose or enter an answer first");
      return;
    }

    setIsLoading(true);
    setError("");
    try {
      const response = await submitLearnAnswer(
        token,
        session.id,
        question.sessionItemId,
        finalAnswer,
        question.questionType,
        Date.now() - startedAt,
        question.questionToken
      );
      setFeedback(response);
      setAnswer(finalAnswer);
      await refreshDeck();
      if (response.progress.remainingTerms === 0) {
        setResult(await getLearnSessionResult(token, session.id));
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not submit answer");
    } finally {
      setIsLoading(false);
    }
  }

  async function next() {
    if (!session) {
      return;
    }
    setIsLoading(true);
    setError("");
    setFeedback(null);
    setAnswer("");
    try {
      const nextQuestion = await getNextLearnQuestion(token, session.id);
      setQuestion(nextQuestion.sessionItemId ? nextQuestion : null);
      setStartedAt(Date.now());
      if (!nextQuestion.sessionItemId) {
        setResult(await getLearnSessionResult(token, session.id));
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not load next question");
    } finally {
      setIsLoading(false);
    }
  }

  const progress = feedback?.progress ?? question?.progress;
  const completed = Boolean(result);
  const enabledQuestionCount = Object.values(questionTypes).filter(Boolean).length;

  return (
    <section className="card learn-card">
      <div className="section-heading">
        <div>
          <h2>Learn</h2>
          <p>{progress ? `${progress.masteredTerms}/${progress.totalTerms} complete` : "Quizlet-style adaptive drilling"}</p>
        </div>
        <div className="button-row">
          <button className="button" type="button" onClick={start} disabled={isLoading || totalWords < 2}>
            {session ? <RotateCcw size={18} aria-hidden="true" /> : <BookOpenCheck size={18} aria-hidden="true" />}
            {session ? "New Learn" : "Start Learn"}
          </button>
        </div>
      </div>

      {error && <p className="form-error">{error}</p>}
      {totalWords < 2 && <p className="form-muted">Need at least 2 vocabulary items with meanings.</p>}

      {!question?.sessionItemId && !result && (
        <div className="learn-setup-grid">
          <label>
            <span>Scope</span>
            <select className="compact-select" value={scope} onChange={(event) => setScope(event.target.value as LearnSessionScope)}>
              <option value="NOT_MASTERED">Not mastered</option>
              <option value="ALL">All terms</option>
              <option value="DIFFICULT_ONLY">Difficult only</option>
              <option value="NEW_ONLY">New only</option>
            </select>
          </label>
          <label>
            <span>Goal</span>
            <select className="compact-select" value={goal} onChange={(event) => setGoal(event.target.value as LearnGoal)}>
              <option value="MASTER_ALL">Master all</option>
              <option value="LEARN_ALL">Learn all</option>
              <option value="QUICK_REVIEW">Quick review</option>
            </select>
          </label>
          <label>
            <span>Direction</span>
            <select className="compact-select" value={answerDirection} onChange={(event) => setAnswerDirection(event.target.value as LearnAnswerDirection)}>
              <option value="BOTH">Both ways</option>
              <option value="WORD_TO_MEANING">Word to meaning</option>
              <option value="MEANING_TO_WORD">Meaning to word</option>
            </select>
          </label>
          <label>
            <span>Grading</span>
            <select className="compact-select" value={gradingMode} onChange={(event) => setGradingMode(event.target.value as LearnGradingMode)}>
              <option value="ACCENT_INSENSITIVE">Ignore accents</option>
              <option value="FUZZY">Fuzzy</option>
              <option value="EXACT">Exact</option>
            </select>
          </label>
          <div className="learn-type-group">
            <span>Question types</span>
            <div className="learn-toggle-row">
              {(["MCQ", "TRUE_FALSE", "WRITTEN"] as LearnQuestionType[]).map((type) => (
                <button
                  key={type}
                  className={`status-pill ${questionTypes[type] ? "ok" : "neutral"}`}
                  type="button"
                  onClick={() => {
                    setQuestionTypes((current) => {
                      if (current[type] && enabledQuestionCount === 1) {
                        return current;
                      }
                      return { ...current, [type]: !current[type] };
                    });
                  }}
                >
                  {learnQuestionLabel(type)}
                </button>
              ))}
            </div>
          </div>
        </div>
      )}

      {progress && (
        <div className="learn-stage-row" aria-label="Learn progress">
          <span className="status-pill neutral">Step 1: {progress.newTerms + progress.seenTerms}</span>
          <span className="status-pill neutral">Step 2: {progress.learningTerms}</span>
          <span className="status-pill neutral">Step 3: {progress.familiarTerms}</span>
          <span className="status-pill ok">Complete: {progress.masteredTerms}</span>
        </div>
      )}

      {question?.sessionItemId && (
        <div className="learn-runner">
          <div className="quiz-progress-row">
            <span>{stageLabel(question.stage)}</span>
            <span className="status-pill neutral">{learnQuestionLabel(question.questionType)}</span>
          </div>

          <h3>{question.trueFalseStatement ?? question.prompt}</h3>

          {question.questionType === "MCQ" || question.questionType === "TRUE_FALSE" ? (
            <div className="quiz-options">
              {(question.options ?? []).map((option) => (
                <button
                  key={option}
                  className={`quiz-option ${answer === option ? "selected" : ""}`}
                  type="button"
                  onClick={() => {
                    setAnswer(option);
                    void submit(option);
                  }}
                  disabled={Boolean(feedback) || isLoading}
                >
                  {option}
                </button>
              ))}
            </div>
          ) : (
            <div className="field">
              <label htmlFor={`learn-answer-${question.sessionItemId}`}>Answer</label>
              <input
                id={`learn-answer-${question.sessionItemId}`}
                value={answer}
                onChange={(event) => setAnswer(event.target.value)}
                disabled={Boolean(feedback)}
                onKeyDown={(event) => {
                  if (event.key === "Enter") {
                    void submit();
                  }
                }}
              />
            </div>
          )}

          {feedback && (
            <div className={`quiz-feedback ${feedback.correct ? "correct" : "incorrect"}`}>
              <strong>
                {feedback.correct ? <Check size={18} aria-hidden="true" /> : <X size={18} aria-hidden="true" />}
                {feedback.correct ? "Correct" : "Incorrect"}
              </strong>
              {!feedback.correct && <p>Correct answer: {feedback.correctAnswer}</p>}
              <p>{stageLabel(feedback.newStage)} · streak {feedback.correctStreak}</p>
            </div>
          )}

          <div className="button-row">
            {question.questionType === "WRITTEN" && !feedback && (
              <button className="button" type="button" onClick={() => submit()} disabled={isLoading}>
                <Send size={18} aria-hidden="true" />
                Submit
              </button>
            )}
            {feedback && !completed && (
              <button className="button" type="button" onClick={next} disabled={isLoading}>
                <ChevronRight size={18} aria-hidden="true" />
                Next
              </button>
            )}
          </div>
        </div>
      )}

      {result && (
        <div className="learn-result">
          <div>
            <span className="result-score">{result.session.totalAnswers ? Math.round((result.session.correctAnswers * 100) / result.session.totalAnswers) : 0}%</span>
            <p>{result.session.masteredTerms}/{result.session.totalTerms} mastered</p>
          </div>
          <div className="quiz-answer-list">
            {result.items.map((item) => (
              <div key={item.vocabId} className="quiz-answer-row">
                <span className={`status-pill ${item.stage === "MASTERED" ? "ok" : "neutral"}`}>{stageLabel(item.stage)}</span>
                <p>{item.word} · {item.correctAttempts} correct · {item.incorrectAttempts} missed</p>
              </div>
            ))}
          </div>
        </div>
      )}
    </section>
  );
}

function learnQuestionLabel(type: LearnQuestion["questionType"]) {
  if (type === "MCQ") return "Choose meaning";
  if (type === "TRUE_FALSE") return "True / False";
  return "Written";
}

function stageLabel(stage: LearnQuestion["stage"]) {
  if (!stage || stage === "NOT_STUDIED" || stage === "NEW") return "Step 1";
  if (stage === "STILL_LEARNING" || stage === "LEARNING") return "Step 2";
  if (stage === "SEEN") return "Step 1";
  if (stage === "FAMILIAR") return "Step 3";
  if (stage === "MASTERED") return "Done";
  const s = stage as string;
  return s.replaceAll("_", " ").toLowerCase().replace(/^\w/, (value) => value.toUpperCase());
}

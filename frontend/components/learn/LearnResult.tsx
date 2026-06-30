"use client";

import { RotateCcw, ArrowLeft, Check, AlertTriangle, X } from "lucide-react";
import { LearnSessionResult, LearnItemStage, LearnVerdict } from "@/lib/api";

type LearnResultProps = {
  result: LearnSessionResult;
  onRestart: () => void;
  onBack: () => void;
};

function formatDuration(ms: number): string {
  if (ms >= 60000) {
    const minutes = Math.floor(ms / 60000);
    const seconds = Math.round((ms % 60000) / 1000);
    return `${minutes}m ${seconds}s`;
  }
  return `${Math.round(ms / 1000)}s`;
}

function verdictLabel(verdict: LearnVerdict): string {
  if (verdict === "CORRECT") return "Correct";
  if (verdict === "CLOSE") return "Close";
  return "Incorrect";
}

export function LearnResult({ result, onRestart, onBack }: LearnResultProps) {
  const { session, items, history } = result;
  const score =
    session.totalAnswers > 0
      ? Math.round((session.correctAnswers / session.totalAnswers) * 100)
      : 0;

  const masteredItems = items.filter((i) => i.stage === "MASTERED");
  const learningItems = items.filter((i) =>
    (["LEARNING", "FAMILIAR", "SEEN"] as LearnItemStage[]).includes(i.stage)
  );
  const reviewItems = items.filter((i) =>
    (["NEW", "NOT_STUDIED", "STILL_LEARNING"] as LearnItemStage[]).includes(i.stage)
  );

  return (
    <div className="learn-result-screen">
      <div className="learn-result-title">
        <span style={{ fontSize: "3rem" }}>🎉</span>
        <h1>Well done!</h1>
      </div>

      <div className="learn-result-score">{score}%</div>

      <div className="learn-result-stats">
        <div className="learn-result-stat">
          <span className="learn-result-stat-value">{session.totalAnswers}</span>
          <span className="learn-result-stat-label">Total Questions</span>
        </div>
        <div className="learn-result-stat">
          <span className="learn-result-stat-value">{session.correctAnswers}</span>
          <span className="learn-result-stat-label">Correct</span>
        </div>
        <div className="learn-result-stat">
          <span className="learn-result-stat-value">{formatDuration(session.durationMs)}</span>
          <span className="learn-result-stat-label">Time</span>
        </div>
      </div>

      <div className="learn-result-groups">
        {masteredItems.length > 0 && (
          <div className="learn-result-group">
            <div className="learn-result-group-header mastered">
              <Check size={16} /> Mastered ({masteredItems.length})
            </div>
            {masteredItems.map((item) => (
              <div key={item.vocabId} className="learn-result-term">
                <span className="learn-result-term-word">{item.word}</span>
                <span className="learn-result-term-meaning">{item.meaningVi}</span>
                <span className="learn-result-term-stats">
                  {item.correctAttempts} correct / {item.incorrectAttempts} incorrect
                </span>
              </div>
            ))}
          </div>
        )}

        {learningItems.length > 0 && (
          <div className="learn-result-group">
            <div className="learn-result-group-header learning">
              <AlertTriangle size={16} /> Still Learning ({learningItems.length})
            </div>
            {learningItems.map((item) => (
              <div key={item.vocabId} className="learn-result-term">
                <span className="learn-result-term-word">{item.word}</span>
                <span className="learn-result-term-meaning">{item.meaningVi}</span>
                <span className="learn-result-term-stats">
                  {item.correctAttempts} correct / {item.incorrectAttempts} incorrect
                </span>
              </div>
            ))}
          </div>
        )}

        {reviewItems.length > 0 && (
          <div className="learn-result-group">
            <div className="learn-result-group-header review">
              <X size={16} /> Needs Review ({reviewItems.length})
            </div>
            {reviewItems.map((item) => (
              <div key={item.vocabId} className="learn-result-term">
                <span className="learn-result-term-word">{item.word}</span>
                <span className="learn-result-term-meaning">{item.meaningVi}</span>
                <span className="learn-result-term-stats">
                  {item.correctAttempts} correct / {item.incorrectAttempts} incorrect
                </span>
              </div>
            ))}
          </div>
        )}
      </div>

      {history.length > 0 && (
        <div className="learn-result-history">
          <div className="learn-result-group-header">Answer history</div>
          {history.map((answer, index) => {
            const verdict = answer.verdict ?? (answer.correct ? "CORRECT" : "INCORRECT");
            const similarity = Math.round((answer.similarityScore ?? 0) * 100);
            return (
              <div key={`${answer.answeredAt}-${index}`} className="learn-result-answer">
                <div className="learn-result-answer-main">
                  <span className="learn-result-answer-prompt">{answer.prompt}</span>
                  <span className="learn-result-answer-detail">
                    {answer.userAnswer || "Don't know"} / {answer.correctAnswer}
                  </span>
                </div>
                <span className={`learn-result-verdict ${verdict.toLowerCase()}`}>
                  {verdictLabel(verdict)}
                  {verdict === "CLOSE" ? ` ${similarity}%` : ""}
                </span>
              </div>
            );
          })}
        </div>
      )}

      <div className="learn-result-actions">
        <button type="button" className="button" onClick={onRestart}>
          <RotateCcw size={16} /> Learn Again
        </button>
        <button type="button" className="button secondary-button" onClick={onBack}>
          <ArrowLeft size={16} /> Back to Deck
        </button>
      </div>
    </div>
  );
}

"use client";

import { useState } from "react";
import { Settings, BookOpenCheck, Lightbulb } from "lucide-react";
import {
  StartLearnOptions,
  LearnSessionScope,
  LearnGoal,
  LearnAnswerDirection,
  LearnGradingMode,
  LearnQuestionType,
} from "@/lib/api";

type LearnSetupProps = {
  deckName: string;
  totalWords: number;
  learnedWords: number;
  onStart: (options: StartLearnOptions, preferences: { showWrittenHint: boolean }) => void;
  isLoading: boolean;
};

export function LearnSetup({
  deckName,
  totalWords,
  learnedWords,
  onStart,
  isLoading,
}: LearnSetupProps) {
  const [showOptions, setShowOptions] = useState(false);
  const [scope, setScope] = useState<LearnSessionScope>("NOT_MASTERED");
  const [goal, setGoal] = useState<LearnGoal>("MASTER_ALL");
  const [direction, setDirection] = useState<LearnAnswerDirection>("BOTH");
  const [grading, setGrading] = useState<LearnGradingMode>("ACCENT_INSENSITIVE");
  const [questionTypes, setQuestionTypes] = useState<LearnQuestionType[]>([
    "MCQ",
    "TRUE_FALSE",
    "WRITTEN",
  ]);
  const [showWrittenHint, setShowWrittenHint] = useState(true);

  const unmastered = totalWords - learnedWords;
  const termsToStudy = unmastered > 0 ? unmastered : totalWords;
  const tooFewTerms = totalWords < 2;

  const toggleQuestionType = (type: LearnQuestionType) => {
    setQuestionTypes((prev) => {
      if (prev.includes(type)) {
        if (prev.length <= 1) return prev;
        return prev.filter((t) => t !== type);
      }
      return [...prev, type];
    });
  };

  const handleStart = () => {
    onStart(
      { scope, goal, answerDirection: direction, gradingMode: grading, questionTypes },
      { showWrittenHint }
    );
  };

  return (
    <div className="learn-setup-screen">
      <h1>{deckName}</h1>
      <p className="learn-setup-subtitle">
        {tooFewTerms
          ? "Not enough terms to start a learn session (minimum 2)"
          : `${termsToStudy} terms to study`}
      </p>

      <button
        className="learn-options-toggle"
        onClick={() => setShowOptions((v) => !v)}
        type="button"
      >
        <Settings size={16} />
        Options
      </button>

      {showOptions && (
        <div className="learn-options-panel">
          <div className="learn-options-grid">
            <label>
              <span>Scope</span>
              <select
                className="compact-select"
                value={scope}
                onChange={(e) => setScope(e.target.value as LearnSessionScope)}
              >
                <option value="NOT_MASTERED">Not mastered</option>
                <option value="ALL">All terms</option>
                <option value="DIFFICULT_ONLY">Difficult only</option>
                <option value="NEW_ONLY">New only</option>
              </select>
            </label>

            <label>
              <span>Goal</span>
              <select
                className="compact-select"
                value={goal}
                onChange={(e) => setGoal(e.target.value as LearnGoal)}
              >
                <option value="MASTER_ALL">Master all</option>
                <option value="LEARN_ALL">Learn all</option>
                <option value="QUICK_REVIEW">Quick review</option>
              </select>
            </label>

            <label>
              <span>Direction</span>
              <select
                className="compact-select"
                value={direction}
                onChange={(e) => setDirection(e.target.value as LearnAnswerDirection)}
              >
                <option value="BOTH">Both ways</option>
                <option value="WORD_TO_MEANING">Word → meaning</option>
                <option value="MEANING_TO_WORD">Meaning → word</option>
              </select>
            </label>

            <label>
              <span>Grading</span>
              <select
                className="compact-select"
                value={grading}
                onChange={(e) => setGrading(e.target.value as LearnGradingMode)}
              >
                <option value="ACCENT_INSENSITIVE">Ignore accents</option>
                <option value="FUZZY">Fuzzy</option>
                <option value="EXACT">Exact</option>
              </select>
            </label>
          </div>

          <div>
            <span>Question types</span>
            <div className="learn-toggle-row">
              {(["MCQ", "TRUE_FALSE", "WRITTEN"] as LearnQuestionType[]).map((type) => (
                <button
                  key={type}
                  type="button"
                  className={`status-pill ${questionTypes.includes(type) ? "ok" : "neutral"}`}
                  onClick={() => toggleQuestionType(type)}
                >
                  {type === "MCQ" ? "MCQ" : type === "TRUE_FALSE" ? "True/False" : "Written"}
                </button>
              ))}
            </div>
          </div>

          <div>
            <span>Written hint</span>
            <div className="learn-toggle-row">
              <button
                type="button"
                className={`status-pill ${showWrittenHint ? "ok" : "neutral"}`}
                onClick={() => setShowWrittenHint((value) => !value)}
              >
                <Lightbulb size={14} />
                {showWrittenHint ? "Hint on" : "Hint off"}
              </button>
            </div>
          </div>
        </div>
      )}

      <button
        className="learn-start-btn button"
        onClick={handleStart}
        disabled={tooFewTerms || isLoading}
        type="button"
      >
        <BookOpenCheck size={20} />
        {isLoading ? "Starting…" : "Start Learning"}
      </button>
    </div>
  );
}

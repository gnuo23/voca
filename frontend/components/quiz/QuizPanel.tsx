"use client";

import { useMemo, useState } from "react";
import { Check, CircleHelp, ListChecks, Send, X } from "lucide-react";
import {
  answerQuizQuestion,
  createQuizAttempt,
  generateQuiz,
  getQuizResult,
  QuizAnswer,
  QuizAttempt,
  QuizQuestion,
  QuizResult
} from "@/lib/api";

type QuizPanelProps = {
  token: string;
  deckId: string;
  totalWords: number;
  refreshDeck: () => void;
};

export function QuizPanel({ token, deckId, totalWords, refreshDeck }: QuizPanelProps) {
  const [attempt, setAttempt] = useState<QuizAttempt | null>(null);
  const [answers, setAnswers] = useState<Record<number, QuizAnswer>>({});
  const [selectedAnswers, setSelectedAnswers] = useState<Record<number, string>>({});
  const [currentIndex, setCurrentIndex] = useState(0);
  const [questionStartedAt, setQuestionStartedAt] = useState(Date.now());
  const [result, setResult] = useState<QuizResult | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState("");

  const currentQuestion = useMemo(() => {
    if (!attempt || attempt.questions.length === 0) {
      return null;
    }
    return attempt.questions[Math.min(currentIndex, attempt.questions.length - 1)];
  }, [attempt, currentIndex]);

  async function startQuiz() {
    setIsLoading(true);
    setError("");
    setResult(null);
    setAnswers({});
      setSelectedAnswers({});
      setCurrentIndex(0);
      setQuestionStartedAt(Date.now());

    try {
      const generated = await generateQuiz(token, deckId);
      const nextAttempt = await createQuizAttempt(
        token,
        deckId,
        generated.questions.map((question) => question.id)
      );
      setAttempt(nextAttempt);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not start quiz");
    } finally {
      setIsLoading(false);
    }
  }

  async function submitAnswer(question: QuizQuestion) {
    const answer = selectedAnswers[question.id]?.trim() ?? "";
    if (!answer) {
      setError("Choose or enter an answer first");
      return;
    }
    if (!attempt) {
      return;
    }

    setIsLoading(true);
    setError("");
    try {
      const response = await answerQuizQuestion(token, attempt.id, question.id, answer, Date.now() - questionStartedAt);
      setAnswers((current) => ({ ...current, [question.id]: response }));
      await refreshDeck();
      if (Object.keys(answers).length + 1 >= attempt.totalQuestions) {
        const nextResult = await getQuizResult(token, attempt.id);
        setResult(nextResult);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not submit answer");
    } finally {
      setIsLoading(false);
    }
  }

  async function loadResult() {
    if (!attempt) {
      return;
    }
    setIsLoading(true);
    setError("");
    try {
      setResult(await getQuizResult(token, attempt.id));
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not load result");
    } finally {
      setIsLoading(false);
    }
  }

  const answeredCount = Object.keys(answers).length;
  const currentAnswer = currentQuestion ? answers[currentQuestion.id] : null;

  return (
    <section className="card quiz-card">
      <div className="section-heading">
        <div>
          <h2>Quiz</h2>
          <p>{attempt ? `${answeredCount}/${attempt.totalQuestions} answered` : "10 rule-based questions"}</p>
        </div>
        <button className="button" type="button" onClick={startQuiz} disabled={isLoading || totalWords < 2}>
          <ListChecks size={18} aria-hidden="true" />
          {attempt ? "New Quiz" : "Start Quiz"}
        </button>
      </div>

      {error && <p className="form-error">{error}</p>}
      {totalWords < 2 && <p className="form-muted">Need at least 2 vocabulary items with meanings.</p>}

      {currentQuestion && (
        <div className="quiz-runner">
          <div className="quiz-progress-row">
            <span>Question {currentIndex + 1} / {attempt?.totalQuestions}</span>
            <span className="status-pill neutral">{questionLabel(currentQuestion.type)}</span>
          </div>

          <h3>{currentQuestion.prompt}</h3>

          {currentQuestion.type === "CHOOSE_MEANING" ? (
            <div className="quiz-options">
              {currentQuestion.options.map((option) => (
                <button
                  key={option}
                  className={`quiz-option ${selectedAnswers[currentQuestion.id] === option ? "selected" : ""}`}
                  type="button"
                  onClick={() => setSelectedAnswers((current) => ({ ...current, [currentQuestion.id]: option }))}
                  disabled={Boolean(currentAnswer)}
                >
                  {option}
                </button>
              ))}
            </div>
          ) : (
            <div className="field">
              <label htmlFor={`quiz-answer-${currentQuestion.id}`}>Answer</label>
              <input
                id={`quiz-answer-${currentQuestion.id}`}
                value={selectedAnswers[currentQuestion.id] ?? ""}
                onChange={(event) =>
                  setSelectedAnswers((current) => ({ ...current, [currentQuestion.id]: event.target.value }))
                }
                disabled={Boolean(currentAnswer)}
              />
            </div>
          )}

          {currentAnswer && (
            <div className={`quiz-feedback ${currentAnswer.correct ? "correct" : "incorrect"}`}>
              <strong>
                {currentAnswer.correct ? <Check size={18} aria-hidden="true" /> : <X size={18} aria-hidden="true" />}
                {currentAnswer.correct ? "Correct" : "Incorrect"}
              </strong>
              <p>{currentAnswer.explanation}</p>
              {!currentAnswer.correct && <p>Correct answer: {currentAnswer.correctAnswer}</p>}
            </div>
          )}

          <div className="button-row">
            {!currentAnswer ? (
              <button className="button" type="button" onClick={() => submitAnswer(currentQuestion)} disabled={isLoading}>
                <Send size={18} aria-hidden="true" />
                Submit
              </button>
            ) : (
              <button
                className="button"
                type="button"
                onClick={() => {
                  setCurrentIndex((index) => Math.min(index + 1, (attempt?.totalQuestions ?? 1) - 1));
                  setQuestionStartedAt(Date.now());
                }}
                disabled={currentIndex + 1 >= (attempt?.totalQuestions ?? 0)}
              >
                Next
              </button>
            )}
            {attempt && answeredCount > 0 && (
              <button className="button secondary-button" type="button" onClick={loadResult} disabled={isLoading}>
                <CircleHelp size={18} aria-hidden="true" />
                Result
              </button>
            )}
          </div>
        </div>
      )}

      {result && (
        <div className="quiz-result">
          <div>
            <span className="result-score">{result.scorePercent}%</span>
            <p>{result.correctCount}/{result.totalQuestions} correct</p>
          </div>
          <div className="quiz-answer-list">
            {result.answers.map((answer) => (
              <div key={answer.questionId} className="quiz-answer-row">
                <span className={`status-pill ${answer.correct ? "ok" : "neutral"}`}>
                  {answer.correct ? "Correct" : "Review"}
                </span>
                <p>{answer.explanation}</p>
              </div>
            ))}
          </div>
        </div>
      )}
    </section>
  );
}

function questionLabel(type: string) {
  return type === "CHOOSE_MEANING" ? "Choose meaning" : "Fill blank";
}

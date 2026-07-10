"use client";

import Image from "next/image";
import { useCallback, useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import {
  ToeicAttempt,
  ToeicGroup,
  ToeicQuestion,
  answerToeicQuestion,
  submitToeicAttempt
} from "@/lib/api";
import { useCountdown } from "@/hooks/useCountdown";
import {
  clearStoredToeicState,
  readStoredToeicState,
  writeStoredToeicState
} from "@/lib/toeicStorage";
import { ToeicAudioPlayer } from "@/components/toeic/ToeicAudioPlayer";

const LABELS = ["A", "B", "C", "D"];

type ToeicRunnerProps = {
  token: string;
  attempt: ToeicAttempt;
  resultHref: (attemptId: number) => string;
};

function partLabel(part: string): string {
  return part.replace("PART_", "Part ");
}

export function ToeicRunner({ token, attempt, resultHref }: ToeicRunnerProps) {
  const router = useRouter();
  const [answers, setAnswers] = useState<Record<number, string>>({});
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const submittedRef = useRef(false);

  useEffect(() => {
    const stored = readStoredToeicState(attempt.id);
    if (stored) setAnswers(stored.answers);
  }, [attempt.id]);

  const persist = useCallback(
    (next: Record<number, string>) => {
      writeStoredToeicState(attempt.id, next);
    },
    [attempt.id]
  );

  const handleSubmit = useCallback(async () => {
    if (submittedRef.current || submitting) return;
    submittedRef.current = true;
    setSubmitting(true);
    setError(null);
    try {
      await submitToeicAttempt(token, attempt.id);
      clearStoredToeicState(attempt.id);
      router.push(resultHref(attempt.id));
    } catch (e) {
      submittedRef.current = false;
      setSubmitting(false);
      setError(e instanceof Error ? e.message : "Nộp bài thất bại");
    }
  }, [attempt.id, resultHref, router, submitting, token]);

  const countdown = useCountdown(attempt.expiresAt, handleSubmit);

  async function selectAnswer(question: ToeicQuestion, label: string) {
    if (answers[question.id]) return;
    const next = { ...answers, [question.id]: label };
    setAnswers(next);
    persist(next);
    try {
      await answerToeicQuestion(token, attempt.id, question.id, label);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Lưu câu trả lời thất bại");
    }
  }

  const answeredCount = Object.keys(answers).length;

  return (
    <div className="toeic-runner">
      <header className="toeic-runner-topbar">
        <div>
          <h1>{attempt.testName}</h1>
          <p>
            {attempt.mode === "PART" && attempt.partFilter
              ? partLabel(attempt.partFilter)
              : "Full test"}{" "}
            · {answeredCount}/{attempt.totalQuestions} câu
          </p>
        </div>
        {attempt.expiresAt ? (
          <div className={`toeic-timer ${countdown.remainingMs < 60000 ? "urgent" : ""}`}>
            {countdown.label}
          </div>
        ) : null}
        <button
          className="button"
          type="button"
          onClick={handleSubmit}
          disabled={submitting}
        >
          {submitting ? "Đang nộp…" : "Nộp bài"}
        </button>
      </header>

      {error ? <div className="form-error">{error}</div> : null}

      <div className="toeic-groups">
        {attempt.groups.map((group) => (
          <ToeicGroupBlock
            key={group.id}
            group={group}
            answers={answers}
            onSelect={selectAnswer}
          />
        ))}
      </div>

      <footer className="toeic-runner-footer">
        <button className="button" type="button" onClick={handleSubmit} disabled={submitting}>
          {submitting ? "Đang nộp…" : `Nộp bài (${answeredCount}/${attempt.totalQuestions})`}
        </button>
      </footer>
    </div>
  );
}

type GroupBlockProps = {
  group: ToeicGroup;
  answers: Record<number, string>;
  onSelect: (question: ToeicQuestion, label: string) => void;
};

function ToeicGroupBlock({ group, answers, onSelect }: GroupBlockProps) {
  const images = group.media.filter((m) => m.fileType === "IMAGE");
  const audios = group.media.filter((m) => m.fileType === "AUDIO");
  const hasPassage = Boolean(group.passageText);

  return (
    <section className="toeic-group card">
      <div className="toeic-group-part">{partLabel(group.questionPart)}</div>

      {audios.map((audio) => (
        <ToeicAudioPlayer key={audio.url} src={audio.url} label="Audio" />
      ))}

      {images.map((image) => (
        <div className="toeic-image" key={image.url}>
          <Image
            src={image.url}
            alt="TOEIC question"
            width={640}
            height={420}
            unoptimized
            style={{ width: "100%", height: "auto" }}
          />
        </div>
      ))}

      <div className={hasPassage ? "toeic-group-body two-col" : "toeic-group-body"}>
        {hasPassage ? (
          <div className="toeic-passage" dangerouslySetInnerHTML={{ __html: group.passageText ?? "" }} />
        ) : null}

        <div className="toeic-questions">
          {group.questions.map((question) => (
            <div className="toeic-question" key={question.id}>
              <div className="toeic-question-number">
                Câu {question.questionNumber}
                {question.questionText ? `: ${question.questionText}` : ""}
              </div>
              <div className="toeic-options">
                {question.options.map((option) => {
                  const label = option.label || LABELS[option.answerOrder - 1] || "?";
                  const selected = answers[question.id] === label;
                  return (
                    <button
                      key={label}
                      type="button"
                      className={`toeic-option ${selected ? "selected" : ""}`}
                      onClick={() => onSelect(question, label)}
                      disabled={Boolean(answers[question.id])}
                    >
                      <span className="toeic-option-label">{label}</span>
                      {option.content ? (
                        <span className="toeic-option-content">{option.content}</span>
                      ) : null}
                    </button>
                  );
                })}
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}

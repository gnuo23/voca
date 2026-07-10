"use client";

import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import Link from "next/link";
import { ArrowLeft } from "lucide-react";
import { AppShell } from "@/components/AppShell";
import { ExplainChatPanel } from "@/components/toeic/ExplainChatPanel";
import { ToeicResult, getStoredToken, getToeicResult } from "@/lib/api";

function partLabel(part: string): string {
  return part.replace("PART_", "Part ");
}

export default function ToeicResultPage() {
  const router = useRouter();
  const params = useParams<{ slug: string; attemptId: string }>();
  const slug = params.slug;
  const attemptId = Number(params.attemptId);
  const [result, setResult] = useState<ToeicResult | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const storedToken = getStoredToken();
    if (!storedToken) {
      router.push("/login");
      return;
    }
    getToeicResult(storedToken, attemptId)
      .then(setResult)
      .catch(() => setError("Không tải được kết quả"))
      .finally(() => setIsLoading(false));
  }, [attemptId, router]);

  return (
    <AppShell>
      <header className="topbar">
        <div>
          <Link className="back-link" href={`/toeic/${slug}`}>
            <ArrowLeft size={16} aria-hidden="true" /> Đề thi
          </Link>
          <h1>Kết quả</h1>
        </div>
      </header>

      {error ? <div className="form-error">{error}</div> : null}
      {isLoading ? <p>Đang tải…</p> : null}

      {result ? (
        <>
          <section className="card toeic-result-score">
            {result.scaledScore != null ? (
              <div className="toeic-scaled">
                <span className="toeic-scaled-value">{result.scaledScore}</span>
                <span className="toeic-scaled-max">/ 990</span>
              </div>
            ) : null}
            <div className="toeic-score-detail">
              <span>
                Đúng {result.correctCount}/{result.totalQuestions}
              </span>
              {result.listeningScore != null ? (
                <span>Listening {result.listeningScore}</span>
              ) : null}
              {result.readingScore != null ? (
                <span>Reading {result.readingScore}</span>
              ) : null}
            </div>
          </section>

          <section className="card toeic-breakdown">
            <h2>Kết quả theo part</h2>
            {result.partBreakdown.map((part) => (
              <div className="toeic-breakdown-row" key={part.part}>
                <span>{partLabel(part.part)}</span>
                <div className="toeic-breakdown-bar">
                  <div
                    className="toeic-breakdown-fill"
                    style={{ width: `${part.accuracyPercent}%` }}
                  />
                </div>
                <span>
                  {part.correct}/{part.total} ({part.accuracyPercent}%)
                </span>
              </div>
            ))}
          </section>

          <section className="toeic-review">
            <h2>Chi tiết đáp án</h2>
            {result.answers.map((answer) => (
              <div
                className={`card toeic-review-item ${answer.correct ? "correct" : "wrong"}`}
                key={answer.questionId}
              >
                <div className="toeic-review-head">
                  <span>
                    Câu {answer.questionNumber} · {partLabel(answer.questionPart)}
                  </span>
                  <span className={`status-pill ${answer.correct ? "ok" : "bad"}`}>
                    {answer.answered
                      ? answer.correct
                        ? "Đúng"
                        : "Sai"
                      : "Chưa trả lời"}
                  </span>
                </div>
                {answer.questionText ? <p>{answer.questionText}</p> : null}
                <div className="toeic-review-labels">
                  <span>Bạn chọn: {answer.selectedLabel ?? "—"}</span>
                  <span>Đáp án đúng: {answer.correctAnswerLabel}</span>
                </div>
                {answer.audioTranscript ? (
                  <details className="toeic-explanation">
                    <summary>Transcript</summary>
                    <p>{answer.audioTranscript}</p>
                  </details>
                ) : null}
                {answer.explanationHtml ? (
                  <details className="toeic-explanation">
                    <summary>Giải thích</summary>
                    <div
                      dangerouslySetInnerHTML={{ __html: answer.explanationHtml }}
                    />
                  </details>
                ) : null}
                <ExplainChatPanel questionId={answer.questionId} />
              </div>
            ))}
          </section>
        </>
      ) : null}
    </AppShell>
  );
}

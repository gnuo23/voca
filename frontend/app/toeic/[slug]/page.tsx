"use client";

import { useCallback, useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import Link from "next/link";
import { ArrowLeft, Clock, ListChecks } from "lucide-react";
import { AppShell } from "@/components/AppShell";
import {
  ToeicTestSummary,
  getStoredToken,
  getToeicTest,
  startToeicAttempt
} from "@/lib/api";

const PART_ORDER = [
  "PART_1",
  "PART_2",
  "PART_3",
  "PART_4",
  "PART_5",
  "PART_6",
  "PART_7"
];

function partLabel(part: string): string {
  return part.replace("PART_", "Part ");
}

export default function ToeicTestOverviewPage() {
  const router = useRouter();
  const params = useParams<{ slug: string }>();
  const slug = params.slug;
  const [token, setToken] = useState("");
  const [test, setTest] = useState<ToeicTestSummary | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [starting, setStarting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const storedToken = getStoredToken();
    if (!storedToken) {
      router.push("/login");
      return;
    }
    setToken(storedToken);
    getToeicTest(storedToken, slug)
      .then(setTest)
      .catch(() => setError("Không tải được đề thi"))
      .finally(() => setIsLoading(false));
  }, [router, slug]);

  const start = useCallback(
    async (mode: string, partFilter?: string) => {
      if (!token || starting) return;
      setStarting(true);
      setError(null);
      try {
        const attempt = await startToeicAttempt(token, slug, { mode, partFilter });
        router.push(`/toeic/${slug}/attempt/${attempt.id}`);
      } catch (e) {
        setError(e instanceof Error ? e.message : "Không bắt đầu được bài thi");
        setStarting(false);
      }
    },
    [router, slug, starting, token]
  );

  return (
    <AppShell>
      <header className="topbar">
        <div>
          <Link className="back-link" href="/toeic">
            <ArrowLeft size={16} aria-hidden="true" /> Danh sách đề
          </Link>
          <h1>{test?.testName ?? (isLoading ? "Đang tải…" : "Đề thi")}</h1>
          {test ? (
            <p>
              {test.collectionName} · {test.totalQuestions} câu · {test.durationMinutes} phút
            </p>
          ) : null}
        </div>
      </header>

      {error ? <div className="form-error">{error}</div> : null}

      {test ? (
        <>
          <section className="card toeic-start-card">
            <h2>
              <ListChecks size={18} aria-hidden="true" /> Làm full test
            </h2>
            <p>200 câu, chấm điểm quy đổi TOEIC (990). Có đếm giờ {test.durationMinutes} phút.</p>
            <button
              className="button"
              type="button"
              onClick={() => start("FULL")}
              disabled={starting}
            >
              <Clock size={16} aria-hidden="true" />
              {starting ? "Đang tạo…" : "Bắt đầu full test"}
            </button>
          </section>

          <section className="toeic-part-grid" aria-label="Luyện theo part">
            {PART_ORDER.map((part) => {
              const count = test.partQuestionCount[part] ?? 0;
              if (count === 0) return null;
              return (
                <button
                  key={part}
                  type="button"
                  className="card toeic-part-card"
                  onClick={() => start("PART", part)}
                  disabled={starting}
                >
                  <span className="toeic-part-name">{partLabel(part)}</span>
                  <span className="toeic-part-count">{count} câu</span>
                </button>
              );
            })}
          </section>
        </>
      ) : null}
    </AppShell>
  );
}

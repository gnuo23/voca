"use client";

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import { ArrowRight, BarChart3, BookOpen, Check, Clock3, Flame, Headphones, ListChecks, MessageCircle, Target, Trophy } from "lucide-react";
import { AppShell } from "@/components/AppShell";
import {
  getStoredToken,
  getToeicDashboard,
  listToeicTests,
  type ToeicDashboard,
  type ToeicTestSummary
} from "@/lib/api";
import { useRouter } from "next/navigation";

const PARTS = ["PART_1", "PART_2", "PART_3", "PART_4", "PART_5", "PART_6", "PART_7"];

function partLabel(part: string) {
  return part.replace("PART_", "Part ");
}

function accuracyClass(value: number) {
  if (value >= 80) return "good";
  if (value >= 60) return "medium";
  return "low";
}

export default function ToeicDashboardPage() {
  const router = useRouter();
  const [tests, setTests] = useState<ToeicTestSummary[]>([]);
  const [dashboard, setDashboard] = useState<ToeicDashboard | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const token = getStoredToken();
    if (!token) {
      router.push("/login");
      return;
    }
    Promise.all([listToeicTests(token), getToeicDashboard(token)])
      .then(([availableTests, metrics]) => {
        setTests(availableTests);
        setDashboard(metrics);
      })
      .catch((cause) => setError(cause instanceof Error ? cause.message : "Không tải được dashboard TOEIC"))
      .finally(() => setLoading(false));
  }, [router]);

  const featured = useMemo(() => tests.slice(0, 3), [tests]);
  const heroTest = tests[0];
  const progressByPart = new Map((dashboard?.partProgress ?? []).map((part) => [part.part, part]));
  const dailyQuestions = Math.min(10, dashboard?.answeredToday ?? 0);
  const dailyMiniTests = Math.min(1, dashboard?.completedToday ?? 0);

  return (
    <AppShell>
      <header className="topbar">
        <div>
          <p className="eyebrow">TOEIC MASTER</p>
          <h1>Luyện thi TOEIC hiệu quả mỗi ngày</h1>
          <p>Theo dõi tiến độ, luyện từng Part và hỏi AI khi bạn cần hiểu sâu hơn.</p>
        </div>
        <Link className="button" href={heroTest ? `/toeic/${heroTest.slug}` : "/toeic"}>
          Bắt đầu luyện <ArrowRight size={16} aria-hidden="true" />
        </Link>
      </header>

      {error ? <div className="form-error">{error}</div> : null}
      {loading ? <p>Đang tải dashboard…</p> : null}

      <section className="toeic-dashboard-hero">
        <div>
          <span className="toeic-hero-kicker"><Headphones size={17} /> Lộ trình luyện thi</span>
          <h2>{heroTest ? `Ôn TOEIC – ${heroTest.testName}` : "Ôn TOEIC theo mục tiêu của bạn"}</h2>
          <p>{heroTest ? `${heroTest.totalQuestions} câu hỏi chuẩn đề, luyện nghe và đọc trong một phiên.` : "Dữ liệu đề thi sẽ xuất hiện sau khi quản trị viên nạp đề."}</p>
          {heroTest ? <Link className="button toeic-hero-button" href={`/toeic/${heroTest.slug}`}>Bắt đầu luyện ngay <ArrowRight size={17} /></Link> : null}
        </div>
        <div className="toeic-hero-visual" aria-hidden="true"><Headphones size={112} strokeWidth={1.2} /></div>
      </section>

      <div className="toeic-dashboard-grid">
        <section className="card toeic-goals-card">
          <div className="toeic-card-heading"><Target size={21} /><h2>Mục tiêu hôm nay</h2></div>
          <div className="toeic-goal-row"><Headphones className="blue" /><span>10 câu luyện nghe</span><strong>{dailyQuestions}/10</strong><div className="toeic-goal-check">{dailyQuestions >= 10 ? <Check size={15} /> : ""}</div></div>
          <div className="toeic-goal-row"><ListChecks className="green" /><span>1 bài test mini</span><strong>{dailyMiniTests}/1</strong><div className="toeic-goal-check">{dailyMiniTests ? <Check size={15} /> : ""}</div></div>
          <div className="toeic-goal-row"><MessageCircle className="orange" /><span>Hỏi AI giải thích</span><strong>Tuỳ chọn</strong><div className="toeic-goal-check">✦</div></div>
        </section>

        <section className="card toeic-progress-card">
          <div className="toeic-card-heading"><BarChart3 size={21} /><h2>Tiến độ học tập</h2></div>
          <div className="toeic-metric-row"><div className="toeic-metric-icon blue"><Target size={19} /></div><span>Độ chính xác</span><strong>{dashboard?.accuracyPercent ?? 0}%</strong><div className="toeic-progress-track"><i style={{ width: `${dashboard?.accuracyPercent ?? 0}%` }} /></div></div>
          <div className="toeic-metric-row"><div className="toeic-metric-icon orange"><Flame size={19} /></div><span>Chuỗi học</span><strong>{dashboard?.streakDays ?? 0} ngày</strong><em>{dashboard?.streakDays ? "Cố lên! 💪" : "Bắt đầu hôm nay"}</em></div>
          <div className="toeic-metric-row"><div className="toeic-metric-icon green"><BookOpen size={19} /></div><span>Đã hoàn thành</span><strong>{dashboard?.completedAttempts ?? 0} bài</strong><em>{dashboard?.totalAnswered ?? 0} câu đã làm</em></div>
        </section>
      </div>

      <section className="card toeic-sample-card">
        <div className="toeic-card-heading"><BookOpen size={21} /><h2>Bài tập mẫu – Part 5</h2><span className="toeic-sample-badge">Grammar</span></div>
        <p className="toeic-sample-prompt">The marketing team will present the new campaign _____ the quarterly meeting.</p>
        <div className="toeic-sample-options"><span><b>A</b> at</span><span><b>B</b> on</span><span><b>C</b> in</span><span><b>D</b> by</span></div>
        <p className="toeic-sample-hint">Mẹo: Sau “at” dùng một thời điểm/sự kiện cụ thể. Làm đề thật để xem đáp án và giải thích chi tiết.</p>
      </section>

      <section className="toeic-dashboard-grid toeic-lower-grid">
        <section className="card toeic-part-progress-card">
          <div className="toeic-card-heading"><Trophy size={21} /><h2>Tiến độ theo Part</h2></div>
          <div className="toeic-part-progress-list">{PARTS.map((part) => { const item = progressByPart.get(part); const accuracy = item?.accuracyPercent ?? 0; return <div className="toeic-part-progress" key={part}><span>{partLabel(part)}</span><div className="toeic-progress-track"><i className={accuracyClass(accuracy)} style={{ width: `${accuracy}%` }} /></div><strong>{item ? `${item.correct}/${item.answered}` : "—"}</strong></div>; })}</div>
        </section>
        <section className="card toeic-recent-card">
          <div className="toeic-card-heading"><Clock3 size={21} /><h2>Bài làm gần đây</h2></div>
          {dashboard?.recentAttempts.length ? dashboard.recentAttempts.map((attempt) => <Link className="toeic-recent-row" key={attempt.attemptId} href={`/toeic/${attempt.testSlug}/result/${attempt.attemptId}`}><span><strong>{attempt.testName}</strong><small>{attempt.mode === "PART" ? partLabel(attempt.partFilter ?? "") : "Full test"} · {attempt.correctCount}/{attempt.totalQuestions}</small></span><b>{attempt.scaledScore ?? `${Math.round(attempt.correctCount * 100 / Math.max(1, attempt.totalQuestions))}%`}</b></Link>) : <p className="muted">Bạn chưa có bài làm nào. Chọn một đề để bắt đầu.</p>}
        </section>
      </section>

      <section className="toeic-featured-section"><div className="toeic-section-title"><h2>Đề luyện nổi bật</h2><Link href="/toeic">Xem tất cả <ArrowRight size={15} /></Link></div><div className="toeic-featured-grid">{featured.map((test, index) => <Link className={`toeic-featured-card tone-${index}`} key={test.id} href={`/toeic/${test.slug}`}><div className="toeic-featured-icon"><ListChecks size={25} /></div><div><strong>{test.testName}</strong><span>{test.totalQuestions} câu hỏi</span><small>{test.collectionName ?? "Luyện TOEIC"} · {test.durationMinutes} phút</small></div><ArrowRight size={18} /></Link>)}</div></section>
    </AppShell>
  );
}

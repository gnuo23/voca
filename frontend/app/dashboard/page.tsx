"use client";

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import {
  Activity,
  BookOpen,
  CalendarClock,
  CheckCircle2,
  Flame,
  Gauge,
  Layers,
  ShieldCheck,
  Star,
  Target,
  Zap
} from "lucide-react";
import { AppShell } from "@/components/AppShell";
import { DashboardMetrics, getCurrentUser, getDashboardMetrics, getHealth, getStoredToken, UserProfile } from "@/lib/api";

function getGreeting(): string {
  const hour = new Date().getHours();
  if (hour < 12) return "Good morning";
  if (hour < 17) return "Good afternoon";
  return "Good evening";
}

function formatScore(value: number): string {
  return Number.isInteger(value) ? `${value}%` : `${value.toFixed(2)}%`;
}

export default function DashboardPage() {
  const router = useRouter();
  const [health, setHealth] = useState("Checking");
  const [user, setUser] = useState<UserProfile | null>(null);
  const [metrics, setMetrics] = useState<DashboardMetrics | null>(null);

  useEffect(() => {
    const token = getStoredToken();
    if (!token) {
      router.push("/login");
      return;
    }

    getHealth()
      .then((response) => setHealth(response.status))
      .catch(() => setHealth("Offline"));

    getCurrentUser(token)
      .then(setUser)
      .catch(() => router.push("/login"));

    getDashboardMetrics(token)
      .then(setMetrics)
      .catch(() => setMetrics(null));
  }, [router]);

  const activeDeck = useMemo(() => metrics?.deckProgress?.[0] ?? null, [metrics]);
  const dailyGoal = user?.dailyGoal ?? 10;
  const learnedToday = metrics?.wordsLearnedToday ?? 0;
  const missionProgress = Math.min(100, Math.round((learnedToday / Math.max(dailyGoal, 1)) * 100));
  const level = user?.englishLevel.replaceAll("_", " ") ?? "BEGINNER";
  const numericLevel = metrics?.level?.level ?? 1;
  const xpProgress = metrics?.level?.progressPercent ?? 0;
  const statusOk = health.toUpperCase() === "UP";
  const weeklyStats = metrics?.weeklyStats?.length ? metrics.weeklyStats : [];
  const weeklyMax = Math.max(1, ...weeklyStats.map((item) => item.total));

  const metricCards = [
    {
      label: "Words to review",
      value: metrics?.wordsToReview ?? 0,
      icon: Gauge,
      tone: "purple"
    },
    {
      label: "Reviewed today",
      value: metrics?.wordsReviewedToday ?? 0,
      icon: BookOpen,
      tone: "blue"
    },
    {
      label: "Learned today",
      value: learnedToday,
      icon: Zap,
      tone: "yellow"
    },
    {
      label: "Streak days",
      value: metrics?.streakDays ?? 0,
      icon: Target,
      tone: "green"
    },
    {
      label: "Accuracy",
      value: `${metrics?.accuracy ?? 0}%`,
      icon: ShieldCheck,
      tone: "orange"
    },
    {
      label: "Overdue",
      value: metrics?.overdueWords ?? 0,
      icon: CalendarClock,
      tone: "red"
    }
  ] as const;

  return (
    <AppShell>
      <div className="dashboard-page">
        <header className="dashboard-hero">
          <div>
            <h1>{getGreeting()}, {user?.displayName ?? "nganh"} 👋</h1>
            <p>{level} level · Your learning overview</p>
            <div className="dashboard-chips" aria-label="Learning summary">
              <span className="dash-chip green"><Star size={16} aria-hidden="true" />Lv. {numericLevel}</span>
              <span className="dash-chip red"><Flame size={16} aria-hidden="true" />{metrics?.streakDays ?? 0} streak day</span>
              <span className="dash-chip purple"><Target size={16} aria-hidden="true" />{learnedToday} learned today</span>
            </div>
          </div>
          <span className={`dashboard-api ${statusOk ? "ok" : "bad"}`}>
            <Activity size={16} aria-hidden="true" />
            API {health}
          </span>
        </header>

        <section className="dashboard-metric-grid" aria-label="Learning metrics">
          {metricCards.map((item) => (
            <article key={item.label} className={`dashboard-metric-card ${item.tone}`}>
              <div className="dashboard-metric-icon">
                <item.icon size={34} aria-hidden="true" />
              </div>
              <div>
                <strong>{item.value}</strong>
                <p>{item.label}</p>
              </div>
            </article>
          ))}
        </section>

        <section className="dashboard-workspace">
          <div className="dashboard-workspace-main">
            <article className="dashboard-paper-card deck-progress-feature">
              <div className="paper-tape purple" aria-hidden="true" />
              <div>
                <h2>📊 Deck Progress</h2>
                {activeDeck ? (
                  <>
                    <strong className="dashboard-deck-name">{activeDeck.deckName}</strong>
                    <p>{activeDeck.masteredCount} mastered · {activeDeck.reviewCount} review · {activeDeck.learningCount} learning</p>
                    <span className="dashboard-progress-score">{formatScore(activeDeck.progressScore)}</span>
                    <div className="progress-track dashboard-progress-track" aria-hidden="true">
                      <div style={{ width: `${Math.min(activeDeck.progressScore, 100)}%` }} />
                    </div>
                    <Link className="dashboard-continue-btn" href={`/mydeck/${activeDeck.deckId}`}>
                      Continue learning
                      <span aria-hidden="true">→</span>
                    </Link>
                  </>
                ) : (
                  <>
                    <strong className="dashboard-deck-name">No deck yet</strong>
                    <p>Create your first deck to start tracking progress.</p>
                    <Link className="dashboard-continue-btn" href="/decks/new">
                      Create deck
                      <span aria-hidden="true">→</span>
                    </Link>
                  </>
                )}
              </div>
              <div className="book-stack" aria-hidden="true">
                <span />
                <span />
                <span />
                <i />
              </div>
            </article>

            <article className="dashboard-paper-card study-note-card">
              <div className="study-note-icon"><Zap size={24} aria-hidden="true" /></div>
              <div>
                <h2>Level Progress</h2>
                <p>Lv. {numericLevel} · {metrics?.level?.xp ?? 0} XP · {xpProgress}% tới level tiếp theo.</p>
                <div className="progress-track dashboard-progress-track" aria-hidden="true">
                  <div style={{ width: `${xpProgress}%` }} />
                </div>
              </div>
              <span className="pencil-mark" aria-hidden="true">✎</span>
            </article>
          </div>

          <aside className="dashboard-workspace-side">
            <article className="dashboard-paper-card hard-words-feature">
              <div className="paper-tape red" aria-hidden="true" />
              <h2>🔥 Hard Words</h2>
              {(metrics?.hardWords ?? []).length > 0 ? (
                <div className="hard-word-list dashboard-hard-list">
                  {(metrics?.hardWords ?? []).slice(0, 4).map((word) => (
                    <div key={word.vocabId} className="hard-word-row">
                      <strong>{word.word}</strong>
                      <p>{word.meaningVi || "—"} · {word.wrongCount} wrong · {word.lapseCount} lapses</p>
                    </div>
                  ))}
                </div>
              ) : (
                <div className="dashboard-empty-hard">
                  <span aria-hidden="true">🔥</span>
                  <p>Great! No hard words yet.<br />Keep learning!</p>
                </div>
              )}
            </article>

            <article className="dashboard-paper-card mission-card">
              <div className="paper-tape green" aria-hidden="true" />
              <h2><Target size={24} aria-hidden="true" />Today Mission</h2>
              <div className="mission-row">
                <CheckCircle2 size={22} aria-hidden="true" />
                <strong>Study {dailyGoal} new words</strong>
                <span>{learnedToday} / {dailyGoal}</span>
              </div>
              <div className="progress-track mission-track" aria-hidden="true">
                <div style={{ width: `${missionProgress}%` }} />
              </div>
            </article>

            <article className="dashboard-paper-card weekly-card">
              <h2>Weekly Stats</h2>
              <div className="weekly-bars" aria-label="Weekly learning stats">
                {weeklyStats.map((item) => (
                  <div key={item.date} className="weekly-bar-item">
                    <span style={{ height: `${Math.max(10, Math.round((item.total / weeklyMax) * 100))}%` }} />
                    <strong>{item.label}</strong>
                  </div>
                ))}
              </div>
            </article>
          </aside>
        </section>
      </div>
    </AppShell>
  );
}

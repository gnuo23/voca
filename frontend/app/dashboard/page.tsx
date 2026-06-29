"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { Activity, AlertTriangle, BookOpen, CalendarClock, Flame, Gauge, Layers, Target, Zap } from "lucide-react";
import { AppShell } from "@/components/AppShell";
import { DashboardMetrics, getCurrentUser, getDashboardMetrics, getHealth, getStoredToken, UserProfile } from "@/lib/api";

function getGreeting(): string {
  const hour = new Date().getHours();
  if (hour < 12) return "Good morning";
  if (hour < 17) return "Good afternoon";
  return "Good evening";
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

  return (
    <AppShell>
      <header className="topbar">
        <div>
          <h1>{getGreeting()}{user ? `, ${user.displayName}` : ""} 👋</h1>
          <p>{user ? `${user.englishLevel.replaceAll("_", " ")} level · Your learning overview` : "Loading profile…"}</p>
        </div>
        <span className="status">
          <Activity size={16} aria-hidden="true" />
          API {health}
        </span>
      </header>

      <section className="grid" aria-label="Learning metrics">
        <article className="card">
          <Gauge size={20} aria-hidden="true" style={{ color: "var(--primary)" }} />
          <div className="metric">{metrics?.wordsToReview ?? 0}</div>
          <p>Words to review</p>
        </article>
        <article className="card">
          <BookOpen size={20} aria-hidden="true" style={{ color: "var(--accent)" }} />
          <div className="metric">{metrics?.wordsReviewedToday ?? 0}</div>
          <p>Reviewed today</p>
        </article>
        <article className="card">
          <Zap size={20} aria-hidden="true" style={{ color: "var(--warning)" }} />
          <div className="metric">{metrics?.wordsLearnedToday ?? 0}</div>
          <p>Learned today</p>
        </article>
        <article className="card">
          <Layers size={20} aria-hidden="true" style={{ color: "var(--success)" }} />
          <div className="metric">{metrics?.accuracy ?? 0}%</div>
          <p>Accuracy</p>
        </article>
        <article className="card">
          <Target size={20} aria-hidden="true" style={{ color: "var(--primary)" }} />
          <div className="metric">{metrics?.streakDays ?? 0}</div>
          <p>Streak days</p>
        </article>
        <article className="card">
          <CalendarClock size={20} aria-hidden="true" style={{ color: "var(--danger)" }} />
          <div className="metric">{metrics?.overdueWords ?? 0}</div>
          <p>Overdue</p>
        </article>
      </section>

      <section className="dashboard-grid">
        <article className="card dashboard-panel">
          <h2>📊 Deck Progress</h2>
          <div className="deck-progress-list">
            {(metrics?.deckProgress ?? []).map((deck) => (
              <div key={deck.deckId} className="deck-progress-row">
                <div>
                  <strong>{deck.deckName}</strong>
                  <p>{deck.masteredCount} mastered · {deck.reviewCount} review · {deck.learningCount} learning</p>
                </div>
                <span>{deck.progressScore}%</span>
                <div className="progress-track" aria-hidden="true">
                  <div style={{ width: `${Math.min(deck.progressScore, 100)}%` }} />
                </div>
              </div>
            ))}
            {metrics?.deckProgress.length === 0 && <p className="form-muted">No deck progress yet. Create a deck to get started!</p>}
          </div>
        </article>

        <article className="card dashboard-panel">
          <h2>🔥 Hard Words</h2>
          <div className="hard-word-list">
            {(metrics?.hardWords ?? []).map((word) => (
              <div key={word.vocabId} className="hard-word-row">
                <strong>{word.word}</strong>
                <p>
                  {word.meaningVi || "—"} ·{" "}
                  <Flame size={12} aria-hidden="true" style={{ display: "inline", verticalAlign: "middle", color: "var(--danger)" }} />{" "}
                  {word.wrongCount} wrong · {word.lapseCount} lapses
                </p>
              </div>
            ))}
            {metrics?.hardWords.length === 0 && <p className="form-muted">No hard words yet. Keep learning!</p>}
          </div>
        </article>
      </section>
    </AppShell>
  );
}

"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import {
  BarChart3,
  BookOpen,
  CalendarCheck,
  ChevronRight,
  Gift,
  GraduationCap,
  Layers,
  Lightbulb,
  PencilLine,
  RotateCcw,
  Sparkles,
  Target,
  Trash2
} from "lucide-react";
import { AppShell } from "@/components/AppShell";
import { DeckForm } from "@/components/decks/DeckForm";
import { DeckSharePanel } from "@/components/decks/DeckSharePanel";
import { MatchGamePanel } from "@/components/match/MatchGamePanel";
import { QuizPanel } from "@/components/quiz/QuizPanel";
import { VocabImportPanel } from "@/components/vocab/VocabImportPanel";
import { VocabStudyPanel } from "@/components/vocab/VocabStudyPanel";
import {
  DashboardMetrics,
  Deck,
  RecentActivity,
  deleteDeck,
  getClassDeck,
  getDashboardMetrics,
  getDeck,
  getStoredToken,
  resetDeckProgress,
  updateDeck
} from "@/lib/api";
import { clearStoredLearnState } from "@/lib/learnStorage";

type DeckDetailControllerProps = {
  deckId: string;
  classId?: string;
  learnHref?: string;
  notFoundHref?: string;
};

function formatRelativeTime(value: string): string {
  const occurredAt = new Date(value).getTime();
  const diffMs = Date.now() - occurredAt;
  const minute = 60 * 1000;
  const hour = 60 * minute;
  const day = 24 * hour;

  if (!Number.isFinite(occurredAt)) return "";
  if (diffMs < minute) return "Vừa xong";
  if (diffMs < hour) return `${Math.max(1, Math.floor(diffMs / minute))} phút trước`;
  if (diffMs < day) return `${Math.floor(diffMs / hour)} giờ trước`;
  if (diffMs < day * 7) return `${Math.floor(diffMs / day)} ngày trước`;
  return new Intl.DateTimeFormat("vi-VN", { day: "2-digit", month: "2-digit" }).format(new Date(value));
}

function activityIcon(type: RecentActivity["type"]) {
  if (type === "DECK_CREATED" || type === "DECK_UPDATED") return PencilLine;
  if (type === "HARD_WORD") return BarChart3;
  return BookOpen;
}

export function DeckDetailController({ deckId, classId, learnHref, notFoundHref }: DeckDetailControllerProps) {
  const router = useRouter();
  const [token, setToken] = useState("");
  const [deck, setDeck] = useState<Deck | null>(null);
  const [metrics, setMetrics] = useState<DashboardMetrics | null>(null);
  const [saved, setSaved] = useState(false);
  const [error, setError] = useState("");
  const [vocabRefreshKey, setVocabRefreshKey] = useState(0);

  const resolvedLearnHref = learnHref ?? `/mydeck/${deckId}/learn`;
  const fallbackHref = notFoundHref ?? (classId ? `/classes/${classId}` : "/decks");

  useEffect(() => {
    const storedToken = getStoredToken();
    if (!storedToken) {
      router.push("/login");
      return;
    }

    setToken(storedToken);
    const loadDeck = classId ? getClassDeck(storedToken, classId, deckId) : getDeck(storedToken, deckId);
    loadDeck
      .then(setDeck)
      .catch(() => router.push(fallbackHref));
    getDashboardMetrics(storedToken)
      .then(setMetrics)
      .catch(() => setMetrics(null));
  }, [classId, deckId, fallbackHref, router]);

  async function loadDeck(authToken = token) {
    return classId ? getClassDeck(authToken, classId, deckId) : getDeck(authToken, deckId);
  }

  async function handleDelete() {
    if (!confirm("Xoá deck này và toàn bộ từ trong đó?")) return;
    setError("");
    try {
      await deleteDeck(token, deckId);
      router.push("/decks");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Không thể xoá deck");
    }
  }

  async function handleReset() {
    if (!confirm("Đặt lại tiến độ học của deck này? Các từ vẫn giữ nguyên, nhưng tiến độ sẽ về 0.")) return;
    setError("");
    try {
      const updated = await resetDeckProgress(token, deckId);
      clearStoredLearnState(deckId);
      setDeck(updated);
      setVocabRefreshKey((v) => v + 1);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Không thể reset tiến độ");
    }
  }

  async function refreshDeck() {
    if (!token) {
      return;
    }

    const refreshed = await loadDeck();
    setDeck(refreshed);
    getDashboardMetrics(token)
      .then(setMetrics)
      .catch(() => setMetrics(null));
  }

  async function handleImported() {
    await refreshDeck();
    setVocabRefreshKey((value) => value + 1);
  }

  const level = metrics?.level?.level ?? 1;
  const weeklyStats = metrics?.weeklyStats?.length ? metrics.weeklyStats : [];
  const weeklyMax = Math.max(1, ...weeklyStats.map((item) => item.total));
  const deckActivities = (metrics?.recentActivities ?? [])
    .filter((item) => item.deckId === Number(deckId))
    .slice(0, 4);
  const canManageDeck = Boolean(deck?.ownedByCurrentUser);

  return (
    <AppShell>
      <header className="topbar deck-hero-topbar">
        <div>
          <p className="deck-welcome">Chào mừng trở lại,</p>
          <h1>
            {deck?.name ?? "Deck Detail"}
            <span className="deck-level-pill">Lv. {level}</span>
          </h1>
          <p className="deck-hero-meta">
            {deck ? `🔥 ${deck.totalWords} từ · ${deck.learnedWords} đã thuộc · ${deck.dueTodayCount ?? 0} cần ôn hôm nay` : "Đang tải deck"}
          </p>
        </div>
        <div className="button-row">
          <button className="button secondary-button" type="button" onClick={handleReset} disabled={!deck}>
            <RotateCcw size={18} aria-hidden="true" />
            Reset tiến độ
          </button>
          {canManageDeck && (
            <button className="button danger-button" type="button" onClick={handleDelete} disabled={!deck}>
              <Trash2 size={18} aria-hidden="true" />
              Xoá deck
            </button>
          )}
        </div>
      </header>

      <div className="deck-command-layout">
        <div className="deck-command-main">
          <section className="deck-stat-grid" aria-label="Deck stats">
            <article className="card stat-card stat-card-purple">
              <div className="stat-icon"><Layers size={26} aria-hidden="true" /></div>
              <h2>Tổng số từ</h2>
              <div className="metric">{deck?.totalWords ?? 0}</div>
              <p>từ trong deck</p>
            </article>
            <article className="card stat-card stat-card-blue">
              <div className="stat-icon"><GraduationCap size={26} aria-hidden="true" /></div>
              <h2>Đã học</h2>
              <div className="metric">{deck?.learnedWords ?? 0}</div>
              <p>từ đã thuộc</p>
            </article>
            <article className="card stat-card stat-card-cyan">
              <div className="stat-icon"><CalendarCheck size={26} aria-hidden="true" /></div>
              <h2>Cần ôn hôm nay</h2>
              <div className="metric">{deck?.dueTodayCount ?? 0}</div>
              <p>đến hạn ôn ngay</p>
            </article>
          </section>

          {deck && (
            <Link href={resolvedLearnHref} className="learn-entry-card deck-learn-spotlight">
              <div className="learn-entry-icon">
                <Target size={34} aria-hidden="true" />
              </div>
              <div className="learn-entry-info">
                <h3>Học từ vựng</h3>
                <p>
                  {deck.totalWords < 2
                    ? "Cần ít nhất 2 từ để bắt đầu"
                    : (deck.dueTodayCount ?? 0) > 0
                      ? `${deck.dueTodayCount} từ cần ôn ngay hôm nay`
                      : deck.totalWords - deck.learnedWords > 0
                        ? `${deck.totalWords - deck.learnedWords} từ mới chưa học`
                        : "Bạn đã thuộc hết deck, ôn lại nhé"}
                </p>
              </div>
              <span className="deck-learn-cta">
                Bắt đầu học
                <ChevronRight size={18} aria-hidden="true" />
              </span>
            </Link>
          )}

          {deck && token && (
            <QuizPanel
              token={token}
              deckId={deckId}
              totalWords={deck.totalWords}
              savedQuestionCount={deck.savedQuestionCount ?? 0}
              canManageQuestions={canManageDeck}
              refreshDeck={refreshDeck}
            />
          )}

          <section className="card daily-goal-card">
            <div>
              <p>Hoàn thành mục tiêu hôm nay</p>
              <div className="daily-goal-track" aria-hidden="true">
                <span style={{ width: `${deck && deck.totalWords > 0 ? Math.min(100, Math.round((deck.learnedWords / deck.totalWords) * 100)) : 0}%` }} />
              </div>
            </div>
            <strong>{deck && deck.totalWords > 0 ? Math.min(100, Math.round((deck.learnedWords / deck.totalWords) * 100)) : 0}%</strong>
            <Gift size={62} aria-hidden="true" />
          </section>
        </div>

        <aside className="deck-command-side">
          <section className="card quick-chart-card">
            <div className="side-card-title">
              <h2>Thống kê nhanh</h2>
              <span>7 ngày</span>
            </div>
            <div className="mini-chart" aria-hidden="true">
              <div className="mini-bar-chart">
                {weeklyStats.map((item) => (
                  <span key={item.date} style={{ height: `${Math.max(8, Math.round((item.total / weeklyMax) * 100))}%` }} />
                ))}
              </div>
              <div className="mini-chart-labels">
                {(weeklyStats.length ? weeklyStats : [
                  { label: "T2" },
                  { label: "T3" },
                  { label: "T4" },
                  { label: "T5" },
                  { label: "T6" },
                  { label: "T7" },
                  { label: "CN" }
                ]).map((item) => <span key={item.label}>{item.label}</span>)}
              </div>
            </div>
          </section>

          <section className="card study-tip-card">
            <div>
              <h2>Mẹo học tập</h2>
              <p>Ôn tập đều đặn mỗi ngày giúp bạn ghi nhớ lâu hơn và tăng hiệu quả học tập.</p>
            </div>
            <div className="tip-icon"><Lightbulb size={26} aria-hidden="true" /></div>
            <div className="tip-dots" aria-hidden="true"><span /><span /><span /><span /></div>
          </section>

          <section className="card recent-card">
            <h2>Hoạt động gần đây</h2>
            {deckActivities.length > 0 ? deckActivities.map((activity) => {
              const Icon = activityIcon(activity.type);
              return (
                <div key={`${activity.type}-${activity.vocabId ?? activity.deckId}-${activity.occurredAt}`} className="recent-row">
                  <span><Icon size={18} aria-hidden="true" /></span>
                  <div><strong>{activity.title}</strong><p>{activity.description} · {formatRelativeTime(activity.occurredAt)}</p></div>
                </div>
              );
            }) : (
              <p className="recent-empty">Chưa có hoạt động thật cho deck này.</p>
            )}
          </section>
        </aside>
      </div>

      <section className="card deck-extra-card">
        <div className="section-heading">
          <div>
            <h2><Sparkles size={18} className="section-heading-icon" aria-hidden="true" />Công cụ deck</h2>
            <p>{canManageDeck ? "Chia sẻ, luyện ghép cặp, xem danh sách từ và nhập thêm từ mới." : "Luyện ghép cặp, xem danh sách từ và lưu tiến độ học của riêng bạn."}</p>
          </div>
        </div>
      </section>

      {deck && token && (
        <MatchGamePanel
          token={token}
          deckId={deckId}
          totalWords={deck.totalWords}
        />
      )}

      {deck && token && canManageDeck && (
        <DeckSharePanel token={token} deckId={deckId} />
      )}

      {deck && token && (
        <VocabStudyPanel
          token={token}
          deckId={deckId}
          canEdit={canManageDeck}
          refreshDeck={refreshDeck}
          refreshKey={vocabRefreshKey}
        />
      )}

      {deck && token && canManageDeck && (
        <VocabImportPanel
          token={token}
          deckId={deckId}
          onImported={handleImported}
        />
      )}

      {canManageDeck && (
        <section className="card profile-card deck-edit-card">
          <h2>Chỉnh sửa deck</h2>
          {deck && (
            <DeckForm
              initialDeck={deck}
              submitLabel="Lưu thay đổi"
              onSubmit={async (payload) => {
                const updated = await updateDeck(token, deckId, payload);
                setDeck(updated);
                setSaved(true);
              }}
            />
          )}
          {saved && <p className="form-success">Deck đã được lưu.</p>}
          {error && <p className="form-error">{error}</p>}
        </section>
      )}
      {!canManageDeck && error && <p className="form-error">{error}</p>}
    </AppShell>
  );
}

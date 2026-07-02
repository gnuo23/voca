"use client";

import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import Link from "next/link";
import {
  BarChart3,
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
import { Deck, deleteDeck, getDeck, getStoredToken, resetDeckProgress, updateDeck } from "@/lib/api";
import { clearStoredLearnState } from "@/lib/learnStorage";

export default function DeckDetailPage() {
  const params = useParams<{ deckId: string }>();
  const router = useRouter();
  const [token, setToken] = useState("");
  const [deck, setDeck] = useState<Deck | null>(null);
  const [saved, setSaved] = useState(false);
  const [error, setError] = useState("");
  const [vocabRefreshKey, setVocabRefreshKey] = useState(0);

  useEffect(() => {
    const storedToken = getStoredToken();
    if (!storedToken) {
      router.push("/login");
      return;
    }

    setToken(storedToken);
    getDeck(storedToken, params.deckId)
      .then(setDeck)
      .catch(() => router.push("/decks"));
  }, [params.deckId, router]);

  async function handleDelete() {
    if (!confirm("Xoá deck này và toàn bộ từ trong đó?")) return;
    setError("");
    try {
      await deleteDeck(token, params.deckId);
      router.push("/decks");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Không thể xoá deck");
    }
  }

  async function handleReset() {
    if (!confirm("Đặt lại tiến độ học của deck này? Các từ vẫn giữ nguyên, nhưng tiến độ sẽ về 0.")) return;
    setError("");
    try {
      const updated = await resetDeckProgress(token, params.deckId);
      clearStoredLearnState(params.deckId);
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

    const refreshed = await getDeck(token, params.deckId);
    setDeck(refreshed);
  }

  async function handleImported() {
    await refreshDeck();
    setVocabRefreshKey((value) => value + 1);
  }

  return (
    <AppShell>
      <header className="topbar deck-hero-topbar">
        <div>
          <p className="deck-welcome">Chào mừng trở lại,</p>
          <h1>{deck?.name ?? "Deck Detail"}</h1>
          <p className="deck-hero-meta">
            {deck ? `🔥 ${deck.totalWords} từ · ${deck.learnedWords} đã thuộc · ${deck.dueTodayCount ?? 0} cần ôn hôm nay` : "Đang tải deck"}
          </p>
        </div>
        <div className="button-row">
          <button className="button secondary-button" type="button" onClick={handleReset} disabled={!deck}>
            <RotateCcw size={18} aria-hidden="true" />
            Reset tiến độ
          </button>
          <button className="button danger-button" type="button" onClick={handleDelete} disabled={!deck}>
            <Trash2 size={18} aria-hidden="true" />
            Xoá deck
          </button>
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
            <Link href={`/decks/${params.deckId}/learn`} className="learn-entry-card deck-learn-spotlight">
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
              deckId={params.deckId}
              totalWords={deck.totalWords}
              savedQuestionCount={deck.savedQuestionCount ?? 0}
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
              <svg viewBox="0 0 260 150" role="img">
                <defs>
                  <linearGradient id="deckChartFill" x1="0" x2="0" y1="0" y2="1">
                    <stop offset="0%" stopColor="#8b5cf6" stopOpacity="0.45" />
                    <stop offset="100%" stopColor="#8b5cf6" stopOpacity="0.02" />
                  </linearGradient>
                </defs>
                <path d="M12 98 C42 118 52 52 86 66 C122 80 117 28 154 48 C190 70 190 112 222 82 C240 64 244 42 252 34" fill="none" stroke="#8b5cf6" strokeWidth="5" strokeLinecap="round" />
                <path d="M12 98 C42 118 52 52 86 66 C122 80 117 28 154 48 C190 70 190 112 222 82 C240 64 244 42 252 34 L252 142 L12 142 Z" fill="url(#deckChartFill)" />
              </svg>
              <div className="mini-chart-labels">
                <span>T2</span><span>T3</span><span>T4</span><span>T5</span><span>T6</span><span>T7</span><span>CN</span>
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
            <div className="recent-row">
              <span><PencilLine size={18} aria-hidden="true" /></span>
              <div><strong>Tạo deck mới</strong><p>2 giờ trước</p></div>
            </div>
            <div className="recent-row">
              <span><GraduationCap size={18} aria-hidden="true" /></span>
              <div><strong>Bắt đầu học</strong><p>2 giờ trước</p></div>
            </div>
            <div className="recent-row">
              <span><BarChart3 size={18} aria-hidden="true" /></span>
              <div><strong>Làm quiz</strong><p>Hôm qua</p></div>
            </div>
          </section>
        </aside>
      </div>

      <section className="card deck-extra-card">
        <div className="section-heading">
          <div>
            <h2><Sparkles size={18} className="section-heading-icon" aria-hidden="true" />Công cụ deck</h2>
            <p>Chia sẻ, luyện ghép cặp, xem danh sách từ và nhập thêm từ mới.</p>
          </div>
        </div>
      </section>

      {deck && token && (
        <MatchGamePanel
          token={token}
          deckId={params.deckId}
          totalWords={deck.totalWords}
        />
      )}

      {deck && token && (
        <DeckSharePanel token={token} deckId={params.deckId} />
      )}

      {deck && token && (
        <VocabStudyPanel
          token={token}
          deckId={params.deckId}
          refreshDeck={refreshDeck}
          refreshKey={vocabRefreshKey}
        />
      )}

      {deck && token && (
        <VocabImportPanel
          token={token}
          deckId={params.deckId}
          onImported={handleImported}
        />
      )}

      <section className="card profile-card deck-edit-card">
        <h2>Chỉnh sửa deck</h2>
        {deck && (
          <DeckForm
            initialDeck={deck}
            submitLabel="Lưu thay đổi"
            onSubmit={async (payload) => {
              const updated = await updateDeck(token, params.deckId, payload);
              setDeck(updated);
              setSaved(true);
            }}
          />
        )}
        {saved && <p className="form-success">Deck đã được lưu.</p>}
        {error && <p className="form-error">{error}</p>}
      </section>
    </AppShell>
  );
}

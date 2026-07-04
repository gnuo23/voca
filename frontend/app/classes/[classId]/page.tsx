"use client";

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { BookOpen, Copy, RefreshCw, Trash2, UsersRound } from "lucide-react";
import { AppShell } from "@/components/AppShell";
import {
  addClassDeck,
  Classroom,
  Deck,
  getClass,
  getStoredToken,
  listDecks,
  removeClassDeck,
  rotateClassCode,
} from "@/lib/api";

export default function ClassDetailPage() {
  const params = useParams<{ classId: string }>();
  const router = useRouter();
  const [token, setToken] = useState("");
  const [classroom, setClassroom] = useState<Classroom | null>(null);
  const [decks, setDecks] = useState<Deck[]>([]);
  const [selectedDeckId, setSelectedDeckId] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState("");
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    const storedToken = getStoredToken();
    if (!storedToken) {
      router.push("/login");
      return;
    }
    setToken(storedToken);
    Promise.all([getClass(storedToken, params.classId), listDecks(storedToken)])
      .then(([loadedClass, loadedDecks]) => {
        setClassroom(loadedClass);
        setDecks(loadedDecks);
      })
      .catch((err) => setError(err instanceof Error ? err.message : "Không tải được lớp"))
      .finally(() => setIsLoading(false));
  }, [params.classId, router]);

  const availableDecks = useMemo(() => {
    const attached = new Set(classroom?.decks.map((deck) => deck.deckId) ?? []);
    return decks.filter((deck) => !attached.has(deck.id));
  }, [classroom, decks]);

  async function copyCode() {
    if (!classroom) return;
    await navigator.clipboard.writeText(classroom.inviteCode);
    setCopied(true);
    setTimeout(() => setCopied(false), 1400);
  }

  async function handleRotateCode() {
    if (!token || !classroom) return;
    setIsSaving(true);
    setError("");
    try {
      setClassroom(await rotateClassCode(token, params.classId));
    } catch (err) {
      setError(err instanceof Error ? err.message : "Không đổi được mã lớp");
    } finally {
      setIsSaving(false);
    }
  }

  async function handleAddDeck(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!token || !selectedDeckId) return;
    setIsSaving(true);
    setError("");
    try {
      setClassroom(await addClassDeck(token, params.classId, selectedDeckId));
      setSelectedDeckId("");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Không thêm được deck");
    } finally {
      setIsSaving(false);
    }
  }

  async function handleRemoveDeck(deckId: number) {
    if (!token) return;
    setIsSaving(true);
    setError("");
    try {
      setClassroom(await removeClassDeck(token, params.classId, String(deckId)));
    } catch (err) {
      setError(err instanceof Error ? err.message : "Không gỡ được deck");
    } finally {
      setIsSaving(false);
    }
  }

  if (isLoading) {
    return (
      <AppShell>
        <section className="empty-state">
          <UsersRound size={28} aria-hidden="true" />
          <h2>Đang tải lớp...</h2>
        </section>
      </AppShell>
    );
  }

  if (!classroom) {
    return (
      <AppShell>
        <section className="empty-state">
          <h2>Không tìm thấy lớp</h2>
          {error && <p>{error}</p>}
          <Link className="button" href="/classes">Quay lại lớp học</Link>
        </section>
      </AppShell>
    );
  }

  return (
    <AppShell>
      <header className="topbar">
        <div>
          <h1>{classroom.name}</h1>
          <p>{classroom.description || "Lớp học từ vựng"}</p>
        </div>
        <div className="classroom-code-box">
          <span>Mã lớp</span>
          <strong>{classroom.inviteCode}</strong>
          <button className="icon-button" type="button" onClick={copyCode} aria-label="Copy class code">
            <Copy size={16} />
          </button>
          {classroom.role === "OWNER" && (
            <button className="icon-button" type="button" onClick={handleRotateCode} disabled={isSaving} aria-label="Rotate class code">
              <RefreshCw size={16} />
            </button>
          )}
          {copied && <small>Đã copy</small>}
        </div>
      </header>

      {error && <p className="form-error">{error}</p>}

      <section className="deck-stat-grid" aria-label="Class stats">
        <article className="stat-card">
          <span>Deck</span>
          <strong>{classroom.deckCount}</strong>
          <p>đang gắn trong lớp</p>
        </article>
        <article className="stat-card">
          <span>Thành viên</span>
          <strong>{classroom.memberCount}</strong>
          <p>đang tham gia</p>
        </article>
        <article className="stat-card">
          <span>Tiến độ của bạn</span>
          <strong>{classroom.learnedWords}/{classroom.totalWords}</strong>
          <p>từ đã học</p>
        </article>
      </section>

      <section className="card classroom-section">
        <div className="section-heading">
          <div>
            <h2>Deck trong lớp</h2>
            <p>Học viên có thể mở Learn trực tiếp từ các deck này.</p>
          </div>
          {classroom.role === "OWNER" && (
            <form className="classroom-add-deck" onSubmit={handleAddDeck}>
              <select value={selectedDeckId} onChange={(event) => setSelectedDeckId(event.target.value)} disabled={isSaving}>
                <option value="">Chọn deck</option>
                {availableDecks.map((deck) => (
                  <option key={deck.id} value={deck.id}>{deck.name}</option>
                ))}
              </select>
              <button className="button" type="submit" disabled={!selectedDeckId || isSaving}>
                Thêm deck
              </button>
            </form>
          )}
        </div>

        {classroom.decks.length === 0 ? (
          <div className="empty-state compact-empty">
            <BookOpen size={24} aria-hidden="true" />
            <h3>Chưa có deck trong lớp</h3>
          </div>
        ) : (
          <div className="classroom-deck-grid">
            {classroom.decks.map((deck) => (
              <article className="classroom-deck-card" key={deck.deckId}>
                <div>
                  <h3>{deck.deckName}</h3>
                  <p>{deck.description || "Chưa có mô tả"}</p>
                  <span>
                    {deck.totalWords} từ · {deck.learnedWords} đã thuộc · {deck.dueTodayCount} cần ôn hôm nay
                  </span>
                </div>
                <div className="button-row">
                  <Link className="button secondary-button" href={`/decks/${deck.deckId}/learn`}>
                    Học
                  </Link>
                  {classroom.role === "OWNER" && (
                    <Link className="button secondary-button" href={`/decks/${deck.deckId}`}>
                      Xem
                    </Link>
                  )}
                  {classroom.role === "OWNER" && (
                    <button className="icon-button danger-icon" type="button" onClick={() => handleRemoveDeck(deck.deckId)} disabled={isSaving} aria-label="Remove deck">
                      <Trash2 size={16} />
                    </button>
                  )}
                </div>
              </article>
            ))}
          </div>
        )}
      </section>

      {classroom.role === "OWNER" && (
        <section className="card classroom-section">
          <div className="section-heading">
            <div>
              <h2>Giám sát học tập</h2>
              <p>Theo dõi tiến độ từng thành viên trên các deck trong lớp.</p>
            </div>
          </div>
          <div className="preview-wrap">
            <table className="preview-table classroom-progress-table">
              <thead>
                <tr>
                  <th>Học viên</th>
                  <th>Đã chạm</th>
                  <th>Đã học</th>
                  <th>Mastered</th>
                  <th>Khó</th>
                  <th>Accuracy</th>
                  <th>Hoạt động cuối</th>
                </tr>
              </thead>
              <tbody>
                {classroom.members.map((member) => (
                  <tr key={member.userId}>
                    <td>
                      <strong>{member.displayName}</strong>
                      <span>{member.email}</span>
                    </td>
                    <td>{member.touchedWords}/{member.totalWords}</td>
                    <td>{member.learnedWords}</td>
                    <td>{member.masteredWords}</td>
                    <td>{member.difficultWords}</td>
                    <td>{member.accuracyPercent}%</td>
                    <td>{member.lastActivityAt ? new Date(member.lastActivityAt).toLocaleDateString("vi-VN") : "Chưa học"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      )}
    </AppShell>
  );
}

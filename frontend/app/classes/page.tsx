"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { BookOpen, Plus, UsersRound } from "lucide-react";
import { AppShell } from "@/components/AppShell";
import { Classroom, createClass, getStoredToken, joinClass, listClasses } from "@/lib/api";

export default function ClassesPage() {
  const router = useRouter();
  const [token, setToken] = useState("");
  const [classes, setClasses] = useState<Classroom[]>([]);
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [joinCode, setJoinCode] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    const storedToken = getStoredToken();
    if (!storedToken) {
      router.push("/login");
      return;
    }
    setToken(storedToken);
    listClasses(storedToken)
      .then(setClasses)
      .catch(() => router.push("/login"))
      .finally(() => setIsLoading(false));
  }, [router]);

  async function refresh(nextToken = token) {
    if (!nextToken) return;
    setClasses(await listClasses(nextToken));
  }

  async function handleCreate(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!token || !name.trim()) return;
    setIsSaving(true);
    setError("");
    try {
      const classroom = await createClass(token, { name: name.trim(), description: description.trim() });
      setName("");
      setDescription("");
      await refresh();
      router.push(`/classes/${classroom.inviteCode}`);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Không tạo được lớp");
    } finally {
      setIsSaving(false);
    }
  }

  async function handleJoin(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!token || !joinCode.trim()) return;
    setIsSaving(true);
    setError("");
    try {
      const classroom = await joinClass(token, joinCode.trim());
      setJoinCode("");
      await refresh();
      router.push(`/classes/${classroom.inviteCode}`);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Không vào được lớp");
    } finally {
      setIsSaving(false);
    }
  }

  return (
    <AppShell>
      <header className="topbar">
        <div>
          <h1>Lớp học</h1>
          <p>{isLoading ? "Đang tải..." : `${classes.length} lớp`}</p>
        </div>
      </header>

      {error && <p className="form-error">{error}</p>}

      <section className="classroom-action-grid" aria-label="Class actions">
        <form className="card classroom-action-card" onSubmit={handleCreate}>
          <div className="classroom-action-title">
            <Plus size={20} aria-hidden="true" />
            <h2>Tạo lớp</h2>
          </div>
          <label>
            Tên lớp
            <input value={name} onChange={(event) => setName(event.target.value)} maxLength={160} required />
          </label>
          <label>
            Mô tả
            <textarea value={description} onChange={(event) => setDescription(event.target.value)} rows={3} maxLength={1000} />
          </label>
          <button className="button" type="submit" disabled={isSaving}>
            Tạo lớp
          </button>
        </form>

        <form className="card classroom-action-card" onSubmit={handleJoin}>
          <div className="classroom-action-title">
            <UsersRound size={20} aria-hidden="true" />
            <h2>Vào lớp bằng mã</h2>
          </div>
          <label>
            Mã lớp
            <input
              value={joinCode}
              onChange={(event) => setJoinCode(event.target.value.toUpperCase())}
              placeholder="VD: A7K2Q9MN"
              maxLength={16}
              required
            />
          </label>
          <p className="form-muted">Nhập mã giáo viên gửi để xem deck và học trong lớp.</p>
          <button className="button secondary-button" type="submit" disabled={isSaving}>
            Vào lớp
          </button>
        </form>
      </section>

      {classes.length === 0 && !isLoading ? (
        <section className="empty-state">
          <UsersRound size={28} aria-hidden="true" />
          <h2>Chưa có lớp nào</h2>
          <p>Tạo lớp để gom deck và theo dõi tiến độ, hoặc nhập mã lớp từ người khác.</p>
        </section>
      ) : (
        <section className="deck-list" aria-label="Classes">
          {classes.map((classroom) => (
            <Link className="card deck-card classroom-card" key={classroom.id} href={`/classes/${classroom.inviteCode}`}>
              <div>
                <h2>{classroom.name}</h2>
                <p>{classroom.description || "Chưa có mô tả"}</p>
              </div>
              <div className="progress-row">
                <span>{classroom.role === "OWNER" ? "Teacher" : "Student"}</span>
                <span>{classroom.deckCount} deck</span>
                <span>{classroom.memberCount} thành viên</span>
                <span>{classroom.learnedWords}/{classroom.totalWords} từ</span>
              </div>
              <BookOpen size={22} aria-hidden="true" />
            </Link>
          ))}
        </section>
      )}
    </AppShell>
  );
}

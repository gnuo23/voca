"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { Activity, BookOpen, Gauge, Layers } from "lucide-react";
import { AppShell } from "@/components/AppShell";
import { getCurrentUser, getHealth, getStoredToken, UserProfile } from "@/lib/api";

export default function DashboardPage() {
  const router = useRouter();
  const [health, setHealth] = useState("Checking");
  const [user, setUser] = useState<UserProfile | null>(null);

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
  }, [router]);

  return (
    <AppShell>
      <header className="topbar">
        <div>
          <h1>Dashboard</h1>
          <p>{user ? `${user.displayName} · ${user.englishLevel.replaceAll("_", " ")}` : "Loading profile"}</p>
        </div>
        <span className="status">
          <Activity size={18} aria-hidden="true" />
          API {health}
        </span>
      </header>

      <section className="grid" aria-label="Learning metrics">
        <article className="card">
          <Gauge size={22} aria-hidden="true" />
          <div className="metric">{user?.dailyGoal ?? 0}</div>
          <p>Daily word goal</p>
        </article>
        <article className="card">
          <BookOpen size={22} aria-hidden="true" />
          <div className="metric">0</div>
          <p>Words learned</p>
        </article>
        <article className="card">
          <Layers size={22} aria-hidden="true" />
          <div className="metric">0</div>
          <p>Active decks</p>
        </article>
      </section>
    </AppShell>
  );
}

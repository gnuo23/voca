import Link from "next/link";
import { Activity, BookOpen, Gauge, Home, Layers, LogOut } from "lucide-react";
import { getHealth } from "@/lib/api";

export default async function DashboardPage() {
  let health = "Unavailable";

  try {
    const response = await getHealth();
    health = response.status;
  } catch {
    health = "Offline";
  }

  return (
    <div className="shell">
      <aside className="sidebar">
        <div className="brand">
          <span className="brand-mark">V</span>
          <span>Voca</span>
        </div>
        <nav className="nav" aria-label="Main navigation">
          <Link className="active" href="/dashboard">
            <Home size={18} aria-hidden="true" />
            Dashboard
          </Link>
          <Link href="/dashboard">
            <BookOpen size={18} aria-hidden="true" />
            Vocabulary
          </Link>
          <Link href="/dashboard">
            <Layers size={18} aria-hidden="true" />
            Decks
          </Link>
          <Link href="/login">
            <LogOut size={18} aria-hidden="true" />
            Sign out
          </Link>
        </nav>
      </aside>
      <main className="main">
        <header className="topbar">
          <div>
            <h1>Dashboard</h1>
            <p>Track learning activity and API connectivity.</p>
          </div>
          <span className="status">
            <Activity size={18} aria-hidden="true" />
            API {health}
          </span>
        </header>

        <section className="grid" aria-label="Learning metrics">
          <article className="card">
            <Gauge size={22} aria-hidden="true" />
            <div className="metric">0</div>
            <p>Words due today</p>
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
      </main>
    </div>
  );
}

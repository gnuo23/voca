"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { CalendarClock, Home, Layers, LogOut, Repeat2, UserRound } from "lucide-react";
import { clearToken } from "@/lib/api";

export function AppShell({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();

  function signOut() {
    clearToken();
    router.push("/login");
  }

  return (
    <div className="shell">
      <aside className="sidebar">
        <div className="brand">
          <span className="brand-mark">V</span>
          <span>Voca</span>
        </div>
        <nav className="nav" aria-label="Main navigation">
          <Link className={pathname === "/dashboard" ? "active" : ""} href="/dashboard">
            <Home size={18} aria-hidden="true" />
            Dashboard
          </Link>
          <Link className={pathname.startsWith("/decks") ? "active" : ""} href="/decks">
            <Layers size={18} aria-hidden="true" />
            My Decks
          </Link>
          <Link className={pathname === "/review" ? "active" : ""} href="/review">
            <Repeat2 size={18} aria-hidden="true" />
            Review
          </Link>
          <Link className={pathname === "/review/schedule" ? "active" : ""} href="/review/schedule">
            <CalendarClock size={18} aria-hidden="true" />
            Schedule
          </Link>
          <Link className={pathname === "/profile" ? "active" : ""} href="/profile">
            <UserRound size={18} aria-hidden="true" />
            Profile
          </Link>
          <button className="nav-button" type="button" onClick={signOut}>
            <LogOut size={18} aria-hidden="true" />
            Sign out
          </button>
        </nav>
      </aside>
      <main className="main">{children}</main>
    </div>
  );
}

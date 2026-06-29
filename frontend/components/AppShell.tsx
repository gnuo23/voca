"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { BookOpen, Home, Layers, LogOut, UserRound } from "lucide-react";
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
          <Link href="/dashboard">
            <BookOpen size={18} aria-hidden="true" />
            Vocabulary
          </Link>
          <Link href="/dashboard">
            <Layers size={18} aria-hidden="true" />
            Decks
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

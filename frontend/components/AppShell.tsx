"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useState, useEffect } from "react";
import { AlertTriangle, CalendarClock, Flame, Home, Layers, LogOut, Menu, Moon, Repeat2, Sparkles, UserRound, X } from "lucide-react";
import { clearToken } from "@/lib/api";

export function AppShell({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const [drawerOpen, setDrawerOpen] = useState(false);

  // Close drawer on route change
  useEffect(() => {
    setDrawerOpen(false);
  }, [pathname]);

  // Close drawer on escape key
  useEffect(() => {
    function handleEsc(e: KeyboardEvent) {
      if (e.key === "Escape") setDrawerOpen(false);
    }
    window.addEventListener("keydown", handleEsc);
    return () => window.removeEventListener("keydown", handleEsc);
  }, []);

  function signOut() {
    clearToken();
    router.push("/login");
  }

  const navItems = [
    { href: "/dashboard", label: "Dashboard", icon: Home },
    { href: "/decks", label: "My Decks", icon: Layers },
    { href: "/review", label: "Review", icon: Repeat2, exact: true },
    { href: "/difficult", label: "Difficult", icon: AlertTriangle, exact: true },
    { href: "/review/schedule", label: "Schedule", icon: CalendarClock, exact: true },
    { href: "/profile", label: "Profile", icon: UserRound, exact: true },
  ];

  function isActive(item: { href: string; exact?: boolean }) {
    if (item.exact) return pathname === item.href;
    return pathname.startsWith(item.href);
  }

  return (
    <div className="shell">
      {/* Mobile menu toggle */}
      <button
        className="menu-toggle"
        type="button"
        onClick={() => setDrawerOpen(true)}
        aria-label="Open menu"
      >
        <Menu size={22} />
      </button>

      {/* Drawer overlay for mobile */}
      <div
        className={`drawer-overlay ${drawerOpen ? "open" : ""}`}
        onClick={() => setDrawerOpen(false)}
        aria-hidden="true"
      />

      <aside className={`sidebar ${drawerOpen ? "open" : ""}`}>
        <div className="brand">
          <span className="brand-mark">V</span>
          <span>Voca</span>
          {drawerOpen && (
            <button
              className="icon-button"
              type="button"
              onClick={() => setDrawerOpen(false)}
              aria-label="Close menu"
              style={{ marginLeft: "auto" }}
            >
              <X size={18} />
            </button>
          )}
        </div>

        <nav className="nav" aria-label="Main navigation">
          {navItems.map((item) => (
            <Link
              key={item.href}
              className={isActive(item) ? "active" : ""}
              href={item.href}
            >
              <item.icon size={18} aria-hidden="true" />
              {item.label}
            </Link>
          ))}
          <div className="sidebar-streak-card" aria-label="Learning streak">
            <div>
              <span>Streak</span>
              <strong>7 <small>ngày</small></strong>
              <p>Cố lên! Bạn đang làm rất tốt!</p>
            </div>
            <Flame size={30} aria-hidden="true" />
            <div className="streak-dots" aria-hidden="true">
              {["T2", "T3", "T4", "T5", "T6", "T7", "CN"].map((day, index) => (
                <span key={day} className={index < 6 ? "done" : ""}>
                  {day}
                </span>
              ))}
            </div>
          </div>
          <button className="nav-button" type="button" onClick={signOut}>
            <LogOut size={18} aria-hidden="true" />
            Sign out
          </button>
          <div className="sidebar-theme-switch" aria-hidden="true">
            <span><Moon size={16} /></span>
            <span className="active"><Sparkles size={16} /></span>
          </div>
        </nav>
      </aside>

      <main className="main">{children}</main>
    </div>
  );
}

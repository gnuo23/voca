"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useState, useEffect } from "react";
import { AlertTriangle, CalendarClock, Flame, Home, Layers, LogOut, Menu, Moon, Repeat2, Sun, UserRound, X } from "lucide-react";
import { clearToken, getDashboardMetrics, getStoredToken, type StreakDay } from "@/lib/api";

const DEFAULT_STREAK_WEEK: StreakDay[] = ["T2", "T3", "T4", "T5", "T6", "T7", "CN"].map((label) => ({
  label,
  date: "",
  active: false,
  today: false
}));
const THEME_STORAGE_KEY = "voca.theme";
type ThemeMode = "dark" | "light";

export function AppShell({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [theme, setTheme] = useState<ThemeMode>("dark");
  const [streakDays, setStreakDays] = useState(0);
  const [streakActiveToday, setStreakActiveToday] = useState(false);
  const [streakWeek, setStreakWeek] = useState<StreakDay[]>(DEFAULT_STREAK_WEEK);

  // Close drawer on route change
  useEffect(() => {
    setDrawerOpen(false);
  }, [pathname]);

  useEffect(() => {
    const storedTheme = window.localStorage.getItem(THEME_STORAGE_KEY);
    if (storedTheme === "light" || storedTheme === "dark") {
      setTheme(storedTheme);
    }
  }, []);

  useEffect(() => {
    document.documentElement.dataset.theme = theme;
    document.documentElement.style.colorScheme = theme;
    window.localStorage.setItem(THEME_STORAGE_KEY, theme);
  }, [theme]);

  useEffect(() => {
    const token = getStoredToken();
    if (!token) {
      return;
    }

    let cancelled = false;
    getDashboardMetrics(token)
      .then((metrics) => {
        if (cancelled) return;
        setStreakDays(metrics.streakDays ?? 0);
        setStreakActiveToday(Boolean(metrics.streakActiveToday));
        setStreakWeek(metrics.streakWeek?.length ? metrics.streakWeek : DEFAULT_STREAK_WEEK);
      })
      .catch(() => {
        if (cancelled) return;
        setStreakDays(0);
        setStreakActiveToday(false);
        setStreakWeek(DEFAULT_STREAK_WEEK);
      });

    return () => {
      cancelled = true;
    };
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
              <strong>{streakDays} <small>ngày</small></strong>
              <p>{streakActiveToday ? "Cố lên! Bạn đang làm rất tốt!" : "Học một câu để giữ nhịp hôm nay."}</p>
            </div>
            <Flame size={30} aria-hidden="true" />
            <div className="streak-dots" aria-hidden="true">
              {streakWeek.map((day) => (
                <span key={`${day.label}-${day.date}`} className={`${day.active ? "done" : ""} ${day.today ? "today" : ""}`.trim()}>
                  {day.label}
                </span>
              ))}
            </div>
          </div>
          <button className="nav-button" type="button" onClick={signOut}>
            <LogOut size={18} aria-hidden="true" />
            Sign out
          </button>
          <div className="sidebar-theme-switch" aria-label="Theme switch">
            <button
              type="button"
              className={theme === "dark" ? "active" : ""}
              onClick={() => setTheme("dark")}
              aria-label="Dark mode"
            >
              <Moon size={16} />
            </button>
            <button
              type="button"
              className={theme === "light" ? "active" : ""}
              onClick={() => setTheme("light")}
              aria-label="Light mode"
            >
              <Sun size={16} />
            </button>
          </div>
        </nav>
      </aside>

      <main className="main">{children}</main>
    </div>
  );
}

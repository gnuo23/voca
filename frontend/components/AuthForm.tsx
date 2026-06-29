"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { FormEvent, useState } from "react";
import { LogIn, UserPlus } from "lucide-react";
import {
  EnglishLevel,
  login,
  register,
  storeToken
} from "@/lib/api";

type AuthMode = "login" | "register";

const englishLevels: { value: EnglishLevel; label: string }[] = [
  { value: "BEGINNER", label: "Beginner" },
  { value: "ELEMENTARY", label: "Elementary" },
  { value: "INTERMEDIATE", label: "Intermediate" },
  { value: "UPPER_INTERMEDIATE", label: "Upper intermediate" },
  { value: "ADVANCED", label: "Advanced" }
];

export function AuthForm({ mode }: { mode: AuthMode }) {
  const router = useRouter();
  const [error, setError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [englishLevel, setEnglishLevel] = useState<EnglishLevel>("BEGINNER");

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    setIsSubmitting(true);

    const formData = new FormData(event.currentTarget);

    try {
      const response =
        mode === "login"
          ? await login({
              email: String(formData.get("email")),
              password: String(formData.get("password"))
            })
          : await register({
              email: String(formData.get("email")),
              password: String(formData.get("password")),
              displayName: String(formData.get("displayName")),
              englishLevel,
              learningGoal: String(formData.get("learningGoal")),
              dailyGoal: Number(formData.get("dailyGoal"))
            });

      storeToken(response.token);
      router.push("/dashboard");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Something went wrong");
    } finally {
      setIsSubmitting(false);
    }
  }

  const isLogin = mode === "login";

  return (
    <section className="login-panel" aria-labelledby="auth-title">
      <div className="brand">
        <span className="brand-mark">V</span>
        <span>Voca</span>
      </div>
      <h1 id="auth-title">{isLogin ? "Welcome back" : "Create account"}</h1>
      <p>{isLogin ? "Sign in to continue your vocabulary practice." : "Set your starting point for daily practice."}</p>
      <form onSubmit={onSubmit}>
        {!isLogin && (
          <div className="field">
            <label htmlFor="displayName">Display name</label>
            <input id="displayName" name="displayName" type="text" placeholder="Your name" required />
          </div>
        )}
        <div className="field">
          <label htmlFor="email">Email</label>
          <input id="email" name="email" type="email" placeholder="you@example.com" required />
        </div>
        <div className="field">
          <label htmlFor="password">Password</label>
          <input id="password" name="password" type="password" placeholder="At least 8 characters" minLength={8} required />
        </div>
        {!isLogin && (
          <>
            <div className="field">
              <label htmlFor="englishLevel">English level</label>
              <select
                id="englishLevel"
                name="englishLevel"
                value={englishLevel}
                onChange={(event) => setEnglishLevel(event.target.value as EnglishLevel)}
              >
                {englishLevels.map((level) => (
                  <option key={level.value} value={level.value}>
                    {level.label}
                  </option>
                ))}
              </select>
            </div>
            <div className="field">
              <label htmlFor="learningGoal">Learning goal</label>
              <textarea id="learningGoal" name="learningGoal" placeholder="Read articles, prepare for interviews..." rows={3} />
            </div>
            <div className="field">
              <label htmlFor="dailyGoal">Daily goal</label>
              <input id="dailyGoal" name="dailyGoal" type="number" min={1} max={200} defaultValue={10} required />
            </div>
          </>
        )}
        {error && <p className="form-error">{error}</p>}
        <button className="button full-width" type="submit" disabled={isSubmitting}>
          {isLogin ? <LogIn size={18} aria-hidden="true" /> : <UserPlus size={18} aria-hidden="true" />}
          {isSubmitting ? "Please wait" : isLogin ? "Sign in" : "Create account"}
        </button>
      </form>
      <div className="auth-switch">
        {isLogin ? (
          <Link href="/register">Create a new account</Link>
        ) : (
          <Link href="/login">Sign in instead</Link>
        )}
      </div>
    </section>
  );
}

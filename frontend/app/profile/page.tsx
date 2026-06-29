"use client";

import { FormEvent, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { Save } from "lucide-react";
import { AppShell } from "@/components/AppShell";
import {
  EnglishLevel,
  getCurrentUser,
  getStoredToken,
  updateProfile,
  UserProfile
} from "@/lib/api";

const englishLevels: { value: EnglishLevel; label: string }[] = [
  { value: "BEGINNER", label: "Beginner" },
  { value: "ELEMENTARY", label: "Elementary" },
  { value: "INTERMEDIATE", label: "Intermediate" },
  { value: "UPPER_INTERMEDIATE", label: "Upper intermediate" },
  { value: "ADVANCED", label: "Advanced" }
];

export default function ProfilePage() {
  const router = useRouter();
  const [token, setToken] = useState("");
  const [user, setUser] = useState<UserProfile | null>(null);
  const [error, setError] = useState("");
  const [saved, setSaved] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    const storedToken = getStoredToken();
    if (!storedToken) {
      router.push("/login");
      return;
    }

    setToken(storedToken);
    getCurrentUser(storedToken)
      .then(setUser)
      .catch(() => router.push("/login"));
  }, [router]);

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    setSaved(false);
    setIsSubmitting(true);

    const formData = new FormData(event.currentTarget);

    try {
      const updated = await updateProfile(token, {
        displayName: String(formData.get("displayName")),
        englishLevel: String(formData.get("englishLevel")) as EnglishLevel,
        learningGoal: String(formData.get("learningGoal")),
        dailyGoal: Number(formData.get("dailyGoal"))
      });
      setUser(updated);
      setSaved(true);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not save profile");
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <AppShell>
      <header className="topbar">
        <div>
          <h1>Profile</h1>
          <p>{user?.email ?? "Loading account"}</p>
        </div>
      </header>

      <section className="card profile-card">
        {user && (
          <form onSubmit={onSubmit}>
            <div className="form-grid">
              <div className="field">
                <label htmlFor="displayName">Display name</label>
                <input id="displayName" name="displayName" defaultValue={user.displayName} required />
              </div>
              <div className="field">
                <label htmlFor="englishLevel">English level</label>
                <select id="englishLevel" name="englishLevel" defaultValue={user.englishLevel}>
                  {englishLevels.map((level) => (
                    <option key={level.value} value={level.value}>
                      {level.label}
                    </option>
                  ))}
                </select>
              </div>
              <div className="field wide">
                <label htmlFor="learningGoal">Learning goal</label>
                <textarea id="learningGoal" name="learningGoal" defaultValue={user.learningGoal ?? ""} rows={4} />
              </div>
              <div className="field">
                <label htmlFor="dailyGoal">Daily goal</label>
                <input id="dailyGoal" name="dailyGoal" type="number" min={1} max={200} defaultValue={user.dailyGoal} required />
              </div>
            </div>
            {error && <p className="form-error">{error}</p>}
            {saved && <p className="form-success">Profile saved.</p>}
            <button className="button" type="submit" disabled={isSubmitting}>
              <Save size={18} aria-hidden="true" />
              {isSubmitting ? "Saving" : "Save profile"}
            </button>
          </form>
        )}
      </section>
    </AppShell>
  );
}

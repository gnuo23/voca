import { LearnProgress, LearnQuestion, LearnSession } from "@/lib/api";

export type StoredLearnState = {
  version: 1;
  session: LearnSession;
  question: LearnQuestion | null;
  questionTurn: number;
  progress: LearnProgress | null;
  pendingNext: boolean;
  showWrittenHint?: boolean;
  savedAt: number;
};

export function learnStorageKey(deckId: string) {
  return `voca.learn.${deckId}.active`;
}

export function readStoredLearnState(deckId: string): StoredLearnState | null {
  if (typeof window === "undefined") return null;
  try {
    const raw = window.localStorage.getItem(learnStorageKey(deckId));
    if (!raw) return null;
    const parsed = JSON.parse(raw) as Partial<StoredLearnState>;
    if (parsed.version !== 1 || !parsed.session) return null;
    return parsed as StoredLearnState;
  } catch {
    clearStoredLearnState(deckId);
    return null;
  }
}

export function writeStoredLearnState(deckId: string, state: Omit<StoredLearnState, "version" | "savedAt">) {
  if (typeof window === "undefined") return;
  window.localStorage.setItem(
    learnStorageKey(deckId),
    JSON.stringify({
      version: 1,
      savedAt: Date.now(),
      ...state,
    })
  );
}

export function clearStoredLearnState(deckId: string) {
  if (typeof window === "undefined") return;
  window.localStorage.removeItem(learnStorageKey(deckId));
}

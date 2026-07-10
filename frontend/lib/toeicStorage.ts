export type StoredToeicState = {
  version: 1;
  attemptId: number;
  answers: Record<number, string>;
  savedAt: number;
};

export function toeicStorageKey(attemptId: number) {
  return `voca.toeic.${attemptId}.active`;
}

export function readStoredToeicState(attemptId: number): StoredToeicState | null {
  if (typeof window === "undefined") return null;
  try {
    const raw = window.localStorage.getItem(toeicStorageKey(attemptId));
    if (!raw) return null;
    const parsed = JSON.parse(raw) as Partial<StoredToeicState>;
    if (parsed.version !== 1 || parsed.attemptId !== attemptId) return null;
    return parsed as StoredToeicState;
  } catch {
    clearStoredToeicState(attemptId);
    return null;
  }
}

export function writeStoredToeicState(attemptId: number, answers: Record<number, string>) {
  if (typeof window === "undefined") return;
  window.localStorage.setItem(
    toeicStorageKey(attemptId),
    JSON.stringify({ version: 1, attemptId, answers, savedAt: Date.now() })
  );
}

export function clearStoredToeicState(attemptId: number) {
  if (typeof window === "undefined") return;
  window.localStorage.removeItem(toeicStorageKey(attemptId));
}

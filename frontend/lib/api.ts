const API_BASE_URL =
  process.env.API_BASE_URL ??
  process.env.NEXT_PUBLIC_API_BASE_URL ??
  "http://localhost:8080";

export const TOKEN_STORAGE_KEY = "voca.auth.token";

export type HealthResponse = {
  status: string;
  service: string;
  timestamp: string;
};

export type EnglishLevel =
  | "BEGINNER"
  | "ELEMENTARY"
  | "INTERMEDIATE"
  | "UPPER_INTERMEDIATE"
  | "ADVANCED";

export type UserProfile = {
  id: number;
  email: string;
  displayName: string;
  englishLevel: EnglishLevel;
  learningGoal: string | null;
  dailyGoal: number;
};

export type AuthResponse = {
  token: string;
  tokenType: "Bearer";
  user: UserProfile;
};

export type RegisterPayload = {
  email: string;
  password: string;
  displayName: string;
  englishLevel: EnglishLevel;
  learningGoal: string;
  dailyGoal: number;
};

export type LoginPayload = {
  email: string;
  password: string;
};

export type UpdateProfilePayload = {
  displayName: string;
  englishLevel: EnglishLevel;
  learningGoal: string;
  dailyGoal: number;
};

export type Deck = {
  id: number;
  name: string;
  description: string | null;
  totalWords: number;
  learnedWords: number;
  dueWords: number;
  createdAt: string;
  updatedAt: string;
};

export type DeckPayload = {
  name: string;
  description: string;
};

export async function getHealth(): Promise<HealthResponse> {
  const response = await fetch(`${API_BASE_URL}/health`, {
    cache: "no-store"
  });

  if (!response.ok) {
    throw new Error(`Health check failed with status ${response.status}`);
  }

  return response.json();
}

export function getStoredToken(): string | null {
  if (typeof window === "undefined") {
    return null;
  }

  return window.localStorage.getItem(TOKEN_STORAGE_KEY);
}

export function storeToken(token: string) {
  window.localStorage.setItem(TOKEN_STORAGE_KEY, token);
}

export function clearToken() {
  window.localStorage.removeItem(TOKEN_STORAGE_KEY);
}

export async function register(payload: RegisterPayload): Promise<AuthResponse> {
  return apiRequest<AuthResponse>("/api/auth/register", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export async function login(payload: LoginPayload): Promise<AuthResponse> {
  return apiRequest<AuthResponse>("/api/auth/login", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export async function getCurrentUser(token: string): Promise<UserProfile> {
  return apiRequest<UserProfile>("/api/auth/me", {
    token
  });
}

export async function updateProfile(
  token: string,
  payload: UpdateProfilePayload
): Promise<UserProfile> {
  return apiRequest<UserProfile>("/api/users/me", {
    method: "PUT",
    token,
    body: JSON.stringify(payload)
  });
}

export async function listDecks(token: string): Promise<Deck[]> {
  return apiRequest<Deck[]>("/api/decks", {
    token
  });
}

export async function createDeck(token: string, payload: DeckPayload): Promise<Deck> {
  return apiRequest<Deck>("/api/decks", {
    method: "POST",
    token,
    body: JSON.stringify(payload)
  });
}

export async function getDeck(token: string, deckId: string): Promise<Deck> {
  return apiRequest<Deck>(`/api/decks/${deckId}`, {
    token
  });
}

export async function updateDeck(
  token: string,
  deckId: string,
  payload: DeckPayload
): Promise<Deck> {
  return apiRequest<Deck>(`/api/decks/${deckId}`, {
    method: "PUT",
    token,
    body: JSON.stringify(payload)
  });
}

export async function deleteDeck(token: string, deckId: string): Promise<void> {
  await apiRequest<void>(`/api/decks/${deckId}`, {
    method: "DELETE",
    token
  });
}

type ApiRequestOptions = RequestInit & {
  token?: string;
};

async function apiRequest<T>(path: string, options: ApiRequestOptions = {}): Promise<T> {
  const headers = new Headers(options.headers);
  headers.set("Content-Type", "application/json");

  if (options.token) {
    headers.set("Authorization", `Bearer ${options.token}`);
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers,
    cache: "no-store"
  });

  if (!response.ok) {
    let message = `Request failed with status ${response.status}`;
    try {
      const error = await response.json();
      message = error.message ?? message;
    } catch {
      // Keep the status-based fallback.
    }
    throw new Error(message);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return response.json();
}

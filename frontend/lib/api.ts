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

export type VocabImportStatus =
  | "OK"
  | "ERROR"
  | "DUPLICATE_IN_DECK"
  | "DUPLICATE_IN_IMPORT";

export type VocabImportItem = {
  lineNumber: number;
  word: string | null;
  partOfSpeech: string | null;
  meaningVi: string | null;
  status: VocabImportStatus;
  message: string | null;
};

export type VocabImportError = {
  lineNumber: number;
  message: string;
};

export type VocabImportPreview = {
  items: VocabImportItem[];
  errors: VocabImportError[];
};

export type VocabImportConfirm = {
  importedCount: number;
  items: VocabImportItem[];
  errors: VocabImportError[];
};

export type VocabProgressStatus = "NEW" | "LEARNING" | "REVIEW" | "DIFFICULT" | "MASTERED";

export type VocabMarkAction = "KNOWN" | "UNKNOWN" | "DIFFICULT";

export type VocabItem = {
  id: number;
  deckId: number;
  word: string;
  partOfSpeech: string | null;
  meaningVi: string | null;
  ipa: string | null;
  pronunciationHint: string | null;
  exampleEn: string | null;
  exampleVi: string | null;
  topic: string | null;
  level: string | null;
  synonyms: string[];
  antonyms: string[];
  collocations: string[];
  enrichedAt: string | null;
  audioUrl: string | null;
  audioUsUrl: string | null;
  audioUkUrl: string | null;
  audioAccent: string | null;
  audioSource: string | null;
  audioRefreshedAt: string | null;
  progressStatus: VocabProgressStatus;
  knownCount: number;
  unknownCount: number;
  difficultCount: number;
  lastMarkedAt: string | null;
  createdAt: string;
  updatedAt: string;
};

export type VocabItemPayload = {
  word: string;
  partOfSpeech: string;
  meaningVi: string;
};

export type VocabAudio = {
  vocabId: number;
  word: string;
  audioUrl: string | null;
  audioUsUrl: string | null;
  audioUkUrl: string | null;
  audioAccent: string | null;
  audioSource: string | null;
  audioRefreshedAt: string | null;
};

export type EnrichmentJobStatus = "PENDING" | "PROCESSING" | "DONE" | "FAILED";

export type EnrichmentJob = {
  id: number;
  status: EnrichmentJobStatus;
  totalItems: number;
  processedItems: number;
  failedItems: number;
  errorMessage: string | null;
  createdAt: string;
  updatedAt: string;
  completedAt: string | null;
};

export type QuizQuestionType = "CHOOSE_MEANING" | "FILL_IN_BLANK" | "CLOZE_CHOICE" | "TRUE_FALSE" | "MATCHING";

export type MatchingOptions = {
  words: string[];
  meanings: string[];
};

export type QuizQuestionOptions = string[] | MatchingOptions;

export type QuizQuestion = {
  id: number;
  deckId: number;
  vocabId: number;
  type: QuizQuestionType;
  prompt: string;
  options: QuizQuestionOptions;
  explanation: string | null;
  createdAt: string;
};

export type ManualQuizPayload = {
  questionTypes?: QuizQuestionType[];
  limit?: number;
  vocabPairs?: Array<{
    vocabId?: number;
    word?: string;
    meaning?: string;
  }>;
  questions?: Array<{
    vocabId?: number;
    word?: string;
    type: QuizQuestionType;
    prompt: string;
    options?: string[];
    matchingOptions?: MatchingOptions;
    correctAnswer?: string;
    correctPairs?: Record<string, string>;
    explanation?: string;
  }>;
};

export type QuizGenerateResponse = {
  deckId: number;
  questionCount: number;
  questions: QuizQuestion[];
};

export type QuizAttempt = {
  id: number;
  deckId: number;
  totalQuestions: number;
  answeredCount: number;
  correctCount: number;
  completed: boolean;
  completedAt: string | null;
  createdAt: string;
  questions: QuizQuestion[];
};

export type QuizAnswer = {
  questionId: number;
  answer: string;
  correctAnswer: string;
  correct: boolean;
  explanation: string;
  answeredAt: string;
};

export type QuizResult = {
  attemptId: number;
  deckId: number;
  totalQuestions: number;
  answeredCount: number;
  correctCount: number;
  scorePercent: number;
  completed: boolean;
  completedAt: string | null;
  answers: QuizAnswer[];
};

export type ReviewQuality = "AGAIN" | "HARD" | "GOOD" | "EASY";
export type ReviewSource = "FLASHCARD" | "QUIZ" | "LEARN";

export type LearnSessionScope = "ALL" | "NOT_MASTERED" | "DIFFICULT_ONLY" | "NEW_ONLY";
export type LearnGoal = "QUICK_REVIEW" | "LEARN_ALL" | "MASTER_ALL";
export type LearnAnswerDirection = "WORD_TO_MEANING" | "MEANING_TO_WORD" | "BOTH";
export type LearnGradingMode = "EXACT" | "ACCENT_INSENSITIVE" | "FUZZY";
export type LearnSessionStatus = "IN_PROGRESS" | "COMPLETED" | "ABANDONED";
export type LearnItemStage = "NEW" | "SEEN" | "LEARNING" | "FAMILIAR" | "MASTERED" | "NOT_STUDIED" | "STILL_LEARNING";
export type LearnQuestionType = "MCQ" | "WRITTEN" | "TRUE_FALSE";
export type LearnVerdict = "CORRECT" | "CLOSE" | "INCORRECT";

export type LearnProgress = {
  masteredTerms: number;
  totalTerms: number;
  remainingTerms: number;
  newTerms: number;
  seenTerms: number;
  learningTerms: number;
  familiarTerms: number;
};

export type LearnSession = {
  id: number;
  deckId: number;
  deckName: string;
  totalTerms: number;
  masteredTerms: number;
  totalAnswers: number;
  correctAnswers: number;
  scope: LearnSessionScope;
  goal: LearnGoal;
  answerDirection: LearnAnswerDirection;
  gradingMode: LearnGradingMode;
  status: LearnSessionStatus;
  startedAt: string;
  completedAt: string | null;
  durationMs: number;
};

export type StartLearnOptions = {
  scope?: LearnSessionScope;
  goal?: LearnGoal;
  answerDirection?: LearnAnswerDirection;
  gradingMode?: LearnGradingMode;
  questionTypes?: LearnQuestionType[];
};

export type LearnQuestion = {
  sessionItemId: number | null;
  vocabId: number | null;
  word: string | null;
  questionType: LearnQuestionType | null;
  questionToken: string | null;
  prompt: string;
  options: string[] | null;
  trueFalseStatement: string | null;
  stage: LearnItemStage | null;
  progress: LearnProgress;
};

export type LearnAnswer = {
  correct: boolean;
  verdict: LearnVerdict;
  similarityScore: number;
  userAnswer: string;
  correctAnswer: string;
  newStage: LearnItemStage;
  correctStreak: number;
  progress: LearnProgress;
};

export type LearnSessionResult = {
  session: LearnSession;
  items: Array<{
    vocabId: number;
    word: string;
    partOfSpeech: string | null;
    meaningVi: string | null;
    stage: LearnItemStage;
    correctAttempts: number;
    incorrectAttempts: number;
    totalAttempts: number;
  }>;
  history: Array<{
    questionType: LearnQuestionType;
    prompt: string;
    userAnswer: string;
    correctAnswer: string;
    correct: boolean;
    verdict: LearnVerdict;
    similarityScore: number;
    responseTimeMs: number | null;
    answeredAt: string;
  }>;
};

export type ReviewScheduleBucket =
  | "NEW"
  | "OVERDUE"
  | "DUE_NOW"
  | "TODAY"
  | "TOMORROW"
  | "THIS_WEEK"
  | "LATER";

export type ReviewItem = {
  vocabId: number;
  deckId: number;
  word: string;
  partOfSpeech: string | null;
  meaningVi: string | null;
  ipaUs: string | null;
  exampleEn: string | null;
  exampleVi: string | null;
  status: VocabProgressStatus;
  nextReviewAt: string | null;
  wrongCount: number;
  correctCount: number;
  lapseCount: number;
};

export type ReviewTodayResponse = {
  items: ReviewItem[];
  totalDue: number;
};

export type ReviewScheduleItem = {
  vocabId: number;
  deckId: number;
  deckName: string;
  word: string;
  partOfSpeech: string | null;
  meaningVi: string | null;
  status: VocabProgressStatus;
  bucket: ReviewScheduleBucket;
  lastReviewedAt: string | null;
  nextReviewAt: string | null;
  minutesUntilReview: number;
  correctCount: number;
  wrongCount: number;
  lapseCount: number;
  repetitionCount: number;
};

export type ReviewScheduleResponse = {
  items: ReviewScheduleItem[];
  totalItems: number;
  dueNow: number;
  overdue: number;
  upcoming: number;
  newItems: number;
};

export type ReviewProgress = {
  vocabId: number;
  status: VocabProgressStatus;
  quality: ReviewQuality;
  correctCount: number;
  wrongCount: number;
  streakCorrectCount: number;
  easeFactor: number;
  intervalDays: number;
  repetitionCount: number;
  lapseCount: number;
  lastReviewedAt: string;
  nextReviewAt: string;
};

export type HardWord = {
  vocabId: number;
  word: string;
  meaningVi: string | null;
  wrongCount: number;
  lapseCount: number;
  status: VocabProgressStatus;
};

export type DeckProgress = {
  deckId: number;
  deckName: string;
  totalWords: number;
  newCount: number;
  learningCount: number;
  reviewCount: number;
  difficultCount: number;
  masteredCount: number;
  progressScore: number;
};

export type DashboardMetrics = {
  wordsLearnedToday: number;
  wordsReviewedToday: number;
  wordsToReview: number;
  overdueWords: number;
  accuracy: number;
  streakDays: number;
  hardWords: HardWord[];
  deckProgress: DeckProgress[];
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

export async function resetDeckProgress(token: string, deckId: string): Promise<Deck> {
  return apiRequest<Deck>(`/api/decks/${deckId}/reset-progress`, {
    method: "POST",
    token
  });
}

export async function previewVocabImport(
  token: string,
  deckId: string,
  rawText: string
): Promise<VocabImportPreview> {
  return apiRequest<VocabImportPreview>("/api/vocab/import/preview", {
    method: "POST",
    token,
    body: JSON.stringify({ deckId: Number(deckId), rawText })
  });
}

export async function confirmVocabImport(
  token: string,
  deckId: string,
  rawText: string
): Promise<VocabImportConfirm> {
  return apiRequest<VocabImportConfirm>("/api/vocab/import/confirm", {
    method: "POST",
    token,
    body: JSON.stringify({ deckId: Number(deckId), rawText })
  });
}

export async function listDeckVocab(token: string, deckId: string): Promise<VocabItem[]> {
  return apiRequest<VocabItem[]>(`/api/decks/${deckId}/vocab`, {
    token
  });
}

export async function getVocabItem(token: string, vocabId: number): Promise<VocabItem> {
  return apiRequest<VocabItem>(`/api/vocab/${vocabId}`, {
    token
  });
}

export async function getVocabAudio(token: string, vocabId: number): Promise<VocabAudio> {
  return apiRequest<VocabAudio>(`/api/vocab/${vocabId}/audio`, {
    token
  });
}

export async function refreshVocabAudio(token: string, vocabId: number): Promise<VocabAudio> {
  return apiRequest<VocabAudio>(`/api/vocab/${vocabId}/refresh-audio`, {
    method: "POST",
    token
  });
}

export async function updateVocabItem(
  token: string,
  vocabId: number,
  payload: VocabItemPayload
): Promise<VocabItem> {
  return apiRequest<VocabItem>(`/api/vocab/${vocabId}`, {
    method: "PUT",
    token,
    body: JSON.stringify(payload)
  });
}

export async function deleteVocabItem(token: string, vocabId: number): Promise<void> {
  await apiRequest<void>(`/api/vocab/${vocabId}`, {
    method: "DELETE",
    token
  });
}

export async function markVocabItem(
  token: string,
  vocabId: number,
  action: VocabMarkAction
): Promise<VocabItem> {
  return apiRequest<VocabItem>(`/api/vocab/${vocabId}/mark`, {
    method: "POST",
    token,
    body: JSON.stringify({ action })
  });
}

export async function enrichVocabItem(token: string, vocabId: number): Promise<EnrichmentJob> {
  return apiRequest<EnrichmentJob>(`/api/vocab/${vocabId}/enrich`, {
    method: "POST",
    token
  });
}

export async function enrichDeck(token: string, deckId: string): Promise<EnrichmentJob> {
  return apiRequest<EnrichmentJob>(`/api/decks/${deckId}/enrich`, {
    method: "POST",
    token
  });
}

export async function getEnrichmentJob(token: string, jobId: number): Promise<EnrichmentJob> {
  return apiRequest<EnrichmentJob>(`/api/enrich/jobs/${jobId}`, {
    token
  });
}

export async function generateQuiz(token: string, deckId: string): Promise<QuizGenerateResponse> {
  return apiRequest<QuizGenerateResponse>(`/api/decks/${deckId}/quiz/generate`, {
    method: "POST",
    token
  });
}

export async function createQuizAttempt(
  token: string,
  deckId: string,
  questionIds: number[]
): Promise<QuizAttempt> {
  return apiRequest<QuizAttempt>("/api/quiz-attempts", {
    method: "POST",
    token,
    body: JSON.stringify({ deckId: Number(deckId), questionIds })
  });
}

export async function createManualQuizAttempt(
  token: string,
  deckId: string,
  payload: ManualQuizPayload
): Promise<QuizAttempt> {
  return apiRequest<QuizAttempt>(`/api/decks/${deckId}/quiz/manual-attempt`, {
    method: "POST",
    token,
    body: JSON.stringify(payload)
  });
}

export async function answerQuizQuestion(
  token: string,
  attemptId: number,
  questionId: number,
  answer: string,
  responseTimeMs?: number
): Promise<QuizAnswer> {
  return apiRequest<QuizAnswer>(`/api/quiz-attempts/${attemptId}/answer`, {
    method: "POST",
    token,
    body: JSON.stringify({ questionId, answer, responseTimeMs })
  });
}

export async function getQuizResult(token: string, attemptId: number): Promise<QuizResult> {
  return apiRequest<QuizResult>(`/api/quiz-attempts/${attemptId}/result`, {
    token
  });
}

export async function startLearnSession(
  token: string,
  deckId: string,
  options: StartLearnOptions = { scope: "NOT_MASTERED" }
): Promise<LearnSession> {
  return apiRequest<LearnSession>("/api/learn/sessions", {
    method: "POST",
    token,
    body: JSON.stringify({ deckId: Number(deckId), ...options })
  });
}

export async function getNextLearnQuestion(token: string, sessionId: number): Promise<LearnQuestion> {
  return apiRequest<LearnQuestion>(`/api/learn/sessions/${sessionId}/next`, {
    token
  });
}

export async function submitLearnAnswer(
  token: string,
  sessionId: number,
  sessionItemId: number,
  answer: string,
  questionType: LearnQuestionType,
  responseTimeMs?: number,
  questionToken?: string | null
): Promise<LearnAnswer> {
  return apiRequest<LearnAnswer>(`/api/learn/sessions/${sessionId}/answer`, {
    method: "POST",
    token,
    body: JSON.stringify({ sessionItemId, answer, questionType, responseTimeMs, questionToken })
  });
}

export async function getLearnSessionResult(token: string, sessionId: number): Promise<LearnSessionResult> {
  return apiRequest<LearnSessionResult>(`/api/learn/sessions/${sessionId}/result`, {
    token
  });
}

export async function overrideLearnAnswer(
  token: string,
  sessionId: number,
  sessionItemId: number,
  verdict: LearnVerdict = "CORRECT"
): Promise<LearnAnswer> {
  return apiRequest<LearnAnswer>(`/api/learn/sessions/${sessionId}/override`, {
    method: "POST",
    token,
    body: JSON.stringify({ sessionItemId, verdict })
  });
}

export async function getDashboardMetrics(token: string): Promise<DashboardMetrics> {
  return apiRequest<DashboardMetrics>("/api/dashboard", {
    token
  });
}

export async function getTodayReview(token: string): Promise<ReviewTodayResponse> {
  return apiRequest<ReviewTodayResponse>("/api/review/today", {
    token
  });
}

export async function getReviewSchedule(
  token: string,
  params: { deckId?: string; status?: VocabProgressStatus | ""; limit?: number } = {}
): Promise<ReviewScheduleResponse> {
  const searchParams = new URLSearchParams();
  if (params.deckId) {
    searchParams.set("deckId", params.deckId);
  }
  if (params.status) {
    searchParams.set("status", params.status);
  }
  if (params.limit) {
    searchParams.set("limit", String(params.limit));
  }
  const query = searchParams.toString();
  return apiRequest<ReviewScheduleResponse>(`/api/review/schedule${query ? `?${query}` : ""}`, {
    token
  });
}

export async function submitReviewResult(
  token: string,
  vocabId: number,
  quality: ReviewQuality,
  responseTimeMs?: number
): Promise<ReviewProgress> {
  return apiRequest<ReviewProgress>(`/api/review/${vocabId}/result`, {
    method: "POST",
    token,
    body: JSON.stringify({ quality, responseTimeMs, source: "FLASHCARD" })
  });
}

export async function submitReviewAnswer(
  token: string,
  vocabId: number,
  isCorrect: boolean,
  responseTimeMs?: number
): Promise<ReviewProgress> {
  return apiRequest<ReviewProgress>(`/api/review/${vocabId}/result`, {
    method: "POST",
    token,
    body: JSON.stringify({ isCorrect, responseTimeMs, source: "FLASHCARD" })
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

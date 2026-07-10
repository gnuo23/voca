"use client";

import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { AppShell } from "@/components/AppShell";
import { ToeicRunner } from "@/components/toeic/ToeicRunner";
import { ToeicAttempt, getStoredToken, getToeicAttempt } from "@/lib/api";

export default function ToeicAttemptPage() {
  const router = useRouter();
  const params = useParams<{ slug: string; attemptId: string }>();
  const slug = params.slug;
  const attemptId = Number(params.attemptId);
  const [token, setToken] = useState("");
  const [attempt, setAttempt] = useState<ToeicAttempt | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const storedToken = getStoredToken();
    if (!storedToken) {
      router.push("/login");
      return;
    }
    setToken(storedToken);
    getToeicAttempt(storedToken, attemptId)
      .then((data) => {
        if (data.status === "COMPLETED") {
          router.push(`/toeic/${slug}/result/${attemptId}`);
          return;
        }
        setAttempt(data);
      })
      .catch(() => setError("Không tải được bài thi"))
      .finally(() => setIsLoading(false));
  }, [attemptId, router, slug]);

  return (
    <AppShell>
      {error ? <div className="form-error">{error}</div> : null}
      {isLoading ? <p>Đang tải…</p> : null}
      {token && attempt ? (
        <ToeicRunner
          token={token}
          attempt={attempt}
          resultHref={(id) => `/toeic/${slug}/result/${id}`}
        />
      ) : null}
    </AppShell>
  );
}

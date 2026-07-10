"use client";

import { FormEvent, useState } from "react";
import { Bot, Send, Sparkles, UserRound } from "lucide-react";
import { explainToeicQuestion, getStoredToken } from "@/lib/api";

type Message = {
  id: number;
  role: "user" | "assistant";
  content: string;
};

export function ExplainChatPanel({ questionId }: { questionId: number }) {
  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState("");
  const [isOpen, setIsOpen] = useState(false);
  const [isSending, setIsSending] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function ask(question?: string) {
    const prompt = question?.trim() || "Hãy giải thích câu này và cách chọn đáp án đúng.";
    const token = getStoredToken();
    if (!token || isSending) return;

    setIsOpen(true);
    setIsSending(true);
    setError(null);
    const userMessage: Message = { id: Date.now(), role: "user", content: prompt };
    setMessages((current) => [...current, userMessage]);
    setInput("");

    try {
      const response = await explainToeicQuestion(token, questionId, prompt);
      setMessages((current) => [
        ...current,
        { id: Date.now() + 1, role: "assistant", content: response.answer }
      ]);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Không thể tải giải thích từ AI");
    } finally {
      setIsSending(false);
    }
  }

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (input.trim()) void ask(input);
  }

  if (!isOpen) {
    return (
      <button className="button secondary toeic-explain-trigger" type="button" onClick={() => void ask()}>
        <Sparkles size={16} aria-hidden="true" /> Hỏi AI giải thích
      </button>
    );
  }

  return (
    <div className="toeic-explain-panel">
      <div className="toeic-explain-title">
        <Bot size={18} aria-hidden="true" />
        <strong>Trợ lý TOEIC</strong>
      </div>
      <div className="toeic-chat-log" aria-live="polite">
        {messages.map((message) => (
          <div className={`toeic-chat-msg ${message.role}`} key={message.id}>
            {message.role === "assistant" ? <Bot size={15} aria-hidden="true" /> : <UserRound size={15} aria-hidden="true" />}
            <span>{message.content}</span>
          </div>
        ))}
        {isSending ? <div className="toeic-chat-thinking">AI đang phân tích…</div> : null}
      </div>
      {error ? <div className="form-error">{error}</div> : null}
      <form className="toeic-chat-input-row" onSubmit={submit}>
        <input
          type="text"
          value={input}
          onChange={(event) => setInput(event.target.value)}
          maxLength={2000}
          placeholder="Hỏi thêm về ngữ pháp, từ vựng…"
          disabled={isSending}
          aria-label="Câu hỏi dành cho trợ lý TOEIC"
        />
        <button className="button" type="submit" disabled={isSending || !input.trim()} aria-label="Gửi câu hỏi">
          <Send size={16} aria-hidden="true" />
        </button>
      </form>
    </div>
  );
}

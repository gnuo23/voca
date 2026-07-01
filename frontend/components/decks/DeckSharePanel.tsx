"use client";

import { useEffect, useState } from "react";
import { Copy, RefreshCcw, Share2 } from "lucide-react";
import { DeckShareCode, getDeckShareCode, rotateDeckShareCode } from "@/lib/api";

type DeckSharePanelProps = {
  token: string;
  deckId: string;
};

export function DeckSharePanel({ token, deckId }: DeckSharePanelProps) {
  const [shareCode, setShareCode] = useState<DeckShareCode | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isCopied, setIsCopied] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    let cancelled = false;
    setIsLoading(true);
    getDeckShareCode(token, deckId)
      .then((data) => {
        if (!cancelled) {
          setShareCode(data);
          setError("");
        }
      })
      .catch((err) => {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : "Không tải được mã chia sẻ");
        }
      })
      .finally(() => {
        if (!cancelled) setIsLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [token, deckId]);

  async function handleCopy() {
    if (!shareCode) return;
    try {
      await navigator.clipboard.writeText(shareCode.code);
      setIsCopied(true);
      setTimeout(() => setIsCopied(false), 1500);
    } catch {
      setError("Không sao chép được, hãy chọn và copy thủ công.");
    }
  }

  async function handleRotate() {
    if (!confirm("Tạo mã mới sẽ làm mã cũ không còn dùng được. Tiếp tục?")) return;
    setIsLoading(true);
    setError("");
    try {
      const rotated = await rotateDeckShareCode(token, deckId);
      setShareCode(rotated);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Không tạo được mã mới");
    } finally {
      setIsLoading(false);
    }
  }

  return (
    <section className="card share-card">
      <div className="section-heading">
        <div>
          <h2>
            <Share2 size={20} aria-hidden="true" className="section-heading-icon" />
            Chia sẻ deck
          </h2>
          <p>Gửi mã này cho người khác để họ có thể nhập và sao chép toàn bộ deck (kèm câu hỏi quiz).</p>
        </div>
      </div>

      {error && <p className="form-error">{error}</p>}

      {isLoading && !shareCode ? (
        <p className="form-muted">Đang tải mã chia sẻ…</p>
      ) : shareCode ? (
        <>
          <div className="share-code-box">
            <code className="share-code-value">{shareCode.code}</code>
            <button
              type="button"
              className="button"
              onClick={handleCopy}
              disabled={isLoading}
              aria-label="Sao chép mã"
            >
              <Copy size={16} aria-hidden="true" />
              {isCopied ? "Đã copy!" : "Sao chép"}
            </button>
          </div>
          <div className="button-row share-code-actions">
            <button
              type="button"
              className="button secondary-button"
              onClick={handleRotate}
              disabled={isLoading}
            >
              <RefreshCcw size={16} aria-hidden="true" />
              Tạo mã mới
            </button>
          </div>
          <p className="form-muted">Mã có thể dùng nhiều lần. Bấm “Tạo mã mới” để vô hiệu hoá mã cũ.</p>
        </>
      ) : null}
    </section>
  );
}

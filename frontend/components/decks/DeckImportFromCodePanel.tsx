"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { DownloadCloud, Eye, Sparkles, X } from "lucide-react";
import { DeckSharePreview, importDeckShare, previewDeckShare } from "@/lib/api";

type DeckImportFromCodePanelProps = {
  token: string;
  onImported?: () => void;
};

export function DeckImportFromCodePanel({ token, onImported }: DeckImportFromCodePanelProps) {
  const router = useRouter();
  const [isOpen, setIsOpen] = useState(false);
  const [code, setCode] = useState("");
  const [preview, setPreview] = useState<DeckSharePreview | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState("");

  function resetState() {
    setCode("");
    setPreview(null);
    setError("");
  }

  async function handlePreview() {
    if (!code.trim()) return;
    setIsLoading(true);
    setError("");
    setPreview(null);
    try {
      const data = await previewDeckShare(token, code.trim().toUpperCase());
      setPreview(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Không tìm thấy deck với mã này");
    } finally {
      setIsLoading(false);
    }
  }

  async function handleImport() {
    if (!preview) return;
    setIsLoading(true);
    setError("");
    try {
      const newDeck = await importDeckShare(token, preview.code);
      onImported?.();
      resetState();
      setIsOpen(false);
      router.push(`/mydeck/${newDeck.id}`);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Không nhập được deck");
    } finally {
      setIsLoading(false);
    }
  }

  if (!isOpen) {
    return (
      <button
        className="button secondary-button"
        type="button"
        onClick={() => setIsOpen(true)}
      >
        <DownloadCloud size={18} aria-hidden="true" />
        Nhập mã chia sẻ
      </button>
    );
  }

  return (
    <section className="card deck-import-card">
      <div className="section-heading">
        <div>
          <h2>
            <DownloadCloud size={20} aria-hidden="true" className="section-heading-icon" />
            Nhập deck từ mã chia sẻ
          </h2>
          <p>Dán mã bạn được chia sẻ. Toàn bộ từ vựng + câu hỏi quiz sẽ được sao chép vào tài khoản của bạn.</p>
        </div>
        <button
          type="button"
          className="icon-button"
          aria-label="Đóng"
          onClick={() => {
            resetState();
            setIsOpen(false);
          }}
        >
          <X size={18} aria-hidden="true" />
        </button>
      </div>

      {error && <p className="form-error">{error}</p>}

      <div className="field share-import-field">
        <label htmlFor="share-code-input">Mã chia sẻ</label>
        <input
          id="share-code-input"
          className="share-code-input"
          value={code}
          onChange={(event) => {
            setCode(event.target.value.toUpperCase());
            setPreview(null);
          }}
          placeholder="VD: ABCD23H7K9"
          autoComplete="off"
          spellCheck={false}
          maxLength={16}
        />
      </div>

      <div className="button-row">
        <button
          type="button"
          className="button"
          onClick={handlePreview}
          disabled={isLoading || code.trim().length === 0}
        >
          <Eye size={18} aria-hidden="true" />
          Xem trước
        </button>
      </div>

      {preview && (
        <div className="share-preview">
          <div className="share-preview-row">
            <Sparkles size={16} aria-hidden="true" />
            <strong>{preview.deckName}</strong>
            <span className="status-pill neutral">{preview.totalWords} từ</span>
            <span className="status-pill neutral">{preview.totalQuestions} câu hỏi</span>
          </div>
          {preview.description && <p className="form-muted">{preview.description}</p>}
          <p className="form-muted">Tạo bởi: <strong>{preview.ownerName}</strong></p>
          <div className="button-row">
            <button
              type="button"
              className="button"
              onClick={handleImport}
              disabled={isLoading}
            >
              <DownloadCloud size={18} aria-hidden="true" />
              Nhập về tài khoản của tôi
            </button>
          </div>
        </div>
      )}
    </section>
  );
}

"use client";

import { useState } from "react";
import { Check, Eye } from "lucide-react";
import {
  confirmVocabImport,
  previewVocabImport,
  VocabImportPreview
} from "@/lib/api";

type VocabImportPanelProps = {
  token: string;
  deckId: string;
  onImported: () => void;
};

const sampleText = `absent ; (adj) vang mat, khong co mat
accumulate ; (v) tich luy, gom gop
real estate ; (n) bat dong san`;

export function VocabImportPanel({ token, deckId, onImported }: VocabImportPanelProps) {
  const [rawText, setRawText] = useState("");
  const [preview, setPreview] = useState<VocabImportPreview | null>(null);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [isPreviewing, setIsPreviewing] = useState(false);
  const [isConfirming, setIsConfirming] = useState(false);

  const hasBlockingIssues =
    !preview ||
    preview.errors.length > 0 ||
    preview.items.length === 0 ||
    preview.items.some((item) => item.status !== "OK");

  async function handlePreview() {
    setError("");
    setSuccess("");
    setIsPreviewing(true);

    try {
      const result = await previewVocabImport(token, deckId, rawText);
      setPreview(result);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not preview vocabulary");
    } finally {
      setIsPreviewing(false);
    }
  }

  async function handleConfirm() {
    setError("");
    setSuccess("");
    setIsConfirming(true);

    try {
      const result = await confirmVocabImport(token, deckId, rawText);
      setSuccess(`Imported ${result.importedCount} item${result.importedCount === 1 ? "" : "s"}.`);
      setRawText("");
      setPreview(null);
      onImported();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not import vocabulary");
    } finally {
      setIsConfirming(false);
    }
  }

  return (
    <section className="card import-card">
      <div className="section-heading">
        <div>
          <h2>Import Vocabulary</h2>
          <p>Paste one word or phrase per line.</p>
        </div>
      </div>

      <div className="field">
        <label htmlFor="rawText">Vocabulary list</label>
        <textarea
          id="rawText"
          value={rawText}
          onChange={(event) => {
            setRawText(event.target.value);
            setPreview(null);
            setSuccess("");
          }}
          placeholder={sampleText}
          rows={8}
        />
      </div>

      <div className="button-row">
        <button className="button" type="button" onClick={handlePreview} disabled={isPreviewing || rawText.trim().length === 0}>
          <Eye size={18} aria-hidden="true" />
          {isPreviewing ? "Previewing" : "Preview"}
        </button>
        <button className="button" type="button" onClick={handleConfirm} disabled={isConfirming || hasBlockingIssues}>
          <Check size={18} aria-hidden="true" />
          {isConfirming ? "Importing" : "Confirm import"}
        </button>
      </div>

      {error && <p className="form-error">{error}</p>}
      {success && <p className="form-success">{success}</p>}

      {preview && (
        <div className="preview-wrap">
          {preview.errors.length > 0 && (
            <div className="error-list">
              {preview.errors.map((item) => (
                <p key={`${item.lineNumber}-${item.message}`}>
                  Line {item.lineNumber}: {item.message}
                </p>
              ))}
            </div>
          )}

          <table className="preview-table">
            <thead>
              <tr>
                <th>Line</th>
                <th>Word</th>
                <th>POS</th>
                <th>Meaning</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {preview.items.map((item) => (
                <tr key={`${item.lineNumber}-${item.word}`}>
                  <td>{item.lineNumber}</td>
                  <td>{item.word}</td>
                  <td>{item.partOfSpeech || "-"}</td>
                  <td>{item.meaningVi || "-"}</td>
                  <td>
                    <span className={`status-pill ${item.status === "OK" ? "ok" : "bad"}`}>
                      {item.status}
                    </span>
                    {item.message && <span className="status-message">{item.message}</span>}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}

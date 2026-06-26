import { useEffect, useRef, useState } from "react";
import * as pdfjsLib from "pdfjs-dist";
// Vite löst dieses ?url-Import zum gebündelten Worker-Asset auf.
import workerUrl from "pdfjs-dist/build/pdf.worker.min.mjs?url";

pdfjsLib.GlobalWorkerOptions.workerSrc = workerUrl;

interface PdfPreviewProps {
  /** Rohe PDF-Bytes (z. B. aus fetchPdfData). */
  data: ArrayBuffer;
}

/**
 * Rendert ein PDF clientseitig mit pdf.js auf <canvas>-Elemente.
 *
 * Hintergrund: Mobile Browser (v. a. Chrome auf Android) zeigen PDFs in einem
 * <iframe>/<embed> nicht inline an, sondern nur eine Download-Karte. Indem wir
 * jede Seite zu einem Bild rastern, ist die Vorschau plattformübergreifend –
 * auch auf dem Smartphone – tatsächlich sichtbar.
 */
export const PdfPreview = ({ data }: PdfPreviewProps) => {
  const containerRef = useRef<HTMLDivElement>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    const container = containerRef.current;
    if (!container) return;

    const render = async () => {
      try {
        // pdf.js übernimmt (neutralisiert) den übergebenen Buffer – daher eine
        // Kopie, damit ein erneuter Effekt-Lauf nicht auf entkoppelte Bytes trifft.
        const bytes = new Uint8Array(data.slice(0));
        const pdf = await pdfjsLib.getDocument({ data: bytes }).promise;
        if (cancelled) return;

        container.replaceChildren();

        const containerWidth = container.clientWidth || 600;
        const dpr = Math.min(window.devicePixelRatio || 1, 2);

        for (let pageNum = 1; pageNum <= pdf.numPages; pageNum++) {
          const page = await pdf.getPage(pageNum);
          if (cancelled) return;

          const unscaled = page.getViewport({ scale: 1 });
          // Auf Container-Breite skalieren, zusätzlich um DPR für scharfe Pixel.
          const scale = (containerWidth / unscaled.width) * dpr;
          const viewport = page.getViewport({ scale });

          const canvas = window.document.createElement("canvas");
          canvas.className = "offer-pdf-page";
          canvas.width = Math.floor(viewport.width);
          canvas.height = Math.floor(viewport.height);
          container.appendChild(canvas);

          const ctx = canvas.getContext("2d");
          if (!ctx) continue;

          await page.render({ canvas, canvasContext: ctx, viewport }).promise;
          if (cancelled) return;
        }
      } catch (e) {
        if (!cancelled) {
          console.error("[PdfPreview] Rendering fehlgeschlagen:", e);
          setError("Vorschau konnte nicht gerendert werden.");
        }
      }
    };

    render();
    return () => {
      cancelled = true;
    };
  }, [data]);

  if (error) {
    return <p className="offer-pdf-render-error text-secondary">{error}</p>;
  }

  return <div ref={containerRef} className="offer-pdf-canvas-container" />;
};

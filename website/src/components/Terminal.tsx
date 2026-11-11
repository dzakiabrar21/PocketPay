"use client";

import { useState, type ReactNode } from "react";

/** A dark terminal block with a copy-to-clipboard button. [text] is exactly what gets copied. */
export default function Terminal({
  text,
  children,
  idle,
  done,
  style,
}: {
  text: string;
  children: ReactNode;
  idle: string;
  done: string;
  style?: React.CSSProperties;
}) {
  const [copied, setCopied] = useState(false);
  const copy = async () => {
    try {
      await navigator.clipboard.writeText(text);
      setCopied(true);
      setTimeout(() => setCopied(false), 1200);
    } catch {
      /* clipboard unavailable (insecure context): leave the text selectable */
    }
  };
  return (
    <div className="term" style={style}>
      <button className="copy" type="button" onClick={copy}>
        {copied ? done : idle}
      </button>
      {children}
    </div>
  );
}

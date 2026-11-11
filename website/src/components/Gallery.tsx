"use client";

import { useState } from "react";
import type { Dict } from "@/content/types";
import type { Theme } from "@/lib/shots";
import Phone from "./Phone";

/** Horizontal film strip of every screen, with a light/dark switch. */
export default function Gallery({ dict }: { dict: Dict }) {
  const { gallery } = dict;
  const [theme, setTheme] = useState<Theme>("light");
  const themes: { id: Theme; label: string }[] = [
    { id: "light", label: gallery.light },
    { id: "dark", label: gallery.dark },
  ];
  return (
    <section id="tampilan" style={{ padding: "20px 0 100px" }}>
      <div className="gallery-band">
        <div className="wrap">
          <div className="eyebrow">{gallery.eyebrow}</div>
          <h2>{gallery.title}</h2>
          <p className="sub">{gallery.sub}</p>
          <div className="tools">
            <div className="seg" role="group" aria-label={gallery.themeAria}>
              {themes.map((t) => (
                <button key={t.id} type="button" aria-pressed={theme === t.id} onClick={() => setTheme(t.id)}>
                  {t.label}
                </button>
              ))}
            </div>
          </div>
        </div>
        <div className="strip" tabIndex={0} aria-label="App screens">
          {gallery.items.map((item) => (
            <figure key={item.shot} className="shot">
              <Phone name={item.shot} lang={dict.lang} theme={theme} alt={dict.alts[item.shot]} sizes="236px" />
              <figcaption>
                <b>{item.title}</b>
                {item.text}
              </figcaption>
            </figure>
          ))}
        </div>
      </div>
    </section>
  );
}

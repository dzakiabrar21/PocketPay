"use client";

import Image from "next/image";
import { useEffect, useRef, useState } from "react";
import type { Dict } from "@/content/types";
import { SHOT_HEIGHT, SHOT_WIDTH, shotSrc } from "@/lib/shots";
import { InfoIcon } from "./Icons";
import Phone from "./Phone";
import Rich from "./Rich";

/**
 * Scroll-driven story: the chapter in the middle of the viewport is highlighted and the sticky phone
 * on the right cross-fades to that chapter's screenshot. On small screens each chapter shows its own phone.
 */
export default function Story({ dict }: { dict: Dict }) {
  const { story, lang } = { story: dict.story, lang: dict.lang };
  const [active, setActive] = useState(0);
  const refs = useRef<(HTMLElement | null)[]>([]);

  useEffect(() => {
    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) setActive(refs.current.indexOf(entry.target as HTMLElement));
        });
      },
      { rootMargin: "-42% 0px -42% 0px" },
    );
    refs.current.forEach((el) => el && observer.observe(el));
    return () => observer.disconnect();
  }, []);

  return (
    <section id="cerita">
      <div className="wrap">
        <div className="eyebrow">{story.eyebrow}</div>
        <h2>{story.title}</h2>
        <p className="sub">{story.sub}</p>
        <div className="warn" role="note">
          <InfoIcon />
          <div>
            <Rich text={story.warn} />
          </div>
        </div>

        <div className="story">
          <div className="chapters">
            {story.chapters.map((c, i) => (
              <article
                key={c.no}
                ref={(el) => {
                  refs.current[i] = el;
                }}
                className={`chapter${i === active ? " active" : ""}`}
              >
                <div className="no">{c.no}</div>
                <h3>{c.title}</h3>
                <p>{c.text}</p>
                <ul>
                  {c.bullets.map((b) => (
                    <li key={b}>{b}</li>
                  ))}
                </ul>
                <Phone name={c.shot} lang={lang} alt={dict.alts[c.shot]} className="inline-phone" sizes="210px" />
              </article>
            ))}
          </div>
          <div className="sticky" aria-hidden="true">
            <div className="phone">
              {story.chapters.map((c, i) => (
                <Image
                  key={c.shot}
                  src={shotSrc(c.shot, lang)}
                  alt=""
                  width={SHOT_WIDTH}
                  height={SHOT_HEIGHT}
                  sizes="330px"
                  style={{ opacity: i === active ? 1 : 0 }}
                  priority={i === 0}
                  unoptimized
                />
              ))}
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}

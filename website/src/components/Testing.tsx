"use client";

import { useEffect, useRef, useState } from "react";
import type { Dict } from "@/content/types";
import Terminal from "./Terminal";

/** Test inventory: bars fill when scrolled into view. */
export default function Testing({ dict }: { dict: Dict }) {
  const t = dict.testing;
  const [shown, setShown] = useState(false);
  const ref = useRef<HTMLDivElement>(null);
  const max = Math.max(...t.bars.map((b) => b.value));

  useEffect(() => {
    const el = ref.current;
    if (!el) return;
    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          setShown(true);
          observer.disconnect();
        }
      },
      { threshold: 0.3 },
    );
    observer.observe(el);
    return () => observer.disconnect();
  }, []);

  const commands = "./gradlew :app:testDebugUnitTest\n./gradlew :app:connectedDebugAndroidTest";

  return (
    <section id="pengujian" style={{ paddingTop: 20 }}>
      <div className="wrap">
        <div className="eyebrow">{t.eyebrow}</div>
        <h2>{t.title}</h2>
        <p className="sub">{t.sub}</p>
        <div className="bars" ref={ref}>
          {t.bars.map((b) => (
            <div key={b.label} className="bar">
              <span>{b.label}</span>
              <div className="t">
                <i style={{ width: shown ? `${Math.max(6, (b.value / max) * 100)}%` : 0 }} />
              </div>
              <span className="v">{b.value}</span>
            </div>
          ))}
        </div>
        <Terminal text={commands} idle={dict.copy.idle} done={dict.copy.done}>
          <span className="c">{t.commentJvm}</span>
          {"\n"}
          <span className="p">$</span> ./gradlew :app:testDebugUnitTest{"\n"}
          <span className="c">{t.commentDevice}</span>
          {"\n"}
          <span className="p">$</span> ./gradlew :app:connectedDebugAndroidTest
        </Terminal>
      </div>
    </section>
  );
}

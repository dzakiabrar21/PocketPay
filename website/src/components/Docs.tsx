import type { Dict } from "@/content/types";
import Rich from "./Rich";

export default function Docs({ dict }: { dict: Dict }) {
  const d = dict.docs;
  return (
    <section id="dokumentasi" style={{ paddingTop: 20 }}>
      <div className="wrap">
        <div className="eyebrow">{d.eyebrow}</div>
        <h2>{d.title}</h2>
        <div className="doclist">
          {d.items.map((item) => (
            <a key={item.file} className="doc" href={item.href} target="_blank" rel="noopener noreferrer">
              <code>{item.file}</code>
              <div>
                <b>{item.title}</b>
                <span>{item.text}</span>
              </div>
              <span className="ar" aria-hidden="true">
                →
              </span>
            </a>
          ))}
        </div>
        <div className="roadmap">
          <div className="rm" style={{ background: "var(--sand)" }}>
            <h3>{d.notBuilt.title}</h3>
            <ul>
              {d.notBuilt.items.map((i) => (
                <li key={i}>
                  <Rich text={i} />
                </li>
              ))}
            </ul>
          </div>
          <div className="rm" style={{ background: "var(--sage)" }}>
            <h3>{d.next.title}</h3>
            <ul>
              {d.next.items.map((i) => (
                <li key={i}>
                  <Rich text={i} />
                </li>
              ))}
            </ul>
          </div>
        </div>
      </div>
    </section>
  );
}

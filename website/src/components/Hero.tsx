import type { Dict } from "@/content/types";
import Phone from "./Phone";
import { REPO_URL } from "./Nav";

const noteIcons = [
  <svg key="a" viewBox="0 0 24 24" aria-hidden="true"><circle cx="9" cy="9" r="6" /><path d="M15 9.3a6 6 0 1 1-5.7 5.7" /></svg>,
  <svg key="b" viewBox="0 0 24 24" aria-hidden="true"><path d="M9 14 4 9l5-5" /><path d="M4 9h10a6 6 0 0 1 0 12h-3" /></svg>,
  <svg key="c" viewBox="0 0 24 24" aria-hidden="true"><rect x="4" y="11" width="16" height="10" rx="2" /><path d="M8 11V7a4 4 0 0 1 8 0v4" /></svg>,
];

export default function Hero({ dict }: { dict: Dict }) {
  const { hero } = dict;
  return (
    <div className="hero" id="top">
      <div className="wrap hero-in">
        <div>
          <span className="chip">
            <i />
            {hero.chip}
          </span>
          <h1>
            {hero.titleBefore}
            <em>{hero.titleEm}</em>
            {hero.titleAfter}
          </h1>
          <p className="lead">{hero.lead}</p>
          <div className="cta">
            <a className="btn btn-ink" href="#cerita">
              {hero.ctaStory}
            </a>
            <a className="btn btn-line" href={REPO_URL} target="_blank" rel="noopener noreferrer">
              {hero.ctaSource} ↗
            </a>
          </div>
          <div className="facts">
            {hero.facts.map((f) => (
              <div key={f.label}>
                <b>{f.value}</b>
                {f.label}
              </div>
            ))}
          </div>
        </div>
        <div className="stage">
          <Phone name="home" lang={dict.lang} alt={dict.alts.home} priority sizes="290px" />
          {hero.notes.map((n, i) => (
            <div key={n.title} className={`note n${i + 1}`}>
              <span className="ic">{noteIcons[i]}</span>
              <div>
                {n.title}
                <small>{n.sub}</small>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

import type { Dict } from "@/content/types";
import Rich from "./Rich";

export default function Security({ dict }: { dict: Dict }) {
  const { security } = dict;
  return (
    <section id="keamanan" style={{ paddingTop: 20 }}>
      <div className="wrap">
        <div className="eyebrow">{security.eyebrow}</div>
        <h2>{security.title}</h2>
        <div className="layers">
          <div>
            {security.layers.map((layer, i) => (
              <div key={layer.title} className="layer reveal">
                <div className="n">{i + 1}</div>
                <div>
                  <h3>{layer.title}</h3>
                  <p>
                    <Rich text={layer.text} />
                  </p>
                </div>
              </div>
            ))}
          </div>
          <aside className="limits reveal">
            <h3>{security.limitsTitle}</h3>
            <ul>
              {security.limits.map((l) => (
                <li key={l}>{l}</li>
              ))}
            </ul>
          </aside>
        </div>
      </div>
    </section>
  );
}

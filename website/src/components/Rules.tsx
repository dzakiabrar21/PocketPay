import type { Dict } from "@/content/types";
import Rich from "./Rich";

/** A receipt-style worked example next to the list of money rules. */
export default function Rules({ dict }: { dict: Dict }) {
  const { rules } = dict;
  const r = rules.receipt;
  return (
    <section id="aturan" style={{ paddingTop: 20 }}>
      <div className="wrap">
        <div className="eyebrow">{rules.eyebrow}</div>
        <h2>{rules.title}</h2>
        <p className="sub">
          <Rich text={rules.sub} />
        </p>
        <div className="rules">
          <div className="receipt reveal" role="img" aria-label={r.ariaLabel}>
            <h4>POCKETPAY</h4>
            <div className="ctr">{r.caption}</div>
            {r.lines.map((line, i) =>
              line === "rule" ? (
                <hr key={`hr${i}`} />
              ) : (
                <div key={line.label} className="row">
                  <span>{line.label}</span>
                  <b>{line.value}</b>
                </div>
              ),
            )}
            <hr />
            <div className="row total">
              <span>{r.totalLabel}</span>
              <b>{r.totalValue}</b>
            </div>
            <div className="ok">{r.badge}</div>
          </div>
          <dl className="defs">
            {rules.defs.map((d) => (
              <div key={d.term} className="reveal">
                <dt>{d.term}</dt>
                <dd>
                  <Rich text={d.text} />
                </dd>
              </div>
            ))}
          </dl>
        </div>
      </div>
    </section>
  );
}

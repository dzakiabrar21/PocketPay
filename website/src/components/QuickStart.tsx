import type { Dict } from "@/content/types";
import { REPO_URL } from "./Nav";
import Rich from "./Rich";
import Terminal from "./Terminal";

export default function QuickStart({ dict }: { dict: Dict }) {
  const s = dict.start;
  const commands = `git clone ${REPO_URL}.git\ncd PocketPay\n./gradlew installDebug`;
  return (
    <section id="mulai" style={{ paddingTop: 20 }}>
      <div className="wrap">
        <div className="eyebrow">{s.eyebrow}</div>
        <h2>{s.title}</h2>
        <div className="two">
          <Terminal text={commands} idle={dict.copy.idle} done={dict.copy.done} style={{ marginTop: 0 }}>
            <span className="p">$</span> git clone {REPO_URL}.git{"\n"}
            <span className="p">$</span> cd PocketPay{"\n"}
            <span className="p">$</span> ./gradlew installDebug
          </Terminal>
          <ul className="tips">
            {s.tips.map((tip) => (
              <li key={tip}>
                <Rich text={tip} />
              </li>
            ))}
          </ul>
        </div>
      </div>
    </section>
  );
}

import type { Dict, TileKey } from "@/content/types";
import { icons } from "./Icons";

/** Order and bento size class of every feature tile. */
const layout: { key: TileKey; cls: string }[] = [
  { key: "qris", cls: "t-a" },
  { key: "roundup", cls: "t-b" },
  { key: "interest", cls: "t-c" },
  { key: "refund", cls: "t-d" },
  { key: "stats", cls: "t-e" },
  { key: "scan", cls: "t-f" },
  { key: "notify", cls: "t-g" },
  { key: "security", cls: "t-h" },
  { key: "i18n", cls: "t-i" },
];

export default function Features({ dict }: { dict: Dict }) {
  const { features } = dict;
  return (
    <section id="fitur" style={{ paddingTop: 20 }}>
      <div className="wrap">
        <div className="eyebrow">{features.eyebrow}</div>
        <h2>{features.title}</h2>
        <div className="bento">
          {layout.map(({ key, cls }) => (
            <article key={key} className={`tile ${cls} reveal`}>
              <div className="ic">{icons[key]}</div>
              <h3>{features.tiles[key].title}</h3>
              <p>{features.tiles[key].text}</p>
              {key === "interest" && (
                <div className="big">
                  {features.interestBig.value}
                  <small>{features.interestBig.label}</small>
                </div>
              )}
            </article>
          ))}
        </div>
      </div>
    </section>
  );
}

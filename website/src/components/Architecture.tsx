import type { Dict } from "@/content/types";
import Rich from "./Rich";

type NodeSpec = { x: number; y: number; w: number; title: string; sub: string; fill: string; dark?: boolean; stroke?: boolean };

const nodes: NodeSpec[] = [
  { x: 10, y: 36, w: 180, title: "Compose UI", sub: "tabs · flows · sheets", fill: "#DDEBC3" },
  { x: 235, y: 36, w: 180, title: "HomeViewModel", sub: "StateFlow · effects", fill: "#D9E8F2" },
  { x: 460, y: 36, w: 180, title: "Repository", sub: "Result · typed errors", fill: "#EFE6D2" },
  { x: 685, y: 36, w: 205, title: "BankingDao", sub: "@Transaction rules", fill: "#F8DFCF" },
  { x: 685, y: 196, w: 205, title: "Room + SQLCipher", sub: "AES-256 database", fill: "#1E2F40", dark: true },
  { x: 460, y: 196, w: 180, title: "MaintenanceWorker", sub: "daily · WorkManager", fill: "#fff", stroke: true },
  { x: 235, y: 196, w: 180, title: "PinManager", sub: "PBKDF2 · biometrics", fill: "#fff", stroke: true },
  { x: 10, y: 196, w: 180, title: "Android Keystore", sub: "non-exportable keys", fill: "#1E2F40", dark: true },
];

const stack: [string, string?][] = [
  ["Kotlin", "2.0"],
  ["Jetpack Compose", "M3"],
  ["Room", "2.7"],
  ["SQLCipher", "4.6"],
  ["Hilt", "2.60"],
  ["WorkManager", "2.10"],
  ["CameraX", "1.4"],
  ["ML Kit"],
  ["ZXing"],
  ["Coroutines & Flow"],
  ["minSdk 26"],
];

export default function Architecture({ dict }: { dict: Dict }) {
  const a = dict.architecture;
  return (
    <section id="arsitektur" style={{ paddingTop: 20 }}>
      <div className="wrap">
        <div className="eyebrow">{a.eyebrow}</div>
        <h2>{a.title}</h2>
        <p className="sub">
          <Rich text={a.sub} />
        </p>
        <div className="archbox">
          <svg viewBox="0 0 900 320" role="img" aria-label={a.diagramAria}>
            <defs>
              <marker id="ar" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto">
                <path d="M0 0 10 5 0 10z" fill="#5E8F2B" />
              </marker>
            </defs>
            <g fontSize="15" fontWeight="800" fill="#1B2B3A" textAnchor="middle">
              {nodes.map((n) => (
                <g key={n.title}>
                  <rect x={n.x} y={n.y} width={n.w} height={88} rx={22} fill={n.fill} stroke={n.stroke ? "#E6E0D1" : undefined} strokeWidth={n.stroke ? 2 : undefined} />
                  <text x={n.x + n.w / 2} y={n.y + 40} fill={n.dark ? "#fff" : undefined}>
                    {n.title}
                  </text>
                  <text x={n.x + n.w / 2} y={n.y + 62} fontSize="12" fontWeight="600" fill={n.dark ? "#B9C6D3" : "#66727F"}>
                    {n.sub}
                  </text>
                </g>
              ))}
            </g>
            <g stroke="#5E8F2B" strokeWidth="2.2" fill="none" markerEnd="url(#ar)">
              <path d="M190 80h42" />
              <path d="M415 80h42" />
              <path d="M640 80h42" />
              <path d="M787 124v70" />
              <path d="M550 194v-68" />
              <path d="M233 240h-40" />
            </g>
            <path d="M640 240h42" stroke="#5E8F2B" strokeWidth="2.2" fill="none" />
            <g fontSize="11" fontWeight="700" fill="#66727F" textAnchor="middle">
              <text x="211" y="70">Intent</text>
              <text x="436" y="70">call</text>
              <text x="661" y="70">SQL</text>
            </g>
          </svg>
        </div>
        <div className="decisions">
          {a.decisions.map((d) => (
            <div key={d.title} className="dec">
              <b>{d.title}</b>
              <p>
                <Rich text={d.text} />
              </p>
            </div>
          ))}
        </div>
        <div className="stack" aria-label="Tech stack">
          {stack.map(([name, version]) => (
            <span key={name}>
              {name}
              {version && <small>{version}</small>}
              {name === "minSdk 26" && <small>{a.stackNote}</small>}
            </span>
          ))}
        </div>
      </div>
    </section>
  );
}

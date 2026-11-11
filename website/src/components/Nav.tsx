import Link from "next/link";
import type { Dict } from "@/content/types";

export const REPO_URL = "https://github.com/dzakiabrar21/PocketPay";

/** Floating pill navigation. The language switch is a pair of links: `/` (Indonesian) and `/en`. */
export default function Nav({ dict }: { dict: Dict }) {
  const links: [string, string][] = [
    ["#cerita", dict.nav.story],
    ["#fitur", dict.nav.features],
    ["#tampilan", dict.nav.screens],
    ["#aturan", dict.nav.rules],
    ["#keamanan", dict.nav.security],
    ["#dokumentasi", dict.nav.docs],
  ];
  return (
    <header className="nav">
      <div className="nav-pill">
        <a className="brand" href="#top" aria-label="PocketPay">
          <span className="dot" />
          PocketPay
        </a>
        <nav style={{ display: "flex" }} aria-label="Main">
          {links.map(([href, label]) => (
            <a key={href} className="l" href={href}>
              {label}
            </a>
          ))}
        </nav>
        <div className="seg" role="group" aria-label={dict.nav.langAria}>
          <Link href="/" hrefLang="id" aria-current={dict.lang === "id" ? "true" : undefined} prefetch={false}>
            ID
          </Link>
          <Link href="/en" hrefLang="en" aria-current={dict.lang === "en" ? "true" : undefined} prefetch={false}>
            EN
          </Link>
        </div>
        <a className="btn btn-ink btn-sm" href={REPO_URL} target="_blank" rel="noopener noreferrer" style={{ marginLeft: 6 }}>
          GitHub
        </a>
      </div>
    </header>
  );
}

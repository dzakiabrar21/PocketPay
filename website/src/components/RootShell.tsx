import type { ReactNode } from "react";
import type { Lang } from "@/content/types";
import { fraunces, manrope } from "@/lib/fonts";

/** <html>/<body> for a language. Each route group has its own root layout so `lang` is correct per page. */
export default function RootShell({ lang, children }: { lang: Lang; children: ReactNode }) {
  return (
    <html lang={lang} className={`${fraunces.variable} ${manrope.variable}`}>
      <body>{children}</body>
    </html>
  );
}

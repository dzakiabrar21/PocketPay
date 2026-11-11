export type Lang = "id" | "en";

/** Names of the app screenshots in /public/screens (`<name>-<lang>-<theme>.webp`). */
export type ShotName =
  | "splash"
  | "pin"
  | "home"
  | "pay"
  | "success"
  | "stats"
  | "pockets"
  | "manage"
  | "receipt"
  | "receive"
  | "history"
  | "profile"
  | "statement";

export type TileKey = "qris" | "roundup" | "interest" | "refund" | "stats" | "scan" | "notify" | "security" | "i18n";

/**
 * Strings may contain **bold** and `code` markers; they are rendered by <Rich />.
 * Both languages implement this same type, so a missing translation is a compile error.
 */
export type Dict = {
  lang: Lang;
  meta: { title: string; description: string; ogTitle: string; ogDescription: string };
  skip: string;
  nav: { story: string; features: string; screens: string; rules: string; security: string; docs: string; langAria: string };
  hero: {
    chip: string;
    titleBefore: string;
    titleEm: string;
    titleAfter: string;
    lead: string;
    ctaStory: string;
    ctaSource: string;
    facts: { value: string; label: string }[];
    notes: { title: string; sub: string }[];
  };
  story: {
    eyebrow: string;
    title: string;
    sub: string;
    warn: string;
    chapters: { no: string; shot: ShotName; title: string; text: string; bullets: string[] }[];
  };
  features: {
    eyebrow: string;
    title: string;
    tiles: Record<TileKey, { title: string; text: string }>;
    interestBig: { value: string; label: string };
  };
  gallery: {
    eyebrow: string;
    title: string;
    sub: string;
    light: string;
    dark: string;
    themeAria: string;
    items: { shot: ShotName; title: string; text: string }[];
  };
  rules: {
    eyebrow: string;
    title: string;
    sub: string;
    receipt: {
      caption: string;
      ariaLabel: string;
      lines: ({ label: string; value: string } | "rule")[];
      totalLabel: string;
      totalValue: string;
      badge: string;
    };
    defs: { term: string; text: string }[];
  };
  security: {
    eyebrow: string;
    title: string;
    layers: { title: string; text: string }[];
    limitsTitle: string;
    limits: string[];
  };
  architecture: {
    eyebrow: string;
    title: string;
    sub: string;
    diagramAria: string;
    decisions: { title: string; text: string }[];
    stackNote: string;
  };
  testing: {
    eyebrow: string;
    title: string;
    sub: string;
    bars: { label: string; value: number }[];
    commentJvm: string;
    commentDevice: string;
  };
  start: {
    eyebrow: string;
    title: string;
    tips: string[];
  };
  docs: {
    eyebrow: string;
    title: string;
    items: { file: string; title: string; text: string; href: string }[];
    notBuilt: { title: string; items: string[] };
    next: { title: string; items: string[] };
  };
  footer: { text: string };
  copy: { idle: string; done: string };
  alts: Record<ShotName, string>;
};

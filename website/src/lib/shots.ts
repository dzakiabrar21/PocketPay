import type { Lang, ShotName } from "@/content/types";

export type Theme = "light" | "dark";

/** Screenshots are 540×1200 WebP files named `<screen>-<lang>-<theme>.webp`. */
export const SHOT_WIDTH = 540;
export const SHOT_HEIGHT = 1200;

export const shotSrc = (name: ShotName, lang: Lang, theme: Theme = "light") =>
  `/screens/${name}-${lang}-${theme}.webp`;

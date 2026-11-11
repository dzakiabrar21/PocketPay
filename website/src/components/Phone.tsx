import Image from "next/image";
import type { Lang, ShotName } from "@/content/types";
import { SHOT_HEIGHT, SHOT_WIDTH, shotSrc, type Theme } from "@/lib/shots";

type Props = {
  name: ShotName;
  lang: Lang;
  alt: string;
  theme?: Theme;
  className?: string;
  priority?: boolean;
  sizes?: string;
};

/** A phone frame around one app screenshot. */
export default function Phone({ name, lang, alt, theme = "light", className = "", priority = false, sizes = "300px" }: Props) {
  return (
    <div className={`phone ${className}`.trim()}>
      <Image src={shotSrc(name, lang, theme)} alt={alt} width={SHOT_WIDTH} height={SHOT_HEIGHT} priority={priority} sizes={sizes} unoptimized />
    </div>
  );
}

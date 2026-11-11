import type { Metadata } from "next";
import type { Dict } from "@/content/types";

/** Public origin of the deployed site. Set NEXT_PUBLIC_SITE_URL in Vercel once the final domain is known. */
export const SITE_URL = process.env.NEXT_PUBLIC_SITE_URL ?? "https://pocketpay.vercel.app";

export function buildMetadata(dict: Dict, path: "/" | "/en"): Metadata {
  return {
    metadataBase: new URL(SITE_URL),
    title: dict.meta.title,
    description: dict.meta.description,
    alternates: { canonical: path, languages: { id: "/", en: "/en", "x-default": "/" } },
    openGraph: {
      title: dict.meta.ogTitle,
      description: dict.meta.ogDescription,
      url: path,
      siteName: "PocketPay",
      locale: dict.lang === "id" ? "id_ID" : "en_US",
      type: "website",
      images: [{ url: "/og-image.png", width: 1200, height: 630, alt: "PocketPay" }],
    },
    twitter: { card: "summary_large_image", title: dict.meta.ogTitle, description: dict.meta.ogDescription, images: ["/og-image.png"] },
  };
}

import type { MetadataRoute } from "next";
import { SITE_URL } from "@/lib/metadata";

export default function sitemap(): MetadataRoute.Sitemap {
  return [
    { url: `${SITE_URL}/`, changeFrequency: "monthly", priority: 1, alternates: { languages: { id: `${SITE_URL}/`, en: `${SITE_URL}/en` } } },
    { url: `${SITE_URL}/en`, changeFrequency: "monthly", priority: 0.8 },
  ];
}

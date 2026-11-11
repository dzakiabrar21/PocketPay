import type { MetadataRoute } from "next";
import { SITE_URL } from "@/lib/metadata";

export default function sitemap(): MetadataRoute.Sitemap {
  return [
    { url: `${SITE_URL}/`, changeFrequency: "monthly", priority: 1, alternates: { languages: { en: `${SITE_URL}/`, id: `${SITE_URL}/id` } } },
    { url: `${SITE_URL}/id`, changeFrequency: "monthly", priority: 0.8 },
  ];
}

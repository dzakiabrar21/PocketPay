# PocketPay website

Bilingual landing page for the [PocketPay](../README.md) Android project, built with **Next.js (App Router) + TypeScript**.

| Route | Language |
|---|---|
| `/` | English (default) |
| `/id` | Indonesian |

Both routes are statically generated. Content lives in typed dictionaries (`src/content/id.ts`, `src/content/en.ts`);
both implement the `Dict` type, so a missing translation is a compile error.

## Develop

```bash
cd website
npm install
npm run dev        # http://localhost:3100 (3000 is often taken, e.g. by Grafana)
npm run typecheck
npm run build && npm start
```

## Structure

```
src/app            routes: (id)/ and en/ each have their own root layout so <html lang> is correct
src/components     one component per section (Hero, Story, Features, Gallery, Rules, Security, ...)
src/content        typed ID/EN copy
src/lib            fonts, screenshot paths, SEO metadata
public/screens     60 optimised WebP screenshots (<screen>-<lang>-<theme>.webp) and og-image.png
```

## Deploy to Vercel

1. Import the GitHub repo at <https://vercel.com/new>.
2. Set **Root Directory** to `website` (framework preset is detected as Next.js; no other settings needed).
3. Optional: add the environment variable `NEXT_PUBLIC_SITE_URL` with the final domain
   (for example `https://pocketpay.vercel.app`) so Open Graph and sitemap URLs are absolute.

## Regenerating screenshots

The screenshots come from the Android screenshot test (`ThemeScreenshotTest`, see `../docs/TESTING.md`).

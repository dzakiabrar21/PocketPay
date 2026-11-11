import type { Dict } from "./types";

const repo = "https://github.com/dzakiabrar21/PocketPay";

export const en: Dict = {
  lang: "en",
  meta: {
    title: "PocketPay — Small change, big savings (Android wallet)",
    description:
      "PocketPay is a portfolio Android app: a digital wallet with QRIS payments, round-up savings, interest-bearing pockets, 24-hour refunds, statistics and layered security. Simulated data, not a real financial product.",
    ogTitle: "PocketPay — Android wallet app (portfolio project)",
    ogDescription:
      "QRIS payments, round-up savings, interest-bearing pockets, refunds and an encrypted, PIN-locked app. A Kotlin + Jetpack Compose portfolio project.",
  },
  skip: "Skip to content",
  nav: {
    story: "Story",
    features: "Features",
    screens: "Screens",
    rules: "Money rules",
    security: "Security",
    docs: "Docs",
    langAria: "Language",
  },
  hero: {
    chip: "Portfolio project · simulated data",
    titleBefore: "Small change, ",
    titleEm: "big",
    titleAfter: " savings.",
    lead: "A portfolio project: an Android wallet app that rounds up every QRIS payment into a savings pocket. It also features interest-bearing pockets, 24-hour refunds, statistics and an encrypted database.",
    ctaStory: "See the story",
    ctaSource: "Source code",
    facts: [
      { value: "6,300+", label: "lines of Kotlin" },
      { value: "99", label: "tests" },
      { value: "2", label: "languages" },
      { value: "0", label: "servers — all on-device" },
    ],
    notes: [
      { title: "+Rp1.500", sub: "to the Spare Change Jar" },
      { title: "24-hour refund", sub: "atomic money back" },
      { title: "AES-256", sub: "key in the Keystore" },
    ],
  },
  story: {
    eyebrow: "The app story",
    title: "One payment, five things happening behind it",
    sub: "Scroll to follow a single transaction. The screen on the right changes with every chapter.",
    warn: "**Heads-up:** the money is simulated and the QRIS codes are only structurally valid because PocketPay is not a registered QRIS issuer. Do not use it for real funds.",
    chapters: [
      {
        no: "01",
        shot: "pay",
        title: "Pay without thinking",
        text: "Scan QRIS with the camera or the gallery. Only valid QRIS (CRC-16, IDR currency) is accepted; anything else is rejected immediately.",
        bullets: [
          "Choose the source: the Main Account or a pocket.",
          "Pocket short? The difference is taken from the Main Account inside one atomic transaction.",
        ],
      },
      {
        no: "02",
        shot: "pockets",
        title: "Spare change becomes savings",
        text: "The amount is rounded up to the next Rp 5,000 and the difference goes to the Spare Change Jar. Other pockets grow their balance with interest.",
        bullets: [
          "Spending pockets at 3.5% p.a. with goals and progress.",
          "Deposits at 6% p.a. locked until maturity.",
          "Daily interest arrives through WorkManager even when the app is closed.",
        ],
      },
      {
        no: "03",
        shot: "stats",
        title: "See where your money goes",
        text: "Weekly and monthly charts are built from real transactions. Tap a bar to see that day's income and spending.",
        bullets: [
          "Refunds, reversed payments and internal moves are never double-counted.",
          "History can be searched and filtered by type and period.",
        ],
      },
      {
        no: "04",
        shot: "pin",
        title: "Safe from the first tap",
        text: "The app always starts locked. A 6-digit PIN or biometrics is asked again after 60 s in the background and before every payment.",
        bullets: [
          "Five wrong tries lock PIN entry for 30 seconds.",
          "SQLCipher database; its key is wrapped by the Android Keystore.",
        ],
      },
      {
        no: "05",
        shot: "receipt",
        title: "Receipts you can reverse",
        text: "Every transaction has a digital receipt with a reference number and an idempotency key, so it can never be recorded twice.",
        bullets: [
          "Refund within 24 hours: money returns and the original is marked reversed.",
          "A multi-page PDF statement to open or share.",
        ],
      },
    ],
  },
  features: {
    eyebrow: "All features",
    title: "What a modern wallet needs, already working",
    interestBig: { value: "6%", label: "per year, locked deposit" },
    tiles: {
      qris: {
        title: "QRIS with cascading funding",
        text: "Camera + gallery scanner, EMVCo validation and atomic transactions. A short pocket is covered by the Main Account.",
      },
      roundup: { title: "Round-up", text: "Rounded to Rp 5,000, switchable per payment." },
      interest: {
        title: "Automatic daily interest",
        text: "Computed with BigDecimal and never paid twice. Notifications when a deposit matures or a goal is reached.",
      },
      refund: { title: "Refund", text: "24 hours, once only, atomic." },
      stats: { title: "Statistics", text: "Tappable weekly & monthly bars." },
      scan: {
        title: "Scan me & PDF e-statement",
        text: "A scannable receive QR and a multi-page statement with letterhead and watermark.",
      },
      notify: {
        title: "Private notifications",
        text: "The lock screen shows a generic line; details appear after unlocking.",
      },
      security: {
        title: "PIN + crypto-bound biometrics",
        text: "A PBKDF2 hash re-encrypted with the Keystore; the biometric key is invalidated by new enrolments.",
      },
      i18n: {
        title: "Bilingual, light & dark",
        text: "Full Indonesian and English; themes follow the system from one set of colour tokens.",
      },
    },
  },
  gallery: {
    eyebrow: "Screens",
    title: "Warm, clean and consistent",
    sub: "Swipe sideways. Images are rendered straight from the app with sample data.",
    light: "Light",
    dark: "Dark",
    themeAria: "Theme",
    items: [
      { shot: "splash", title: "Splash", text: "Branded entrance." },
      { shot: "pin", title: "PIN lock", text: "Keypad and fingerprint." },
      { shot: "home", title: "Home", text: "Balance, pockets, history." },
      { shot: "pay", title: "Pay with QRIS", text: "Funding & round-up." },
      { shot: "success", title: "Success", text: "Receipt-style summary." },
      { shot: "stats", title: "Statistics", text: "A tappable chart." },
      { shot: "pockets", title: "Pockets", text: "Spending, deposit, goals." },
      { shot: "manage", title: "Manage pocket", text: "Top up, withdraw, close." },
      { shot: "receipt", title: "Receipt & refund", text: "Transaction details." },
      { shot: "receive", title: "Scan me", text: "Receive QR." },
      { shot: "history", title: "History", text: "Search and filter." },
      { shot: "profile", title: "Profile", text: "Security status." },
      { shot: "statement", title: "E-statement", text: "PDF export." },
    ],
  },
  rules: {
    eyebrow: "Money rules",
    title: "Numbers you can check yourself",
    sub: "A real example: paying Rp 23,500 from the Jajan Harian pocket. Every rule lives in `BankingDao` and is tested.",
    receipt: {
      caption: "SAMPLE QRIS PAYMENT",
      ariaLabel: "Sample payment receipt",
      lines: [
        { label: "Merchant", value: "Kantin Mahasiswa" },
        { label: "Source", value: "Jajan Harian" },
        { label: "Paid", value: "Rp23.500" },
        "rule",
        { label: "Round-up → Rp25.000", value: "Rp1.500" },
        { label: "From", value: "Main Account" },
        { label: "Saved to", value: "Celengan Receh" },
      ],
      totalLabel: "Total out",
      totalValue: "Rp25.000",
      badge: "✓ ATOMIC · IDEMPOTENCY KEY 76367D01",
    },
    defs: [
      {
        term: "Round-up",
        text: "Rounded up to a multiple of Rp 5,000. The difference moves from the Main Account to the Spare Change Jar.",
      },
      {
        term: "Cascading",
        text: "Pocket short? The deficit comes from the Main Account. Rejected if the combined balance cannot cover payment + round-up.",
      },
      { term: "Deposit", text: "Locked until maturity: it cannot pay, be transferred, withdrawn or closed." },
      {
        term: "Interest",
        text: "`balance × rate ÷ 100 ÷ 365 × whole days` — rounded down, at most 365 days per run; the partial day carries over.",
      },
      {
        term: "Refund",
        text: "24 hours. Money returns to the Main Account, the original becomes `REVERSED` and the round-up stays saved. A second refund is rejected.",
      },
      {
        term: "Integrity",
        text: "`Long` balances, amounts must be > 0, and balances are re-read inside the transaction so a stale screen cannot overspend.",
      },
    ],
  },
  security: {
    eyebrow: "Security",
    title: "Five layers, honest about the limits",
    layers: [
      {
        title: "Screen",
        text: "`FLAG_SECURE` blocks screenshots and Recents. The app starts locked and relocks after 60 s in the background.",
      },
      {
        title: "Access",
        text: "A 6-digit PIN (PBKDF2, 120k iterations; weak PINs rejected), a 30 s lockout after 5 misses, and `CryptoObject`-bound biometrics.",
      },
      {
        title: "Data at rest",
        text: "SQLCipher AES-256 with a random passphrase wrapped by an Android Keystore key. Older unencrypted databases are converted automatically.",
      },
      {
        title: "Transactions",
        text: "Atomic, balances re-read, idempotency keys, and typed errors that never leak internals.",
      },
      {
        title: "Device",
        text: "No backups, a FileProvider limited to the statements folder, R8 enabled, and private lock-screen notifications.",
      },
    ],
    limitsTitle: "What is not there",
    limits: [
      "No backend or accounts; the device is the only source of truth.",
      "No root/emulator detection (Play Integrity).",
      "PIN-attempt counters live in regular preferences, so a rooted device could reset them.",
      "QRIS is structurally valid, but PocketPay is not a registered issuer.",
    ],
  },
  architecture: {
    eyebrow: "Architecture",
    title: "One direction, easy to test",
    sub: "Clean Architecture + MVI: the UI sends intents, the ViewModel owns a `StateFlow`, and the repository calls a DAO that runs the money rules atomically.",
    diagramAria: "Architecture diagram",
    decisions: [
      { title: "One Room transaction", text: "Every money operation is atomic; balances are re-read inside it." },
      { title: "Typed errors", text: "`BankingError` maps to bilingual messages." },
      { title: "Safe overlays", text: "Swallow touches and hide what is beneath from TalkBack." },
      { title: "Semantic colour", text: "Light/dark with no hex values in screens." },
    ],
    stackNote: "· target 36",
  },
  testing: {
    eyebrow: "Testing",
    title: "99 tests, including on a real device",
    sub: "Money rules, PIN, QRIS, statistics, the encrypted-database migration and PDF pagination. Every screen is also rendered to an image for design review.",
    bars: [
      { label: "Unit (JVM)", value: 76 },
      { label: "Functional on device", value: 8 },
      { label: "Screen renders", value: 15 },
    ],
    commentJvm: "# JVM",
    commentDevice: "# Keystore, SQLCipher, PDF, worker, screenshots",
  },
  start: {
    eyebrow: "Quick start",
    title: "Run it in minutes",
    tips: [
      "Needs Android Studio (JDK 17+) and an Android 8.0+ (API 26) device or emulator.",
      "On first launch create a 6-digit PIN (no repeats or sequences).",
      "On an emulator use **Enter manually** in the scanner since there is no camera.",
      "Seed data: a Rp 5,000,000 Main Account plus Jajan Harian and Deposito Darurat pockets.",
    ],
  },
  docs: {
    eyebrow: "Documentation",
    title: "Read further",
    items: [
      {
        file: "ARCHITECTURE.md",
        title: "Architecture",
        text: "Layers, packages, the MVI flow, migrations, background work.",
        href: `${repo}/blob/main/docs/ARCHITECTURE.md`,
      },
      {
        file: "FEATURES.md",
        title: "Features & rules",
        text: "Each feature with its formulas and limits.",
        href: `${repo}/blob/main/docs/FEATURES.md`,
      },
      {
        file: "SECURITY.md",
        title: "Security notes",
        text: "What is protected, how, and what is not.",
        href: `${repo}/blob/main/docs/SECURITY.md`,
      },
      {
        file: "TESTING.md",
        title: "Testing",
        text: "The test inventory, how to run it and the screenshot harness.",
        href: `${repo}/blob/main/docs/TESTING.md`,
      },
      { file: "README.md", title: "README", text: "Project overview and setup steps.", href: `${repo}#readme` },
    ],
    notBuilt: {
      title: "Not built yet",
      items: [
        "Transfers to other people, bill payments and external top-ups.",
        "Accounts/login and a sync backend.",
        "Categories, budgets and scheduled payments.",
      ],
    },
    next: {
      title: "Next up",
      items: [
        "Transfers to simulated contacts and a `PENDING` → success/fail flow.",
        "Dedicated use cases, ViewModel tests and GitHub Actions CI.",
      ],
    },
  },
  footer: { text: "A portfolio project with simulated data. MIT License." },
  copy: { idle: "Copy", done: "✓" },
  alts: {
    splash: "Splash screen",
    pin: "PIN lock screen",
    home: "Home with balance card and pockets",
    pay: "QRIS payment form",
    success: "Payment success page",
    stats: "Income and expense statistics",
    pockets: "Pockets list",
    manage: "Manage pocket sheet",
    receipt: "Transaction receipt with refund button",
    receive: "Receive QR code",
    history: "Transaction history",
    profile: "Profile and security status",
    statement: "E-statement export sheet",
  },
};

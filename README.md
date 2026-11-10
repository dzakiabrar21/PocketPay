# 💳 PocketPay — Digital Wallet & Micro-Savings (Android)

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF.svg?style=flat&logo=kotlin)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Android-API%2026%2B-3DDC84.svg?style=flat&logo=android)](https://developer.android.com)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20%2F%20Material3-4285F4.svg?style=flat&logo=android)](https://developer.android.com/jetpack/compose)
[![Architecture](https://img.shields.io/badge/Architecture-Clean%20%2B%20MVI-FF6F00.svg?style=flat)](https://developer.android.com/topic/architecture)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

**PocketPay** is a portfolio prototype of an Android digital wallet. Every feature really works on the device, but the money is **simulated** and there is no backend. It demonstrates fintech-style engineering: atomic money rules, an encrypted database, QRIS payments, round-up savings, refunds, background interest and a modern design system.

> **Not for real funds.** QRIS codes are structurally valid but PocketPay is not a registered QRIS issuer.

📖 **Landing page & docs:** [`docs/index.html`](docs/index.html) · [Architecture](docs/ARCHITECTURE.md) · [Features & rules](docs/FEATURES.md) · [Security notes](docs/SECURITY.md) · [Testing](docs/TESTING.md)

<p align="center">
  <img src="docs/assets/screens/home-en-light.webp" width="19%" alt="Home">
  <img src="docs/assets/screens/pay-en-light.webp" width="19%" alt="QRIS payment">
  <img src="docs/assets/screens/success-en-light.webp" width="19%" alt="Payment success">
  <img src="docs/assets/screens/stats-en-light.webp" width="19%" alt="Statistics">
  <img src="docs/assets/screens/home-en-dark.webp" width="19%" alt="Dark mode">
</p>

---

## 🌟 Key Features

### 1. 🔀 Smart Cascading Payment Routing
Eliminates payment drop-offs caused by insufficient pocket funds.
* **Multi-Source Funding**: If a spending pocket balance is less than the QRIS payment amount, the system automatically calculates the deficit and draws the remaining balance from the Primary Account.
* **Atomic Execution**: Executed within a single Room `@Transaction` block to guarantee financial data integrity.

### 2. 🐷 Round-Up Micro-Savings (Celengan Receh)
Integrated micro-savings mechanism inspired by modern digital banking.
* **Automatic Rounding**: Every QRIS payment with a non-round amount (e.g., Rp 23,500) is rounded up to the nearest Rp 5,000 threshold (Rp 25,000).
* **Automated Deposit**: The Rp 1,500 change is automatically transferred from the Primary Account into a dedicated 3.5% p.a. high-yield "Celengan Receh" spending pocket.

### 3. 🛡️ Fintech App Hardening & Security Shield
Comprehensive commercial banking security safeguards:
* **Screenshot & Screen Recording Blocker (`FLAG_SECURE`)**: Prevents screen capturing, recording, and malware screen-scraping.
* **Auto-Lock Shield**: The app starts locked and re-locks after 60 seconds in the background; unlocking needs the PIN or a biometric match.
* **Encrypted at rest**: The Room database is encrypted with SQLCipher (AES-256). Its random passphrase is wrapped by an Android Keystore key and never stored in clear. Older plaintext databases are converted in place on first launch.
* **Keystore-protected PIN**: 6-digit PIN stored as a salted PBKDF2 hash, additionally encrypted with a Keystore key; 5 wrong attempts trigger a 30 s lockout. Backups are disabled.
* **Crypto-bound biometrics**: `BiometricPrompt` uses a `CryptoObject` over a Keystore key that is invalidated when new biometrics are enrolled (re-armed only after the PIN is entered), with PIN fallback.
* **Privacy Toggle**: Instant balance masking (`Rp ••••••`) to prevent shoulder surfing.

### 4. 🔑 Idempotency Key & Payment State Machine
* **Duplicate Payment Prevention**: Every transaction generates a unique UUID-based `idempotencyKey` verified by the database prior to execution, blocking double-clicks or duplicate network retries.
* **Explicit Payment States**: Tracks transactions through explicit states (`SUCCESS`, `PENDING`, `REVERSED`).

### 5. 📄 Official E-Statement PDF Generator
* **Native PDF Engine**: Uses Android's `android.graphics.pdf.PdfDocument` API to generate multi-page A4 bank statements (all transactions, repeated headers, page numbers) without third-party dependencies.
* **Official Branding**: Features official PocketPay letterhead, security watermark, account balance summary, and mutation tables.
* **Sharing & Viewing**: Integrated with Android `FileProvider` for opening and sharing PDFs directly to WhatsApp, Google Drive, or Email.

### 6. 🧾 Digital Receipt Engine & Advanced Mutations
* **Official Mutation Receipts**: Tap any transaction to view a digital receipt with reference numbers, exact timestamps, authentication methods, and status badges.
* **Mutation Filtering & Search**: Real-time searching by merchant or reference number, combined with filters for transaction type (`DEBIT`, `CREDIT`, `REALLOCATION`) and date ranges (7 days, 30 days, all time).

### 7. 📷 QRIS Camera & Gallery Scanner
* **Live Camera Scanning**: Powered by CameraX and Google ML Kit Barcode Scanning.
* **Gallery Image Scanning**: Pick QR images from the device gallery for instant ML Kit decoding.

### 8. 🔲 Dynamic Receive QR Generator
* **QRIS (EMVCo) QR**: Receive-QR codes are real, scannable QR codes carrying a CRC-checked QRIS payload; the scanner parses/validates QRIS (CRC16, IDR, merchant, amount) and rejects anything else. PocketPay is not a registered QRIS issuer, so third-party wallets will not settle against it.
* **Localization & theming**: all UI text lives in `strings.xml` (Indonesian default) with a full English translation in `values-en`, kept in sync by a unit test. Light and dark themes follow the system setting.
* **Fintech design system**: deep-navy hero card with a lime accent, floating bottom navigation (Home, Statistics, Pockets, Profile), a PIN keypad for lock/setup/payment, full-screen payment flow (scan, confirm, PIN, receipt-style success page), "Scan me" receive screen, bottom sheets for secondary actions, and real statistics (weekly/monthly) computed from your transactions. Light and dark themes share one semantic token set (`ui/theme`, `ui/components`).

### 9. 👤 User Account Profile Management
* **Dedicated Account Page**: Dedicated full-screen page to view and update user profile information (Name, Phone, Email) and monitor security configuration.

---

## 🏗️ Architecture & Tech Stack

PocketPay is engineered using **Clean Architecture** paired with the **MVI (Model-View-Intent)** presentation pattern and **Unidirectional Data Flow (UDF)**.

```
com.example.pocketpay/
├── data/
│   ├── AppDatabase.kt          # Room database configuration & seeding
│   ├── BankingDao.kt           # Atomic Room DAO methods
│   ├── Entities.kt             # Room entities & enums
│   └── repository/             # Repository implementation
├── domain/
│   └── repository/             # Banking repository interface
├── presentation/
│   ├── home/                   # Home UI, ViewModel, MVI Contract
│   ├── scanner/                # CameraX & ML Kit QRIS Scanner
│   ├── security/               # BiometricHelper authentication
│   └── statement/              # Native PDF E-Statement Generator
├── di/                         # Hilt Dependency Injection modules
└── ui/theme/                   # Material 3 Emerald Green theme
```

### Libraries & Tools
* **Kotlin**: Core programming language.
* **Jetpack Compose**: Declarative UI framework with Material 3.
* **Room Database**: Local persistence with `@Transaction` atomic safety.
* **Dagger Hilt**: Dependency injection framework.
* **Kotlin Coroutines & Flow**: Reactive data streams (`StateFlow`, `Channel`).
* **CameraX & Google ML Kit**: Live camera barcode & image processing.
* **Biometric API**: System biometric prompt integration.
* **FileProvider**: Secure content URI file sharing.

---

## 🚀 Getting Started

### Prerequisites
* **Android Studio**: Ladybug / Jellyfish or newer.
* **JDK**: Version 17 or higher.
* **Android Device / Emulator**: Android 8.0 (API level 26) or above.

### Installation Steps
1. **Clone the Repository**:
   ```bash
   git clone https://github.com/YOUR_USERNAME/PocketPay.git
   cd PocketPay
   ```
2. **Open in Android Studio**:
   Open Android Studio and select `Open an existing project`, then navigate to the cloned `PocketPay` directory.
3. **Gradle Sync & Build**:
   Allow Gradle to download dependencies and sync the project structure.
4. **Run the Application**:
   Select your device or emulator, then press **Run 'app'** (`Shift + F10`).

---

## ✅ Tests

```bash
./gradlew :app:testDebugUnitTest          # JVM unit tests (Robolectric): DAO money logic, PIN, QRIS, profile
./gradlew :app:connectedDebugAndroidTest  # needs a device/emulator: Keystore, SQLCipher migration, PDF pagination
```

## 🔐 Security & Testing Credentials
* **Security PIN**: created by the user on first launch (6 digits, no repeated/sequential digits); can be changed from Account Profile → "Ganti PIN"
* **Default Primary Account Number**: `9012839102`
* **Initial Seed Balance**: `Rp 5,000,000`

---

## 📝 License
This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.

---

**Developed with ❤️ as a showcase of modern Android Fintech & Digital Banking Engineering.**

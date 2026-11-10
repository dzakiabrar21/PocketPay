# Security notes

PocketPay is a portfolio prototype with simulated money. These notes describe what the app does to protect the data on
the device, and — just as importantly — what it does not do.

## Implemented

| Area | Measure |
|---|---|
| Screen capture | `FLAG_SECURE` blocks screenshots, screen recording and Recents thumbnails |
| App lock | The app starts locked and locks again after 60 s in the background. Unlock with the PIN or a biometric |
| PIN | 6 digits. Repeated or sequential PINs are rejected. Stored as a salted PBKDF2-HMAC-SHA256 hash (120 000 iterations); the salt and hash are additionally encrypted with an AES-256-GCM key in the Android Keystore |
| Brute force | 5 wrong attempts lock PIN entry for 30 s, on the lock screen, at payment and when changing the PIN |
| Biometrics | `BIOMETRIC_STRONG` with a `CryptoObject`: success is accepted only if a Keystore key that requires biometric authentication actually performs a cipher operation. The key is invalidated when new biometrics are enrolled and re-created only after the PIN is entered |
| Data at rest | Room database encrypted with SQLCipher (AES-256). The random 32-byte passphrase is stored only as a Keystore-wrapped blob. Older plaintext databases are converted in place |
| Backups | `allowBackup="false"` and empty backup/extraction rules, so the database and preferences never leave the device |
| Files | `FileProvider` exposes only the generated statement folder |
| Notifications | Private on the lock screen; amounts are not shown until unlocked |
| Integrity of money | Atomic transactions, fresh balance reads, positive amounts only, idempotent payments, typed errors |
| Build | R8 minification and resource shrinking for release |

## Not implemented / limitations

* **No backend, no accounts.** The device is the only source of truth, so this cannot protect real funds.
* **No root, emulator or tamper detection** (no Play Integrity).
* **Attempt counters** are stored in regular preferences; on a rooted device they could be reset.
* **Biometric path** was verified by code review and on an emulator without enrolled biometrics, not on a physical device
  with a fingerprint.
* **QRIS** payloads are structurally valid but PocketPay is not a registered issuer.
* Stored transaction titles are in Indonesian regardless of the UI language.

## Reporting

This is a personal portfolio project. If you spot a problem, open an issue on the repository.

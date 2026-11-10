# Testing

```bash
./gradlew :app:testDebugUnitTest          # 76 JVM tests (Robolectric where Android classes are needed)
./gradlew :app:connectedDebugAndroidTest  # 23 tests on a device or emulator
```

## Unit tests (76)

| Area | What is verified |
|---|---|
| `BankingDaoTest` | Cascading payment, round-up, atomic rollback on insufficient funds, duplicate idempotency key, locked deposits, stale-object protection, top-up/withdraw/transfer/close rules, refunds (once only, 24 h window), daily interest (whole days, no double pay, per-pocket rate) |
| `PinManagerTest` | Weak PIN rejection, hash not stored in clear, attempt counter, lockout, change PIN, tampered or legacy stores treated as "no PIN" |
| `QrisTest` | CRC-16 check value, build/parse round trip, tampered payload, wrong CRC, non-IDR currency, legacy ad-hoc format rejected, length limits |
| `StatisticsTest` | Weekly/monthly buckets, exclusion of reversed/refund/internal entries |
| `MilestonesTest` | Deposit matured and goal reached are detected once |
| `ErrorMessagesTest` | Every `BankingError` has a distinct message in Indonesian and English; the DAO raises the right code |
| `StringsParityTest` | Both languages have the same keys and format arguments |
| `ProfileStoreTest`, `ThousandsTransformationTest` | Persistence and amount formatting with cursor mapping |

## Instrumented tests (23)

| Test | What is verified on a real device |
|---|---|
| `SecurityInstrumentedTest` | Keystore cipher round trip and tamper rejection; PIN with the real Keystore; plaintext database → encrypted with data intact; encrypted file cannot be opened without the key; PDF with 100 transactions paginates |
| `MaintenanceWorkerTest` | The real worker runs against the encrypted database through Hilt, twice; the daily job is scheduled |
| `ThemeScreenshotTest` (15) | Renders every screen in Indonesian and English, light and dark, and writes PNGs |

### Why a screenshot test?

The app window uses `FLAG_SECURE`, so system screenshots are black. The harness captures each composable directly,
which sidesteps `FLAG_SECURE` and gives reproducible screenshots for design review (and for the landing page).

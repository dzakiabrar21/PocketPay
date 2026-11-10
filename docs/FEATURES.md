# Features and rules

All behaviour below is implemented and covered by tests unless noted. Money is simulated.

## Pockets

| Type | Rate | Behaviour |
|---|---|---|
| Spending pocket | 3.5 % p.a. | Can pay QRIS, receive transfers, top up, withdraw and close. Optional savings goal with a progress bar |
| Locked deposit | 6.0 % p.a. | Term of 1, 3 or 6 months (30 days each). Cannot pay, transfer, withdraw or close until maturity |
| Spare Change Jar | 3.5 % p.a. | Created automatically by the first round-up |

New pockets are funded from the Main Account (initial allocation is debited atomically).

## QRIS payment

1. **Scan** with the camera or pick a QR image from the gallery. Only structurally valid QRIS is accepted
   (format indicator, CRC-16, IDR currency, merchant name). Static QR codes leave the amount for you to type.
2. **Choose the source**: the Main Account or a spending pocket.
3. **Smart cascading**: if the pocket is short, the deficit is taken from the Main Account in the same transaction.
   The payment is rejected if the combined balance cannot cover the payment plus the round-up.
4. **Round-up**: the amount is rounded up to the next Rp 5,000; the difference moves from the Main Account into the
   Spare Change Jar. It can be switched off per payment.
5. **Confirm** with biometrics or the 6-digit PIN.
6. A receipt-style success page shows merchant, source of funds, round-up, reference and time.

## Refunds and transaction status

A successful payment can be refunded for 24 hours. The money returns to the Main Account, the original becomes
`REVERSED` (struck through in the list) and a `+` "Refund" entry is recorded. A second refund is rejected. The round-up
that accompanied the payment stays in the savings pocket by design.

## Interest

Simple daily interest: `balance × rate ÷ 100 ÷ 365 × whole days`, rounded down with `BigDecimal`, at most 365 days per
run. Only whole days are consumed, so a partial day carries over and the same day is never paid twice. Interest is
credited when the app opens and by the daily background job, and appears as a "Bunga …" credit.

## Statistics

Weekly (7 days) and monthly (6 months) bars of money in and out, built from real transactions. Refunds, reversed
payments and internal moves between pockets are excluded so nothing is counted twice. Tap a bar to see that day or month.

## Other features

* **Receive** — a scannable QRIS code with a logo and an optional amount. PocketPay is not a registered QRIS issuer, so
  third-party wallets will not settle against it.
* **E-statement PDF** — multi-page A4 statement with letterhead, watermark, summary and all transactions; open or share it.
* **History** — search plus filters by type (Expense, Income, Transfer) and period (all, 7 days, 30 days).
* **Notifications** — matured deposit, savings goal reached, interest credited (permission requested once).
* **Languages and themes** — Indonesian and English; light and dark follow the system.
* **Profile** — editable name, phone and email (persisted) and live security status.

## Not implemented

Transfers to other people, bill payments and top-ups from outside, accounts/login, categories and budgets, scheduled
payments, and a backend.

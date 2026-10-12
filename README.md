# Zelu

A private, personal expense and income tracker for Android. It reads your
bank and UPI SMS directly on the phone, categorises spending and income, and
keeps everything local — nothing is uploaded.

Native Android app written in Kotlin with Jetpack Compose.

## What's here

- `app/src/main/java/com/nagesh/zelu/`
  - `Parser.kt` — turns bank/UPI SMS into transactions (amount, debit/credit, date, counterparty, reference).
  - `Classifier.kt` — income / expense / transfer / refund, with keyword categories.
  - `Ingest.kt` — filters by your bank senders, parses, classifies and stores.
  - `Storage.kt` — settings in SharedPreferences, transactions as JSON in app-private storage.
  - `Summary.kt` — monthly totals and expenses by category.
  - `SmsReader.kt` — reads the SMS inbox.
  - `SmsReceiver.kt` — captures new SMS as they arrive, even when the app is closed.
  - `MainActivity.kt` — the Compose UI.

## Build

GitHub Actions builds a release APK on every push (`.github/workflows/build-apk.yml`).
The APK is signed with the debug key so it can be sideloaded.

## Permissions

`READ_SMS` and `RECEIVE_SMS`, used only on-device to read transaction alerts.

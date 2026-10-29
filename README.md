# Zelu

A private, personal expense and income tracker for Android. It reads your
bank and UPI SMS directly on the phone, categorises spending and income, and
keeps everything local — nothing is uploaded.

Native Android app written in Kotlin with Jetpack Compose.

## What's here

`app/src/main/java/com/koolicream/zelu/`

- Reading messages
  - `SmsReader.kt` — reads the SMS inbox.
  - `SmsReceiver.kt` — captures new SMS as they arrive, even when the app is closed.
  - `Parser.kt` — turns bank/UPI SMS into transactions (amount, debit/credit, date, payee, account, reference),
    bill reminders, AutoPay mandates and balances.
  - `Classifier.kt`, `Brands.kt` — income / expense / transfer / refund, and the category.
  - `Ingest.kt` — sender and link checks, parse, classify, store; `rebuild` re-reads everything after a parser change.
  - `Rebuild.kt` — keeps payments whose SMS has left the inbox when everything is read again.
  - `Taught.kt` — message shapes you taught from the Missed SMS list.
- Working out what to show
  - `Reconcile.kt` — joins one account written several ways, counts repeated notices once, pairs refunds with their
    spends and finds transfers between your own accounts.
  - `AccountBook.kt` — the Your accounts screen: which labels are one account, cards, loans.
  - `Analytics.kt` — totals by month, category, day; dates.
- Storage
  - `Storage.kt` — settings in SharedPreferences; payments and your decisions in the database (`Db.kt`, Room/SQLite):
    payments in paise, one-value-per-key decisions as rows (`kv`), other small documents whole (`doc`). Any of the
    files earlier versions kept, found in app storage (from an update, or written by a restored backup), is taken into
    the database and kept as `<name>.imported`; backups and undo still use those file names and contents. Payments are
    worked out in three cached stages, each redone only when something it reads changes.
  - `SafeFile.kt` — atomic saves that keep the previous version, and reads that fall back to it.
  - `Backup.kt`, `BackupCrypto.kt` — password-locked backup files in Downloads/Zelu.
- Screens
  - `MainActivity.kt` — the activity, app lock, and `ZeluApp`: start-up, onboarding, and the home pages.
  - `AppState.kt` — the main screen's ViewModel: data read from storage, the pages open over Home (a typed back
    stack), dialogs and filters, and the actions that change them; where you are is in its saved state.
  - `MenuDialog.kt`, `MoneyDialogs.kt`, `MessageDialogs.kt`, `SettingsDialogs.kt`, `Dialogs.kt` — dialogs.
  - `LedgerView.kt`, `SpendViews.kt`, `TrendsScreen.kt`, `PayeesView.kt`, `RemindersView.kt`, `CardScreens.kt`,
    `AccountsScreens.kt`, `Onboarding.kt` — pages; `UiKit.kt`, `Visuals.kt` — shared pieces.
- Background: `DueReminders.kt`, `DailySummary.kt`, `WeeklySummary.kt` (daily alarms set through `Alarms.kt`),
  `Notifier.kt`, `ZeluWidget.kt`.

## Tests

`app/src/test` runs on the JVM with every build (parser, classifier, reconcile, the 200-message set). `app/src/androidTest`
runs on an emulator (`.github/workflows/device-tests.yml`, on pull requests to main and by hand): the database migration,
taking in older files, and a screen. Lint runs weekly (`lint.yml`).

## Build

`./gradlew testReleaseUnitTest assembleRelease` with the Android SDK installed; the wrapper pins the Gradle version CI uses.

GitHub Actions builds, runs the unit tests and checks the APK asks for no internet access on every push
(`.github/workflows/build-apk.yml`). Release builds are signed with the upload key kept in the repository secrets;
builds from `main` are published as a GitHub release. A build goes to Play only when started by hand with the upload
ticked: internal testing (any branch, no review) or closed testing (main only); see `docs/play-upload.md`.

## Permissions

`READ_SMS` and `RECEIVE_SMS`, used only on-device to read transaction alerts. `POST_NOTIFICATIONS` for payment and
bill alerts, `RECEIVE_BOOT_COMPLETED` to put the daily checks back after a restart. `USE_BIOMETRIC` (added by the
biometric library) for the app lock's fingerprint / face prompt. `WRITE_EXTERNAL_STORAGE` on Android 7-9 only, asked for
when you save a backup or CSV to Downloads/Zelu. No internet permission.

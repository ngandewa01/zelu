# Zelu (React Native, Android)

A personal expense and income tracker that reads your bank and UPI SMS directly
from the phone. New alerts are captured automatically, including when the app is closed.

## What's here

| Path | Purpose |
| --- | --- |
| `App.tsx` | Main screen: settings, import, monthly totals, categories, recent transactions |
| `src/parser.ts` | Parses debit and credit SMS (amount, date, direction, UPI ref, counterparty) |
| `src/classify.ts` | Assigns income / expense / transfer / refund and a category |
| `src/ingest.ts` | Filters by bank sender, parses, classifies, stores (deduplicated) |
| `src/storage.ts` | AsyncStorage persistence for transactions and settings |
| `src/summary.ts` | Monthly totals and expenses by category |
| `src/sms.ts` | Bridge to the native SMS module and permission requests |
| `android-overlay/` | Kotlin native module, SMS receiver, package registration, manifest |

SMS access has to be native on Android. Everything else is TypeScript.

## Setup

Requirements: Node 18+, JDK 17, Android Studio with an Android SDK, and an Android phone with USB debugging on.

1. Create the React Native project (this generates the `android/` folder):

   ```bash
   npx @react-native-community/cli init WalnutApp --version 0.74.5
   cd WalnutApp
   ```

2. Copy this project's `App.tsx`, `src/`, and `package.json` over the generated files.
   Then install the storage package:

   ```bash
   npm install
   npm install @react-native-async-storage/async-storage@1.23.1
   ```

3. Copy the Kotlin files into the generated project:

   ```
   android-overlay/app/src/main/java/com/example/walnutapp/*.kt
     -> android/app/src/main/java/com/example/walnutapp/
   ```

   If your generated package name differs from `com.example.walnutapp`, change the
   `package` line in each `.kt` file to match.

4. Merge `android-overlay/app/src/main/AndroidManifest.xml` into
   `android/app/src/main/AndroidManifest.xml`. The key additions are the two SMS
   permissions and the `SmsReceiver` entry.

5. Register the package in `android/app/src/main/java/com/example/walnutapp/MainApplication.kt`:

   ```kotlin
   override fun getPackages(): List<ReactPackage> =
       PackageList(this).packages.apply {
           add(SmsPackage())
       }
   ```

6. Build and run on the phone:

   ```bash
   npx react-native run-android
   ```

7. On first launch, enter your bank sender IDs (for example `HDFCBK, SBIINB`) and
   your own UPI IDs. Tap **Import past SMS**, then grant the permissions. New
   alerts after that are picked up automatically when you open the app.

## Notes and limits

- **Find your bank's sender ID.** Open a bank SMS on the phone and read the sender
  name at the top. Enter it in the sender field. Without this, personal messages
  containing "sent" and an amount would be counted.
- **Refund, transfer, and income accuracy depends on the rules.** Money from friends
  arrives as a credit and is counted as income. Add their UPI IDs or names to
  "your own accounts" if you want it excluded.
- **Categories are keyword-based** (see `src/classify.ts`). Extend the lists to match your merchants.
- **Phone and OEM settings.** Some phone makers (Xiaomi, OnePlus, Samsung) restrict
  background receivers. Exclude the app from battery optimization so SMS capture is reliable.
- **Google Play.** Play restricts SMS permissions to default SMS apps. This is built
  as a personal APK, not for the Play Store.
- **Not compiled here.** This code is written but has not been built or run in this
  environment, because the Android SDK isn't installed here. Expect to fix small issues
  on the first build. Share the error output and I'll fix it.
- **Data stays on the phone.** Nothing is uploaded anywhere.

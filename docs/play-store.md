# Zelu on Google Play: what to fill in

Everything to paste into the Play Console, in the order the Console asks for it. Text in code blocks is ready to
paste. Check each field's character limit in the Console; the counts here were right when written.

Privacy policy URL (used in several places): **https://ngandewa01.github.io/zelu-privacy/**

---

## 1. Create the app

| Field | Answer |
|---|---|
| App name | `Zelu: Expense Tracker` |
| Default language | English (India) – en-IN |
| App or game | App |
| Free or paid | Free |
| Declarations | Tick both (developer policies, US export laws) |

When asked about **app signing**, choose **Let Google manage and protect your app signing key** (the default).
Upload `zelu.aab` from the latest release; it is signed with your upload key (`58:D3:37…A8:DA`).

---

## 2. SMS permissions declaration

*Play Console → App content → Sensitive permissions → SMS and Call Log permissions.* It appears once an `.aab` asking
for READ_SMS / RECEIVE_SMS has been uploaded to any track.

**Core functionality to select:** **SMS-based money management**. Google lists this as a use case that may get an
exception (budgeting apps that read bank SMS). Read the live policy page linked from the form before submitting; if
the option has a different name now, pick the one about money management or budgeting from SMS.

**Permissions:** READ_SMS and RECEIVE_SMS only. Zelu does not ask for SEND_SMS, MMS or call-log permissions.

### "Describe how your app uses the permissions" (core functionality)

```
Zelu is a personal expense tracker for India. Its core feature is reading the transaction SMS that banks, credit
cards and UPI apps send for every payment, and turning them into a spending record automatically: amount, payee,
account, date and category. Without SMS access the app has nothing to show; this is the only way the user's
payments reach the app.

READ_SMS: on first launch, Zelu reads the existing inbox once to build the spending history, and the user can
rescan later. RECEIVE_SMS: new bank alerts are recorded as they arrive, so the user gets a payment alert and an
up-to-date balance.

Zelu looks only at messages from banks, cards, UPI apps and billers, and skips one-time passwords and personal
chats. All processing happens on the phone. The app has no internet permission, no ads, no analytics and no
account or server, so SMS content never leaves the device. Zelu does not send, delete or change any message.
```

### "Why can't an alternative be used?"

```
Indian banks do not offer a consumer API that a budgeting app can use to read a person's transactions across all
their banks and cards. The account aggregator framework needs a regulated licence and is not supported by many
banks, rural (Gramin) banks and co-operative banks. Manual entry defeats the purpose of automatic tracking. Bank
SMS is the one source every Indian bank sends for every debit and credit, so reading it is the only way to
provide this feature to all users.
```

### Demo video (required)

Upload to YouTube as **Unlisted** and paste the link. 1–2 minutes, screen recording on a real phone. Hide account
numbers and balances if you record with real messages, or record the app part with sample data (see Graphics).

| Time | Show | Say / on-screen text |
|---|---|---|
| 0:00 | Install, open Zelu | "Zelu tracks spending from bank SMS." |
| 0:10 | Onboarding screen "Why Zelu asks for SMS" | Pause on it: this is the prominent disclosure |
| 0:20 | Tap **Allow both**, the Android SMS permission dialog, tap Allow | "The user grants SMS access" |
| 0:30 | First scan finishing; Home with payments | "Existing bank messages become a spending record" |
| 0:45 | Open a payment: amount, payee, account, category | "Read from this bank SMS" (show the SMS in Messages side by side if possible) |
| 1:00 | A new bank SMS arrives; Zelu's payment notification; it appears in the list | "RECEIVE_SMS: new payments are recorded as they arrive" |
| 1:20 | Menu → Help → Privacy | "No internet, no ads, data stays on the phone" |
| 1:30 | Android Settings → Apps → Zelu → Permissions | Show only SMS and Notifications are granted |

---

## 3. Data safety

*App content → Data safety.* Zelu has no internet permission, so the app itself sends nothing anywhere. The only
thing that can leave the phone is an email the user writes and sends from their own mail app ("Report a problem"
and the crash prompt).

| Question | Answer |
|---|---|
| Does your app collect or share any of the required user data types? | **Yes** (only because of the user-sent problem report; see below) |
| Is all of the user data collected by your app encrypted in transit? | **Yes**: reports go through the user's own email app over its secure connection |
| Do you provide a way for users to request that their data is deleted? | **Yes**: users can ask by email; in-app data is erased with Menu → Backup and security → Erase everything, or by uninstalling |

Data types to tick, **collected, not shared, optional, for App functionality**:

- **App info and performance → Crash logs** (crash details, if the user ticks them in the report)
- **App info and performance → Diagnostics** (app version, phone model, counts, if ticked)
- **Messages → Other in-app messages** (the user's own problem description, and up to 3 unreadable bank messages
  with long numbers and UPI names hidden, only if the user ticks "Messages Zelu couldn't read")

For each: "Is this data processed ephemerally?" **No**. "Is collection required or optional?" **Optional (users can
choose)**. Purpose: **App functionality**. Not used for advertising, not shared.

If you'd rather declare **No data collected**: that is defensible because reports are user-initiated emails sent
from another app, but the conservative answer above avoids a mismatch finding in review.

---

## 4. Other App content forms

| Form | Answer |
|---|---|
| Privacy policy | https://ngandewa01.github.io/zelu-privacy/ |
| Ads | **No, my app does not contain ads** |
| App access | **All functionality is available without special access** (no login). Add a note: "Needs SMS permission; the app reads bank SMS on the device. No account or login." |
| Content rating | Fill the questionnaire: category **Utility, productivity, communication or other**; answer No to violence, sexual content, language, controlled substances, gambling; **No** to user interaction / sharing location / digital purchases. Expected rating: **Everyone / 3+** |
| Target audience | **18 and over** only (finance app; keeps it out of the Families policy) |
| News app | No |
| COVID-19 tracing | No |
| Government app | No |
| Financial features | Zelu tracks spending; it does not move money, lend, invest, or hold funds. If the form lists a budgeting / personal finance management option, pick that; otherwise select that the app **does not provide** the listed financial features. No licences needed. |
| Health | No |

---

## 5. Store listing

**Category:** Finance. **Tags:** Budget, Expense tracker, Personal finance. **Contact email:** ngandewar01@gmail.com.

### App name (30)

```
Zelu: Expense Tracker
```

### Short description (80)

```
Tracks spending from your bank SMS. Private: no internet, no ads, no login.
```

### Full description (4000)

```
Zelu turns the payment texts your bank already sends you into a clear picture of where your money goes. No typing,
no linking bank accounts, no login.

AUTOMATIC
• Reads debit, credit card and UPI alerts from 100+ Indian banks, including rural (Gramin) and co-operative banks
• Picks out the amount, who you paid, which account and the date
• Sorts spending into categories like Food, Groceries, Bills, Fuel and Travel, and learns your payees
• Spots refunds, card bill payments and transfers between your own accounts so they are not counted as spending
• Foreign currency card spends are marked and estimated in rupees

SEE YOUR MONEY
• Today, this week and this month at a glance, with a nightly "spent today" summary
• Spending by category, by payee and by day, with trends over time
• Credit cards with spend per billing cycle
• Budgets per category: each payment alert shows how much is left
• Bill and EMI reminders from your bank's due-date messages

PRIVATE BY DESIGN
• Zelu has no internet permission: it cannot send your messages or payments anywhere
• No ads, no analytics, no tracking, no account
• Everything is processed and stored on your phone
• It skips OTPs and personal chats
• Back up to a password-locked file you control; erase everything any time

Zelu needs SMS permission because bank SMS is the only record every Indian bank sends for every payment. It never
sends, changes or deletes a message.

Problems or a bank Zelu misses? Menu → Report a problem opens an email you can check before sending.
```

Avoid adding words like "best", "#1" or long lists of bank names: Play treats those as keyword spam.

### Graphics

| Asset | Spec | Plan |
|---|---|---|
| App icon | 512×512 PNG, 32-bit | Export the Zelu launcher icon at 512 px |
| Feature graphic | 1024×500 PNG/JPG | Zelu logo + "Your bank SMS, sorted. Privately." on the brand colour |
| Phone screenshots | 2–8, 9:16, at least 1080 px wide recommended | See below |

Screenshots (in this order), each with a short caption bar at the top:

1. Home: "Spending tracked from your bank SMS"
2. Spend tab, categories donut: "See where your money goes"
3. Card detail with billing-cycle spend: "Every credit card, cycle by cycle"
4. Reminders: "Bills and EMIs, before they're due"
5. Trends: "Month by month"
6. Privacy screen: "No internet. No ads. Stays on your phone."

**Never use your real data in screenshots or the video**: names, account digits, balances and payees would be
public. Turn on sample data first: **Menu → Help → Advanced → Show sample data**. Zelu then shows two months of
made-up payments; your own data is untouched. Switch back with **Menu → Help → Sample data is on**.

---

## 6. Testing before production

New personal developer accounts must run a **closed test with at least 12 testers opted in for 14 days in a row**
before production access opens; organisation accounts with a D-U-N-S number are exempt. Third-party guides say
Google also checks that testers really used the app.

1. Testing → Closed testing → Create track → upload `zelu.aab`.
2. Add testers by email (a Google Group is easiest) and share the opt-in link.
3. Keep at least 12 opted in for 14 days; ask them to open Zelu every few days and use Report a problem for anything wrong.
4. Then: Production → Apply for production access, answering the questions about the test.

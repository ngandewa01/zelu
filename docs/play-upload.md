# Automatic upload to Google Play

Once this is set up, every build of `main` is uploaded to the **Closed testing (Alpha)** track by GitHub. It is not sent
for review by itself: it waits in **Play Console → Publishing overview**, and you press **Send changes for review** when
you want testers to get it. Release notes come from `distribution/whatsnew/whatsnew-en-US` (500 characters at most).

One-time setup, about 15 minutes. The key file you make in step 4 works like a password: keep it private and delete
the downloaded copy after step 6.

## A. Google Cloud: a robot account allowed to upload

Sign in at https://console.cloud.google.com with the business Gmail (koolicream@gmail.com).

1. Top bar → project picker → **New project**. Name: `zelu-publish` → **Create**. Make sure it is selected.
2. Search bar → **Google Play Android Developer API** → **Enable**.
3. Menu → **IAM & Admin → Service accounts** → **Create service account**. Name: `zelu-upload` →
   **Create and continue** → skip the roles → **Done**.
4. Click the new account → **Keys** tab → **Add key → Create new key → JSON → Create**. A `.json` file downloads.
   Copy the account's email; it ends with `.iam.gserviceaccount.com`.

## B. Play Console: let that account upload Zelu

5. Play Console (the developer account, not the app) → **Users and permissions** → **Invite new users**.
   - Email: the `...iam.gserviceaccount.com` address from step 4.
   - **App permissions → Add app → Zelu: Expense Tracker**, tick **Release apps to testing tracks** (leave production off).
   - **Invite user**.

## C. GitHub: give the key to the build

6. https://github.com/ngandewa01/zelu/settings/secrets/actions → **New repository secret**.
   Name: `PLAY_SERVICE_ACCOUNT_JSON`. Value: open the `.json` file in a text editor and paste all of it. **Add secret**.

The next build of `main` uploads by itself. Google can take up to a day to activate a new account's permission; until
then the upload step fails with a permission error and the build's APK and AAB are still published on GitHub.

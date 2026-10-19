# Sponsor card

Zelu can show one small "Sponsored" card at the bottom of Home. There is no ad network: the card text and link come from a JSON file you host.

1. Host `sponsor.json` at a public https address (a public GitHub Gist "raw" link works).
2. Put that address in `gradle.properties` as `zelu.sponsorUrl=...` (or set `ZELU_SPONSOR_URL` in the build).
3. Edit the file any time. Phones pick it up within about 12 hours. No new app version is needed.

Fields per card: `id` (change it for each new sponsor, so people who hid the last one see the new one), `enabled`, `title` (max 60 characters), `body` (max 140), `cta` (button text, max 24), `url` (https only), `from` and `until` (yyyy-MM-dd, optional).

Zelu sends no identifier, cookie or personal data when it downloads the file. The card is hidden for the first 7 days after install, never shown if the file cannot be read, and can be hidden by the user.

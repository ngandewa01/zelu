package com.nagesh.zelu

// The privacy policy shown in Menu, then Help. Short points, not paragraphs. Keep it in step with the published policy page.
object PrivacyText {
    const val UPDATED = "19 October 2026"
    const val SUMMARY = "Your messages and payments stay on your phone. Zelu goes online only to fetch one small sponsor card, and sends nothing about you."

    class Section(val title: String, val points: List<String>)

    /** The three things people most want to know, shown as tiles at the top. */
    val facts: List<Pair<String, String>> = listOf(
        "On your phone" to "Nothing is uploaded",
        "Nothing sent" to "Online only for the sponsor card",
        "No tracking" to "No ads or analytics",
    )

    val sections: List<Section> = listOf(
        Section("What Zelu reads", listOf(
            "Asks to read and receive SMS.",
            "Looks only at messages from banks, cards, UPI apps and billers.",
            "Picks out the amount, payee, account and due date.",
            "Skips one-time passwords and personal chats.",
        )),
        Section("Where your data is kept", listOf(
            "Payments, categories, budgets and settings sit in the app's private storage on your phone.",
            "No account, no login, no server.",
            "No analytics, ad network or tracking tools. Nothing about you is shared with anyone.",
        )),
        Section("What leaves your phone, only when you choose", listOf(
            "Backup: saved to Downloads/Zelu, locked with a password you set. Zelu cannot recover a forgotten password.",
            "CSV export: a plain file in Downloads/Zelu. Delete it in your Files app when done.",
            "Report a problem: opens an email draft. You see everything in it and send it yourself. Long numbers and UPI names are hidden first.",
        )),
        Section("Sponsor card", listOf(
            "Home may show one card marked Sponsored, a few days after you start using Zelu.",
            "To get it, Zelu downloads a small text file. No account, ID, cookie, message or payment is sent with that request.",
            "Zelu has no ad network and no tracking. Tapping the card opens the sponsor's website in your browser. That site has its own privacy policy.",
            "Tap Hide on the card to remove it.",
        )),
        Section("Notifications", listOf(
            "Payment alerts, daily and weekly summaries, bill reminders.",
            "Made on your phone. Switch off in Menu, then Notifications.",
        )),
        Section("Deleting your data", listOf(
            "Menu, then Backup and security, then Erase everything clears all Zelu data.",
            "Uninstalling the app does the same.",
            "Your original text messages are never changed or deleted.",
            "Backup and export files already in Downloads/Zelu stay until you delete them.",
        )),
        Section("Children", listOf("Zelu is not directed at children under 13.")),
        Section("Changes to this policy", listOf("A new version will appear here with a new date.")),
    )
}

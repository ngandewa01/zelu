package com.nagesh.zelu

// The privacy policy shown in Menu, then Help. Short points, not paragraphs. Keep it in step with the published policy page.
object PrivacyText {
    const val UPDATED = "20 October 2026"
    const val SUMMARY = "Your messages and payments stay on your phone. Zelu has no ads and no internet access, so it cannot send them anywhere."

    class Section(val title: String, val points: List<String>)

    /** The three things people most want to know, shown as tiles at the top. */
    val facts: List<Pair<String, String>> = listOf(
        "On your phone" to "Your data stays here",
        "No internet" to "No ads, no tracking",
        "No login" to "No account, no server",
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
            "Your messages and payments are shared with no one.",
        )),
        Section("What leaves your phone, only when you choose", listOf(
            "Backup: saved to Downloads/Zelu, locked with a password you set. Zelu cannot recover a forgotten password.",
            "CSV export: a plain file in Downloads/Zelu. Delete it in your Files app when done.",
            "Report a problem: opens an email draft. You see everything in it and send it yourself. Long numbers and UPI names are hidden first.",
        )),
        Section("No ads, no internet", listOf(
            "Zelu shows no ads and contains no ad, analytics or tracking code.",
            "Zelu does not ask for internet access, so it has no way to send anything off your phone by itself.",
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

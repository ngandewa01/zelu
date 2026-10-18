package com.nagesh.zelu

// The privacy policy shown in Menu, then Help. Keep it in step with the published policy page.
object PrivacyText {
    const val UPDATED = "18 October 2026"
    const val SUMMARY = "Zelu reads your bank text messages on your phone to track your money. Your messages and payments never leave your phone, because Zelu has no internet permission."

    val sections: List<Pair<String, String>> = listOf(
        "What Zelu reads" to "Zelu asks for permission to read and receive SMS. It uses this only to find messages from banks, cards, UPI apps and billers, and to work out the amount, the payee, the account and any due date. Zelu skips one-time passwords and personal chats.",
        "Where your data is kept" to "Everything Zelu works out (payments, categories, budgets, settings) is stored in the app's private storage on your phone. Zelu has no account, no login and no server. It does not use analytics, advertising or tracking tools, and it does not share data with anyone.",
        "What leaves your phone, and only when you choose" to "Backups: when you tap Back up now, or switch on the weekly backup, Zelu saves a file to Downloads/Zelu. Backups are locked with a password you set. Zelu cannot recover a forgotten password.\n\nCSV export: the export is a plain file in Downloads/Zelu. Delete it from your Files app if you no longer need it.\n\nProblem reports: Report a problem opens an email draft in your mail app. You see exactly what is in it and send it yourself. Long numbers and UPI names in message text are hidden first.",
        "Notifications" to "Zelu can show payment alerts, daily and weekly summaries and bill reminders. They are made on your phone and can be switched off in Menu, then Notifications.",
        "Deleting your data" to "Menu, then Backup and security, then Erase everything removes all Zelu data from your phone. Uninstalling the app does the same. Your original text messages are never changed or deleted by Zelu. Backup and export files already saved in Downloads/Zelu stay until you delete them.",
        "Children" to "Zelu is not directed at children under 13.",
        "Changes to this policy" to "If this policy changes, the new version will appear here with a new date.",
    )
}

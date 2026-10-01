# Λογαριασμοί (Bills)

Υπενθυμίσεις πληρωμής λογαριασμών για Android, φτιαγμένες για την Ελλάδα.
Δωρεάν, χωρίς login, χωρίς διαφημίσεις, χωρίς internet: όλα τα δεδομένα μένουν στο κινητό σου.

A bill-payment reminder app for Android, built for Greece. Free, no account, no ads, no
internet access: all data stays on your device. Greek and English UI.

## Features

- 80+ Greek providers and subscriptions (electricity, gas, water, mobile/internet/TV, banks,
  insurance, taxes and tolls, streaming and other subscriptions), plus custom bills
- Any repeat schedule: every N days / weeks / months / years, until a date or for N payments
- Multiple reminders per bill (on the day, 1–14 days before, or any number of days), with
  "Paid", "Later" and "Copy RF" actions on the notification
- Daily nudges for overdue bills
- "Paid" tracking with the actual amount and date, full payment history
- RF payment code, account number, direct-debit flag and notes per bill
- Month overview, calendar, yearly summary by category
- Home-screen widget
- Export / import to a JSON file and automatic backup to a folder of your choice
- Light, dark and pitch-black themes, four launcher icons

## Privacy

The app has no internet permission. Nothing is collected or sent anywhere. Backups are plain
JSON files that you create and control.

## Building

Requirements: JDK 17 or newer and the Android SDK (API 36).

```bash
./gradlew testDebugUnitTest      # unit tests
./gradlew assembleRelease        # APK  -> app/build/outputs/apk/release/
./gradlew bundleRelease          # AAB for Google Play -> app/build/outputs/bundle/release/
```

Release builds are signed with `keystore/logariasmoi.jks` if `keystore/keystore.properties`
exists (both are git-ignored); otherwise the debug key is used.

## Project layout

| Path | What it is |
| --- | --- |
| `app/src/main/java/gr/logariasmoi/data` | Models, recurrence logic, JSON storage, formatting, translations |
| `app/src/main/java/gr/logariasmoi/notify` | Alarm scheduling, notifications, boot receiver |
| `app/src/main/java/gr/logariasmoi/ui` | Jetpack Compose screens |
| `app/src/main/java/gr/logariasmoi/widget` | Home-screen widget |
| `app/src/test` | Unit tests for recurrence, reminders, formatting |
| `tools/providers.tsv` | The provider list; run `tools/gen_providers.ps1` after editing it |

### Adding a provider

Add a line to `tools/providers.tsv` (tab separated: id, Greek name, category, website, colour,
optional English name) and run `tools/gen_providers.ps1`. Brand logos are deliberately not
bundled; companies are shown as a coloured tile with their initials.

### Translations

UI texts are written inline as `tr("Ελληνικά", "English")` (see `data/I18n.kt`).

## Disclaimer

Company and product names belong to their respective owners. This app is not affiliated with,
or endorsed by, any of them.

## License

Licensed under the [GNU General Public License v3.0](LICENSE). You may use, study, share and
modify this app, provided that any version you distribute stays open source under the same
license.

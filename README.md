# Anime Black Android

Native Android app built with Kotlin and Jetpack Compose. It combines the Supabase-backed account flow with a social anime community: personalized feed, posts and polls, stories, communities, direct messages, short videos, profiles, watchlist tracking, achievements, avatar frames, search, and privacy settings.

## Supabase project

The app uses the existing `anime-black-fresh` project (`kpowibropzeapdfpkhqo`) and only the public publishable key. The `service_role` key is never included in the Android app.

- Email/password authentication and confirmation/recovery callbacks use `animeblack://auth-callback`.
- Phone/SMS and Google OAuth are not configured in this Supabase project and remain clearly disabled in the app.
- The social screens use the project's existing public tables and owner-scoped RLS policies. Post publishing and avatar-frame selection use authenticated RPCs.
- `supabase/migrations/` includes local copies of the social migrations recorded on the hosted project.

Supabase's default email sender is still in use. It is suitable for initial testing but may have delivery/rate limits; configure a custom SMTP provider before wider testing or production.

For local builds, add these lines to the ignored `local.properties` file (keep the Android SDK path already written there):

```properties
SUPABASE_URL=https://kpowibropzeapdfpkhqo.supabase.co
SUPABASE_PUBLISHABLE_KEY=your_supabase_publishable_key
```

## Version 0.3.3

This test build adds a Supabase-backed personal library, public profiles with recent posts, story navigation, notification deep links, quote suggestions with admin approval and audit history, and official anime trailer/site links when those URLs exist in the catalog. The launcher icon keeps its solid black background and the app retains its Luxury Dark styling. The APK is debug-signed and intended for testing, not Play Store distribution.

## Build and download

Open the repository in Android Studio (JDK 17, Android SDK 35), or run:

```bash
./gradlew assembleDebug
```

The installable debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`. GitHub Actions builds the app on pushes to the Arena working branch and publishes tagged milestone releases.

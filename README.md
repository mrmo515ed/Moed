# Anime Black Android

Native Android app in Kotlin + Jetpack Compose. This milestone completes the account/onboarding flow from the supplied design: welcome, method chooser, email sign-up and verification, sign-in, password recovery/reset, phone/OTP screens, profile setup/avatar upload, genre interests, success, policies, offline/session-expired/restricted states, and sign-out.

## Supabase project

The app is connected to the existing healthy project `anime-black-fresh` (`kpowibropzeapdfpkhqo`); no duplicate project was created. It uses only the public publishable key. The `service_role` key is never included in the Android app.

- Email/password authentication is enabled.
- Email auto-confirm has been turned off so new accounts must follow the confirmation link before continuing. This does not unverify existing accounts.
- The confirmation/recovery redirect is `animeblack://auth-callback`.
- The existing `handle_new_user` trigger creates each new user's profile and default gamification records.
- A `profile_genre_interests` table and `replace_my_genre_interests` RPC were added with owner-only RLS policies.
- The existing public `avatars` bucket and owner-only storage policies are used for profile pictures.
- Phone/SMS and Google OAuth are not enabled/configured in this Supabase project. Their screens are present and clearly marked as requiring provider setup; they are not represented as working sign-in methods until configured.

Supabase's default email sender is still in use. It is suitable for initial testing but may have delivery/rate limits; configure a custom SMTP provider before wider testing or production.

For local builds, add these lines to the ignored `local.properties` file (keep the Android SDK path already written there):

```properties
SUPABASE_URL=https://kpowibropzeapdfpkhqo.supabase.co
SUPABASE_PUBLISHABLE_KEY=your_supabase_publishable_key
```

## Build and download

Open the repository in Android Studio (JDK 17, Android SDK 35), or run:

```bash
./gradlew assembleDebug
```

The installable debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`. GitHub Actions builds it on pushes to the Arena working branch; tagged milestones are published as GitHub releases.

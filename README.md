# Anime Black Android

Native Android starter for the Anime Black app, written in Kotlin + Jetpack Compose. The first milestone ports the welcome, email sign-in, account creation, password recovery, and reset-password flow from the supplied HTML design and connects those actions to Supabase Auth.

## Supabase

The app is configured for the existing healthy Supabase project `anime-black-fresh` (`kpowibropzeapdfpkhqo`); no duplicate project was created. Auth uses only the project's **publishable** key. The `service_role` / secret key is never used in the Android app.

For local builds, add these lines to the ignored `local.properties` file (keep the Android SDK path already written there):

```properties
SUPABASE_URL=https://kpowibropzeapdfpkhqo.supabase.co
SUPABASE_PUBLISHABLE_KEY=your_supabase_publishable_key
```

The registration request sends `display_name` and `username` as Supabase user metadata. The existing `handle_new_user` trigger creates the related profile and default gamification rows. Password-reset links use the `animeblack://auth-callback` deep link.

Email/password auth is enabled in the existing project. Phone/SMS auth and Google OAuth are not enabled there yet, so those options are intentionally not presented as working features in this first milestone.

## Build

Open the repository in Android Studio (JDK 17, Android SDK 35), or run:

```bash
./gradlew assembleDebug
```

The installable debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`. A GitHub Actions workflow builds this APK on pushes to the Arena working branch and publishes it as a downloadable build artifact.

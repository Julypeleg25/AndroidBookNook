# BookNook (Android)

## Goal
Search books via Google Books API, create public review posts (rating + review + optional image), view others' posts, like/comment, manage wishlist, and edit profile.

## Architecture (Course Style / GoalGuru Reference)
- Single Activity (MainActivity) + Fragments
- Navigation Graph (SafeArgs enabled)
- MVVM-ish with a central `Model` singleton orchestrating:
  - Room cache (required)
  - Firebase Auth + Firestore + Storage (remote DB + images)
  - Google Books REST API (required)

## Requirements coverage
- Remote DB read/write text + images: Firestore + Storage
- Social interaction: posts feed + like + comments
- External REST API: Google Books volumes endpoint
- Local cache: Room tables (posts, user, wishlist, cached_books)
- No synchronous network calls: coroutines + loading indicators

## Setup
1. Create Firebase project + Android app package `com.booknook.app`
2. Download `google-services.json` into `app/google-services.json` (replace placeholder)
3. Enable Firebase Auth (Email/Password), Firestore, Storage
4. Run

Notes:
- For SafeArgs generation, ensure `androidx.navigation.safeargs.kotlin` plugin is applied in `app/build.gradle`.

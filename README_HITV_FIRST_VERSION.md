# AWR Stream Hub - HiTV API Version

Implemented in this version:
- HiTV-style home with banner and rails.
- Live API loading for Anime through Jikan v4:
  - https://api.jikan.moe/v4/top/anime
  - https://api.jikan.moe/v4/anime?q=...
- Consumet route wired for Movies and K-Drama search:
  - https://api.consumet.org/movies/flixhq/{query}
- API results are merged with local fallback data, so the app still works if an external source is down.
- Cover images from live APIs are rendered with Coil.
- DataStore keeps favorites, history and continue-watching progress without accounts.
- Details page, episode list, in-app player, resume, next episode, subtitle selector and AI Translation entry point are preserved.

Important:
Consumet public hosts can change or go offline. If that happens, change consumetBase in MainActivity.kt to your own deployed Consumet-compatible server.

Open this folder in Android Studio and run:
:app:assembleDebug

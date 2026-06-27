# AWR Stream Hub - HiTV First Version

This output contains the edited Android Jetpack Compose project.

Implemented:
- HiTV-style home with a large banner.
- Trending, Popular Anime, Movies, K-Drama, Continue Watching and Recently Added rails.
- No-account local library using DataStore Preferences.
- Favorites, History and saved watch progress.
- Details page with cover area, title, rating, genres, story, episode count and episode list.
- In-app player using Android VideoView.
- Resume position, next episode action and subtitle selector.
- AI Translation entry point showing the planned pipeline: audio -> Grok ASR -> OpenAI Translation -> SRT -> video subtitles.
- API placeholders for Jikan and Consumet integration.

Open this folder in Android Studio:
outputs/AWR_Stream_Hub_HiTV_First_Version

Build task:
:app:assembleDebug

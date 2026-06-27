# API Plan

- Catalog pages: `/data/catalog/movies.json`, `/data/catalog/kdrama.json`, `/data/catalog/anime.json`
- Request queue: `/data/requests.json`
- Subtitle jobs: `/data/subtitle_jobs.json`
- Subtitle outputs: `/data/subtitles/<media_id>/ar.srt`, `/en.srt`

The Android frontend currently ships with sample data. The next backend step is to load these JSON URLs from GitHub Raw.

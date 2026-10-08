# Manga Heroes AI gateway

The Android app never contains the OpenRouter/provider API key. `ai_server.py` reads it from `server/.env`, calls the OpenRouter-compatible chat-completions endpoint, and returns a short response to the app.

## Local run

1. Copy `.env.example` to `.env`, put the provider key and a long random `MANGA_HEROES_AI_TOKEN` in that server-only file, then restrict it (`chmod 600 .env`).
2. Run `set -a; . ./.env; set +a; python3 ai_server.py` from the repository root with `cd server`.
3. Build the app with `MANGA_HEROES_AI_BASE_URL=https://your-host MANGA_HEROES_AI_TOKEN=<same gateway token> ./gradlew :app:assembleRelease` from the repository root (configure local signing as described in the root README). The shared app token is compiled into the APK and can be extracted; it only gates the prototype gateway and is not the provider key. For production, replace it with user/session authentication, quotas, abuse monitoring, and a permanent TLS endpoint.

## Guardrails

The gateway only exposes a fixed recommendation/chat operation, pins one configured model, caps prompt size and completion length, limits each IP to 8 calls/hour and the service to 100 calls/day in-process, suppresses prompts and credentials from logs, and does not forward provider error bodies. Limits reset when this prototype process restarts. Keep `.env` out of Git and backups shared with clients.

#!/usr/bin/env python3
"""Small, dependency-free AI gateway. Provider credentials stay server-side."""
from __future__ import annotations

import json
import os
import threading
import time
from collections import defaultdict, deque
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen

PORT = int(os.environ.get("PORT", "8765"))
MODEL = os.environ.get("OPENROUTER_MODEL", "google/gemini-2.5-flash")
API_KEY = os.environ.get("OPENROUTER_API_KEY", "").strip()
APP_TOKEN = os.environ.get("MANGA_HEROES_AI_TOKEN", "").strip()
if not API_KEY or not APP_TOKEN:
    raise SystemExit("Missing server-side AI configuration; set secrets in server/.env")

_lock = threading.Lock()
_hourly: dict[str, deque[float]] = defaultdict(deque)
_daily: deque[float] = deque()


def _admit(client: str) -> bool:
    now = time.time()
    with _lock:
        per_ip = _hourly[client]
        while per_ip and now - per_ip[0] > 3600:
            per_ip.popleft()
        while _daily and now - _daily[0] > 86400:
            _daily.popleft()
        if len(per_ip) >= 8 or len(_daily) >= 100:
            return False
        per_ip.append(now)
        _daily.append(now)
        return True


class Handler(BaseHTTPRequestHandler):
    server_version = "MangaHeroesGateway/1.0"

    def log_message(self, fmt: str, *args: object) -> None:
        # Never log request bodies, prompts, authorization headers, or provider details.
        print(f"{self.log_date_time_string()} {self.address_string()} {fmt % args}", flush=True)

    def _json(self, status: int, payload: dict) -> None:
        body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Cache-Control", "no-store")
        self.send_header("X-Content-Type-Options", "nosniff")
        self.end_headers()
        self.wfile.write(body)

    def do_GET(self) -> None:
        if self.path == "/health":
            self._json(200, {"status": "ok", "model": MODEL, "api_key_in_app": False})
        else:
            self._json(404, {"error": "not_found"})

    def do_POST(self) -> None:
        if self.path != "/v1/recommend":
            self._json(404, {"error": "not_found"})
            return
        if self.headers.get("Authorization", "") != f"Bearer {APP_TOKEN}":
            self._json(401, {"error": "unauthorized"})
            return
        try:
            size = int(self.headers.get("Content-Length", "0"))
            if size < 2 or size > 4096:
                self._json(413, {"error": "request_size_limit"})
                return
            data = json.loads(self.rfile.read(size))
            prompt = str(data.get("prompt", "")).strip()
            if not prompt or len(prompt) > 600:
                self._json(400, {"error": "prompt_must_be_1_to_600_characters"})
                return
            genres = data.get("genres", [])
            if not isinstance(genres, list):
                genres = []
            genres = [str(x)[:32] for x in genres[:8]]
            language = "Arabic" if str(data.get("language", "")).lower().startswith("ar") else "English"
            username = str(data.get("username", "Reader"))[:32]
        except (ValueError, TypeError, json.JSONDecodeError):
            self._json(400, {"error": "invalid_json"})
            return
        if not _admit(self.client_address[0]):
            self._json(429, {"error": "rate_limit_reached"})
            return

        system = (
            "You are Manga Heroes, a thoughtful manga/manhwa/webtoon discovery assistant. "
            f"Reply in {language}. Be concise, friendly, and useful. The reader is named {username}. "
            f"Their chosen genres are: {', '.join(genres) if genres else 'not specified'}. "
            "Give suggestions as ideas, never claim a title exists in an installed source or is available unless the user provided that fact. "
            "Do not request credentials, do not provide pirated download links, and acknowledge uncertainty."
        )
        payload = json.dumps({
            "model": MODEL,
            "messages": [
                {"role": "system", "content": system},
                {"role": "user", "content": prompt},
            ],
            "temperature": 0.55,
            "max_tokens": 320,
            "stream": False,
        }).encode("utf-8")
        request = Request(
            "https://openrouter.ai/api/v1/chat/completions",
            data=payload,
            headers={
                "Authorization": f"Bearer {API_KEY}",
                "Content-Type": "application/json",
                "HTTP-Referer": "https://manga-heroes.app",
                "X-OpenRouter-Title": "Manga Heroes",
            },
            method="POST",
        )
        try:
            with urlopen(request, timeout=45) as response:
                result = json.loads(response.read(1_000_000))
            answer = result.get("choices", [{}])[0].get("message", {}).get("content", "")
            if not isinstance(answer, str) or not answer.strip():
                self._json(502, {"error": "empty_model_response"})
                return
            self._json(200, {"answer": answer.strip()[:6000], "model": MODEL})
        except HTTPError as error:
            # Provider messages can contain account details; return only the status category.
            code = error.code
            self._json(503 if code >= 500 else 502, {"error": "model_provider_error", "provider_status": code})
        except (TimeoutError, URLError, OSError, json.JSONDecodeError):
            self._json(503, {"error": "model_temporarily_unavailable"})


if __name__ == "__main__":
    print(f"Manga Heroes AI gateway listening on 0.0.0.0:{PORT}; model={MODEL}", flush=True)
    ThreadingHTTPServer(("0.0.0.0", PORT), Handler).serve_forever()

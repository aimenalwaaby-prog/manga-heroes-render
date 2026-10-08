# Source and attribution notices

Manga Heroes is a modified derivative of Mihon. It is not an official Mihon build and is not developed or endorsed by Mihon.

- Upstream: https://github.com/mihonapp/mihon
- Upstream documentation: https://mihon.app/docs/
- Upstream license: Apache License 2.0 (see the retained `LICENSE` file)
- Original copyright notices in upstream files are preserved. Added files are copyright Manga Heroes contributors and provided under Apache-2.0 unless a file states otherwise.
- Third-party library and asset notices remain governed by their respective licenses. Consult the upstream dependency/license inventory before redistributing a built APK.

The Android AI client contains only the gateway endpoint and its gateway access token when configured at build time. The OpenRouter provider API key is read only by `server/ai_server.py` from the server's private environment and must never be committed or placed in Android resources, source code, Gradle files, logs, or releases.

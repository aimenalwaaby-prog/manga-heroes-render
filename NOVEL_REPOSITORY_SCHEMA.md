# Manga Heroes novel repository bridge

Manga Heroes keeps the existing Mihon extension repository flow for manga. Novel sources can be declared by the same repository index as a JSON-only `novelSources` array.

Each entry uses:

```json
{
  "id": "example-novel",
  "name": "Example Novel Source",
  "lang": "en",
  "homeUrl": "https://example.com",
  "searchUrl": "https://example.com/search?q={query}&page={page}",
  "searchItemSelector": ".novel-card",
  "titleSelector": ".title",
  "urlSelector": "a",
  "chapterListSelector": ".chapter",
  "chapterNameSelector": ".name",
  "chapterUrlSelector": "a",
  "contentSelector": ".chapter-content",
  "contentWarning": "SAFE"
}
```

The bridge is declarative: Manga Heroes fetches HTML and applies the selectors. It does not execute repository-provided application code. Only entries explicitly marked `SAFE` are exposed by the novel UI.

This does not pretend that an arbitrary LNReader JavaScript plugin is a Mihon extension. LNReader plugins use a separate JavaScript plugin API and manifest format, so executing those plugins would require a dedicated runtime/adapter rather than silently treating their files as Android source APKs.

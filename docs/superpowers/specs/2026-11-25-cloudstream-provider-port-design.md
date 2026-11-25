# Design: Port Vega providers to CloudStream plugins

Date: 2026-11-25
Status: Approved for spec writing; implementation plan pending.

## Goal

Make a selected subset of the Vega providers from
`https://github.com/Zenda-Cross/vega-providers/tree/main/providers` usable in
CloudStream by porting them into native CloudStream plugin modules.

Target repository: `https://github.com/R3vll/TestPlugins` (fork of
`recloudstream/TestPlugins`), cloned locally at `C:\Users\admin\TestPlugins`.

## Scope and boundary

- Source repo contains ~70 provider folders; `manifest.json` enables ~45.
- Target feature parity per provider is **MVP**: CloudStream `search()`,
  `load()`, and `loadLinks()` only. Home/catalog pages, settings schema,
  `kvStore`, WebView/WAF solving, and DRM are out of scope.
- Validation is **GitHub Actions CI only**: compile all modules and publish
  artifacts. No on-device playback testing is assumed.
- Only providers that retrieve public or authorized content will be ported.
  Providers whose primary purpose is unauthorized access to copyrighted media
  are marked `blocked` in `providers.yaml` and are not implemented.
- This spec's first implementation plan covers the framework only: repository
  setup, core mapping utilities and tests, `providers.yaml`, CI, and
  `repository.json`. YouTube and Internet Archive already exist upstream in
  `recloudstream/extensions`, so they are not recreated. Individual provider
  ports are handled in follow-up plans once authorized providers are nominated.

## Approach

Manual Kotlin ports in a forked `TestPlugins` repo, with a shared utility layer
and CI-generated `plugins.json`. Alternatives considered and rejected:

- Embedding a JS runtime to load Vega bundles unchanged: large compatibility
  surface, sandbox/performance risk, and would run blocked providers unchanged.
- Auto-generating Kotlin from Vega TypeScript: scrapers are imperative and
  site-specific; generated code still needs manual repair.

## Architecture

- Local clone uses branch `master` initially; rename to `main` during
  implementation and set GitHub default branch to `main` via `gh repo edit`.
- A `builds` branch holds generated `.cs3` files and `plugins.json`, matching
  the template's GitHub Actions workflow.
- One Gradle module per ported provider, e.g. `YouTubeProvider/`. Each module
  contains a `MainAPI` subclass and per-provider metadata in
  `build.gradle.kts` (`description`, `authors`, `status`, `tvTypes`, `language`,
  `iconUrl`).
- Shared utilities live in a `core` source set/module: HTTP helpers using
  NiceHttp, base-URL/mirror resolution, HTML parsing with Jsoup, and mapping
  from Vega `Stream` to CloudStream `ExtractorLink`/`SubtitleLink`. If the
  CloudStream Gradle plugin does not bundle `core` into each `.cs3`, fall back
  to shared source files included directly in each provider module.
- A `providers.yaml` file records every Vega provider ID with status
  `ported`, `skipped`, or `blocked` plus a short reason. Only `ported`
  providers become Gradle modules.
- A `repository.json` file on `main` contains:
  `{ "name": ..., "description": ..., "manifestVersion": 1,
    "pluginLists": ["https://raw.githubusercontent.com/R3vll/TestPlugins/builds/plugins.json"] }`.
  Users add this single URL in CloudStream.

## Data flow and API mapping

| Vega | CloudStream | MVP treatment |
| --- | --- | --- |
| `getSearchPosts(query, page)` | `search(query)` -> `SearchResponse` | Implement |
| `getMeta(link)` | `load(url)` -> `MovieLoadResponse` / `TvSeriesLoadResponse` | Implement |
| `getEpisodes(url)` | episode list built inside `load()` | Implement |
| `getStream(link, type)` | `loadLinks()` -> `ExtractorLink` + `SubtitleFile` | Implement |
| `catalog.ts` / `getPosts(filter, page)` | `mainPageOf(...)` + `getMainPage()` | Skip |
| `settings.ts` / `kvStore` | CloudStream plugin settings | Skip |
| `openWebView` WAF solving | no plugin API equivalent | Skip |
| `Stream.drm` | CloudStream extractor data | Skip |

Field mapping: Vega `Stream.server/link/type/quality/subtitles` become
`ExtractorLink(name, url, quality, ExtractorLinkType.M3U8/VIDEO, source)` and
`SubtitleFile`. Vega `getBaseUrl` mirror resolution becomes a cached Kotlin
helper using NiceHttp.

Error handling: Vega providers often catch errors and return empty arrays;
ported providers instead throw `ErrorLoadingException` with a readable message
so CloudStream can display it.

## Validation and release

- GitHub Actions on push to `main`: checkout source and `builds`, set up JDK 17
  and Android SDK, run `./gradlew make makePluginsJson`, copy `.cs3` files and
  `plugins.json` into `builds`, commit amended and force-push.
- Add a CI step that fails if any provider module fails to compile.
- Add JUnit tests for pure mapping helpers using fixture JSON derived from
  Vega provider responses.
- Dependency versions stay pinned to the template, especially Jackson 2.13.1,
  for CloudStream compatibility.
- Release is the `builds` branch plus `repository.json`; users install
  individual plugins from the CloudStream app.

## Risks

- Third-party sites change markup/APIs; scrapers will need ongoing maintenance.
- CI-only validation proves compilation, not playback.
- CloudStream version skew: use the same pre-release stubs as the template.

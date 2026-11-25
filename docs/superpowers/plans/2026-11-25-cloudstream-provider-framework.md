# CloudStream Provider Port Framework Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use subagent-driven-development (recommended) or executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Set up the `R3vll/TestPlugins` fork so authorized Vega providers can be ported as native CloudStream plugins, with shared mapping utilities, a scope registry, CI, and a publishable repository manifest.

**Architecture:** The repo is a fork of `recloudstream/TestPlugins`; each future provider will be a Gradle module with a `MainAPI` subclass. A plain JVM `core` module holds Vega to CloudStream mapping helpers and their tests; its Kotlin sources are also injected into every plugin module's Android source set so no runtime dependency packaging is needed. GitHub Actions tests `core`, builds all plugins, and publishes `.cs3` files plus `plugins.json` to the `builds` branch.

**Tech Stack:** Kotlin 2.1.0, Gradle 8.7.3 Android plugin, JDK 17, CloudStream pre-release stubs (`com.lagradot:cloudstream3:pre-release`), NiceHttp, Jsoup, Jackson 2.13.1, JUnit 4, GitHub Actions.

**Scope note:** This plan builds the framework only. Individual provider ports are follow-up plans. YouTube and Internet Archive are intentionally not recreated because `recloudstream/extensions` already ships them.

---

### Task 1: Normalize branches on the fork

**Files:**
- Modify: git branch state in `C:/Users/admin/TestPlugins`

- [ ] **Step 1: Rename the working branch to `main`**

Run:
```bash
cd C:/Users/admin/TestPlugins
git branch -m master main
git push -u origin main
```
Expected: `main` is pushed and tracks `origin/main`.

- [ ] **Step 2: Set the GitHub default branch and remove the old branch**

Run:
```bash
gh repo edit R3vll/TestPlugins --default-branch main
git push origin --delete master
```
Expected: `gh` prints the updated repository; GitHub no longer has a `master` branch.

- [ ] **Step 3: Confirm the `builds` branch exists locally**

Run:
```bash
git fetch origin
git branch builds origin/builds
```
Expected: `git branch --list` shows `builds`.

- [ ] **Step 4: Commit nothing for this task**

No files changed; proceed to Task 2.

---

### Task 2: Add the CloudStream repository manifest

**Files:**
- Create: `repository.json`

- [ ] **Step 1: Create `repository.json`**

Write exactly:
```json
{
  "name": "Vega Ports by R3vll",
  "description": "Native CloudStream ports of authorized Vega providers",
  "manifestVersion": 1,
  "pluginLists": [
    "https://raw.githubusercontent.com/R3vll/TestPlugins/builds/plugins.json"
  ]
}
```

- [ ] **Step 2: Validate the JSON**

Run:
```bash
python -m json.tool repository.json
```
Expected: formatted JSON is printed with no error.

- [ ] **Step 3: Commit**

```bash
git add repository.json
git commit -m "build: add CloudStream repository manifest"
```

---

### Task 3: Add the provider scope registry

**Files:**
- Create: `scripts/generate-providers-yaml.py`
- Create: `providers.yaml`

- [ ] **Step 1: Write the generator script**

Create `scripts/generate-providers-yaml.py` with exactly:
```python
#!/usr/bin/env python3
"""Generate providers.yaml from the Vega providers manifest."""
import json
import urllib.request
from pathlib import Path

MANIFEST_URL = (
    "https://raw.githubusercontent.com/Zenda-Cross/vega-providers/main/manifest.json"
)
OUTPUT = Path(__file__).resolve().parents[1] / "providers.yaml"

with urllib.request.urlopen(MANIFEST_URL, timeout=30) as response:
    entries = json.load(response)

lines = ["providers:"]
for entry in entries:
    lines.append(f"  - id: {entry['value']}")
    lines.append(f"    display_name: {entry.get('display_name', entry['value'])}")
    lines.append("    status: blocked")
    lines.append("    reason: Not yet reviewed against authorization policy")

OUTPUT.write_text("\n".join(lines) + "\n", encoding="utf-8")
print(f"Wrote {len(entries)} providers to {OUTPUT}")
```

- [ ] **Step 2: Run the generator**

Run:
```bash
python scripts/generate-providers-yaml.py
```
Expected output ends with `Wrote 70 providers to ...providers.yaml` (count may change upstream).

- [ ] **Step 3: Verify the generated file has no empty ids**

Run:
```bash
findstr /R /C:"id: $" providers.yaml
```
Expected: no output.

- [ ] **Step 4: Commit**

```bash
git add scripts/generate-providers-yaml.py providers.yaml
git commit -m "chore: add provider scope registry"
```

---

### Task 4: Add the shared Vega to CloudStream mapping utilities and tests

**Files:**
- Modify: `build.gradle.kts`
- Create: `core/build.gradle.kts`
- Create: `core/src/main/kotlin/com/r3vll/vegaport/VegaStream.kt`
- Create: `core/src/main/kotlin/com/r3vll/vegaport/StreamMapper.kt`
- Create: `core/src/test/kotlin/com/r3vll/vegaport/StreamMapperTest.kt`

- [ ] **Step 1: Exclude `core` from CloudStream plugin configuration**

In `build.gradle.kts`, replace the line:
```kotlin
subprojects {
```
with:
```kotlin
subprojects {
    if (project.name == "core") return@subprojects
```
Expected: the rest of the `subprojects` block is unchanged.

- [ ] **Step 2: Create `core/build.gradle.kts`**

Write exactly:
```kotlin
plugins {
    kotlin("jvm")
}

repositories {
    google()
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("com.lagradot:cloudstream3:pre-release")
    testImplementation(kotlin("test"))
    testImplementation("junit:junit:4.13.2")
}

kotlin {
    jvmToolchain(17)
}
```

- [ ] **Step 3: Create the Vega data model**

Create `core/src/main/kotlin/com/r3vll/vegaport/VegaStream.kt` with exactly:
```kotlin
package com.r3vll.vegaport

data class VegaSubtitle(
    val language: String? = null,
    val uri: String? = null,
    val type: String? = null,
    val title: String? = null,
)

data class VegaStream(
    val server: String? = null,
    val link: String? = null,
    val type: String? = null,
    val quality: String? = null,
    val subtitles: List<VegaSubtitle> = emptyList(),
)
```

- [ ] **Step 4: Write the failing tests first**

Create `core/src/test/kotlin/com/r3vll/vegaport/StreamMapperTest.kt` with exactly:
```kotlin
package com.r3vll.vegaport

import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.Qualities
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamMapperTest {
    @Test
    fun `maps m3u8 stream with 1080 quality and server name`() {
        val link = mapStreams(
            source = "Vega",
            streams = listOf(
                VegaStream(
                    server = "Server A",
                    link = "https://example.com/master.m3u8",
                    type = "m3u8",
                    quality = "1080",
                ),
            ),
        ).single()

        assertEquals("Vega", link.source)
        assertEquals("Server A", link.name)
        assertEquals("https://example.com/master.m3u8", link.url)
        assertEquals(ExtractorLinkType.M3U8, link.type)
        assertEquals(Qualities.P1080.value, link.quality)
    }

    @Test
    fun `maps quality with p suffix and 4k alias`() {
        assertEquals(Qualities.P720.value, mapQuality("720p"))
        assertEquals(Qualities.P2160.value, mapQuality("4K"))
        assertEquals(Qualities.Unknown.value, mapQuality("ultra"))
    }

    @Test
    fun `drops streams with blank links`() {
        val links = mapStreams(
            source = "Vega",
            streams = listOf(VegaStream(server = "Broken", link = "  ")),
        )
        assertTrue(links.isEmpty())
    }

    @Test
    fun `maps subtitles using language prefix and ignores blank urls`() {
        val subs = mapSubtitles(
            VegaStream(
                subtitles = listOf(
                    VegaSubtitle(language = "en-US", uri = "https://example.com/en.vtt"),
                    VegaSubtitle(title = "Français", uri = "https://example.com/fr.vtt"),
                    VegaSubtitle(language = "de", uri = "  "),
                ),
            ),
        )

        assertEquals(2, subs.size)
        assertEquals("en", subs[0].lang)
        assertEquals("https://example.com/en.vtt", subs[0].url)
        assertEquals("fr", subs[1].lang)
    }
}
```

- [ ] **Step 5: Run the tests to verify they fail**

Run:
```bash
cd C:/Users/admin/TestPlugins
.\gradlew :core:test
```
Expected: FAIL with unresolved references `mapStreams`, `mapQuality`, `mapSubtitles`.

- [ ] **Step 6: Write the implementation**

Create `core/src/main/kotlin/com/r3vll/vegaport/StreamMapper.kt` with exactly:
```kotlin
package com.r3vll.vegaport

import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.newSubtitleFile
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.Qualities

fun mapType(type: String?): ExtractorLinkType = when (type?.trim()?.lowercase()) {
    "m3u8" -> ExtractorLinkType.M3U8
    "dash", "mpd" -> ExtractorLinkType.DASH
    else -> ExtractorLinkType.VIDEO
}

fun mapQuality(quality: String?): Int {
    val cleaned = quality?.trim()?.lowercase()?.removeSuffix("p")
        ?: return Qualities.Unknown.value
    return when (cleaned) {
        "4k", "2160" -> Qualities.P2160.value
        "1440" -> Qualities.P1440.value
        "1080" -> Qualities.P1080.value
        "720" -> Qualities.P720.value
        "480" -> Qualities.P480.value
        "360" -> Qualities.P360.value
        "240" -> Qualities.P240.value
        "144" -> Qualities.P144.value
        else -> cleaned.toIntOrNull() ?: Qualities.Unknown.value
    }
}

fun mapSubtitles(stream: VegaStream): List<SubtitleFile> =
    stream.subtitles.mapNotNull { subtitle ->
        val url = subtitle.uri?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
        val language = (subtitle.language ?: subtitle.title ?: "en").take(2)
        newSubtitleFile(lang = language, url = url)
    }

fun mapStreams(
    source: String,
    streams: List<VegaStream>,
    referer: String = "",
): List<ExtractorLink> = streams.mapNotNull { stream ->
    val url = stream.link?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
    ExtractorLink(
        source = source,
        name = stream.server?.takeIf { it.isNotBlank() } ?: source,
        url = url,
        referer = referer,
        quality = mapQuality(stream.quality),
        type = mapType(stream.type),
    )
}
```

- [ ] **Step 7: Run the tests to verify they pass**

Run:
```bash
.\gradlew :core:test
```
Expected: PASS with 4 tests.

- [ ] **Step 8: Commit**

```bash
git add build.gradle.kts core
git commit -m "feat: add Vega stream mapping helpers and tests"
```

---

### Task 5: Inject shared sources into future plugin modules

**Files:**
- Modify: `build.gradle.kts`

- [ ] **Step 1: Add the shared source directory inside the `subprojects` block**

In `build.gradle.kts`, inside the `subprojects { ... }` block, immediately after the three `apply(plugin = ...)` lines, add:
```kotlin
        android.sourceSets.getByName("main").java.srcDir(
            rootProject.file("core/src/main/kotlin")
        )
```
Expected: every future plugin module can import `com.r3vll.vegaport.*` without an extra Gradle dependency.

- [ ] **Step 2: Verify existing plugins still build**

Run:
```bash
.\gradlew make
```
Expected: BUILD SUCCESSFUL; `ExampleProvider.cs3` is produced under `ExampleProvider/build/`.

- [ ] **Step 3: Commit**

```bash
git add build.gradle.kts
git commit -m "build: share mapping sources with plugin modules"
```

---

### Task 6: Add core tests to CI

**Files:**
- Modify: `.github/workflows/build.yml`

- [ ] **Step 1: Insert a test step before plugin build**

In `.github/workflows/build.yml`, replace:
```yaml
      - name: Build Plugins
        run: |
```
with:
```yaml
      - name: Test core mapping helpers
        run: |
          cd $GITHUB_WORKSPACE/src
          chmod +x gradlew
          ./gradlew :core:test

      - name: Build Plugins
        run: |
```
Expected: CI runs `:core:test` before `make makePluginsJson`.

- [ ] **Step 2: Verify the YAML parses**

Run:
```bash
python -c "import yaml,pathlib; yaml.safe_load(pathlib.Path('.github/workflows/build.yml').read_text(encoding='utf-8')); print('ok')"
```
Expected: `ok`.

- [ ] **Step 3: Commit**

```bash
git add .github/workflows/build.yml
git commit -m "ci: run core mapping tests before plugin build"
```

---

### Task 7: Document the framework for future provider work

**Files:**
- Modify: `README.md`

- [ ] **Step 1: Append the framework section**

Append exactly:
```markdown
## Vega provider port framework

This fork is the build home for native CloudStream ports of authorized Vega providers.

- `providers.yaml` is the scope registry. A provider must be marked `ported` before a Gradle module is created for it.
- `core/src/main/kotlin/com/r3vll/vegaport` contains Vega `Stream` to CloudStream `ExtractorLink` / `SubtitleFile` mapping helpers. These sources are injected into every plugin module.
- Each port is its own Gradle module with a `MainAPI` subclass. The MVP implements `search()`, `load()`, and `loadLinks()`.
- CI runs `:core:test`, builds all modules, and publishes `.cs3` files plus `plugins.json` to the `builds` branch.
- Add this repository to CloudStream using the URL in `repository.json`.
```

- [ ] **Step 2: Commit**

```bash
git add README.md
git commit -m "docs: describe Vega port framework"
```

---

### Task 8: Push and verify the remote state

**Files:**
- No files; git remote state only.

- [ ] **Step 1: Push all commits**

Run:
```bash
git push origin main
git push origin builds
```
Expected: both branches are up to date.

- [ ] **Step 2: Confirm CI workflow is present on the remote**

Run:
```bash
gh api repos/R3vll/TestPlugins/contents/.github/workflows/build.yml --jq '.name'
```
Expected: `build.yml`.

- [ ] **Step 3: Confirm repository manifest is served from `main`**

Run:
```bash
gh api repos/R3vll/TestPlugins/contents/repository.json --jq '.name'
```
Expected: `repository.json`.

---

## Self-review checklist

- [ ] Spec coverage: Task 2 covers `repository.json`; Task 3 covers `providers.yaml`; Tasks 4-5 cover `core` utilities and injection; Task 6 covers CI; Task 7 documents scope; Task 8 covers publishing.
- [ ] Placeholder scan: no `TBD`, `TODO`, "similar to", or unspecified error handling remains.
- [ ] Type consistency: `VegaStream`, `VegaSubtitle`, `mapStreams`, `mapQuality`, `mapSubtitles` are defined in Task 4 and used with the same names/signatures in Task 5 and the README.
- [ ] Scope check: provider ports are explicitly deferred to follow-up plans; no unauthorized provider is implemented here.

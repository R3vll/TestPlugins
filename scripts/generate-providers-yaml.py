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

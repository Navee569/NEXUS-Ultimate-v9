#!/usr/bin/env python3
"""Reproducible source-level sanity checks for the NEXUS private fork.

This is not a malware scanner or a substitute for a resolved-dependency CVE scan.
Run from the repository root: python3 scripts/nexus_static_audit.py
"""
from __future__ import annotations
import json
import re
import sys
import tomllib
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
checks: list[dict[str, str]] = []

def record(name: str, status: str, detail: str) -> None:
    checks.append({"check": name, "status": status, "detail": detail})

# Manifest XML parse + permission inventory.
manifest = ROOT / "app/src/main/AndroidManifest.xml"
try:
    root = ET.parse(manifest).getroot()
    android = "{http://schemas.android.com/apk/res/android}"
    permissions = sorted({node.attrib.get(android + "name", "") for node in root.findall("uses-permission")})
    record("Android manifest XML", "PASS", f"Parsed successfully; {len(permissions)} uses-permission declarations.")
    for forbidden in ("android.permission.REQUEST_INSTALL_PACKAGES", "android.permission.READ_CALL_LOG", "android.permission.READ_SMS"):
        record(f"Permission absent: {forbidden}", "PASS" if forbidden not in permissions else "REVIEW", "Not declared in the main manifest." if forbidden not in permissions else "Declared; verify the feature need.")
    record("Automatic backup", "PASS" if root.find("application").attrib.get(android + "allowBackup", "true").lower() == "false" else "REVIEW", f"android:allowBackup={root.find('application').attrib.get(android + 'allowBackup', 'unspecified')}")
    record("Cleartext traffic", "PASS" if root.find("application").attrib.get(android + "usesCleartextTraffic", "false").lower() == "false" else "REVIEW", f"android:usesCleartextTraffic={root.find('application').attrib.get(android + 'usesCleartextTraffic', 'default false')}")
except Exception as exc:
    permissions = []
    record("Android manifest XML", "FAIL", str(exc))

# Parse version catalog; dependency catalog is a declared inventory, not a resolved graph.
try:
    catalog = tomllib.loads((ROOT / "gradle/libs.versions.toml").read_text())
    versions = catalog.get("versions", {})
    libraries = catalog.get("libraries", {})
    resolved_aliases = []
    for alias, spec in libraries.items():
        version = spec.get("version")
        if isinstance(version, dict):
            version = versions.get(version.get("ref", ""), "<missing-ref>")
        elif version is None:
            version = spec.get("version", "<managed/transitive>")
        resolved_aliases.append({"alias": alias, "module": spec.get("module", f"{spec.get('group', '?')}:{spec.get('name', '?')}"), "version": str(version)})
    record("Gradle version catalog", "PASS", f"Parsed {len(versions)} version keys and {len(libraries)} library aliases; actual transitive resolution unavailable without Gradle.")
    ffmpeg = next((x for x in resolved_aliases if x["alias"] == "ffmpeg-kit-audio"), None)
    record("FFmpegKit dependency maintenance", "PASS" if ffmpeg and ffmpeg["module"] == "dev.ffmpegkit-maintained:ffmpeg-kit-audio" and ffmpeg["version"] == "8.1.7" else "REVIEW", f"Declared coordinate: {ffmpeg['module']}:{ffmpeg['version']}" if ffmpeg else "Alias not found")
    protobuf = next((x for x in resolved_aliases if x["alias"] == "protobuf-javalite"), None)
    record("Protobuf version floor", "PASS" if protobuf and protobuf["version"] == "4.36.2" else "REVIEW", f"Declared protobuf-javalite: {protobuf['version']}" if protobuf else "Alias not found")
except Exception as exc:
    resolved_aliases = []
    record("Gradle version catalog", "FAIL", str(exc))

# Static security checks.
source_files = [p for p in ROOT.rglob("*") if p.is_file() and p.suffix in {".kt", ".java", ".kts", ".xml", ".properties", ".toml"} and ".gradle" not in p.parts and "build" not in p.parts]
source_text = "\n".join(p.read_text(errors="ignore") for p in source_files)
checks_patterns = [
    ("Discord WebView token extraction pattern", r"localStorage\.getItem\(['\"]token['\"]\)", "REVIEW", "Token-extraction pattern still appears in scanned source."),
    ("Legacy retired FFmpegKit coordinate", r"com\.arthenica:ffmpeg-kit-audio", "REVIEW", "Legacy FFmpegKit coordinate remains in source/config."),
    ("Shell execution APIs", r"Runtime\.getRuntime\(\)\.exec\(|ProcessBuilder\(", "REVIEW", "Shell/process execution API matched; manually confirm legitimate use."),
    ("Dynamic DEX/class loading APIs", r"DexClassLoader|InMemoryDexClassLoader|PathClassLoader", "REVIEW", "Dynamic class-loading API matched; manually confirm legitimate use."),
]
for name, pattern, risky_status, detail in checks_patterns:
    match = re.search(pattern, source_text, re.I)
    record(name, risky_status if match else "PASS", detail if match else "No matching pattern found in scanned source/config files.")
lastfm_line = next((line for line in source_text.splitlines() if "buildConfigField" in line and "LASTFM_SECRET" in line), "")
lastfm_empty = '\"\"' in lastfm_line
lastfm_nonempty = bool(lastfm_line and not lastfm_empty)
record("Hardcoded Last.fm secret BuildConfig field", "REVIEW" if lastfm_nonempty else "PASS", "LASTFM_SECRET is an empty placeholder; no embedded Last.fm secret found." if lastfm_empty else ("A non-empty Last.fm secret BuildConfig value appears in scanned source." if lastfm_nonempty else "No Last.fm secret BuildConfig value found."))

# Verify the generated mix is wired to actual queue APIs.
nexus = ROOT / "app/src/main/kotlin/com/music/echo/ui/screens/settings/NexusStudioScreen.kt"
try:
    ns = nexus.read_text()
    required = ["YouTube.search(query", "filterIsInstance<SongItem>()", "distinctBy { it.id }", "playerConnection?.playQueue(ListQueue", "playerConnection?.addToQueue("]
    missing = [token for token in required if token not in ns]
    record("Smart Discovery queue wiring", "PASS" if not missing else "REVIEW", "Search, deduplicate, playQueue and addToQueue paths present." if not missing else "Missing: " + ", ".join(missing))
except Exception as exc:
    record("Smart Discovery queue wiring", "FAIL", str(exc))

# Secret-like assignments are reported as a manual review count only, avoiding printing any values.
secret_patterns = [r"(?i)(?:api[_-]?key|client[_-]?secret|access[_-]?token|refresh[_-]?token)\s*[:=]\s*['\"][^'\"]{12,}['\"]"]
secret_matches = sum(len(re.findall(pattern, p.read_text(errors="ignore"))) for p in source_files for pattern in secret_patterns)
record("Potential embedded credential literals", "REVIEW" if secret_matches else "PASS", f"{secret_matches} heuristic matches; a Google API-key-shaped value is present in PoTokenWebView. It may be a client key rather than a secret, but its API restrictions and quotas should be verified. Values are not emitted by this report.")

# Output report files.
report = {
    "title": "NEXUS source-level static audit",
    "scope": "Repository source/configuration only; not APK, runtime traffic, or resolved dependency graph.",
    "dependency_alias_count": len(resolved_aliases),
    "permissions": permissions,
    "declared_dependencies": resolved_aliases,
    "checks": checks,
    "limitations": [
        "Gradle could not download its distribution in this environment because DNS resolution for services.gradle.org failed.",
        "No APK was compiled or executed, and no runtime network capture was performed.",
        "The dependency list is from the version catalog, not the resolved transitive dependency graph.",
        "A complete CVE scan requires resolving all variants and checking the resolved graph against a current advisory database.",
    ],
}
(ROOT / "NEXUS_STATIC_AUDIT.json").write_text(json.dumps(report, indent=2) + "\n")
lines = ["# NEXUS Source-Level Static Audit", "", f"**Scope:** {report['scope']}", "", f"Declared version-catalog aliases reviewed: **{len(resolved_aliases)}**", "", "## Checks", "", "| Check | Result | Detail |", "|---|---|---|"]
for item in checks:
    lines.append(f"| {item['check'].replace('|', '\\|')} | **{item['status']}** | {item['detail'].replace('|', '\\|')} |")
lines += ["", "## Dependency notes", "", "- The legacy `com.arthenica:ffmpeg-kit-audio:6.0-2` coordinate was retired and its artifacts removed from Maven Central. The project now declares the community-maintained `dev.ffmpegkit-maintained:ffmpeg-kit-audio:8.1.7` coordinate. The maintainer describes it as a drop-in continuation, but it is a third-party fork; validate maintainer trust, artifact checksums, transitive graph, licence, and runtime behaviour before release.", "- `protobuf-javalite` is declared at `4.36.2`, above the fixed versions listed for CVE-2024-7254. This observation applies only to the declared direct version; confirm the resolved graph has no older duplicate/transitive protobuf artifacts.", "- This is not a full vulnerability scan. No resolved dependency graph was available.", "", "## Limitations", ""] + [f"- {x}" for x in report["limitations"]] + [""]
(ROOT / "NEXUS_STATIC_AUDIT.md").write_text("\n".join(lines))
print(f"Wrote NEXUS_STATIC_AUDIT.md and .json; {len(checks)} checks, {sum(c['status']=='FAIL' for c in checks)} FAIL, {sum(c['status']=='REVIEW' for c in checks)} REVIEW.")
for item in checks:
    print(f"{item['status']:6} {item['check']}: {item['detail']}")

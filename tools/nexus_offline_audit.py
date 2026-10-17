#!/usr/bin/env python3
"""Repeatable offline NEXUS source audit. No network access; heuristic only."""
from pathlib import Path
import re, json, tomllib, xml.etree.ElementTree as ET
ROOT = Path(__file__).resolve().parents[1]
EXTS = {'.kt','.java','.xml','.gradle','.kts','.json','.properties','.toml'}
PATS = {
 'shell_execution': r'Runtime\.getRuntime\(\)\.exec|ProcessBuilder\s*\(',
 'dynamic_dex_loading': r'DexClassLoader|InMemoryDexClassLoader|PathClassLoader',
 'reflection_class_loading': r'Class\.forName\s*\(|\.loadClass\s*\(',
 'discord_localstorage_token': r'localStorage\.getItem\s*\(\s*[\'\"]token[\'\"]\s*\)|DiscordInterface\.onToken',
 'webview_js_interface': r'addJavascriptInterface\s*\(',
 'cleartext_http_literals': r'http://(?!schemas\.android\.com|schemas\.microsoft\.com|www\.w3\.org)',
 'credential_like_literals': r'(api[_-]?key|client[_-]?secret|access[_-]?token|password|private[_-]?key)\s*[:=]\s*[\'\"][^\'\"]{12,}[\'\"]',
}
xml_files=list(ROOT.rglob('*.xml')); xml_errors=[]
for f in xml_files:
 try: ET.parse(f)
 except Exception as e: xml_errors.append({'file':str(f.relative_to(ROOT)),'error':str(e)})
files=[p for p in ROOT.rglob('*') if p.is_file() and p.suffix.lower() in EXTS and '.git' not in p.parts and p.name not in {'NEXUS-LOCAL-AUDIT-RESULTS.json'} and p.parent.name != 'tools']
results={k:[] for k in PATS}; endpoints=set()
for f in files:
 try: lines=f.read_text(errors='ignore').splitlines()
 except Exception: continue
 for n,line in enumerate(lines,1):
  for k,pattern in PATS.items():
   if re.search(pattern,line,re.I):
    snippet=re.sub(r'AIza[0-9A-Za-z_-]{30,}','[REDACTED_GOOGLE_API_KEY_LIKE_VALUE]',line.strip()[:220])
    results[k].append({'file':str(f.relative_to(ROOT)),'line':n,'snippet':snippet})
  for u in re.findall(r'https?://[A-Za-z0-9._~:/?#\[\]@!$&()*+,;=%-]+|wss?://[A-Za-z0-9._~:/?#\[\]@!$&()*+,;=%-]+',line):
   if not u.startswith(('http://schemas.android.com','http://schemas.microsoft.com','http://www.w3.org')):
    endpoints.add(u.rstrip('"\'`),.;'))
try:
 catalog=tomllib.loads((ROOT/'gradle/libs.versions.toml').read_text())
 aliases=len(catalog.get('libraries',{})); plugins=len(catalog.get('plugins',{}))
except Exception: aliases=plugins=0
unit_tests=[p for p in ROOT.rglob('*') if p.is_file() and '/src/test/' in str(p).replace('\\','/') and p.suffix in {'.kt','.java'}]
instrumentation=[p for p in ROOT.rglob('*') if p.is_file() and '/src/androidTest/' in str(p).replace('\\','/') and p.suffix in {'.kt','.java'}]
locks=[p for p in ROOT.rglob('*') if p.is_file() and (p.name=='gradle.lockfile' or 'dependency-lock' in p.name.lower() or p.name.endswith('.lockfile'))]
out={'scope':'offline heuristic static scan; not malware certification','xml_total':len(xml_files),'xml_errors':xml_errors,'source_config_files_scanned':len(files),'library_aliases':aliases,'plugin_aliases':plugins,'unit_test_source_files':len(unit_tests),'instrumentation_test_source_files':len(instrumentation),'dependency_lockfiles':[str(p.relative_to(ROOT)) for p in locks],'pattern_matches':results,'external_endpoint_literals':sorted(endpoints)}
(ROOT/'NEXUS-LOCAL-AUDIT-RESULTS.json').write_text(json.dumps(out,indent=2))
print(json.dumps({k:out[k] for k in ('xml_total','xml_errors','source_config_files_scanned','library_aliases','plugin_aliases','unit_test_source_files','instrumentation_test_source_files','dependency_lockfiles')},indent=2))
print('pattern matches:', {k:len(v) for k,v in results.items()})
print('external endpoint literals:',len(endpoints))

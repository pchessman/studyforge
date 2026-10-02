#!/usr/bin/env python3
"""Turn the web build (operation-phoenix.html) into a fully offline page for the Android and LG TV apps.

- Tailwind is inlined from app/vendor, so nothing is fetched from a CDN.
- Google Fonts links are dropped (the game falls back to built-in rounded fonts).
- The content policy is rewritten for an offline app: no network connections of any kind.
"""
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
SRC = ROOT / 'operation-phoenix.html'
TAILWIND = ROOT / 'app' / 'vendor' / 'tailwind-browser-4.3.3.js'
CSP = ("default-src 'none'; script-src 'unsafe-inline'; style-src 'unsafe-inline'; font-src data:; "
       "img-src data: blob:; media-src data: blob:; connect-src 'none'; worker-src 'none'; object-src 'none'; "
       "frame-src 'none'; base-uri 'none'; form-action 'none'")


def build(out_dir: pathlib.Path) -> pathlib.Path:
    html = SRC.read_text(encoding='utf-8')
    tw = TAILWIND.read_text(encoding='utf-8')
    if '</script' in tw.lower():
        sys.exit('Tailwind bundle contains a closing script tag; refusing to inline it.')
    # the pinned CDN script becomes an inline copy of the same file
    html, n = re.subn(r'<script src="https://cdn\.jsdelivr\.net/npm/@tailwindcss/browser@[^"]+"[^>]*></script>',
                      lambda _: '<script>' + tw + '</script>', html, count=1)
    if n != 1:
        sys.exit('Could not find the Tailwind script tag in operation-phoenix.html')
    html = re.sub(r'<link rel="(preconnect|stylesheet)" href="https://fonts\.(googleapis|gstatic)\.com[^>]*>\n?', '', html)
    html, n = re.subn(r'<meta http-equiv="Content-Security-Policy" content="[^"]*">',
                      lambda _: f'<meta http-equiv="Content-Security-Policy" content="{CSP}">', html, count=1)
    if n != 1:
        sys.exit('Could not find the Content-Security-Policy meta tag')
    if re.search(r'(src|href)="https?://', html):
        sys.exit('A remote resource is still referenced; the app must work offline.')
    out_dir.mkdir(parents=True, exist_ok=True)
    out = out_dir / 'index.html'
    out.write_text(html, encoding='utf-8')
    return out


if __name__ == '__main__':
    target = pathlib.Path(sys.argv[1]) if len(sys.argv) > 1 else ROOT / 'app' / 'build' / 'www'
    print(build(target))

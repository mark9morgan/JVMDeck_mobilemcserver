#!/usr/bin/env python3
"""
Helper script to spin up a local HTTP server for testing Dynamic JRE Download in JvmDeck.
Copies required JRE 21 archives from assets to a local serving directory and starts python http.server.
"""

import os
import shutil
from pathlib import Path
import http.server
import socketserver

PORT = 8000

def main():
    project_root = Path(__file__).resolve().parent.parent
    assets_dir = project_root / "app" / "src" / "main" / "assets" / "jre-21"
    server_dir = project_root / "local_jre_repo"
    server_dir.mkdir(parents=True, exist_ok=True)

    print(f"Preparing local JRE repository in {server_dir}...")

    files_to_copy = ["universal.tar.xz", "bin-arm64.tar.xz"]
    for filename in files_to_copy:
        src = assets_dir / filename
        dst = server_dir / filename
        if src.exists():
            shutil.copy2(src, dst)
            print(f"Copied {filename} ({dst.stat().st_size / 1024 / 1024:.2f} MB)")
        else:
            print(f"Warning: {filename} not found in {assets_dir}")

    os.chdir(server_dir)
    Handler = http.server.SimpleHTTPRequestHandler

    print(f"\n==================================================")
    print(f" Local JRE HTTP Server running at http://0.0.0.0:{PORT}")
    print(f" Emulator URL to use in app: http://10.0.2.2:{PORT}")
    print(f"==================================================\n")
    print("Press Ctrl+C to stop the server.")

    with socketserver.TCPServer(("", PORT), Handler) as httpd:
        try:
            httpd.serve_forever()
        except KeyboardInterrupt:
            print("\nServer stopped.")

if __name__ == "__main__":
    main()

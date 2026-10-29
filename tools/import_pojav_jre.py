#!/usr/bin/env python3
"""
Utility script to download PojavLauncher OpenJDK releases and format them for JvmDeck.

Usage:
  python tools/import_pojav_jre.py --version 8
  python tools/import_pojav_jre.py --version 17
  python tools/import_pojav_jre.py --version 21
  python tools/import_pojav_jre.py --version 25
  python tools/import_pojav_jre.py --all
"""

import os
import sys
import tarfile
import zipfile
import urllib.request
import argparse
from pathlib import Path

POJAV_RELEASES = {
    "8": "https://github.com/itsaky/openjdk-8-android/releases/download/2021-12-26/jre8-aarch64.tar.xz",
    "17": "https://github.com/MojoLauncher/android-openjdk-build-17-25/releases/download/rolling/jre17-pojav.zip",
    "21": "https://github.com/MojoLauncher/android-openjdk-build-17-25/releases/download/rolling/jre21-pojav.zip",
    "25": "https://github.com/MojoLauncher/android-openjdk-build-17-25/releases/download/rolling/jre25-pojav.zip",
}

def download_file(url: str, dest: Path):
    print(f"Downloading {url} ...")
    dest.parent.mkdir(parents=True, exist_ok=True)
    req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
    with urllib.request.urlopen(req) as response, open(dest, "wb") as out_file:
        out_file.write(response.read())
    print(f"Saved to {dest} ({dest.stat().st_size} bytes)")

def process_jre_archive(archive_path: Path, java_version: str, project_root: Path):
    print(f"Processing JRE {java_version} from {archive_path}...")

    asset_dir_name = "jre" if java_version == "8" else f"jre-{java_version}"
    asset_dir = project_root / "app" / "src" / "main" / "assets" / asset_dir_name
    asset_dir.mkdir(parents=True, exist_ok=True)

    jni_dir = project_root / "app" / "src" / "main" / "jniLibs" / "arm64-v8a"
    jni_dir.mkdir(parents=True, exist_ok=True)

    ver_libjava = jni_dir / f"libjava_{java_version}.so"
    main_libjava = jni_dir / "libjava.so"

    if archive_path.suffix == ".zip" or str(archive_path).endswith(".zip"):
        print(f"Extracting zip contents to {asset_dir}...")
        with zipfile.ZipFile(archive_path, "r") as z:
            z.extractall(asset_dir)

        bin_arm64_tar = asset_dir / "bin-arm64.tar.xz"
        if bin_arm64_tar.exists():
            with tarfile.open(bin_arm64_tar, "r:xz") as tar:
                for member in tar.getmembers():
                    if member.name.endswith("bin/java") or member.name == "bin/java" or member.name == "./bin/java":
                        print(f"Extracting {member.name} to {ver_libjava}...")
                        f = tar.extractfile(member)
                        if f:
                            content = f.read()
                            ver_libjava.write_bytes(content)
                            main_libjava.write_bytes(content)
                            print(f"Updated {ver_libjava.name} ({ver_libjava.stat().st_size} bytes)")
                        break
    else:
        target_asset_tar = asset_dir / "jre.tar.xz"
        print(f"Copying archive to {target_asset_tar}...")
        target_asset_tar.write_bytes(archive_path.read_bytes())

        with tarfile.open(archive_path, "r:xz") as tar:
            java_member = None
            for member in tar.getmembers():
                if member.name.endswith("bin/java") or member.name == "bin/java" or member.name == "./bin/java":
                    java_member = member
                    break

            if java_member:
                print(f"Extracting {java_member.name} to {ver_libjava}...")
                f = tar.extractfile(java_member)
                if f:
                    content = f.read()
                    ver_libjava.write_bytes(content)
                    main_libjava.write_bytes(content)
                    print(f"Updated {ver_libjava.name} ({ver_libjava.stat().st_size} bytes)")

        (asset_dir / "version").write_text(f"PojavLauncher JRE {java_version}\n")

    print(f"Successfully imported JRE {java_version} into {asset_dir}")

def import_version(ver: str, project_root: Path, file_path=None, custom_url=None):
    url = custom_url or POJAV_RELEASES.get(ver)
    if not file_path and not url:
        print(f"Error: No download URL configured for Java {ver}.")
        return False

    if file_path:
        archive_file = Path(file_path)
    else:
        ext = ".zip" if url.endswith(".zip") else ".tar.xz"
        tmp_dir = project_root / ".tmp_jre"
        tmp_dir.mkdir(exist_ok=True)
        archive_file = tmp_dir / f"jre{ver}{ext}"
        if not archive_file.exists():
            download_file(url, archive_file)

    process_jre_archive(archive_file, ver, project_root)
    return True

def main():
    parser = argparse.ArgumentParser(description="Import PojavLauncher JRE for JvmDeck")
    parser.add_argument("--version", help="Java major version (e.g. 8, 17, 21, 25)")
    parser.add_argument("--all", action="store_true", help="Download and import all available versions (8, 17, 21, 25)")
    parser.add_argument("--file", help="Local path to file")
    parser.add_argument("--url", help="Custom download URL")

    args = parser.parse_args()
    project_root = Path(__file__).resolve().parent.parent

    if args.all:
        for ver in ["8", "17", "21", "25"]:
            print(f"\n--- Importing Java {ver} ---")
            import_version(ver, project_root)
    elif args.version:
        import_version(args.version, project_root, file_path=args.file, custom_url=args.url)
    else:
        parser.print_help()

if __name__ == "__main__":
    main()

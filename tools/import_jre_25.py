#!/usr/bin/env python3
import os
import io
import zipfile
import tarfile
import urllib.request
from pathlib import Path

URL = "https://github.com/MojoLauncher/android-openjdk-build-17-25/releases/download/rolling/jre25-pojav.zip"

def main():
    project_root = Path(__file__).resolve().parent.parent
    tmp_dir = project_root / ".tmp_jre"
    tmp_dir.mkdir(exist_ok=True)
    zip_path = tmp_dir / "jre25-pojav.zip"

    print(f"Downloading Java 25 JRE from {URL}...")
    req = urllib.request.Request(URL, headers={"User-Agent": "Mozilla/5.0"})
    with urllib.request.urlopen(req) as resp, open(zip_path, "wb") as out:
        out.write(resp.read())
    print("Download complete.")

    jni_dir = project_root / "app" / "src" / "main" / "jniLibs" / "arm64-v8a"
    jni_dir.mkdir(parents=True, exist_ok=True)
    libjava_25 = jni_dir / "libjava_25.so"

    print("Extracting bin/java as libjava_25.so...")
    with zipfile.ZipFile(zip_path, "r") as z:
        # Look for bin-arm64.tar.xz inside the zip
        if "bin-arm64.tar.xz" in z.namelist():
            tar_data = z.read("bin-arm64.tar.xz")
            with tarfile.open(fileobj=io.BytesIO(tar_data), mode="r:xz") as tar:
                for member in tar.getmembers():
                    if member.name.endswith("bin/java") or member.name == "bin/java" or member.name == "./bin/java":
                        f = tar.extractfile(member)
                        if f:
                            libjava_25.write_bytes(f.read())
                            print(f"Successfully extracted {libjava_25} ({libjava_25.stat().st_size} bytes)")
                        break

    # Also pack into server_test/jre-25.zip
    server_test = project_root / "server_test"
    server_test.mkdir(exist_ok=True)
    dest_zip = server_test / "jre-25.zip"
    dest_zip.write_bytes(zip_path.read_bytes())
    print(f"Copied jre-25.zip to {dest_zip}")

if __name__ == "__main__":
    main()

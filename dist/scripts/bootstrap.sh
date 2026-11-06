#!/usr/bin/env bash
set -euo pipefail
# Official tools only, pinned releases, TLS and checksums enabled. No checkout resets.
mkdir -p /workspace/tools/downloads
python3 - <<'PYTHON'
import pathlib,urllib.request,hashlib,tarfile,zipfile,os
root=pathlib.Path('/workspace/tools')
items=[
 ('jdk-21.0.9+10','jdk21.tar.gz','https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.9%2B10/OpenJDK21U-jdk_x64_linux_hotspot_21.0.9_10.tar.gz','810d3773df7e0d6c4394e4e244b264c8b30e0b05a0acf542d065fd78a6b65c2f'),
 ('gradle-8.14.3','gradle.zip','https://services.gradle.org/distributions/gradle-8.14.3-bin.zip','bd71102213493060956ec229d946beee57158dbd89d0e62b91bca0fa2c5f3531')]
for folder,name,url,expected in items:
 if (root/folder).exists(): continue
 p=root/'downloads'/name
 urllib.request.urlretrieve(url,p)
 if hashlib.sha256(p.read_bytes()).hexdigest()!=expected: raise RuntimeError('Checksum mismatch: '+name)
 if name.endswith('.gz'):
  with tarfile.open(p) as z: z.extractall(root,filter='data')
 else:
  with zipfile.ZipFile(p) as z: z.extractall(root)
os.chmod(root/'gradle-8.14.3/bin/gradle',0o755)
PYTHON

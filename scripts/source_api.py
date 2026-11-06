#!/usr/bin/env python3
"""Reproducible compile-only source build. Never substitutes a Minecraft server implementation."""
from pathlib import Path
import hashlib,io,json,tarfile,urllib.request
ROOT=Path('/workspace/tools/paper-api-build')
SOURCES=[
 ('PaperMC/Paper','c5eb0790f199da6c38d0a650e1e5cd5415b28185','4dc0372ef8618b8af832da9d33276d1d8552dc74dc2ba003d0c27b78f148abaa','paper-api'),
 ('Mojang/brigadier','9ba4f13c0fe82b07c08c2dc2d8043f075ffd0d98','7f454f947bea3ead05a4973ce2b3ed061c1600259a0b16fbdde39b8c9cf69683','brigadier')]
for repo,commit,expected,target in SOURCES:
    archive=ROOT/'downloads'/f'{target}-{commit}.tar.gz';archive.parent.mkdir(parents=True,exist_ok=True)
    if not archive.exists():
        with urllib.request.urlopen(f'https://codeload.github.com/{repo}/tar.gz/{commit}',timeout=90) as response: archive.write_bytes(response.read())
    if hashlib.sha256(archive.read_bytes()).hexdigest()!=expected: raise RuntimeError(f'SHA-256 mismatch: {repo}')
    # Extract only the API/source subtree; no upstream build scripts are executed.
    with tarfile.open(archive) as tar:
        for member in tar.getmembers():
            parts=Path(member.name).parts[1:]
            if not parts or not member.isfile(): continue
            if target=='paper-api':
                if parts[0]!='paper-api' and parts[0]!='LICENSE.md': continue
                relative=Path(*parts[1:]) if parts[0]=='paper-api' else Path('UPSTREAM-LICENSE.md')
            else:
                if parts[0] not in ('src','LICENSE'): continue
                relative=Path(*parts)
            if relative.is_absolute() or '..' in relative.parts: raise RuntimeError('Unsafe archive path')
            out=ROOT/'upstream'/target/relative;out.parent.mkdir(parents=True,exist_ok=True)
            out.write_bytes(tar.extractfile(member).read())
report={'paper_commit':SOURCES[0][1],'brigadier_commit':SOURCES[1][1],
 'status':'compile-only API built from unmodified official sources; not an official Paper release binary',
 'dependency_deviations':{'bungeecord-chat':'Maven Central 1.21-R0.2 instead of unavailable Paper deprecated+build.21',
 'brigadier':'pinned official source commit instead of official 1.3.10 Maven binary'},
 'api_bundled_in_plugin':False}
(Path(__file__).resolve().parents[1]/'reports/source-api-provenance.json').write_text(json.dumps(report,indent=2)+'\n')
print('Source archives verified; upstream Java files left unmodified.')

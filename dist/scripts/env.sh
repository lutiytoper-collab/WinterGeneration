# Source this file. It selects retained tools and preserves the platform proxy/trust.
export JAVA_HOME=/workspace/tools/jdk-21.0.9+10
export PATH="$JAVA_HOME/bin:/workspace/tools/gradle-8.14.3/bin:$PATH"
export GRADLE_USER_HOME=/workspace/.gradle-winter
_winter_proxy_flags=$(python3 - <<'PYTHON'
import os
from urllib.parse import urlsplit
p=urlsplit(os.environ.get('HTTPS_PROXY',''))
if p.hostname:
 if not all(c.isalnum() or c in '.-' for c in p.hostname): raise SystemExit('Invalid proxy host')
 print(f'-Dhttps.proxyHost={p.hostname} -Dhttps.proxyPort={p.port or 8080} -Dhttp.proxyHost={p.hostname} -Dhttp.proxyPort={p.port or 8080}')
PYTHON
)
read -r -a WINTER_JAVA_FLAGS <<< "$_winter_proxy_flags"
WINTER_JAVA_FLAGS+=("-Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts")
export GRADLE_OPTS="${GRADLE_OPTS:-} ${WINTER_JAVA_FLAGS[*]}"
unset _winter_proxy_flags

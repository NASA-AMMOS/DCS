#!/bin/bash
# Starts the SA management backend for the Playwright suite: the real SaController on an empty in-memory H2 SADB,
# serving the built frontend from ../dist under /sa-mgmt. Started by `yarn e2e` (via playwright.config.js) or by hand
# with `yarn e2e:server` to keep it running between test runs.
#
# Environment:
#   E2E_PORT          port to listen on (default 18447)
#   E2E_SKIP_INSTALL  set to 1 to skip installing the current kmc-sa-db-api/kmc-sa-db-lib into ~/.m2 first
#   MVN_ARGS          extra Maven arguments, e.g. -o
set -euo pipefail

FRONTEND_DIR="$(cd "$(dirname "$0")/.." && pwd)"
GUI_DIR="$(cd "${FRONTEND_DIR}/../../.." && pwd)"
SA_MGMT_DIR="$(cd "${GUI_DIR}/.." && pwd)"
ROOT_DIR="$(cd "${SA_MGMT_DIR}/.." && pwd)"
PORT="${E2E_PORT:-18447}"
MVN="mvn -q -B ${MVN_ARGS:-}"
# the frontend is served from dist, so skip the frontend-maven-plugin steps
SKIP_FRONTEND="-Dskip.installnodenpm -Dskip.installyarn -Dskip.yarn"

if [ ! -f "${FRONTEND_DIR}/dist/index.html" ]; then
  echo "No ${FRONTEND_DIR}/dist/index.html, run 'yarn build' first" >&2
  exit 1
fi

if [ "${E2E_SKIP_INSTALL:-0}" != "1" ]; then
  # kmc-sa-gui resolves these (and the kmc-sa-db-lib tests jar with the H2 schema) from ~/.m2
  ${MVN} -f "${ROOT_DIR}/pom.xml" install -N
  ${MVN} -f "${SA_MGMT_DIR}/pom.xml" install -DskipTests -pl kmc-sa-db-api,kmc-sa-db-lib -am
fi

exec ${MVN} -f "${GUI_DIR}/pom.xml" ${SKIP_FRONTEND} test-compile exec:java \
  -Dexec.classpathScope=test \
  -Dexec.mainClass=gov.nasa.jpl.ammos.asec.kmc.saserver.app.sa.E2eServer \
  -Dexec.args="--server.port=${PORT} --server.servlet.context-path=/sa-mgmt --spring.web.resources.static-locations=file:${FRONTEND_DIR}/dist/"

#!/bin/sh
set -eu

port="${PORT:-8080}"
case "$port" in
    ''|*[!0-9]*)
        printf >&2 'PORT must be numeric, got: %s\n' "$port"
        exit 1
        ;;
esac

sed -i "s/port=\"8080\"/port=\"$port\"/" "$CATALINA_HOME/conf/server.xml"
exec "$CATALINA_HOME/bin/catalina.sh" run

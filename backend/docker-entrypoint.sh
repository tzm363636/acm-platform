#!/bin/sh
set -eu
umask 077

fail() { printf '%s\n' "$1" >&2; exit 1; }
# Render secret files accept the PEM CA. Build a private PKCS12 truststore at runtime.
# Never put a password in argv or print keytool's diagnostic content.
if [ -n "${DB_TRUSTSTORE_PATH:-}" ]; then
  [ -r "$DB_TRUSTSTORE_PATH" ] || fail 'Configured database truststore is not readable.'
elif [ -n "${DB_CA_CERT_PATH:-}" ]; then
  [ -r "$DB_CA_CERT_PATH" ] || fail 'Configured database CA file is not readable.'
  [ -n "${DB_TRUSTSTORE_PASSWORD:-}" ] || fail 'Set DB_TRUSTSTORE_PASSWORD as a backend secret.'
  trust_dir=$(mktemp -d)
  export DB_TRUSTSTORE_PATH="$trust_dir/database.p12"
  if ! keytool -importcert -noprompt -alias database-ca -file "$DB_CA_CERT_PATH" \
    -keystore "$DB_TRUSTSTORE_PATH" -storetype PKCS12 -storepass:env DB_TRUSTSTORE_PASSWORD >/dev/null 2>&1; then
    rm -f "$DB_TRUSTSTORE_PATH"
    rmdir "$trust_dir"
    fail 'Database CA import failed. Check the PEM certificate and truststore secret.'
  fi
else
  case ",${SPRING_PROFILES_ACTIVE:-}," in
    *,render,*|*,aiven,*) fail 'Set DB_CA_CERT_PATH or mount DB_TRUSTSTORE_PATH before cloud startup.' ;;
  esac
fi
exec "$@"

#!/bin/sh
set -eu

fail() {
    printf '%s\n' "$1" >&2
    exit 1
}

PORT=${PORT:-10000}
case "$PORT" in
    ''|*[!0-9]*) fail 'PORT debe ser un número entre 1024 y 65535.' ;;
esac
[ "${#PORT}" -le 5 ] || fail 'PORT fuera de rango.'
[ "$PORT" -ge 1024 ] && [ "$PORT" -le 65535 ] || fail 'PORT fuera de rango.'

[ -n "${DB_URL:-}" ] || fail 'Falta configurar DB_URL.'
[ -n "${DB_USER:-}" ] || fail 'Falta configurar DB_USER.'
[ -n "${DB_PASSWORD:-}" ] || fail 'Falta configurar DB_PASSWORD.'
[ -n "${ADMIN_EMAIL:-}" ] || fail 'Falta configurar ADMIN_EMAIL.'
[ -n "${ADMIN_PASSWORD_HASH:-}" ] || fail 'Falta configurar ADMIN_PASSWORD_HASH.'
case "$DB_URL" in
    jdbc:mysql://*) ;;
    *) fail 'DB_URL debe ser una URL JDBC de MySQL.' ;;
esac
case "$ADMIN_PASSWORD_HASH" in
    'pbkdf2_sha256$'*) ;;
    *) fail 'ADMIN_PASSWORD_HASH debe generarse con PasswordTool hash.' ;;
esac
case "${SESSION_COOKIE_SECURE:-true}" in
    true|false) ;;
    *) fail 'SESSION_COOKIE_SECURE debe ser true o false.' ;;
esac

case "${TRUST_RENDER_PROXY:-false}" in
    true)
        # Public Render traffic goes through Cloudflare, which overwrites this header.
        remote_header=CF-Connecting-IP
        protocol_header=X-Forwarded-Proto
        ;;
    false)
        # Disable forwarded-header processing for direct local HTTP connections.
        remote_header=
        protocol_header=
        ;;
    *) fail 'TRUST_RENDER_PROXY debe ser true o false.' ;;
esac

# The CA is supplied at runtime, never copied into the image or repository.
# This truststore contains only public certificates; its fixed password is not a credential.
tls_options=
if [ -n "${DB_CA_CERT_BASE64:-}" ]; then
    umask 077
    tls_dir=$(mktemp -d /tmp/teachmate-tls.XXXXXX)
    printf '%s' "$DB_CA_CERT_BASE64" | base64 --decode > "$tls_dir/ca.pem" \
        || fail 'DB_CA_CERT_BASE64 no es un certificado codificado válido.'
    # Keep Java's public roots for HTTPS services (Brevo), then add Aiven's CA.
    tls_public_roots="${JAVA_HOME:-}/lib/security/cacerts"
    [ -r "$tls_public_roots" ] || fail 'No se encontró el almacén público de certificados de Java.'
    keytool -importkeystore -noprompt -srckeystore "$tls_public_roots" -srcstorepass changeit \
        -destkeystore "$tls_dir/truststore.p12" -deststoretype PKCS12 -deststorepass changeit \
        > /dev/null 2>&1 || fail 'No se pudieron conservar los certificados públicos para HTTPS.'
    keytool -importcert -noprompt -alias mysql-ca -file "$tls_dir/ca.pem" \
        -keystore "$tls_dir/truststore.p12" -storetype PKCS12 -storepass changeit \
        > /dev/null 2>&1 || fail 'No se pudo importar el certificado CA de MySQL.'
    tls_options="-Djavax.net.ssl.trustStore=$tls_dir/truststore.p12 -Djavax.net.ssl.trustStoreType=PKCS12 -Djavax.net.ssl.trustStorePassword=changeit"
fi

# Values appended here are validated numbers or fixed strings, never credentials.
CATALINA_OPTS="${CATALINA_OPTS:-} $tls_options -Dhttp.port=$PORT -Dproxy.remoteHeader=$remote_header -Dproxy.protocolHeader=$protocol_header"
export CATALINA_OPTS
printf 'Iniciando TeachMate en el puerto %s.\n' "$PORT"
exec "${CATALINA_HOME:-/usr/local/tomcat}/bin/catalina.sh" run

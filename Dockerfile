# --- ETAPA 1: Compilación ---
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# --- ETAPA 2: Ejecución ---
FROM eclipse-temurin:21-jre
WORKDIR /app

USER root
RUN mkdir -p /app/wallet && apt-get update && apt-get install -y unzip && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/target/*.jar app.jar

# Creación del script usando Heredoc (Evita errores de escape de caracteres)
RUN cat <<-'EOF' > /app/entrypoint.sh
#!/bin/sh
set -e

echo "[INIT] =========================================="
echo "[INIT] Inicializando entorno para Oracle Wallet"
echo "[INIT] =========================================="

if [ ! -z "$WALLET_BASE64" ]; then
    echo "[INIT] Decodificando WALLET_BASE64..."
    echo "$WALLET_BASE64" | base64 -d > /tmp/wallet.zip
    
    echo "[INIT] Extrayendo wallet..."
    unzip -o /tmp/wallet.zip -d /app/wallet/
    rm /tmp/wallet.zip
    
    echo "[INIT] Archivos del wallet:"
    ls -la /app/wallet/
    echo "[INIT] Wallet decodificado y extraído con éxito."
else
    echo "[WARN] WALLET_BASE64 no está configurado"
fi

if [ ! -z "$ORACLE_TNSNAMES" ]; then
    echo "[INIT] Generando tnsnames.ora..."
    echo "$ORACLE_TNSNAMES" > /app/wallet/tnsnames.ora
    echo "[INIT] tnsnames.ora generado con éxito."
fi

if [ ! -z "$ORACLE_SQLNET" ]; then
    echo "[INIT] Generando sqlnet.ora..."
    echo "$ORACLE_SQLNET" > /app/wallet/sqlnet.ora
    echo "[INIT] sqlnet.ora generado con éxito."
fi

# Permisos correctos para los archivos del wallet
chmod 600 /app/wallet/*
echo "[INIT] Permisos del wallet configurados"

echo "[INIT] =========================================="
echo "[INIT] Iniciando aplicación Spring Boot..."
echo "[INIT] =========================================="

exec java $JAVA_OPTS -jar app.jar
EOF

RUN chmod +x /app/entrypoint.sh
EXPOSE 8080
ENTRYPOINT ["/app/entrypoint.sh"]

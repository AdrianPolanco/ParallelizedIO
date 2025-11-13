#!/bin/sh

echo "Esperando a que MinIO esté disponible..."
sleep 5

# Configurar alias mc (usa el nombre del servicio en docker-compose)
echo "Configurando alias de MinIO..."
until mc alias set myminio http://minio:9000 admin admin1234 2>/dev/null; do
  echo "Reintentando conexión con MinIO..."
  sleep 2
done

echo "MinIO listo, aplicando configuración..."

# Crear el bucket si no existe
mc mb myminio/demo --ignore-existing

# LIMPIAR eventos previos del bucket (esto evita el conflicto)
echo "Limpiando eventos previos..."
mc event remove myminio/demo arn:minio:sqs::1:webhook --event put --force 2>/dev/null || true

# Configurar webhook para eventos de bucket
mc admin config set myminio notify_webhook:1 \
  endpoint="http://host.docker.internal:8080/files/upload/webhook" \
  queue_limit="10" \
  auth_token="Bearer 6jvX8^2uo#^Zc+Ve@1K@Rx6ZBt@9LfxRc8gd#os/wf^="

# Reiniciar MinIO sin TTY (usando kill -HUP)
echo "Reiniciando MinIO..."
docker exec my_minio sh -c 'kill -HUP 1' 2>/dev/null || mc admin service restart myminio --json 2>/dev/null || true

echo "Esperando reinicio de MinIO..."
sleep 10

# Reconfigurar alias después del reinicio
echo "Reconectando con MinIO..."
until mc alias set myminio http://minio:9000 admin admin1234 2>/dev/null; do
  echo "Reintentando conexión con MinIO después del reinicio..."
  sleep 2
done

# Configurar eventos del bucket para usar el webhook
echo "Configurando eventos del bucket..."
mc event add myminio/demo arn:minio:sqs::1:webhook --event put

echo "✓ Webhook configurado y vinculado al bucket 'demo'"
echo "✓ Eventos configurados: PUT (subida de archivos)"
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

# Configurar webhook para eventos de bucket
mc admin config set myminio notify_webhook:1 \
  endpoint="http://host.docker.internal:8080/files/upload/webhook" \
  auth_token="Bearer 6jvX8^2uo#^Zc+Ve@1K@Rx6ZBt@9LfxRc8gd#os/wf^=" \
  queue_limit="10"

# Reiniciar servicio para aplicar configuración
mc admin service restart myminio

echo "Esperando reinicio de MinIO..."
sleep 5

# Configurar eventos del bucket para usar el webhook
mc event add myminio/demo arn:minio:sqs::1:webhook --event put

echo "Webhook configurado y vinculado al bucket 'demo'"
echo "Eventos configurados: PUT (subida de archivos)"
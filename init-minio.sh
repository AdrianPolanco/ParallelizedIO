#!/bin/sh
set -e

echo "Esperando a que MinIO esté disponible..."
until curl -s http://localhost:9000/minio/health/ready > /dev/null; do
  sleep 1
done

echo "MinIO listo, aplicando configuración..."
mc alias set myminio http://localhost:9000 admin admin1234
mc admin config set myminio notify_webhook:1 \
  endpoint="http://host.docker.internal:8080/files/upload/webhook" \
  queue_limit="10"

echo "Reiniciando MinIO..."
mc admin service restart myminio --json || true

echo "Configuración aplicada y reinicio ejecutado."
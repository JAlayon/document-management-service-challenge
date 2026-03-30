#!/bin/sh
set -e

echo "⏳ Waiting for MinIO..."

until mc alias set local http://minio:9000 "$MINIO_ROOT_USER" "$MINIO_ROOT_PASSWORD"; do
  sleep 2
done

if mc ls local/"$MINIO_BUCKET" >/dev/null 2>&1; then
  echo "Bucket exists"
else
  echo "Creating bucket"
  mc mb -p local/"$MINIO_BUCKET"
fi

echo "✅ Ready"
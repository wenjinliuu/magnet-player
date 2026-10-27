#!/usr/bin/env bash
# 下载 TorrServer 官方 android-arm64 二进制，校验 SHA-256，放到 jniLibs 里命名为 libtorrserver.so
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PROPS="$ROOT/torrserver.properties"
VERSION="$(grep '^version=' "$PROPS" | cut -d= -f2)"
SHA256="$(grep '^sha256=' "$PROPS" | cut -d= -f2)"
DEST_DIR="$ROOT/app/src/main/jniLibs/arm64-v8a"
DEST="$DEST_DIR/libtorrserver.so"
URL="https://github.com/YouROK/TorrServer/releases/download/${VERSION}/TorrServer-android-arm64"

if [ -f "$DEST" ] && echo "$SHA256  $DEST" | sha256sum -c --status; then
  echo "TorrServer $VERSION 已存在，校验通过"
  exit 0
fi

mkdir -p "$DEST_DIR"
echo "下载 TorrServer $VERSION: $URL"
curl -fL --retry 4 --retry-delay 2 -o "$DEST.tmp" "$URL"
if ! echo "$SHA256  $DEST.tmp" | sha256sum -c -; then
  echo "::error::TorrServer SHA-256 校验失败，实际值：$(sha256sum "$DEST.tmp" | cut -d' ' -f1)"
  rm -f "$DEST.tmp"
  exit 1
fi
mv "$DEST.tmp" "$DEST"
echo "已放入 $DEST"

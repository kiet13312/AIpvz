#!/usr/bin/env bash
set -euo pipefail

# Download small CC0 OpenGameArt sprite sheets. The APK build contains the images,
# so the installed game does not need an internet connection to display them.
OUT="app/src/main/res/drawable-nodpi"
mkdir -p "$OUT"

download_asset() {
  local url="$1"
  local target="$2"
  if curl -L --fail --silent --show-error --retry 3 --connect-timeout 15 --max-time 90 \
      -A "GardenDefense asset build" "$url" -o "$OUT/$target"; then
    if file "$OUT/$target" | grep -qi 'PNG image'; then
      echo "Downloaded $target"
    else
      echo "Warning: $target was not a PNG; using existing in-game fallback instead."
      rm -f "$OUT/$target"
    fi
  else
    echo "Warning: could not download $target; using existing in-game fallback instead."
    rm -f "$OUT/$target"
  fi
}

download_asset "https://opengameart.org/sites/default/files/Vegetables_0.png" "pult_vegetables_sheet.png"
download_asset "https://opengameart.org/sites/default/files/Cereals_0.png" "pult_cereals_sheet.png"
download_asset "https://opengameart.org/sites/default/files/Fruits_1.png" "pult_fruits_sheet.png"

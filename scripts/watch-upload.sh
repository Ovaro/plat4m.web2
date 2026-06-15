#!/bin/bash

set -euo pipefail

if [ "$#" -ne 3 ]; then
    echo "Usage:"
    echo "  $0 <source_file> <staging_dir> <upload_url>"
    echo
    echo "Example:"
    echo "  export BEARER_TOKEN=\"eyJhbGciOi...\""
    echo "  $0 ~/Documents/report.pdf ~/upload-staging https://example.com/upload"
    exit 1
fi

# Verify required environment variable exists
: "${BEARER_TOKEN:?BEARER_TOKEN environment variable is required}"

SOURCE_FILE="$1"
STAGING_DIR="$2"
UPLOAD_URL="$3"

if [ ! -f "$SOURCE_FILE" ]; then
    echo "Error: Source file does not exist: $SOURCE_FILE"
    exit 1
fi

mkdir -p "$STAGING_DIR"

echo "Watching: $SOURCE_FILE"
echo "Staging:  $STAGING_DIR"
echo "Upload:   $UPLOAD_URL$MNY_PASSWORD"
echo "Bearer Token: [configured]"

fswatch -0 "$SOURCE_FILE" | while read -d "" event
do
    echo "$(date) - Change detected"

    # Allow file writes to complete
    sleep 2

    DEST_FILE="$STAGING_DIR/$(basename "$SOURCE_FILE")"

    cp "$SOURCE_FILE" "$DEST_FILE"

    echo "$(date) - Uploading $DEST_FILE"

    curl --fail \
         --silent \
         --show-error \
         -X POST \
         -H "Authorization: Bearer $BEARER_TOKEN" \
         -F "file=@$DEST_FILE" \
         "$UPLOAD_URL$MNY_PASSWORD"

    echo "$(date) - Upload complete"
done

#!/bin/bash

# --- PATH CONFIGURATION ---
CREDENTIALS_FILE="aws/credentials"
TARGET_FILE="src/main/resources/aws_credentials.txt"

# --- 1. FILE EXISTENCE CHECK ---
if [ ! -f "$CREDENTIALS_FILE" ]; then
    echo "Error: Credentials file not found at the specified path: $CREDENTIALS_FILE" >&2
    echo "Please check that you are running the script from the correct directory." >&2
    exit 1
fi

# --- 2. KEY VALUE EXTRACTION (Assuming INI format under [default] profile) ---
echo "Starting credentials extraction..."

# 1. Extracts aws_access_key_id:
# The pipeline filters the line, cuts at the '=' sign, and 'tr' removes all whitespace.
KEY_ID=$(grep 'aws_access_key_id' "$CREDENTIALS_FILE" | head -1 | cut -d '=' -f 2 | tr -d '[:space:]')

# 2. Extracts aws_secret_access_key in the same way:
SECRET_KEY=$(grep 'aws_secret_access_key' "$CREDENTIALS_FILE" | head -1 | cut -d '=' -f 2 | tr -d '[:space:]')

# --- 3. VALIDITY AND CLEANUP CHECK ---

if [ -z "$KEY_ID" ] || [ -z "$SECRET_KEY" ]; then
    echo "Error: Could not extract one or both keys from $CREDENTIALS_FILE." >&2
    echo "Verify keys are present, uncommented, and formatted as 'key = value'." >&2
    exit 1
fi

# --- 4. WRITING TO TARGET FILE (Overwrite) ---

echo "Writing to file $TARGET_FILE..."

# Writes the first line (overwrites the file if it exists with >)
echo "$KEY_ID" > "$TARGET_FILE"

# Writes the second line (appends to the file with >>)
echo "$SECRET_KEY" >> "$TARGET_FILE"

echo "Done! Keys have been successfully written and the target file overwritten."
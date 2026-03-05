#!/bin/bash

# Define the secrets step
SECRETS_STEP="      - name: Decode Keystore and Setup Secrets\n        env:\n          ENCODED_KEYSTORE: \${{ secrets.ENCODED_KEYSTORE }}\n          CLIENT_SECRETS_TEXT: \${{ secrets.CLIENT_SECRETS_TEXT }}\n        run: |\n          if [ ! -z \"\$ENCODED_KEYSTORE\" ]; then\n            echo \"\$ENCODED_KEYSTORE\" | base64 --decode > keystore.jks\n          fi\n          if [ ! -z \"\$CLIENT_SECRETS_TEXT\" ]; then\n            mkdir -p app/src/main/assets\n            echo \"\$CLIENT_SECRETS_TEXT\" > app/src/main/assets/client_secrets.json\n          fi\n"

# Define the env step
ENV_STEP="\n        env:\n          KEYSTORE_FILE: \${{ github.workspace }}/keystore.jks\n          KEYSTORE_PASSWORD: \${{ secrets.KEYSTORE_PASSWORD }}\n          KEY_ALIAS: \${{ secrets.KEY_ALIAS }}\n          KEY_PASSWORD: \${{ secrets.KEY_PASSWORD }}"

# Function to patch a workflow
patch_workflow() {
    local file=$1
    local build_cmd=$2

    # Insert secrets step before the build app step
    sed -i "s|      - name: Build app|${SECRETS_STEP}\n      - name: Build app|g" "$file"

    # Append the environment variables block to the build command
    sed -i "s|        run: ./gradlew ${build_cmd}|        run: ./gradlew ${build_cmd}${ENV_STEP}|g" "$file"

    # Fix the upload path for release APK
    sed -i "s/app-arm64-v8a-release-unsigned.apk/*.apk/g" "$file"
}

patch_workflow ".github/workflows/Main.yml" "assembleDebug"
patch_workflow ".github/workflows/ci.yml" "assembleDebug"
patch_workflow ".github/workflows/build_pull_request.yml" "assembleRelease"

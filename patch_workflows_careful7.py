import re

workflows = [
    ".github/workflows/Main.yml",
    ".github/workflows/ci.yml",
    ".github/workflows/build_pull_request.yml"
]

secrets_step = """      - name: Decode Keystore and Setup Secrets
        env:
          ENCODED_KEYSTORE: ${{ secrets.ENCODED_KEYSTORE }}
          CLIENT_SECRETS_TEXT: ${{ secrets.CLIENT_SECRETS_TEXT }}
        run: |
          if [ ! -z "$ENCODED_KEYSTORE" ]; then
            echo "$ENCODED_KEYSTORE" | base64 --decode > keystore.jks
          fi
          if [ ! -z "$CLIENT_SECRETS_TEXT" ]; then
            mkdir -p app/src/main/assets
            echo "$CLIENT_SECRETS_TEXT" > app/src/main/assets/client_secrets.json
          fi

"""

env_str = """
        env:
          KEYSTORE_FILE: ${{ github.workspace }}/keystore.jks
          KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
          KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
          KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}"""

for workflow in workflows:
    with open(workflow, "r") as f:
        content = f.read()

    # Apply secrets_step right before "- name: Build app" block.
    # Use exact match so it doesn't get messed up.
    if "Decode Keystore" not in content:
        content = content.replace("      - name: Build app", secrets_step + "      - name: Build app")

    # The issue in build_pull_request is likely that it says "run: ./gradlew assemble" not assembleRelease?
    # No, it was replaced earlier by mistake.

    # We use regex to replace exactly the full line and append env_str
    content = re.sub(r"(        run: \./gradlew assembleDebug)\n", r"\1" + env_str + "\n", content)
    content = re.sub(r"(        run: \./gradlew assembleRelease)\n", r"\1" + env_str + "\n", content)

    if "path: app/build/outputs/apk/release/app-arm64-v8a-release-unsigned.apk" in content:
        content = content.replace("path: app/build/outputs/apk/release/app-arm64-v8a-release-unsigned.apk", "path: app/build/outputs/apk/release/*.apk")

    with open(workflow, "w") as f:
        f.write(content)

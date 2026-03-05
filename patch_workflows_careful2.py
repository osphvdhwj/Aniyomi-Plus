import re

workflows = [
    ".github/workflows/Main.yml",
    ".github/workflows/ci.yml",
    ".github/workflows/build_pull_request.yml"
]

secrets_step = """
      - name: Decode Keystore and Setup Secrets
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

env_str = "\n        env:\n          KEYSTORE_FILE: ${{ github.workspace }}/keystore.jks\n          KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}\n          KEY_ALIAS: ${{ secrets.KEY_ALIAS }}\n          KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}\n"

for workflow in workflows:
    with open(workflow, "r") as f:
        content = f.read()

    # Apply secrets_step right before "- name: Build app"
    if "Decode Keystore" not in content:
        content = re.sub(r"      - name: Build app", secrets_step + "      - name: Build app", content)

    # Insert env_str using regex to specifically match the full line
    content = re.sub(r"(        run: \./gradlew assembleDebug)\n", r"\1" + env_str, content)
    content = re.sub(r"(        run: \./gradlew assembleRelease)\n", r"\1" + env_str, content)

    if "path: app/build/outputs/apk/release/app-arm64-v8a-release-unsigned.apk" in content:
        content = content.replace("path: app/build/outputs/apk/release/app-arm64-v8a-release-unsigned.apk", "path: app/build/outputs/apk/release/*.apk")

    with open(workflow, "w") as f:
        f.write(content)

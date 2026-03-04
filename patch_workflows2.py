import re

# Refine workflow patches
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

for workflow in workflows:
    with open(workflow, "r") as f:
        content = f.read()

    # Find the "- name: Build app" block and replace it
    if "Decode Keystore" not in content:
        content = content.replace("      - name: Build app\n        run: ./gradlew assembleDebug",
                                  secrets_step + "\n      - name: Build app\n        env:\n          KEYSTORE_FILE: ${{ github.workspace }}/keystore.jks\n          KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}\n          KEY_ALIAS: ${{ secrets.KEY_ALIAS }}\n          KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}\n        run: ./gradlew assembleDebug")

        content = content.replace("      - name: Build app\n        run: ./gradlew assembleRelease",
                                  secrets_step + "\n      - name: Build app\n        env:\n          KEYSTORE_FILE: ${{ github.workspace }}/keystore.jks\n          KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}\n          KEY_ALIAS: ${{ secrets.KEY_ALIAS }}\n          KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}\n        run: ./gradlew assembleRelease")

    if "path: app/build/outputs/apk/release/app-arm64-v8a-release-unsigned.apk" in content:
        content = content.replace("path: app/build/outputs/apk/release/app-arm64-v8a-release-unsigned.apk", "path: app/build/outputs/apk/release/*.apk")

    with open(workflow, "w") as f:
        f.write(content)

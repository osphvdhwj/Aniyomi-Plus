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

    # Apply secrets_step right before "- name: Build app" block.
    # Use replace to be safe.
    if "Decode Keystore" not in content:
        content = content.replace("      - name: Build app", secrets_step + "      - name: Build app")

    # Add environment block
    content = content.replace("        run: ./gradlew assembleDebug\n", "        run: ./gradlew assembleDebug" + env_str)
    content = content.replace("        run: ./gradlew assembleRelease\n", "        run: ./gradlew assembleRelease" + env_str)

    if "path: app/build/outputs/apk/release/app-arm64-v8a-release-unsigned.apk" in content:
        content = content.replace("path: app/build/outputs/apk/release/app-arm64-v8a-release-unsigned.apk", "path: app/build/outputs/apk/release/*.apk")

    with open(workflow, "w") as f:
        f.write(content)

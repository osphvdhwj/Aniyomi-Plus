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

env_str_debug = """        run: ./gradlew assembleDebug
        env:
          KEYSTORE_FILE: ${{ github.workspace }}/keystore.jks
          KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
          KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
          KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}"""

env_str_release = """        run: ./gradlew assembleRelease
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

    # Do replacing
    if "        run: ./gradlew assembleDebug\n" in content:
        content = content.replace("        run: ./gradlew assembleDebug\n", env_str_debug + "\n")
    if "        run: ./gradlew assembleRelease\n" in content:
        content = content.replace("        run: ./gradlew assembleRelease\n", env_str_release + "\n")

    if "path: app/build/outputs/apk/release/app-arm64-v8a-release-unsigned.apk" in content:
        content = content.replace("path: app/build/outputs/apk/release/app-arm64-v8a-release-unsigned.apk", "path: app/build/outputs/apk/release/*.apk")

    with open(workflow, "w") as f:
        f.write(content)

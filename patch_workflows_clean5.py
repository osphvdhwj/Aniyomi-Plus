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

for workflow in workflows:
    with open(workflow, "r") as f:
        content = f.read()

    # Find the EXACT build app string and replace it.

    # Debug build (Main.yml and ci.yml)
    old_debug_step = """      - name: Build app
        run: ./gradlew assembleDebug"""
    new_debug_step = secrets_step + """      - name: Build app
        env:
          KEYSTORE_FILE: ${{ github.workspace }}/keystore.jks
          KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
          KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
          KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
        run: ./gradlew assembleDebug"""

    content = content.replace(old_debug_step, new_debug_step)

    # Release build (build_pull_request.yml)
    old_release_step = """      - name: Build app
        run: ./gradlew assembleRelease"""
    new_release_step = secrets_step + """      - name: Build app
        env:
          KEYSTORE_FILE: ${{ github.workspace }}/keystore.jks
          KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
          KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
          KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
        run: ./gradlew assembleRelease"""

    content = content.replace(old_release_step, new_release_step)

    # Replace apk path
    content = content.replace("app-arm64-v8a-release-unsigned.apk", "*.apk")

    with open(workflow, "w") as f:
        f.write(content)

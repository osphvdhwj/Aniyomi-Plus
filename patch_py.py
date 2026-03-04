import sys

workflows = [
    (".github/workflows/Main.yml", "assembleDebug"),
    (".github/workflows/ci.yml", "assembleDebug"),
    (".github/workflows/build_pull_request.yml", "assembleRelease")
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

env_step = """
        env:
          KEYSTORE_FILE: ${{ github.workspace }}/keystore.jks
          KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
          KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
          KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}"""

for file, build_cmd in workflows:
    with open(file, "r") as f:
        content = f.read()

    # Step 1: Add secrets step before "- name: Build app"
    # But note: The job itself is named "name: Build app"!
    # Oh! THAT'S WHY IT WAS MATCHING THE JOB NAME!

    # We should match:
    # "      - name: Build app\n        run: ./gradlew assembleDebug"

    old_step = f"      - name: Build app\n        run: ./gradlew {build_cmd}"
    new_step = secrets_step + f"      - name: Build app\n        run: ./gradlew {build_cmd}" + env_step

    content = content.replace(old_step, new_step)

    # Step 2: Fix unsigned APK path
    content = content.replace("app-arm64-v8a-release-unsigned.apk", "*.apk")

    with open(file, "w") as f:
        f.write(content)

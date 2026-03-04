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

    # Just put secrets_step before "name: Check code format"
    # Actually, that's better! Just inject it before `run: ./gradlew assemble`

    # Wait, the problem is python's script is being run MULTIPLE TIMES in my previous iterations! Oh, no it is restoring.
    # Ah, look at my previous script. It has:
    # content.replace("app-arm64-v8a-release-unsigned.apk", "*.apk")
    # This was mutating `- name: Upload APK \n path: app/build/outputs/apk/release/app-arm64-v8a-release-unsigned.apk`

    # BUT WHY IS `run: ./gradlew assembleDebug` becoming `run: ./gradlew assemble...Release`?
    # Because my script was:
    # content = content.replace("app-arm64-v8a-release-unsigned.apk", "*.apk")
    # Wait, no. Look at `patch_workflows_clean6.py`.
    # content = content.replace("      - name: Build app\n        run: ./gradlew assembleRelease\n", secrets_step + ...)
    # Wait. Look at the FIRST file in the list. `Main.yml`. Then `ci.yml`. Then `build_pull_request.yml`.
    # Ah! In build_pull_request.yml, `run: ./gradlew assembleRelease` was being replaced by `run: ./gradlew assembleRelease\n` but the rest of the file... no.
    # Let's use `sed`!

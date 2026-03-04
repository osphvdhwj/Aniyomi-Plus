import sys

file = ".github/workflows/build_pull_request.yml"
with open(file, "r") as f:
    content = f.read()

old_step = "      - name: Build app\n        run: ./gradlew assembleRelease"
new_step = """      - name: Decode Keystore and Setup Secrets
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

      - name: Build app
        env:
          KEYSTORE_FILE: ${{ github.workspace }}/keystore.jks
          KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
          KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
          KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
        run: ./gradlew assembleRelease"""

content = content.replace(old_step, new_step)

# Wait... in previous iterations I did:
# `content.replace("app-arm64-v8a-release-unsigned.apk", "*.apk")`
# IS THERE ANOTHER `Release` IN THE STRING?
# `app-arm64-v8a-release-unsigned.apk` -> `app-arm64-v8a-*.apk`?
# NO... wait! "Release" at the end of "KEY_PASSWORD: ...Release".
# Ah! Look at the earlier string:
# `run: ./gradlew assembleRelease`!
# IF I replace "Release" globally or something? I am not doing that!

content = content.replace("app-arm64-v8a-release-unsigned.apk", "*.apk")

with open(file, "w") as f:
    f.write(content)

import os

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

env_step = """
        env:
          KEYSTORE_FILE: ${{ github.workspace }}/keystore.jks
          KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
          KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
          KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}"""

for workflow in workflows:
    with open(workflow, "r") as f:
        content = f.read()

    if "Decode Keystore and Setup Secrets" not in content:
        # Insert secrets step before building
        build_step_marker = "- name: Build app\n        run: ./gradlew assemble"
        if build_step_marker in content:
            content = content.replace(build_step_marker, secrets_step + "\n      " + build_step_marker + env_step)

    # For PR workflow it builds assembleRelease
    build_release_marker = "- name: Build app\n        run: ./gradlew assembleRelease"
    if build_release_marker in content and "Decode Keystore and Setup Secrets" not in content:
        content = content.replace(build_release_marker, secrets_step + "\n      " + build_release_marker + env_step)

    # In build_pull_request.yml it uploads unsigned APK
    if "app-arm64-v8a-release-unsigned.apk" in content:
        content = content.replace("app-arm64-v8a-release-unsigned.apk", "app-arm64-v8a-release*.apk")
        content = content.replace("name: arm64-v8a-${{ github.sha }}", "name: arm64-v8a-${{ github.sha }}\n          pattern: app-arm64-v8a-release*.apk\n          merge-multiple: true")
        # Let's just use the path with wildcard and no pattern, since older action version doesn't support pattern.
        # It's better to just replace `unsigned` with `*` or remove `unsigned` if signing works.
        content = content.replace("path: app/build/outputs/apk/release/app-arm64-v8a-release-unsigned.apk", "path: app/build/outputs/apk/release/*.apk")

    with open(workflow, "w") as f:
        f.write(content)

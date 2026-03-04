import sys

workflows = [
    (".github/workflows/Main.yml", "assembleDebug"),
    (".github/workflows/ci.yml", "assembleDebug"),
    (".github/workflows/build_pull_request.yml", "assembleRelease")
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

env_step = """
        env:
          KEYSTORE_FILE: ${{ github.workspace }}/keystore.jks
          KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
          KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
          KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}"""

for file, build_cmd in workflows:
    with open(file, "r") as f:
        content = f.read()

    # I NEED TO ONLY DO THIS ONCE! My previous attempts failed because `content.replace("run: ./gradlew assembleDebug", ...)` matched multiple times or something?
    # No, look at the result: "run: ./gradlew assemble\n        env: ...Release"
    # This means `content.replace("app-arm64-v8a-release-unsigned.apk", "*.apk")` was also replacing something else? No...
    # Where does `Release` come from at the end of `KEY_PASSWORD`?
    # Ah! `content.replace(old_step, new_step)` where `old_step` = `... assemble` and then `new_step` has `assemble` and `env_step` which DOES NOT HAVE `Release`!
    # Ahhhh! The python script was replacing just `assemble` because I probably missed the `Debug` vs `Release` in my python script in previous iterations?!
    # No, look at `build_cmd`. For `build_pull_request.yml`, `build_cmd` is "assembleRelease".
    # So `old_step` is "      - name: Build app\n        run: ./gradlew assembleRelease"
    # `new_step` is secrets + "- name: Build app\n run: ./gradlew assembleRelease" + env_step
    # BUT wait! If `env_step` is placed AFTER `run: ./gradlew assembleRelease`, the line becomes `run: ./gradlew assembleRelease\n        env:\n ...`
    # BUT wait again! I was previously using `content.replace("run: ./gradlew assemble", ...)` somewhere!
    # Let's write the exact text manually in the script.

    old_step = "      - name: Build app\n        run: ./gradlew " + build_cmd
    new_step = secrets_step + "      - name: Build app\n        run: ./gradlew " + build_cmd + env_step

    content = content.replace(old_step, new_step)

    content = content.replace("app-arm64-v8a-release-unsigned.apk", "*.apk")

    with open(file, "w") as f:
        f.write(content)

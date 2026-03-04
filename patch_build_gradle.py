import sys

file = "app/build.gradle.kts"
with open(file, "r") as f:
    content = f.read()

signing_config = """
    signingConfigs {
        create("release") {
            val keystoreFile = System.getenv("KEYSTORE_FILE")
            if (keystoreFile != null) {
                storeFile = file(keystoreFile)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            } else {
                // Dummy values for local builds without secrets
                storeFile = file("dummy.jks")
                storePassword = "dummy"
                keyAlias = "dummy"
                keyPassword = "dummy"
            }
        }
    }
"""

# Insert signingConfigs before buildTypes
if "signingConfigs {" not in content:
    content = content.replace("    buildTypes {", signing_config + "\n    buildTypes {")

# Apply signing config to release and preview
if "signingConfig = signingConfigs.getByName(\"release\")" not in content:
    content = content.replace(
        "val release by getting {",
        "val release by getting {\n            signingConfig = signingConfigs.getByName(\"release\")"
    )

with open(file, "w") as f:
    f.write(content)

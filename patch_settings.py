import sys

def main():
    filepath = './app/src/main/java/eu/kanade/presentation/more/settings/screen/SettingsAppearanceScreen.kt'
    with open(filepath, 'r') as f:
        content = f.read()

    # Fix pref = -> preference =
    content = content.replace("pref = uiPreferences.bottomNavStyle()", "preference = uiPreferences.bottomNavStyle()")

    # Add import for persistentMapOf if not present
    if "import kotlinx.collections.immutable.persistentMapOf" not in content:
        content = content.replace("import kotlinx.collections.immutable.persistentListOf", "import kotlinx.collections.immutable.persistentListOf\nimport kotlinx.collections.immutable.persistentMapOf")

    with open(filepath, 'w') as f:
        f.write(content)

    print("Patched SettingsAppearanceScreen.kt")

if __name__ == '__main__':
    main()

import sys

def main():
    filepath = './app/src/main/java/eu/kanade/tachiyomi/ui/library/AniMaTab.kt'
    with open(filepath, 'r') as f:
        content = f.read()

    # Need to add import cafe.adriel.voyager.navigator.tab.TabOptions
    if "import cafe.adriel.voyager.navigator.tab.TabOptions" not in content:
        content = content.replace("import eu.kanade.presentation.util.Tab", "import eu.kanade.presentation.util.Tab\nimport cafe.adriel.voyager.navigator.tab.TabOptions")

    with open(filepath, 'w') as f:
        f.write(content)

    print("Patched AniMaTab.kt")

if __name__ == '__main__':
    main()

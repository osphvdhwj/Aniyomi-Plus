import sys

def main():
    filepath = './app/src/main/java/eu/kanade/tachiyomi/ui/library/AniMaTab.kt'
    with open(filepath, 'r') as f:
        content = f.read()

    # Change cafe.adriel.voyager.navigator.tab.Tab to eu.kanade.presentation.util.Tab
    content = content.replace("import cafe.adriel.voyager.navigator.tab.Tab", "import eu.kanade.presentation.util.Tab")

    with open(filepath, 'w') as f:
        f.write(content)

    print("Patched AniMaTab.kt")

if __name__ == '__main__':
    main()

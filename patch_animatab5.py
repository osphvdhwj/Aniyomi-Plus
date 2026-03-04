import sys

def main():
    filepath = './app/src/main/java/eu/kanade/tachiyomi/ui/library/AniMaTab.kt'
    with open(filepath, 'r') as f:
        content = f.read()

    # Need to remove one of the conflicting 'Tab' imports
    # Specifically `import cafe.adriel.voyager.navigator.tab.Tab` shouldn't be there because we want `eu.kanade.presentation.util.Tab`
    lines = content.split('\n')
    cleaned_lines = []

    for line in lines:
        if line == 'import cafe.adriel.voyager.navigator.tab.Tab':
            continue # skip this one
        cleaned_lines.append(line)

    with open(filepath, 'w') as f:
        f.write('\n'.join(cleaned_lines))

    print("Patched AniMaTab.kt imports (removed conflicting Tab)")

if __name__ == '__main__':
    main()

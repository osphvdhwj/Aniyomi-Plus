import sys

def main():
    filepath = './app/src/main/java/eu/kanade/tachiyomi/ui/library/AniMaTab.kt'
    with open(filepath, 'r') as f:
        content = f.read()

    # Need to remove the duplicate `import eu.kanade.presentation.util.Tab`
    lines = content.split('\n')
    cleaned_lines = []

    seen_tab_import = False

    for line in lines:
        if line == 'import eu.kanade.presentation.util.Tab':
            if not seen_tab_import:
                seen_tab_import = True
                cleaned_lines.append(line)
            # if seen already, skip
        else:
            cleaned_lines.append(line)

    with open(filepath, 'w') as f:
        f.write('\n'.join(cleaned_lines))

    print("Patched AniMaTab.kt imports (removed duplicate Tab import)")

if __name__ == '__main__':
    main()

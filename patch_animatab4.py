import sys

def main():
    filepath = './app/src/main/java/eu/kanade/tachiyomi/ui/library/AniMaTab.kt'
    with open(filepath, 'r') as f:
        content = f.read()

    # My previous replacement replaced "Tab" which caused "TabOptions" to become "eu.kanade.presentation.util.TabOptions"
    # because of Tab -> eu.kanade.presentation.util.Tab.
    # Actually wait, `import cafe.adriel.voyager.navigator.tab.TabOptions` was replaced when I did a replace on "import cafe.adriel.voyager.navigator.tab.Tab".
    # Let's clean up imports.

    imports_to_remove = [
        "import eu.kanade.presentation.util.Tab\nimport cafe.adriel.voyager.navigator.tab.TabOptionsOptions",
        "import eu.kanade.presentation.util.TabOptions"
    ]

    # Just rewrite the imports block to be safe
    lines = content.split('\n')
    cleaned_lines = []

    for line in lines:
        if line.startswith('import cafe.adriel.voyager.navigator.tab.Tab'):
            if line == 'import cafe.adriel.voyager.navigator.tab.TabOptions':
                cleaned_lines.append(line)
        elif line.startswith('import eu.kanade.presentation.util.Tab'):
            if line == 'import eu.kanade.presentation.util.Tab':
                cleaned_lines.append(line)
        else:
            cleaned_lines.append(line)

    # ensure both imports exist
    content = '\n'.join(cleaned_lines)

    if 'import eu.kanade.presentation.util.Tab\n' not in content:
        content = content.replace('import cafe.adriel.voyager.navigator.tab.TabOptions', 'import cafe.adriel.voyager.navigator.tab.TabOptions\nimport eu.kanade.presentation.util.Tab')

    # fix any weird TabOptionsOptions
    content = content.replace('TabOptionsOptions', 'TabOptions')

    with open(filepath, 'w') as f:
        f.write(content)

    print("Patched AniMaTab.kt imports again")

if __name__ == '__main__':
    main()

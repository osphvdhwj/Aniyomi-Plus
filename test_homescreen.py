import sys
import re

def main():
    filepath = './app/src/main/java/eu/kanade/tachiyomi/ui/home/HomeScreen.kt'
    with open(filepath, 'r') as f:
        content = f.read()

    # check if Imports are there
    imports = [
        "eu.kanade.tachiyomi.ui.updates.UpdatesTab",
        "eu.kanade.tachiyomi.ui.history.HistoriesTab",
        "eu.kanade.tachiyomi.ui.browse.BrowseTab",
        "eu.kanade.tachiyomi.ui.more.MoreTab"
    ]

    for i in imports:
        if i not in content:
            print(f"Missing import: {i}")
        else:
            print(f"Found import: {i}")

if __name__ == '__main__':
    main()

file = "app/src/main/java/eu/kanade/tachiyomi/ui/player/controls/GestureHandler.kt"
with open(file, "r") as f:
    lines = f.readlines()

for i, line in enumerate(lines):
    if ".pointerInput" in line:
        print(f"Line {i+1}: {line.strip()}")

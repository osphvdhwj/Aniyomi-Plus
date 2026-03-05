import re

file_path = "app/src/main/java/eu/kanade/tachiyomi/ui/player/controls/GestureHandler.kt"
with open(file_path, "r") as f:
    content = f.read()

# Modify the 2. Custom Detector
old_block = """                        val down = awaitFirstDown(requireUnconsumed = false)
                        val downTime = System.currentTimeMillis()
                        var isLongPress = false
                        var isHorizontalDrag = false
                        var totalDragDistanceX = 0f"""

new_block = """                        val down = awaitFirstDown(requireUnconsumed = false)
                        val downTime = System.currentTimeMillis()
                        var isLongPress = false
                        var isHorizontalDrag = false
                        var totalDragDistanceX = 0f

                        val screenWidth = size.width
                        val isCenterTouch = down.position.x > screenWidth * 0.2f && down.position.x < screenWidth * 0.8f"""

content = content.replace(old_block, new_block)

old_logic = """                                // A. Detect Long Press
                                if (!isLongPress &&
                                    !isHorizontalDrag &&
                                    timeElapsed > viewConfiguration.longPressTimeoutMillis
                                ) {"""

new_logic = """                                // A. Detect Long Press
                                if (isCenterTouch &&
                                    !isLongPress &&
                                    !isHorizontalDrag &&
                                    timeElapsed > viewConfiguration.longPressTimeoutMillis
                                ) {"""

content = content.replace(old_logic, new_logic)

with open(file_path, "w") as f:
    f.write(content)

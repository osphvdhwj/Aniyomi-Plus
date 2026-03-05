import re

file_path = "app/src/main/java/eu/kanade/tachiyomi/ui/player/controls/GestureHandler.kt"
with open(file_path, "r") as f:
    content = f.read()

# I will write a precise regex or string replacement.
old_block = """                var startingY = 0f
                var mpvVolumeStartingY = 0f
                var originalVolume = 0
                var originalMPVVolume = 0
                var originalBrightness = 0f

                detectVerticalDragGestures(
                    onDragStart = {
                        startingY = 0f
                        mpvVolumeStartingY = 0f
                        originalVolume = currentVolume
                        originalMPVVolume = currentMPVVolume
                        originalBrightness = currentBrightness
                    },
                    onDragEnd = { startingY = 0f },
                ) { change, amount ->"""

new_block = """                var startingY = 0f
                var mpvVolumeStartingY = 0f
                var originalVolume = 0
                var originalMPVVolume = 0
                var originalBrightness = 0f

                val screenWidth = size.width
                val touchSlop = viewConfiguration.touchSlop
                var isEdgeSwipe = false
                var hasPassedSlop = false

                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        startingY = offset.y
                        mpvVolumeStartingY = offset.y
                        originalVolume = currentVolume
                        originalMPVVolume = currentMPVVolume
                        originalBrightness = currentBrightness

                        isEdgeSwipe = offset.x < screenWidth * 0.2f || offset.x > screenWidth * 0.8f
                        hasPassedSlop = false
                    },
                    onDragEnd = { startingY = 0f },
                    onDragCancel = { startingY = 0f },
                ) { change, amount ->
                    if (!isEdgeSwipe) return@detectVerticalDragGestures

                    val currentY = change.position.y
                    val dragDistance = startingY - currentY

                    if (!hasPassedSlop && kotlin.math.abs(dragDistance) > touchSlop) {
                        hasPassedSlop = true
                    }

                    if (!hasPassedSlop) return@detectVerticalDragGestures
"""

content = content.replace(old_block, new_block)

# Since startingY is set in onDragStart to the exact offset.y,
# if (startingY == 0f) startingY = change.position.y is dead code.
# The previous code set startingY=0 in onDragStart, meaning it delayed setting startingY to the FIRST drag event.
# With touch slop, startingY is already accurate from the touch down event.
content = content.replace(
"""                        if (isIncreasingVolumeBoost || isDecreasingVolumeBoost) {
                            if (mpvVolumeStartingY == 0f) {
                                startingY = 0f
                                originalVolume = currentVolume
                                mpvVolumeStartingY = change.position.y
                            }""",
"""                        if (isIncreasingVolumeBoost || isDecreasingVolumeBoost) {
                            // Already handled dynamically by the gesture limits
                            """
)

content = content.replace(
"""                        } else {
                            if (startingY == 0f) {
                                mpvVolumeStartingY = 0f
                                originalMPVVolume = currentMPVVolume
                                startingY = change.position.y
                            }""",
"""                        } else {
                            // Handled correctly"""
)

content = content.replace(
"""                    val changeBrightness = {
                        if (startingY == 0f) startingY = change.position.y
                        viewModel.changeBrightnessTo(""",
"""                    val changeBrightness = {
                        viewModel.changeBrightnessTo("""
)


with open(file_path, "w") as f:
    f.write(content)

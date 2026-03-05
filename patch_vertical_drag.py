import re

file_path = "app/src/main/java/eu/kanade/tachiyomi/ui/player/controls/GestureHandler.kt"
with open(file_path, "r") as f:
    content = f.read()

# Replace variables before detectVerticalDragGestures
old_vars = """
                var startingY = 0f
                var mpvVolumeStartingY = 0f
                var originalVolume = 0
                var originalMPVVolume = 0
                var originalBrightness = 0f

                detectVerticalDragGestures("""

new_vars = """
                var startingY = 0f
                var mpvVolumeStartingY = 0f
                var originalVolume = 0
                var originalMPVVolume = 0
                var originalBrightness = 0f

                val screenWidth = size.width
                val touchSlop = viewConfiguration.touchSlop
                var isEdgeSwipe = false
                var hasPassedSlop = false

                detectVerticalDragGestures("""

content = content.replace(old_vars, new_vars)

# Replace onDragStart
old_onDragStart = """
                    onDragStart = {
                        startingY = 0f
                        mpvVolumeStartingY = 0f
                        originalVolume = currentVolume
                        originalMPVVolume = currentMPVVolume
                        originalBrightness = currentBrightness
                    },
                    onDragEnd = { startingY = 0f },
                ) { change, amount ->"""

new_onDragStart = """
                    onDragStart = { offset ->
                        startingY = offset.y
                        mpvVolumeStartingY = offset.y
                        originalVolume = currentVolume
                        originalMPVVolume = currentMPVVolume
                        originalBrightness = currentBrightness

                        // Lock this gesture entirely to the left 20% and right 20%
                        isEdgeSwipe = offset.x < screenWidth * 0.2f || offset.x > screenWidth * 0.8f
                        hasPassedSlop = false
                    },
                    onDragEnd = { startingY = 0f },
                    onDragCancel = { startingY = 0f },
                ) { change, amount ->
                    if (!isEdgeSwipe) return@detectVerticalDragGestures

                    val currentY = change.position.y
                    val dragDistance = startingY - currentY

                    // Ensure the drag passes the physical hardware threshold before reacting
                    if (!hasPassedSlop && kotlin.math.abs(dragDistance) > touchSlop) {
                        hasPassedSlop = true
                    }

                    if (!hasPassedSlop) return@detectVerticalDragGestures
"""

content = content.replace(old_onDragStart, new_onDragStart)

# Make sure startingY=0f checks inside changeVolume and changeBrightness don't reset it to change.position.y
# Since startingY is now initialized in onDragStart, we can remove the `if (startingY == 0f) startingY = change.position.y` logic.
content = content.replace("if (startingY == 0f) startingY = change.position.y", "")
content = content.replace("if (mpvVolumeStartingY == 0f) {\n                                startingY = 0f\n                                originalVolume = currentVolume\n                                mpvVolumeStartingY = change.position.y\n                            }", "")
content = content.replace("if (startingY == 0f) {\n                                mpvVolumeStartingY = 0f\n                                originalMPVVolume = currentMPVVolume\n                                startingY = change.position.y\n                            }", "")

# Wait! Let's just fix the whole block so it matches the current implementation precisely without breaking the original calculations!

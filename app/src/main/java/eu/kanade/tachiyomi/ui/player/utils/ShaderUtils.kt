package eu.kanade.tachiyomi.ui.player.utils

import android.content.Context
import java.io.File
import java.io.FileOutputStream

object ShaderUtils {
    private const val SHADER_DIR = "shaders"

    fun getShaderPath(context: Context, filename: String): String {
        return File(context.filesDir, "$SHADER_DIR/$filename").absolutePath
    }

    fun copyShadersIfNeeded(context: Context) {
        val shaderDir = File(context.filesDir, SHADER_DIR)
        if (!shaderDir.exists()) {
            shaderDir.mkdirs()
        }

        try {
            val assets = context.assets.list(SHADER_DIR) ?: return
            for (asset in assets) {
                val file = File(shaderDir, asset)
                // Simple check: if file doesn't exist, copy.
                if (!file.exists()) {
                    context.assets.open("$SHADER_DIR/$asset").use { input ->
                        FileOutputStream(file).use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

data class ShaderPreset(
    val name: String,
    val shaders: List<String>,
)

val ANIME4K_MODE_A = ShaderPreset(
    "Anime4K: Mode A (Fast)",
    listOf(
        "Anime4K_Clamp_Highlights.glsl",
        "Anime4K_Restore_CNN_M.glsl",
        "Anime4K_Upscale_CNN_x2_M.glsl",
    ),
)

val SHADER_PRESETS = listOf(ANIME4K_MODE_A)

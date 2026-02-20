package eu.kanade.tachiyomi.extension.anime.installer

import android.app.Service
import eu.kanade.tachiyomi.extension.InstallStep
import eu.kanade.tachiyomi.util.system.getUriSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import logcat.LogPriority
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.system.logcat
import tachiyomi.i18n.MR
import java.io.BufferedReader
import java.io.InputStream

class RootInstallerAnime(private val service: Service) : InstallerAnime(service) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override var ready = canUseRoot()

    private val rootUnavailableMessage = stringResource(MR.strings.ext_installer_root_unavailable_dialog)

    override fun processEntry(entry: Entry) {
        super.processEntry(entry)
        scope.launch {
            var sessionId: String? = null
            try {
                val size = service.getUriSize(entry.uri) ?: throw IllegalStateException()
                service.contentResolver.openInputStream(entry.uri)!!.use {
                    val createCommand = "pm install-create -r -i ${service.packageName} -S $size"
                    val createResult = exec(createCommand)
                    sessionId = SESSION_ID_REGEX.find(createResult.out)?.value
                        ?: throw RuntimeException("Failed to create install session")

                    val writeResult = exec("pm install-write -S $size $sessionId base -", it)
                    if (writeResult.resultCode != 0) {
                        throw RuntimeException("Failed to write APK to session $sessionId")
                    }

                    val commitResult = exec("pm install-commit $sessionId")
                    if (commitResult.resultCode != 0) {
                        throw RuntimeException("Failed to commit install session $sessionId")
                    }

                    continueQueue(InstallStep.Installed)
                }
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e) { "Failed to install extension ${entry.downloadId} ${entry.uri}" }
                if (sessionId != null) {
                    exec("pm install-abandon $sessionId")
                }
                continueQueue(InstallStep.Error)
            }
        }
    }

    override fun cancelEntry(entry: Entry): Boolean = getActiveEntry() != entry

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    init {
        if (!ready) {
            logcat(LogPriority.ERROR) { rootUnavailableMessage }
            service.stopSelf()
        }
    }

    private fun exec(command: String, stdin: InputStream? = null): ShellResult {
        val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
        if (stdin != null) {
            process.outputStream.use { stdin.copyTo(it) }
        }
        val stdout = process.inputStream.bufferedReader().use(BufferedReader::readText)
        val stderr = process.errorStream.bufferedReader().use(BufferedReader::readText)
        val resultCode = process.waitFor()
        return ShellResult(resultCode, if (stderr.isBlank()) stdout else "$stdout\n$stderr".trim())
    }

    private fun canUseRoot(): Boolean {
        return try {
            Runtime.getRuntime().exec(arrayOf("su", "-c", "id")).waitFor() == 0
        } catch (_: Exception) {
            false
        }
    }

    private data class ShellResult(val resultCode: Int, val out: String)
}

private val SESSION_ID_REGEX = Regex("(?<=\\[).+?(?=])")

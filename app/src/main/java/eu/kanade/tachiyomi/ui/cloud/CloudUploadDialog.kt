package eu.kanade.tachiyomi.ui.cloud

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun CloudUploadDialog(
    onDismissRequest: () -> Unit,
    onUploadClick: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = "Upload to Cloud") },
        text = { Text(text = "Upload local files to your Google Drive Cloud Library?") },
        confirmButton = {
            TextButton(onClick = onUploadClick) {
                Text(text = stringResource(MR.strings.action_add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(MR.strings.action_cancel))
            }
        },
    )
}

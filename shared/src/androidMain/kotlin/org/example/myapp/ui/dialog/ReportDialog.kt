package org.example.myapp.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.example.myapp.auth.network.ReportReason

@Composable
fun ReportDialog(
    onDismiss: () -> Unit,
    onConfirm: (ReportReason, String) -> Unit
) {
    var selectedReason by rememberSaveable { mutableStateOf(ReportReason.SPAM) }
    var detail by rememberSaveable { mutableStateOf("") }
    val isDetailValid = detail.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "게시물 신고") },
        text = {
            Column {
                ReportReason.values().forEach { reason ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedReason == reason,
                            onClick = { selectedReason = reason }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (reason) {
                                ReportReason.SPAM -> "스팸 / 도배"
                                ReportReason.INAPPROPRIATE -> "부적절한 내용"
                                ReportReason.VIOLENCE -> "폭력적 콘텐츠"
                                ReportReason.COPYRIGHT -> "저작권 침해"
                                ReportReason.OTHER -> "기타"
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = detail,
                    onValueChange = { detail = it },
                    label = { Text(text = "상세 사유 (필수)") },
                    placeholder = { Text(text = "구체적인 신고 사유를 작성해주세요.") },
                    singleLine = false,
                    maxLines = 3,
                    isError = detail.isEmpty(),
                    supportingText = {
                        if (detail.isBlank()) {
                            Text(
                                text = "상세 사유를 반드시 입력해야 합니다.",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedReason, detail.trim()) },
                enabled = isDetailValid
            ) {
                Text(
                    text = "신고",
                    color = if (isDetailValid) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "취소"
                )
            }
        }
    )
}
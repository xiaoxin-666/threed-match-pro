package com.caluad.match3d.ui.task

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.caluad.match3d.data.local.entity.TaskEntity
import androidx.compose.material3.AlertDialog

@Composable
fun TaskEditDialog(
    existingTask: TaskEntity? = null,
    onDismiss: () -> Unit,
    onSave: (goodsId: String, productName: String, intervalMs: Long, turboMode: Boolean, totalCount: Int) -> Unit
) {
    val isEditing = existingTask != null
    var goodsId by remember { mutableStateOf(existingTask?.goodsId ?: "") }
    var productName by remember { mutableStateOf(existingTask?.productName ?: "") }
    var intervalSec by remember { mutableFloatStateOf((existingTask?.intervalMs ?: 2000L) / 1000f) }
    var turboMode by remember { mutableStateOf(existingTask?.turboMode ?: false) }
    var totalCount by remember { mutableStateOf((existingTask?.totalCount ?: 1).toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "编辑任务" else "新建任务") },
        text = {
            Column {
                OutlinedTextField(
                    value = goodsId,
                    onValueChange = { goodsId = it },
                    label = { Text("商品ID") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = productName,
                    onValueChange = { productName = it },
                    label = { Text("商品名称（可选）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "请求间隔: ${"%.1f".format(intervalSec)}秒",
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = intervalSec,
                    onValueChange = { intervalSec = it },
                    valueRange = 0.2f..10f,
                    enabled = !turboMode,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "极速模式（无延迟）",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = turboMode,
                        onCheckedChange = { turboMode = it }
                    )
                }

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = totalCount,
                    onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() }) totalCount = it },
                    label = { Text("发送次数") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val count = totalCount.toIntOrNull() ?: 1
                    if (goodsId.isNotBlank() && count > 0) {
                        onSave(goodsId, productName, (intervalSec * 1000).toLong(), turboMode, count)
                        onDismiss()
                    }
                },
                enabled = goodsId.isNotBlank() && (totalCount.toIntOrNull() ?: 0) > 0
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

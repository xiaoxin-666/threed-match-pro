package com.example.demo.ui.task

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.demo.data.local.entity.TaskEntity
import com.example.demo.data.local.entity.TaskStatus

@Composable
fun TaskListScreen(
    viewModel: TaskViewModel,
    onShowEditDialog: (Long?) -> Unit
) {
    val tasksState = viewModel.tasks.collectAsStateWithLifecycle(emptyList())
    val tasks = tasksState.value
    val engineStateState = viewModel.engineState.collectAsStateWithLifecycle()
    val engineState = engineStateState.value
    var deleteConfirmId by remember { mutableStateOf<Long?>(null) }
    val bgSeed = remember { kotlin.random.Random.nextInt() }

    Box(modifier = Modifier.fillMaxSize()) {
        AsyncImage(
            model = "https://imgapi.cn/api.php?zd=mobile&fl=meizi&_t=$bgSeed",
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.7f),
            contentScale = ContentScale.Crop
        )

        if (tasks.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "暂无任务，点击 + 创建新任务",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { Spacer(Modifier.height(8.dp)) }
            items(items = tasks, key = { t: TaskEntity -> t.id }) { task ->
                TaskCard(
                    task = task,
                    isActive = engineState.activeTaskIds.contains(task.id),
                    onPlayPause = { viewModel.toggleTask(task.id) },
                    onEdit = { onShowEditDialog(task.id) },
                    onDelete = { deleteConfirmId = task.id }
                )
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (deleteConfirmId != null) {
        AlertDialog(
            onDismissRequest = { deleteConfirmId = null },
            title = { Text("删除任务") },
            text = { Text("确定要删除此任务吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    deleteConfirmId?.let { viewModel.deleteTask(it) }
                    deleteConfirmId = null
                }) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmId = null }) { Text("取消") }
            }
        )
    }
    }
}

@Composable
private fun TaskCard(
    task: TaskEntity,
    isActive: Boolean,
    onPlayPause: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val progress: Float = if (task.totalCount > 0) {
        task.completedCount.toFloat() / task.totalCount.toFloat()
    } else {
        0f
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
            }
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.productName.ifEmpty { "商品 ${task.goodsId}" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "ID: ${task.goodsId}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.width(8.dp))

                TaskStatusChip(task.status)
            }

            Spacer(Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${task.completedCount}/${task.totalCount}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (task.turboMode) {
                    Text(
                        text = " · 极速",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFE65100)
                    )
                }

                Spacer(Modifier.weight(1f))

                val canPlay = task.status in listOf(
                    TaskStatus.PENDING, TaskStatus.PAUSED,
                    TaskStatus.ERROR, TaskStatus.CIRCUIT_BROKEN
                )
                val canPause = task.status == TaskStatus.RUNNING

                IconButton(
                    onClick = onPlayPause,
                    enabled = canPlay || canPause,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (canPause) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (canPause) "暂停" else "开始",
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = "编辑",
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    enabled = !isActive,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "删除",
                        modifier = Modifier.size(20.dp),
                        tint = if (!isActive) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

@Composable
private fun TaskStatusChip(status: String) {
    val (label, color) = statusLabelAndColor(status)
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        fontWeight = FontWeight.Medium
    )
}

private fun statusLabelAndColor(status: String): Pair<String, Color> = when (status) {
    TaskStatus.PENDING -> "等待中" to Color(0xFF616161)
    TaskStatus.RUNNING -> "执行中" to Color(0xFF1565C0)
    TaskStatus.PAUSED -> "已暂停" to Color(0xFFF57F17)
    TaskStatus.COMPLETED -> "已完成" to Color(0xFF2E7D32)
    TaskStatus.ERROR -> "错误" to Color(0xFFC62828)
    TaskStatus.CIRCUIT_BROKEN -> "熔断" to Color(0xFFE65100)
    else -> status to Color(0xFF616161)
}

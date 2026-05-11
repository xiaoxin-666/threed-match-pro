package com.example.demo.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.demo.data.local.entity.TaskEntity

@Composable
fun DashboardScreen(viewModel: DashboardViewModel) {
    val allTasksState = viewModel.allTasks.collectAsStateWithLifecycle(emptyList())
    val allTasks = allTasksState.value
    val runningCountState = viewModel.runningTaskCount.collectAsStateWithLifecycle(0)
    val runningCount = runningCountState.value
    val progressState = viewModel.overallProgress.collectAsStateWithLifecycle(0f)
    val progress = progressState.value
    var showStartAllDialog by remember { mutableStateOf(false) }
    var showStopAllDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }

    // Query state
    val queryGoodsId by viewModel.queryGoodsId.collectAsStateWithLifecycle()
    val queryResult by viewModel.queryResult.collectAsStateWithLifecycle()
    val queryLoading by viewModel.queryLoading.collectAsStateWithLifecycle()
    val queryError by viewModel.queryError.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    val progressPercent = (progress * 100f).toInt()
    val startableCount = allTasks.count {
        it.status in listOf("PENDING", "PAUSED", "ERROR", "CIRCUIT_BROKEN")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // --- Query Section ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = queryGoodsId,
                onValueChange = { viewModel.updateQueryGoodsId(it) },
                modifier = Modifier.weight(1f),
                placeholder = { Text("输入商品 Goods ID") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        focusManager.clearFocus()
                        viewModel.queryGoods()
                    }
                )
            )

            Button(
                onClick = {
                    focusManager.clearFocus()
                    viewModel.queryGoods()
                },
                enabled = !queryLoading && queryGoodsId.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                if (queryLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = "查询",
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(4.dp))
                Text("查询")
            }
        }

        Spacer(Modifier.height(8.dp))

        // Error strip
        AnimatedVisibility(
            visible = queryError != null,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically()
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFCDD2) // Coral pink
                )
            ) {
                Text(
                    text = queryError ?: "",
                    modifier = Modifier.padding(12.dp),
                    color = Color(0xFFC62828),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Result strip
        AnimatedVisibility(
            visible = queryResult != null,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically()
        ) {
            queryResult?.let { info ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = info.productName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            ScoreChip("总推荐", info.totalRecommend, MaterialTheme.colorScheme.primary)
                            ScoreChip("普通会员", info.memberRecommend, MaterialTheme.colorScheme.secondary)
                            ScoreChip("专家", info.expertRecommend, MaterialTheme.colorScheme.tertiary)
                            ScoreChip("数字认证", info.certRecommend, MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Progress + Running cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Progress card
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.ThumbUp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "$progressPercent%",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "总任务进度",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Running card
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "$runningCount",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "运行中",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Quick actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { showStartAllDialog = true },
                modifier = Modifier.weight(1f),
                enabled = startableCount > 0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("启动全部", style = MaterialTheme.typography.labelLarge)
            }

            Button(
                onClick = { showStopAllDialog = true },
                modifier = Modifier.weight(1f),
                enabled = runningCount > 0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Filled.Stop, contentDescription = null, Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("停止全部", style = MaterialTheme.typography.labelLarge)
            }

            Button(
                onClick = { showClearDialog = true },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.ClearAll, contentDescription = null, Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("清除日志", style = MaterialTheme.typography.labelLarge)
            }
        }

        Spacer(Modifier.height(24.dp))

        // Task summary
        if (allTasks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "暂无任务，点击 + 创建新任务",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Text(
                text = "任务列表 (${allTasks.size})",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            LazyColumn {
                items(items = allTasks.take(10), key = { t: TaskEntity -> t.id }) { task ->
                    MiniTaskCard(task)
                }
            }
        }
    }

    if (showStartAllDialog) {
        AlertDialog(
            onDismissRequest = { showStartAllDialog = false },
            title = { Text("启动所有任务") },
            text = { Text("确定要启动所有可执行的任务吗？共 $startableCount 个任务等待执行。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.startAllTasks()
                    showStartAllDialog = false
                }) { Text("启动") }
            },
            dismissButton = {
                TextButton(onClick = { showStartAllDialog = false }) { Text("取消") }
            }
        )
    }

    if (showStopAllDialog) {
        AlertDialog(
            onDismissRequest = { showStopAllDialog = false },
            title = { Text("停止所有任务") },
            text = { Text("确定要停止所有正在运行的任务吗？") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.stopAllTasks()
                    showStopAllDialog = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showStopAllDialog = false }) { Text("取消") }
            }
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("清除日志") },
            text = { Text("确定要清除所有日志记录吗？") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAllLogs()
                    showClearDialog = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun ScoreChip(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value.ifEmpty { "-" },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MiniTaskCard(task: TaskEntity) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.productName.ifEmpty { "商品 ${task.goodsId}" },
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${task.completedCount}/${task.totalCount}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            StatusChip(task.status)
        }
    }
}

@Composable
private fun StatusChip(status: String) {
    val (label, color) = statusInfo(status)
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = color
    )
}

private fun statusInfo(status: String): Pair<String, Color> = when (status) {
    "PENDING" -> "等待中" to Color(0xFF757575)
    "RUNNING" -> "执行中" to Color(0xFF1565C0)
    "PAUSED" -> "已暂停" to Color(0xFFCC8800)
    "COMPLETED" -> "已完成" to Color(0xFF2E7D32)
    "ERROR" -> "错误" to Color(0xFFC62828)
    "CIRCUIT_BROKEN" -> "熔断保护" to Color(0xFFE65100)
    else -> status to Color(0xFF757575)
}

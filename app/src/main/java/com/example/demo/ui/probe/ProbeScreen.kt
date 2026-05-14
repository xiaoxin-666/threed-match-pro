package com.example.demo.ui.probe

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

private val EmeraldGreen = Color(0xFF00C853)
private val CoralPink = Color(0xFFFF5252)
private val AmberWarn = Color(0xFFFFAB00)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProbeScreen(
    initialGoodsId: String = "",
    onNavigateBack: () -> Unit,
    viewModel: ProbeViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(initialGoodsId) {
        viewModel.setInitialGoodsId(initialGoodsId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.mode.label) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ---- Mode Toggle ----
            item {
                ModeToggle(
                    mode = state.mode,
                    enabled = !state.isRunning,
                    onToggle = { viewModel.toggleMode() }
                )
            }

            // ---- Stats Dashboard ----
            if (state.isRunning || state.completed) {
                item {
                    StatsDashboard(
                        sentCount = state.sentCount,
                        successCount = state.successCount,
                        totalCount = state.totalCount,
                        isRunning = state.isRunning
                    )
                }
            }

            // ---- Result card after completion ----
            if (state.completed) {
                item {
                    ResultCard(
                        successCount = state.successCount,
                        totalCount = state.totalCount
                    )
                }
            }

            // ---- Goods ID input ----
            item {
                OutlinedTextField(
                    value = state.goodsId,
                    onValueChange = { viewModel.updateGoodsId(it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("商品 Goods ID") },
                    placeholder = { Text("输入商品ID，如 51448") },
                    singleLine = true,
                    enabled = !state.isRunning,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    trailingIcon = {
                        if (state.goodsId.isNotEmpty() && !state.isRunning) {
                            IconButton(onClick = { viewModel.updateGoodsId("") }) {
                                Icon(Icons.Filled.Clear, contentDescription = "清除")
                            }
                        }
                    }
                )
            }

            // ---- Concurrency selector ----
            item {
                Text(
                    text = "并发请求数: ${state.concurrency}",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Spacer(Modifier.height(8.dp))
                ConcurrencySelector(
                    selected = state.concurrency,
                    enabled = !state.isRunning,
                    onSelect = { viewModel.updateConcurrency(it) }
                )
            }

            // ---- Execute button ----
            item {
                Button(
                    onClick = { viewModel.executeProbe() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = state.goodsId.isNotBlank() && !state.isRunning,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    if (state.isRunning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.5.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeCap = StrokeCap.Round
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "${state.mode.actionLabel}中...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(Icons.Filled.Bolt, contentDescription = null, Modifier.size(24.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "开始并发${state.mode.actionLabel}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // ---- Result log header ----
            if (state.results.isNotEmpty()) {
                item {
                    Text(
                        text = "探测日志",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ---- Result log items ----
            items(state.results, key = { it.index }) { result ->
                ResultLogItem(result = result)
            }

            // Bottom spacer
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun StatsDashboard(
    sentCount: Int,
    successCount: Int,
    totalCount: Int,
    isRunning: Boolean
) {
    val successRate = if (sentCount > 0) successCount.toFloat() / sentCount.toFloat() else 0f
    val animatedRate by animateFloatAsState(
        targetValue = successRate,
        animationSpec = tween(durationMillis = 400),
        label = "successRate"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Large success rate
            Text(
                text = "${(animatedRate * 100).toInt()}%",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = if (successCount > 0) EmeraldGreen else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "成功率",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(16.dp))

            // Sent / Success counters
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CounterBadge(
                    label = "已发送",
                    count = sentCount,
                    color = MaterialTheme.colorScheme.primary
                )
                CounterBadge(
                    label = "成功",
                    count = successCount,
                    color = EmeraldGreen
                )
                CounterBadge(
                    label = "失败",
                    count = sentCount - successCount,
                    color = CoralPink
                )
            }

            if (isRunning) {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        strokeCap = StrokeCap.Round
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "探测进行中...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun CounterBadge(label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$count",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
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
private fun ResultCard(successCount: Int, totalCount: Int) {
    val failCount = totalCount - successCount
    val allSuccess = successCount == totalCount
    val allFail = successCount == 0

    val borderColor = when {
        allSuccess -> EmeraldGreen
        allFail -> CoralPink
        else -> AmberWarn
    }
    val bgColor = when {
        allSuccess -> EmeraldGreen.copy(alpha = 0.08f)
        allFail -> CoralPink.copy(alpha = 0.08f)
        else -> AmberWarn.copy(alpha = 0.08f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, borderColor, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (allSuccess) Icons.Filled.CheckCircle else Icons.Filled.Error,
                contentDescription = null,
                tint = borderColor,
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = when {
                        allSuccess -> "全部成功"
                        allFail -> "全部失败"
                        else -> "部分成功"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = borderColor
                )
                Text(
                    text = "成功 $successCount / $totalCount 次请求",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ConcurrencySelector(
    selected: Int,
    enabled: Boolean,
    onSelect: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        (1..5).forEach { count ->
            val isSelected = count == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .then(
                        if (!enabled) Modifier
                        else Modifier.clickable { onSelect(count) }
                    )
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$count",
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ResultLogItem(result: ProbeResult) {
    val icon: ImageVector
    val tint: Color
    val statusText: String

    if (result.success) {
        icon = Icons.Filled.CheckCircle
        tint = EmeraldGreen
        statusText = "SUCCESS"
    } else {
        icon = Icons.Filled.Error
        tint = CoralPink
        statusText = "FAIL"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Request number badge
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "#${result.index}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = tint
                )
            }

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = tint
                    )
                    Spacer(Modifier.width(12.dp))
                    Icon(
                        Icons.Filled.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(
                        text = "${result.elapsedMs}ms",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    text = result.msg,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 3
                )

                if (result.info.isNotEmpty() && result.info != result.msg) {
                    Text(
                        text = result.info,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
            }
        }
    }
}

@Composable
private fun ModeToggle(
    mode: ProbeMode,
    enabled: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ProbeMode.entries.forEach { m ->
            val isSelected = m == mode
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .then(
                        if (!enabled) Modifier
                        else Modifier.clickable { onToggle() }
                    )
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (m == ProbeMode.ADMIRE) Icons.Filled.Bolt else Icons.Filled.Clear,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = m.label,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

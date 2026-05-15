package com.caluad.match3d.ui.console

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.caluad.match3d.data.local.entity.LogLevel
import com.caluad.match3d.engine.LogEvent
import com.caluad.match3d.ui.theme.ConsoleBackground
import com.caluad.match3d.ui.theme.ConsoleError
import com.caluad.match3d.ui.theme.ConsoleInfo
import com.caluad.match3d.ui.theme.ConsoleMuted
import com.caluad.match3d.ui.theme.ConsoleSuccess
import com.caluad.match3d.ui.theme.ConsoleText
import com.caluad.match3d.util.formatTimestamp

@Composable
fun ConsoleScreen(viewModel: ConsoleViewModel) {
    val logsState = viewModel.logs.collectAsStateWithLifecycle(emptyList())
    val logs = logsState.value
    val autoScrollState = viewModel.autoScroll.collectAsStateWithLifecycle(true)
    val autoScroll = autoScrollState.value
    val listState = rememberLazyListState()

    LaunchedEffect(logs.size, autoScroll) {
        if (autoScroll && logs.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ConsoleBackground)
    ) {
        if (logs.isEmpty()) {
            Text(
                text = "日志为空，启动任务后将在此显示实时日志",
                color = ConsoleMuted,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp)
            ) {
                item { Spacer(Modifier.padding(4.dp)) }
                items(items = logs, key = { e: LogEvent -> e.timestamp.hashCode() + e.message.hashCode() }) { event ->
                    LogRow(event)
                }
                item { Spacer(Modifier.padding(4.dp)) }
            }
        }

        // Bottom bar
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(ConsoleBackground.copy(alpha = 0.95f))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "已显示 ${logs.size} 条记录",
                style = MaterialTheme.typography.bodySmall,
                color = ConsoleMuted
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "自动滚动",
                style = MaterialTheme.typography.bodySmall,
                color = ConsoleMuted
            )
            Switch(
                checked = autoScroll,
                onCheckedChange = { viewModel.toggleAutoScroll() }
            )
            IconButton(onClick = { viewModel.clearLogs() }) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "清除日志",
                    tint = ConsoleMuted
                )
            }
        }
    }
}

@Composable
private fun LogRow(event: LogEvent) {
    val (prefix, color) = when (event.level) {
        LogLevel.INFO -> "[INFO]" to ConsoleInfo
        LogLevel.SUCCESS -> "[SUCCESS]" to ConsoleSuccess
        LogLevel.ERROR -> "[ERROR]" to ConsoleError
        else -> "[LOG]" to ConsoleText
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp, horizontal = 4.dp)
    ) {
        Text(
            text = formatTimestamp(event.timestamp),
            color = ConsoleMuted,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = prefix,
            color = color,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(end = 6.dp)
        )
        Text(
            text = event.message,
            color = ConsoleText,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

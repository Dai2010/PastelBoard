package dev.pastel.pastelboard.ui.logs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.pastel.pastelboard.BuildConfig
import dev.pastel.pastelboard.PastelBoardUiState
import dev.pastel.pastelboard.ui.components.PastelBackground
import dev.pastel.pastelboard.ui.components.SoftCard

@Composable
fun InternalLogScreen(
    state: PastelBoardUiState,
    onBack: () -> Unit,
    onOpenMenu: () -> Unit,
    onExportLogs: () -> Unit,
    onClearLogs: () -> Unit,
) {
    PastelBackground(
        palette = state.palette,
        backgroundImageUri = state.backgroundImageUri,
        backgroundImageOpacity = state.backgroundImageOpacity,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SoftCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Rounded.ArrowBack, contentDescription = "返回")
                        }
                        IconButton(onClick = onOpenMenu) {
                            Icon(Icons.Rounded.Menu, contentDescription = "菜单")
                        }
                        Column {
                            Text(
                                text = "内部测试日志",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "${BuildConfig.VERSION_NAME} · 实时记录连接、按键、触控板事件",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = onClearLogs) {
                            Icon(Icons.Rounded.DeleteSweep, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("清空")
                        }
                        Button(onClick = onExportLogs) {
                            Icon(Icons.Rounded.IosShare, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("选择位置导出")
                        }
                    }
                }
                state.logExportMessage?.let { message ->
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            SoftCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                Text(
                    text = "HID 状态",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))
                Text("profile=${state.hidDiagnostics.profileReady} · registered=${state.hidDiagnostics.appRegistered}")
                Text("plugged=${state.hidDiagnostics.pluggedDevice ?: "无"}")
                Text("connected=${state.hidDiagnostics.connectedDevice ?: "无"}")
                Text(
                    "键盘报告=${state.hidDiagnostics.lastKeyboardReport?.let { report -> "id=${report.reportId}, size=${report.size}, accepted=${report.accepted}" } ?: "无"}",
                )
                Text(
                    "指针报告=${state.hidDiagnostics.lastPointerReport?.let { report -> "id=${report.reportId}, size=${report.size}, accepted=${report.accepted}" } ?: "无"}",
                )
                state.hidDiagnostics.lastFailure?.let { failure ->
                    Text("最近失败=$failure", color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "实时日志 ${state.diagnosticLogs.size} 条",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Icon(
                        imageVector = Icons.Rounded.BugReport,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(Modifier.height(12.dp))
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (state.diagnosticLogs.isEmpty()) {
                        Text(
                            text = "暂无日志。连接设备、点击键盘或移动触控板后会实时显示。",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        state.diagnosticLogs.asReversed().forEach { entry ->
                            Text(
                                text = "${entry.timestamp} [${entry.category}] ${entry.message}",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                            )
                        }
                    }
                }
            }
        }
    }
}

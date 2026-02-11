package com.wechatmonitor.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wechatmonitor.model.ChatMessage
import com.wechatmonitor.model.MonitorSettings
import com.wechatmonitor.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * 主界面
 */
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val messages by viewModel.messages.collectAsState(initial = emptyList())
    val importantMessages by viewModel.importantMessages.collectAsState(initial = emptyList())
    val settings by viewModel.settings.collectAsState(initial = null)

    var selectedTab by remember { mutableIntStateOf(0) }
    var showAccessibilityDialog by remember { mutableStateOf(false) }

    // 显示无障碍权限提示
    LaunchedEffect(uiState.accessibilityEnabled) {
        if (!uiState.accessibilityEnabled) {
            showAccessibilityDialog = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("微信监听助手") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.ChatBubble, "消息") },
                    label = { Text("消息") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Star, "重要") },
                    label = { Text("重要") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Settings, "设置") },
                    label = { Text("设置") }
                )
            }
        }
    ) { padding ->
        when (selectedTab) {
            0 -> MessageListScreen(
                messages = messages,
                modifier = Modifier.padding(padding)
            )
            1 -> ImportantMessagesScreen(
                messages = importantMessages,
                modifier = Modifier.padding(padding)
            )
            2 -> settings?.let {
                SettingsScreen(
                    settings = it,
                    accessibilityEnabled = uiState.accessibilityEnabled,
                    onOpenAccessibilitySettings = { viewModel.openAccessibilitySettings() },
                    onUpdateSettings = { dailyEnabled, dailyTime ->
                        viewModel.updateSettings(dailyEnabled, dailyTime)
                    },
                    onAddGroup = { viewModel.addMonitoredGroup(it) },
                    onRemoveGroup = { viewModel.removeMonitoredGroup(it) },
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }

    // 无障碍权限提示对话框
    if (showAccessibilityDialog && !uiState.accessibilityEnabled) {
        AccessibilityPermissionDialog(
            onOpenSettings = {
                viewModel.openAccessibilitySettings()
                showAccessibilityDialog = false
            },
            onDismiss = { showAccessibilityDialog = false }
        )
    }
}

/**
 * 消息列表界面
 */
@Composable
fun MessageListScreen(
    messages: List<ChatMessage>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                "全部消息 (${messages.size})",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        if (messages.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.ChatBubbleOutline,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "暂无消息",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(messages) { message ->
                MessageCard(message)
            }
        }
    }
}

/**
 * 重要消息界面
 */
@Composable
fun ImportantMessagesScreen(
    messages: List<ChatMessage>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                "重要消息 (${messages.size})",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        if (messages.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.StarOutline,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "暂无重要消息",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(messages) { message ->
                ImportantMessageCard(message)
            }
        }
    }
}

/**
 * 设置界面
 */
@Composable
fun SettingsScreen(
    settings: MonitorSettings,
    accessibilityEnabled: Boolean,
    onOpenAccessibilitySettings: () -> Unit,
    onUpdateSettings: (Boolean?, String?) -> Unit,
    onAddGroup: (String) -> Unit,
    onRemoveGroup: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddGroupDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 服务状态
        item {
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "无障碍服务",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                if (accessibilityEnabled) "服务运行中" else "服务未启动",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (accessibilityEnabled)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.error
                            )
                        }
                        Button(onClick = onOpenAccessibilitySettings) {
                            Text(if (accessibilityEnabled) "重新配置" else "去开启")
                        }
                    }
                }
            }
        }

        // 监听的群组
        item {
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "监听的群组",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (settings.monitoredGroups.isEmpty()) {
                        Text(
                            "暂无监听群组",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        settings.monitoredGroups.forEach { group ->
                            GroupItem(
                                groupName = group,
                                onRemove = { onRemoveGroup(group) }
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { showAddGroupDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, "添加")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("添加群组")
                    }
                }
            }
        }

        // 每日摘要设置
        item {
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "每日摘要",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("启用每日摘要")
                        Switch(
                            checked = settings.dailySummaryEnabled,
                            onCheckedChange = { onUpdateSettings(it, null) }
                        )
                    }

                    if (settings.dailySummaryEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("发送时间: ${settings.dailySummaryTime}")
                    }
                }
            }
        }
    }

    // 添加群组对话框
    if (showAddGroupDialog) {
        AddGroupDialog(
            onDismiss = { showAddGroupDialog = false },
            onConfirm = { groupName ->
                onAddGroup(groupName)
                showAddGroupDialog = false
            }
        )
    }
}

/**
 * 消息卡片
 */
@Composable
fun MessageCard(message: ChatMessage) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (message.isImportant) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    message.groupName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    formatTime(message.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                message.senderName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                message.content,
                style = MaterialTheme.typography.bodyMedium
            )
            if (message.isImportant) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "重要 (${(message.importanceScore * 100).toInt()}%)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

/**
 * 重要消息卡片
 */
@Composable
fun ImportantMessageCard(message: ChatMessage) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        message.groupName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    formatTime(message.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                message.senderName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                message.content,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/**
 * 群组项
 */
@Composable
fun GroupItem(groupName: String, onRemove: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(groupName, style = MaterialTheme.typography.bodyMedium)
        IconButton(onClick = onRemove) {
            Icon(Icons.Default.Close, "删除")
        }
    }
}

/**
 * 添加群组对话框
 */
@Composable
fun AddGroupDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加监听群组") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("群组名称") },
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(text) },
                enabled = text.isNotBlank()
            ) {
                Text("添加")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

/**
 * 无障碍权限提示对话框
 */
@Composable
fun AccessibilityPermissionDialog(
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("开启无障碍服务") },
        text = {
            Text("请开启无障碍服务以监听微信群消息。开启后，应用将在后台自动分析重要消息并通知您。")
        },
        confirmButton = {
            Button(onClick = onOpenSettings) {
                Text("去开启")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("稍后")
            }
        }
    )
}

/**
 * 格式化时间
 */
private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

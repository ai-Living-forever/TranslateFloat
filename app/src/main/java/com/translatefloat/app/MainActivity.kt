package com.translatefloat.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private lateinit var settingsManager: SettingsManager

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // Permission result handled in UI
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settingsManager = SettingsManager(this)

        setContent {
            MaterialTheme {
                AppNavigation(
                    settingsManager = settingsManager,
                    onRequestOverlayPermission = { requestOverlayPermission() }
                )
            }
        }
    }

    private fun requestOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            overlayPermissionLauncher.launch(intent)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    settingsManager: SettingsManager,
    onRequestOverlayPermission: () -> Unit
) {
    val navController = rememberNavController()
    var selectedItem by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TranslateFloat") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF6366F1),
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "首页") },
                    label = { Text("首页") },
                    selected = selectedItem == 0,
                    onClick = {
                        selectedItem = 0
                        navController.navigate("home")
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = "设置") },
                    label = { Text("设置") },
                    selected = selectedItem == 1,
                    onClick = {
                        selectedItem = 1
                        navController.navigate("settings")
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "历史") },
                    label = { Text("历史") },
                    selected = selectedItem == 2,
                    onClick = {
                        selectedItem = 2
                        navController.navigate("history")
                    }
                )
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("home") {
                HomeScreen(
                    settingsManager = settingsManager,
                    onRequestOverlayPermission = onRequestOverlayPermission
                )
            }
            composable("settings") {
                SettingsScreen(settingsManager = settingsManager)
            }
            composable("history") {
                HistoryScreen(settingsManager = settingsManager)
            }
        }
    }
}

@Composable
fun HomeScreen(
    settingsManager: SettingsManager,
    onRequestOverlayPermission: () -> Unit
) {
    val context = LocalContext.current
    var hasOverlayPermission by remember {
        mutableStateOf(Settings.canDrawOverlays(context))
    }
    var isServiceRunning by remember {
        mutableStateOf(FloatWindowService.isRunning)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "TranslateFloat",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF6366F1)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "选中文字 → 翻译 → 保存到 Obsidian",
            fontSize = 14.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Permission Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (hasOverlayPermission) Color(0xFFD1FAE5) else Color(0xFFFEF3C7)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "悬浮窗权限",
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (hasOverlayPermission) "已授权" else "未授权",
                        fontSize = 12.sp,
                        color = if (hasOverlayPermission) Color(0xFF059669) else Color(0xFFD97706)
                    )
                }
                if (!hasOverlayPermission) {
                    Button(
                        onClick = onRequestOverlayPermission,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                    ) {
                        Text("去授权")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Service Toggle Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "悬浮窗服务",
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (isServiceRunning) "运行中" else "已停止",
                        fontSize = 12.sp,
                        color = if (isServiceRunning) Color(0xFF059669) else Color.Gray
                    )
                }
                Switch(
                    checked = isServiceRunning,
                    onCheckedChange = { checked ->
                        if (checked) {
                            if (hasOverlayPermission) {
                                context.startService(
                                    Intent(context, FloatWindowService::class.java)
                                )
                                isServiceRunning = true
                            } else {
                                onRequestOverlayPermission()
                            }
                        } else {
                            context.stopService(
                                Intent(context, FloatWindowService::class.java)
                            )
                            isServiceRunning = false
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF6366F1)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Instructions
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F4F6))
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "使用说明",
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "1. 复制需要翻译的文字",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Text(
                    text = "2. 点击屏幕边缘的悬浮球",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Text(
                    text = "3. 查看翻译结果并保存到 Obsidian",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(settingsManager: SettingsManager) {
    val context = LocalContext.current
    var vaultName by remember { mutableStateOf(settingsManager.vaultName) }
    var savePath by remember { mutableStateOf(settingsManager.savePath) }
    var targetLang by remember { mutableStateOf(settingsManager.targetLang) }
    var expanded by remember { mutableStateOf(false) }
    var showSavedMessage by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "设置",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = vaultName,
            onValueChange = { vaultName = it },
            label = { Text("Obsidian Vault 名称") },
            placeholder = { Text("输入你的 Vault 名称") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = savePath,
            onValueChange = { savePath = it },
            label = { Text("保存路径") },
            placeholder = { Text("例如: Inbox/翻译笔记") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Language Dropdown
        Text(
            text = "目标语言",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            onClick = { expanded = true }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(SettingsManager.LANGUAGES[targetLang] ?: targetLang)
                Text("▼", color = Color.Gray)
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            SettingsManager.LANGUAGES.forEach { (code, name) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        targetLang = code
                        expanded = false
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                settingsManager.vaultName = vaultName
                settingsManager.savePath = savePath
                settingsManager.targetLang = targetLang
                showSavedMessage = true
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
        ) {
            Text("保存设置")
        }

        if (showSavedMessage) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "设置已保存",
                color = Color(0xFF059669),
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = {
                vaultName = ""
                savePath = SettingsManager.DEFAULT_SAVE_PATH
                targetLang = SettingsManager.DEFAULT_TARGET_LANG
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("重置为默认值", color = Color.Gray)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(settingsManager: SettingsManager) {
    val context = LocalContext.current
    val history = remember { mutableStateOf(settingsManager.getHistory()) }
    val obsidianHelper = remember { ObsidianHelper(context) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "翻译历史",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            if (history.value.isNotEmpty()) {
                TextButton(
                    onClick = {
                        settingsManager.clearHistory()
                        history.value = emptyList()
                    }
                ) {
                    Text("清空", color = Color.Red)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (history.value.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "暂无翻译记录",
                    color = Color.Gray
                )
            }
        } else {
            LazyColumn {
                items(history.value) { record ->
                    HistoryItem(
                        record = record,
                        onSaveClick = {
                            try {
                                obsidianHelper.saveToObsidianWithRecord(record)
                            } catch (e: Exception) {
                                // Show error
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun HistoryItem(
    record: TranslationRecord,
    onSaveClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    val timeStr = dateFormat.format(Date(record.timestamp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = record.original,
                fontSize = 12.sp,
                color = Color.Gray,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = record.translated,
                fontSize = 14.sp,
                color = Color.Black,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = timeStr,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                TextButton(
                    onClick = onSaveClick,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF6366F1))
                ) {
                    Text("保存到 Obsidian", fontSize = 12.sp)
                }
            }
        }
    }
}

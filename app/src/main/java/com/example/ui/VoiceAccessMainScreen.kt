package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.model.ActionType
import com.example.model.DeviceAction
import com.example.ui.components.ActionExecutionCard
import com.example.ui.components.AudioWaveVisualizer
import com.example.ui.components.CommandCheatsheetSheet
import com.example.ui.components.HighThinkingCard
import com.example.ui.components.ScreenshotPreviewCard
import com.example.ui.components.SettingsScreen
import com.example.ui.components.VoiceAccessGridOverlay
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.VoiceAccentPurple
import com.example.ui.theme.VoiceCyan
import com.example.ui.theme.VoiceGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceAccessMainScreen(
    viewModel: VoiceAccessViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var textInput by remember { mutableStateOf("") }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startListening()
        } else {
            Toast.makeText(context, "ভয়েস কমান্ডের জন্য মাইক্রোফোন পারমিশন প্রয়োজন", Toast.LENGTH_SHORT).show()
        }
    }

    val requestMicAndListen = {
        val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            if (uiState.isListening) {
                viewModel.stopListening()
            } else {
                viewModel.startListening()
            }
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBg,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(
                                    Brush.linearGradient(listOf(Color(0xFF0284C7), Color(0xFF00E5FF)))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.RecordVoiceOver,
                                contentDescription = "Voice Access Icon",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Voice Access",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(PrimaryBlue.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "বাংলা AI",
                                        color = VoiceCyan,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = if (uiState.isListening) "শুনছি…" else if (uiState.isThinking) "বিশ্লেষণ হচ্ছে…" else "প্রস্তুত (Ready)",
                                color = if (uiState.isListening) VoiceCyan else Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }
                    }
                },
                actions = {
                    // Manual screenshot button shortcut
                    IconButton(
                        onClick = { viewModel.captureScreenshotManually() },
                        modifier = Modifier.testTag("action_capture_screenshot")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "স্ক্রিনশট নিন",
                            tint = VoiceCyan
                        )
                    }

                    // Dialect badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E293B))
                            .clickable { viewModel.setActiveTab(3) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = uiState.selectedDialect.code,
                            color = PrimaryBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = DarkSurface,
                modifier = Modifier.border(1.dp, Color(0xFF1E293B))
            ) {
                NavigationBarItem(
                    selected = uiState.activeTab == 0,
                    onClick = { viewModel.setActiveTab(0) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "ভয়েস"
                        )
                    },
                    label = { Text("ভয়েস কন্ট্রোল") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = VoiceCyan,
                        selectedTextColor = VoiceCyan,
                        indicatorColor = Color(0xFF1E293B),
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8)
                    ),
                    modifier = Modifier.testTag("nav_voice_tab")
                )

                NavigationBarItem(
                    selected = uiState.activeTab == 1,
                    onClick = { viewModel.setActiveTab(1) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.GridOn,
                            contentDescription = "নম্বর গ্রিড"
                        )
                    },
                    label = { Text("নম্বর গ্রিড") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = VoiceCyan,
                        selectedTextColor = VoiceCyan,
                        indicatorColor = Color(0xFF1E293B),
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8)
                    ),
                    modifier = Modifier.testTag("nav_grid_tab")
                )

                NavigationBarItem(
                    selected = uiState.activeTab == 2,
                    onClick = { viewModel.setActiveTab(2) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = "কমান্ড তালিকা"
                        )
                    },
                    label = { Text("কমান্ডসমূহ") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = VoiceCyan,
                        selectedTextColor = VoiceCyan,
                        indicatorColor = Color(0xFF1E293B),
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8)
                    ),
                    modifier = Modifier.testTag("nav_cheatsheet_tab")
                )

                NavigationBarItem(
                    selected = uiState.activeTab == 3,
                    onClick = { viewModel.setActiveTab(3) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "সেটিংস"
                        )
                    },
                    label = { Text("সেটিংস") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = VoiceCyan,
                        selectedTextColor = VoiceCyan,
                        indicatorColor = Color(0xFF1E293B),
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8)
                    ),
                    modifier = Modifier.testTag("nav_settings_tab")
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (uiState.activeTab) {
                0 -> {
                    // Home / Voice Access Tab
                    HomeVoiceView(
                        uiState = uiState,
                        onMicClick = requestMicAndListen,
                        onQuickCommand = { cmd -> viewModel.processCommand(cmd) },
                        onExecuteAction = { action -> viewModel.executeAction(action) },
                        onShareScreenshotToWhatsApp = { viewModel.shareScreenshotToWhatsApp() },
                        onRetakeScreenshot = { viewModel.captureScreenshotManually() },
                        textInput = textInput,
                        onTextInputChange = { textInput = it },
                        onSubmitText = {
                            if (textInput.isNotBlank()) {
                                viewModel.processCommand(textInput)
                                textInput = ""
                            }
                        }
                    )
                }
                1 -> {
                    // Grid Mode Tab
                    VoiceAccessGridOverlay(
                        onItemClick = { item ->
                            viewModel.processCommand(item.commandText)
                        },
                        modifier = Modifier.padding(16.dp)
                    )
                }
                2 -> {
                    // Command Cheatsheet Tab
                    CommandCheatsheetSheet(
                        commands = viewModel.presetCommands,
                        onSelectCommand = { cmd ->
                            viewModel.setActiveTab(0)
                            viewModel.processCommand(cmd)
                        },
                        modifier = Modifier.padding(16.dp)
                    )
                }
                3 -> {
                    // Settings Tab (Modeled after Voice Access settings screenshot)
                    SettingsScreen(
                        isServiceActive = uiState.isServiceActive,
                        onToggleService = { viewModel.toggleService(it) },
                        selectedDialect = uiState.selectedDialect,
                        onSelectDialect = { viewModel.setDialect(it) },
                        isHighThinking = uiState.isHighThinkingEnabled,
                        onToggleHighThinking = { viewModel.toggleHighThinking(it) },
                        autoExecute = uiState.autoExecuteActions,
                        onToggleAutoExecute = { viewModel.toggleAutoExecute(it) },
                        speechRate = uiState.speechRate,
                        onSpeechRateChange = { viewModel.setSpeechRate(it) }
                    )
                }
            }
        }
    }
}

@Composable
fun HomeVoiceView(
    uiState: VoiceAccessUiState,
    onMicClick: () -> Unit,
    onQuickCommand: (String) -> Unit,
    onExecuteAction: (DeviceAction) -> Unit,
    onShareScreenshotToWhatsApp: () -> Unit,
    onRetakeScreenshot: () -> Unit,
    textInput: String,
    onTextInputChange: (String) -> Unit,
    onSubmitText: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "micPulse")
    val micRingScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "micScale"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Voice Hero & Status Header
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Microphone Big Action Circle
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.isListening || uiState.isThinking) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .scale(micRingScale)
                                .clip(CircleShape)
                                .background(
                                    if (uiState.isListening) VoiceCyan.copy(alpha = 0.25f)
                                    else VoiceAccentPurple.copy(alpha = 0.25f)
                                )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    if (uiState.isListening) listOf(Color(0xFF00E5FF), Color(0xFF0284C7))
                                    else if (uiState.isThinking) listOf(VoiceAccentPurple, Color(0xFF6D28D9))
                                    else listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                                )
                            )
                            .border(
                                2.dp,
                                if (uiState.isListening) VoiceCyan else Color(0xFF334155),
                                CircleShape
                            )
                            .clickable(onClick = onMicClick)
                            .testTag("mic_listening_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (uiState.isListening) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = if (uiState.isListening) "বন্ধ করুন" else "শুনুন",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (uiState.isListening) "শুনছি… বাংলায় বলুন"
                    else if (uiState.isThinking) "Gemini High-Thinking বিশ্লেষণ করছে…"
                    else "ভয়েস কমান্ড দিতে মাইক্রোফোনে চাপ দিন",
                    color = if (uiState.isListening) VoiceCyan else Color(0xFFCBD5E1),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )

                // Audio Wave Visualizer
                Spacer(modifier = Modifier.height(10.dp))
                AudioWaveVisualizer(
                    isListening = uiState.isListening,
                    isThinking = uiState.isThinking,
                    rmsLevel = uiState.audioRmsLevel
                )
            }
        }

        // Realtime Transcript Bubble
        if (uiState.currentTranscript.isNotBlank() || uiState.partialTranscript.isNotBlank()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF152238))
                        .border(1.dp, PrimaryBlue.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        .padding(14.dp)
                        .testTag("transcript_card")
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                tint = VoiceCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "উচ্চারিত কমান্ড:",
                                color = VoiceCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (uiState.currentTranscript.isNotBlank()) uiState.currentTranscript else uiState.partialTranscript,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Text input fallback bar (in case user wants to type or test hands-free)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = onTextInputChange,
                    placeholder = { Text("অথবা বাংলায় লিখে কমান্ড দিন…", color = Color(0xFF64748B), fontSize = 13.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("voice_command_input"),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedContainerColor = Color(0xFF0F172A),
                        unfocusedContainerColor = Color(0xFF0F172A),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onSubmitText,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(PrimaryBlue)
                        .testTag("send_command_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "কমান্ড পাঠান",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Quick Command Suggestions Chips
        item {
            Column {
                Text(
                    text = "দ্রুত কমান্ড (Quick Voice Prompts):",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val quickChips = listOf(
                        "YouTube kholo" to "ইউটিউব খোলো",
                        "Ai gan ta play koro" to "গান চালাও",
                        "Screen shot naw" to "স্ক্রিনশট নাও",
                        "Screen shot ti WhatsApp e pathaw" to "হোয়াটসঅ্যাপে পাঠাও",
                        "Phone e call korbe" to "কল করো",
                        "Flashlight on koro" to "ফ্ল্যাশলাইট"
                    )
                    items(quickChips) { (query, label) ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF1E293B))
                                .border(1.dp, Color(0xFF334155), RoundedCornerShape(20.dp))
                                .clickable { onQuickCommand(query) }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                                .testTag("quick_chip_${query.take(8)}")
                        ) {
                            Text(
                                text = label,
                                color = Color(0xFFE2E8F0),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // Screenshot Preview Card (if available)
        if (uiState.capturedScreenshot != null) {
            item {
                ScreenshotPreviewCard(
                    screenshot = uiState.capturedScreenshot,
                    onShareToWhatsApp = onShareScreenshotToWhatsApp,
                    onRetake = onRetakeScreenshot
                )
            }
        }

        // High Thinking Reasoning Card
        item {
            HighThinkingCard(
                thinkingText = uiState.lastResult?.thinkingProcess ?: "",
                isThinking = uiState.isThinking
            )
        }

        // Actions Detected by Gemini / AI
        if (uiState.lastResult?.actions?.isNotEmpty() == true) {
            item {
                Text(
                    text = "সনাক্তকৃত অ্যাকশনসমূহ (${uiState.lastResult.actions.size} টি):",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            items(uiState.lastResult.actions) { action ->
                ActionExecutionCard(
                    action = action,
                    onExecuteClick = onExecuteAction
                )
            }
        }

        // Execution status message
        if (uiState.lastExecutedStatus != null) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0F2537))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "স্ট্যাটাস: ${uiState.lastExecutedStatus}",
                        color = VoiceCyan,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

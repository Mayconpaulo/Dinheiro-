package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.AddTransactionScreen
import com.example.ui.screens.ChatbotScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.SettingsDialog
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.CardBackground
import com.example.ui.theme.DeepBackground
import com.example.ui.theme.PrimaryCyan
import com.example.ui.theme.PrimaryPurple
import com.example.ui.theme.AccentPink
import com.example.ui.theme.BorderColor
import com.example.ui.viewmodel.FinanceViewModel

enum class Screen {
    Dashboard,
    History
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainContent()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContent() {
    val viewModel: FinanceViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    
    var currentScreen by remember { mutableStateOf(Screen.Dashboard) }
    var showChatbotPopup by remember { mutableStateOf(false) }
    var showAddTransactionPopup by remember { mutableStateOf(false) }
    var showSettingsPopup by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("app_scaffold"),
        containerColor = Color.Transparent // Allow animated background to shine through
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black) // Dark substrate
        ) {
            // 1. Gentle passing lights (Ambient background gradient flow)
            AmbientGlowingBackground()

            if (!uiState.isLoggedIn) {
                // Centered Cinematic Glass Login Card
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    LoginScreen(viewModel = viewModel)
                }
            } else {
                // 2. Active Screen Content
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(bottom = 76.dp) // Leave correct clearance for the floating islands bar
                ) {
                    when (currentScreen) {
                        Screen.Dashboard -> {
                            DashboardScreen(
                                viewModel = viewModel,
                                onNavigateToAdd = { showAddTransactionPopup = true },
                                onOpenSettings = { showSettingsPopup = true }
                            )
                        }
                        Screen.History -> {
                            HistoryScreen(
                                viewModel = viewModel
                            )
                        }
                    }
                }

                // 3. Floating Apple Glassmorphism Island Dock + Adaptive FAB Bubbles
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // LEFT SIDE: Artificial Intelligence Chatbot FAB
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(PrimaryCyan, PrimaryPurple, AccentPink)
                                    )
                                )
                                .border(
                                    width = 1.dp,
                                    color = Color.White.copy(alpha = 0.3f),
                                    shape = CircleShape
                                )
                                .clickable { showChatbotPopup = true }
                                .testTag("ai_chatbot_fab"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "FinTrack Chatbot IA",
                                tint = Color.Black,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // CENTER: Translucent Frosted Glass Navigation Island Dock
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .clip(RoundedCornerShape(26.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .border(
                                    width = 1.dp,
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.2f),
                                            Color.White.copy(alpha = 0.04f)
                                        )
                                    ),
                                    shape = RoundedCornerShape(26.dp)
                                )
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Dashboard Glass Islet
                                val isDashboard = currentScreen == Screen.Dashboard
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(22.dp))
                                        .let { modifier ->
                                            if (isDashboard) {
                                                modifier.background(
                                                    Brush.linearGradient(
                                                        colors = listOf(
                                                            PrimaryCyan.copy(alpha = 0.2f),
                                                            PrimaryPurple.copy(alpha = 0.15f)
                                                        )
                                                    )
                                                )
                                            } else modifier
                                        }
                                        .border(
                                            width = 1.dp,
                                            color = if (isDashboard) PrimaryCyan.copy(alpha = 0.3f) else Color.Transparent,
                                            shape = RoundedCornerShape(22.dp)
                                        )
                                        .clickable { currentScreen = Screen.Dashboard }
                                        .testTag("nav_dashboard"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Dashboard,
                                            contentDescription = "Dashboard",
                                            tint = if (isDashboard) PrimaryCyan else Color.White.copy(alpha = 0.6f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Dashboard",
                                            color = if (isDashboard) Color.White else Color.White.copy(alpha = 0.6f),
                                            fontSize = 12.sp,
                                            fontWeight = if (isDashboard) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                }

                                // Histórico Glass Islet
                                val isHistory = currentScreen == Screen.History
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(22.dp))
                                        .let { modifier ->
                                            if (isHistory) {
                                                modifier.background(
                                                    Brush.linearGradient(
                                                        colors = listOf(
                                                            PrimaryCyan.copy(alpha = 0.2f),
                                                            PrimaryPurple.copy(alpha = 0.15f)
                                                        )
                                                    )
                                                )
                                            } else modifier
                                        }
                                        .border(
                                            width = 1.dp,
                                            color = if (isHistory) PrimaryCyan.copy(alpha = 0.3f) else Color.Transparent,
                                            shape = RoundedCornerShape(22.dp)
                                        )
                                        .clickable { currentScreen = Screen.History }
                                        .testTag("nav_history"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.History,
                                            contentDescription = "Histórico",
                                            tint = if (isHistory) PrimaryCyan else Color.White.copy(alpha = 0.6f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Histórico",
                                            color = if (isHistory) Color.White else Color.White.copy(alpha = 0.6f),
                                            fontSize = 12.sp,
                                            fontWeight = if (isHistory) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }

                        // RIGHT SIDE: Add Transaction "+" Circular FAB
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(AccentPink, PrimaryPurple, PrimaryCyan)
                                    )
                                )
                                .border(
                                    width = 1.dp,
                                    color = Color.White.copy(alpha = 0.3f),
                                    shape = CircleShape
                                )
                                .clickable { showAddTransactionPopup = true }
                                .testTag("add_transaction_fab"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Adicionar Movimentação",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal Holographic Chatbot Overlay Dialog
    if (showChatbotPopup) {
        Dialog(
            onDismissRequest = { showChatbotPopup = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .padding(horizontal = 16.dp, vertical = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .border(
                            width = 1.5.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(PrimaryCyan, PrimaryPurple, AccentPink)
                            ),
                            shape = RoundedCornerShape(24.dp)
                        ),
                    colors = CardDefaults.cardColors(containerColor = CardBackground.copy(alpha = 0.9f)),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        ChatbotScreen(
                            viewModel = viewModel,
                            onClose = { showChatbotPopup = false }
                        )
                    }
                }
            }
        }
    }

    // Modal Holographic Add Transaction Overlay Dialog
    if (showAddTransactionPopup) {
        Dialog(
            onDismissRequest = { showAddTransactionPopup = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .padding(horizontal = 16.dp, vertical = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .border(
                            width = 1.5.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(AccentPink, PrimaryPurple, PrimaryCyan)
                            ),
                            shape = RoundedCornerShape(24.dp)
                        ),
                    colors = CardDefaults.cardColors(containerColor = CardBackground.copy(alpha = 0.9f)),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AddTransactionScreen(
                            viewModel = viewModel,
                            onTransactionSaved = {
                                showAddTransactionPopup = false
                            },
                            onClose = { showAddTransactionPopup = false }
                        )
                    }
                }
            }
        }
    }

    // Modal Holographic Settings Overlay Dialog
    if (showSettingsPopup) {
        SettingsDialog(
            viewModel = viewModel,
            onDismiss = { showSettingsPopup = false }
        )
    }
}

@Composable
fun AmbientGlowingBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "ambient_glow")

    // Gentle floating orbs translation coordinates
    val dx1 by infiniteTransition.animateFloat(
        initialValue = -120f,
        targetValue = 120f,
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dx1"
    )
    val dy1 by infiniteTransition.animateFloat(
        initialValue = -180f,
        targetValue = 180f,
        animationSpec = infiniteRepeatable(
            animation = tween(18000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dy1"
    )

    val dx2 by infiniteTransition.animateFloat(
        initialValue = 180f,
        targetValue = -180f,
        animationSpec = infiniteRepeatable(
            animation = tween(16000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dx2"
    )
    val dy2 by infiniteTransition.animateFloat(
        initialValue = -120f,
        targetValue = 120f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dy2"
    )

    val scaleFactor by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        val width = size.width
        val height = size.height

        if (width <= 0f || height <= 0f) return@Canvas

        // Draw deep pure dark base layer
        drawRect(color = Color(0xFF030305))

        // Orb 1: Futuristic Cyan light
        val center1 = Offset(
            x = width * 0.25f + dx1,
            y = height * 0.35f + dy1
        )
        val radius1 = (width * 0.65f) * scaleFactor
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    PrimaryCyan.copy(alpha = 0.15f),
                    PrimaryCyan.copy(alpha = 0.04f),
                    Color.Transparent
                ),
                center = center1,
                radius = radius1.coerceAtLeast(10f)
            ),
            center = center1,
            radius = radius1
        )

        // Orb 2: Deep Holographic Violet light
        val center2 = Offset(
            x = width * 0.75f + dx2,
            y = height * 0.6f + dy2
        )
        val radius2 = (width * 0.75f) * (2f - scaleFactor)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    PrimaryPurple.copy(alpha = 0.14f),
                    PrimaryPurple.copy(alpha = 0.03f),
                    Color.Transparent
                ),
                center = center2,
                radius = radius2.coerceAtLeast(10f)
            ),
            center = center2,
            radius = radius2
        )

        // Orb 3: Soft Sunset Pulse pink aura at center top
        val center3 = Offset(
            x = width * 0.5f + (dx1 * 0.4f),
            y = height * 0.15f + (dy2 * 0.4f)
        )
        val radius3 = width * 0.55f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    AccentPink.copy(alpha = 0.12f),
                    AccentPink.copy(alpha = 0.02f),
                    Color.Transparent
                ),
                center = center3,
                radius = radius3
            ),
            center = center3,
            radius = radius3
        )
    }
}

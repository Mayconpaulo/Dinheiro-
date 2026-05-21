package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.FinanceViewModel
import kotlinx.coroutines.launch

@Composable
fun ChatbotScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var messageText by remember { mutableStateOf("") }

    // Quick suggestion questions
    val suggestions = listOf(
        "Qual é minha projeção de gastos para os próximos meses?",
        "Quanto eu gastei com Comida?",
        "Consolide o total de gastos fixos vs parcelados.",
        "Como economizar ou de onde posso cortar custos?"
    )

    // Auto-scroll to the bottom when a new message arrives
    LaunchedEffect(uiState.chatbotMessages.size) {
        if (uiState.chatbotMessages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.chatbotMessages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // --- Header of the Assistant ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(PrimaryCyan, PrimaryPurple, AccentPink))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "FinTrack AI Avatar",
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "FinTrack Chatbot AI",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(AccentGreen)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Online • Analisando suas transações",
                            color = GrayText,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.clearChat() },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(CardBackground)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reiniciar Chat",
                        tint = GrayText,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (onClose != null) {
                    IconButton(
                        onClick = { onClose() },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CardBackground)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fechar Chat",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- Message List Area ---
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.chatbotMessages) { msg ->
                    val alignment = if (msg.isUser) Alignment.End else Alignment.Start
                    val containerColor = if (msg.isUser) {
                        PrimaryCyan
                    } else {
                        CardBackground
                    }
                    val textColor = if (msg.isUser) Color.Black else Color.White

                    val bubbleShape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (msg.isUser) 16.dp else 2.dp,
                        bottomEnd = if (msg.isUser) 2.dp else 16.dp
                    )

                    val bubbleModifier = if (!msg.isUser) {
                        Modifier
                            .clip(bubbleShape)
                            .background(containerColor)
                            .border(
                                width = 1.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        PrimaryCyan.copy(alpha = 0.5f),
                                        PrimaryPurple.copy(alpha = 0.5f),
                                        AccentPink.copy(alpha = 0.3f)
                                    )
                                ),
                                shape = bubbleShape
                            )
                    } else {
                        Modifier
                            .clip(bubbleShape)
                            .background(containerColor)
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = alignment
                    ) {
                        Box(
                            modifier = bubbleModifier
                                .padding(14.dp)
                                .widthIn(max = 280.dp)
                        ) {
                            Text(
                                text = msg.text,
                                color = textColor,
                                fontSize = 14.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                // If loading, show custom shimmering/pulsating message
                if (uiState.isChatLoading) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(CardBackground),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = PrimaryCyan,
                                    strokeWidth = 2.dp
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "FinTrack AI está analisando seus dados...",
                                color = GrayText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // --- Quick Suggestions Bubbles (only visible if chat loading is false and query is blank) ---
        if (!uiState.isChatLoading) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                suggestions.forEach { suggest ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(CardBackground)
                            .clickable {
                                viewModel.sendMessageToChatbot(suggest)
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = suggest,
                                color = PrimaryCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowForwardIos,
                                contentDescription = "Seta",
                                tint = GrayText.copy(alpha = 0.5f),
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }
                }
            }
        }

        // --- Input Controls ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                placeholder = { Text("Faça perguntas sobre seus gastos...") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryCyan,
                    unfocusedBorderColor = BorderColor,
                    focusedContainerColor = CardBackground,
                    unfocusedContainerColor = CardBackground,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedPlaceholderColor = GrayText,
                    unfocusedPlaceholderColor = GrayText
                ),
                shape = RoundedCornerShape(24.dp),
                singleLine = true
            )

            FloatingActionButton(
                onClick = {
                    if (messageText.isNotBlank()) {
                        viewModel.sendMessageToChatbot(messageText.trim())
                        messageText = ""
                    }
                },
                containerColor = PrimaryCyan,
                contentColor = Color.Black,
                shape = CircleShape,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("send_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Enviar mensagem"
                )
            }
        }
    }
}

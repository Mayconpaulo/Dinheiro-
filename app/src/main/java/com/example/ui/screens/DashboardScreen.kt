package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Transaction
import com.example.ui.theme.*
import com.example.ui.viewmodel.FinanceViewModel
import com.example.ui.viewmodel.MonthlyMetrics
import java.text.SimpleDateFormat
import java.util.*

fun Modifier.holdOrClick(
    key: Any?,
    onClick: () -> Unit,
    onHoldStart: () -> Unit,
    onHoldEnd: () -> Unit
): Modifier = this.pointerInput(key) {
    detectTapGestures(
        onLongPress = {
            onClick()
        }
    )
}

fun getCategoryIconAndColor(category: String): Pair<ImageVector, Color> {
    return when (category.lowercase().trim()) {
        "comida", "alimentação", "alimentacao", "restaurante", "supermercado" -> Pair(Icons.Default.Restaurant, Color(0xFFFF9800))
        "lazer", "entretenimento", "jogos", "viagem" -> Pair(Icons.Default.Celebration, Color(0xFFE91E63))
        "moradia", "aluguel", "casa", "condomínio", "condominio" -> Pair(Icons.Default.Home, Color(0xFF2196F3))
        "transporte", "combustível", "combustivel", "uber", "ônibus" -> Pair(Icons.Default.DirectionsCar, Color(0xFF00E5FF))
        "salário", "salario", "entrada", "renda", "recebimento" -> Pair(Icons.Default.Payments, Color(0xFF00E676))
        "saúde", "saude", "farmácia", "medicina" -> Pair(Icons.Default.LocalHospital, Color(0xFFE91E63))
        "educação", "educacao", "curso", "faculdade", "livros" -> Pair(Icons.Default.School, Color(0xFFAB47BC))
        else -> Pair(Icons.Default.Category, Color(0xFF9E9EAF))
    }
}

@Composable
fun DashboardScreen(
    viewModel: FinanceViewModel,
    onNavigateToAdd: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenProfile: () -> Unit,
    onEditTransaction: (Transaction) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    // Calculated metrics for selected month offset
    val metrics = viewModel.getProjectionsForMonthOffset(uiState.selectedMonthOffset)

    var holdCategoryDetails by remember { mutableStateOf<String?>(null) }
    var clickCategoryDetails by remember { mutableStateOf<String?>(null) }
    val selectedCategoryDetails = holdCategoryDetails ?: clickCategoryDetails

    val context = LocalContext.current
    var categoryForPhotoPicking by remember { mutableStateOf<String?>(null) }

    val categoryPhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        val cat = categoryForPhotoPicking
        if (uri != null && cat != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val file = java.io.File(context.filesDir, "category_${cat.lowercase().replace(" ", "_").replace("/", "_")}.jpg")
                val outputStream = java.io.FileOutputStream(file)
                inputStream?.use { input ->
                    outputStream.use { output ->
                        input.copyTo(output)
                    }
                }
                viewModel.setCategoryCustomImage(cat, file.absolutePath)
                Toast.makeText(context, "Logo da categoria atualizado!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Erro ao carregar a foto.", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
        categoryForPhotoPicking = null
    }

    var holdTransactionDetails by remember { mutableStateOf<Transaction?>(null) }
    var clickTransactionDetails by remember { mutableStateOf<Transaction?>(null) }
    val selectedTransactionDetails = holdTransactionDetails ?: clickTransactionDetails

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        // --- Header with custom profile ---
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onOpenProfile() }
                        .padding(4.dp)
                ) {
                    if (uiState.userProfileImageUri != null) {
                        AsyncImage(
                            model = java.io.File(uiState.userProfileImageUri!!),
                            contentDescription = "Foto de perfil",
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, PrimaryCyan, CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(PrimaryCyan, PrimaryPurple, AccentPink))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = uiState.userAvatar.ifBlank { "👤" },
                                fontSize = 24.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        val displayName = if (uiState.userName.isNotBlank()) uiState.userName else "bem-vindo!"
                        Text(
                            text = "Olá, $displayName",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(AccentGreen)
                            )
                            Text(
                                text = "Google Sincronizado",
                                color = AccentGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Security visibility toggle icon (eye icon)
                    IconButton(
                        onClick = { viewModel.toggleValuesHidden() },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(CardBackground)
                            .testTag("dashboard_security_eye_icon")
                    ) {
                        Icon(
                            imageVector = if (uiState.isValuesHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Ocultar Valores",
                            tint = if (uiState.isValuesHidden) AccentPink else PrimaryCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(CardBackground)
                            .testTag("dashboard_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Configurações",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // --- Horizontal Projections & Months Picker ---
        item {
            Column {
                Text(
                    text = "Visualizar Mês / Projeções",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollState),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (offset in 0..5) {
                        val targetCal = Calendar.getInstance().apply { add(Calendar.MONTH, offset) }
                        val monthLabel = SimpleDateFormat("MMM", Locale("pt", "BR"))
                            .format(targetCal.time)
                            .replaceFirstChar { it.uppercase() }
                        val label = if (offset == 0) "Atual ($monthLabel)" else monthLabel
                        val isSelected = uiState.selectedMonthOffset == offset

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (isSelected) {
                                        Brush.linearGradient(
                                            colors = listOf(
                                                PrimaryCyan.copy(alpha = 0.25f),
                                                PrimaryPurple.copy(alpha = 0.25f),
                                                AccentPink.copy(alpha = 0.15f)
                                            )
                                        )
                                    } else {
                                        Brush.linearGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = 0.05f),
                                                Color.White.copy(alpha = 0.02f)
                                            )
                                        )
                                    }
                                )
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    brush = if (isSelected) {
                                        Brush.linearGradient(
                                            colors = listOf(PrimaryCyan, PrimaryPurple, AccentPink)
                                        )
                                    } else {
                                        Brush.linearGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = 0.15f),
                                                Color.White.copy(alpha = 0.05f)
                                            )
                                        )
                                    },
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .clickable { viewModel.setMonthOffset(offset) }
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }

        // --- Projections Alert Notification ---
        if (uiState.selectedMonthOffset > 0) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBackground.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(PrimaryCyan.copy(0.3f), PrimaryPurple.copy(0.3f))))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Insight Icon",
                            tint = PrimaryCyan,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Projeção Inteligente de Gastos",
                                color = PrimaryCyan,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Estes valores representam os seus gastos fixos contínuos e prestações parceladas que vencem em ${metrics.monthName}.",
                                color = GrayText,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }

        // --- Primary FinTrack Metric Panel (Total Spending & Income) ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Saídas Previstas (${metrics.monthName})",
                        color = GrayText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (uiState.isValuesHidden) "R$ ••••" else "R$ ${"%,.2f".format(metrics.expenseTotal)}",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = "Entradas",
                                tint = AccentGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (uiState.isValuesHidden) "Entradas: R$ ••••" else "Entradas: R$ ${"%,.2f".format(metrics.incomeTotal)}",
                                color = AccentGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        val progressPct = if (metrics.incomeTotal > 0) {
                            (metrics.expenseTotal / metrics.incomeTotal).coerceIn(0.0, 1.0)
                        } else 0.0

                        Text(
                            text = "Comprometido: ${(progressPct * 100).toInt()}%",
                            color = if (progressPct > 0.8) AccentPink else PrimaryCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // --- Custom Radial Graphic for Balance ---
                    Box(
                        modifier = Modifier.size(170.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(140.dp)) {
                            // Background track
                            drawArc(
                                color = BorderColor,
                                startAngle = 140f,
                                sweepAngle = 260f,
                                useCenter = false,
                                style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                            )

                            // Income/Expense Progress
                            val sweep = if (metrics.incomeTotal > 0) {
                                (metrics.expenseTotal / metrics.incomeTotal).toFloat() * 260f
                            } else 0f
                            val progressSweep = sweep.coerceIn(0f, 260f)

                            if (progressSweep > 0) {
                                drawArc(
                                    brush = Brush.linearGradient(
                                        colors = listOf(PrimaryCyan, PrimaryPurple, AccentPink)
                                    ),
                                    startAngle = 140f,
                                    sweepAngle = progressSweep,
                                    useCenter = false,
                                    style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Livre / Saldo",
                                color = GrayText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (uiState.isValuesHidden) "R$ ••••" else "R$ ${"%,.0f".format(metrics.balance)}",
                                color = if (metrics.balance >= 0) AccentGreen else AccentPink,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }

        // --- Category Breakdown ---
        item {
            Text(
                text = "Gastos por Categoria",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (metrics.categoryBreakdown.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardBackground)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Inbox,
                            contentDescription = "Sem dados",
                            tint = GrayText,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Nenhum gasto registrado para este mês.",
                            color = GrayText,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(metrics.categoryBreakdown.entries.toList()) { entry ->
                val category = entry.key
                val spent = entry.value
                val pct = if (metrics.expenseTotal > 0) (spent / metrics.expenseTotal) else 0.0

                val iconAndColor = getCategoryIconAndColor(category)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .holdOrClick(
                            key = category,
                            onClick = { clickCategoryDetails = category },
                            onHoldStart = { holdCategoryDetails = category },
                            onHoldEnd = { holdCategoryDetails = null }
                        ),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val customImage = viewModel.getCategoryCustomImage(category)
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (customImage == null) iconAndColor.second.copy(alpha = 0.2f) else Color.Transparent),
                            contentAlignment = Alignment.Center
                        ) {
                            if (customImage != null) {
                                AsyncImage(
                                    model = java.io.File(customImage),
                                    contentDescription = category,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = iconAndColor.first,
                                    contentDescription = category,
                                    tint = iconAndColor.second
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = category,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (uiState.isValuesHidden) "R$ ••••" else "R$ ${"%,.2f".format(spent)}",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { pct.toFloat() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = iconAndColor.second,
                                trackColor = BorderColor
                            )
                        }
                    }
                }
            }
        }

        // --- Recent Transactions ---
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Movimentações no Mês",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                if (metrics.transactionsForMonth.isNotEmpty()) {
                    Text(
                        text = "${metrics.transactionsForMonth.size} itens",
                        color = PrimaryCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (metrics.transactionsForMonth.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Nenhuma movimentação identificada.",
                            color = GrayText,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        } else {
            items(metrics.transactionsForMonth.take(10)) { tx ->
                val df = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val txIconColor = getCategoryIconAndColor(tx.category)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .holdOrClick(
                            key = tx,
                            onClick = { clickTransactionDetails = tx },
                            onHoldStart = { holdTransactionDetails = tx },
                            onHoldEnd = { holdTransactionDetails = null }
                        )
                        .testTag("transaction_item_${tx.id}"),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (tx.type == "gasto") AccentPink.copy(alpha = 0.15f)
                                    else AccentGreen.copy(alpha = 0.15f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (tx.type == "gasto") Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                contentDescription = tx.type,
                                tint = if (tx.type == "gasto") AccentPink else AccentGreen
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (uiState.isValuesHidden) "••••" else tx.name,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${df.format(Date(tx.date))} • ${tx.category}",
                                    color = GrayText,
                                    fontSize = 11.sp
                                )
                                if (tx.bankOrNote.isNotEmpty()) {
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "• ${tx.bankOrNote}",
                                        color = PrimaryPurple.copy(alpha = 0.8f),
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            if (tx.expenseType == "parcelado") {
                                Text(
                                    text = "Parcela ${tx.paidInstallments + 1}/${tx.totalInstallments} (Faltam ${tx.remainingInstallments})",
                                    color = PrimaryCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            } else if (tx.expenseType == "fixo") {
                                Text(
                                    text = "Mensal / Fixo",
                                    color = PrimaryPurple,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (uiState.isValuesHidden) "R$ ••••" else (if (tx.type == "gasto") "- R$ ${"%,.2f".format(tx.amount)}" else "+ R$ ${"%,.2f".format(tx.amount)}"),
                            color = if (tx.type == "gasto") AccentPink else AccentGreen,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // --- Popup details for a selected Category ---
    if (selectedCategoryDetails != null) {
        val category = selectedCategoryDetails!!
        val categoryTxList = uiState.transactions.filter { it.category.equals(category, ignoreCase = true) }
        val categoryTotal = categoryTxList.sumOf { if (it.type == "gasto") it.amount else 0.0 }
        val limitSpent = categoryTxList.sumOf {
            if (it.type == "gasto") {
                if (it.expenseType == "parcelado" && it.totalInstallments > 0) {
                    it.amount * it.totalInstallments
                } else {
                    it.amount
                }
            } else {
                0.0
            }
        }
        val categoryIconAndColor = getCategoryIconAndColor(category)

        val creditLimit = viewModel.getCategoryLimit(category)
        var isEditingLimit by remember(category) { mutableStateOf(false) }
        var limitInputText by remember(category) { mutableStateOf(creditLimit?.toString() ?: "") }

        Dialog(onDismissRequest = { holdCategoryDetails = null; clickCategoryDetails = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.85f)
                    .border(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(listOf(categoryIconAndColor.second, PrimaryPurple)),
                        shape = RoundedCornerShape(24.dp)
                    ),
                colors = CardDefaults.cardColors(containerColor = CardBackground.copy(alpha = 0.98f)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val catCustomImg = viewModel.getCategoryCustomImage(category)
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (catCustomImg == null) categoryIconAndColor.second.copy(alpha = 0.2f) else Color.Transparent)
                                    .clickable {
                                        categoryForPhotoPicking = category
                                        categoryPhotoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (catCustomImg != null) {
                                    AsyncImage(
                                        model = java.io.File(catCustomImg),
                                        contentDescription = category,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        imageVector = categoryIconAndColor.first,
                                        contentDescription = category,
                                        tint = categoryIconAndColor.second
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = category,
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Logo de banco editável",
                                    color = GrayText,
                                    fontSize = 11.sp,
                                    modifier = Modifier.clickable {
                                        categoryForPhotoPicking = category
                                        categoryPhotoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                )
                            }
                        }

                        IconButton(
                            onClick = { holdCategoryDetails = null; clickCategoryDetails = null },
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.08f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Fechar",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Total Row Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(BorderColor.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                            .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Total de Saídas:",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (uiState.isValuesHidden) "R$ ••••" else "R$ ${"%,.2f".format(categoryTotal)}",
                                color = AccentPink,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    // --- Credit Limit Box ---
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(14.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                            .padding(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CreditCard,
                                        contentDescription = null,
                                        tint = PrimaryCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Limite de Crédito",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                if (!isEditingLimit) {
                                    Text(
                                        text = "Editar",
                                        color = PrimaryCyan,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .clickable { isEditingLimit = true }
                                            .padding(4.dp)
                                    )
                                }
                            }

                            if (isEditingLimit) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = limitInputText,
                                        onValueChange = { limitInputText = it },
                                        placeholder = { Text("Valor do limite (ex: 5000)", fontSize = 11.sp, color = GrayText) },
                                        modifier = Modifier.weight(1f).height(48.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = PrimaryCyan,
                                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                                        ),
                                        singleLine = true
                                    )

                                    Button(
                                        onClick = {
                                            val value = limitInputText.toDoubleOrNull()
                                            viewModel.setCategoryLimit(category, value)
                                            isEditingLimit = false
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan),
                                        contentPadding = PaddingValues(horizontal = 10.dp),
                                        modifier = Modifier.height(36.dp)
                                    ) {
                                        Text("Salvar", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Text(
                                        text = "Cancelar",
                                        color = GrayText,
                                        fontSize = 11.sp,
                                        modifier = Modifier.clickable {
                                            isEditingLimit = false
                                            limitInputText = creditLimit?.toString() ?: ""
                                        }
                                    )
                                }
                            } else {
                                if (creditLimit != null) {
                                    val spent = limitSpent
                                    val remaining = creditLimit - spent
                                    val pct = if (creditLimit > 0) (spent / creditLimit).coerceIn(0.0, 1.0) else 0.0

                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Limite: R$ ${"%,.2f".format(creditLimit)}", color = GrayText, fontSize = 11.sp)
                                            Text("Utilizado: R$ ${"%,.2f".format(spent)}", color = AccentPink, fontSize = 11.sp)
                                        }

                                        // Progress Bar for Credit Limit
                                        LinearProgressIndicator(
                                            progress = { pct.toFloat() },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(3.dp)),
                                            color = if (pct >= 0.9) AccentPink else PrimaryCyan,
                                            trackColor = Color.White.copy(alpha = 0.08f)
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "Disponível: R$ ${"%,.2f".format(remaining)}",
                                                color = if (remaining >= 0) AccentGreen else AccentPink,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${(pct * 100).toInt()}% consumido",
                                                color = GrayText,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                } else {
                                    Text(
                                        text = "Nenhum limite cadastrado para esta categoria de gastos / banco. Clique em 'Editar' para definir e controlar seus limites.",
                                        color = GrayText,
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = "Movimentações (${categoryTxList.size})",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    if (categoryTxList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Nenhum gasto nesta categoria ainda.",
                                color = GrayText,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(categoryTxList) { tx ->
                                val df = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(CardBackground, RoundedCornerShape(12.dp))
                                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                                        .clickable {
                                            clickTransactionDetails = tx
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (uiState.isValuesHidden) "••••" else tx.name,
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = df.format(Date(tx.date)),
                                            color = GrayText,
                                            fontSize = 10.sp
                                        )
                                        if (tx.expenseType == "parcelado") {
                                            Text(
                                                text = "Parcela ${tx.paidInstallments + 1}/${tx.totalInstallments}",
                                                color = PrimaryCyan,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (uiState.isValuesHidden) "R$ ••••" else (if (tx.type == "gasto") "- R$ ${"%,.2f".format(tx.amount)}" else "+ R$ ${"%,.2f".format(tx.amount)}"),
                                        color = if (tx.type == "gasto") AccentPink else AccentGreen,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Popup details with advanced options for a selected Transaction ---
    if (selectedTransactionDetails != null) {
        val tx = selectedTransactionDetails!!
        val df = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        var showConfirmDelete by remember { mutableStateOf(false) }

        Dialog(onDismissRequest = { holdTransactionDetails = null; clickTransactionDetails = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .wrapContentHeight()
                    .border(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                if (tx.type == "gasto") AccentPink else AccentGreen,
                                PrimaryPurple
                            )
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ),
                colors = CardDefaults.cardColors(containerColor = CardBackground.copy(alpha = 0.98f)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Icon
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(
                                if (tx.type == "gasto") AccentPink.copy(alpha = 0.15f)
                                else AccentGreen.copy(alpha = 0.15f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (tx.type == "gasto") Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                            contentDescription = tx.type,
                            tint = if (tx.type == "gasto") AccentPink else AccentGreen,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Text(
                        text = if (uiState.isValuesHidden) "••••" else tx.name,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    // Detail Row Info
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(BorderColor.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                            .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Valor:", color = GrayText, fontSize = 13.sp)
                            Text(
                                text = if (uiState.isValuesHidden) "R$ ••••" else "R$ ${"%,.2f".format(tx.amount)}",
                                color = if (tx.type == "gasto") AccentPink else AccentGreen,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Divider(color = BorderColor)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Tipo de Lançamento:", color = GrayText, fontSize = 13.sp)
                            Text(
                                text = if (tx.type == "gasto") "Gasto / Saída" else "Renda / Entrada",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Divider(color = BorderColor)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Data:", color = GrayText, fontSize = 13.sp)
                            Text(
                                text = df.format(Date(tx.date)),
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Divider(color = BorderColor)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Categoria / Origem:", color = GrayText, fontSize = 13.sp)
                            Text(
                                text = tx.category,
                                color = PrimaryCyan,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (tx.expenseType.isNotEmpty()) {
                            Divider(color = BorderColor)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Perfil de Recorrência:", color = GrayText, fontSize = 13.sp)
                                Text(
                                    text = if (tx.expenseType == "parcelado") "Parcelamento" else "Fixo / Mensal",
                                    color = PrimaryPurple,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (tx.expenseType == "parcelado") {
                            Divider(color = BorderColor)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Parcelamento Atual:", color = GrayText, fontSize = 13.sp)
                                Text(
                                    text = "${tx.paidInstallments + 1}ª de ${tx.totalInstallments} parcelas",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Divider(color = BorderColor)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Parcelas Faltantes:", color = GrayText, fontSize = 13.sp)
                                Text(
                                    text = "${tx.remainingInstallments} restantes",
                                    color = AccentPink,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (showConfirmDelete) {
                        Text(
                            text = "Deseja realmente apagar esta movimentação?",
                            color = AccentPink,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { showConfirmDelete = false },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = BorderColor)
                            ) {
                                Text("Voltar", color = Color.White)
                            }
                            Button(
                                onClick = {
                                    viewModel.deleteTransaction(tx)
                                    showConfirmDelete = false
                                    holdTransactionDetails = null
                                    clickTransactionDetails = null
                                },
                                modifier = Modifier.weight(1.2f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3366))
                            ) {
                                Text("Sim, Excluir", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { showConfirmDelete = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3366).copy(alpha = 0.15f)),
                                border = BorderStroke(1.dp, Color(0xFFFF3366).copy(alpha = 0.4f)),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Excluir", color = Color(0xFFFF7A8A), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Button(
                                onClick = {
                                    onEditTransaction(tx)
                                    holdTransactionDetails = null
                                    clickTransactionDetails = null
                                    holdCategoryDetails = null
                                    clickCategoryDetails = null
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple.copy(alpha = 0.2f)),
                                border = BorderStroke(1.5.dp, PrimaryPurple.copy(alpha = 0.6f)),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Editar", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Button(
                                onClick = { holdTransactionDetails = null; clickTransactionDetails = null },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Voltar", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
}

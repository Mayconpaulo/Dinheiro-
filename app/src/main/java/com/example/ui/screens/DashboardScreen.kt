package com.example.ui.screens

import androidx.compose.animation.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Transaction
import com.example.ui.theme.*
import com.example.ui.viewmodel.FinanceViewModel
import com.example.ui.viewmodel.MonthlyMetrics
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    viewModel: FinanceViewModel,
    onNavigateToAdd: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    // Calculated metrics for selected month offset
    val metrics = viewModel.getProjectionsForMonthOffset(uiState.selectedMonthOffset)

    LazyColumn(
        modifier = modifier
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
                        tint = Color.White
                    )
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
                        text = "R$ ${"%,.2f".format(metrics.expenseTotal)}",
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
                                text = "Entradas: R$ ${"%,.2f".format(metrics.incomeTotal)}",
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
                                text = "R$ ${"%,.0f".format(metrics.balance)}",
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
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(iconAndColor.second.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = iconAndColor.first,
                                contentDescription = category,
                                tint = iconAndColor.second
                            )
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
                                    text = "R$ ${"%,.2f".format(spent)}",
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
                                text = tx.name,
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
                            text = if (tx.type == "gasto") "- R$ ${"%,.2f".format(tx.amount)}" else "+ R$ ${"%,.2f".format(tx.amount)}",
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

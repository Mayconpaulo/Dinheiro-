package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Transaction
import com.example.ui.theme.*
import com.example.ui.viewmodel.FinanceViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Filters state
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("todos") } // "todos", "gastos", "entradas"

    val filteredList = uiState.transactions.filter { tx ->
        // Name / bank match query
        val matchesQuery = tx.name.contains(searchQuery, ignoreCase = true) || 
                         tx.category.contains(searchQuery, ignoreCase = true) ||
                         tx.bankOrNote.contains(searchQuery, ignoreCase = true)

        val matchesFilter = when (selectedFilter) {
            "gastos" -> tx.type == "gasto"
            "entradas" -> tx.type == "entrada"
            else -> true
        }

        matchesQuery && matchesFilter
    }

    val df = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Histórico Financeiro",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        // --- Search bar ---
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Pesquisar por nome, categoria ou banco...") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_bar"),
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
            shape = RoundedCornerShape(12.dp),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search icon",
                    tint = GrayText
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear search",
                            tint = GrayText
                        )
                    }
                }
            },
            singleLine = true
        )

        // --- Filter Pills ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                Pair("todos", "Todos"),
                Pair("gastos", "Gastos"),
                Pair("entradas", "Entradas")
            ).forEach { pair ->
                val isSelected = selectedFilter == pair.first
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) PrimaryCyan else CardBackground)
                        .clickable { selectedFilter = pair.first }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = pair.second,
                        color = if (isSelected) Color.Black else Color.White,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // --- Divider line ---
        Divider(color = BorderColor, thickness = 1.dp)

        // --- List of Transactions ---
        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SearchOff,
                        contentDescription = "Não encontrado",
                        tint = GrayText,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Nenhuma transação encontrada.",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Tente alterar os termos da busca ou filtre por outro tipo.",
                        color = GrayText,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredList, key = { it.id }) { tx ->
                    var showDeleteConfirm by remember { mutableStateOf(false) }

                    if (showDeleteConfirm) {
                        AlertDialog(
                            onDismissRequest = { showDeleteConfirm = false },
                            containerColor = CardBackground,
                            title = {
                                Text(
                                    text = "Excluir transação?",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            text = {
                                Text(
                                    text = "Tem certeza que deseja excluir '${tx.name}'? Esta operação não pode ser desfeita.",
                                    color = GrayText,
                                    fontSize = 13.sp
                                )
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        viewModel.deleteTransaction(tx)
                                        showDeleteConfirm = false
                                    }
                                ) {
                                    Text("Excluir", color = AccentPink, fontWeight = FontWeight.Bold)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDeleteConfirm = false }) {
                                    Text("Cancelar", color = Color.White)
                                }
                            }
                        )
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize(),
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
                                        text = "Parcelas: ${tx.paidInstallments} pagas de ${tx.totalInstallments} (Faltam ${tx.remainingInstallments})",
                                        color = PrimaryCyan,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                } else if (tx.expenseType == "fixo") {
                                    Text(
                                        text = "Tipo: Mensal / Fixo",
                                        color = PrimaryPurple,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (tx.type == "gasto") "- R$ ${"%,.2f".format(tx.amount)}" else "+ R$ ${"%,.2f".format(tx.amount)}",
                                    color = if (tx.type == "gasto") AccentPink else AccentGreen,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                IconButton(
                                    onClick = { showDeleteConfirm = true },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Excluir transação",
                                        tint = GrayText.copy(alpha = 0.6f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

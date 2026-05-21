package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.FinanceViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    viewModel: FinanceViewModel,
    onTransactionSaved: () -> Unit,
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Form states
    var transactionType by remember { mutableStateOf("gasto") } // "gasto" or "entrada"
    var name by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var expenseType by remember { mutableStateOf("fixo") } // "fixo", "variavel", "parcelado"

    // Installment states
    var totalInstallmentsStr by remember { mutableStateOf("12") }
    var paidInstallmentsStr by remember { mutableStateOf("0") }

    // Category and Custom bank fields
    var categoryText by remember { mutableStateOf("Comida") }
    var bankOrNoteText by remember { mutableStateOf("") }

    // Validation
    var validationError by remember { mutableStateOf<String?>(null) }

    val dateFormater = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    // Quick pick lists
    val categories = if (transactionType == "gasto") {
        listOf("Comida", "Lazer", "Moradia", "Transporte", "Saúde", "Educação")
    } else {
        listOf("Salário", "Investimento", "Reembolso", "Outros")
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Nova Movimentação",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            if (onClose != null) {
                IconButton(
                    onClick = { onClose() },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Fechar",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // --- Gasto vs Entrada Switcher Tab ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(CardBackground)
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (transactionType == "gasto") AccentPink else Color.Transparent)
                    .clickable {
                        transactionType = "gasto"
                        if (categoryText == "Salário") categoryText = "Comida"
                    }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Gasto",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (transactionType == "entrada") AccentGreen else Color.Transparent)
                    .clickable {
                        transactionType = "entrada"
                        categoryText = "Salário"
                    }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Entrada",
                    color = if (transactionType == "entrada") Color.Black else Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        // --- Amount Input Card ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "VALOR DA TRANSAÇÃO",
                    color = GrayText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "R$ ",
                        color = if (transactionType == "gasto") AccentPink else AccentGreen,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black
                    )
                    BasicTextField(
                        value = amountStr,
                        onValueChange = { newValue ->
                            if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                amountStr = newValue
                            }
                        },
                        textStyle = LocalTextStyle.current.copy(
                            color = Color.White,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Start
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .widthIn(min = 120.dp, max = 220.dp)
                            .testTag("amount_input"),
                        singleLine = true
                    )
                }
            }
        }

        // --- Description Row ---
        Text(
            text = "Descrição",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            placeholder = { Text("Ex: Compra de mercado, Almoço, etc.") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("description_input"),
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
            singleLine = true
        )

        // --- Date Picker Row ---
        Text(
            text = "Data de Registro",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        OutlinedTextField(
            value = dateFormater.format(Date(selectedDate)),
            onValueChange = { },
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    val cal = Calendar.getInstance().apply { timeInMillis = selectedDate }
                    DatePickerDialog(
                        context,
                        { _, year, month, day ->
                            val selectedCal = Calendar.getInstance().apply {
                                set(Calendar.YEAR, year)
                                set(Calendar.MONTH, month)
                                set(Calendar.DAY_OF_MONTH, day)
                            }
                            selectedDate = selectedCal.timeInMillis
                        },
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH),
                        cal.get(Calendar.DAY_OF_MONTH)
                    ).show()
                },
            enabled = false, // Intercept clicks using clickable modifier
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = Color.White,
                disabledBorderColor = BorderColor,
                disabledContainerColor = CardBackground,
                disabledLabelColor = GrayText,
                disabledTrailingIconColor = PrimaryCyan
            ),
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = "Selecionar data",
                    tint = PrimaryCyan
                )
            },
            shape = RoundedCornerShape(12.dp)
        )

        // --- If Gasto: Show Type Selection (Fixo / Variável / Parcelado) ---
        if (transactionType == "gasto") {
            Text(
                text = "Tipo de Gasto",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    Pair("fixo", "Fixo"),
                    Pair("variavel", "Variável"),
                    Pair("parcelado", "Parcelado")
                ).forEach { pair ->
                    val isSelected = expenseType == pair.first
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) PrimaryCyan else CardBackground)
                            .clickable { expenseType = pair.first }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = pair.second,
                            color = if (isSelected) Color.Black else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // If Installment: Show total/paid fields and auto compute remaining
            if (expenseType == "parcelado") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(PrimaryCyan.copy(alpha = 0.2f), PrimaryPurple.copy(alpha = 0.2f))))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Configuração do Parcelamento",
                            color = PrimaryCyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Total de parcelas",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                OutlinedTextField(
                                    value = totalInstallmentsStr,
                                    onValueChange = { totalInstallmentsStr = it.filter { c -> c.isDigit() } },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = PrimaryCyan,
                                        unfocusedBorderColor = BorderColor
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    singleLine = true
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Parcelas já pagas",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                OutlinedTextField(
                                    value = paidInstallmentsStr,
                                    onValueChange = { paidInstallmentsStr = it.filter { c -> c.isDigit() } },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = PrimaryCyan,
                                        unfocusedBorderColor = BorderColor
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    singleLine = true
                                )
                            }
                        }

                        // Auto calculation of remaining installments (quantas parcelas faltam)
                        val totalVal = totalInstallmentsStr.toIntOrNull() ?: 0
                        val paidVal = paidInstallmentsStr.toIntOrNull() ?: 0
                        val remainingVal = (totalVal - paidVal).coerceAtLeast(0)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(BorderColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Parcelas Restantes (Faltam):",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "$remainingVal parcelas",
                                color = PrimaryCyan,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }

        // --- Category Selection (Fully editable as requested!) ---
        Text(
            text = "Categoria (Customizada / Editável)",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        // Show horizontal row of quick-pick choices
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.take(4).forEach { cat ->
                val isSelected = categoryText.equals(cat, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSelected) PrimaryCyan.copy(alpha = 0.2f) else CardBackground)
                        .clickable { categoryText = cat }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = cat,
                        color = if (isSelected) PrimaryCyan else Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        OutlinedTextField(
            value = categoryText,
            onValueChange = { categoryText = it },
            placeholder = { Text("Digite ou escolha a categoria") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("category_input"),
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
            singleLine = true
        )

        // --- Customizable Bank / Bank platform / Notes ("posso colocar o banco, coisas assim") ---
        Text(
            text = "Banco / Notas de Entrada (Customizado / Editável)",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        OutlinedTextField(
            value = bankOrNoteText,
            onValueChange = { bankOrNoteText = it },
            placeholder = { Text("Ex: Nubank, Banco do Brasil, Dinheiro, etc.") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("bank_input"),
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
            singleLine = true
        )

        // --- Error message if any ---
        if (validationError != null) {
            Text(
                text = validationError!!,
                color = AccentPink,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }

        // --- Save CTA ---
        Button(
            onClick = {
                val amount = amountStr.toDoubleOrNull()
                if (name.isBlank()) {
                    validationError = "Por favor, preencha a descrição do que comprou/recebeu."
                } else if (amount == null || amount <= 0) {
                    validationError = "Por favor, insira um valor válido maior que zero."
                } else if (categoryText.isBlank()) {
                    validationError = "Defina uma categoria para organizar suas finanças."
                } else {
                    validationError = null

                    val totalInst = totalInstallmentsStr.toIntOrNull() ?: 0
                    val paidInst = paidInstallmentsStr.toIntOrNull() ?: 0
                    val remainingInst = (totalInst - paidInst).coerceAtLeast(0)

                    viewModel.addTransaction(
                        name = name.trim(),
                        type = transactionType,
                        amount = amount,
                        date = selectedDate,
                        expenseType = if (transactionType == "gasto") expenseType else "",
                        totalInstallments = if (transactionType == "gasto" && expenseType == "parcelado") totalInst else 0,
                        paidInstallments = if (transactionType == "gasto" && expenseType == "parcelado") paidInst else 0,
                        remainingInstallments = if (transactionType == "gasto" && expenseType == "parcelado") remainingInst else 0,
                        category = categoryText.trim(),
                        bankOrNote = bankOrNoteText.trim()
                    )

                    onTransactionSaved()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("save_transaction_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (transactionType == "gasto") AccentPink else AccentGreen
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Salvar",
                tint = if (transactionType == "entrada") Color.Black else Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Salvar Transação",
                color = if (transactionType == "entrada") Color.Black else Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

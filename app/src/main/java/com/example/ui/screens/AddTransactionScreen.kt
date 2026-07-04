package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.filled.Add
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.window.Dialog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.FinanceViewModel
import com.example.data.model.Transaction
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    viewModel: FinanceViewModel,
    transactionToEdit: Transaction? = null,
    onTransactionSaved: () -> Unit,
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null,
    initialType: String = "gasto"
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Form states
    var transactionType by remember(transactionToEdit, initialType) { mutableStateOf(transactionToEdit?.type ?: initialType) }
    var name by remember(transactionToEdit) { mutableStateOf(transactionToEdit?.name ?: "") }
    var amountStr by remember(transactionToEdit) {
        mutableStateOf(
            if (transactionToEdit != null && transactionToEdit.expenseType == "parcelado") {
                (transactionToEdit.amount * transactionToEdit.totalInstallments).toString()
            } else {
                transactionToEdit?.amount?.toString() ?: ""
            }
        )
    }
    var selectedDate by remember(transactionToEdit) { mutableStateOf(transactionToEdit?.date ?: System.currentTimeMillis()) }
    var expenseType by remember(transactionToEdit) { mutableStateOf(transactionToEdit?.expenseType ?: "fixo") }
    var paymentMethod by remember(transactionToEdit) {
        mutableStateOf(
            if (transactionToEdit != null && transactionToEdit.bankOrNote.isNotEmpty()) {
                transactionToEdit.bankOrNote
            } else if (transactionToEdit != null && transactionToEdit.expenseType == "parcelado") {
                "Crédito"
            } else {
                "Pix"
            }
        )
    }

    LaunchedEffect(expenseType) {
        if (expenseType == "parcelado") {
            paymentMethod = "Crédito"
        }
    }

    // Installment states
    var totalInstallmentsStr by remember(transactionToEdit) { mutableStateOf(transactionToEdit?.totalInstallments?.toString() ?: "12") }
    var paidInstallmentsStr by remember(transactionToEdit) { mutableStateOf(transactionToEdit?.paidInstallments?.toString() ?: "0") }

    // Category and Custom bank fields
    val uiState by viewModel.uiState.collectAsState()
    val categories = uiState.categories

    var categoryText by remember(transactionToEdit) { mutableStateOf(transactionToEdit?.category ?: "") }
    
    // Auto preset first available category
    LaunchedEffect(categories, transactionType) {
        if (transactionToEdit == null && (categoryText.isBlank() || !categories.contains(categoryText))) {
            if (transactionType == "gasto") {
                val hasComida = categories.any { it.equals("Comida", ignoreCase = true) }
                categoryText = if (hasComida) {
                    categories.first { it.equals("Comida", ignoreCase = true) }
                } else if (categories.isNotEmpty()) {
                    categories.first()
                } else {
                    ""
                }
            } else {
                val hasSalario = categories.any { it.equals("Salário", ignoreCase = true) || it.equals("Salario", ignoreCase = true) }
                categoryText = if (hasSalario) {
                    categories.first { it.equals("Salário", ignoreCase = true) || it.equals("Salario", ignoreCase = true) }
                } else if (categories.isNotEmpty()) {
                    categories.first()
                } else {
                    ""
                }
            }
        }
    }

    var bankOrNoteText by remember { mutableStateOf("") }

    // Dynamic Category States
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryInput by remember { mutableStateOf("") }
    var selectedCategoryForAction by remember { mutableStateOf<String?>(null) }

    // Validation
    var validationError by remember { mutableStateOf<String?>(null) }

    val dateFormater = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

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
                text = if (transactionToEdit != null) "Editar Movimentação" else "Nova Movimentação",
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
                    color = if (transactionType == "gasto") Color.Black else Color.White,
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

        // --- Amount Input Box (Highly Intuitive & Clickable Box) ---
        var isAmountFocused by remember { mutableStateOf(false) }
        val amountFocusRequester = remember { FocusRequester() }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Valor da Transação",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardBackground)
                    .border(
                        width = if (isAmountFocused) 1.5.dp else 1.dp,
                        color = if (isAmountFocused) (if (transactionType == "gasto") AccentPink else AccentGreen) else BorderColor,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { amountFocusRequester.requestFocus() }
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "R$ ",
                        color = if (transactionType == "gasto") AccentPink else AccentGreen,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black
                    )
                    
                    Box(modifier = Modifier.weight(1f)) {
                        if (amountStr.isEmpty()) {
                            Text(
                                text = "0,00",
                                color = GrayText.copy(alpha = 0.5f),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        BasicTextField(
                            value = amountStr,
                            onValueChange = { newValue ->
                                val normalized = newValue.replace(',', '.')
                                if (normalized.isEmpty() || normalized.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                    amountStr = normalized
                                }
                            },
                            textStyle = LocalTextStyle.current.copy(
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                textAlign = TextAlign.Start
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(amountFocusRequester)
                                .onFocusChanged { isAmountFocused = it.isFocused }
                                .testTag("amount_input"),
                            singleLine = true
                        )
                    }
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

            Text(
                text = "Forma de Pagamento",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    Pair("Pix", "Pix"),
                    Pair("Débito", "Débito"),
                    Pair("Crédito", "Crédito")
                ).forEach { pair ->
                    val isEnabled = expenseType != "parcelado" || pair.first == "Crédito"
                    val isSelected = paymentMethod.equals(pair.first, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) {
                                    if (pair.first == "Crédito") AccentPink else PrimaryCyan
                                } else {
                                    CardBackground
                                }
                            )
                            .clickable(enabled = isEnabled) { paymentMethod = pair.first }
                            .alpha(if (isEnabled) 1.0f else 0.4f)
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

        // --- Category Selection (Dynamic, fully manageable as requested!) ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Categorias",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            // Little plus button to add a category
            IconButton(
                onClick = { showAddCategoryDialog = true },
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(PrimaryCyan.copy(alpha = 0.15f))
                    .testTag("add_category_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Adicionar categoria",
                    tint = PrimaryCyan,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Horizontal pill selector for categories
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(CardBackground)
                .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                .padding(10.dp)
        ) {
            if (categories.isEmpty()) {
                Text(
                    text = "Nenhuma categoria. Clique no '+' acima para cadastrar!",
                    color = GrayText,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    categories.forEach { cat ->
                        val isSelected = categoryText.equals(cat, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) AccentGreen.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.02f))
                                .border(
                                    width = if (isSelected) 1.5.dp else 0.dp,
                                    color = if (isSelected) AccentGreen else Color.Transparent,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .pointerInput(cat) {
                                    detectTapGestures(
                                        onTap = {
                                            // Click immediately selects the category/bank
                                            categoryText = cat
                                        },
                                        onLongPress = {
                                            // Long press lets you manage it (popup details/delete)
                                            selectedCategoryForAction = cat
                                        }
                                    )
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = cat,
                                color = if (isSelected) AccentGreen else Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

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

                    val totalInst = totalInstallmentsStr.toIntOrNull() ?: 12
                    val paidInst = paidInstallmentsStr.toIntOrNull() ?: 0
                    val remainingInst = (totalInst - paidInst).coerceAtLeast(0)

                    val finalAmount = if (transactionType == "gasto" && expenseType == "parcelado") {
                        val divisor = if (totalInst > 0) totalInst else 1
                        amount / divisor
                    } else {
                        amount
                    }

                    if (transactionToEdit != null) {
                        val updatedTx = transactionToEdit.copy(
                            name = name.trim(),
                            type = transactionType,
                            amount = finalAmount,
                            date = selectedDate,
                            expenseType = if (transactionType == "gasto") expenseType else "",
                            totalInstallments = if (transactionType == "gasto" && expenseType == "parcelado") totalInst else 0,
                            paidInstallments = if (transactionType == "gasto" && expenseType == "parcelado") paidInst else 0,
                            remainingInstallments = if (transactionType == "gasto" && expenseType == "parcelado") remainingInst else 0,
                            category = categoryText.trim(),
                            bankOrNote = if (transactionType == "gasto") paymentMethod else ""
                        )
                        viewModel.updateTransaction(updatedTx)
                    } else {
                        viewModel.addTransaction(
                            name = name.trim(),
                            type = transactionType,
                            amount = finalAmount,
                            date = selectedDate,
                            expenseType = if (transactionType == "gasto") expenseType else "",
                            totalInstallments = if (transactionType == "gasto" && expenseType == "parcelado") totalInst else 0,
                            paidInstallments = if (transactionType == "gasto" && expenseType == "parcelado") paidInst else 0,
                            remainingInstallments = if (transactionType == "gasto" && expenseType == "parcelado") remainingInst else 0,
                            category = categoryText.trim(),
                            bankOrNote = if (transactionType == "gasto") paymentMethod else ""
                        )
                    }

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
                tint = Color.Black // Rich readable contrasting color instead of white!
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Salvar Transação",
                color = Color.Black, // Rich readable black text
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }

        Spacer(modifier = Modifier.height(30.dp))
    }

    // --- Dynamic Category Management Dialogs ---
    if (showAddCategoryDialog) {
        Dialog(
            onDismissRequest = { showAddCategoryDialog = false }
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .wrapContentHeight()
                    .border(1.5.dp, PrimaryCyan, RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Nova Categoria",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = newCategoryInput,
                        onValueChange = { newCategoryInput = it },
                        placeholder = { Text("Ex: Nubank, Mercado Pago, Restaurante") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_category_dialog_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = PrimaryCyan,
                            unfocusedBorderColor = BorderColor,
                            focusedContainerColor = CardBackground,
                            unfocusedContainerColor = CardBackground
                        ),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showAddCategoryDialog = false }) {
                            Text("Cancelar", color = GrayText)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val trimmed = newCategoryInput.trim()
                                if (trimmed.isNotEmpty()) {
                                    viewModel.addCategory(trimmed)
                                    categoryText = trimmed
                                    newCategoryInput = ""
                                    showAddCategoryDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan)
                        ) {
                            Text("Adicionar", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (selectedCategoryForAction != null) {
        val cat = selectedCategoryForAction!!
        Dialog(
            onDismissRequest = { selectedCategoryForAction = null }
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .wrapContentHeight()
                    .border(1.5.dp, PrimaryCyan, RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Categoria: $cat",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Escolha se quer selecionar esta categoria para a transação atual ou excluí-la de suas opções futuras.",
                        color = GrayText,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // EXCLUDE BUTTON
                        Button(
                            onClick = {
                                viewModel.deleteCategory(cat)
                                if (categoryText.equals(cat, ignoreCase = true)) {
                                    categoryText = ""
                                }
                                selectedCategoryForAction = null
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F).copy(alpha = 0.2f)),
                            border = BorderStroke(1.dp, Color(0xFFD32F2F).copy(alpha = 0.5f))
                        ) {
                            Text("Excluir", color = Color(0xFFFF8A80), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        // SELECT BUTTON
                        Button(
                            onClick = {
                                categoryText = cat
                                selectedCategoryForAction = null
                            },
                            modifier = Modifier.weight(1.2f),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan)
                        ) {
                            Text("Selecionar", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    TextButton(onClick = { selectedCategoryForAction = null }) {
                        Text("Cancelar", color = GrayText, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

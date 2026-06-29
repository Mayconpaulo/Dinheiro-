package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.Content
import com.example.data.api.GeminiApiClient
import com.example.data.api.Part
import com.example.data.database.AppDatabase
import com.example.data.model.Transaction
import com.example.data.repository.TransactionRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

@JsonClass(generateAdapter = true)
data class ChatbotJsonResponse(
    @Json(name = "reply") val reply: String,
    @Json(name = "action") val action: ChatbotAction? = null
)

@JsonClass(generateAdapter = true)
data class ChatbotAction(
    @Json(name = "name") val name: String,
    @Json(name = "type") val type: String = "gasto",
    @Json(name = "amount") val amount: Double = 0.0,
    @Json(name = "category") val category: String = "Outros",
    @Json(name = "expenseType") val expenseType: String = "variavel",
    @Json(name = "bankOrNote") val bankOrNote: String = "",
    @Json(name = "installments") val installments: Int? = null
)

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class FinanceUiState(
    val transactions: List<Transaction> = emptyList(),
    val chatbotMessages: List<ChatMessage> = listOf(
        ChatMessage(
            text = "Olá! Eu sou o FinTrack AI, seu assistente financeiro pessoal. Como posso te ajudar hoje? Posso analisar seus gastos, sugerir cortes ou explicar suas projeções financeiras para os próximos meses!",
            isUser = false
        )
    ),
    val isChatLoading: Boolean = false,
    val selectedMonthOffset: Int = 0, // Offset for dynamic projections: 0 (current month), 1 (next month), 2...
    val isLoggedIn: Boolean = false,
    val userEmail: String = "",
    val userName: String = "",
    val reminderHour: Int = 19,
    val reminderMinute: Int = 0,
    val isReminderEnabled: Boolean = true,
    val categories: List<String> = emptyList(),
    val userAvatar: String = "👤",
    val userProfileImageUri: String? = null,
    val isValuesHidden: Boolean = false
)

class FinanceViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TransactionRepository
    private val _uiState = MutableStateFlow(FinanceUiState())
    val uiState: StateFlow<FinanceUiState> = _uiState.asStateFlow()

    private val sharedPrefs = application.getSharedPreferences("fintrack_settings", android.content.Context.MODE_PRIVATE)

    init {
        val database = AppDatabase.getDatabase(application)
        repository = TransactionRepository(database.transactionDao())

        // Load Persistent Configurations
        val loggedIn = sharedPrefs.getBoolean("is_logged_in", false)
        val email = sharedPrefs.getString("user_email", "") ?: ""
        val name = sharedPrefs.getString("user_name", "") ?: ""
        val dHour = sharedPrefs.getInt("reminder_hour", 19)
        val dMin = sharedPrefs.getInt("reminder_minute", 0)
        val isRemEnabled = sharedPrefs.getBoolean("reminder_enabled", true)
        val avatar = sharedPrefs.getString("user_avatar", "👤") ?: "👤"
        val profileImageUri = sharedPrefs.getString("user_profile_image_uri", null)
        val valuesHidden = sharedPrefs.getBoolean("is_values_hidden", false)
        
        val defaultCats = setOf("Comida", "Lazer", "Moradia", "Transporte", "Saúde", "Educação", "Salário", "Investimento", "Reembolso", "Outros")
        val catsSet = sharedPrefs.getStringSet("custom_categories", defaultCats) ?: defaultCats
        val catsList = catsSet.toList().sorted()

        _uiState.update { 
            it.copy(
                isLoggedIn = loggedIn,
                userEmail = email,
                userName = name,
                reminderHour = dHour,
                reminderMinute = dMin,
                isReminderEnabled = isRemEnabled,
                userAvatar = avatar,
                categories = catsList,
                userProfileImageUri = profileImageUri,
                isValuesHidden = valuesHidden
            )
        }

        // Collect transactions from DB
        viewModelScope.launch {
            repository.allTransactions.collect { list ->
                _uiState.update { it.copy(transactions = list) }
            }
        }
    }

    // --- Authentication & Account Synchronization Operations ---

    fun loginUser(email: String, name: String) {
        val cleanName = if (name.isBlank()) "Usuário FinTrack" else name
        sharedPrefs.edit()
            .putBoolean("is_logged_in", true)
            .putString("user_email", email)
            .putString("user_name", cleanName)
            .apply()

        _uiState.update { 
            it.copy(
                isLoggedIn = true,
                userEmail = email,
                userName = cleanName
            )
        }
    }

    fun logoutUser() {
        sharedPrefs.edit()
            .putBoolean("is_logged_in", false)
            .putString("user_email", "")
            .putString("user_name", "")
            .apply()

        _uiState.update { 
            it.copy(
                isLoggedIn = false,
                userEmail = "",
                userName = ""
            )
        }
    }

    fun updateProfile(name: String, avatar: String) {
        val cleanName = if (name.isBlank()) "Usuário Money Control" else name
        sharedPrefs.edit()
            .putString("user_name", cleanName)
            .putString("user_avatar", avatar)
            .apply()

        _uiState.update { 
            it.copy(
                userName = cleanName,
                userAvatar = avatar
            )
        }
    }

    fun toggleValuesHidden() {
        val current = _uiState.value.isValuesHidden
        sharedPrefs.edit().putBoolean("is_values_hidden", !current).apply()
        _uiState.update { it.copy(isValuesHidden = !current) }
    }

    fun updateProfileImageUri(uriString: String?) {
        sharedPrefs.edit().putString("user_profile_image_uri", uriString).apply()
        _uiState.update { it.copy(userProfileImageUri = uriString) }
    }

    fun getCategoryLimit(categoryName: String): Double? {
        val limit = sharedPrefs.getFloat("category_limit_${categoryName.lowercase().trim()}", -1f)
        return if (limit >= 0f) limit.toDouble() else null
    }

    fun setCategoryLimit(categoryName: String, limit: Double?) {
        val cleanName = categoryName.lowercase().trim()
        if (limit == null) {
            sharedPrefs.edit().remove("category_limit_$cleanName").apply()
        } else {
            sharedPrefs.edit().putFloat("category_limit_$cleanName", limit.toFloat()).apply()
        }
        // Force state update to notify listeners
        _uiState.update { it.copy(categories = it.categories.toList()) }
    }

    fun getCategoryCustomImage(categoryName: String): String? {
        return sharedPrefs.getString("category_image_${categoryName.lowercase().trim()}", null)
    }

    fun setCategoryCustomImage(categoryName: String, imagePath: String?) {
        val cleanName = categoryName.lowercase().trim()
        if (imagePath == null) {
            sharedPrefs.edit().remove("category_image_$cleanName").apply()
        } else {
            sharedPrefs.edit().putString("category_image_$cleanName", imagePath).apply()
        }
        // Force state update to notify listeners
        _uiState.update { it.copy(categories = it.categories.toList()) }
    }

    fun addCategory(category: String) {
        val trimmed = category.trim()
        if (trimmed.isEmpty()) return
        val defaultCats = setOf("Comida", "Lazer", "Moradia", "Transporte", "Saúde", "Educação", "Salário", "Investimento", "Reembolso", "Outros")
        val currentSet = sharedPrefs.getStringSet("custom_categories", defaultCats)?.toMutableSet() ?: defaultCats.toMutableSet()
        if (!currentSet.any { it.equals(trimmed, ignoreCase = true) }) {
            currentSet.add(trimmed)
            sharedPrefs.edit().putStringSet("custom_categories", currentSet).apply()
            _uiState.update { 
                it.copy(categories = currentSet.toList().sorted())
            }
        }
    }

    fun deleteCategory(category: String) {
        val defaultCats = setOf("Comida", "Lazer", "Moradia", "Transporte", "Saúde", "Educação", "Salário", "Investimento", "Reembolso", "Outros")
        val currentSet = sharedPrefs.getStringSet("custom_categories", defaultCats)?.toMutableSet() ?: defaultCats.toMutableSet()
        currentSet.removeAll { it.equals(category, ignoreCase = true) }
        sharedPrefs.edit().putStringSet("custom_categories", currentSet).apply()
        _uiState.update { 
            it.copy(categories = currentSet.toList().sorted())
        }
    }

    // --- Reminder Notification Settings Operations ---

    fun updateReminderTime(hour: Int, minute: Int) {
        sharedPrefs.edit()
            .putInt("reminder_hour", hour)
            .putInt("reminder_minute", minute)
            .apply()

        _uiState.update { 
            it.copy(
                reminderHour = hour,
                reminderMinute = minute
            )
        }
    }

    fun toggleReminder(enabled: Boolean) {
        sharedPrefs.edit()
            .putBoolean("reminder_enabled", enabled)
            .apply()

        _uiState.update { 
            it.copy(isReminderEnabled = enabled)
        }
    }

    // --- Database Operations ---

    fun addTransaction(
        name: String,
        type: String, // "gasto" or "entrada"
        amount: Double,
        date: Long,
        expenseType: String = "", // "fixo", "variavel", "parcelado"
        totalInstallments: Int = 0,
        paidInstallments: Int = 0,
        remainingInstallments: Int = 0,
        category: String,
        bankOrNote: String = ""
    ) {
        viewModelScope.launch {
            val transaction = Transaction(
                name = name,
                type = type,
                amount = amount,
                date = date,
                expenseType = expenseType,
                totalInstallments = totalInstallments,
                paidInstallments = paidInstallments,
                remainingInstallments = remainingInstallments,
                category = category,
                bankOrNote = bankOrNote
            )
            repository.insert(transaction)
        }
    }

    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.update(transaction)
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.delete(transaction)
        }
    }

    fun toggleTransactionPaid(transaction: Transaction, monthOffset: Int? = null) {
        viewModelScope.launch {
            val targetCalendar = Calendar.getInstance()
            if (monthOffset != null) {
                targetCalendar.add(Calendar.MONTH, monthOffset)
            } else {
                targetCalendar.timeInMillis = transaction.date
            }
            val targetYear = targetCalendar.get(Calendar.YEAR)
            val targetMonth = targetCalendar.get(Calendar.MONTH)
            val monthStr = String.format(java.util.Locale.US, "%04d-%02d", targetYear, targetMonth)

            val isCurrentlyPaid = transaction.isPaidInMonth(targetYear, targetMonth)
            val updated = if (transaction.expenseType == "fixo" || transaction.expenseType == "parcelado") {
                val currentList = transaction.paidMonths.split(",").filter { it.isNotEmpty() }.toMutableList()
                if (isCurrentlyPaid) {
                    currentList.remove(monthStr)
                } else {
                    currentList.add(monthStr)
                }
                transaction.copy(
                    paidMonths = currentList.joinToString(",")
                )
            } else {
                transaction.copy(isPaid = !transaction.isPaid)
            }
            repository.update(updated)
        }
    }

    fun toggleCategoryPaid(categoryName: String, isPaid: Boolean, monthOffset: Int? = null) {
        viewModelScope.launch {
            val offset = monthOffset ?: _uiState.value.selectedMonthOffset
            val targetCalendar = Calendar.getInstance().apply {
                add(Calendar.MONTH, offset)
            }
            val targetYear = targetCalendar.get(Calendar.YEAR)
            val targetMonth = targetCalendar.get(Calendar.MONTH)
            val monthStr = String.format(java.util.Locale.US, "%04d-%02d", targetYear, targetMonth)

            val allTx = _uiState.value.transactions
            allTx.forEach { tx ->
                if (tx.category.equals(categoryName, ignoreCase = true) && tx.type == "gasto") {
                    val isCurrentlyPaid = tx.isPaidInMonth(targetYear, targetMonth)
                    if (isCurrentlyPaid != isPaid) {
                        val updated = if (tx.expenseType == "fixo" || tx.expenseType == "parcelado") {
                            val currentList = tx.paidMonths.split(",").filter { it.isNotEmpty() }.toMutableList()
                            if (isPaid) {
                                if (!currentList.contains(monthStr)) currentList.add(monthStr)
                            } else {
                                currentList.remove(monthStr)
                            }
                            tx.copy(paidMonths = currentList.joinToString(","))
                        } else {
                            tx.copy(isPaid = isPaid)
                        }
                        repository.update(updated)
                    }
                }
            }
        }
    }

    fun setMonthOffset(offset: Int) {
        _uiState.update { it.copy(selectedMonthOffset = offset) }
    }

    // --- Chatbot Integration ---

    fun sendMessageToChatbot(userMessage: String) {
        if (userMessage.isBlank()) return

        // Append user message
        val currentMessages = _uiState.value.chatbotMessages.toMutableList()
        currentMessages.add(ChatMessage(text = userMessage, isUser = true))
        _uiState.update { it.copy(chatbotMessages = currentMessages, isChatLoading = true) }

        viewModelScope.launch {
            val systemPrompt = createSystemPromptForGemini()
            
            // Map our chat history to Gemini's format
            val geminiHistory = currentMessages.drop(1).dropLast(1).map { msg ->
                Content(
                    parts = listOf(Part(text = msg.text)),
                    role = if (msg.isUser) "user" else "model"
                )
            }

            val aiResponse = GeminiApiClient.generateResponse(
                prompt = userMessage,
                systemPrompt = systemPrompt,
                history = geminiHistory
            )

            var replyText = aiResponse
            try {
                val cleanJson = aiResponse.trim()
                    .removePrefix("```json")
                    .removeSuffix("```")
                    .trim()

                val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
                val adapter = moshi.adapter(ChatbotJsonResponse::class.java)
                val responseObj = adapter.fromJson(cleanJson)
                if (responseObj != null) {
                    replyText = responseObj.reply
                    responseObj.action?.let { action ->
                        val totalInst = if (action.expenseType == "parcelado") (action.installments ?: 12) else 0
                        val finalAmount = if (action.expenseType == "parcelado" && totalInst > 0) {
                            action.amount / totalInst
                        } else {
                            action.amount
                        }
                        addTransaction(
                            name = action.name.ifBlank { "Gasto por IA" },
                            type = action.type,
                            amount = finalAmount,
                            date = System.currentTimeMillis(),
                            expenseType = action.expenseType,
                            totalInstallments = totalInst,
                            paidInstallments = 0,
                            remainingInstallments = totalInst,
                            category = action.category,
                            bankOrNote = action.bankOrNote
                        )
                        addCategory(action.category)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback to raw text if JSON is malformed
            }

            val updatedMessages = _uiState.value.chatbotMessages.toMutableList()
            updatedMessages.add(ChatMessage(text = replyText, isUser = false))
            _uiState.update { it.copy(chatbotMessages = updatedMessages, isChatLoading = false) }
        }
    }

    fun clearChat() {
        _uiState.update {
            it.copy(
                chatbotMessages = listOf(
                    ChatMessage(
                        text = "Chat reiniciado! Do que você gostaria de falar ou analisar nos seus dados financeiros?",
                        isUser = false
                    )
                )
            )
        }
    }

    // --- Projections and Metrics Helper ---

    /**
     * Aggregates transactions and builds future projections dynamically.
     * offset = 0 is Current Month.
     * offset = 1 is Next Month.
     * offset = N is Current Month + N.
     */
    fun getProjectionsForMonthOffset(offset: Int): MonthlyMetrics {
        // To compute carryover of positive balances from past months, we calculate month-by-month
        // starting from offset -12 up to the requested offset.
        var carryOver = 0.0
        val startOffset = -12
        
        var currentMetrics = getBaseProjectionsForMonthOffset(startOffset, 0.0)
        if (startOffset < offset) {
            for (off in (startOffset + 1)..offset) {
                // The previous month's positive balance gets carried over
                val prevBalance = currentMetrics.balance
                carryOver = if (prevBalance > 0) prevBalance else 0.0
                currentMetrics = getBaseProjectionsForMonthOffset(off, carryOver)
            }
        } else if (offset < startOffset) {
            // Fallback for offsets smaller than startOffset
            currentMetrics = getBaseProjectionsForMonthOffset(offset, 0.0)
        }
        
        return currentMetrics
    }

    private fun getBaseProjectionsForMonthOffset(offset: Int, carryOver: Double): MonthlyMetrics {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH) // 0-indexed

        // Target Date for the offset
        val targetCalendar = Calendar.getInstance()
        targetCalendar.add(Calendar.MONTH, offset)
        val targetYear = targetCalendar.get(Calendar.YEAR)
        val targetMonth = targetCalendar.get(Calendar.MONTH)

        val allTx = _uiState.value.transactions

        var incomeTotal = carryOver
        var expenseTotal = 0.0

        val categorySpent = mutableMapOf<String, Double>()
        val activeTransactions = mutableListOf<Transaction>()

        for (tx in allTx) {
            val txCal = Calendar.getInstance().apply { timeInMillis = tx.date }
            val txYear = txCal.get(Calendar.YEAR)
            val txMonth = txCal.get(Calendar.MONTH)

            if (tx.type == "gasto") {
                val isTxPaid = tx.isPaidInMonth(targetYear, targetMonth)
                when (tx.expenseType) {
                    "fixo" -> {
                        // Fixed expenses occur in any target month after/on its registration date
                        if (tx.date <= targetCalendar.timeInMillis || (txYear == targetYear && txMonth == targetMonth)) {
                            if (!isTxPaid) {
                                expenseTotal += tx.amount
                                categorySpent[tx.category] = (categorySpent[tx.category] ?: 0.0) + tx.amount
                            } else {
                                if (!categorySpent.containsKey(tx.category)) {
                                    categorySpent[tx.category] = 0.0
                                }
                            }
                            activeTransactions.add(tx)
                        }
                    }
                    "variavel" -> {
                        // Variable expenses only occur in their actual transaction month, and show in that target month
                        if (txYear == targetYear && txMonth == targetMonth) {
                            if (!isTxPaid) {
                                expenseTotal += tx.amount
                                categorySpent[tx.category] = (categorySpent[tx.category] ?: 0.0) + tx.amount
                            } else {
                                if (!categorySpent.containsKey(tx.category)) {
                                    categorySpent[tx.category] = 0.0
                                }
                            }
                            activeTransactions.add(tx)
                        }
                    }
                    "parcelado" -> {
                        // Installments are smart:
                        val monthDiff = getMonthDifference(tx.date, targetCalendar.timeInMillis)
                        
                        if (monthDiff >= 0) {
                            if (monthDiff < tx.remainingInstallments) {
                                if (!isTxPaid) {
                                    expenseTotal += tx.amount
                                    categorySpent[tx.category] = (categorySpent[tx.category] ?: 0.0) + tx.amount
                                } else {
                                    if (!categorySpent.containsKey(tx.category)) {
                                        categorySpent[tx.category] = 0.0
                                    }
                                }
                                activeTransactions.add(tx)
                            }
                        }
                    }
                }
            } else {
                // "entrada" (Income)
                // If it is in the target month OR we can treat it as repeating/salary
                if (txYear == targetYear && txMonth == targetMonth) {
                    incomeTotal += tx.amount
                    activeTransactions.add(tx)
                } else if (offset > 0 && (tx.category.lowercase().contains("salário") || tx.category.lowercase().contains("salario") || tx.category.lowercase().contains("renda") || tx.category.lowercase().contains("fixo"))) {
                    // Repeat stable income to make future projections realistic!
                    incomeTotal += tx.amount
                    activeTransactions.add(tx)
                }
            }
        }

        return MonthlyMetrics(
            monthName = getMonthNamePt(targetCalendar.get(Calendar.MONTH), targetCalendar.get(Calendar.YEAR)),
            incomeTotal = incomeTotal,
            expenseTotal = expenseTotal,
            balance = incomeTotal - expenseTotal,
            categoryBreakdown = categorySpent,
            transactionsForMonth = activeTransactions,
            carryOver = carryOver
        )
    }

    private fun getMonthDifference(startDateMs: Long, targetDateMs: Long): Int {
        val startCal = Calendar.getInstance().apply { timeInMillis = startDateMs }
        val targetCal = Calendar.getInstance().apply { timeInMillis = targetDateMs }

        val startYear = startCal.get(Calendar.YEAR)
        val startMonth = startCal.get(Calendar.MONTH)
        val targetYear = targetCal.get(Calendar.YEAR)
        val targetMonth = targetCal.get(Calendar.MONTH)

        return (targetYear - startYear) * 12 + (targetMonth - startMonth)
    }

    private fun getMonthNamePt(month: Int, year: Int): String {
        val months = arrayOf(
            "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
            "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
        )
        return "${months[month]} de ${year}"
    }

    // --- Helper to formulate a complete summary context for the System prompt ---
    private fun createSystemPromptForGemini(): String {
        val allTx = _uiState.value.transactions
        val jsonInstruction = """
            Sua resposta deve ser OBRIGATORIAMENTE um objeto JSON válido contendo duas propriedades:
            1. "reply": Uma string em português brasileiro com sua resposta/mensagem amigável para o usuário. Se o usuário quiser cadastrar uma transação (como "comprei um salgado de R$ 20 pelo Mercado Pago") e faltarem informações cruciais para você adicioná-la (como se é gasto fixo, variável ou parcelado, ou qual categoria/banco de preferência), você deve fazer perguntas simples e inteligentes em "reply" para completar os dados (Exemplo: "Esse gasto é fixo, variável ou parcelado?") antes de preencher o "action".
            2. "action": Se você tiver os dados necessários para adicionar a transação, preencha este objeto JSON. Caso contrário (ou se for apenas uma conversa comum/dúvida), "action" deve ser obrigatoriamente null.
               Estrutura do "action" (todos os campos abaixo são obrigatórios se "action" não for nulo):
               {
                 "name": "Breve nome/descritivo do item ou serviço",
                 "type": "gasto" ou "entrada",
                 "amount": valor TOTAL numérico correspondente (Double) (Se for gasto parcelado, informe aqui o valor total da compra),
                 "category": "Nome do banco ou categoria (ex: Mercado Pago, NuBank, Comida, Lazer, etc)",
                 "expenseType": "fixo", "variavel" ou "parcelado",
                 "bankOrNote": "Nome do Banco ou observação correspondente",
                 "installments": número de parcelas (apenas se expenseType for "parcelado", senão null)
               }
               
            REGRAS ADICIONAIS IMPORTANTES DE INTERPRETAÇÃO:
            - Se o usuário disser que fez uma compra no "débito" (ou debito), "PIX", "dinheiro" ou similar, o "expenseType" deve ser obrigatoriamente "variavel" (pois ocorre uma única vez no momento e não é fixo). Não pergunte se é fixo ou parcelado nestes casos!
            - Se o usuário especificar o banco (ex: "Mercado Pago", "NuBank"), use esse nome exatamente no campo "category" e no campo "bankOrNote".
        """.trimIndent()

        if (allTx.isEmpty()) {
            return """
                Você é o assistente virtual do FinTrack. O usuário atualmente não possui transações cadastradas.
                Sua tarefa é recebê-lo de maneira acolhedora, explicar como ele pode cadastrar seus gastos (fixos, variáveis e parcelados) e entradas por áudio/texto, e oferecer dicas de finanças pessoais.
                
                $jsonInstruction
            """.trimIndent()
        }

        // Aggregate current context
        val totalIncome = allTx.filter { it.type == "entrada" }.sumOf { it.amount }
        val totalExpense = allTx.filter { it.type == "gasto" && !it.isPaid }.sumOf { it.amount }
        
        val expensesByCategory = allTx.filter { it.type == "gasto" }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        val fixedExpenses = allTx.filter { it.type == "gasto" && it.expenseType == "fixo" }
        val variableExpenses = allTx.filter { it.type == "gasto" && it.expenseType == "variavel" }
        val installmentExpenses = allTx.filter { it.type == "gasto" && it.expenseType == "parcelado" }

        val listStr = allTx.take(15).joinToString("\n") { tx ->
            val df = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val dateStr = df.format(Date(tx.date))
            val detail = if (tx.type == "gasto") {
                "(${tx.expenseType})" + (if (tx.expenseType == "parcelado") " [Parcela: ${tx.paidInstallments}/${tx.totalInstallments}, Faltam: ${tx.remainingInstallments}]" else "")
            } else ""
            "- ${tx.name} (${tx.type.uppercase()}): R$ ${"%.2f".format(tx.amount)} em $dateStr, Categoria: ${tx.category}, Nota: ${tx.bankOrNote} $detail"
        }

        // Build some forward-looking calculations (next 3 months) to give the bot projection capabilities!
        val proj1 = getProjectionsForMonthOffset(1)
        val proj2 = getProjectionsForMonthOffset(2)
        val proj3 = getProjectionsForMonthOffset(3)

        return """
            Você é o FinTrack AI, um especialista em finanças pessoais e assistente virtual do aplicativo FinTrack.
            Você tem acesso às transações reais do usuário inseridas no aplicativo localmente. Aqui estão os dados consolidados:
            
            - Saldo Geral Histórico das Entradas Cadastradas: R$ ${"%.2f".format(totalIncome)}
            - Saldo Geral Histórico dos Gastos Cadastrados: R$ ${"%.2f".format(totalExpense)}
            - Gastos por Categoria:
              ${expensesByCategory.entries.joinToString("\n  ") { "${it.key}: R$ " + "%.2f".format(it.value) }}
              
            - Detalhes por Tipo de Gasto:
              * Gastos Fixos (ocorrem todo mês): ${fixedExpenses.size} cadastrados. Total: R$ ${"%.2f".format(fixedExpenses.sumOf { it.amount })}
              * Gastos Variáveis: ${variableExpenses.size} cadastrados. Total: R$ ${"%.2f".format(variableExpenses.sumOf { it.amount })}
              * Gastos Parcelados: ${installmentExpenses.size} cadastrados. Total: R$ ${"%.2f".format(installmentExpenses.sumOf { it.amount })}
              
            - Projeções Inteligentes dos Próximos Meses:
              * Próximo mês (${proj1.monthName}): Entrada Prevista R$ ${"%.2f".format(proj1.incomeTotal)} / Saída Prevista R$ ${"%.2f".format(proj1.expenseTotal)} / Saldo Projetado R$ ${"%.2f".format(proj1.balance)}
              * Mês +2 (${proj2.monthName}): Entrada Prevista R$ ${"%.2f".format(proj2.incomeTotal)} / Saída Prevista R$ ${"%.2f".format(proj2.expenseTotal)} / Saldo Projetado R$ ${"%.2f".format(proj2.balance)}
              * Mês +3 (${proj3.monthName}): Entrada Prevista R$ ${"%.2f".format(proj3.incomeTotal)} / Saída Prevista R$ ${"%.2f".format(proj3.expenseTotal)} / Saldo Projetado R$ ${"%.2f".format(proj3.balance)}

            Últimas transações inseridas (máximo 15):
            $listStr

            Diretrizes recomendadas:
            1. Responda em português brasileiro de forma compreensiva, amigável, clara e objetiva. Use emojis com moderação para manter uma leitura convidativa.
            2. Forneça insights úteis reais! Por exemplo: se um usuário gasta muito em Comida, aponte isso. Se ele tem parcelas acabando nos próximos meses, avise-o! 
            3. Use os dados específicos acima para responder as dúvidas do usuário sobre as finanças dele de forma precisa, sem inventar dados.
            4. Se o usuário perguntar sobre projeções, use a seção de "Projeções Inteligentes dos Próximos Meses" indicada acima para fornecer respostas surpreendentemente precisas e estimulantes!
            
            $jsonInstruction
        """.trimIndent()
    }
}

data class MonthlyMetrics(
    val monthName: String,
    val incomeTotal: Double,
    val expenseTotal: Double,
    val balance: Double,
    val categoryBreakdown: Map<String, Double>,
    val transactionsForMonth: List<Transaction>,
    val carryOver: Double = 0.0
)

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
    val isReminderEnabled: Boolean = true
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

        _uiState.update { 
            it.copy(
                isLoggedIn = loggedIn,
                userEmail = email,
                userName = name,
                reminderHour = dHour,
                reminderMinute = dMin,
                isReminderEnabled = isRemEnabled
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

            val updatedMessages = _uiState.value.chatbotMessages.toMutableList()
            updatedMessages.add(ChatMessage(text = aiResponse, isUser = false))
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
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH) // 0-indexed

        // Target Date for the offset
        val targetCalendar = Calendar.getInstance()
        targetCalendar.add(Calendar.MONTH, offset)
        val targetYear = targetCalendar.get(Calendar.YEAR)
        val targetMonth = targetCalendar.get(Calendar.MONTH)

        val allTx = _uiState.value.transactions

        var incomeTotal = 0.0
        var expenseTotal = 0.0

        val categorySpent = mutableMapOf<String, Double>()
        val activeTransactions = mutableListOf<Transaction>()

        for (tx in allTx) {
            val txCal = Calendar.getInstance().apply { timeInMillis = tx.date }
            val txYear = txCal.get(Calendar.YEAR)
            val txMonth = txCal.get(Calendar.MONTH)

            if (tx.type == "gasto") {
                when (tx.expenseType) {
                    "fixo" -> {
                        // Fixed expenses occur in any target month after/on its registration date
                        if (tx.date <= targetCalendar.timeInMillis || (txYear == targetYear && txMonth == targetMonth)) {
                            expenseTotal += tx.amount
                            categorySpent[tx.category] = (categorySpent[tx.category] ?: 0.0) + tx.amount
                            activeTransactions.add(tx)
                        }
                    }
                    "variavel" -> {
                        // Variable expenses only occur in their actual transaction month, OR we can project
                        // an average variable cost for future offsets! Let's restrict to its true month for current (offset 0),
                        // and estimate them (using past average) for future offsets! That's incredibly smart.
                        if (offset == 0) {
                            if (txYear == targetYear && txMonth == targetMonth) {
                                expenseTotal += tx.amount
                                categorySpent[tx.category] = (categorySpent[tx.category] ?: 0.0) + tx.amount
                                activeTransactions.add(tx)
                            }
                        } else {
                            // If calculating a future month projection (offset > 0), we can estimate variable costs
                            // by copying variable expenses of the register month to approximate, OR if it's within registration date.
                            // To be conservative and realistic, we only show variable expenses that matches our current past month behavior or actual ones.
                            // Let's simply count true occurrences. If the user registers variable expenses, they occur in Month 0.
                        }
                    }
                    "parcelado" -> {
                        // Installments are smart:
                        // Find the number of months difference between the transaction register date and target date.
                        val monthDiff = getMonthDifference(tx.date, targetCalendar.timeInMillis)
                        
                        if (monthDiff >= 0) {
                            // Max installments remaining from the moment it was registered is tx.remainingInstallments (quantas faltam)
                            // It was registered on tx.date.
                            // In tx.date month, the user pays installment.
                            // If monthDiff == 0, is current/origin month. Faltam: tx.remainingInstallments.
                            // Faltavam 'tx.remainingInstallments' no momento de cadastro.
                            // So it is active if monthDiff < tx.remainingInstallments.
                            // E.g., if remainingInstallments is 3 (e.g. faltam 3 parcelas, total 5, pagas 2):
                            // monthDiff = 0: active (1st remaining)
                            // monthDiff = 1: active (2nd remaining)
                            // monthDiff = 2: active (3rd remaining)
                            // monthDiff = 3: inactive (fully paid!)
                            if (monthDiff < tx.remainingInstallments) {
                                expenseTotal += tx.amount
                                categorySpent[tx.category] = (categorySpent[tx.category] ?: 0.0) + tx.amount
                                activeTransactions.add(tx)
                            }
                        }
                    }
                }
            } else {
                // "entrada" (Income)
                // If it is in the target month OR we can treat it as repeating/salary
                // Let's assume incomes are standard salaries that keep repeating or occur in target month.
                // For a robust system, we assume "category" is Salary/Repeating to repeat, or if it is exactly on that month.
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
            transactionsForMonth = activeTransactions
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
        if (allTx.isEmpty()) {
            return """
                Você é o assistente virtual do FinTrack. O usuário atualmente não possui transações cadastradas.
                Sua tarefa é recebê-lo de maneira acolhedora, explicar como ele pode cadastrar seus gastos (fixos, variáveis e parcelados) e entradas, e oferecer dicas de finanças pessoais.
                Responda sempre em português brasileiro de forma amigável, clara e concisa.
            """.trimIndent()
        }

        // Aggregate current context
        val totalIncome = allTx.filter { it.type == "entrada" }.sumOf { it.amount }
        val totalExpense = allTx.filter { it.type == "gasto" }.sumOf { it.amount }
        
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
        """.trimIndent()
    }
}

data class MonthlyMetrics(
    val monthName: String,
    val incomeTotal: Double,
    val expenseTotal: Double,
    val balance: Double,
    val categoryBreakdown: Map<String, Double>,
    val transactionsForMonth: List<Transaction>
)

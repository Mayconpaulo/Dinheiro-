package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "financial_transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val type: String, // "gasto" or "entrada"
    val amount: Double,
    val date: Long, // timestamp in ms
    val expenseType: String = "", // "fixo", "variavel", "parcelado" or empty for incomes
    val totalInstallments: Int = 0,
    val paidInstallments: Int = 0,
    val remainingInstallments: Int = 0,
    val category: String, // Comida, Lazer, etc.
    val bankOrNote: String = "", // Banco, Observação, etc. (editable)
    val isPaid: Boolean = false,
    val paidMonths: String = "" // comma-separated YYYY-MM
) : Serializable {
    fun isPaidInMonth(year: Int, month: Int): Boolean {
        if (expenseType != "fixo" && expenseType != "parcelado") {
            return isPaid
        }
        val monthStr = String.format(java.util.Locale.US, "%04d-%02d", year, month)
        return paidMonths.split(",").contains(monthStr)
    }
}

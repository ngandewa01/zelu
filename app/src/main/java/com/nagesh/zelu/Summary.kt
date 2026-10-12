package com.nagesh.zelu

object Summary {

    data class MonthTotal(
        val month: String, // YYYY-MM
        val income: Double, // income + refunds
        val expense: Double,
        val net: Double,
    )

    // Transfers between your own accounts are excluded from every total.
    fun monthlyTotals(rows: List<Row>): List<MonthTotal> {
        val income = HashMap<String, Double>()
        val expense = HashMap<String, Double>()
        for (r in rows) {
            val key = r.date.take(7)
            when (r.kind) {
                "income", "refund" -> income[key] = (income[key] ?: 0.0) + r.amount
                "expense" -> expense[key] = (expense[key] ?: 0.0) + r.amount
            }
        }
        val months = (income.keys + expense.keys).toSortedSet()
        return months.map { m ->
            val inc = income[m] ?: 0.0
            val exp = expense[m] ?: 0.0
            MonthTotal(m, inc, exp, inc - exp)
        }
    }

    fun expenseByCategory(rows: List<Row>): List<Pair<String, Double>> {
        val totals = HashMap<String, Double>()
        for (r in rows) {
            if (r.kind == "expense") {
                totals[r.category] = (totals[r.category] ?: 0.0) + r.amount
            }
        }
        return totals.entries.sortedByDescending { it.value }.map { it.key to it.value }
    }
}

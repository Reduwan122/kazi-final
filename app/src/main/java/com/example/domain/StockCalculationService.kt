package com.example.domain

import com.example.data.local.DailyReportEntity

/**
 * Represents the computed stock values for a specific day in the chronological stock chain.
 */
data class DailyStockRecord(
    val date: String,
    val openingStock: Int,
    val production: Int,
    val sales: Int,
    val otherStockIn: Int = 0,
    val otherStockOut: Int = 0,
    val stockAdjustment: Int = 0,
    val closingStock: Int,
    val isNegativeStock: Boolean = false,
    val shortage: Int = 0
)

/**
 * Summary of stock movements for a specific period (e.g. Month or custom date range).
 */
data class StockSummary(
    val openingStock: Int,
    val totalProduction: Int,
    val totalSales: Int,
    val totalOtherIn: Int = 0,
    val totalOtherOut: Int = 0,
    val totalAdjustment: Int = 0,
    val netStockMovement: Int,
    val closingStock: Int,
    val hasNegativeStockWarning: Boolean = false,
    val negativeStockDates: List<String> = emptyList()
)

/**
 * Central Stock Calculation Engine for Kazi Agrotech application.
 * 
 * Core Formula:
 *   Closing Stock = Opening Stock + Production - Sales - Other Out + Other In ± Adjustment
 *   Today's Opening Stock = Previous Day's Closing Stock
 * 
 * Every screen and reporting component in the app must derive stock using this single engine.
 */
object StockCalculationService {

    /**
     * Calculates the sequential chronological stock ledger from the list of daily reports.
     * Records are sorted chronologically ascending.
     * The closing stock of day N becomes the opening stock of day N+1.
     */
    fun calculateSequentialStockLedger(
        reports: List<DailyReportEntity>,
        baselineInitialStock: Int = 0
    ): Map<String, DailyStockRecord> {
        if (reports.isEmpty()) return emptyMap()

        val sortedReports = reports.sortedBy { it.date }
        val ledger = LinkedHashMap<String, DailyStockRecord>()
        var previousClosingStock = baselineInitialStock

        for (report in sortedReports) {
            val openingStock = previousClosingStock
            val production = report.eggProduction
            val sales = report.eggSold
            val otherIn = report.otherStockIn
            val otherOut = report.otherStockOut
            val adjustment = report.stockAdjustment

            val closingStock = openingStock + production - sales - otherOut + otherIn + adjustment
            val isNegative = closingStock < 0
            val shortage = if (isNegative) -closingStock else 0

            val record = DailyStockRecord(
                date = report.date,
                openingStock = openingStock,
                production = production,
                sales = sales,
                otherStockIn = otherIn,
                otherStockOut = otherOut,
                stockAdjustment = adjustment,
                closingStock = closingStock,
                isNegativeStock = isNegative,
                shortage = shortage
            )
            ledger[report.date] = record
            previousClosingStock = closingStock
        }

        return ledger
    }

    /**
     * Calculates the latest valid closing stock across all records.
     * If no reports exist, returns baselineInitialStock.
     */
    fun calculateCurrentStock(
        reports: List<DailyReportEntity>,
        baselineInitialStock: Int = 0
    ): Int {
        if (reports.isEmpty()) return baselineInitialStock
        val ledger = calculateSequentialStockLedger(reports, baselineInitialStock)
        val latestDate = reports.maxOfOrNull { it.date } ?: return baselineInitialStock
        return ledger[latestDate]?.closingStock ?: baselineInitialStock
    }

    /**
     * Calculates the opening stock for a target date based on all prior records.
     */
    fun calculateOpeningStockForDate(
        reports: List<DailyReportEntity>,
        targetDate: String,
        baselineInitialStock: Int = 0
    ): Int {
        val priorReports = reports.filter { it.date < targetDate }
        if (priorReports.isEmpty()) return baselineInitialStock
        val ledger = calculateSequentialStockLedger(priorReports, baselineInitialStock)
        val latestPriorDate = priorReports.maxOfOrNull { it.date } ?: return baselineInitialStock
        return ledger[latestPriorDate]?.closingStock ?: baselineInitialStock
    }

    /**
     * Calculates stock summary for a specific period (such as a month or range).
     * 
     * Formula:
     *   Closing Stock = Opening Stock + Total Production + Total Other In - Total Sales - Total Other Out ± Adjustments
     */
    fun calculateStockForPeriod(
        allReports: List<DailyReportEntity>,
        startDate: String?,
        endDate: String?,
        baselineInitialStock: Int = 0
    ): StockSummary {
        if (allReports.isEmpty()) {
            return StockSummary(
                openingStock = baselineInitialStock,
                totalProduction = 0,
                totalSales = 0,
                totalOtherIn = 0,
                totalOtherOut = 0,
                totalAdjustment = 0,
                netStockMovement = 0,
                closingStock = baselineInitialStock,
                hasNegativeStockWarning = false
            )
        }

        val fullLedger = calculateSequentialStockLedger(allReports, baselineInitialStock)

        val filtered = allReports.filter { r ->
            val afterStart = startDate == null || r.date >= startDate
            val beforeEnd = endDate == null || r.date <= endDate
            afterStart && beforeEnd
        }.sortedBy { it.date }

        if (filtered.isEmpty()) {
            val openingStock = if (startDate != null) {
                calculateOpeningStockForDate(allReports, startDate, baselineInitialStock)
            } else {
                baselineInitialStock
            }
            return StockSummary(
                openingStock = openingStock,
                totalProduction = 0,
                totalSales = 0,
                totalOtherIn = 0,
                totalOtherOut = 0,
                totalAdjustment = 0,
                netStockMovement = 0,
                closingStock = openingStock,
                hasNegativeStockWarning = false
            )
        }

        val firstDateInPeriod = filtered.first().date
        val openingStock = fullLedger[firstDateInPeriod]?.openingStock
            ?: calculateOpeningStockForDate(allReports, firstDateInPeriod, baselineInitialStock)

        val totalProd = filtered.sumOf { it.eggProduction }
        val totalSales = filtered.sumOf { it.eggSold }
        val totalOtherIn = filtered.sumOf { it.otherStockIn }
        val totalOtherOut = filtered.sumOf { it.otherStockOut }
        val totalAdjustment = filtered.sumOf { it.stockAdjustment }

        val netMovement = totalProd + totalOtherIn - totalSales - totalOtherOut + totalAdjustment
        val closingStock = openingStock + netMovement

        val negativeDates = filtered.filter { (fullLedger[it.date]?.closingStock ?: 0) < 0 }.map { it.date }

        return StockSummary(
            openingStock = openingStock,
            totalProduction = totalProd,
            totalSales = totalSales,
            totalOtherIn = totalOtherIn,
            totalOtherOut = totalOtherOut,
            totalAdjustment = totalAdjustment,
            netStockMovement = netMovement,
            closingStock = closingStock,
            hasNegativeStockWarning = negativeDates.isNotEmpty(),
            negativeStockDates = negativeDates
        )
    }

    /**
     * Validates a single proposed daily report against prior stock.
     * Returns true if stock is sufficient, or false if sales exceed available stock.
     */
    fun validateDailyStockTransaction(
        openingStock: Int,
        production: Int,
        sales: Int,
        otherStockIn: Int = 0,
        otherStockOut: Int = 0,
        stockAdjustment: Int = 0
    ): Pair<Boolean, Int> {
        val closing = openingStock + production - sales - otherStockOut + otherStockIn + stockAdjustment
        return if (closing >= 0) {
            Pair(true, 0)
        } else {
            Pair(false, -closing) // shortage quantity
        }
    }
}


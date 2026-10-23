package com.keuangan.app.data.model

data class CategoryItem(
    val name: String,
    val icon: String // Emoji representation for clean aesthetic
)

object FinanceConstants {

    val expenseCategories = listOf(
        CategoryItem("Makanan & Minuman", "🍔"),
        CategoryItem("Belanja Bulanan", "🛒"),
        CategoryItem("Kopi & Jajan", "☕"),
        CategoryItem("Transportasi", "🚗"),
        CategoryItem("Tagihan & Listrik", "⚡"),
        CategoryItem("Hiburan & Liburan", "🎬"),
        CategoryItem("Kesehatan & Obat", "💊"),
        CategoryItem("Pendidikan & Buku", "📚"),
        CategoryItem("Sedekah & Donasi", "🤲"),
        CategoryItem("Lain-lain", "📦")
    )

    val incomeCategories = listOf(
        CategoryItem("Gaji Bulanan", "💼"),
        CategoryItem("Bonus & THR", "🎉"),
        CategoryItem("Bisnis & Sampingan", "🚀"),
        CategoryItem("Investasi & Deviden", "📈"),
        CategoryItem("Hadiah / Rezeki", "🎁"),
        CategoryItem("Lain-lain", "💰")
    )

    val paymentAccounts = listOf(
        "Cash (Tunai)",
        "BCA",
        "Mandiri",
        "BRI",
        "BNI",
        "GoPay",
        "OVO",
        "ShopeePay",
        "DANA"
    )

    val defaultProfiles = listOf("Suami", "Istri")
}

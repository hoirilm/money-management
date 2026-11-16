package com.keuangan.app.data.model

import com.google.gson.annotations.SerializedName

data class CategoryItem(
    @SerializedName(value = "name", alternate = ["nama"])
    val name: String,
    @SerializedName("icon")
    val icon: String = "🏷️" // Emoji representation for clean aesthetic
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

    val billCategories = listOf(
        CategoryItem("Tagihan & Listrik", "⚡"),
        CategoryItem("Wi-Fi & Internet", "🌐"),
        CategoryItem("Cicilan & Pinjaman", "💳"),
        CategoryItem("Air PDAM", "💧"),
        CategoryItem("Pendidikan / SPP", "📚"),
        CategoryItem("Hiburan / Langganan", "🎬"),
        CategoryItem("Asuransi & BPJS", "🛡️"),
        CategoryItem("Lainnya", "📦")
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

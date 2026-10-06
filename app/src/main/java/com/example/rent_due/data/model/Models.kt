package com.example.rent_due.data.model

data class RentLog(
    val id: String,
    val timestamp: Long,
    val description: String
)

data class Bill(
    val id: String,
    val name: String,
    val amount: Double,
    val paidAmount: Double = 0.0,
    val isRent: Boolean = false,
    val targetMonthKey: String // Format: "yyyy-MM" e.g. "2026-10"
) {
    val remainingAmount: Double
        get() = amount - paidAmount

    val isPaid: Boolean
        get() = remainingAmount <= 0.0
}

data class Room(
    val id: String,
    val name: String,
    val imagePath: String? = null,
    val renterName: String = "",
    val renterIdImagePath: String? = null,
    val baseRentAmount: Double = 0.0,
    val previousDueAmount: Double = 0.0,
    val bills: List<Bill> = emptyList(),
    val logs: List<RentLog> = emptyList()
)

data class Floor(
    val floorNumber: Int,
    val rooms: List<Room> = emptyList()
)

data class House(
    val id: String,
    val name: String,
    val imagePath: String? = null,
    val numberOfStories: Int,
    val floors: List<Floor> = emptyList()
)

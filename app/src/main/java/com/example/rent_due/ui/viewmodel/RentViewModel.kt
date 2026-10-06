package com.example.rent_due.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.rent_due.data.model.Bill
import com.example.rent_due.data.model.Floor
import com.example.rent_due.data.model.House
import com.example.rent_due.data.model.RentLog
import com.example.rent_due.data.model.Room
import com.example.rent_due.data.repository.RentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class RentViewModel(private val repository: RentRepository) : ViewModel() {

    private val _houses = MutableStateFlow<List<House>>(emptyList())
    val houses: StateFlow<List<House>> = _houses.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isDeleteMode = MutableStateFlow(false)
    val isDeleteMode: StateFlow<Boolean> = _isDeleteMode.asStateFlow()

    init {
        viewModelScope.launch {
            repository.housesFlow.collect { list ->
                _houses.value = list
                _isLoading.value = false
            }
        }
    }

    fun toggleDeleteMode() {
        _isDeleteMode.value = !_isDeleteMode.value
    }

    fun setDeleteMode(enabled: Boolean) {
        _isDeleteMode.value = enabled
    }

    // ======================================================
    // DATE HELPERS
    // ======================================================
    fun currentMonthKey(): String {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        return sdf.format(Date())
    }

    fun nextMonthKey(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, 1)
        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        return sdf.format(cal.time)
    }

    fun formatDisplayDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun currentMonthName(): String {
        val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        return sdf.format(Date())
    }

    fun nextMonthName(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, 1)
        val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        return sdf.format(cal.time)
    }

    fun formatMoney(amount: Double): String {
        return "৳${amount.toLong()}"
    }

    // ======================================================
    // HOUSE OPERATIONS
    // ======================================================
    fun addHouse(name: String, numberOfStories: Int, imagePath: String?) {
        val houseId = System.currentTimeMillis().toString()
        val floors = (1..numberOfStories).map { floorNum ->
            Floor(floorNumber = floorNum, rooms = emptyList())
        }
        val newHouse = House(
            id = houseId,
            name = name,
            imagePath = imagePath,
            numberOfStories = numberOfStories,
            floors = floors
        )
        viewModelScope.launch {
            repository.saveHouse(newHouse)
        }
    }

    fun updateHouseImage(houseId: String, newImagePath: String) {
        val current = _houses.value.find { it.id == houseId } ?: return
        val updated = current.copy(imagePath = newImagePath)
        viewModelScope.launch {
            repository.saveHouse(updated)
        }
    }

    fun deleteHouse(houseId: String) {
        viewModelScope.launch {
            repository.deleteHouseById(houseId)
            if (_houses.value.size <= 1) {
                _isDeleteMode.value = false
            }
        }
    }

    // ======================================================
    // FLOOR & ROOM OPERATIONS
    // ======================================================
    fun addRoom(houseId: String, floorNumber: Int) {
        val currentHouse = _houses.value.find { it.id == houseId } ?: return
        val updatedFloors = currentHouse.floors.map { floor ->
            if (floor.floorNumber == floorNumber) {
                val nextRoomIndex = floor.rooms.size + 1
                val roomNumberFormatted = String.format(Locale.US, "%02d", nextRoomIndex)
                val roomName = "$floorNumber$roomNumberFormatted"
                val newRoom = Room(
                    id = System.currentTimeMillis().toString() + "_room",
                    name = roomName
                )
                floor.copy(rooms = floor.rooms + newRoom)
            } else floor
        }
        val updatedHouse = currentHouse.copy(floors = updatedFloors)
        viewModelScope.launch {
            repository.saveHouse(updatedHouse)
        }
    }

    fun removeLastRoom(houseId: String, floorNumber: Int) {
        val currentHouse = _houses.value.find { it.id == houseId } ?: return
        val updatedFloors = currentHouse.floors.map { floor ->
            if (floor.floorNumber == floorNumber && floor.rooms.isNotEmpty()) {
                floor.copy(rooms = floor.rooms.dropLast(1))
            } else floor
        }
        val updatedHouse = currentHouse.copy(floors = updatedFloors)
        viewModelScope.launch {
            repository.saveHouse(updatedHouse)
        }
    }

    fun updateRoomImage(houseId: String, roomId: String, newImagePath: String) {
        updateRoom(houseId, roomId) { room ->
            room.copy(imagePath = newImagePath)
        }
    }

    fun updateRenterInfo(houseId: String, roomId: String, renterName: String, renterIdImagePath: String?) {
        updateRoom(houseId, roomId) { room ->
            val newLogs = listOf(
                RentLog(
                    id = System.currentTimeMillis().toString(),
                    timestamp = System.currentTimeMillis(),
                    description = "Renter information updated."
                )
            ) + room.logs
            room.copy(
                renterName = renterName,
                renterIdImagePath = renterIdImagePath,
                logs = newLogs
            )
        }
    }

    // ======================================================
    // BILLS & PAYMENTS LOGIC
    // ======================================================

    fun ensureDefaultMonthlyBills(houseId: String, roomId: String) {
        val curKey = currentMonthKey()
        updateRoom(houseId, roomId) { room ->
            val existingCurBills = room.bills.filter { it.targetMonthKey == curKey }
            val existingKeys = existingCurBills.map { if (it.isRent) "rent" else it.name.trim().lowercase() }.toSet()

            val newBills = room.bills.toMutableList()
            var stateChanged = false

            // Ensure Rent
            if ("rent" !in existingKeys) {
                newBills.add(
                    Bill(
                        id = "${System.currentTimeMillis()}_rent",
                        name = "Rent",
                        amount = room.baseRentAmount,
                        paidAmount = 0.0,
                        isRent = true,
                        targetMonthKey = curKey
                    )
                )
                stateChanged = true
            }

            // Ensure Gas
            if ("gas" !in existingKeys) {
                val lastGas = findLastKnownAmount(room.bills, "gas", curKey)
                newBills.add(
                    Bill(
                        id = "${System.currentTimeMillis()}_gas",
                        name = "Gas",
                        amount = lastGas,
                        paidAmount = 0.0,
                        isRent = false,
                        targetMonthKey = curKey
                    )
                )
                stateChanged = true
            }

            // Ensure Electricity
            if ("electricity" !in existingKeys) {
                val lastElec = findLastKnownAmount(room.bills, "electricity", curKey)
                newBills.add(
                    Bill(
                        id = "${System.currentTimeMillis()}_elec",
                        name = "Electricity",
                        amount = lastElec,
                        paidAmount = 0.0,
                        isRent = false,
                        targetMonthKey = curKey
                    )
                )
                stateChanged = true
            }

            if (stateChanged) {
                val logList = if (room.baseRentAmount > 0) {
                    listOf(
                        RentLog(
                            id = System.currentTimeMillis().toString(),
                            timestamp = System.currentTimeMillis(),
                            description = "Monthly bills were added automatically for ${currentMonthName()}."
                        )
                    ) + room.logs
                } else room.logs
                room.copy(bills = newBills, logs = logList)
            } else {
                room
            }
        }
    }

    private fun findLastKnownAmount(bills: List<Bill>, name: String, beforeMonthKey: String): Double {
        val targetName = name.trim().lowercase()
        val matching = bills.filter { !it.isRent && it.name.trim().lowercase() == targetName && it.targetMonthKey < beforeMonthKey }
        return matching.maxByOrNull { it.targetMonthKey }?.amount ?: 0.0
    }

    fun applyPayment(houseId: String, roomId: String, paymentAmount: Double) {
        val curKey = currentMonthKey()
        updateRoom(houseId, roomId) { room ->
            var remainingToPay = paymentAmount
            var newPreviousDue = room.previousDueAmount

            // 1. Pay previous due first
            if (newPreviousDue > 0 && remainingToPay > 0) {
                val previousDuePayment = if (remainingToPay >= newPreviousDue) newPreviousDue else remainingToPay
                newPreviousDue -= previousDuePayment
                remainingToPay -= previousDuePayment
            }

            // 2. Pay current month bills in order
            val updatedBills = room.bills.map { bill ->
                if (bill.targetMonthKey == curKey && remainingToPay > 0) {
                    val remainingBill = bill.remainingAmount
                    if (remainingBill > 0) {
                        if (remainingToPay >= remainingBill) {
                            remainingToPay -= remainingBill
                            bill.copy(paidAmount = bill.paidAmount + remainingBill)
                        } else {
                            val paid = remainingToPay
                            remainingToPay = 0.0
                            bill.copy(paidAmount = bill.paidAmount + paid)
                        }
                    } else bill
                } else bill
            }.toMutableList()

            // 3. Excess overpayment allocated to Rent or first current bill
            if (remainingToPay > 0) {
                val rentIdx = updatedBills.indexOfFirst { it.targetMonthKey == curKey && it.isRent }
                if (rentIdx != -1) {
                    val rentBill = updatedBills[rentIdx]
                    updatedBills[rentIdx] = rentBill.copy(paidAmount = rentBill.paidAmount + remainingToPay)
                } else {
                    val curIdx = updatedBills.indexOfFirst { it.targetMonthKey == curKey }
                    if (curIdx != -1) {
                        val firstBill = updatedBills[curIdx]
                        updatedBills[curIdx] = firstBill.copy(paidAmount = firstBill.paidAmount + remainingToPay)
                    }
                }
            }

            val newLogs = listOf(
                RentLog(
                    id = System.currentTimeMillis().toString(),
                    timestamp = System.currentTimeMillis(),
                    description = "Bills updated with payment: ${formatMoney(paymentAmount)}."
                )
            ) + room.logs

            room.copy(
                previousDueAmount = newPreviousDue,
                bills = updatedBills,
                logs = newLogs
            )
        }
    }

    fun addBill(houseId: String, roomId: String, name: String, amount: Double, isCurrentMonth: Boolean) {
        val targetMonthKey = if (isCurrentMonth) currentMonthKey() else nextMonthKey()
        val targetMonthLabel = if (isCurrentMonth) currentMonthName() else nextMonthName()

        updateRoom(houseId, roomId) { room ->
            val existing = room.bills.find {
                it.targetMonthKey == targetMonthKey && it.name.trim().equals(name.trim(), ignoreCase = true)
            }
            val newBills = room.bills.toMutableList()
            if (existing != null) {
                val idx = newBills.indexOf(existing)
                newBills[idx] = existing.copy(name = name, amount = amount)
            } else {
                newBills.add(
                    Bill(
                        id = "${System.currentTimeMillis()}_bill",
                        name = name,
                        amount = amount,
                        paidAmount = 0.0,
                        isRent = false,
                        targetMonthKey = targetMonthKey
                    )
                )
            }

            val newLogs = listOf(
                RentLog(
                    id = System.currentTimeMillis().toString(),
                    timestamp = System.currentTimeMillis(),
                    description = "Added $name bill ${formatMoney(amount)} for $targetMonthLabel."
                )
            ) + room.logs

            room.copy(bills = newBills, logs = newLogs)
        }
    }

    fun editBill(
        houseId: String,
        roomId: String,
        billId: String,
        newName: String,
        newAmount: Double,
        isCurrentMonth: Boolean
    ) {
        val targetMonthKey = if (isCurrentMonth) currentMonthKey() else nextMonthKey()
        val targetMonthLabel = if (isCurrentMonth) currentMonthName() else nextMonthName()

        updateRoom(houseId, roomId) { room ->
            val billToEdit = room.bills.find { it.id == billId } ?: return@updateRoom room
            val oldName = billToEdit.name
            val oldAmount = billToEdit.amount

            val isRent = billToEdit.isRent
            val effectiveName = if (isRent) "Rent" else newName

            val newBills = room.bills.map { stored ->
                if (!isRent && stored.name.trim().equals(oldName.trim(), ignoreCase = true)) {
                    stored.copy(name = effectiveName)
                } else stored
            }.toMutableList()

            // Find target bill for target month
            val targetExisting = newBills.find {
                it.targetMonthKey == targetMonthKey &&
                        (if (isRent) it.isRent else it.name.trim().equals(effectiveName.trim(), ignoreCase = true))
            }

            if (targetExisting != null) {
                val idx = newBills.indexOf(targetExisting)
                newBills[idx] = targetExisting.copy(name = effectiveName, amount = newAmount)
            } else {
                newBills.add(
                    Bill(
                        id = "${System.currentTimeMillis()}_edited",
                        name = effectiveName,
                        amount = newAmount,
                        paidAmount = 0.0,
                        isRent = isRent,
                        targetMonthKey = targetMonthKey
                    )
                )
            }

            val newBaseRent = if (isRent) newAmount else room.baseRentAmount

            val newLogs = listOf(
                RentLog(
                    id = System.currentTimeMillis().toString(),
                    timestamp = System.currentTimeMillis(),
                    description = "Bill updated: $oldName → $effectiveName, ${formatMoney(oldAmount)} → ${formatMoney(newAmount)} from $targetMonthLabel."
                )
            ) + room.logs

            room.copy(
                baseRentAmount = newBaseRent,
                bills = newBills,
                logs = newLogs
            )
        }
    }

    fun deleteBill(houseId: String, roomId: String, billId: String) {
        updateRoom(houseId, roomId) { room ->
            val target = room.bills.find { it.id == billId } ?: return@updateRoom room
            val newBills = room.bills.filter { it.id != billId }
            val newLogs = listOf(
                RentLog(
                    id = System.currentTimeMillis().toString(),
                    timestamp = System.currentTimeMillis(),
                    description = "Deleted bill: ${target.name} ${formatMoney(target.amount)}."
                )
            ) + room.logs
            room.copy(bills = newBills, logs = newLogs)
        }
    }

    fun addPreviousDue(houseId: String, roomId: String, amount: Double) {
        updateRoom(houseId, roomId) { room ->
            val newDue = room.previousDueAmount + amount
            val newLogs = listOf(
                RentLog(
                    id = System.currentTimeMillis().toString(),
                    timestamp = System.currentTimeMillis(),
                    description = "Previous due added: ${formatMoney(amount)}."
                )
            ) + room.logs
            room.copy(previousDueAmount = newDue, logs = newLogs)
        }
    }

    fun clearRoomHistory(houseId: String, roomId: String) {
        val curKey = currentMonthKey()
        updateRoom(houseId, roomId) { room ->
            // Clear bills, logs, previous due, base rent; recreate zero default bills for current month
            val resetBills = listOf(
                Bill(id = "${System.currentTimeMillis()}_rent", name = "Rent", amount = 0.0, paidAmount = 0.0, isRent = true, targetMonthKey = curKey),
                Bill(id = "${System.currentTimeMillis()}_gas", name = "Gas", amount = 0.0, paidAmount = 0.0, isRent = false, targetMonthKey = curKey),
                Bill(id = "${System.currentTimeMillis()}_elec", name = "Electricity", amount = 0.0, paidAmount = 0.0, isRent = false, targetMonthKey = curKey)
            )
            room.copy(
                baseRentAmount = 0.0,
                previousDueAmount = 0.0,
                bills = resetBills,
                logs = emptyList()
            )
        }
    }

    private fun updateRoom(houseId: String, roomId: String, transform: (Room) -> Room) {
        val currentHouse = _houses.value.find { it.id == houseId } ?: return
        val updatedFloors = currentHouse.floors.map { floor ->
            val updatedRooms = floor.rooms.map { room ->
                if (room.id == roomId) transform(room) else room
            }
            floor.copy(rooms = updatedRooms)
        }
        val updatedHouse = currentHouse.copy(floors = updatedFloors)
        viewModelScope.launch {
            repository.saveHouse(updatedHouse)
        }
    }
}

class RentViewModelFactory(private val repository: RentRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RentViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RentViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

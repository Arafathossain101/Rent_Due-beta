package com.example.rent_due.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import com.example.rent_due.ui.components.AdBannerView
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AssignmentInd
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HistoryToggleOff
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.rent_due.data.model.Bill
import com.example.rent_due.data.model.RentLog
import com.example.rent_due.ui.theme.BackgroundLight
import com.example.rent_due.ui.theme.BrandTeal
import com.example.rent_due.ui.theme.BrandTealDark
import com.example.rent_due.ui.theme.BrandTealLight
import com.example.rent_due.ui.theme.DangerRed
import com.example.rent_due.ui.theme.SuccessGreen
import com.example.rent_due.ui.theme.SuccessGreenLight
import com.example.rent_due.ui.theme.TextDark
import com.example.rent_due.ui.theme.WarningOrange
import com.example.rent_due.ui.theme.WarningOrangeLight
import com.example.rent_due.ui.viewmodel.RentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordScreen(
    houseId: String,
    roomId: String,
    viewModel: RentViewModel,
    onBackClick: () -> Unit
) {
    val houses by viewModel.houses.collectAsState()
    val house = houses.find { it.id == houseId }
    val room = house?.floors?.flatMap { it.rooms }?.find { it.id == roomId }

    // Automatic recurring bills check on screen launch
    LaunchedEffect(houseId, roomId) {
        viewModel.ensureDefaultMonthlyBills(houseId, roomId)
    }

    var showRenterDialog by remember { mutableStateOf(false) }
    var showIdViewerDialog by remember { mutableStateOf(false) }
    var showUpdateBillsDialog by remember { mutableStateOf(false) }
    var showAddBillDialog by remember { mutableStateOf(false) }
    var showAddDueDialog by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var billToEdit by remember { mutableStateOf<Bill?>(null) }
    var showLogSheet by remember { mutableStateOf(false) }

    val currentMonthKey = viewModel.currentMonthKey()
    val nextMonthKey = viewModel.nextMonthKey()

    // Filter current month bills to calculate total due
    val currentBills = room?.bills?.filter { it.targetMonthKey == currentMonthKey } ?: emptyList()
    val currentBillsDue = currentBills.sumOf { it.remainingAmount.coerceAtLeast(0.0) }
    val totalDue = currentBillsDue + (room?.previousDueAmount ?: 0.0)

    // Deduplicated visible bills for the "Added Bills" card carousel:
    // Show each bill identity type, preferring next-month configured amount if already created
    val addedBills = remember(room?.bills) {
        val bills = room?.bills ?: emptyList()
        val visible = mutableMapOf<String, Bill>()
        for (bill in bills) {
            val key = if (bill.isRent) "rent" else bill.name.trim().lowercase()
            val existing = visible[key]
            if (existing == null) {
                visible[key] = bill
            } else {
                if (bill.targetMonthKey == nextMonthKey && existing.targetMonthKey != nextMonthKey) {
                    visible[key] = bill
                }
            }
        }
        visible.values.sortedWith(
            compareBy<Bill> { !it.isRent }
                .thenBy {
                    when (it.name.trim().lowercase()) {
                        "gas" -> 1
                        "electricity" -> 2
                        else -> 3
                    }
                }
                .thenBy { it.name }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Room ${room?.name ?: ""}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showRenterDialog = true },
                        modifier = Modifier.testTag("renter_info_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Renter Information",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    IconButton(
                        onClick = { showClearHistoryDialog = true },
                        modifier = Modifier.testTag("clear_history_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear History",
                            tint = Color(0xFFFF8A80),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = BrandTeal)
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showLogSheet = true }
                    .testTag("view_activity_log_button"),
                color = Color(0xFFF0F4F4),
                shadowElevation = 8.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    AdBannerView(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = BrandTeal,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "View Activity Log (${room?.logs?.size ?: 0})",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandTeal
                    )
                }
            }
        }
    },
    containerColor = BackgroundLight
    ) { innerPadding ->
        if (room == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Room not found", fontSize = 20.sp, color = TextDark)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Renter Information Card
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { showRenterDialog = true }
                        .testTag("renter_info_card"),
                    shape = RoundedCornerShape(18.dp),
                    color = BrandTealLight,
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF80CBC4))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Name", fontSize = 14.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = room.renterName.ifBlank { "Not added" },
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Tap to edit",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BrandTeal
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "ID Document", fontSize = 14.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val hasId = !room.renterIdImagePath.isNullOrEmpty()
                                Surface(
                                    color = if (hasId) SuccessGreenLight else Color(0xFFE0E0E0),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (hasId) Icons.Default.Verified else Icons.Default.AssignmentInd,
                                            contentDescription = null,
                                            tint = if (hasId) SuccessGreen else Color.DarkGray,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (hasId) "Added" else "Not added",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (hasId) SuccessGreen else Color.DarkGray
                                        )
                                    }
                                }

                                if (hasId) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(
                                        onClick = { showIdViewerDialog = true },
                                        modifier = Modifier
                                            .size(38.dp)
                                            .background(BrandTeal, CircleShape)
                                            .testTag("view_id_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Visibility,
                                            contentDescription = "View ID",
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Tap to edit",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BrandTeal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Date & Total Due Overview Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFE3F2FD)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text("Current Date", fontSize = 14.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = viewModel.formatDisplayDate(System.currentTimeMillis()),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        color = if (totalDue > 0) WarningOrangeLight else SuccessGreenLight,
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp,
                            if (totalDue > 0) Color(0xFFFFCC80) else Color(0xFFA5D6A7)
                        )
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text("Total Due", fontSize = 14.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = viewModel.formatMoney(totalDue),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (totalDue > 0) WarningOrange else SuccessGreen,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Added Bills Section Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Added Bills",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    Text(
                        text = "Bill types",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Added Bills Horizontal Carousel
                if (addedBills.isEmpty()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFEEEEEE)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("No bills have been added.", fontSize = 16.sp, color = Color.Gray)
                        }
                    }
                } else {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(addedBills) { bill ->
                            BillItemCard(
                                bill = bill,
                                formatMoney = { viewModel.formatMoney(it) },
                                onClick = { billToEdit = bill }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Primary Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Button(
                        onClick = { showUpdateBillsDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp)
                            .testTag("update_bills_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandTeal)
                    ) {
                        Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(26.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Update Bills", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { showAddBillDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp)
                            .testTag("add_bill_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandTeal)
                    ) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(26.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Bills", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = { showAddDueDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .testTag("add_previous_due_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandTeal)
                ) {
                    Icon(Icons.Default.HistoryToggleOff, contentDescription = null, modifier = Modifier.size(26.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Add Previous Due", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Modal Bottom Sheet: Activity Log
    if (showLogSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
        ModalBottomSheet(
            onDismissRequest = { showLogSheet = false },
            sheetState = sheetState,
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Activity Log",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    Text(
                        text = "${room?.logs?.size ?: 0} entries",
                        fontSize = 15.sp,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Divider()

                if (room?.logs.isNullOrEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No activity yet.", fontSize = 18.sp, color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(420.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        items(room!!.logs) { log ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = BackgroundLight,
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0))
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(BrandTealLight, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.History,
                                            contentDescription = null,
                                            tint = BrandTeal,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = log.description,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = TextDark
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = viewModel.formatDisplayDate(log.timestamp),
                                            fontSize = 14.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Renter Details Dialog
    if (showRenterDialog && room != null) {
        RenterDetailsDialog(
            currentName = room.renterName,
            currentIdPath = room.renterIdImagePath,
            onDismiss = { showRenterDialog = false },
            onSave = { name, path ->
                viewModel.updateRenterInfo(houseId, roomId, name, path)
                showRenterDialog = false
            }
        )
    }

    // View ID Dialog
    if (showIdViewerDialog && room?.renterIdImagePath != null) {
        IdViewerDialog(
            imagePath = room.renterIdImagePath!!,
            onDismiss = { showIdViewerDialog = false }
        )
    }

    // Update Bills (Pay) Dialog
    if (showUpdateBillsDialog && room != null) {
        UpdateBillsDialog(
            totalDue = totalDue,
            formatMoney = { viewModel.formatMoney(it) },
            onDismiss = { showUpdateBillsDialog = false },
            onPay = { amount ->
                viewModel.applyPayment(houseId, roomId, amount)
                showUpdateBillsDialog = false
            }
        )
    }

    // Add Bill Dialog
    if (showAddBillDialog) {
        AddBillDialog(
            currentMonthLabel = viewModel.currentMonthName(),
            nextMonthLabel = viewModel.nextMonthName(),
            onDismiss = { showAddBillDialog = false },
            onAdd = { name, amount, isCurrent ->
                viewModel.addBill(houseId, roomId, name, amount, isCurrent)
                showAddBillDialog = false
            }
        )
    }

    // Edit Bill Dialog
    if (billToEdit != null) {
        EditBillDialog(
            bill = billToEdit!!,
            currentMonthLabel = viewModel.currentMonthName(),
            nextMonthLabel = viewModel.nextMonthName(),
            onDismiss = { billToEdit = null },
            onSave = { name, amount, isCurrent ->
                viewModel.editBill(houseId, roomId, billToEdit!!.id, name, amount, isCurrent)
                billToEdit = null
            },
            onDelete = {
                viewModel.deleteBill(houseId, roomId, billToEdit!!.id)
                billToEdit = null
            }
        )
    }

    // Add Previous Due Dialog
    if (showAddDueDialog) {
        AddPreviousDueDialog(
            onDismiss = { showAddDueDialog = false },
            onAdd = { amount ->
                viewModel.addPreviousDue(houseId, roomId, amount)
                showAddDueDialog = false
            }
        )
    }

    // Clear Everything Dialog
    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = DangerRed,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Clear Everything?",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = DangerRed
                )
            },
            text = {
                Text(
                    text = "This will permanently remove all bill records, payments, previous due amounts, and the activity log.\n\nRenter info, room details, and room image will remain.\n\nRent, Gas, and Electricity will reset to zero.",
                    fontSize = 17.sp,
                    color = TextDark
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearRoomHistory(houseId, roomId)
                        showClearHistoryDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Clear Everything", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("Cancel", fontSize = 17.sp, color = TextDark)
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color.White
        )
    }
}

@Composable
fun BillItemCard(
    bill: Bill,
    formatMoney: (Double) -> String,
    onClick: () -> Unit
) {
    val nameLower = bill.name.trim().lowercase()
    val icon: ImageVector = when {
        bill.isRent -> Icons.Default.Home
        nameLower == "gas" -> Icons.Default.LocalFireDepartment
        nameLower == "electricity" -> Icons.Default.Bolt
        else -> Icons.Default.ReceiptLong
    }

    val iconColor: Color = when {
        bill.isRent -> BrandTeal
        nameLower == "gas" -> Color(0xFFE65100)
        nameLower == "electricity" -> Color(0xFFF57F17)
        else -> Color(0xFF455A64)
    }

    Surface(
        modifier = Modifier
            .width(160.dp)
            .height(150.dp)
            .clickable(onClick = onClick)
            .testTag("bill_card_${bill.name}"),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 3.dp,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFE0E0E0))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = bill.name,
                tint = iconColor,
                modifier = Modifier.size(38.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = bill.name,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = formatMoney(bill.amount),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = BrandTealDark
            )
        }
    }
}

@Composable
fun RenterDetailsDialog(
    currentName: String,
    currentIdPath: String?,
    onDismiss: () -> Unit,
    onSave: (String, String?) -> Unit
) {
    var renterName by remember { mutableStateOf(currentName) }
    var idPath by remember { mutableStateOf(currentIdPath) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            idPath = uri.toString()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Renter Information", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextDark)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = renterName,
                    onValueChange = { renterName = it },
                    label = { Text("Name (Optional)", fontSize = 16.sp) },
                    placeholder = { Text("Enter renter name") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = BrandTeal) },
                    modifier = Modifier.fillMaxWidth().testTag("renter_name_input"),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(20.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = if (!idPath.isNullOrEmpty()) SuccessGreenLight else Color(0xFFF5F5F5),
                    border = androidx.compose.foundation.BorderStroke(
                        1.5.dp,
                        if (!idPath.isNullOrEmpty()) SuccessGreen else Color.LightGray
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (!idPath.isNullOrEmpty()) Icons.Default.Verified else Icons.Default.AssignmentInd,
                                contentDescription = null,
                                tint = if (!idPath.isNullOrEmpty()) SuccessGreen else Color.Gray,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ID Document (Optional)",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (!idPath.isNullOrEmpty()) "Document Attached" else "Select photo of ID.",
                            fontSize = 14.sp,
                            color = Color.DarkGray
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { photoPickerLauncher.launch("image/*") },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (!idPath.isNullOrEmpty()) "Change ID" else "Select ID",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(renterName.trim(), idPath) },
                colors = ButtonDefaults.buttonColors(containerColor = BrandTeal),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("save_renter_button")
            ) {
                Text("Save", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", fontSize = 18.sp, color = TextDark)
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White
    )
}

@Composable
fun IdViewerDialog(
    imagePath: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("ID Document", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextDark)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(26.dp))
                }
            }
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = imagePath,
                    contentDescription = "ID Document Image",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = BrandTeal),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("Close", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White
    )
}

@Composable
fun UpdateBillsDialog(
    totalDue: Double,
    formatMoney: (Double) -> String,
    onDismiss: () -> Unit,
    onPay: (Double) -> Unit
) {
    var isFullPayment by remember { mutableStateOf(false) }
    var customAmountText by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Update the Bills", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextDark)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = WarningOrangeLight,
                    border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFFB74D))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Total Bills Remaining", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatMoney(totalDue),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = WarningOrange
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Text("How do you want to pay?", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextDark)
                Spacer(modifier = Modifier.height(10.dp))

                // Option: Specific Amount
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { isFullPayment = false },
                    shape = RoundedCornerShape(14.dp),
                    color = if (!isFullPayment) BrandTeal else Color.White,
                    border = androidx.compose.foundation.BorderStroke(2.dp, if (!isFullPayment) BrandTeal else Color.Gray)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Payments,
                            contentDescription = null,
                            tint = if (!isFullPayment) Color.White else TextDark,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Specific Amount",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (!isFullPayment) Color.White else TextDark
                            )
                            Text(
                                "Pay a custom amount",
                                fontSize = 14.sp,
                                color = if (!isFullPayment) Color.White.copy(alpha = 0.9f) else Color.Gray
                            )
                        }
                    }
                }

                if (!isFullPayment) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = customAmountText,
                        onValueChange = {
                            customAmountText = it
                            errorText = null
                        },
                        label = { Text("Amount to Pay") },
                        prefix = { Text("৳ ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("payment_amount_input"),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Option: Full Payment
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { isFullPayment = true },
                    shape = RoundedCornerShape(14.dp),
                    color = if (isFullPayment) BrandTeal else Color.White,
                    border = androidx.compose.foundation.BorderStroke(2.dp, if (isFullPayment) BrandTeal else Color.Gray)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Verified,
                            contentDescription = null,
                            tint = if (isFullPayment) Color.White else TextDark,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Full Payment",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isFullPayment) Color.White else TextDark
                            )
                            Text(
                                "Pay ${formatMoney(totalDue)}",
                                fontSize = 14.sp,
                                color = if (isFullPayment) Color.White.copy(alpha = 0.9f) else Color.Gray
                            )
                        }
                    }
                }

                if (errorText != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = errorText!!, color = DangerRed, fontSize = 14.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isFullPayment) {
                        onPay(totalDue)
                    } else {
                        val amount = customAmountText.toDoubleOrNull()
                        if (amount == null || amount <= 0) {
                            errorText = "Enter a valid amount."
                        } else {
                            onPay(amount)
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrandTeal),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("confirm_payment_button")
            ) {
                Text("Update Bills", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", fontSize = 17.sp, color = TextDark)
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White
    )
}

@Composable
fun AddBillDialog(
    currentMonthLabel: String,
    nextMonthLabel: String,
    onDismiss: () -> Unit,
    onAdd: (name: String, amount: Double, isCurrent: Boolean) -> Unit
) {
    var billName by remember { mutableStateOf("") }
    var billAmount by remember { mutableStateOf("") }
    var isCurrentMonth by remember { mutableStateOf(false) } // Default next month
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Bill", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextDark) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = billName,
                    onValueChange = { billName = it; errorText = null },
                    label = { Text("Bill Name") },
                    placeholder = { Text("Water, Internet...") },
                    modifier = Modifier.fillMaxWidth().testTag("bill_name_input"),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = billAmount,
                    onValueChange = { billAmount = it; errorText = null },
                    label = { Text("Bill Amount") },
                    prefix = { Text("৳ ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("bill_amount_input"),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(18.dp))
                Text("Start this bill from:", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextDark)
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable { isCurrentMonth = true },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isCurrentMonth) BrandTeal else Color.White,
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, if (isCurrentMonth) BrandTeal else Color.Gray)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Current Month", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = if (isCurrentMonth) Color.White else TextDark)
                            Text(currentMonthLabel, fontSize = 12.sp, color = if (isCurrentMonth) Color.White.copy(alpha = 0.85f) else Color.Gray)
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable { isCurrentMonth = false },
                        shape = RoundedCornerShape(12.dp),
                        color = if (!isCurrentMonth) BrandTeal else Color.White,
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, if (!isCurrentMonth) BrandTeal else Color.Gray)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Next Month", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = if (!isCurrentMonth) Color.White else TextDark)
                            Text(nextMonthLabel, fontSize = 12.sp, color = if (!isCurrentMonth) Color.White.copy(alpha = 0.85f) else Color.Gray)
                        }
                    }
                }

                if (errorText != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = errorText!!, color = DangerRed, fontSize = 14.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val name = billName.trim()
                    val amount = billAmount.toDoubleOrNull()
                    if (name.isEmpty()) {
                        errorText = "Enter the bill name."
                    } else if (amount == null || amount <= 0) {
                        errorText = "Enter a valid amount."
                    } else {
                        onAdd(name, amount, isCurrentMonth)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrandTeal),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("save_bill_button")
            ) {
                Text("Add Bill", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", fontSize = 17.sp, color = TextDark) }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White
    )
}

@Composable
fun EditBillDialog(
    bill: Bill,
    currentMonthLabel: String,
    nextMonthLabel: String,
    onDismiss: () -> Unit,
    onSave: (name: String, amount: Double, isCurrent: Boolean) -> Unit,
    onDelete: () -> Unit
) {
    var billName by remember { mutableStateOf(bill.name) }
    var billAmount by remember { mutableStateOf(bill.amount.toLong().toString()) }
    var isCurrentMonth by remember { mutableStateOf(false) } // Default next month for safe edits
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (bill.isRent) "Edit Rent" else "Edit Bill", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextDark) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = billName,
                    onValueChange = { if (!bill.isRent) billName = it },
                    enabled = !bill.isRent,
                    label = { Text("Bill Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = billAmount,
                    onValueChange = { billAmount = it; errorText = null },
                    label = { Text("Amount") },
                    prefix = { Text("৳ ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("edit_bill_amount_input"),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(18.dp))
                Text("Start this amount from:", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextDark)
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable { isCurrentMonth = true },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isCurrentMonth) BrandTeal else Color.White,
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, if (isCurrentMonth) BrandTeal else Color.Gray)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Current Month", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = if (isCurrentMonth) Color.White else TextDark)
                            Text(currentMonthLabel, fontSize = 12.sp, color = if (isCurrentMonth) Color.White.copy(alpha = 0.85f) else Color.Gray)
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable { isCurrentMonth = false },
                        shape = RoundedCornerShape(12.dp),
                        color = if (!isCurrentMonth) BrandTeal else Color.White,
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, if (!isCurrentMonth) BrandTeal else Color.Gray)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Next Month", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = if (!isCurrentMonth) Color.White else TextDark)
                            Text(nextMonthLabel, fontSize = 12.sp, color = if (!isCurrentMonth) Color.White.copy(alpha = 0.85f) else Color.Gray)
                        }
                    }
                }

                if (errorText != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = errorText!!, color = DangerRed, fontSize = 14.sp)
                }

                if (!bill.isRent) {
                    Spacer(modifier = Modifier.height(14.dp))
                    TextButton(
                        onClick = onDelete,
                        modifier = Modifier.align(Alignment.Start)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = DangerRed)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete Bill", color = DangerRed, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = billAmount.toDoubleOrNull()
                    if (amount == null || amount < 0) {
                        errorText = "Enter a valid amount."
                    } else {
                        onSave(billName.trim(), amount, isCurrentMonth)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrandTeal),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("save_edit_bill_button")
            ) {
                Text("Save", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", fontSize = 17.sp, color = TextDark) }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White
    )
}

@Composable
fun AddPreviousDueDialog(
    onDismiss: () -> Unit,
    onAdd: (Double) -> Unit
) {
    var dueAmount by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.HistoryToggleOff, contentDescription = null, tint = BrandTeal, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text("Add Previous Due", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextDark)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = BrandTealLight,
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, BrandTeal)
                ) {
                    Text(
                        text = "Enter any unpaid amount from earlier months. It will be added to the Total Due.",
                        fontSize = 15.sp,
                        color = TextDark,
                        modifier = Modifier.padding(14.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                OutlinedTextField(
                    value = dueAmount,
                    onValueChange = { dueAmount = it; errorText = null },
                    label = { Text("Previous Due Amount") },
                    prefix = { Text("৳ ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("previous_due_input"),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                if (errorText != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = errorText!!, color = DangerRed, fontSize = 14.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = dueAmount.toDoubleOrNull()
                    if (amount == null || amount <= 0) {
                        errorText = "Enter a valid amount."
                    } else {
                        onAdd(amount)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrandTeal),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("confirm_due_button")
            ) {
                Text("Add Due", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", fontSize = 17.sp, color = TextDark) }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White
    )
}

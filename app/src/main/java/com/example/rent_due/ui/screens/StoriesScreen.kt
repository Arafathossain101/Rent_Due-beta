package com.example.rent_due.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.rent_due.data.model.Floor
import com.example.rent_due.data.model.Room
import com.example.rent_due.ui.theme.BackgroundLight
import com.example.rent_due.ui.theme.BrandTeal
import com.example.rent_due.ui.theme.BrandTealDark
import com.example.rent_due.ui.theme.BrandTealLight
import com.example.rent_due.ui.theme.DangerRed
import com.example.rent_due.ui.theme.TextDark
import com.example.rent_due.ui.viewmodel.RentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoriesScreen(
    houseId: String,
    viewModel: RentViewModel,
    onBackClick: () -> Unit,
    onRoomClick: (roomId: String) -> Unit
) {
    val houses by viewModel.houses.collectAsState()
    val house = houses.find { it.id == houseId }

    var roomToDelete by remember { mutableStateOf<Pair<Int, Room>?>(null) } // (floorNumber, room)
    var roomToChangePhoto by remember { mutableStateOf<Room?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null && roomToChangePhoto != null) {
            viewModel.updateRoomImage(houseId, roomToChangePhoto!!.id, uri.toString())
            roomToChangePhoto = null
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = house?.name ?: "House Stories",
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
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = BrandTeal)
            )
        },
        containerColor = BackgroundLight
    ) { innerPadding ->
        if (house == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("House not found", fontSize = 20.sp, color = TextDark)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(house.floors, key = { it.floorNumber }) { floor ->
                    FloorCard(
                        floor = floor,
                        onAddRoom = { viewModel.addRoom(houseId, floor.floorNumber) },
                        onRemoveRoom = {
                            if (floor.rooms.isNotEmpty()) {
                                roomToDelete = Pair(floor.floorNumber, floor.rooms.last())
                            }
                        },
                        onRoomClick = { room -> onRoomClick(room.id) },
                        onRoomChangePhoto = { room ->
                            roomToChangePhoto = room
                            photoPickerLauncher.launch("image/*")
                        }
                    )
                }
            }
        }
    }

    // Confirmation dialog for deleting room
    if (roomToDelete != null) {
        val (floorNumber, room) = roomToDelete!!
        AlertDialog(
            onDismissRequest = { roomToDelete = null },
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
                    text = "Delete Room?",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = DangerRed
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete Room ${room.name}?\n\nThis will remove the room from this floor.",
                    fontSize = 18.sp,
                    color = TextDark
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.removeLastRoom(houseId, floorNumber)
                        roomToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Delete", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { roomToDelete = null }) {
                    Text("Cancel", fontSize = 18.sp, color = TextDark)
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color.White
        )
    }
}

fun getOrdinalString(number: Int): String {
    if (number % 100 in 11..13) return "${number}th"
    return when (number % 10) {
        1 -> "${number}st"
        2 -> "${number}nd"
        3 -> "${number}rd"
        else -> "${number}th"
    }
}

@Composable
fun FloorCard(
    floor: Floor,
    onAddRoom: () -> Unit,
    onRemoveRoom: () -> Unit,
    onRoomClick: (Room) -> Unit,
    onRoomChangePhoto: (Room) -> Unit
) {
    val roomCount = floor.rooms.size

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 4.dp,
        border = androidx.compose.foundation.BorderStroke(2.dp, BrandTeal.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Floor Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BrandTealLight, RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .background(BrandTeal, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${floor.floorNumber}",
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = "${getOrdinalString(floor.floorNumber)} Floor",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    Text(
                        text = if (roomCount == 1) "1 Room" else "$roomCount Rooms",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BrandTealDark
                    )
                }
            }

            // Action buttons: Remove Room / Add Room
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onRemoveRoom,
                    enabled = floor.rooms.isNotEmpty(),
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .testTag("remove_room_button_${floor.floorNumber}"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = DangerRed
                    ),
                    border = androidx.compose.foundation.BorderStroke(2.dp, if (floor.rooms.isNotEmpty()) DangerRed else Color.LightGray)
                ) {
                    Icon(Icons.Default.RemoveCircleOutline, contentDescription = null, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Remove Room", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onAddRoom,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .testTag("add_room_button_${floor.floorNumber}"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandTeal)
                ) {
                    Icon(Icons.Default.AddCircleOutline, contentDescription = null, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Room", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Room list or empty message
            if (floor.rooms.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.MeetingRoom,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No rooms yet.\nTap \"Add Room\" to create one.",
                        fontSize = 18.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(floor.rooms) { _, room ->
                        RoomCard(
                            room = room,
                            onClick = { onRoomClick(room) },
                            onChangePhoto = { onRoomChangePhoto(room) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun RoomCard(
    room: Room,
    onClick: () -> Unit,
    onChangePhoto: () -> Unit
) {
    val hasImage = !room.imagePath.isNullOrEmpty()

    Box(
        modifier = Modifier
            .width(160.dp)
            .height(200.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(BrandTealLight)
            .border(3.dp, BrandTeal, RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onChangePhoto
            )
            .testTag("room_card_${room.id}")
    ) {
        if (hasImage) {
            AsyncImage(
                model = room.imagePath,
                contentDescription = "Room ${room.name} image",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MeetingRoom,
                    contentDescription = null,
                    tint = BrandTeal.copy(alpha = 0.35f),
                    modifier = Modifier.size(68.dp)
                )
            }
        }

        // Room number badge at top
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 10.dp)
        ) {
            Surface(
                color = Color.White.copy(alpha = if (hasImage) 0.85f else 1f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(2.dp, BrandTeal)
            ) {
                Text(
                    text = room.name,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
        }

        // Bottom strip with "Open Room" and Camera icon
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(44.dp),
            color = BrandTeal,
            shape = RoundedCornerShape(bottomStart = 13.dp, bottomEnd = 13.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Open Room",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = onChangePhoto,
                    modifier = Modifier
                        .size(34.dp)
                        .background(Color.White, CircleShape)
                ) {
                    Icon(
                        imageVector = if (hasImage) Icons.Default.Cameraswitch else Icons.Default.CameraAlt,
                        contentDescription = "Change photo",
                        tint = BrandTeal,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

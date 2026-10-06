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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddHome
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.rent_due.data.model.House
import com.example.rent_due.ui.components.AdBannerView
import com.example.rent_due.ui.theme.BackgroundLight
import com.example.rent_due.ui.theme.BrandTeal
import com.example.rent_due.ui.theme.BrandTealDark
import com.example.rent_due.ui.theme.BrandTealLight
import com.example.rent_due.ui.theme.DangerRed
import com.example.rent_due.ui.theme.DangerRedLight
import com.example.rent_due.ui.theme.TextDark
import com.example.rent_due.ui.viewmodel.RentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: RentViewModel,
    onHouseClick: (String) -> Unit
) {
    val houses by viewModel.houses.collectAsState()
    val isDeleteMode by viewModel.isDeleteMode.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var houseToDelete by remember { mutableStateOf<House?>(null) }
    var houseToChangePhoto by remember { mutableStateOf<House?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null && houseToChangePhoto != null) {
            viewModel.updateHouseImage(houseToChangePhoto!!.id, uri.toString())
            houseToChangePhoto = null
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "My Properties",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = BrandTeal
                )
            )
        },
        bottomBar = {
            Surface(
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    AdBannerView(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                    Button(
                        onClick = { showAddDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(60.dp)
                            .testTag("add_house_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandTeal)
                    ) {
                        Icon(Icons.Default.AddHome, contentDescription = null, modifier = Modifier.size(26.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add House", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { viewModel.toggleDeleteMode() },
                        enabled = houses.isNotEmpty(),
                        modifier = Modifier
                            .weight(1f)
                            .height(60.dp)
                            .testTag("delete_mode_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDeleteMode) DangerRed else Color(0xFF455A64)
                        )
                    ) {
                        Icon(
                            if (isDeleteMode) Icons.Default.Check else Icons.Default.DeleteOutline,
                            contentDescription = null,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isDeleteMode) "Done" else "Delete", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    },
    containerColor = BackgroundLight
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Hint banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isDeleteMode) DangerRedLight else BrandTealLight)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isDeleteMode) "Tap a house to delete it" else "Tap a house to open · Hold to change photo",
                    color = if (isDeleteMode) DangerRed else BrandTealDark,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }

            if (houses.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.HomeWork,
                            contentDescription = null,
                            tint = Color.Gray.copy(alpha = 0.5f),
                            modifier = Modifier.size(100.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No houses yet",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandTealDark
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap Add House below to create your first property.",
                            fontSize = 18.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyRow(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 16.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    itemsIndexed(houses) { _, house ->
                        HouseCard(
                            house = house,
                            isDeleteMode = isDeleteMode,
                            onClick = {
                                if (isDeleteMode) {
                                    houseToDelete = house
                                } else {
                                    onHouseClick(house.id)
                                }
                            },
                            onLongClick = {
                                if (!isDeleteMode) {
                                    houseToChangePhoto = house
                                    photoPickerLauncher.launch("image/*")
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Add House Dialog
    if (showAddDialog) {
        AddHouseDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, stories, imageUri ->
                viewModel.addHouse(name, stories, imageUri)
                showAddDialog = false
            }
        )
    }

    // Delete House Confirmation Dialog
    if (houseToDelete != null) {
        AlertDialog(
            onDismissRequest = { houseToDelete = null },
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
                    text = "Delete House?",
                    color = DangerRed,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to remove \"${houseToDelete!!.name}\"?\n\nAll floors, rooms and rent records for this house will be deleted.",
                    fontSize = 18.sp,
                    color = TextDark
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteHouse(houseToDelete!!.id)
                        houseToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Yes, Delete", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { houseToDelete = null }) {
                    Text("Cancel", fontSize = 18.sp, color = TextDark)
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color.White
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun HouseCard(
    house: House,
    isDeleteMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val borderColor = if (isDeleteMode) DangerRed else Color(0xFF80CBC4)
    val borderWidth = if (isDeleteMode) 3.dp else 2.dp

    Box(
        modifier = Modifier
            .width(300.dp)
            .fillMaxSize()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .border(borderWidth, borderColor, RoundedCornerShape(20.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("house_card_${house.id}")
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header with house name
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BrandTealLight)
                    .padding(vertical = 16.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = house.name,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandTealDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Stories chip
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = Color(0xFFB2DFDB),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = if (house.numberOfStories == 1) "1 Story" else "${house.numberOfStories} Stories",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandTealDark,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }

            // Image area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(14.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFEEEEEE)),
                contentAlignment = Alignment.Center
            ) {
                if (!house.imagePath.isNullOrEmpty()) {
                    AsyncImage(
                        model = house.imagePath,
                        contentDescription = "House photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No house picture",
                            fontSize = 16.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }

        // Delete mode overlay
        if (isDeleteMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DangerRed.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = "Delete",
                    tint = Color.White,
                    modifier = Modifier.size(90.dp)
                )
            }
        } else {
            // Hint badge at bottom right
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.TouchApp,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Hold to change photo",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun AddHouseDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, stories: Int, imageUri: String?) -> Unit
) {
    var houseName by remember { mutableStateOf("") }
    var selectedStories by remember { mutableIntStateOf(1) }
    var selectedImageUri by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri.toString()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add a New House",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "House Name",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = houseName,
                    onValueChange = {
                        houseName = it
                        errorMessage = null
                    },
                    placeholder = { Text("Enter house name", fontSize = 18.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Home, contentDescription = null, tint = BrandTeal)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("house_name_input"),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandTeal,
                        unfocusedBorderColor = Color.Gray
                    ),
                    singleLine = true
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = DangerRed,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Number of Stories",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Gray
                )
                Text(
                    text = "Choose 1 – 100 stories",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Stories interactive selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BrandTealLight, RoundedCornerShape(14.dp))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = { if (selectedStories > 1) selectedStories-- },
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.White, CircleShape)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = BrandTeal)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$selectedStories",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandTealDark
                        )
                        Text(
                            text = if (selectedStories == 1) "1 Story" else "$selectedStories Stories",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BrandTeal
                        )
                    }

                    IconButton(
                        onClick = { if (selectedStories < 100) selectedStories++ },
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.White, CircleShape)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increase", tint = BrandTeal)
                    }
                }

                // Quick jump stories chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(1, 2, 3, 5, 10).forEach { storiesNum ->
                        Surface(
                            onClick = { selectedStories = storiesNum },
                            color = if (selectedStories == storiesNum) BrandTeal else Color.White,
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BrandTeal)
                        ) {
                            Text(
                                text = "${storiesNum}F",
                                color = if (selectedStories == storiesNum) Color.White else BrandTeal,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "House Picture (Optional)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { photoPickerLauncher.launch("image/*") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (selectedImageUri != null) Color(0xFFE8F5E9) else Color.White
                    )
                ) {
                    Icon(
                        imageVector = if (selectedImageUri != null) Icons.Default.CheckCircle else Icons.Default.AddPhotoAlternate,
                        contentDescription = null,
                        tint = if (selectedImageUri != null) Color(0xFF2E7D32) else BrandTeal,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (selectedImageUri != null) "Picture Selected" else "Add Picture",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (selectedImageUri != null) Color(0xFF2E7D32) else BrandTeal
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = houseName.trim()
                    if (trimmed.isEmpty()) {
                        errorMessage = "Enter the house name."
                    } else {
                        onConfirm(trimmed, selectedStories, selectedImageUri)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrandTeal),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("save_house_button")
            ) {
                Text("Save House", fontSize = 18.sp, fontWeight = FontWeight.Bold)
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

package project.unibo.tankyou.ui.screens

import android.Manifest
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.launch
import project.unibo.tankyou.R
import project.unibo.tankyou.data.database.auth.AuthViewModel
import project.unibo.tankyou.data.database.entities.GasStation
import project.unibo.tankyou.data.database.entities.User
import project.unibo.tankyou.data.database.model.SavedGasStationsModel
import project.unibo.tankyou.data.repositories.UserRepository
import project.unibo.tankyou.ui.components.SavedGasStationCard
import project.unibo.tankyou.ui.theme.ThemeManager
import project.unibo.tankyou.utils.Constants
import project.unibo.tankyou.utils.getResourceString
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    onNavigateToLogin: () -> Unit = {},
    onStationClick: (GasStation) -> Unit = {},
    authViewModel: AuthViewModel = viewModel()
) {
    val userRepository = remember { UserRepository.getInstance() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val savedGasStationsModel: SavedGasStationsModel = viewModel()

    val isGuestMode by authViewModel.isGuestMode.collectAsState()

    var currentUser by remember { mutableStateOf<User?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var isUploadingPhoto by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showPhotoDialog by remember { mutableStateOf(false) }
    var isEmailVerified by remember { mutableStateOf(true) }

    var editedName by remember { mutableStateOf("") }
    var editedSurname by remember { mutableStateOf("") }
    var editedUsername by remember { mutableStateOf("") }
    var editedEmail by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    val photoFile = remember {
        createImageFile(context)
    }

    val photoUri = remember {
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            photoFile
        )
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            coroutineScope.launch {
                uploadPhoto(photoUri, userRepository, context, currentUser) { result ->
                    when (result) {
                        is PhotoUploadResult.Success -> {
                            currentUser = result.updatedUser
                            Log.d("ProfileScreen", "Profile photo updated successfully via camera")
                        }

                        is PhotoUploadResult.Error -> {
                            errorMessage = result.message
                        }
                    }
                    isUploadingPhoto = false
                }
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            coroutineScope.launch {
                uploadPhoto(it, userRepository, context, currentUser) { result ->
                    when (result) {
                        is PhotoUploadResult.Success -> {
                            currentUser = result.updatedUser
                            Log.d("ProfileScreen", "Profile photo updated successfully via gallery")
                        }

                        is PhotoUploadResult.Error -> {
                            errorMessage = result.message
                        }
                    }
                    isUploadingPhoto = false
                }
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraLauncher.launch(photoUri)
        } else {
            errorMessage = "Camera permission is required to take photos"
        }
    }

    LaunchedEffect(Unit) {
        if (!isGuestMode) {
            savedGasStationsModel.refreshSavedStations()
        }
    }

    LaunchedEffect(isGuestMode) {
        if (!isGuestMode) {
            isLoading = true
            try {
                isEmailVerified = authViewModel.isCurrentUserEmailVerified()

                if (isEmailVerified) {
                    currentUser = userRepository.getCurrentUser()
                }
            } catch (e: Exception) {
                Log.e("ProfileScreen", "Error loading user", e)
                if (isEmailVerified) {
                    errorMessage = "Error loading profile"
                }
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            editedName = user.name
            editedSurname = user.surname
            editedUsername = user.username
            editedEmail = user.email
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ThemeManager.palette.background)
            // @FIXME .windowInsetsPadding(WindowInsets.statusBars)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = Constants.App.STATUS_BAR_PADDING,
                bottom = 16.dp
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when {
            isGuestMode -> {
                GuestModeContent(onNavigateToLogin = onNavigateToLogin)
            }

            !isGuestMode && !isEmailVerified -> {
                EmailNotVerifiedContent(onLogout = onLogout)
            }

            isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.padding(32.dp),
                    color = ThemeManager.palette.primary
                )
            }

            else -> {
                ProfileInfoCard(
                    currentUser = currentUser,
                    isEditing = isEditing,
                    editedName = editedName,
                    editedSurname = editedSurname,
                    editedUsername = editedUsername,
                    editedEmail = editedEmail,
                    onNameChange = { editedName = it },
                    onSurnameChange = { editedSurname = it },
                    onUsernameChange = { editedUsername = it },
                    onEmailChange = { editedEmail = it },
                    onEditToggle = { isEditing = !isEditing },
                    isUploadingPhoto = isUploadingPhoto,
                    onPhotoClick = { showPhotoDialog = true }
                )

                errorMessage?.let { message ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = message,
                        color = ThemeManager.palette.alert,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (isEditing) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TextButton(
                            onClick = {
                                isEditing = false
                                newPassword = ""
                                confirmPassword = ""
                                errorMessage = null
                                currentUser?.let { user ->
                                    editedName = user.name
                                    editedSurname = user.surname
                                    editedUsername = user.username
                                    editedEmail = user.email
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                getResourceString(R.string.cancel_button),
                                color = ThemeManager.palette.text
                            )
                        }

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    isSaving = true
                                    errorMessage = null
                                    try {
                                        currentUser?.let { user ->
                                            val updatedUser = user.copy(
                                                name = editedName,
                                                surname = editedSurname,
                                                username = editedUsername,
                                                email = editedEmail
                                            )
                                            val success = userRepository.updateUser(updatedUser)
                                            if (success) {
                                                currentUser = updatedUser
                                                isEditing = false
                                                newPassword = ""
                                                confirmPassword = ""
                                            } else {
                                                errorMessage = "Failed to save changes"
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Log.e("ProfileScreen", "Error saving profile", e)
                                        errorMessage = "Error saving changes: ${e.message}"
                                    } finally {
                                        isSaving = false
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ThemeManager.palette.primary,
                                contentColor = ThemeManager.palette.text,
                                disabledContainerColor = ThemeManager.palette.disabledBackground,
                                disabledContentColor = ThemeManager.palette.disabledText
                            ),
                            enabled = !isSaving &&
                                    editedName.isNotBlank() &&
                                    editedSurname.isNotBlank() &&
                                    editedUsername.isNotBlank() &&
                                    editedEmail.isNotBlank() &&
                                    (newPassword.isEmpty() || (newPassword == confirmPassword && newPassword.length >= 6))
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = ThemeManager.palette.white
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Save,
                                    contentDescription = "Save",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))

                                Text(getResourceString(R.string.save_button))
                            }
                        }
                    }
                }

                if (!isEditing) {
                    Spacer(modifier = Modifier.height(24.dp))

                    SavedGasStationCard(
                        onStationClick = onStationClick,
                        savedGasStationsModel = savedGasStationsModel
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = onLogout,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ThemeManager.palette.alert
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = getResourceString(R.string.logout),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(getResourceString(R.string.logout), color = ThemeManager.palette.white)
                    }
                }
            }
        }
    }

    if (showPhotoDialog) {
        PhotoSelectionDialog(
            onDismiss = { showPhotoDialog = false },
            onCameraSelected = {
                showPhotoDialog = false
                isUploadingPhoto = true
                errorMessage = null
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            },
            onGallerySelected = {
                showPhotoDialog = false
                isUploadingPhoto = true
                errorMessage = null
                galleryLauncher.launch("image/*")
            }
        )
    }
}

@Composable
private fun GuestModeContent(
    onNavigateToLogin: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = ThemeManager.palette.accent
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = getResourceString(R.string.guest_mode_title),
            style = MaterialTheme.typography.headlineMedium,
            color = ThemeManager.palette.title,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = getResourceString(R.string.guest_mode_description),
            style = MaterialTheme.typography.bodyLarge,
            color = ThemeManager.palette.text,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onNavigateToLogin,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = ThemeManager.palette.primary
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Login,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = getResourceString(R.string.join_now),
                color = ThemeManager.palette.white,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun EmailNotVerifiedContent(
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Email,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = ThemeManager.palette.warning
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = getResourceString(R.string.email_required),
            style = MaterialTheme.typography.headlineMedium,
            color = ThemeManager.palette.title,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = getResourceString(R.string.email_required_description),
            style = MaterialTheme.typography.bodyLarge,
            color = ThemeManager.palette.text,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = getResourceString(R.string.email_required_instruction),
            style = MaterialTheme.typography.bodyMedium,
            color = ThemeManager.palette.text.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = ThemeManager.palette.alert
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                contentDescription = "Sign out",
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Sign out",
                color = ThemeManager.palette.white,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun PhotoSelectionDialog(
    onDismiss: () -> Unit,
    onCameraSelected: () -> Unit,
    onGallerySelected: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = getResourceString(R.string.get_photo),
                color = ThemeManager.palette.title,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Text(
                text = getResourceString(R.string.get_photo_from),
                color = ThemeManager.palette.text,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onCameraSelected,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ThemeManager.palette.primary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(getResourceString(R.string.photo_camera))
                }

                Button(
                    onClick = onGallerySelected,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ThemeManager.palette.accent
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(getResourceString(R.string.photo_gallery))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    getResourceString(R.string.cancel_button),
                    color = ThemeManager.palette.text
                )
            }
        },
        containerColor = ThemeManager.palette.background,
        shape = RoundedCornerShape(16.dp)
    )
}

private fun createImageFile(context: Context): File {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val imageFileName = "JPEG_${timeStamp}_"
    val storageDir = File(context.cacheDir, "images")
    if (!storageDir.exists()) {
        storageDir.mkdirs()
    }
    return File.createTempFile(imageFileName, ".jpg", storageDir)
}

private sealed class PhotoUploadResult {
    data class Success(val updatedUser: User) : PhotoUploadResult()
    data class Error(val message: String) : PhotoUploadResult()
}

private suspend fun uploadPhoto(
    uri: Uri,
    userRepository: UserRepository,
    context: Context,
    currentUser: User?,
    onResult: (PhotoUploadResult) -> Unit
) {
    try {
        val photoUrl = userRepository.uploadProfilePhoto(uri, context)
        val updatedUser = currentUser?.copy(profilePicture = photoUrl)
        updatedUser?.let { user ->
            val success = userRepository.updateUser(user)
            if (success) {
                onResult(PhotoUploadResult.Success(user))
            } else {
                onResult(PhotoUploadResult.Error("Failed to update profile photo"))
            }
        } ?: onResult(PhotoUploadResult.Error("User not found"))
    } catch (e: Exception) {
        Log.e("ProfileScreen", "Error uploading profile photo", e)
        onResult(PhotoUploadResult.Error("Error uploading photo: ${e.message}"))
    }
}

@Composable
private fun ProfileInfoCard(
    currentUser: User?,
    isEditing: Boolean,
    editedName: String,
    editedSurname: String,
    editedUsername: String,
    editedEmail: String,
    onNameChange: (String) -> Unit,
    onSurnameChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onEditToggle: () -> Unit,
    isUploadingPhoto: Boolean,
    onPhotoClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = ThemeManager.palette.primary
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = getResourceString(R.string.profile_page_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = ThemeManager.palette.title,
                    fontWeight = FontWeight.Medium
                )

                IconButton(onClick = onEditToggle) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = if (isEditing) getResourceString(R.string.stop_editing_info) else getResourceString(
                            R.string.editing_info
                        ),
                        tint = ThemeManager.palette.accent
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier.size(80.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isUploadingPhoto) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(80.dp),
                        color = ThemeManager.palette.accent
                    )
                } else {
                    val hasValidProfilePicture = !currentUser?.profilePicture.isNullOrBlank()

                    if (hasValidProfilePicture) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(currentUser.profilePicture)
                                .crossfade(true)
                                .build(),
                            contentDescription = getResourceString(R.string.profile_picture),
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .border(2.dp, ThemeManager.palette.accent, CircleShape)
                                .clickable { onPhotoClick() },
                            contentScale = ContentScale.Crop,
                            onError = {
                                Log.e(
                                    "ProfileScreen",
                                    "Error loading profile image: ${currentUser.profilePicture}"
                                )
                            }
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(ThemeManager.palette.accent.copy(alpha = 0.1f))
                                .border(2.dp, ThemeManager.palette.accent, CircleShape)
                                .clickable { onPhotoClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = getResourceString(R.string.default_photo),
                                modifier = Modifier.size(40.dp),
                                tint = ThemeManager.palette.accent
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(ThemeManager.palette.secondary)
                        .clickable { onPhotoClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = getResourceString(R.string.change_photo),
                        modifier = Modifier.size(12.dp),
                        tint = ThemeManager.palette.white
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (isEditing) {
                OutlinedTextField(
                    value = editedName,
                    onValueChange = onNameChange,
                    label = { Text(getResourceString(R.string.name)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ThemeManager.palette.secondary,
                        unfocusedBorderColor = ThemeManager.palette.border,
                        focusedTextColor = ThemeManager.palette.text,
                        unfocusedTextColor = ThemeManager.palette.text,
                        focusedLabelColor = ThemeManager.palette.secondary,
                        unfocusedLabelColor = ThemeManager.palette.text,
                        cursorColor = ThemeManager.palette.secondary
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = editedSurname,
                    onValueChange = onSurnameChange,
                    label = { Text(getResourceString(R.string.surname)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ThemeManager.palette.secondary,
                        unfocusedBorderColor = ThemeManager.palette.border,
                        focusedTextColor = ThemeManager.palette.text,
                        unfocusedTextColor = ThemeManager.palette.text,
                        focusedLabelColor = ThemeManager.palette.secondary,
                        unfocusedLabelColor = ThemeManager.palette.text,
                        cursorColor = ThemeManager.palette.secondary
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = editedUsername,
                    onValueChange = onUsernameChange,
                    label = { Text(getResourceString(R.string.username)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ThemeManager.palette.secondary,
                        unfocusedBorderColor = ThemeManager.palette.border,
                        focusedTextColor = ThemeManager.palette.text,
                        unfocusedTextColor = ThemeManager.palette.text,
                        focusedLabelColor = ThemeManager.palette.secondary,
                        unfocusedLabelColor = ThemeManager.palette.text,
                        cursorColor = ThemeManager.palette.secondary
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = editedEmail,
                    onValueChange = onEmailChange,
                    label = { Text(getResourceString(R.string.email)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ThemeManager.palette.secondary,
                        unfocusedBorderColor = ThemeManager.palette.border,
                        focusedTextColor = ThemeManager.palette.text,
                        unfocusedTextColor = ThemeManager.palette.text,
                        focusedLabelColor = ThemeManager.palette.secondary,
                        unfocusedLabelColor = ThemeManager.palette.text,
                        cursorColor = ThemeManager.palette.secondary
                    )
                )
            } else {
                ProfileInfoRow(
                    label = getResourceString(R.string.name),
                    value = currentUser?.let { "${it.name} ${it.surname}" }
                        ?: getResourceString(R.string.not_available)
                )

                ProfileInfoRow(
                    label = getResourceString(R.string.username),
                    value = currentUser?.username ?: getResourceString(R.string.not_available)
                )

                ProfileInfoRow(
                    label = getResourceString(R.string.email),
                    value = currentUser?.email ?: getResourceString(R.string.not_available)
                )
            }
        }
    }
}

@Composable
private fun ProfileInfoRow(
    label: String,
    value: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = ThemeManager.palette.text.copy(alpha = 0.7f),
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = ThemeManager.palette.text,
            fontWeight = FontWeight.Normal
        )
    }
}
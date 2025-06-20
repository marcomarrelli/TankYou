package project.unibo.tankyou.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import project.unibo.tankyou.data.database.auth.AuthViewModel
import project.unibo.tankyou.data.database.entities.User
import project.unibo.tankyou.data.repositories.UserRepository
import project.unibo.tankyou.ui.theme.ThemeManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    authViewModel: AuthViewModel = viewModel()
) {
    val userRepository = remember { UserRepository.getInstance() }
    val coroutineScope = rememberCoroutineScope()

    var currentUser by remember { mutableStateOf<User?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    var editedName by remember { mutableStateOf("") }
    var editedSurname by remember { mutableStateOf("") }
    var editedUsername by remember { mutableStateOf("") }
    var editedEmail by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        isLoading = true
        currentUser = userRepository.getCurrentUser()
        isLoading = false
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
            .padding(16.dp)
            .imePadding()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.padding(32.dp),
                color = ThemeManager.palette.primary
            )
        } else {
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
                onEditToggle = { isEditing = !isEditing }
            )

            if (isEditing) {
                Spacer(modifier = Modifier.height(16.dp))

                PasswordChangeCard(
                    newPassword = newPassword,
                    confirmPassword = confirmPassword,
                    onNewPasswordChange = { newPassword = it },
                    onConfirmPasswordChange = { confirmPassword = it }
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = {
                            isEditing = false
                            newPassword = ""
                            confirmPassword = ""
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
                            "Cancel",
                            color = ThemeManager.palette.text
                        )
                    }

                    Button(
                        onClick = { // TODO: Fix with backend logic please :(
                            coroutineScope.launch {
                                isSaving = true
                                try {
                                    kotlinx.coroutines.delay(1000)

                                    isEditing = false
                                    newPassword = ""
                                    confirmPassword = ""

                                    currentUser = userRepository.getCurrentUser()
                                } catch (e: Exception) {
                                    e.printStackTrace()
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
                            Text("Save Changes")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    authViewModel.signOut()
                    onLogout()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ThemeManager.palette.alert
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = "Logout",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Logout", color = ThemeManager.palette.white)
            }
        }
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
    onEditToggle: () -> Unit
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
                    text = "Profile Information",
                    style = MaterialTheme.typography.headlineSmall,
                    color = ThemeManager.palette.title,
                    fontWeight = FontWeight.Medium
                )

                IconButton(onClick = onEditToggle) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = if (isEditing) "Stop Editing" else "Edit Profile",
                        tint = ThemeManager.palette.accent
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Profile",
                modifier = Modifier.size(80.dp),
                tint = ThemeManager.palette.accent
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (isEditing) {
                OutlinedTextField(
                    value = editedName,
                    onValueChange = onNameChange,
                    label = { Text("First Name") },
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
                    label = { Text("Last Name") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ThemeManager.palette.secondary,
                        unfocusedBorderColor = ThemeManager.palette.border,
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedLabelColor = ThemeManager.palette.secondary,
                        unfocusedLabelColor = ThemeManager.palette.text,
                        cursorColor = ThemeManager.palette.secondary
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = editedUsername,
                    onValueChange = onUsernameChange,
                    label = { Text("Username") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ThemeManager.palette.secondary,
                        unfocusedBorderColor = ThemeManager.palette.border,
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedLabelColor = ThemeManager.palette.secondary,
                        unfocusedLabelColor = ThemeManager.palette.text,
                        cursorColor = ThemeManager.palette.secondary
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = editedEmail,
                    onValueChange = onEmailChange,
                    label = { Text("Email") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ThemeManager.palette.secondary,
                        unfocusedBorderColor = ThemeManager.palette.border,
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedLabelColor = ThemeManager.palette.secondary,
                        unfocusedLabelColor = ThemeManager.palette.text,
                        cursorColor = ThemeManager.palette.secondary
                    )
                )
            } else {
                ProfileInfoRow(
                    label = "Name",
                    value = currentUser?.let { "${it.name} ${it.surname}" } ?: "Not set"
                )

                ProfileInfoRow(
                    label = "Username",
                    value = currentUser?.username ?: "Not set"
                )

                ProfileInfoRow(
                    label = "Email",
                    value = currentUser?.email ?: "Not set"
                )
            }
        }
    }
}

@Composable
private fun PasswordChangeCard(
    newPassword: String,
    confirmPassword: String,
    onNewPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = ThemeManager.palette.primary
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Change Password",
                style = MaterialTheme.typography.headlineSmall,
                color = ThemeManager.palette.title,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = "Leave empty if you don't want to change your password",
                style = MaterialTheme.typography.bodySmall,
                color = ThemeManager.palette.text.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            OutlinedTextField(
                value = newPassword,
                onValueChange = onNewPasswordChange,
                label = { Text("New Password") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ThemeManager.palette.secondary,
                    unfocusedBorderColor = ThemeManager.palette.border,
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    focusedLabelColor = ThemeManager.palette.secondary,
                    unfocusedLabelColor = ThemeManager.palette.text,
                    cursorColor = ThemeManager.palette.secondary
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = onConfirmPasswordChange,
                label = { Text("Confirm Password") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ThemeManager.palette.secondary,
                    unfocusedBorderColor = ThemeManager.palette.border,
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    focusedLabelColor = ThemeManager.palette.secondary,
                    unfocusedLabelColor = ThemeManager.palette.text,
                    cursorColor = ThemeManager.palette.secondary
                ),
                isError = newPassword.isNotEmpty() && confirmPassword.isNotEmpty() && newPassword != confirmPassword
            )

            if (newPassword.isNotEmpty() && confirmPassword.isNotEmpty() && newPassword != confirmPassword) {
                Text(
                    text = "Passwords do not match",
                    color = ThemeManager.palette.alert,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (newPassword.isNotEmpty() && newPassword.length < 6) {
                Text(
                    text = "Password must be at least 6 characters",
                    color = ThemeManager.palette.warning,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
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
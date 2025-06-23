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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import project.unibo.tankyou.R
import project.unibo.tankyou.data.database.auth.AuthState
import project.unibo.tankyou.data.database.auth.AuthViewModel
import project.unibo.tankyou.ui.theme.ThemeManager
import project.unibo.tankyou.utils.getResourceString

/**
 * Login screen composable that provides user authentication interface.
 * Displays email and password input fields with validation and login functionality.
 * Also provides options to navigate to registration or continue as guest.
 *
 * @param onNavigateToRegister Callback function to navigate to registration screen
 * @param onLoginSuccess Callback function to handle successful login
 * @param onContinueAsGuest Callback function to continue without authentication
 * @param authViewModel ViewModel for managing authentication state and operations
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: () -> Unit,
    onContinueAsGuest: () -> Unit,
    authViewModel: AuthViewModel = viewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val authState by authViewModel.authState.collectAsState()

    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            onLoginSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ThemeManager.palette.background)
            .padding(16.dp)
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        /** Application title text with primary color styling and bold font */
        Text(
            text = getResourceString(R.string.app_name),
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = ThemeManager.palette.text,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        /** Email input field with validation styling */
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = {
                Text(
                    getResourceString(R.string.email),
                    color = ThemeManager.palette.text
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ThemeManager.palette.primary,
                unfocusedBorderColor = ThemeManager.palette.border,
                focusedTextColor = ThemeManager.palette.text,
                unfocusedTextColor = ThemeManager.palette.text,
                cursorColor = ThemeManager.palette.primary
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        /** Password input field with visual transformation for security */
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = {
                Text(
                    getResourceString(R.string.password),
                    color = ThemeManager.palette.text
                )
            },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ThemeManager.palette.primary,
                unfocusedBorderColor = ThemeManager.palette.border,
                focusedTextColor = ThemeManager.palette.text,
                unfocusedTextColor = ThemeManager.palette.text,
                cursorColor = ThemeManager.palette.primary
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        /** Primary login button with loading indicator support and larger text */
        Button(
            onClick = {
                authViewModel.signIn(email, password)
            },
            enabled = email.isNotEmpty() && password.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = ThemeManager.palette.primary,
                contentColor = ThemeManager.palette.background,
                disabledContainerColor = ThemeManager.palette.disabledBackground,
                disabledContentColor = ThemeManager.palette.disabledText
            )
        ) {
            if (authState is AuthState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = ThemeManager.palette.background
                )
            } else {
                Text(
                    getResourceString(R.string.log_in),
                    fontSize = 18.sp,
                    color = ThemeManager.palette.text
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        /** Secondary action buttons row for registration and guest access */
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            /** Registration navigation button */
            TextButton(
                onClick = onNavigateToRegister,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = ThemeManager.palette.secondary
                )
            ) {
                Text(
                    getResourceString(R.string.register_now),
                    textAlign = TextAlign.Left
                )
            }

            /** Guest access button for using app without authentication */
            TextButton(
                onClick = {
                    authViewModel.enterAsGuest()
                    onContinueAsGuest()
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = ThemeManager.palette.secondary
                )
            ) {
                Text(
                    getResourceString(R.string.enter_as_guest),
                    textAlign = TextAlign.Right
                )
            }
        }

        /** Error message display for authentication failures */
        if (authState is AuthState.Error) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = (authState as AuthState.Error).message,
                color = ThemeManager.palette.alert,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
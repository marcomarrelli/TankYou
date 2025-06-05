package project.unibo.tankyou.ui.screens

import android.util.Patterns

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.lifecycle.viewmodel.compose.viewModel

import project.unibo.tankyou.data.database.auth.AuthState
import project.unibo.tankyou.data.database.auth.AuthViewModel
import project.unibo.tankyou.ui.theme.ThemeManager

/**
 * Register screen composable that provides user registration interface.
 * Displays email, password and confirm password input fields with validation.
 * Also provides navigation back to login screen.
 *
 * @param onNavigateToLogin Callback function to navigate to login screen
 * @param onRegisterSuccess Callback function to handle successful registration
 * @param authViewModel ViewModel for managing authentication state and operations
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onNavigateToLogin: () -> Unit,
    onRegisterSuccess: () -> Unit,
    authViewModel: AuthViewModel = viewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    val authState by authViewModel.authState.collectAsState()

    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            onRegisterSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ThemeManager.palette.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        /** Application title text with primary color styling and bold font */
        Text(
            text = "Registrati su TankYou",
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
            label = { Text("Email", color = ThemeManager.palette.text) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
            isError = email.isNotEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ThemeManager.palette.primary,
                unfocusedBorderColor = ThemeManager.palette.border,
                focusedTextColor = ThemeManager.palette.text,
                unfocusedTextColor = ThemeManager.palette.text,
                cursorColor = ThemeManager.palette.primary,
                errorBorderColor = ThemeManager.palette.alert,
                errorTextColor = ThemeManager.palette.text
            )
        )

        /** Email validation error message */
        if (email.isNotEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Text(
                text = "Inserisci un'email valida",
                color = ThemeManager.palette.alert,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        /** Password input field with visual transformation for security */
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password", color = ThemeManager.palette.text) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            isError = password.isNotEmpty() && password.length < 6,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ThemeManager.palette.primary,
                unfocusedBorderColor = ThemeManager.palette.border,
                focusedTextColor = ThemeManager.palette.text,
                unfocusedTextColor = ThemeManager.palette.text,
                cursorColor = ThemeManager.palette.primary,
                errorBorderColor = ThemeManager.palette.alert,
                errorTextColor = ThemeManager.palette.text
            )
        )

        /** Password validation error message */
        if (password.isNotEmpty() && password.length < 6) {
            Text(
                text = "La password deve avere almeno 6 caratteri",
                color = ThemeManager.palette.alert,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        /** Confirm password input field with visual transformation for security */
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Conferma Password", color = ThemeManager.palette.text) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            isError = confirmPassword.isNotEmpty() && password != confirmPassword,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ThemeManager.palette.primary,
                unfocusedBorderColor = ThemeManager.palette.border,
                focusedTextColor = ThemeManager.palette.text,
                unfocusedTextColor = ThemeManager.palette.text,
                cursorColor = ThemeManager.palette.primary,
                errorBorderColor = ThemeManager.palette.alert,
                errorTextColor = ThemeManager.palette.text
            )
        )

        /** Confirm password validation error message */
        if (confirmPassword.isNotEmpty() && password != confirmPassword) {
            Text(
                text = "Le password non corrispondono",
                color = ThemeManager.palette.alert,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        /** Primary register button with loading indicator support and larger text */
        Button(
            onClick = {
                authViewModel.signUp(email, password)
            },
            enabled = email.isNotEmpty() &&
                    password.isNotEmpty() &&
                    password.length >= 6 &&
                    password == confirmPassword &&
                    Patterns.EMAIL_ADDRESS.matcher(email).matches(),
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
                    "Registrati",
                    fontSize = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        /** Navigation back to login button */
        TextButton(
            onClick = onNavigateToLogin,
            colors = ButtonDefaults.textButtonColors(
                contentColor = ThemeManager.palette.secondary
            )
        ) {
            Text("Hai già un account? Accedi")
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
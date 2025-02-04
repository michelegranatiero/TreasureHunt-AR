package com.example.treasurehunt_ar.ui.authentication


import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.treasurehunt_ar.R
import com.example.treasurehunt_ar.Route
import com.example.treasurehunt_ar.TreasureHuntApplication
import com.example.treasurehunt_ar.ui.utils.customViewModelFactory

@Composable
fun LoginScreen(
    openAndPopUp: (Route, Route) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = viewModel<LoginViewModel>(
        factory = customViewModelFactory {
            LoginViewModel(TreasureHuntApplication.serviceModule.accountService)
        }
    )
) {
    val email = viewModel.email.collectAsState()
    val password = viewModel.password.collectAsState()

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        launchCredManBottomSheet(context) { result ->
            viewModel.onSignInWithGoogle(result, openAndPopUp)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight()
            // .imePadding()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.auth_image),
            contentDescription = "Auth image",
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp, 4.dp)
        )

        Spacer(modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp))

        OutlinedTextField(
            singleLine = true,
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp, 4.dp)
                .border(
                    BorderStroke(width = 2.dp, color = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(50)
                ),
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            value = email.value,
            onValueChange = { viewModel.updateEmail(it) },
            placeholder = { Text(stringResource(R.string.email)) },
            leadingIcon = { Icon(imageVector = Icons.Default.Email, contentDescription = "Email") }
        )

        OutlinedTextField(
            singleLine = true,
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp, 4.dp)
                .border(
                    BorderStroke(width = 2.dp, color = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(50)
                ),
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            value = password.value,
            onValueChange = { viewModel.updatePassword(it) },
            placeholder = { Text(stringResource(R.string.password)) },
            leadingIcon = { Icon(imageVector = Icons.Default.Lock, contentDescription = "Email") },
            visualTransformation = PasswordVisualTransformation()
        )

        Spacer(modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp))

        Button(
            onClick = { viewModel.onSignInClick(openAndPopUp) },
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp, 0.dp)
        ) {
            Text(
                text = stringResource(R.string.sign_in),
                fontSize = 16.sp,
                modifier = modifier.padding(0.dp, 6.dp)
            )
        }

        Spacer(modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp))

        Text(text = stringResource(R.string.or), fontSize = 16.sp, color = MaterialTheme.colorScheme.primaryContainer)

        Spacer(modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp))

        AuthenticationButton(buttonText = R.string.sign_in_with_google) { credential ->
            viewModel.onSignInWithGoogle(credential, openAndPopUp)
            // SnackbarManager.showMessage(context.getString(R.string.sign_in_success))
            // Toast.makeText(context, "Sign in success", Toast.LENGTH_SHORT).show()

        }

        Spacer(modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp))

        TextButton(onClick = { viewModel.onSignUpClick(openAndPopUp) }) {
            Text(text = stringResource(R.string.sign_up_description), fontSize = 16.sp, color = MaterialTheme.colorScheme.primaryContainer)
        }

        /* Spacer(modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp))

        Button(
            onClick = { viewModel.onSignInAnonymous(openAndPopUp) },
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp, 0.dp)
        ) {
            Text(
                text = stringResource(R.string.sign_in_with_anonymous),
                fontSize = 16.sp,
                modifier = modifier.padding(0.dp, 6.dp)
            )
        } */
    }
}
package com.example.treasurehunt_ar.ui.account_center

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.treasurehunt_ar.R
import com.example.treasurehunt_ar.Route
import com.example.treasurehunt_ar.TreasureHuntApplication
import com.example.treasurehunt_ar.model.User
import com.example.treasurehunt_ar.ui.utils.customViewModelFactory
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountCenterScreen(
    restartApp: (Route) -> Unit,
    openScreen: (Route) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AccountCenterViewModel = viewModel<AccountCenterViewModel>(
        factory = customViewModelFactory {
            AccountCenterViewModel(TreasureHuntApplication.serviceModule.accountService)
        }
    )
) {
    val user by viewModel.user.collectAsState(initial = User())
    val provider = user.provider.replaceFirstChar { it.titlecase(Locale.getDefault()) }

    Scaffold (
        topBar = { TopAppBar(title = { Text(stringResource(R.string.account_center)) })},
        contentWindowInsets = WindowInsets.safeContent,
    ){ innerPadding ->
        Column(
            modifier = modifier
                .padding(innerPadding)
                .fillMaxWidth()
                .fillMaxHeight()
                .imePadding()
                .verticalScroll(rememberScrollState())
            ,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp))

            DisplayNameCard(user.displayName) { viewModel.onUpdateDisplayNameClick(it) }

            Spacer(modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp))

            Card(modifier = Modifier.card()) {
                Column(modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp)) {
                    if (!user.isAnonymous) {
                        Text(
                            text = String.format(stringResource(R.string.profile_email), user.email),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        )
                    }

                    // ⚠️This is for demonstration purposes only, it's not a common
                    // practice to show the unique ID or account provider of an account⚠️
                    Text(
                        text = String.format(stringResource(R.string.profile_uid), user.id),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    )

                    Text(
                        text = String.format(stringResource(R.string.profile_provider), provider),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp))

            if (user.isAnonymous) {
                AccountCenterCard(stringResource(R.string.authenticate), Icons.Filled.AccountCircle, Modifier.card()) {
                    viewModel.onSignInClick(openScreen)
                }
            } else {
                ExitAppCard { viewModel.onSignOutClick(restartApp) }
                RemoveAccountCard { viewModel.onDeleteAccountClick(restartApp) }
            }
        }
    }

}
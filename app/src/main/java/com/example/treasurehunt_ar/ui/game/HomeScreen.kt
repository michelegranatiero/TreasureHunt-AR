package com.example.treasurehunt_ar.ui.game

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.treasurehunt_ar.R
import com.example.treasurehunt_ar.Route
import com.example.treasurehunt_ar.TreasureHuntApplication
import com.example.treasurehunt_ar.ui.utils.customViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    restartApp: (Route) -> Unit,
    openScreen: (Route) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel<HomeViewModel>(
        factory = customViewModelFactory {
            HomeViewModel(TreasureHuntApplication.serviceModule.accountService)
        }
    )
) {

    // LaunchedEffect(Unit) { viewModel.initialize(restartApp) }

    Scaffold (
        modifier = modifier,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
                title = { Text(stringResource(R.string.app_game_name)) },
                actions = {
                    IconButton(onClick = { openScreen(Route.AccountCenter) }) {
                        Icon(
                            Icons.Filled.Person,
                            "Account center",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

            )
        }
    ){ innerPadding ->
        Column(modifier = Modifier
            .padding(innerPadding)
            .fillMaxWidth()
            .fillMaxHeight()
        ){

        }
    }
}
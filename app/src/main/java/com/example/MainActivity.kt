package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.ElectricBolt
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.local.ResidentLedgerWithDetails
import com.example.notifications.ReminderScheduler
import com.example.ui.MainViewModel
import com.example.ui.components.PaymentBottomSheet
import com.example.ui.components.RolloverPeriodDialog
import com.example.ui.components.UtilitiesBillDialog
import com.example.ui.components.WhatsAppQueueDialog
import com.example.ui.screens.ElectricityAnalyticsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ResidentsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.EzbahTheme

private data class NavDestinationItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val destinations = listOf(
    NavDestinationItem("home", "الرئيسية", Icons.Filled.Home, Icons.Outlined.Home),
    NavDestinationItem("residents", "السكان", Icons.Filled.Group, Icons.Outlined.Group),
    NavDestinationItem("electricity", "الفواتير", Icons.Filled.ElectricBolt, Icons.Outlined.ElectricBolt),
    NavDestinationItem("settings", "الإعدادات", Icons.Filled.Settings, Icons.Outlined.Settings)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ReminderScheduler.ensureChannel(this)
        enableEdgeToEdge()
        setContent {
            EzbahTheme {
                EzbahApp()
            }
        }
    }
}

@Composable
fun EzbahApp() {
    val context = LocalContext.current
    val viewModel: MainViewModel = viewModel(factory = MainViewModel.Factory(context))
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val snackbarHostState = remember { SnackbarHostState() }

    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val useNavRail = screenWidthDp >= 600

    var activePaymentTarget by remember { mutableStateOf<ResidentLedgerWithDetails?>(null) }
    var showUtilitiesDialog by remember { mutableStateOf(false) }
    var showRolloverDialog by remember { mutableStateOf(false) }
    var showWhatsAppQueue by remember { mutableStateOf(false) }

    LaunchedEffect(state.snackbarMessage) {
        val msg = state.snackbarMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    val navigateTo: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val navBarItemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.primary,
        selectedTextColor = MaterialTheme.colorScheme.primary,
        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    )

    val navRailItemColors = NavigationRailItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.primary,
        selectedTextColor = MaterialTheme.colorScheme.primary,
        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (!useNavRail) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp
                ) {
                    destinations.forEach { dest ->
                        val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navigateTo(dest.route) },
                            colors = navBarItemColors,
                            icon = {
                                Icon(
                                    imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                    contentDescription = dest.label
                                )
                            },
                            label = { Text(dest.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (useNavRail) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    destinations.forEach { dest ->
                        val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true
                        NavigationRailItem(
                            selected = selected,
                            onClick = { navigateTo(dest.route) },
                            colors = navRailItemColors,
                            icon = {
                                Icon(
                                    imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                    contentDescription = dest.label
                                )
                            },
                            label = { Text(dest.label) }
                        )
                    }
                }
            }

            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                NavHost(
                    navController = navController,
                    startDestination = "home",
                    modifier = Modifier.weight(1f)
                ) {
                    composable("home") {
                        HomeScreen(
                            state = state,
                            onSelectPeriod = viewModel::selectPeriod,
                            onRoomFilter = viewModel::setRoomFilter,
                            onStatusFilter = viewModel::setStatusFilter,
                            onSearchQuery = viewModel::setSearchQuery,
                            onToggleMultiSelect = viewModel::toggleMultiSelectMode,
                            onToggleLedgerSelection = viewModel::toggleLedgerSelection,
                            onSelectAllUnpaid = viewModel::selectAllUnpaid,
                            onBatchPaySelected = viewModel::batchPaySelected,
                            onOpenPaymentSheet = { activePaymentTarget = it },
                            onOpenUtilitiesDialog = { showUtilitiesDialog = true },
                            onOpenRolloverDialog = { showRolloverDialog = true },
                            onOpenWhatsAppQueue = { showWhatsAppQueue = true }
                        )
                    }
                    composable("residents") {
                        ResidentsScreen(
                            state = state,
                            onGenerateResidentsAuto = viewModel::generateResidentsAuto,
                            onAddResident = viewModel::addResident,
                            onUpdateResident = viewModel::updateResident,
                            onDeleteResident = viewModel::deleteResident
                        )
                    }
                    composable("electricity") {
                        ElectricityAnalyticsScreen(
                            state = state,
                            onUpdateCurrentUtilities = viewModel::updateCurrentUtilities,
                            onAddHistoricalElectricity = viewModel::addOrUpdateHistoricalElectricity
                        )
                    }
                    composable("settings") {
                        SettingsScreen(
                            state = state,
                            onSaveConfig = viewModel::updateConfig,
                            onTriggerTestNotification = viewModel::triggerTestNotification
                        )
                    }
                }
            }
        }
    }

    activePaymentTarget?.let { target ->
        PaymentBottomSheet(
            item = target,
            config = state.config,
            monthlyRentPerPerson = state.monthlyRentPerPerson,
            cycleRentPerPerson = state.cycleRentPerPerson,
            onDismiss = { activePaymentTarget = null },
            onConfirmPayment = { amount, mode, customRent, customElec, customWater, note ->
                activePaymentTarget = null
                viewModel.recordPayment(
                    ledgerId = target.ledger.id,
                    residentName = target.resident.name,
                    amountBrought = amount,
                    mode = mode,
                    customRentPart = customRent,
                    customElectricityPart = customElec,
                    customWaterPart = customWater,
                    note = note
                )
            },
            onResetPayment = {
                activePaymentTarget = null
                viewModel.resetResidentPayment(target.ledger.id, target.resident.name)
            }
        )
    }

    val currentPeriod = state.selectedPeriod
    if (showUtilitiesDialog && currentPeriod != null) {
        UtilitiesBillDialog(
            initialYear = currentPeriod.year,
            initialMonth = currentPeriod.month,
            initialElecAmount = currentPeriod.electricityTotalAmount,
            initialWaterAmount = currentPeriod.waterTotalAmount,
            initialPrevReading = currentPeriod.prevMeterReading,
            initialCurrReading = currentPeriod.currMeterReading,
            waterIncludedInRent = state.config.waterIncludedInRent,
            activeResidentsCount = state.activeResidentsCount,
            currency = state.config.currencySymbol,
            allowMonthSelection = false,
            onDismiss = { showUtilitiesDialog = false },
            onSave = { _, _, elecAmt, waterAmt, prev, curr ->
                showUtilitiesDialog = false
                viewModel.updateCurrentUtilities(currentPeriod.id, elecAmt, waterAmt, prev, curr)
            }
        )
    }

    if (showRolloverDialog && currentPeriod != null) {
        RolloverPeriodDialog(
            currentPeriod = currentPeriod,
            unpaidResidentsCount = state.unpaidCount + state.partialCount,
            totalCarryOverAmount = state.totalRemaining,
            totalAdvanceCredits = state.totalAdvanceCredits,
            config = state.config,
            onDismiss = { showRolloverDialog = false },
            onConfirmRollover = { nextElec, nextWater, nextPrev, nextCurr, chargeRent ->
                showRolloverDialog = false
                viewModel.closePeriodAndRollover(
                    currentPeriodId = currentPeriod.id,
                    nextElectricityAmount = nextElec,
                    nextWaterAmount = nextWater,
                    nextPrevReading = nextPrev,
                    nextCurrReading = nextCurr,
                    chargeRentInNewPeriod = chargeRent
                )
            }
        )
    }

    if (showWhatsAppQueue && currentPeriod != null) {
        WhatsAppQueueDialog(
            items = state.allLedgerItems,
            period = currentPeriod,
            config = state.config,
            onDismiss = { showWhatsAppQueue = false }
        )
    }
}

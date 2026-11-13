package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.ApartmentConfigEntity
import com.example.data.local.BillingPeriodEntity
import com.example.ui.MainUiState
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.EzbahTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("عزبة", appName)
    }

    @Test
    fun `home screen renders Ezbah logo drawable without crashing`() {
        val period = BillingPeriodEntity(
            id = 1L,
            year = 2026,
            month = 11,
            label = "نوفمبر 2026",
            rentPoolAmount = 1500.0,
            rentCycleTotalAmount = 4500.0,
            rentCycleMonths = 3
        )
        val state = MainUiState(
            isLoading = false,
            config = ApartmentConfigEntity(),
            periods = listOf(period),
            selectedPeriod = period
        )

        composeRule.setContent {
            EzbahTheme {
                HomeScreen(
                    state = state,
                    onSelectPeriod = {},
                    onRoomFilter = {},
                    onStatusFilter = {},
                    onSearchQuery = {},
                    onToggleMultiSelect = {},
                    onToggleLedgerSelection = {},
                    onSelectAllUnpaid = {},
                    onBatchPaySelected = {},
                    onOpenPaymentSheet = {},
                    onOpenUtilitiesDialog = {},
                    onOpenRolloverDialog = {},
                    onOpenWhatsAppQueue = {}
                )
            }
        }

        composeRule.onNodeWithContentDescription("شعار عزبة").assertIsDisplayed()
    }
}

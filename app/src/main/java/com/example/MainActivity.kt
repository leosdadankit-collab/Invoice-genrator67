package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.ui.screens.BusinessProfileScreen
import com.example.ui.screens.CreateEditInvoiceScreen
import com.example.ui.screens.CustomerListScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.InvoiceDetailScreen
import com.example.ui.screens.InvoiceListScreen
import com.example.ui.screens.ProductListScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.theme.BharatBillTheme
import com.example.ui.theme.Navy900
import com.example.ui.viewmodel.BillingViewModel

enum class MainTab(val label: String, val icon: ImageVector) {
    DASHBOARD("Dashboard", Icons.Default.Dashboard),
    INVOICES("Invoices", Icons.Default.ReceiptLong),
    PRODUCTS("Items", Icons.Default.Inventory2),
    CUSTOMERS("Parties", Icons.Default.People),
    REPORTS("Reports", Icons.Default.Assessment),
    SETTINGS("Business", Icons.Default.Storefront)
}

sealed class AppScreen {
    object Main : AppScreen()
    object CreateInvoice : AppScreen()
    data class EditInvoice(val invoiceId: Long) : AppScreen()
    data class ViewInvoice(val invoiceId: Long) : AppScreen()
}

class MainActivity : ComponentActivity() {

    private val viewModel: BillingViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            BharatBillTheme {
                var currentTab by remember { mutableStateOf(MainTab.DASHBOARD) }
                var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.Main) }

                BackHandler(enabled = currentScreen !is AppScreen.Main) {
                    currentScreen = AppScreen.Main
                }

                when (val screen = currentScreen) {
                    is AppScreen.CreateInvoice -> {
                        CreateEditInvoiceScreen(
                            viewModel = viewModel,
                            onNavigateBack = { currentScreen = AppScreen.Main },
                            onInvoiceSaved = { savedId ->
                                currentScreen = AppScreen.ViewInvoice(savedId)
                            }
                        )
                    }

                    is AppScreen.EditInvoice -> {
                        CreateEditInvoiceScreen(
                            viewModel = viewModel,
                            onNavigateBack = { currentScreen = AppScreen.ViewInvoice(screen.invoiceId) },
                            onInvoiceSaved = { savedId ->
                                currentScreen = AppScreen.ViewInvoice(savedId)
                            }
                        )
                    }

                    is AppScreen.ViewInvoice -> {
                        InvoiceDetailScreen(
                            invoiceId = screen.invoiceId,
                            viewModel = viewModel,
                            onNavigateBack = { currentScreen = AppScreen.Main },
                            onEditInvoice = { invId ->
                                viewModel.loadInvoiceForEditing(invId)
                                currentScreen = AppScreen.EditInvoice(invId)
                            }
                        )
                    }

                    is AppScreen.Main -> {
                        Scaffold(
                            topBar = {
                                TopAppBar(
                                    title = {
                                        Text(
                                            text = when (currentTab) {
                                                MainTab.DASHBOARD -> "Bharat Bill"
                                                MainTab.INVOICES -> "Invoices & Bills"
                                                MainTab.PRODUCTS -> "Products & Services"
                                                MainTab.CUSTOMERS -> "Customer Directory"
                                                MainTab.REPORTS -> "GST Reports & Analytics"
                                                MainTab.SETTINGS -> "Business Profile & Settings"
                                            },
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 20.sp
                                        )
                                    },
                                    colors = TopAppBarDefaults.topAppBarColors(
                                        containerColor = Navy900,
                                        titleContentColor = Color.White
                                    )
                                )
                            },
                            bottomBar = {
                                NavigationBar(
                                    containerColor = Color.White,
                                    modifier = Modifier
                                        .windowInsetsPadding(WindowInsets.navigationBars)
                                        .testTag("main_navigation_bar")
                                ) {
                                    MainTab.values().forEach { tab ->
                                        NavigationBarItem(
                                            selected = currentTab == tab,
                                            onClick = { currentTab = tab },
                                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                                            label = { Text(tab.label, fontSize = 11.sp, fontWeight = if (currentTab == tab) FontWeight.Bold else FontWeight.Normal) },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = Navy900,
                                                selectedTextColor = Navy900,
                                                indicatorColor = Color(0xFFDCE6F2),
                                                unselectedIconColor = Color.Gray,
                                                unselectedTextColor = Color.Gray
                                            )
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        ) { innerPadding ->
                            when (currentTab) {
                                MainTab.DASHBOARD -> {
                                    DashboardScreen(
                                        viewModel = viewModel,
                                        onCreateInvoiceClick = {
                                            viewModel.initNewInvoice()
                                            currentScreen = AppScreen.CreateInvoice
                                        },
                                        onViewInvoiceClick = { id ->
                                            currentScreen = AppScreen.ViewInvoice(id)
                                        },
                                        onViewAllInvoicesClick = {
                                            currentTab = MainTab.INVOICES
                                        },
                                        onAddCustomerClick = {
                                            currentTab = MainTab.CUSTOMERS
                                        },
                                        onAddProductClick = {
                                            currentTab = MainTab.PRODUCTS
                                        },
                                        modifier = Modifier.padding(innerPadding)
                                    )
                                }

                                MainTab.INVOICES -> {
                                    InvoiceListScreen(
                                        viewModel = viewModel,
                                        onCreateInvoiceClick = {
                                            viewModel.initNewInvoice()
                                            currentScreen = AppScreen.CreateInvoice
                                        },
                                        onViewInvoiceClick = { id ->
                                            currentScreen = AppScreen.ViewInvoice(id)
                                        },
                                        modifier = Modifier.padding(innerPadding)
                                    )
                                }

                                MainTab.PRODUCTS -> {
                                    ProductListScreen(
                                        viewModel = viewModel,
                                        modifier = Modifier.padding(innerPadding)
                                    )
                                }

                                MainTab.CUSTOMERS -> {
                                    CustomerListScreen(
                                        viewModel = viewModel,
                                        modifier = Modifier.padding(innerPadding)
                                    )
                                }

                                MainTab.REPORTS -> {
                                    ReportsScreen(
                                        viewModel = viewModel,
                                        modifier = Modifier.padding(innerPadding)
                                    )
                                }

                                MainTab.SETTINGS -> {
                                    BusinessProfileScreen(
                                        viewModel = viewModel,
                                        modifier = Modifier.padding(innerPadding)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

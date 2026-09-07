package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.CalibrationScreen
import com.example.ui.screens.DecisionHistoryScreen
import com.example.ui.screens.DeliverySimulatorScreen
import com.example.ui.screens.LiveMatchScreen
import com.example.viewmodel.CricketVisionViewModel

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import com.example.ui.theme.NaturalBackground
import com.example.ui.theme.NaturalBorderSubtle
import com.example.ui.theme.NaturalSage
import com.example.ui.theme.NaturalSageLight
import com.example.ui.theme.NaturalSandBorder
import com.example.ui.theme.NaturalSurface
import com.example.ui.theme.NaturalTextPrimary
import com.example.ui.theme.NaturalTextSecondary

enum class AppDestination(val title: String, val testTag: String) {
    LIVE_MATCH("Live Match", "nav_live_match"),
    PRACTICE_LAB("Net Practice", "nav_practice_lab"),
    DECISION_LOGS("Decision Logs", "nav_decision_logs"),
    CALIBRATION("Calibration", "nav_calibration")
}

@Composable
fun CricketVisionApp(
    viewModel: CricketVisionViewModel = viewModel()
) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestination.LIVE_MATCH) }

    Scaffold(
        bottomBar = {
            if (currentDestination != AppDestination.CALIBRATION) {
                NavigationBar(
                    containerColor = NaturalSurface,
                    tonalElevation = 4.dp,
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                        .border(1.dp, NaturalSandBorder, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                        .testTag("main_bottom_nav")
                ) {
                    NavigationBarItem(
                        selected = currentDestination == AppDestination.LIVE_MATCH,
                        onClick = { currentDestination = AppDestination.LIVE_MATCH },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = "Live Match"
                            )
                        },
                        label = {
                            Text(
                                text = "Live Match",
                                fontSize = 11.sp,
                                fontWeight = if (currentDestination == AppDestination.LIVE_MATCH) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NaturalSage,
                            selectedTextColor = NaturalSage,
                            indicatorColor = NaturalSageLight,
                            unselectedIconColor = NaturalTextSecondary.copy(alpha = 0.6f),
                            unselectedTextColor = NaturalTextSecondary.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.testTag("nav_item_live_match")
                    )

                    NavigationBarItem(
                        selected = currentDestination == AppDestination.PRACTICE_LAB,
                        onClick = { currentDestination = AppDestination.PRACTICE_LAB },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Science,
                                contentDescription = "Net Practice"
                            )
                        },
                        label = {
                            Text(
                                text = "Net Practice",
                                fontSize = 11.sp,
                                fontWeight = if (currentDestination == AppDestination.PRACTICE_LAB) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NaturalSage,
                            selectedTextColor = NaturalSage,
                            indicatorColor = NaturalSageLight,
                            unselectedIconColor = NaturalTextSecondary.copy(alpha = 0.6f),
                            unselectedTextColor = NaturalTextSecondary.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.testTag("nav_item_practice_lab")
                    )

                    NavigationBarItem(
                        selected = currentDestination == AppDestination.DECISION_LOGS,
                        onClick = { currentDestination = AppDestination.DECISION_LOGS },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "Decision Logs"
                            )
                        },
                        label = {
                            Text(
                                text = "Decision Logs",
                                fontSize = 11.sp,
                                fontWeight = if (currentDestination == AppDestination.DECISION_LOGS) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NaturalSage,
                            selectedTextColor = NaturalSage,
                            indicatorColor = NaturalSageLight,
                            unselectedIconColor = NaturalTextSecondary.copy(alpha = 0.6f),
                            unselectedTextColor = NaturalTextSecondary.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.testTag("nav_item_decision_logs")
                    )
                }
            }
        },
        containerColor = NaturalBackground,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(NaturalBackground)
        ) {
            when (currentDestination) {
                AppDestination.LIVE_MATCH -> {
                    LiveMatchScreen(
                        viewModel = viewModel,
                        onNavigateCalibration = { currentDestination = AppDestination.CALIBRATION },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                AppDestination.PRACTICE_LAB -> {
                    DeliverySimulatorScreen(
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                AppDestination.DECISION_LOGS -> {
                    DecisionHistoryScreen(
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                AppDestination.CALIBRATION -> {
                    CalibrationScreen(
                        viewModel = viewModel,
                        onNavigateBack = { currentDestination = AppDestination.LIVE_MATCH },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

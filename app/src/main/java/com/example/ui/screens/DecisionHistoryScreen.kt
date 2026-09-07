package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.DecisionLogEntity
import com.example.ui.theme.NaturalBackground
import com.example.ui.theme.NaturalBorderSubtle
import com.example.ui.theme.NaturalCardWhite
import com.example.ui.theme.NaturalOchre
import com.example.ui.theme.NaturalOchreLight
import com.example.ui.theme.NaturalRed
import com.example.ui.theme.NaturalRedLight
import com.example.ui.theme.NaturalSage
import com.example.ui.theme.NaturalSageLight
import com.example.ui.theme.NaturalSandBorder
import com.example.ui.theme.NaturalSurface
import com.example.ui.theme.NaturalTextMuted
import com.example.ui.theme.NaturalTextPrimary
import com.example.ui.theme.NaturalTextSecondary
import com.example.viewmodel.CricketVisionViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DecisionHistoryScreen(
    viewModel: CricketVisionViewModel,
    modifier: Modifier = Modifier
) {
    val decisions by viewModel.decisionHistory.collectAsStateWithLifecycle()
    val useUrdu by viewModel.useUrdu.collectAsStateWithLifecycle()

    var selectedFilter by remember { mutableStateOf("ALL") }

    val filteredList = when (selectedFilter) {
        "NO_BALL" -> decisions.filter { it.decisionType == "NO_BALL" || it.decisionType == "OVERSTEP" }
        "WIDE" -> decisions.filter { it.decisionType == "WIDE" }
        "LBW" -> decisions.filter { it.decisionType.startsWith("LBW") }
        "BOUNDARIES" -> decisions.filter { it.decisionType == "FOUR" || it.decisionType == "SIX" }
        else -> decisions
    }

    // Stats
    val noBallCount = decisions.count { it.decisionType == "NO_BALL" || it.decisionType == "OVERSTEP" }
    val wideCount = decisions.count { it.decisionType == "WIDE" }
    val lbwCount = decisions.count { it.decisionType == "LBW_OUT" }
    val boundaryCount = decisions.count { it.decisionType == "FOUR" || it.decisionType == "SIX" }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NaturalBackground)
            .padding(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Decision Logs",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = NaturalTextPrimary
                    )
                )
                Text(
                    text = "${decisions.size} deliveries recorded locally in Room DB",
                    style = MaterialTheme.typography.bodySmall.copy(color = NaturalTextSecondary)
                )
            }

            if (decisions.isNotEmpty()) {
                IconButton(
                    onClick = { viewModel.clearAllLogs() },
                    modifier = Modifier
                        .size(40.dp)
                        .background(NaturalSurface, CircleShape)
                        .border(1.dp, NaturalSandBorder, CircleShape)
                        .testTag("clear_logs_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ClearAll,
                        contentDescription = "Clear All Logs",
                        tint = NaturalRed
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Match Stat Badges
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard("NO BALLS", noBallCount.toString(), NaturalRed, Modifier.weight(1f))
            StatCard("WIDES", wideCount.toString(), NaturalOchre, Modifier.weight(1f))
            StatCard("LBW OUT", lbwCount.toString(), NaturalRed, Modifier.weight(1f))
            StatCard("BOUNDARIES", boundaryCount.toString(), NaturalSage, Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Filter Chips Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("ALL", "NO_BALL", "WIDE", "LBW", "BOUNDARIES").forEach { filterKey ->
                val isSelected = selectedFilter == filterKey
                Surface(
                    color = if (isSelected) NaturalSageLight else NaturalCardWhite,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) NaturalSage else NaturalSandBorder
                    ),
                    modifier = Modifier
                        .clickable { selectedFilter = filterKey }
                        .testTag("filter_${filterKey.lowercase()}")
                ) {
                    Text(
                        text = filterKey.replace("_", " "),
                        color = if (isSelected) NaturalSage else NaturalTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // List of Decisions
        if (filteredList.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "No Logs",
                        tint = NaturalTextMuted,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "No deliveries recorded yet",
                        color = NaturalTextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Bowl a delivery from Live Match or Simulator to test auto umpire",
                        color = NaturalTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredList, key = { it.id }) { item ->
                    DecisionLogCard(
                        item = item,
                        useUrdu = useUrdu,
                        onDelete = { viewModel.deleteDecisionLog(item.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, accentColor: Color, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NaturalCardWhite),
        border = BorderStroke(1.dp, NaturalSandBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                color = accentColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                color = NaturalTextSecondary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun DecisionLogCard(
    item: DecisionLogEntity,
    useUrdu: Boolean,
    onDelete: () -> Unit
) {
    val (badgeBg, badgeTextColor) = when (item.decisionType) {
        "OVERSTEP", "NO_BALL", "WIDE", "LBW_OUT" -> Pair(NaturalRedLight, NaturalRed)
        "FOUR", "SIX", "LBW_NOT_OUT", "LEGAL_BALL" -> Pair(NaturalSageLight, NaturalSage)
        else -> Pair(NaturalOchreLight, NaturalOchre)
    }

    val timeStr = remember(item.timestamp) {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        sdf.format(Date(item.timestamp))
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = NaturalCardWhite),
        border = BorderStroke(1.dp, NaturalSandBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("decision_log_item_${item.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = badgeBg,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = item.decisionType.replace("_", " "),
                            color = badgeTextColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        color = NaturalSurface,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${(item.confidence * 100).toInt()}% CONF",
                            color = NaturalTextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = timeStr,
                        color = NaturalTextMuted,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = NaturalTextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val displayReason = if (useUrdu && item.urduReason.isNotBlank()) item.urduReason else item.reason
            Text(
                text = displayReason,
                color = NaturalTextPrimary,
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 18.sp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Preset: ${item.deliveryPreset}",
                color = NaturalTextSecondary,
                fontSize = 10.sp
            )
        }
    }
}


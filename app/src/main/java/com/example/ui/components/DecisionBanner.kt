package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Decision
import com.example.model.DecisionType

import androidx.compose.foundation.border
import com.example.ui.theme.NaturalBorderSubtle
import com.example.ui.theme.NaturalGreen
import com.example.ui.theme.NaturalGreenLight
import com.example.ui.theme.NaturalOchre
import com.example.ui.theme.NaturalOchreBorder
import com.example.ui.theme.NaturalOchreLight
import com.example.ui.theme.NaturalRed
import com.example.ui.theme.NaturalRedBorder
import com.example.ui.theme.NaturalRedLight
import com.example.ui.theme.NaturalSage
import com.example.ui.theme.NaturalSageBorder
import com.example.ui.theme.NaturalSageLight
import com.example.ui.theme.NaturalTextPrimary
import com.example.ui.theme.NaturalTextSecondary

@Composable
fun DecisionBanner(
    decision: Decision?,
    isUrdu: Boolean = false,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = decision != null,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f)
        ),
        exit = slideOutVertically(targetOffsetY = { -it }),
        modifier = modifier.fillMaxWidth()
    ) {
        if (decision != null) {
            val (containerBg, borderCol, accentCol, icon) = when (decision.type) {
                DecisionType.OVERSTEP, DecisionType.NO_BALL, DecisionType.WIDE, DecisionType.LBW_OUT -> {
                    listOf(NaturalRedLight, NaturalRedBorder, NaturalRed, Icons.Default.SportsCricket)
                }
                DecisionType.LBW_NOT_OUT, DecisionType.FOUR, DecisionType.SIX, DecisionType.LEGAL_BALL -> {
                    listOf(NaturalSageLight, NaturalSageBorder, NaturalSage, Icons.Default.Gavel)
                }
                DecisionType.UMPIRE_REVIEW -> {
                    listOf(NaturalOchreLight, NaturalOchreBorder, NaturalOchre, Icons.Default.ReportProblem)
                }
            }
            val bgColor = containerBg as Color
            val borderColor = borderCol as Color
            val primaryColor = accentCol as Color
            val iconVec = icon as androidx.compose.ui.graphics.vector.ImageVector

            Card(
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                colors = CardDefaults.cardColors(containerColor = bgColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, borderColor, RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                    .testTag("decision_banner")
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    color = primaryColor.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = iconVec,
                                            contentDescription = "Decision Icon",
                                            tint = primaryColor,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = if (isUrdu) decision.type.urduName else decision.type.displayName,
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp,
                                            color = primaryColor
                                        )
                                    )
                                    if (isUrdu && decision.type.displayName.isNotEmpty()) {
                                        Text(
                                            text = decision.type.displayName,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = NaturalTextSecondary,
                                                fontWeight = FontWeight.Medium
                                            )
                                        )
                                    }
                                }
                            }

                            // Confidence pill
                            Surface(
                                color = primaryColor.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.padding(horizontal = 6.dp)
                            ) {
                                Text(
                                    text = "${(decision.confidence * 100).toInt()}% CONF",
                                    color = primaryColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }

                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("dismiss_banner_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss Banner",
                                    tint = NaturalTextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val reasonText = if (isUrdu && decision.urduReason.isNotBlank()) {
                            decision.urduReason
                        } else {
                            decision.reason
                        }

                        Text(
                            text = reasonText,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = NaturalTextPrimary,
                                lineHeight = 19.sp
                            ),
                            textAlign = if (isUrdu) TextAlign.End else TextAlign.Start,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

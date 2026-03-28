package com.vextiq.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Stat Card
@Composable
fun StatCard(
    label: String,
    value: String,
    history: List<Int>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(VextiqColors.Card, RoundedCornerShape(10.dp))
            .border(1.dp, VextiqColors.Border, RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Column {
            Text(
                text = label,
                fontSize = 10.sp,
                color = VextiqColors.TextMuted,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = value,
                fontSize = 26.sp,
                color = VextiqColors.Primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            // Mini chart
            Row(
                modifier = Modifier.fillMaxWidth().height(30.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                history.takeLast(20).forEach { v ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(maxOf(0.05f, v / 100f))
                            .background(
                                VextiqColors.Primary.copy(alpha = 0.6f),
                                RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)
                            )
                    )
                }
            }
        }
    }
}

// Circular Gauge
@Composable
fun CircularGauge(value: Int, modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(80.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(80.dp)) {
            // Background
            drawArc(
                color = VextiqColors.Hover,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
            )
            // Value
            drawArc(
                color = VextiqColors.Primary,
                startAngle = -90f,
                sweepAngle = (value / 100f) * 360f,
                useCenter = false,
                style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        Text(
            text = value.toString(),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = VextiqColors.Primary
        )
    }
}

// Game Item - Fixed colors for visibility
@Composable
fun GameItem(
    icon: String,
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) VextiqColors.Primary.copy(alpha = 0.15f) 
                else VextiqColors.Hover.copy(alpha = 0.5f)
            )
            .border(
                1.dp,
                if (isSelected) VextiqColors.Primary else VextiqColors.Border,
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon with background
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(VextiqColors.Card, RoundedCornerShape(6.dp))
                .border(1.dp, VextiqColors.Border, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, fontSize = 16.sp)
        }
        Spacer(Modifier.width(12.dp))
        // Game name - WHITE text for visibility
        Text(
            name, 
            fontSize = 14.sp, 
            fontWeight = FontWeight.Medium,
            color = if (isSelected) VextiqColors.Primary else VextiqColors.TextPrimary,
            modifier = Modifier.weight(1f)
        )
        // Checkmark
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(VextiqColors.Success, RoundedCornerShape(50)),
                contentAlignment = Alignment.Center
            ) {
                Text("✓", color = VextiqColors.Background, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// Boost Button
@Composable
fun BoostButton(
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .height(50.dp)
            .width(200.dp),
        shape = RoundedCornerShape(25.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = VextiqColors.Primary,
            contentColor = VextiqColors.Background,
            disabledContainerColor = VextiqColors.Primary.copy(alpha = 0.5f)
        )
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

// Action Card - Improved with icon background
@Composable
fun ActionCard(
    icon: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(VextiqColors.Card)
            .border(1.dp, VextiqColors.Border, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Icon with glow background
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                VextiqColors.Primary.copy(alpha = 0.2f),
                                Color.Transparent
                            )
                        ),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(icon, fontSize = 28.sp)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                title, 
                fontSize = 14.sp, 
                fontWeight = FontWeight.Bold,
                color = VextiqColors.TextPrimary
            )
            Text(
                subtitle, 
                fontSize = 11.sp, 
                color = VextiqColors.TextSecondary
            )
        }
    }
}

// Terminal/Log
@Composable
fun Terminal(
    title: String,
    logs: List<Pair<String, String>>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(VextiqColors.Card, RoundedCornerShape(10.dp))
            .border(1.dp, VextiqColors.Border, RoundedCornerShape(10.dp))
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(VextiqColors.Surface)
                .padding(10.dp, 8.dp)
        ) {
            Text(
                text = "◉ $title",
                fontSize = 11.sp,
                color = VextiqColors.Primary,
                fontWeight = FontWeight.Bold
            )
        }
        // Content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(10.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Column {
                logs.forEach { (msg, type) ->
                    val color = when (type) {
                        "ok" -> VextiqColors.Success
                        "err" -> VextiqColors.Error
                        "warn" -> VextiqColors.Warning
                        else -> VextiqColors.Primary
                    }
                    Text(
                        text = msg,
                        fontSize = 12.sp,
                        color = color,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

// Nav Item
@Composable
fun NavItem(
    icon: String,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(
                if (isActive) Brush.horizontalGradient(
                    listOf(VextiqColors.Primary.copy(alpha = 0.15f), Color.Transparent)
                ) else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
            )
            .border(
                1.dp,
                if (isActive) VextiqColors.Primary else Color.Transparent,
                RoundedCornerShape(6.dp)
            )
            .clickable(onClick = onClick)
            .padding(10.dp, 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(icon, fontSize = 16.sp)
            Spacer(Modifier.width(10.dp))
            Text(
                label,
                fontSize = 14.sp,
                color = if (isActive) VextiqColors.Primary else VextiqColors.TextPrimary
            )
        }
    }
}

package com.vextiq.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vextiq.core.*

@Composable
internal fun DashStatCard(label: String, value: String, unit: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(VextiqColors.Card, RoundedCornerShape(12.dp))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column {
            Text(label, fontSize = 10.sp, color = VextiqColors.TextMuted, letterSpacing = 1.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = if (value == "--") VextiqColors.TextMuted else VextiqColors.TextPrimary)
                Spacer(Modifier.width(4.dp))
                Text(unit, fontSize = 13.sp, color = VextiqColors.TextMuted, modifier = Modifier.padding(bottom = 6.dp))
            }
            Spacer(Modifier.height(10.dp))
            // Thin color bar
            Box(Modifier.fillMaxWidth().height(3.dp).background(VextiqColors.Surface, RoundedCornerShape(2.dp))) {
                val progress = when {
                    value == "--" -> 0f
                    unit == "%" -> (value.toFloatOrNull() ?: 0f) / 100f
                    unit == "ms" -> ((value.toFloatOrNull() ?: 0f) / 100f).coerceIn(0f, 1f)
                    unit == "FPS" -> ((value.toFloatOrNull() ?: 0f) / 144f).coerceIn(0f, 1f)
                    else -> 0.5f
                }
                Box(Modifier.fillMaxWidth(progress).fillMaxHeight().background(color, RoundedCornerShape(2.dp)))
            }
        }
    }
}

@Composable
internal fun DashActionCard(title: String, description: String, accentColor: Color, buttonText: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(VextiqColors.Card, RoundedCornerShape(12.dp))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .padding(18.dp)
    ) {
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = accentColor, letterSpacing = 0.5.sp)
        Spacer(Modifier.height(10.dp))
        Text(description, fontSize = 11.sp, color = VextiqColors.TextMuted, lineHeight = 17.sp)
        Spacer(Modifier.height(14.dp))
        Box(
            modifier = Modifier.fillMaxWidth()
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                .clickable { onClick() }
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(buttonText, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = VextiqColors.TextPrimary, letterSpacing = 2.sp)
        }
    }
}

@Composable
internal fun ProcessItem(name: String, impact: String, impactColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(name, fontSize = 11.sp, color = VextiqColors.TextSecondary, modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .background(impactColor.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(impact, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = impactColor, letterSpacing = 1.sp)
        }
    }
}

@Composable
internal fun TagBadge(text: String, bgColor: Color, textColor: Color) {
    Box(
        modifier = Modifier.background(bgColor, RoundedCornerShape(4.dp)).border(1.dp, textColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp)).padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = textColor, letterSpacing = 1.sp)
    }
}

@Composable
internal fun ToolSectionHeader(title: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Box(Modifier.weight(1f).height(1.dp).background(Color.White.copy(alpha = 0.06f)))
        Text(title, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color, letterSpacing = 3.sp, modifier = Modifier.padding(horizontal = 16.dp))
        Box(Modifier.weight(1f).height(1.dp).background(Color.White.copy(alpha = 0.06f)))
    }
}

@Composable
internal fun ToolToggleCard(title: String, desc: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.02f))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = VextiqColors.TextPrimary)
            Text(desc, fontSize = 10.sp, color = VextiqColors.TextMuted, lineHeight = 14.sp)
        }
    }
}

@Composable
internal fun ToolActionCard(title: String, desc: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.02f))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = VextiqColors.TextPrimary)
        Spacer(Modifier.height(4.dp))
        Text(desc, fontSize = 10.sp, color = VextiqColors.TextMuted, lineHeight = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
fun ToolSection(
    title: String,
    titleColor: Color = VextiqColors.Primary,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .background(VextiqColors.Card, RoundedCornerShape(12.dp))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(title, fontSize = 11.sp, color = titleColor, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        content()
    }
}

@Composable
internal fun NetDetail(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 8.sp, color = VextiqColors.TextMuted, letterSpacing = 0.5.sp)
        Text(value, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = VextiqColors.TextPrimary)
    }
}

@Composable
internal fun CfgToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 13.sp, color = VextiqColors.TextPrimary)
        // Toggle switch
        Box(
            modifier = Modifier.width(44.dp).height(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (checked) VextiqColors.Primary else Color(0xFF3A3A4A))
                .padding(2.dp)
        ) {
            Box(
                modifier = Modifier.size(20.dp)
                    .offset(x = if (checked) 20.dp else 0.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White)
            )
        }
    }
}

// Keep old components for backward compatibility
@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .background(VextiqColors.Card, RoundedCornerShape(12.dp))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = VextiqColors.Primary)
        content()
    }
}


package com.vextiq.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Animated Circular Score Gauge
 */
@Composable
fun AnimatedGauge(
    value: Int,
    modifier: Modifier = Modifier.size(120.dp)
) {
    val animatedValue by animateFloatAsState(
        targetValue = value.toFloat(),
        animationSpec = tween(800, easing = FastOutSlowInEasing)
    )
    
    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 10.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2
            
            // Rotating outer dashed ring
            drawCircle(
                color = VextiqColors.Primary.copy(alpha = 0.2f),
                radius = radius + 8.dp.toPx(),
                style = Stroke(
                    width = 1.dp.toPx(),
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6f, 6f), rotation)
                )
            )
            
            // Background arc
            drawArc(
                color = VextiqColors.Surface,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            
            // Value arc with gradient
            drawArc(
                brush = Brush.sweepGradient(listOf(VextiqColors.Primary, VextiqColors.Accent, VextiqColors.Primary)),
                startAngle = -90f,
                sweepAngle = (animatedValue / 100f) * 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            
            // Glow dot at end
            val angle = Math.toRadians((-90 + (animatedValue / 100f) * 360).toDouble())
            val dotX = center.x + radius * kotlin.math.cos(angle).toFloat()
            val dotY = center.y + radius * kotlin.math.sin(angle).toFloat()
            drawCircle(
                color = VextiqColors.Accent.copy(alpha = glowAlpha),
                radius = 14.dp.toPx(),
                center = Offset(dotX, dotY)
            )
        }
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = animatedValue.toInt().toString(),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = VextiqColors.TextPrimary
            )
            Text(
                text = "SCORE",
                fontSize = 10.sp,
                color = VextiqColors.Accent,
                letterSpacing = 2.sp
            )
        }
    }
}

/**
 * Stat Card with animated progress
 */
@Composable
fun StatCard(
    label: String,
    value: String,
    progress: Float = 0f,
    color: Color = VextiqColors.Primary,
    modifier: Modifier = Modifier
) {
    var isHovered by remember { mutableStateOf(false) }
    
    Box(
        modifier = modifier
            .background(VextiqColors.Card, RoundedCornerShape(14.dp))
            .border(
                1.dp,
                if (isHovered) VextiqColors.BorderHover else VextiqColors.Border,
                RoundedCornerShape(14.dp)
            )
            .padding(18.dp)
    ) {
        Column {
            Text(
                text = label,
                fontSize = 11.sp,
                color = VextiqColors.TextMuted,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                val digits = value.filter { it.isDigit() || it == '.' }
                val displayValue = digits.ifEmpty { "--" }
                Text(
                    text = displayValue,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (digits.isEmpty()) VextiqColors.TextMuted else color
                )
                // Only show unit if we have actual digits (not placeholder --)
                val unit = value.filter { !it.isDigit() && it != '.' && it != '-' }.trim()
                if (unit.isNotEmpty() && digits.isNotEmpty()) {
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = unit,
                        fontSize = 14.sp,
                        color = VextiqColors.TextMuted,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(VextiqColors.Surface, RoundedCornerShape(3.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(listOf(VextiqColors.Primary, VextiqColors.Accent)),
                            RoundedCornerShape(3.dp)
                        )
                )
            }
        }
    }
}

/**
 * Navigation Item with animated indicator
 */
@Composable
fun NavItem(
    icon: String,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val bgAlpha by animateFloatAsState(if (isActive) 0.15f else 0f)
    val indicatorWidth by animateFloatAsState(if (isActive) 3f else 0f)
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(VextiqColors.Primary.copy(alpha = bgAlpha))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        // Left indicator
        if (indicatorWidth > 0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = (-14).dp)
                    .width(indicatorWidth.dp)
                    .height(20.dp)
                    .background(
                        Brush.verticalGradient(listOf(VextiqColors.Primary, VextiqColors.Accent)),
                        RoundedCornerShape(topEnd = 2.dp, bottomEnd = 2.dp)
                    )
            )
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(icon, fontSize = 18.sp)
            Spacer(Modifier.width(12.dp))
            Text(
                text = label,
                fontSize = 14.sp,
                color = if (isActive) VextiqColors.Primary else VextiqColors.TextSecondary,
                fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal
            )
        }
    }
}

/**
 * Navigation Item with PNG icon image
 */
@Composable
fun ImageNavItem(
    iconPath: String,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val bgAlpha by animateFloatAsState(if (isActive) 0.15f else 0f)
    val indicatorWidth by animateFloatAsState(if (isActive) 3f else 0f)
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(VextiqColors.Primary.copy(alpha = bgAlpha))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        if (indicatorWidth > 0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = (-14).dp)
                    .width(indicatorWidth.dp)
                    .height(20.dp)
                    .background(
                        Brush.verticalGradient(listOf(VextiqColors.Primary, VextiqColors.Accent)),
                        RoundedCornerShape(topEnd = 2.dp, bottomEnd = 2.dp)
                    )
            )
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = androidx.compose.ui.res.painterResource(iconPath),
                contentDescription = label,
                modifier = Modifier.size(22.dp),
                alpha = if (isActive) 1f else 0.5f
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = label,
                fontSize = 14.sp,
                color = if (isActive) VextiqColors.Primary else VextiqColors.TextSecondary,
                fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal
            )
        }
    }
}

/**
 * Quick Action Button
 */
@Composable
fun QuickActionButton(
    icon: String,
    label: String,
    isHighlighted: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isPressed) 0.95f else 1f)
    
    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isHighlighted) VextiqColors.Primary.copy(alpha = 0.15f) 
                else VextiqColors.Card
            )
            .border(
                1.dp,
                if (isHighlighted) VextiqColors.Primary.copy(alpha = 0.4f) else VextiqColors.Border,
                RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(icon, fontSize = 28.sp)
            Spacer(Modifier.height(10.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                color = if (isHighlighted) VextiqColors.Primary else VextiqColors.TextPrimary
            )
        }
    }
}

/**
 * Playstyle Tag
 */
@Composable
fun PlaystyleTag(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (isSelected) VextiqColors.Primary else VextiqColors.Surface
            )
            .border(
                1.dp,
                if (isSelected) VextiqColors.Primary else VextiqColors.Border,
                RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = if (isSelected) VextiqColors.Background else VextiqColors.TextSecondary
        )
    }
}

/**
 * Primary Boost Button with glow
 */
@Composable
fun BoostButton(
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .height(52.dp)
            .drawBehind {
                if (enabled) {
                    drawRoundRect(
                        color = VextiqColors.Primary.copy(alpha = glowAlpha),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(14.dp.toPx()),
                        size = size.copy(
                            width = size.width + 16.dp.toPx(),
                            height = size.height + 8.dp.toPx()
                        ),
                        topLeft = Offset(-8.dp.toPx(), -4.dp.toPx())
                    )
                }
            },
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = VextiqColors.Surface
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(VextiqColors.GradientPrimary, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = VextiqColors.Background,
                letterSpacing = 1.sp
            )
        }
    }
}

/**
 * Terminal/Log display
 */
@Composable
fun Terminal(
    title: String,
    logs: List<Pair<String, String>>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(VextiqColors.Card, RoundedCornerShape(12.dp))
            .border(1.dp, VextiqColors.Border, RoundedCornerShape(12.dp))
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(VextiqColors.Surface, RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                .padding(12.dp, 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(VextiqColors.Accent, CircleShape)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = title,
                    fontSize = 12.sp,
                    color = VextiqColors.Primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        // Content
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(14.dp)
                .verticalScroll(rememberScrollState())
        ) {
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
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}

/**
 * Active Game Card
 */
@Composable
fun ActiveGameCard(gameName: String, playTime: String = "") {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    listOf(
                        VextiqColors.Success.copy(alpha = 0.1f),
                        VextiqColors.Primary.copy(alpha = 0.05f)
                    )
                ),
                RoundedCornerShape(12.dp)
            )
            .border(1.dp, VextiqColors.Success.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Column {
            Text(
                text = "NOW PLAYING",
                fontSize = 10.sp,
                color = VextiqColors.Success,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = gameName,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = VextiqColors.TextPrimary
            )
            if (playTime.isNotEmpty()) {
                Text(
                    text = playTime,
                    fontSize = 11.sp,
                    color = VextiqColors.TextMuted
                )
            }
        }
    }
}

/**
 * Online Status Indicator
 */
@Composable
fun OnlineIndicator() {
    val infiniteTransition = rememberInfiniteTransition()
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    Row(
        modifier = Modifier
            .background(VextiqColors.Success.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
            .border(1.dp, VextiqColors.Success.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(VextiqColors.Success.copy(alpha = pulseAlpha), CircleShape)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "System Online",
            fontSize = 11.sp,
            color = VextiqColors.Success
        )
    }
}

/**
 * Tool Button for Tools page
 */
@Composable
fun ToolButton(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(VextiqColors.Surface)
            .border(1.dp, VextiqColors.Border, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Column {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.White)
            Text(subtitle, fontSize = 11.sp, color = VextiqColors.TextSecondary)
        }
    }
}

/**
 * Game Item for game list
 */
@Composable
fun GameItem(
    profile: String,
    color: Long = 0xFF00D2FF,
    name: String,
    isSelected: Boolean,
    hasPath: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isSelected) VextiqColors.Primary.copy(alpha = 0.15f) else Color.Transparent
            )
            .border(
                if (isSelected) 1.dp else 0.dp,
                if (isSelected) VextiqColors.Primary else Color.Transparent,
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GameIcon(profile = profile, color = color, size = 30.dp, fontSize = 10.sp)
        Spacer(Modifier.width(12.dp))
        Text(
            text = name,
            fontSize = 14.sp,
            color = if (isSelected) VextiqColors.Primary else VextiqColors.TextPrimary,
            modifier = Modifier.weight(1f)
        )
        if (hasPath) {
            Text("✓", fontSize = 14.sp, color = VextiqColors.Success)
        }
    }
}

/**
 * Game Icon Badge - Professional colored badge with abbreviation
 * Replaces emoji icons with clean styled badges
 */
@Composable
fun GameIcon(
    profile: String,
    color: Long,
    size: Dp = 32.dp,
    fontSize: androidx.compose.ui.unit.TextUnit = 11.sp
) {
    val bgColor = Color(color)
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor.copy(alpha = 0.2f))
            .border(1.dp, bgColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = profile.take(3),
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            color = bgColor,
            fontFamily = FontFamily.Monospace
        )
    }
}

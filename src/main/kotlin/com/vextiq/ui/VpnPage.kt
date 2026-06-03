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

/**
 * VPN Page
 */
@Composable
fun VpnPage() {
    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("🌐 Gaming VPN", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        
        // VPN Status
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(VextiqColors.Card, RoundedCornerShape(14.dp))
                .border(1.dp, VextiqColors.Border, RoundedCornerShape(14.dp))
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Status", fontSize = 12.sp, color = VextiqColors.TextMuted)
                    Text("Disconnected", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = VextiqColors.TextSecondary)
                }
                Box(
                    modifier = Modifier
                        .background(VextiqColors.Primary, RoundedCornerShape(10.dp))
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Text("CONNECT", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = VextiqColors.Background)
                }
            }
        }
        
        // Server Selection
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(VextiqColors.Card, RoundedCornerShape(14.dp))
                .border(1.dp, VextiqColors.Border, RoundedCornerShape(14.dp))
                .padding(20.dp)
        ) {
            Text("🌍 Server Region", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = VextiqColors.Primary)
            Spacer(Modifier.height(16.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf(
                    "🇺🇸 US West" to "45ms",
                    "🇪🇺 Europe" to "120ms",
                    "🇯🇵 Asia" to "180ms"
                ).forEach { (region, ping) ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(VextiqColors.Surface)
                            .border(1.dp, VextiqColors.Border, RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(region, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text(ping, fontSize = 12.sp, color = VextiqColors.Success)
                        }
                    }
                }
            }
        }
        
        // Info
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(VextiqColors.Primary.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
                .border(1.dp, VextiqColors.Primary.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                .padding(20.dp)
        ) {
            Text("ℹ️ Gaming VPN Features", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = VextiqColors.Primary)
            Spacer(Modifier.height(10.dp))
            Text("• Low latency optimized servers", fontSize = 12.sp, color = VextiqColors.TextSecondary)
            Text("• DDoS protection", fontSize = 12.sp, color = VextiqColors.TextSecondary)
            Text("• No bandwidth limits", fontSize = 12.sp, color = VextiqColors.TextSecondary)
            Text("• WireGuard protocol", fontSize = 12.sp, color = VextiqColors.TextSecondary)
        }
    }
}

package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Screenshot
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.VoiceAccentPurple
import com.example.ui.theme.VoiceCyan
import com.example.ui.theme.VoiceGreen

data class GridItem(
    val number: Int,
    val titleBn: String,
    val commandText: String,
    val icon: ImageVector,
    val color: Color
)

@Composable
fun VoiceAccessGridOverlay(
    onItemClick: (GridItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val gridItems = listOf(
        GridItem(1, "ইউটিউব", "YouTube kholo", Icons.Default.SmartDisplay, Color(0xFFFF3B30)),
        GridItem(2, "গান চালাও", "Ai gan ta play koro", Icons.Default.MusicNote, Color(0xFFFF2D55)),
        GridItem(3, "স্ক্রিনশট", "Screen shot naw", Icons.Default.Screenshot, VoiceCyan),
        GridItem(4, "হোয়াটসঅ্যাপ", "WhatsApp e pathaw", Icons.Default.PhotoLibrary, VoiceGreen),
        GridItem(5, "ফোন কল", "Phone e call korbe", Icons.Default.Call, Color(0xFF007AFF)),
        GridItem(6, "ক্যামেরা", "Camera kholo", Icons.Default.CameraAlt, VoiceAccentPurple),
        GridItem(7, "ফ্ল্যাশলাইট", "Flashlight on koro", Icons.Default.FlashlightOn, Color(0xFFFFCC00)),
        GridItem(8, "সেটিংস", "Settings kholo", Icons.Default.Settings, Color(0xFF8E8E93))
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF0F172A))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(20.dp))
            .padding(16.dp)
            .testTag("voice_access_grid")
    ) {
        // Floating Voice Access Indicator Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(30.dp))
                .background(Brush.horizontalGradient(listOf(Color(0xFF1E293B), Color(0xFF0D1B2A))))
                .border(1.dp, PrimaryBlue.copy(alpha = 0.4f), RoundedCornerShape(30.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(VoiceCyan)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Voice Access নম্বর গ্রিড মোড",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "বলুন: \"১\" বা \"Tap 1\"",
                color = VoiceCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "স্ক্রিনে থাকা যেকোনো নম্বর মুখে বলুন অথবা ট্যাপ করুন:",
            color = Color(0xFF94A3B8),
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        // 4x2 grid of actionable Voice Access targets
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.height(300.dp)
        ) {
            items(gridItems) { item ->
                GridTargetCard(item = item, onClick = { onItemClick(item) })
            }
        }
    }
}

@Composable
fun GridTargetCard(
    item: GridItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, item.color.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("grid_target_${item.number}"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141D2E))
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
            // Voice Access Number Badge (Classic Google Voice Access badge design)
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0284C7))
                    .border(1.5.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${item.number}",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(item.color.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.titleBn,
                        tint = item.color,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = item.titleBn,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = item.commandText,
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }
        }
    }
}

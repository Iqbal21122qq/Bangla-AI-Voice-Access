package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Screenshot
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ActionType
import com.example.model.DeviceAction
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.VoiceAccentPurple
import com.example.ui.theme.VoiceCyan
import com.example.ui.theme.VoiceGreen

@Composable
fun ActionExecutionCard(
    action: DeviceAction,
    onExecuteClick: (DeviceAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val (icon, iconColor, bgColor) = when (action.type) {
        ActionType.OPEN_YOUTUBE -> Triple(Icons.Default.SmartDisplay, Color(0xFFFF0000), Color(0xFF2B1214))
        ActionType.PLAY_SONG -> Triple(Icons.Default.MusicNote, Color(0xFFFF5252), Color(0xFF2D1619))
        ActionType.TAKE_SCREENSHOT -> Triple(Icons.Default.Screenshot, VoiceCyan, Color(0xFF102735))
        ActionType.SHARE_WHATSAPP -> Triple(Icons.Default.Share, VoiceGreen, Color(0xFF112B1F))
        ActionType.PHONE_CALL -> Triple(Icons.Default.Call, Color(0xFF3B82F6), Color(0xFF132238))
        ActionType.OPEN_APP -> Triple(Icons.Default.OpenInNew, VoiceAccentPurple, Color(0xFF231633))
        ActionType.VOICE_ACCESS_TAP -> Triple(Icons.Default.TouchApp, Color(0xFFF59E0B), Color(0xFF2A2012))
        ActionType.DEVICE_CONTROL -> Triple(Icons.Default.FlashlightOn, Color(0xFFEAB308), Color(0xFF282313))
        ActionType.UNKNOWN -> Triple(Icons.Default.PlayArrow, Color(0xFF94A3B8), Color(0xFF1E293B))
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, iconColor.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .testTag("action_card_${action.id}"),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(iconColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = action.titleBn,
                            tint = iconColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = action.titleBn,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = action.descriptionBn,
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            maxLines = 2
                        )
                    }
                }

                Button(
                    onClick = { onExecuteClick(action) },
                    colors = ButtonDefaults.buttonColors(containerColor = iconColor),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("execute_action_${action.type.name}")
                ) {
                    Text(
                        text = "চালাও",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            if (action.parameters.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    action.parameters.forEach { (key, value) ->
                        if (value.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF0F172A).copy(alpha = 0.7f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "$key: $value",
                                    color = Color(0xFFCBD5E1),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

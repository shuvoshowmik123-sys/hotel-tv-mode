package com.hotelvision.launcher.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.hotelvision.launcher.data.db.entities.RoomInfoEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

internal const val STATUS_BAR_REFRESH_MS = 60_000L

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun StatusBar(
    roomInfo: RoomInfoEntity?,
    modifier: Modifier = Modifier
) {
    var currentTime by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val formatter = SimpleDateFormat("hh:mm a", Locale.US)
        while (true) {
            currentTime = formatter.format(Date())
            delay(STATUS_BAR_REFRESH_MS)
        }
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TemperatureWidget(temperature = "32 C", condition = "Sunny")
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = currentTime,
                fontSize = 18.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            if (roomInfo?.checkoutTime != null) {
                Text(
                    text = "Checkout ${roomInfo.checkoutTime}",
                    fontSize = 12.sp,
                    color = Color(0xFFD7E2F2),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

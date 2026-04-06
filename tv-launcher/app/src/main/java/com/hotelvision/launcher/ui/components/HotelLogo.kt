package com.hotelvision.launcher.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.hotelvision.launcher.ui.HotelBranding

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HotelLogo(
    branding: HotelBranding,
    modifier: Modifier = Modifier,
    showText: Boolean = true
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Text(
            text = branding.shortBrand,
            color = Color(0xFF8CB6FF),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )

        if (showText) {
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = branding.hotelName,
                    fontSize = 18.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${branding.tagline} | ${branding.location}",
                    fontSize = 12.sp,
                    color = Color(0xFF9CB0C5)
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TemperatureWidget(
    temperature: String = "32 C",
    condition: String = "Sunny",
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Text(
            text = temperature,
            fontSize = 14.sp,
            color = Color.White,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = condition,
            fontSize = 12.sp,
            color = Color(0xFF9CB0C5)
        )
    }
}

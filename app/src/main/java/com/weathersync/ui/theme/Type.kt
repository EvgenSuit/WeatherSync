package com.weathersync.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.weathersync.R

val quicksandMedium = FontFamily(Font(R.font.quicksand_medium))
val arapeyRegular = FontFamily(Font(R.font.arapey_regular))
val arapeyItalic = FontFamily(Font(R.font.arapey_italic))
val ubuntuRegular = FontFamily(Font(R.font.ubuntu_regular))
val AppTypography = Typography(
    titleLarge = TextStyle(
        fontFamily = quicksandMedium,
        fontSize = 40.sp
    ),
    titleMedium = TextStyle(
        fontFamily = quicksandMedium,
        fontSize = 30.sp
    ),
    displayMedium = TextStyle(
        fontFamily = arapeyRegular,
        fontSize = 26.sp
    ),
    labelMedium = TextStyle(
        fontFamily = arapeyItalic,
        fontSize = 16.sp
    )
)

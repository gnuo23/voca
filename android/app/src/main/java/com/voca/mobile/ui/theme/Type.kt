package com.voca.mobile.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.voca.mobile.R

// Duolingo leans on a rounded, heavy display face. Nunito (a variable font)
// gives us that friendly, rounded look. Each weight is derived from the single
// variable ttf via FontVariation; on API < 26 the platform falls back to the
// nearest static instance, still rendering Nunito.
@OptIn(ExperimentalTextApi::class)
private fun nunito(weight: FontWeight) = Font(
    R.font.nunito_variable,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

private val Rounded = FontFamily(
    nunito(FontWeight.Normal),
    nunito(FontWeight.Medium),
    nunito(FontWeight.SemiBold),
    nunito(FontWeight.Bold),
    nunito(FontWeight.ExtraBold),
)

val VocaTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = Rounded,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 40.sp,
        lineHeight = 46.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = Rounded,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 30.sp,
        lineHeight = 36.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = Rounded,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = Rounded,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = Rounded,
        fontWeight = FontWeight.Bold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = Rounded,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = Rounded,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = Rounded,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 16.sp,
        lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = Rounded,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
)

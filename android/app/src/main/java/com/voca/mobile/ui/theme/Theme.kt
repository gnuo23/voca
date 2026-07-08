package com.voca.mobile.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/** Extra brand colors that Material3's ColorScheme doesn't cover. */
data class VocaBrandColors(
    val primaryShadow: Color,
    val blue: Color,
    val blueShadow: Color,
    val yellow: Color,
    val yellowShadow: Color,
    val danger: Color,
    val dangerShadow: Color,
    val success: Color,
    val successShadow: Color,
    val close: Color,
    val streak: Color,
    val muted: Color,
    val border: Color,
)

private val LightBrand = VocaBrandColors(
    primaryShadow = DuoGreenShadow,
    blue = DuoBlue,
    blueShadow = DuoBlueShadow,
    yellow = DuoYellow,
    yellowShadow = DuoYellowShadow,
    danger = DuoRed,
    dangerShadow = DuoRedShadow,
    success = DuoGreen,
    successShadow = DuoGreenShadow,
    close = DuoYellow,
    streak = Color(0xFFFF9600),
    muted = LightMuted,
    border = LightBorder,
)

private val DarkBrand = VocaBrandColors(
    primaryShadow = DuoGreenShadow,
    blue = DuoBlue,
    blueShadow = DuoBlueShadow,
    yellow = DuoYellow,
    yellowShadow = DuoYellowShadow,
    danger = DuoRed,
    dangerShadow = DuoRedShadow,
    success = DuoGreen,
    successShadow = DuoGreenShadow,
    close = DuoYellow,
    streak = Color(0xFFFF9600),
    muted = DarkMuted,
    border = DarkBorder,
)

val LocalVocaBrand = staticCompositionLocalOf { LightBrand }

private val LightColors = lightColorScheme(
    primary = DuoGreen,
    onPrimary = Color.White,
    secondary = DuoBlue,
    onSecondary = Color.White,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightMuted,
    error = DuoRed,
    onError = Color.White,
    outline = LightBorder,
)

private val DarkColors = darkColorScheme(
    primary = DuoGreen,
    onPrimary = Color.White,
    secondary = DuoBlue,
    onSecondary = Color.White,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkMuted,
    error = DuoRed,
    onError = Color.White,
    outline = DarkBorder,
)

/** Convenient accessor: `VocaTheme.brand.streak`. */
object VocaTheme {
    val brand: VocaBrandColors
        @Composable get() = LocalVocaBrand.current
}

@Composable
fun VocaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val brand = if (darkTheme) DarkBrand else LightBrand

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalVocaBrand provides brand) {
        MaterialTheme(
            colorScheme = colors,
            typography = VocaTypography,
            shapes = VocaShapes,
            content = content,
        )
    }
}

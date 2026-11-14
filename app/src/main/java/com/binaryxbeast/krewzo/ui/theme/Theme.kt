package com.binaryxbeast.krewzo.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ─────────────────────────────────────────────
// Krewzo Theme
// ─────────────────────────────────────────────

private val LightColorScheme = lightColorScheme(
    primary = Light_Primary,
    onPrimary = Light_OnPrimary,
    primaryContainer = Light_PrimaryContainer,
    onPrimaryContainer = Light_Primary,
    secondary = Light_TextSecondary,
    onSecondary = Light_OnPrimary,
    secondaryContainer = Light_SurfaceSecondary,
    onSecondaryContainer = Light_TextPrimary,
    tertiary = Light_TextTertiary,
    tertiaryContainer = Light_SurfaceTertiary,
    background = Light_Background,
    onBackground = Light_TextPrimary,
    surface = Light_Surface,
    onSurface = Light_TextPrimary,
    surfaceVariant = Light_SurfaceSecondary,
    onSurfaceVariant = Light_TextSecondary,
    outline = Light_Border,
    outlineVariant = Light_Divider,
    error = Light_Error,
    onError = Light_OnPrimary,
    errorContainer = Light_ErrorContainer,
    onErrorContainer = Light_Error,
)

private val DarkColorScheme = darkColorScheme(
    primary = Dark_Primary,
    onPrimary = Dark_OnPrimary,
    primaryContainer = Dark_PrimaryContainer,
    onPrimaryContainer = Dark_Primary,
    secondary = Dark_TextSecondary,
    onSecondary = Dark_OnPrimary,
    secondaryContainer = Dark_SurfaceSecondary,
    onSecondaryContainer = Dark_TextPrimary,
    tertiary = Dark_TextTertiary,
    tertiaryContainer = Dark_SurfaceTertiary,
    background = Dark_Background,
    onBackground = Dark_TextPrimary,
    surface = Dark_Surface,
    onSurface = Dark_TextPrimary,
    surfaceVariant = Dark_SurfaceSecondary,
    onSurfaceVariant = Dark_TextSecondary,
    outline = Dark_Border,
    outlineVariant = Dark_Divider,
    error = Dark_Error,
    onError = Dark_OnPrimary,
    errorContainer = Dark_ErrorContainer,
    onErrorContainer = Dark_Error,
)

@Composable
fun KrewzoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val krewzoColors = if (darkTheme) DarkKrewzoColors else LightKrewzoColors

    // Set status bar color and appearance
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(
        LocalKrewzoColors provides krewzoColors,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = KrewzoTypography,
            content = content,
        )
    }
}

/**
 * Access extended Krewzo colors and dimension tokens that aren't part of Material 3's ColorScheme.
 *
 * Usage:
 * ```
 * val border = KrewzoTheme.colors.border
 * val success = KrewzoTheme.colors.success
 * ```
 */
object KrewzoTheme {
    val colors: KrewzoColors
        @Composable
        get() = LocalKrewzoColors.current

    val dimens: KrewzoDimens
        get() = KrewzoDimens
}

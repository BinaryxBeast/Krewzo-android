package com.binaryxbeast.krewzo.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────
// Extended Color Scheme
// Colors that Material 3 ColorScheme doesn't
// natively provide (semantic, surface variants,
// booking status, etc.)
// ─────────────────────────────────────────────

@Immutable
data class KrewzoColors(
    // Surfaces
    val surfaceSecondary: Color,
    val surfaceTertiary: Color,

    // Text
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,

    // Borders
    val border: Color,
    val divider: Color,
    val focusRing: Color,

    // Primary variants
    val primaryHover: Color,

    // Semantic
    val success: Color,
    val successContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val info: Color,
    val infoContainer: Color,

    // Rating & Highlights
    val ratingGreen: Color,
    val ratingGreenContainer: Color,
    val offerGold: Color,
    val offerGoldContainer: Color,
    val brandCrimson: Color,
    val brandCrimsonContainer: Color,
    val brandPrimary: Color,
    val brandPrimaryContainer: Color,
)

val LightKrewzoColors = KrewzoColors(
    surfaceSecondary = Light_SurfaceSecondary,
    surfaceTertiary = Light_SurfaceTertiary,
    textPrimary = Light_TextPrimary,
    textSecondary = Light_TextSecondary,
    textTertiary = Light_TextTertiary,
    border = Light_Border,
    divider = Light_Divider,
    focusRing = Light_FocusRing,
    primaryHover = Light_PrimaryHover,
    success = Light_Success,
    successContainer = Light_SuccessContainer,
    warning = Light_Warning,
    warningContainer = Light_WarningContainer,
    info = Light_Info,
    infoContainer = Light_InfoContainer,
    ratingGreen = RatingGreen,
    ratingGreenContainer = RatingGreenContainer,
    offerGold = OfferGold,
    offerGoldContainer = OfferGoldContainer,
    brandCrimson = Light_Error,
    brandCrimsonContainer = Light_ErrorContainer,
    brandPrimary = ElectricIndigo,
    brandPrimaryContainer = ElectricIndigoContainer,
)

val DarkKrewzoColors = KrewzoColors(
    surfaceSecondary = Dark_SurfaceSecondary,
    surfaceTertiary = Dark_SurfaceTertiary,
    textPrimary = Dark_TextPrimary,
    textSecondary = Dark_TextSecondary,
    textTertiary = Dark_TextTertiary,
    border = Dark_Border,
    divider = Dark_Divider,
    focusRing = Dark_FocusRing,
    primaryHover = Dark_PrimaryHover,
    success = Dark_Success,
    successContainer = Dark_SuccessContainer,
    warning = Dark_Warning,
    warningContainer = Dark_WarningContainer,
    info = Dark_Info,
    infoContainer = Dark_InfoContainer,
    ratingGreen = Dark_Success,
    ratingGreenContainer = Dark_SuccessContainer,
    offerGold = Dark_Warning,
    offerGoldContainer = Dark_WarningContainer,
    brandCrimson = Dark_Error,
    brandCrimsonContainer = Dark_ErrorContainer,
    brandPrimary = Dark_Primary,
    brandPrimaryContainer = Dark_PrimaryContainer,
)

val LocalKrewzoColors = staticCompositionLocalOf { LightKrewzoColors }

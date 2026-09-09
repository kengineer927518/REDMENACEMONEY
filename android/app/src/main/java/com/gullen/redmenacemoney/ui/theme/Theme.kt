package com.gullen.redmenacemoney.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val RedMenaceColors = lightColorScheme(
    primary = Rail,
    onPrimary = Paper,
    secondary = Amber,
    onSecondary = Ink,
    background = Paper,
    onBackground = Ink,
    surface = PaperCard,
    onSurface = Ink,
    error = Rust,
    onError = Paper
)

// Mono is used for all dollar figures/dates, matching the ledger identity from the web app.
val MonoFamily = FontFamily.Monospace
val DisplayFamily = FontFamily.SansSerif

private val RedMenaceTypography = Typography(
    titleLarge = TextStyle(fontFamily = DisplayFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp),
    titleMedium = TextStyle(fontFamily = DisplayFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp),
    bodyLarge = TextStyle(fontFamily = DisplayFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    bodyMedium = TextStyle(fontFamily = DisplayFamily, fontWeight = FontWeight.Normal, fontSize = 13.sp),
    labelLarge = TextStyle(fontFamily = DisplayFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
)

@Composable
fun RedMenaceMoneyTheme(content: @Composable () -> Unit) {
    // Always light: this is a paper-ledger look by design, not something that should flip to a dark palette.
    MaterialTheme(
        colorScheme = RedMenaceColors,
        typography = RedMenaceTypography,
        content = content
    )
}

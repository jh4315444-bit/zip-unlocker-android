package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CyberVaultDarkColorScheme = darkColorScheme(
    primary = AmberGoldLight,
    onPrimary = Color(0xFF1E1300),
    primaryContainer = AmberGoldContainer,
    onPrimaryContainer = AmberGoldLight,
    secondary = CyanLight,
    onSecondary = Color(0xFF002631),
    secondaryContainer = CyanContainer,
    onSecondaryContainer = CyanLight,
    tertiary = EmeraldUnlock,
    onTertiary = Color(0xFF00291B),
    tertiaryContainer = EmeraldContainer,
    onTertiaryContainer = Color(0xFF6EE7B7),
    error = CrimsonLock,
    onError = Color.White,
    errorContainer = CrimsonContainer,
    onErrorContainer = Color(0xFFFFB4C0),
    background = VaultNavy,
    onBackground = TextPrimaryDark,
    surface = VaultSlate,
    onSurface = TextPrimaryDark,
    surfaceVariant = VaultCard,
    onSurfaceVariant = TextSecondaryDark,
    outline = OutlineDark
)

private val CyberVaultLightColorScheme = lightColorScheme(
    primary = AmberDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFEF3C7),
    onPrimaryContainer = Color(0xFF451A03),
    secondary = CyanDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCFFAFE),
    onSecondaryContainer = Color(0xFF083344),
    tertiary = Color(0xFF059669),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD1FAE5),
    onTertiaryContainer = Color(0xFF064E3B),
    background = LightBackground,
    onBackground = Color(0xFF0F172A),
    surface = LightSurface,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) CyberVaultDarkColorScheme else CyberVaultLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

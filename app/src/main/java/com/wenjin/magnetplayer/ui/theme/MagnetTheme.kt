package com.wenjin.magnetplayer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.TextStyle
import com.wenjin.magnetplayer.preferences.AppearancePreferences
import com.wenjin.magnetplayer.preferences.preference.collectAsState
import org.koin.compose.koinInject

/** App 主题：浅色纯白 / 深色，默认跟随系统，设置里可切换 */
@Composable
fun MagnetTheme(content: @Composable () -> Unit) {
  val preferences = koinInject<AppearancePreferences>()
  val darkMode by preferences.darkMode.collectAsState()
  val dark = when (darkMode) {
    DarkMode.Dark -> true
    DarkMode.Light -> false
    DarkMode.System -> isSystemInDarkTheme()
  }
  MagnetTheme(dark, content)
}

@Composable
fun MagnetTheme(dark: Boolean, content: @Composable () -> Unit) {
  val colors = if (dark) MagnetColors.Dark else MagnetColors.Light
  CompositionLocalProvider(LocalMagnetColors provides colors) {
    MaterialTheme(colorScheme = colors.toColorScheme(), content = content)
  }
}

/** 给 Material3 组件（对话框、菜单、设置页等）用的配色，和 token 保持一致 */
fun MagnetColors.toColorScheme() = if (isDark) {
  darkColorScheme(
    primary = MagnetColors.Primary,
    onPrimary = blue.onFill,
    primaryContainer = blue.tint,
    onPrimaryContainer = blue.onTint,
    secondary = purple.icon,
    secondaryContainer = purple.tint,
    onSecondaryContainer = purple.onTint,
    tertiary = green.icon,
    tertiaryContainer = green.tint,
    onTertiaryContainer = green.onTint,
    background = background,
    onBackground = onBackground,
    surface = background,
    onSurface = onBackground,
    surfaceVariant = surface,
    onSurfaceVariant = onSurfaceVariant,
    surfaceContainerLowest = background,
    surfaceContainerLow = surface,
    surfaceContainer = surface,
    surfaceContainerHigh = surfaceItem,
    surfaceContainerHighest = surfaceItem,
    outline = divider,
    outlineVariant = outline,
    error = red.icon,
  )
} else {
  lightColorScheme(
    primary = MagnetColors.Primary,
    onPrimary = blue.onFill,
    primaryContainer = blue.tint,
    onPrimaryContainer = blue.onTint,
    secondary = purple.fill,
    secondaryContainer = purple.tint,
    onSecondaryContainer = purple.onTint,
    tertiary = green.fill,
    tertiaryContainer = green.tint,
    onTertiaryContainer = green.onTint,
    background = background,
    onBackground = onBackground,
    surface = background,
    onSurface = onBackground,
    surfaceVariant = surface,
    onSurfaceVariant = onSurfaceVariant,
    surfaceContainerLowest = background,
    surfaceContainerLow = surface,
    surfaceContainer = surface,
    surfaceContainerHigh = surface,
    surfaceContainerHighest = divider,
    outline = divider,
    outlineVariant = outline,
    error = red.fill,
  )
}

object MagnetTheme {
  val colors: MagnetColors
    @Composable get() = LocalMagnetColors.current
}

/** 数字用等宽（tnum） */
val TabularNums = TextStyle(fontFeatureSettings = "tnum")

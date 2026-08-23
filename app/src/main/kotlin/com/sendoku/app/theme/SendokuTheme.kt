package com.sendoku.app.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The looks Sendoku can wear.
 *
 * Only Deep Field exists so far. Three more are already chosen and will arrive as entries
 * here rather than as changes anywhere else: Ink and Paper, Slate Zen, and Terminal. That
 * is the whole reason the tokens are shaped the way they are.
 */
public enum class SendokuThemeId(public val displayName: String) {
    DEEP_FIELD("Deep Field"),
}

/**
 * Where a theme's tokens come from.
 *
 * A theme is data, not code. Adding one means adding a branch here and nothing else, and if
 * a new theme ever needs different spacing, rounding or timing it says so by handing back a
 * different [SendokuDimens] or [SendokuMotion], not by touching a composable.
 */
public object SendokuThemes {

    public fun colors(id: SendokuThemeId, dark: Boolean): SendokuColors = when (id) {
        SendokuThemeId.DEEP_FIELD -> if (dark) DeepFieldDark else DeepFieldLight
    }

    public fun type(id: SendokuThemeId): SendokuType = when (id) {
        SendokuThemeId.DEEP_FIELD -> DefaultType
    }

    public fun dimens(id: SendokuThemeId): SendokuDimens = when (id) {
        SendokuThemeId.DEEP_FIELD -> DefaultDimens
    }

    public fun motion(id: SendokuThemeId): SendokuMotion = when (id) {
        SendokuThemeId.DEEP_FIELD -> DefaultMotion
    }
}

internal val LocalSendokuColors = staticCompositionLocalOf { DeepFieldDark }
internal val LocalSendokuType = staticCompositionLocalOf { DefaultType }
internal val LocalSendokuDimens = staticCompositionLocalOf { DefaultDimens }
internal val LocalSendokuMotion = staticCompositionLocalOf { DefaultMotion }

/**
 * How a composable reaches the theme.
 *
 * `Sendoku.colors.given` rather than a hex value, `Sendoku.dimens.spaceM` rather than
 * `16.dp`. Anything that reaches past this to a literal has quietly opted out of every
 * theme but the one it was written against.
 */
public object Sendoku {

    public val colors: SendokuColors
        @Composable @ReadOnlyComposable get() = LocalSendokuColors.current

    public val type: SendokuType
        @Composable @ReadOnlyComposable get() = LocalSendokuType.current

    public val dimens: SendokuDimens
        @Composable @ReadOnlyComposable get() = LocalSendokuDimens.current

    public val motion: SendokuMotion
        @Composable @ReadOnlyComposable get() = LocalSendokuMotion.current
}

/**
 * Wraps the app in a theme.
 *
 * Material is still underneath, because ripples, text selection handles and the odd Material
 * component need a colour scheme to read from. Sendoku's own tokens are the ones composables
 * actually use; the Material scheme exists so nothing inherited looks out of place.
 */
@Composable
public fun SendokuTheme(
    themeId: SendokuThemeId = SendokuThemeId.DEEP_FIELD,
    dark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = SendokuThemes.colors(themeId, dark)
    val type = SendokuThemes.type(themeId)

    val material = if (dark) {
        darkColorScheme(
            primary = colors.accent,
            onPrimary = colors.onAccent,
            background = colors.background,
            onBackground = colors.given,
            surface = colors.surface,
            onSurface = colors.given,
            surfaceVariant = colors.surfaceRaised,
            onSurfaceVariant = colors.muted,
            error = colors.conflict,
            outline = colors.boxLine,
        )
    } else {
        lightColorScheme(
            primary = colors.accent,
            onPrimary = colors.onAccent,
            background = colors.background,
            onBackground = colors.given,
            surface = colors.surface,
            onSurface = colors.given,
            surfaceVariant = colors.surfaceRaised,
            onSurfaceVariant = colors.muted,
            error = colors.conflict,
            outline = colors.boxLine,
        )
    }

    CompositionLocalProvider(
        LocalSendokuColors provides colors,
        LocalSendokuType provides type,
        LocalSendokuDimens provides SendokuThemes.dimens(themeId),
        LocalSendokuMotion provides SendokuThemes.motion(themeId),
    ) {
        MaterialTheme(
            colorScheme = material,
            typography = MaterialTheme.typography.copy(
                bodyMedium = type.body,
                labelLarge = type.label,
                titleMedium = type.title,
            ),
            content = content,
        )
    }
}

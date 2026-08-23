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
public enum class SendokuThemeId(public val displayName: String, public val summary: String) {
    DEEP_FIELD("Deep Field", "True black, one cyan accent, nothing else"),
    INK("Ink and Paper", "A newspaper puzzle book, printed on warm paper"),
    ZEN("Slate Zen", "Sage and stone, and as little else as possible"),
    TERMINAL("Terminal", "Monospace, hard edges, dark only"),
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
        SendokuThemeId.INK -> if (dark) InkDark else InkLight
        SendokuThemeId.ZEN -> if (dark) ZenDark else ZenLight
        // Terminal is dark whatever the system says. A terminal in light mode is a text
        // editor, and looking nothing like a phone app is the entire point of it.
        SendokuThemeId.TERMINAL -> TerminalDark
    }

    /** True when a theme ignores the light and dark setting. */
    public fun isFixed(id: SendokuThemeId): Boolean = id == SendokuThemeId.TERMINAL

    public fun type(id: SendokuThemeId): SendokuType = when (id) {
        SendokuThemeId.DEEP_FIELD, SendokuThemeId.ZEN -> DefaultType
        SendokuThemeId.INK -> InkType
        SendokuThemeId.TERMINAL -> TerminalType
    }

    public fun dimens(id: SendokuThemeId): SendokuDimens = when (id) {
        SendokuThemeId.DEEP_FIELD -> DefaultDimens
        SendokuThemeId.INK -> InkDimens
        SendokuThemeId.ZEN -> ZenDimens
        SendokuThemeId.TERMINAL -> TerminalDimens
    }

    public fun motion(id: SendokuThemeId): SendokuMotion = when (id) {
        SendokuThemeId.DEEP_FIELD, SendokuThemeId.INK -> DefaultMotion
        SendokuThemeId.ZEN -> ZenMotion
        SendokuThemeId.TERMINAL -> TerminalMotion
    }
}

/**
 * True when the player has turned animation off in the system settings.
 *
 * Accessibility settings and battery savers both set the animator scale to zero, and an app
 * that keeps animating anyway is ignoring somebody who asked it not to. Read once: it needs
 * a restart to change, which is what the platform does too.
 */
@Composable
@ReadOnlyComposable
private fun motionIsOff(): Boolean {
    val context = androidx.compose.ui.platform.LocalContext.current
    return android.provider.Settings.Global.getFloat(
        context.contentResolver,
        android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
        1f,
    ) == 0f
}

/** Everything at once, for when motion is switched off. */
private val Still: SendokuMotion = SendokuMotion(
    instant = 0,
    quick = 0,
    settle = 0,
    celebrate = 0,
    easing = androidx.compose.animation.core.LinearEasing,
    enter = androidx.compose.animation.core.LinearEasing,
)

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
    val motion = SendokuThemes.motion(themeId).takeUnless { motionIsOff() } ?: Still

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
        LocalSendokuMotion provides motion,
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

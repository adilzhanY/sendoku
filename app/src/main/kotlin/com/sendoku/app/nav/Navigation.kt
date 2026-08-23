package com.sendoku.app.nav

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sendoku.engine.Grade

/**
 * Every screen the app has, as a type.
 *
 * A sealed hierarchy rather than route strings. There are six screens and they will never
 * be deep linked from outside, so a string parser and a navigation library would buy
 * nothing that the compiler is not already giving for free: a destination that carries the
 * wrong argument does not compile, and a screen that is never reachable shows up as an
 * unused declaration.
 */
@Immutable
public sealed interface Destination {

    public data object Home : Destination

    /** A new puzzle at a chosen grade. */
    public data class Play(val grade: Grade) : Destination

    /** The puzzle the player already had on the go. */
    public data object Resume : Destination

    /** Today's puzzle, the same one everybody gets. */
    public data class Daily(val epochDay: Long) : Destination

    public data object Settings : Destination

    /** Every technique the app knows, for somebody who has just met one. */
    public data object Glossary : Destination

    public data object Stats : Destination

    public data object About : Destination

    public data object Licences : Destination
}

/**
 * A back stack, and nothing else.
 *
 * Home is always at the bottom and cannot be popped, so back from a screen always has
 * somewhere to go and the app never closes from a screen the player navigated into.
 *
 * Saveable, because it has to be. Turning the phone sideways recreates the activity, and a
 * back stack held in a plain `remember` is gone by the time the new one draws, which put
 * the player back on the home screen in the middle of a puzzle.
 */
public class Navigator(stack: List<Destination> = listOf(Destination.Home)) {

    private var stack by mutableStateOf(stack)

    public val current: Destination get() = stack.last()

    public val canGoBack: Boolean get() = stack.size > 1

    public fun go(destination: Destination) {
        stack = stack + destination
    }

    /** Replaces the top of the stack, for when going back to where you were makes no sense. */
    public fun replace(destination: Destination) {
        stack = stack.dropLast(1) + destination
    }

    public fun back(): Boolean {
        if (!canGoBack) return false
        stack = stack.dropLast(1)
        return true
    }

    public fun home() {
        stack = listOf(stack.first())
    }

    internal fun snapshot(): List<Destination> = stack

    public companion object {
        /**
         * Saves the stack across a configuration change.
         *
         * Destinations are written as short strings rather than serialised, because there
         * are five of them and one carries a single number.
         */
        public val Saver: androidx.compose.runtime.saveable.Saver<Navigator, List<String>> =
            androidx.compose.runtime.saveable.Saver(
                save = { navigator -> navigator.snapshot().map { it.encode() } },
                restore = { saved -> Navigator(saved.map { decode(it) }) },
            )

        private fun Destination.encode(): String = when (this) {
            Destination.Home -> "home"
            is Destination.Play -> "play:${grade.name}"
            Destination.Resume -> "resume"
            is Destination.Daily -> "daily:$epochDay"
            Destination.Settings -> "settings"
            Destination.Glossary -> "glossary"
            Destination.Stats -> "stats"
            Destination.About -> "about"
            Destination.Licences -> "licences"
        }

        private fun decode(value: String): Destination = when {
            value == "home" -> Destination.Home
            value == "resume" -> Destination.Resume
            value == "settings" -> Destination.Settings
            value == "glossary" -> Destination.Glossary
            value == "stats" -> Destination.Stats
            value == "about" -> Destination.About
            value == "licences" -> Destination.Licences
            value.startsWith("play:") -> Destination.Play(Grade.valueOf(value.removePrefix("play:")))
            value.startsWith("daily:") -> Destination.Daily(value.removePrefix("daily:").toLong())
            else -> Destination.Home
        }
    }
}

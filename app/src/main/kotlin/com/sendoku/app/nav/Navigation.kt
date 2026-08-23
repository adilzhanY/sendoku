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
}

/**
 * A back stack, and nothing else.
 *
 * Home is always at the bottom and cannot be popped, so back from a screen always has
 * somewhere to go and the app never closes from a screen the player navigated into.
 */
public class Navigator(start: Destination = Destination.Home) {

    private var stack by mutableStateOf(listOf(start))

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
}

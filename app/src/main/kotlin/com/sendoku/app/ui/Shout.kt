package com.sendoku.app.ui

import java.util.Locale

/**
 * Upper case, in the language the app is being read in.
 *
 * Kotlin's own `uppercase()` takes no locale and uses the root one, which is the right
 * default for anything a machine reads and the wrong one for anything a person does. The
 * difference has a name in Turkish: a dotless i becomes a dotted capital, and every overline
 * in the app would be spelled wrong for a Turkish reader without this.
 *
 * Only ever for text on screen. Anything compared, stored or sent stays at the root locale,
 * where "i" and "I" mean what everybody else in the world thinks they mean.
 */
internal fun String.shout(): String = uppercase(Locale.getDefault())

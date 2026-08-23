package com.sendoku.app

/**
 * Marks a test that exists to draw a picture rather than to check anything.
 *
 * The ordinary run excludes it, because eight full screen renders are slow and none of them
 * can fail. tools/store-shots.sh asks for it by name.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class StoreShot

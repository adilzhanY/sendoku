package com.sendoku.app.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import java.io.File

/**
 * Writes the card and hands it to whatever the player picks.
 *
 * Into the cache directory, because a shared card is not a document the app is keeping. It is
 * written, handed over, and left for Android to clear when it needs the space. The same file
 * name every time, so sharing twice does not leave two.
 */
public object ShareResult {

    private const val FOLDER = "shared"
    private const val NAME = "sendoku-result.png"

    public fun share(context: Context, card: Bitmap, chooserTitle: String) {
        val folder = File(context.cacheDir, FOLDER).apply { mkdirs() }
        val file = File(folder, NAME)
        file.outputStream().use { card.compress(Bitmap.CompressFormat.PNG, 100, it) }

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.shared", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, chooserTitle))
    }
}

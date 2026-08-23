package com.sendoku.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import com.sendoku.app.theme.Sendoku

/** One thing the app was built out of. */
public data class Licence(val artifact: String, val licence: String)

/**
 * What Sendoku is standing on.
 *
 * The list of artifacts is generated at build time from the resolved dependency graph and
 * read back here, so it cannot fall behind what the app actually ships. The licence names
 * are declared in the build file rather than parsed out of each POM, which is the one part
 * a person has to keep honest.
 */
@Composable
public fun LicencesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Sendoku.colors
    val dimens = Sendoku.dimens
    val context = LocalContext.current
    val licences = remember { readLicences(context) }

    Column(modifier = modifier.fillMaxSize().background(colors.background)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(dimens.spaceM),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceM),
        ) {
            Text(
                text = "BACK",
                style = Sendoku.type.overline,
                color = colors.muted,
                modifier = Modifier
                    .clip(RoundedCornerShape(dimens.radiusS))
                    .clickable(onClick = onBack)
                    .padding(dimens.spaceS),
            )
            Text("Licences", style = Sendoku.type.title, color = colors.given)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = dimens.spaceM),
            verticalArrangement = Arrangement.spacedBy(dimens.spaceS),
            contentPadding = PaddingValues(bottom = dimens.spaceXl),
        ) {
            items(licences) { entry ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(dimens.radiusM))
                        .background(colors.surface)
                        .padding(dimens.spaceM),
                ) {
                    Text(entry.artifact, style = Sendoku.type.body, color = colors.given)
                    Text(entry.licence, style = Sendoku.type.body, color = colors.muted)
                }
            }
        }
    }
}

/** Reads the list the build generated. Blank rather than crashing if it is somehow missing. */
private fun readLicences(context: android.content.Context): List<Licence> = runCatching {
    context.assets.open("licences.txt").bufferedReader().readLines()
        .filter { it.isNotBlank() }
        .map { line ->
            val parts = line.split("|")
            Licence(parts[0], parts.getOrElse(1) { "unknown" })
        }
}.getOrDefault(emptyList())

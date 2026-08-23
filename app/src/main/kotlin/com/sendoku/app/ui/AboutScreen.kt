package com.sendoku.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import com.sendoku.app.R
import com.sendoku.app.theme.Sendoku

/**
 * What this app is, and what it will not do.
 *
 * The promises are written down here because they are the reason the app exists, and
 * because a promise nobody can find is not much of a promise.
 */
@Composable
public fun AboutScreen(version: String, onBack: () -> Unit, onLicences: () -> Unit, modifier: Modifier = Modifier) {
    val colors = Sendoku.colors
    val dimens = Sendoku.dimens

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(dimens.spaceM),
        verticalArrangement = Arrangement.spacedBy(dimens.spaceM),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceM),
        ) {
            Text(
                text = stringResource(R.string.back),
                style = Sendoku.type.overline,
                color = colors.muted,
                modifier = Modifier
                    .clip(RoundedCornerShape(dimens.radiusS))
                    .clickable(onClick = onBack)
                    .padding(dimens.spaceS),
            )
            Text(stringResource(R.string.about_title), style = Sendoku.type.title, color = colors.given)
        }

        Text(stringResource(R.string.about_version, version), style = Sendoku.type.display, color = colors.given)

        Text(
            text = stringResource(R.string.about_tagline),
            style = Sendoku.type.body,
            color = colors.given,
        )

        Promise(stringResource(R.string.about_no_ads), stringResource(R.string.about_no_ads_detail))
        Promise(stringResource(R.string.about_no_tracking), stringResource(R.string.about_no_tracking_detail))
        Promise(stringResource(R.string.about_no_purchases), stringResource(R.string.about_no_purchases_detail))
        Promise(stringResource(R.string.about_offline), stringResource(R.string.about_offline_detail))
        Promise(stringResource(R.string.about_hints), stringResource(R.string.about_hints_detail))

        Text(
            text = stringResource(R.string.about_rating),
            style = Sendoku.type.body,
            color = colors.muted,
        )

        Text(
            text = stringResource(R.string.about_licences),
            style = Sendoku.type.overline,
            color = colors.accent,
            modifier = Modifier
                .padding(top = dimens.spaceL)
                .clip(RoundedCornerShape(dimens.radiusS))
                .clickable(onClick = onLicences)
                .padding(dimens.spaceS),
        )
    }
}

@Composable
private fun Promise(title: String, detail: String) {
    Column(verticalArrangement = Arrangement.spacedBy(Sendoku.dimens.spaceXs)) {
        Text(title, style = Sendoku.type.label, color = Sendoku.colors.accent)
        Text(detail, style = Sendoku.type.body, color = Sendoku.colors.muted)
    }
}

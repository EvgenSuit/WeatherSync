package com.weathersync.common.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weathersync.R
import com.weathersync.ui.theme.arapeyRegular
import com.weathersync.ui.theme.quicksandMedium

@Composable
fun CustomButton(
    enabled: Boolean,
    text: String,
    colors: ButtonColors = ButtonDefaults.elevatedButtonColors(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer
    ),
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    ElevatedButton(
        enabled = enabled,
        onClick = onClick,
        colors = colors,
        shape = RoundedCornerShape(dimensionResource(id = R.dimen.button_angle)),
        modifier = modifier.fillMaxWidth()) {
        Text(text = text,
            textAlign = TextAlign.Center,
            style = TextStyle(fontFamily = arapeyRegular, fontSize = 20.sp))
    }
}

@Composable
fun CustomCircularProgressIndicator(
    modifier: Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxWidth()) {
        CircularProgressIndicator(modifier = modifier)
    }
}

@Composable
fun UpgradeToPremium(onClick: () -> Unit) {
    TextButton(onClick = onClick
    ) {
        Text(text = stringResource(id = R.string.upgrade_to_premium),
            style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun Modifier.backgroundBrush(): Modifier = this.background(brush = Brush.linearGradient(
    colors = listOf(
        MaterialTheme.colorScheme.secondaryContainer,
        MaterialTheme.colorScheme.primaryContainer
    )
))

@Composable
fun NextGenerationComponent(nextUpdateTime: String,
                            upgradePrompt: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .testTag("Next generation time")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val info = Icons.Filled.Info
            Icon(imageVector = info,
                contentDescription = info.name,
                tint = MaterialTheme.colorScheme.onBackground.copy(0.7f),
                modifier = Modifier.size(20.dp))
            Text(text = nextUpdateTime,
                style = TextStyle(fontFamily = quicksandMedium))
        }
        upgradePrompt()
    }
}

@Composable
fun CustomLinearProgressIndicator(
    modifier: Modifier
) {
    LinearProgressIndicator(modifier = modifier
        .fillMaxWidth()
        .height(dimensionResource(id = R.dimen.linear_progress_height)))
}

@Composable
fun PrivacyTermsLinks(
    modifier: Modifier = Modifier
        .padding(16.dp)
        .padding(top = 20.dp)
) {
    val context = LocalContext.current
    val privacyPolicyLink = stringResource(id = R.string.privacy_policy_link)
    val termsOfServiceLink = stringResource(id = R.string.terms_of_service_link)
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
    ) {
        Text(text = stringResource(id = R.string.privacy_policy),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.clickable { openLinkInBrowser(context, privacyPolicyLink) })
        Text(text = stringResource(id = R.string.terms_of_service),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.clickable { openLinkInBrowser(context, termsOfServiceLink) })
    }
}

fun openLinkInBrowser(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    context.startActivity(intent)
}
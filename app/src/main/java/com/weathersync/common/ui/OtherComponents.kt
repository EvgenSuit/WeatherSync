package com.weathersync.common.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.weathersync.R
import com.weathersync.ui.theme.arapeyRegular

@Composable
fun CustomButton(
    enabled: Boolean,
    text: String,
    colors: ButtonColors = ButtonDefaults.elevatedButtonColors(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer
    ),
    onClick: () -> Unit,
) {
    ElevatedButton(
        enabled = enabled,
        onClick = onClick,
        colors = colors,
        shape = RoundedCornerShape(dimensionResource(id = R.dimen.button_angle)),
        modifier = Modifier.fillMaxWidth()) {
        Text(text = text,
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
fun CustomLinearProgressIndicator(
    modifier: Modifier
) {
    LinearProgressIndicator(modifier = modifier
        .fillMaxWidth()
        .height(dimensionResource(id = R.dimen.linear_progress_height)))
}
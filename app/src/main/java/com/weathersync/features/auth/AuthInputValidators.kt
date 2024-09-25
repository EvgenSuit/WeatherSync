package com.weathersync.features.auth

import android.util.Patterns
import com.weathersync.R
import com.weathersync.common.ui.UIText

class EmailValidator {
    private val emailPattern = Patterns.EMAIL_ADDRESS

    operator fun invoke(value: String): UIText =
        if (value.isBlank()) {
            UIText.StringResource(R.string.email_empty)
        } else if (!emailPattern.matcher(value).matches()) {
            UIText.StringResource(R.string.email_invalid)
        } else UIText.Empty
}

class PasswordValidator {
    private val minLength = 8
    private val maxLength = 30

    operator fun invoke(value: String): UIText =
        if (value.isBlank()) UIText.StringResource(R.string.password_empty)
        else if (!value.any { it.isDigit() }) UIText.StringResource(R.string.password_no_digits)
        else if (!(value.any { it.isUpperCase() } && value.any { it.isLowerCase() })) UIText.StringResource(R.string.password_no_mixed_case)
        else if (value.any { it.isWhitespace() }) UIText.StringResource(R.string.password_whitespace)
        else if (value.all { it.isLetterOrDigit() }) UIText.StringResource(R.string.password_no_special)
        else if (value.length < minLength) UIText.StringResource(R.string.password_short)
        else if (value.length > maxLength) UIText.StringResource(R.string.password_long)
        else UIText.Empty
}
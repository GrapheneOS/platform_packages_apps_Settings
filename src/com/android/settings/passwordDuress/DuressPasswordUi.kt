/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.passwordDuress

import android.app.Activity
import android.app.AlertDialog
import android.app.admin.DevicePolicyManager
import android.app.admin.PasswordMetrics
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import com.android.internal.widget.LockPatternUtils
import com.android.internal.widget.LockscreenCredential
import com.android.internal.widget.PasswordValidationError
import com.android.settings.password.ChooseLockSettingsHelper
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textview.MaterialTextView
import com.android.settings.R

const val CONFIRM_EXISTING_REQUEST = 100

class DuressPasswordUi : AppCompatActivity() {

    private val context: Context by lazy { this }

    private lateinit var title: MaterialTextView
    private lateinit var submitBtn: MaterialButton
    private lateinit var removeBtn: MaterialButton

    private lateinit var pinInput: TextInputEditText
    private lateinit var pinInputCnf: TextInputEditText

    private lateinit var passwordInput: TextInputEditText
    private lateinit var passwordInputCnf: TextInputEditText

    private lateinit var userCredential: LockscreenCredential

    private val lockPatternUtils: LockPatternUtils by lazy {
        LockPatternUtils(this)
    }

    private fun saveAndFinish(pin: String, password: String) {

        pinInput.text?.clear()
        pinInputCnf.text?.clear()
        passwordInput.text?.clear()
        pinInput.text?.clear()
        submitBtn.isEnabled = false

        val pinCredential = LockscreenCredential.createPin(pin)
        val passwordCredential = LockscreenCredential.createPassword(password)

        val executors = java.util.concurrent.Executors.newSingleThreadExecutor()
        executors.submit {
            try {
                lockPatternUtils.setDuressCredentials(userCredential, pinCredential, passwordCredential)
            } catch (e: java.lang.IllegalStateException) {
                runOnUiThread {
                    Toast.makeText(this, e.localizedMessage, Toast.LENGTH_LONG).show()
                }
            }
            runOnUiThread {
                userCredential.zeroize()
                pinCredential.zeroize()
                passwordCredential.zeroize()

                //close this activity
                finish()
            }

        }
    }

    private val editorActionListener = TextView.OnEditorActionListener { view, actionId, _ ->

        val isPinEntry = view.id == R.id.pin_input || view.id == R.id.pin_input_conformation
        val value = view.text?.toString() ?: ""
        val errors = if (isPinEntry) validatePin(value) else validatePassword(value)
        val isValid = errors.isEmpty()
        val errorMsg = errors.firstOrNull()?.toString()
        view.error = null

        val validPins by lazy { validatePins() }

        if (actionId != EditorInfo.IME_ACTION_NEXT && actionId != EditorInfo.IME_ACTION_DONE) {
            view.error = null
            return@OnEditorActionListener true
        }

        when (view.id) {
            R.id.pin_input -> {
                if (!isValid) {
                    view.error = errorMsg
                    return@OnEditorActionListener true
                }
                pinInputCnf.requestFocus()
            }

            R.id.pin_input_conformation -> {
                if (!validPins) {
                    view.error = context.getString(R.string.lockpassword_confirm_pins_dont_match)
                    return@OnEditorActionListener true
                }
                passwordInput.requestFocus()
            }

            R.id.password_input -> {
                if (!isValid) {
                    view.error = errorMsg
                    return@OnEditorActionListener true
                }
                passwordInputCnf.requestFocus()
            }

            R.id.password_input_conformation -> submitBtn.performClick()
            else -> throw IllegalStateException("unhanded view")
        }

        true
    }

    private val onSubmitListener = View.OnClickListener { _ ->

        val pin = pinInput.text.toString()
        val password = passwordInput.text.toString()

        val pinErrors = validatePin(pin)
        val passwordErrors = validatePassword(password)

        val validPins by lazy { validatePins() }
        val validPasswords by lazy { validatePasswords() }

        if (pinErrors.isNotEmpty()) {
            pinInput.error = pinErrors.firstOrNull()?.toString()
            return@OnClickListener
        }

        if (passwordErrors.isNotEmpty()) {
            passwordInput.error = passwordErrors.firstOrNull()?.toString()
            return@OnClickListener
        }

        if (!validPins) {
            pinInputCnf.error = context.getString(R.string.lockpassword_confirm_pins_dont_match)
            return@OnClickListener
        }

        if (!validPasswords) {
            passwordInputCnf.error = context.getString(R.string.lockpassword_confirm_passwords_dont_match)
            return@OnClickListener
        }

        saveAndFinish(pin = pin, password = password)
    }

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)
        setTheme(com.google.android.material.R.style.Theme_Material3_DayNight_NoActionBar)
        com.google.android.material.color.DynamicColors.applyToActivityIfAvailable(this)
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        setContentView(R.layout.duress_password_ui)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        title = findViewById<MaterialTextView>(R.id.lock_title)
        submitBtn = findViewById<MaterialButton>(R.id.confirm)
        removeBtn = findViewById<MaterialButton>(R.id.remove)
        pinInput = findViewById<TextInputEditText>(R.id.pin_input)
        pinInputCnf = findViewById<TextInputEditText>(R.id.pin_input_conformation)
        passwordInput = findViewById<TextInputEditText>(R.id.password_input)
        passwordInputCnf = findViewById<TextInputEditText>(R.id.password_input_conformation)


        title.text =
                if (lockPatternUtils.validDuressCredentialsExist()) getString(R.string.update_or_remove_duress_password_title)
                else getString(R.string.choose_duress_password_title)

        pinInput.requestFocus()
        pinInput.setOnEditorActionListener(editorActionListener)
        pinInputCnf.setOnEditorActionListener(editorActionListener)
        passwordInput.setOnEditorActionListener(editorActionListener)
        passwordInputCnf.setOnEditorActionListener(editorActionListener)
        submitBtn.setOnClickListener(onSubmitListener)
        removeBtn.isVisible = lockPatternUtils.validDuressCredentialsExist()
        removeBtn.setOnClickListener {
            AlertDialog.Builder(context)
                    .setTitle(R.string.delete_duress_title)
                    .setMessage(R.string.delete_duress_descriptions)
                    .setPositiveButton(android.R.string.ok) { dialog, _ ->
                        lockPatternUtils.deleteDuressConfig(userCredential)
                        dialog.dismiss()
                        this@DuressPasswordUi.finish()
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()

        }

        if (!this::userCredential.isInitialized) {
            ChooseLockSettingsHelper.Builder(this)
                    .setRequestCode(CONFIRM_EXISTING_REQUEST)
                    .setTitle(getString(R.string.unlock_set_unlock_launch_picker_title))
                    .setReturnCredentials(true)
                    .setUserId(android.os.UserHandle.myUserId())
                    .show()
        }
    }

    override fun onResume() {
        super.onResume()
        val mask = android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS

        findViewById<View>(R.id.duress_root).windowInsetsController?.setSystemBarsAppearance(
                if ((resources.configuration.uiMode and
                                android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                        == android.content.res.Configuration.UI_MODE_NIGHT_YES) 0 else mask, mask
        )
    }

    //ChooseLockSettingsHelper.Builder doesn't support IntentLauncher
    @Suppress("OVERRIDE_DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode != CONFIRM_EXISTING_REQUEST) return
        if (resultCode != Activity.RESULT_OK) finish()
        data?.getParcelableExtra(ChooseLockSettingsHelper.EXTRA_KEY_PASSWORD, LockscreenCredential::class.java)?.let {
            userCredential = it
        }
    }

    private fun validatePins(): Boolean {
        val pin = pinInput.text.toString()
        val pinConformation = pinInputCnf.text.toString()
        val isValid = (pin == pinConformation)
        return isValid && pin.isNotEmpty()
    }

    private fun validatePasswords(): Boolean {
        val password = passwordInput.text.toString()
        val passwordConformation = passwordInputCnf.text.toString()
        val isValid = (password == passwordConformation)
        return isValid && password.isNotEmpty()
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun validatePin(pin: String) = validatePinOrPassword(
            pinOrPassword = pin,
            isPin = true
    )

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun validatePassword(password: String) = validatePinOrPassword(
            pinOrPassword = password,
            isPin = false
    )

    private fun validatePinOrPassword(pinOrPassword: String, isPin: Boolean): List<PasswordValidationError> {
        val devicePolicyManager = context.getSystemService(DevicePolicyManager::class.java)!!
        val adminMetrics = devicePolicyManager.getPasswordMinimumMetrics(context.userId)
        val complexity = devicePolicyManager.passwordComplexity
        val pinBytes = pinOrPassword.toByteArray()
        return PasswordMetrics.validatePassword(
                adminMetrics,
                complexity,
                isPin,
                pinBytes
        )
    }

}
package com.documate.settings

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service

/**
 * Stores and retrieves the Gemini API key using IntelliJ's PasswordSafe,
 * which delegates to the OS keychain (Keychain on macOS, Secret Service on Linux,
 * Credential Manager on Windows). The key is never written to disk in plaintext.
 */
@Service(Service.Level.APP)
class DocuMateSettings {
    private val credentialAttributes = CredentialAttributes(
        generateServiceName("DocuMate", "GeminiApiKey")
    )

    var apiKey: String
        get() = PasswordSafe.instance.getPassword(credentialAttributes) ?: ""
        set(value) {
            val credentials = if (value.isBlank()) null else Credentials("apiKey", value)
            PasswordSafe.instance.set(credentialAttributes, credentials)
        }

    companion object {
        fun getInstance(): DocuMateSettings =
            ApplicationManager.getApplication().getService(DocuMateSettings::class.java)
    }
}
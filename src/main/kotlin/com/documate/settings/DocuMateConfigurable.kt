package com.documate.settings

import com.documate.api.GeminiClient
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.NlsContexts
import com.intellij.util.ui.JBUI
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.Box
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JPasswordField
import javax.swing.SwingUtilities

/**
 * Registers a settings page under Settings -> Tools -> DocuMate.
 * The API key field uses JPasswordField so the value is masked in the UI.
 */

class DocuMateConfigurable : Configurable {
    private var panel: JPanel? = null
    private var apiKeyField: JPasswordField? = null
    private var testButton: JButton? = null

    override fun getDisplayName(): @NlsContexts.ConfigurableName String = "DocuMate"

    override fun createComponent(): JComponent {
        val apiKeyLabel = JLabel("Gemini API Key:")
        val field = JPasswordField(40).also { apiKeyField = it }
        val button = JButton("Test Connection").also { testButton = it }

        button.addActionListener { testConnection() }

        val p = JPanel(GridBagLayout()).also { panel = it }
        val gbc = GridBagConstraints().apply {
            insets = JBUI.insets(4)
            anchor = GridBagConstraints.WEST
        }

        // Row 0: label
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 1
        p.add(apiKeyLabel, gbc)

        // Row 0: field
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0
        p.add(field, gbc)

        // Row 1: test button (right-aligned)
        gbc.gridx = 1; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0
        p.add(button, gbc)

        // Row 2: hint label
        gbc.gridx = 1; gbc.gridy = 2
        p.add(JLabel("<html><small>Get your key at <a href=''>https://aistudio.google.com/</a></small></html>"), gbc)

        // Filler to push everything to the top
        gbc.gridx = 0; gbc.gridy = 3; gbc.weighty = 1.0; gbc.gridwidth = 2
        p.add(Box.createVerticalGlue(), gbc)

        return p
    }

    override fun isModified(): Boolean {
        val current = DocuMateSettings.getInstance().apiKey
        val entered = String(apiKeyField?.password ?: charArrayOf())
        return entered != current
    }

    override fun apply() {
        val entered = String(apiKeyField?.password ?: charArrayOf()).trim()
        DocuMateSettings.getInstance().apiKey = entered
    }

    private fun testConnection() {
        val key = String(apiKeyField?.password ?: charArrayOf()).trim()
        if (key.isBlank()) {
            Messages.showWarningDialog("Please enter an API key first.", "DocuMate")
            return
        }

        testButton?.isEnabled = false
        testButton?.text = "Testing..."

        // Run off EDT to avoid freezing the UI
        Thread {
            val result = GeminiClient.generateDocumentation(key, "fun hello() = println(\"Hello\")")
            SwingUtilities.invokeLater {
                testButton?.isEnabled = true
                testButton?.text = "Test Connection"
                result.fold(
                    onSuccess = { Messages.showInfoMessage("Connection successful! DocuMate is ready.", "DocuMate") },
                    onFailure = { Messages.showErrorDialog("Connection failed:\n${it.message}", "DocuMate") }
                )
            }
        }.start()
    }

    override fun disposeUIResources() {
        panel = null
        apiKeyField = null
        testButton = null
    }
}
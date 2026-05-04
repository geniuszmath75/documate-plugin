package com.documate.actions

import com.documate.api.GeminiClient
import com.documate.inserter.DocumentationInserter
import com.documate.psi.PsiHelper
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.ui.Messages
import com.documate.settings.DocuMateSettings

class GenerateDocAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return
        val psiFile = event.getData(CommonDataKeys.PSI_FILE) ?: return

        // Validate API key before doing anything else
        val apiKey = DocuMateSettings.getInstance().apiKey
        if (apiKey.isBlank()) {
            Messages.showWarningDialog(
                project,
                "No API key configured.\nPlease go to Settings -> Tools -> DocuMate and enter your Gemini API key.",
                "DocuMate: Missing API Key"
            )
            return
        }

        val targetElement = PsiHelper.findTargetElement(psiFile, editor);
        if (targetElement == null) {
            Messages.showInfoMessage(
                project,
                "Place the caret inside or on a function or class to generate documentation.",
                "DocuMate: Nothing to Document"
            )
            return
        }

        val sourceCode = targetElement.text

        // Run network call in background – never block the EDT
        ProgressManager.getInstance()
            .run(object : Task.Backgroundable(project, "DocuMate: Generating documentation...", true) {
                override fun run(indicator: ProgressIndicator) {
                    indicator.isIndeterminate = true

                    val result = GeminiClient.generateDocumentation(apiKey, sourceCode)

                    result.fold(
                        onSuccess = { docComment ->
                            DocumentationInserter.insert(project, editor, psiFile, targetElement, docComment)
                        },
                        onFailure = { error ->
                            // Show error on EDT via invokeLater handled inside Messages
                            ApplicationManager.getApplication().invokeLater {
                                Messages.showErrorDialog(
                                    project,
                                    "Failed to generate documentation:\n${error.message}",
                                    "DocuMate: Error"
                                )
                            }
                        }
                    )
                }
            })
    }
}
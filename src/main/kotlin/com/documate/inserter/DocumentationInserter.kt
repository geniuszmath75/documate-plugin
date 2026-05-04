package com.documate.inserter

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

object DocumentationInserter {
    /**
     * Inserts [docComment] as a line directly above [targetElement].
     * Must be called from any thread - internally dispatches to EDT via WriteCommandAction.
     */
    fun insert(
        project: Project,
        editor: Editor,
        psiFile: PsiFile,
        targetElement: PsiElement,
        docComment: String,
    ) {
        ApplicationManager.getApplication().invokeLater {
            WriteCommandAction.runWriteCommandAction(project, "Generate Documentation", null, {
                val document = editor.document

                val elementStart = targetElement.textRange.startOffset
                val lineNumber = document.getLineNumber(elementStart)
                val lineStart = document.getLineStartOffset(lineNumber)

                // Detect indentation of the target element to align the comment
                val lineText = document.getText(
                    com.intellij.openapi.util.TextRange(lineStart, elementStart)
                )
                val indent = lineText.takeWhile { it == ' ' || it == '\t' }

                val indentedComment = docComment
                    .lines()
                    .joinToString("\n") { "$indent$it" }

                document.insertString(lineStart, "$indentedComment\n")
            }, psiFile)
        }
    }
}
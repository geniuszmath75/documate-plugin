package com.documate.psi

import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

object PsiHelper {

    /**
     * Walks up the PSI tree from the current caret position and returns
     * the nearest enclosing named function or class element.
     *
     * Returns null if the caret is not inside any supported element.
     */
    fun findTargetElement(psiFile: PsiFile, editor: Editor): PsiElement? {
        val offset = editor.caretModel.offset
        val elementAtCaret = psiFile.findElementAt(offset) ?: return null

        return findNearestDocumentableParent(elementAtCaret)
    }

    /**
     * Walks up the PSI tree looking for a named function or class.
     * Uses class names as strings to avoid hard compile-time dependencies
     * on specific language plugins (Kotlin / Java).
     */
    private fun findNearestDocumentableParent(element: PsiElement): PsiElement? {
        var current: PsiElement? = element.parent

        while (current != null) {
            val className = current.javaClass.simpleName

            // Matches KtNamedFunction, KtClass, KtObjectDeclaration (Kotlin)
            // and PsiMethod, PsiClass (Java)
            if (isDocumentable(className)) {
                return current
            }

            current = current.parent
        }

        return null
    }

    private fun isDocumentable(className: String): Boolean = className in DOCUMENTABLE_PSI_TYPES

    private val DOCUMENTABLE_PSI_TYPES = setOf(
        "KtNamedFunction",
        "KtClass",
        "KtObjectDeclaration",
        "PsiMethodImpl",
        "PsiClassImpl",
    )
}
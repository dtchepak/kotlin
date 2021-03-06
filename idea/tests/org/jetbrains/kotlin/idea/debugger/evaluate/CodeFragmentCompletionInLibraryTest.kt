/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.debugger.evaluate

import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.idea.completion.test.AbstractJvmBasicCompletionTest
import org.jetbrains.kotlin.idea.completion.test.testCompletion
import org.jetbrains.kotlin.idea.test.SdkAndMockLibraryProjectDescriptor
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

private val LIBRARY_SRC_PATH = KotlinTestUtils.getHomeDirectory() + "/idea/idea-completion/testData/codeFragmentInLibrarySource/customLibrary/"

class CodeFragmentCompletionInLibraryTest : AbstractJvmBasicCompletionTest() {

    override fun getProjectDescriptor() = object: SdkAndMockLibraryProjectDescriptor(LIBRARY_SRC_PATH, false) {
        override fun configureModule(module: Module, model: ModifiableRootModel) {
            super.configureModule(module, model)

            val library = model.moduleLibraryTable.getLibraryByName(SdkAndMockLibraryProjectDescriptor.LIBRARY_NAME)!!
            val modifiableModel = library.modifiableModel

            modifiableModel.addRoot(findLibrarySourceDir(), OrderRootType.SOURCES)
            modifiableModel.commit()
        }
    }

    fun testCompletionInCustomLibrary() {
        testCompletionInLibraryCodeFragment("<caret>", "EXIST: parameter")
    }

    fun testSecondCompletionInCustomLibrary() {
        testCompletionInLibraryCodeFragment("Ch<caret>", "EXIST: CharRange", "EXIST: Char", "INVOCATION_COUNT: 2")
    }

    fun testExtensionCompletionInCustomLibrary() {
        testCompletionInLibraryCodeFragment("3.extOn<caret>", "EXIST: extOnInt")
    }

    fun testJavaTypesCompletion() {
        testCompletionInLibraryCodeFragment("Hash<caret>", "EXIST: HashMap", "EXIST: HashSet")
    }

    private fun testCompletionInLibraryCodeFragment(fragmentText: String, vararg completionDirectives: String) {
        setupFixtureByCodeFragment(fragmentText)
        val directives = completionDirectives.map { "//$it" }.joinToString(separator = "\n")
        testCompletion(directives, JvmPlatform, { completionType, count -> myFixture.complete(completionType, count) })
    }

    private fun setupFixtureByCodeFragment(fragmentText: String) {
        val sourceFile = findLibrarySourceDir().findChild("customLibrary.kt")!!
        val jetFile = PsiManager.getInstance(project).findFile(sourceFile) as KtFile
        val fooFunctionFromLibrary = jetFile.declarations.first() as KtFunction
        val codeFragment = KtPsiFactory(fooFunctionFromLibrary).createExpressionCodeFragment(
                fragmentText,
                KotlinCodeFragmentFactory.getContextElement(fooFunctionFromLibrary.bodyExpression)
        )
        codeFragment.forceResolveScope(GlobalSearchScope.allScope(project))
        myFixture.configureFromExistingVirtualFile(codeFragment.virtualFile)
    }

    private fun findLibrarySourceDir(): VirtualFile {
        return LocalFileSystem.getInstance().findFileByIoFile(File(LIBRARY_SRC_PATH))!!
    }
}

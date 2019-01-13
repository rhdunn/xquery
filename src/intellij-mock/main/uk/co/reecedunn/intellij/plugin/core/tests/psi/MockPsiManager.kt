/*
 * Copyright (C) 2016-2019 Reece H. Dunn
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
package uk.co.reecedunn.intellij.plugin.core.tests.psi

import com.intellij.lang.LanguageUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import uk.co.reecedunn.intellij.plugin.core.vfs.decode
import java.io.IOException

class MockPsiManager(project: Project) : com.intellij.mock.MockPsiManager(project) {
    override fun findFile(file: VirtualFile): PsiFile? {
        try {
            val language = LanguageUtil.getLanguageForPsi(project, file) ?: return null
            return PsiFileFactory.getInstance(project).createFileFromText(
                file.name, language, file.decode()!!, true, false, false, file
            )
        } catch (e: IOException) {
            return null
        }
    }
}

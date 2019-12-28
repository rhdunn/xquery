/*
 * Copyright (C) 2019 Reece H. Dunn
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
package uk.co.reecedunn.intellij.plugin.xdm.module.loader

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.testFramework.LightVirtualFileBase
import uk.co.reecedunn.intellij.plugin.core.vfs.toPsiFile
import uk.co.reecedunn.intellij.plugin.xdm.context.XdmStaticContext
import uk.co.reecedunn.intellij.plugin.xdm.module.path.XdmModuleLocationPath
import uk.co.reecedunn.intellij.plugin.xdm.module.path.XdmModulePath

object RelativeModuleLoader : XdmModuleLoader, XdmModuleLoaderFactory {
    // region XdmModuleLoader

    private fun findFileByPath(path: String, root: VirtualFile): VirtualFile? {
        if (root is LightVirtualFileBase) {
            return root.originalFile?.parent?.findFileByRelativePath(path)
        }
        return root.parent.findFileByRelativePath(path)
    }

    override fun resolve(path: XdmModulePath, context: PsiElement): PsiElement? {
        return when (path) {
            is XdmModuleLocationPath -> {
                findFileByPath(path.path, context.containingFile.virtualFile)?.toPsiFile(path.project)
            }
            else -> null
        }
    }

    override fun context(path: XdmModulePath, context: PsiElement): XdmStaticContext? {
        return when (path) {
            is XdmModuleLocationPath -> resolve(path, context) as? XdmStaticContext
            else -> null
        }
    }

    // endregion
    // region XdmModuleLoaderFactory

    override fun loader(context: String?): XdmModuleLoader? = this

    // endregion
}
/*
 * Copyright (C) 2019-2020 Reece H. Dunn
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
package uk.co.reecedunn.intellij.plugin.xpm.module.loader.impl

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import uk.co.reecedunn.intellij.plugin.core.vfs.originalFile
import uk.co.reecedunn.intellij.plugin.core.vfs.relativePathTo
import uk.co.reecedunn.intellij.plugin.core.vfs.toPsiFile
import uk.co.reecedunn.intellij.plugin.xdm.module.path.XdmModuleType
import uk.co.reecedunn.intellij.plugin.xpm.context.XpmStaticContext
import uk.co.reecedunn.intellij.plugin.xpm.module.loader.XpmModuleLoader
import uk.co.reecedunn.intellij.plugin.xpm.module.loader.XpmModuleLoaderFactory
import uk.co.reecedunn.intellij.plugin.xpm.module.path.XpmModulePath
import uk.co.reecedunn.intellij.plugin.xpm.module.path.impl.XpmModuleLocationPath

class FixedModuleLoader(val root: VirtualFile) : XpmModuleLoader {
    // region XpmModuleLoader

    private fun findFileByPath(path: String, moduleTypes: Array<XdmModuleType>?): VirtualFile? {
        moduleTypes?.forEach { type ->
            type.extensions.forEach { extension ->
                val file = root.findFileByRelativePath("$path$extension")
                if (file != null) return file
            }
        }
        return root.findFileByRelativePath(path)
    }

    override fun resolve(path: XpmModulePath, context: VirtualFile?): PsiElement? = when (path) {
        is XpmModuleLocationPath -> {
            if (path.isResource == null) // BaseX reverse domain name module path
                findFileByPath(path.path, path.moduleTypes)?.toPsiFile(path.project)
            else
                findFileByPath(path.path, null)?.toPsiFile(path.project)
        }
        else -> null
    }

    override fun context(path: XpmModulePath, context: VirtualFile?): XpmStaticContext? = when (path) {
        is XpmModuleLocationPath -> resolve(path, context) as? XpmStaticContext
        else -> null
    }

    override fun relativePathTo(file: VirtualFile, project: Project): String? = root.relativePathTo(file)

    // endregion
    // region XpmModuleLoaderFactory

    companion object : XpmModuleLoaderFactory {
        override fun loader(context: String?): XpmModuleLoader? {
            return context?.let {
                LocalFileSystem.getInstance().findFileByPath(context)?.let { loader(it) }
            }
        }

        fun loader(file: VirtualFile): XpmModuleLoader = FixedModuleLoader(file.originalFile)
    }

    // endregion
}

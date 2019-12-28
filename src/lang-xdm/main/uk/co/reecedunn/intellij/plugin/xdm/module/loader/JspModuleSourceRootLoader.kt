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

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.jps.model.java.JavaSourceRootType
import org.jetbrains.jps.model.module.JpsModuleSourceRootType
import uk.co.reecedunn.intellij.plugin.core.roots.getSourceRootType
import uk.co.reecedunn.intellij.plugin.core.roots.sourceFolders
import uk.co.reecedunn.intellij.plugin.core.vfs.toPsiFile
import uk.co.reecedunn.intellij.plugin.xdm.context.XdmStaticContext
import uk.co.reecedunn.intellij.plugin.xdm.module.path.XdmModuleLocationPath
import uk.co.reecedunn.intellij.plugin.xdm.module.path.XdmModulePath
import uk.co.reecedunn.intellij.plugin.xdm.module.path.XdmModuleType

class JspModuleSourceRootLoader(private val rootType: JpsModuleSourceRootType<*>) : XdmModuleLoader {
    // region XdmModuleLoader

    private fun findFileByPath(root: VirtualFile, path: String, moduleTypes: Array<XdmModuleType>): VirtualFile? {
        moduleTypes.forEach { type ->
            type.extensions.forEach { extension ->
                val file = root.findFileByRelativePath("$path$extension")
                if (file != null) return file
            }
        }
        return root.findFileByRelativePath(path)
    }

    private fun findFileByPath(project: Project, path: String, moduleTypes: Array<XdmModuleType>): VirtualFile? {
        return project.sourceFolders(rootType).map { folder ->
            folder.file?.let { findFileByPath(it, path, moduleTypes) }
        }.filterNotNull().firstOrNull()
    }

    override fun resolve(path: XdmModulePath, context: PsiElement): PsiElement? {
        return when (path) {
            is XdmModuleLocationPath -> {
                if (rootType === JavaSourceRootType.SOURCE || rootType === context.getSourceRootType(path.project))
                    findFileByPath(path.project, path.path, path.moduleTypes)?.toPsiFile<PsiFile>(path.project)
                else
                    null
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

    companion object : XdmModuleLoaderFactory {
        override fun loader(context: String?): XdmModuleLoader? {
            return when (context) {
                "java:source" -> JspModuleSourceRootLoader(JavaSourceRootType.SOURCE)
                "java:test-source" -> JspModuleSourceRootLoader(JavaSourceRootType.TEST_SOURCE)
                else -> null
            }
        }
    }

    // endregion
}
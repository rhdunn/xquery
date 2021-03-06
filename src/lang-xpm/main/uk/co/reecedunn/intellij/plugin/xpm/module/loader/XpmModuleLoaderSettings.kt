/*
 * Copyright (C) 2019-2021 Reece H. Dunn
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
package uk.co.reecedunn.intellij.plugin.xpm.module.loader

import com.intellij.javaee.ExternalResourceManagerEx
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.util.xmlb.XmlSerializerUtil
import uk.co.reecedunn.intellij.plugin.core.progress.TaskManager
import uk.co.reecedunn.intellij.plugin.core.util.UserDataHolderBase
import uk.co.reecedunn.intellij.plugin.xdm.types.XsAnyUriValue
import uk.co.reecedunn.intellij.plugin.xdm.types.element
import uk.co.reecedunn.intellij.plugin.xpm.context.XpmStaticContext
import uk.co.reecedunn.intellij.plugin.xpm.lang.XpmVendorType
import uk.co.reecedunn.intellij.plugin.xpm.module.path.XpmModulePath
import uk.co.reecedunn.intellij.plugin.xpm.module.path.paths
import uk.co.reecedunn.intellij.plugin.xpm.resources.XpmBundle

data class XpmModuleLoaderSettingsData(
    var databasePath: String = "",
    var registerSchemaFile: Boolean = true
)

@State(name = "XIJPModuleLoaderSettings", storages = [Storage(StoragePathMacros.WORKSPACE_FILE)])
class XpmModuleLoaderSettings(val project: Project) :
    UserDataHolderBase(),
    XpmModuleLoader,
    PersistentStateComponent<XpmModuleLoaderSettingsData> {
    companion object {
        private val LOADERS = Key.create<List<XpmModuleLoader>>("LOADERS")

        fun getInstance(project: Project): XpmModuleLoaderSettings {
            return project.getService(XpmModuleLoaderSettings::class.java)
        }
    }
    // region Settings :: Module Loaders

    private val loaderBeans: List<XpmModuleLoaderBean> = arrayListOf(
        XpmModuleLoaderBean("java", null),
        XpmModuleLoaderBean("module", "java:source"),
        XpmModuleLoaderBean("module", "java:test-source"),
        XpmModuleLoaderBean("project", null),
        XpmModuleLoaderBean("relative", null)
    )

    private val loaders: Sequence<XpmModuleLoader>
        get() = computeUserDataIfAbsent(LOADERS) {
            loaderBeans.mapNotNull { it.loader }
        }.asSequence()

    private fun registerModulePath(path: String) {
        if (loaderBeans.find { it.name == "fixed" && it.context == path } == null) {
            (loaderBeans as ArrayList).add(XpmModuleLoaderBean("fixed", path))
            clearUserData(LOADERS)
        }
    }

    private fun unregisterModulePath(path: String) {
        loaderBeans.find { it.name == "fixed" && it.context == path }?.let {
            (loaderBeans as ArrayList).remove(it)
            clearUserData(LOADERS)
        }
    }

    // endregion
    // region Settings :: Schema Files

    var registerSchemaFile: Boolean
        get() = data.registerSchemaFile
        set(value) {
            data.registerSchemaFile = value
        }

    private class SchemaFileRegistration {
        var unregisterVendor: XpmVendorType? = null
        var unregisterDatabasePath: String? = null

        var registerVendor: XpmVendorType? = null
        var registerDatabasePath: String? = null
    }

    private val tasks = TaskManager<SchemaFileRegistration>()

    private fun registerSchemaFiles(settings: SchemaFileRegistration): Boolean {
        return tasks.backgroundable(XpmBundle.message("task.schema-file-registration.title"), settings) {
            runInEdt {
                ApplicationManager.getApplication().runWriteAction {
                    val manager = ExternalResourceManagerEx.getInstanceEx()
                    settings.unregisterVendor?.schemaFiles(settings.unregisterDatabasePath!!)
                        ?.forEach { (_, targetNamespace) ->
                            manager.removeResource(targetNamespace, project)
                        }
                    settings.registerVendor?.schemaFiles(settings.registerDatabasePath!!)
                        ?.forEach { (location, targetNamespace) ->
                            manager.addResource(targetNamespace, location, project)
                        }
                }
            }
        }
    }

    // endregion
    // region Settings :: Database Path

    private fun registerDatabasePath(vendor: XpmVendorType, databasePath: String, settings: SchemaFileRegistration) {
        // Add the module path to the list of module loaders.
        vendor.modulePath?.let { registerModulePath("$databasePath$it") }

        // Defer registering schema files to avoid blocking the settings page.
        if (data.registerSchemaFile) {
            settings.registerVendor = vendor
            settings.registerDatabasePath = databasePath
        }
    }

    private fun unregisterDatabasePath(vendor: XpmVendorType, databasePath: String, settings: SchemaFileRegistration) {
        // Remove the module path to the list of module loaders.
        vendor.modulePath?.let { unregisterModulePath("$databasePath$it") }

        // Defer unregistering schema files to avoid blocking the settings page.
        if (data.registerSchemaFile) {
            settings.unregisterVendor = vendor
            settings.unregisterDatabasePath = databasePath
        }
    }

    var databasePath: String
        get() = data.databasePath
        set(value) {
            if (value != data.databasePath) {
                val settings = SchemaFileRegistration()
                vendor?.let { unregisterDatabasePath(it, data.databasePath, settings) }

                data.databasePath = value

                vendor?.let { registerDatabasePath(it, data.databasePath, settings) }
                registerSchemaFiles(settings)
            }
        }

    val vendor: XpmVendorType?
        get() = XpmVendorType.types.find { it.isValidInstallDir(databasePath) }

    // endregion
    // region XpmModuleLoader

    override fun resolve(path: XpmModulePath, context: VirtualFile?): PsiElement? {
        return loaders.mapNotNull { it.resolve(path, context) }.firstOrNull()
    }

    override fun context(path: XpmModulePath, context: VirtualFile?): XpmStaticContext? {
        return loaders.mapNotNull { it.context(path, context) }.firstOrNull()
    }

    override fun relativePathTo(file: VirtualFile, project: Project): String? {
        return loaders.mapNotNull { it.relativePathTo(file, project) }.firstOrNull()
    }

    // endregion
    // region PersistentStateComponent

    private val data = XpmModuleLoaderSettingsData()

    override fun getState(): XpmModuleLoaderSettingsData = data

    override fun loadState(state: XpmModuleLoaderSettingsData) {
        XmlSerializerUtil.copyBean(state, data)

        // Add the module path to the list of module loaders.
        vendor?.modulePath?.let { registerModulePath("$databasePath$it") }
    }

    // endregion
}

fun XsAnyUriValue.resolve(): PsiElement? = element?.let { this.resolve(it) }

fun XsAnyUriValue.resolve(element: PsiElement): PsiElement? {
    return resolve(element.project, element.containingFile?.virtualFile)
}

fun XsAnyUriValue.resolve(project: Project, file: VirtualFile?): PsiElement? {
    val loaders = XpmModuleLoaderSettings.getInstance(project)
    return paths(project).mapNotNull { loaders.resolve(it, file) }.firstOrNull()
}

fun XsAnyUriValue.context(): XpmStaticContext? = element?.let { this.context(it) }

fun XsAnyUriValue.context(element: PsiElement): XpmStaticContext? {
    return context(element.project, element.containingFile?.virtualFile)
}

fun XsAnyUriValue.context(project: Project, file: VirtualFile?): XpmStaticContext? {
    val loaders = XpmModuleLoaderSettings.getInstance(project)
    return paths(project).mapNotNull { loaders.context(it, file) }.firstOrNull()
}

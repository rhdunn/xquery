/*
 * Copyright (C) 2020 Reece H. Dunn
 * Copyright 2000-2020 JetBrains s.r.o.
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
package com.intellij.compat.testFramework

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.ComponentManager
import com.intellij.openapi.extensions.BaseExtensionPointName
import com.intellij.openapi.extensions.ExtensionPoint
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.util.Disposer
import org.jetbrains.annotations.TestOnly

@TestOnly
fun <T : Any> ComponentManager.registerExtensionPointBean(
    name: ExtensionPointName<*>,
    aClass: Class<T>,
    parentDisposable: Disposable
) {
    if (!extensionArea.hasExtensionPoint(name)) {
        extensionArea.registerExtensionPoint(name.name, aClass.name, ExtensionPoint.Kind.BEAN_CLASS)
        Disposer.register(parentDisposable, Disposable {
            extensionArea.unregisterExtensionPoint(name.name)
        })
    }
}

@TestOnly
fun <T : Any> ComponentManager.registerExtension(
    name: BaseExtensionPointName<*>,
    instance: T,
    parentDisposable: Disposable
) {
    extensionArea.getExtensionPoint<T>(name.name).registerExtension(instance, parentDisposable)
}
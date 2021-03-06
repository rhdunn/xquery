/*
 * Copyright (C) 2020 Reece H. Dunn
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
package uk.co.reecedunn.intellij.plugin.xslt.ast.saxon

import com.intellij.psi.PsiElement

/**
 * A Saxon 9.9 `saxon:rename` vendor extension node in the XSLT AST.
 *
 * Reference: https://saxonica.com/documentation/index.html#!extensions/instructions/update
 */
interface SaxonRename : PsiElement

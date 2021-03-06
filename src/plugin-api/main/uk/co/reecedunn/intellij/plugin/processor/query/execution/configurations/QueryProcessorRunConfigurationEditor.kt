/*
 * Copyright (C) 2018-2021 Reece H. Dunn
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
package uk.co.reecedunn.intellij.plugin.processor.query.execution.configurations

import com.intellij.lang.Language
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.util.text.nullize
import uk.co.reecedunn.intellij.plugin.core.fileChooser.FileNameMatcherDescriptor
import uk.co.reecedunn.intellij.plugin.core.lang.*
import uk.co.reecedunn.intellij.plugin.core.ui.layout.*
import uk.co.reecedunn.intellij.plugin.intellij.lang.XPathSubset
import uk.co.reecedunn.intellij.plugin.processor.query.QueryServer
import uk.co.reecedunn.intellij.plugin.processor.query.execution.configurations.rdf.RDF_FORMATS
import uk.co.reecedunn.intellij.plugin.processor.query.execution.ui.QueryProcessorComboBox
import uk.co.reecedunn.intellij.plugin.processor.query.execution.ui.QueryServerComboBoxModel
import uk.co.reecedunn.intellij.plugin.processor.resources.PluginApiBundle
import javax.swing.*

class QueryProcessorRunConfigurationEditor(private val project: Project, private vararg val languages: Language) :
    SettingsEditor<QueryProcessorRunConfiguration>() {
    // region "Query" Page

    private lateinit var queryProcessor: QueryProcessorComboBox
    private lateinit var server: JComboBox<String>
    private lateinit var modulePath: TextFieldWithBrowseButton
    private lateinit var database: JComboBox<String>

    private lateinit var rdfOutputFormat: JComboBox<Language>
    private lateinit var scriptFile: QueryProcessorDataSource
    private lateinit var xpathSubset: JComboBox<XPathSubset>
    private lateinit var updating: JCheckBox

    private lateinit var xpathSubsetLabel: JLabel

    @Suppress("DuplicatedCode")
    private val queryPanel: JPanel = panel {
        row {
            label(PluginApiBundle.message("xquery.configurations.processor.query-processor.label"), column.vgap())
            queryProcessor = QueryProcessorComboBox(project)
            add(queryProcessor.component, column.spanCols().horizontal().hgap().vgap())
            queryProcessor.addActionListener {
                updateUI(false)
                queryProcessor.servers(refresh = true) { queryServers ->
                    val databases = queryServers.map { it.database }.distinct()
                    (database.model as QueryServerComboBoxModel).update(databases)
                }
            }
        }
        row {
            label(PluginApiBundle.message("xquery.configurations.processor.content-database.label"), column.vgap())
            database = comboBox(column.horizontal().hgap().vgap()) {
                model = QueryServerComboBoxModel()
                addActionListener {
                    val database = database.selectedItem as? String? ?: QueryServer.NONE
                    queryProcessor.servers { queryServers ->
                        val servers = queryServers.asSequence().filter { it.database == database }.map { it.server }
                        (server.model as QueryServerComboBoxModel).update(servers.toList())
                    }
                }
            }
            label(PluginApiBundle.message("xquery.configurations.processor.server.label"), column.hgap().vgap())
            server = comboBox(column.horizontal().hgap().vgap()) {
                model = QueryServerComboBoxModel()
            }
        }
        row {
            label(PluginApiBundle.message("xquery.configurations.processor.module-root.label"), column.vgap())
            modulePath = textFieldWithBrowseButton(column.spanCols().horizontal().hgap().vgap()) {
                val descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
                descriptor.title = PluginApiBundle.message("browser.choose.module-path")
                addBrowseFolderListener(null, null, project, descriptor)
            }
        }
        row {
            label(
                PluginApiBundle.message("xquery.configurations.processor.run-query-from.label"),
                column.spanCols().vgap(6, LayoutPosition.Both)
            )
        }
        scriptFile = queryProcessorDataSource {
            val descriptor = FileNameMatcherDescriptor(languages.getAssociations())
            descriptor.title = PluginApiBundle.message("browser.choose.script-file")
            addBrowseFolderListener(null, null, project, descriptor)
            addActionListener {
                if (languages[0].getLanguageMimeTypes()[0] == "application/sparql-query") {
                    updateUI(true)
                }
            }
        }
        row {
            xpathSubsetLabel = label(
                PluginApiBundle.message("xquery.configurations.processor.xpath-subset.label"), column.vgap()
            )
            xpathSubset = comboBox(column.spanCols().horizontal().hgap().vgap()) {
                renderer = coloredListCellRenderer { _, value, _, _, _ ->
                    clear()
                    value?.let { append(it.displayName) }
                }
                addItem(XPathSubset.XPath)
                addItem(XPathSubset.XsltPattern)
            }
        }
        row {
            updating = checkBox(column.spanCols()) {
                text = PluginApiBundle.message("xquery.configurations.processor.updating.label")
            }
        }
        row {
            spacer(column.vertical())
        }
    }

    // endregion
    // region "Context Item"/"Input" Page

    private lateinit var contextItem: QueryProcessorDataSource

    private val inputLabel: String?
        get() = when {
            languages.findByMimeType { it == "application/xslt+xml" } != null -> {
                // Use "Input" instead of "Context Item" for XSLT queries.
                PluginApiBundle.message("xquery.configurations.processor.group.input.label")
            }
            languages.findByMimeType { it == "application/xquery" || it == "application/vnd+xpath" } == null -> {
                // Server-side JS, SPARQL, and SQL queries don't support an input/context item;
                // XSLT, XQuery, and XPath do.
                null
            }
            else -> PluginApiBundle.message("xquery.configurations.processor.group.context-item.label")
        }

    private val inputPanel: JPanel = panel {
        contextItem = queryProcessorDataSource(allowUnspecified = true) {
            val descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor()
            descriptor.title = PluginApiBundle.message("browser.choose.context-item")
            addBrowseFolderListener(null, null, project, descriptor)
        }
        row {
            spacer(column.vertical())
        }
    }

    // endregion
    // region "Output" Page

    private lateinit var reformatResults: JCheckBox

    private val outputPanel: JPanel = panel {
        row {
            label(PluginApiBundle.message("xquery.configurations.processor.rdf-format.label"), column.vgap())
            rdfOutputFormat = comboBox(column.horizontal().hgap().vgap()) {
                renderer = LanguageCellRenderer()
                addItem(null)
                RDF_FORMATS.forEach { addItem(it) }
            }
        }
        row {
            reformatResults = checkBox(column.horizontal().spanCols()) {
                text = PluginApiBundle.message("xquery.configurations.processor.reformat-results.label")
            }
        }
        row {
            spacer(column.vertical())
            spacer(column.horizontal())
        }
    }

    // endregion
    // region Form

    private fun configureUI() {
        if (languages.findByMimeType { it == "application/vnd+xpath" } == null) {
            xpathSubsetLabel.isVisible = false
            xpathSubset.isVisible = false
        }
    }

    private fun updateUI(isSparql: Boolean) {
        val processor = queryProcessor.settings
        rdfOutputFormat.isEnabled = processor?.api?.canOutputRdf(null) == true
        updating.isEnabled = processor?.api?.canUpdate(languages[0]) == true

        if (isSparql) {
            val path = scriptFile.path ?: ""
            val lang = languages.findByAssociations(path) ?: languages[0]
            updating.isSelected = !lang.getLanguageMimeTypes().contains("application/sparql-query")
        }
    }

    private val panel: JPanel = tabbedPanel {
        tab(PluginApiBundle.message("xquery.configurations.processor.group.query.label"), queryPanel)
        inputLabel?.let { tab(it, inputPanel) }
        tab(PluginApiBundle.message("xquery.configurations.processor.group.output.label"), outputPanel)
    }

    // endregion
    // region SettingsEditor

    override fun createEditor(): JComponent = panel

    override fun resetEditorFrom(configuration: QueryProcessorRunConfiguration) {
        queryProcessor.processorId = configuration.processorId
        rdfOutputFormat.selectedItem = configuration.rdfOutputFormat
        (database.model as QueryServerComboBoxModel).defaultSelection = configuration.database
        (server.model as QueryServerComboBoxModel).defaultSelection = configuration.server
        modulePath.textField.text = configuration.modulePath ?: ""
        scriptFile.type = configuration.scriptSource
        scriptFile.path = configuration.scriptFilePath
        updating.isSelected = configuration.updating
        xpathSubset.selectedItem = configuration.xpathSubset
        contextItem.type = configuration.contextItemSource
        contextItem.path = configuration.contextItemValue
        reformatResults.isSelected = configuration.reformatResults

        configureUI()
        updateUI(languages.findByMimeType { it == "application/sparql-query" } != null)
    }

    override fun applyEditorTo(configuration: QueryProcessorRunConfiguration) {
        configuration.processorId = queryProcessor.processorId
        configuration.rdfOutputFormat = rdfOutputFormat.selectedItem as? Language
        configuration.database = database.selectedItem as? String
        configuration.server = server.selectedItem as? String
        configuration.modulePath = modulePath.textField.text.nullize()
        configuration.scriptSource = scriptFile.type!!
        configuration.scriptFilePath = scriptFile.path
        configuration.updating = updating.isSelected
        configuration.xpathSubset = xpathSubset.selectedItem as XPathSubset
        configuration.contextItemSource = contextItem.type
        configuration.contextItemValue = contextItem.path
        configuration.reformatResults = reformatResults.isSelected
    }

    // endregion
}

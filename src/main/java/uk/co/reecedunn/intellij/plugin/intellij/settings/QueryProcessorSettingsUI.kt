package uk.co.reecedunn.intellij.plugin.intellij.settings

import com.intellij.openapi.fileChooser.FileTypeDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.ComponentWithBrowseButton
import com.intellij.openapi.ui.TextComponentAccessor
import com.intellij.ui.ColoredListCellRenderer
import uk.co.reecedunn.intellij.plugin.core.ui.SettingsUI
import uk.co.reecedunn.intellij.plugin.intellij.resources.XQueryBundle
import uk.co.reecedunn.intellij.plugin.processor.query.ConnectionSettings
import uk.co.reecedunn.intellij.plugin.processor.query.QUERY_PROCESSOR_APIS
import uk.co.reecedunn.intellij.plugin.processor.query.QueryProcessorApi
import uk.co.reecedunn.intellij.plugin.processor.query.QueryProcessorSettings
import javax.swing.*

class QueryProcessorSettingsUI(private val project: Project) : SettingsUI<QueryProcessorSettings> {
    // region Processor APIs

    private var api: JComboBox<QueryProcessorApi>? = null

    private fun createQueryProcessorApiUI() {
        api = ComboBox()
        api!!.renderer = object : ColoredListCellRenderer<QueryProcessorApi>() {
            override fun customizeCellRenderer(
                list: JList<out QueryProcessorApi>,
                value: QueryProcessorApi?,
                index: Int, selected: Boolean, hasFocus: Boolean
            ) {
                if (value != null) {
                    append(value.displayName)
                }
            }
        }

        QUERY_PROCESSOR_APIS.forEach { value -> api!!.addItem(value) }

        api!!.addActionListener { _ ->
            val selection = api!!.selectedItem as QueryProcessorApi
            jar!!.isEnabled = selection.requireJar
        }
    }

    // endregion
    // region Processor JAR

    private var jar: ComponentWithBrowseButton<JTextField>? = null

    private fun createJarUI() {
        jar = ComponentWithBrowseButton(JTextField(), null)
        jar!!.addBrowseFolderListener(
            XQueryBundle.message("browser.choose.implementation-api-jar"), null,
            project,
            FileTypeDescriptor(XQueryBundle.message("browser.choose.implementation-api-jar"), "jar"),
            TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT
        )
    }

    // endregion
    // region Standalone

    private var standalone: JCheckBox? = null

    private fun createStandaloneUI() {
        standalone = JCheckBox()
        standalone!!.addActionListener { action ->
            val serverEnabled = !standalone!!.isSelected
            hostname!!.isEnabled = serverEnabled
            serverPort!!.isEnabled = serverEnabled
            username!!.isEnabled = serverEnabled
            password!!.isEnabled = serverEnabled
        }
    }

    // endregion
    // region Form

    private var name: JTextField? = null
    private var hostname: JTextField? = null
    private var serverPort: JTextField? = null
    private var username: JTextField? = null
    private var password: JPasswordField? = null

    private fun createUIComponents() {
        name = JTextField()
        hostname = JTextField()
        serverPort = JTextField()
        username = JTextField()
        password = JPasswordField()
        createQueryProcessorApiUI()
        createJarUI()
        createStandaloneUI()
    }

    // endregion
    // region SettingsUI

    override var panel: JPanel? = null

    override fun isModified(configuration: QueryProcessorSettings): Boolean {
        return false
    }

    override fun reset(configuration: QueryProcessorSettings) {
        name!!.text = configuration.name
        api!!.selectedItem = configuration.api
        jar!!.childComponent.text = configuration.jar
        if (configuration.connection != null) {
            standalone!!.isSelected = true
            hostname!!.text = configuration.connection!!.hostname
            serverPort!!.text = configuration.connection!!.port.toString()
            username!!.text = configuration.connection!!.username
            password!!.text = configuration.connection!!.password
        } else {
            standalone!!.isSelected = false
            hostname!!.text = ""
            serverPort!!.text = "0"
            username!!.text = ""
            password!!.text = ""
        }
    }

    override fun apply(configuration: QueryProcessorSettings) {
        configuration.name = name!!.text.let { if (it.isEmpty()) null else it }
        configuration.api = api!!.selectedItem as QueryProcessorApi
        configuration.jar = jar!!.childComponent.text.let { if (it.isEmpty()) null else it }
        if (standalone!!.isSelected) {
            val port = serverPort!!.text.toInt()
            val user = username!!.text?.let { if (it.isEmpty()) null else it }
            val pass = password!!.password?.let { if (it.isEmpty()) null else it }
            configuration.connection = ConnectionSettings(hostname!!.text, port, user, pass?.toString())
        } else {
            configuration.connection = null
        }
    }

    // endregion
}

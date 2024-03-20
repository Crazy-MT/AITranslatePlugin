package com.crazymt.aitranslate

import AppSettingsComponent
import com.crazymt.aitranslate.net.*
import com.intellij.openapi.options.Configurable
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.Nullable
import javax.swing.JComponent

/**
 * Provides controller functionality for application settings.
 */
internal class AppSettingsConfigurable : Configurable {
    private var mySettingsComponent: AppSettingsComponent? = null

    @Nls(capitalization = Nls.Capitalization.Title)
    override fun getDisplayName(): String {
        return "AITranslate"
    }

    @Nullable
    override fun createComponent(): JComponent? {
        mySettingsComponent = AppSettingsComponent()
        return mySettingsComponent?.panel
    }
    ///判断是否调用apply
    override fun isModified(): Boolean {
        val sourceType = LocalData.read("sourceType")
        val ollamaURL = LocalData.read("ollamaURL")
        val modelName = LocalData.read("modelName")
        val geminiAPIkey = LocalData.read("geminiAPIKey")

        var modified: Boolean = !mySettingsComponent?.sourceType.equals(sourceType)

        modified = modified
                || !mySettingsComponent?.ollamaURL.equals(ollamaURL)
                || !mySettingsComponent?.modelName.equals(modelName)
                || !mySettingsComponent?.geminiAPIKey.equals(geminiAPIkey)
        return modified
    }
    ///保存设置
    override fun apply() {
        var _sourceType = mySettingsComponent?.sourceType ?: SourceGemini

        val _ollamaURL = mySettingsComponent?.ollamaURL ?: ""
        val _modelName = mySettingsComponent?.modelName ?: ""
        val _geminiAPIKey = mySettingsComponent?.geminiAPIKey ?: ""

        LocalData.store("sourceType", _sourceType)
        LocalData.store("ollamaURL", _ollamaURL)
        LocalData.store("modelName", _modelName)
        LocalData.store("geminiAPIKey", _geminiAPIKey)
        sourceType = _sourceType
        ollamaURL = _ollamaURL
        modelName = _modelName
        geminiAPIKey = _geminiAPIKey
    }

    override fun reset() {
        val sourceType = LocalData.read("sourceType")
        val ollamaURL = LocalData.read("ollamaURL")
        val modelName = LocalData.read("modelName")
        val geminiAPIKey = LocalData.read("geminiAPIKey")

        mySettingsComponent?.let {
            it.sourceType = sourceType ?: ""
            it.ollamaURL = ollamaURL ?: ""
            it.modelName = modelName ?: ""
            it.geminiAPIKey = geminiAPIKey ?: ""
        }
    }

    override fun disposeUIResources() {
        mySettingsComponent = null
    }
}

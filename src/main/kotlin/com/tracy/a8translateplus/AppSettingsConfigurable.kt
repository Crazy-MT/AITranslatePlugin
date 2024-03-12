package com.tracy.a8translateplus

import AppSettingsComponent
import com.intellij.openapi.options.Configurable
import com.tracy.a8translateplus.net.*
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
        val baiduAppId = LocalData.read("baiduAppId")
        val baiduSecret = LocalData.read("baiduSecret")
        val ollamaURL = LocalData.read("ollamaURL")
        val modelName = LocalData.read("modelName")

        var modified: Boolean = !mySettingsComponent?.sourceType.equals(sourceType)
        modified = modified
                || !mySettingsComponent?.baiduAppId.equals(baiduAppId) || !mySettingsComponent?.baiduSecret.equals(baiduSecret)
                || !mySettingsComponent?.ollamaURL.equals(ollamaURL) || !mySettingsComponent?.modelName.equals(modelName)
        return modified
    }
    ///保存设置
    override fun apply() {
        println("apply settings")
        var _sourceType = mySettingsComponent?.sourceType ?: SourceNiu
        val _baiduAppId = mySettingsComponent?.baiduAppId ?: ""
        val _baiduSecret = mySettingsComponent?.baiduSecret ?: ""

        val _ollamaURL = mySettingsComponent?.ollamaURL ?: ""
        val _modelName = mySettingsComponent?.modelName ?: ""

        if (_sourceType == SourceBaidu) {
            if (_baiduAppId.isNullOrEmpty() || _baiduSecret.isNullOrEmpty()) {
                _sourceType = SourceNiu
                println("选择百度翻译，百度appId和secret不能为空！")
            }
        }
        LocalData.store("sourceType", _sourceType)
        LocalData.store("baiduAppId", _baiduAppId)
        LocalData.store("baiduSecret", _baiduSecret)
        LocalData.store("ollamaURL", _ollamaURL)
        LocalData.store("modelName", _modelName)
        sourceType = _sourceType
        baiduAppId = _baiduAppId
        baiduSecret = _baiduSecret
        ollamaURL = _ollamaURL
        modelName = _modelName
    }

    override fun reset() {
        val sourceType = LocalData.read("sourceType")
        val baiduAppId = LocalData.read("baiduAppId")
        val baiduSecret = LocalData.read("baiduSecret")
        val ollamaURL = LocalData.read("ollamaURL")
        val modelName = LocalData.read("modelName")

        mySettingsComponent?.let {
            it.sourceType = sourceType ?: ""
            it.baiduAppId = baiduAppId ?: ""
            it.baiduSecret = baiduSecret ?: ""
            it.ollamaURL = ollamaURL ?: ""
            it.modelName = modelName ?: ""
        }
    }

    override fun disposeUIResources() {
        mySettingsComponent = null
    }
}

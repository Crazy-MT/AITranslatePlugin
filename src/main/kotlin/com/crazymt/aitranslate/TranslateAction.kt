package com.crazymt.aitranslate

import com.crazymt.aitranslate.bean.ModelResult
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.crazymt.aitranslate.net.NetCallback
import com.crazymt.aitranslate.net.requestNetData

class TranslateAction : AnAction() {

    var balloon: Balloon? = null

    init {
        createPopup()
    }

    private fun createPopup() {
        balloon = JBPopupFactory.getInstance()
                .createHtmlTextBalloonBuilder("加载中，稍安勿躁～", MessageType.INFO, null)
                .setFadeoutTime(15000)
                .setHideOnAction(true)
                .createBalloon()
    }

    private lateinit var editor: Editor
    private var latestClickTime = 0L
    override fun actionPerformed(e: AnActionEvent) {
        if (!isFastClick(1000)) {
            editor = e.getData(PlatformDataKeys.EDITOR) ?: return

            // 获取选择模式对象
            val model = editor.selectionModel
            val fileExtension = FileDocumentManager.getInstance().getFile(editor.document)?.extension

            // 选中文字
            val selectedText = model.selectedText ?: return
            if (selectedText.isBlank()) return
            if (selectedText.length > 5000) {
                showPopupWindow("翻译最大字符不能超过5000")
                return
            }

            if (balloon == null || balloon?.isDisposed == true) {
                createPopup()
            }
            balloon?.show(JBPopupFactory.getInstance().guessBestPopupLocation(editor), Balloon.Position.below)

            Thread{
                /* 第二步 ---> API查询 */
                requestNetData(fileExtension, selectedText, object : NetCallback<ModelResult> {
                    override fun onSuccess(data: ModelResult) {
                        println(data.toString())
                        val text: String
                        if (data.result != null) {
                            if (selectedText.length < 50) {
                                text = "$selectedText:\n${data.result}"
                            } else {
                                text = "段落翻译：\n${data.result}"
                            }
                        } else {
                            text = "error:${data.error}"
                        }
                        showPopupWindow(text)
                    }

                    override fun onFail(message: String) = showPopupWindow("error:$message")

                    override fun onError(error: String) = showPopupWindow("error:$error")
                })
            }.start()
        }
    }

    private fun showPopupWindow(result: String) {
        if (balloon != null && balloon?.isDisposed == false) {
            balloon?.hide()
        }
        ApplicationManager.getApplication().invokeLater {
            JBPopupFactory.getInstance()
                    .createHtmlTextBalloonBuilder(result, MessageType.INFO, null)
                    .setFadeoutTime(15000)
                    .setHideOnAction(true)
                    .createBalloon()
                    .show(JBPopupFactory.getInstance().guessBestPopupLocation(editor), Balloon.Position.below)
        }
    }

    private fun isFastClick(timeMillis: Long): Boolean {
        val begin = System.currentTimeMillis()
        val end = begin - latestClickTime
        if (end in 1..(timeMillis - 1)) return true
        latestClickTime = begin
        return false
    }
}
package com.crazymt.aitranslate.net

import com.crazymt.aitranslate.bean.*
import com.google.gson.Gson
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

import java.io.OutputStreamWriter

val SourceOllama = "Ollama(本地模型)"
val SourceGemini = "Gemini"

var sourceType = LocalData.read("sourceType")

var ollamaURL = LocalData.read("ollamaURL")
var modelName = LocalData.read("modelName")
var geminiAPIKey = LocalData.read("geminiAPIKey")

fun requestNetData(file: String?, queryWord: String, callBack: NetCallback<TranslateResult>) {
    try {
        /*LocalData.read(queryWord)?.let {
            try {
                val bean = Gson().fromJson<TranslateResult>(it, TranslateResult::class.java)
                callBack.onSuccess(bean)
            } catch (e: JsonSyntaxException) {
                callBack.onFail(" 返回解析失败，github issue to me：\n$it")
            }
            return
        }*/
        if (sourceType == SourceOllama) {
            val url = URL(ollamaURL)

            val connection = url.openConnection() as HttpURLConnection

            connection.requestMethod = "POST"
            connection.connectTimeout = 3000
            connection.readTimeout = 30000
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")

            connection.doOutput = true

            val jsonInputString = """
                {
                    "model": "$modelName",
                    "prompt": "${queryWord.replace(Regex("[*+\\- \r]+"), " ")}",
                    "stream": false,
                    "system":"#角色：你是一位精通简体中文的专业翻译。我希望你能帮我将以下英文翻译成中文。 # 规则：- 分成两次翻译，并且打印每一次结果：1. 根据内容直译，不要遗漏任何信息2. 根据第一次直译的结果重新意译，遵守原意的前提下让内容更通俗易懂，符合中文表达习惯。#开始 文本：${
                        queryWord.replace(
                            Regex("[*+\\- \r]+"),
                            " "
                        )
                    }"
                }
                """.trimIndent()

            val outputStream = OutputStreamWriter(connection.outputStream)
            outputStream.write(jsonInputString)
            outputStream.flush()

            if (connection.responseCode == 200) {
                val ins = connection.inputStream

                val content = StreamUtils.getStringFromStream(ins)
                if (content.isNotBlank()) {
                    println(content);
                    val result = Gson().fromJson(content, OllamaBean::class.java).toTranslateResult()
                    callBack.onSuccess(result)
//                    LocalData.store(queryWord, Gson().toJson(result))
                } else {
                    callBack.onFail("翻译接口返回为空")
                }
            } else {
                callBack.onFail("错误码：${connection.responseCode}\n错误信息：\n${connection.responseMessage}")
            }
            return
        } else if (sourceType == SourceGemini) {
            val url =
                URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.0-pro:generateContent?key=$geminiAPIKey")

            val connection = url.openConnection() as HttpURLConnection

            connection.requestMethod = "POST"
            connection.connectTimeout = 3000
            connection.readTimeout = 30000
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")

            connection.doOutput = true
            val jsonInputString = """
                {
                  "contents": [
                    {
                      "parts": [
                        {
                          "text": "#角色：你是一位精通简体中文的专业翻译。我希望你能帮我将以下英文翻译成中文。 # 规则： - 分成两次翻译，并且打印每一次结果：1. 根据内容直译，不要遗漏任何信息2. 根据第一次直译的结果重新意译，遵守原意的前提下让内容更通俗易懂，符合中文表达习惯。#开始：${
                                queryWord.replace(
                                    Regex("[*+\\- \r]+"),
                                    " "
                                )
                            }"
                        }
                      ]
                    }
                  ],
                }
                """.trimIndent()

            val outputStream = OutputStreamWriter(connection.outputStream)
            outputStream.write(jsonInputString)
            outputStream.flush()

            if (connection.responseCode == 200) {
                val ins = connection.inputStream

                val content = StreamUtils.getStringFromStream(ins)
                if (content.isNotBlank()) {
                    println(content);
                    val result = Gson().fromJson(content, GeminiBean::class.java).toTranslateResult()
                    callBack.onSuccess(result)
//                    LocalData.store(queryWord, Gson().toJson(result))
                } else {
                    callBack.onFail("翻译接口返回为空")
                }
            } else {
                callBack.onFail("错误码：${connection.responseCode}\n错误信息：\n${connection.responseMessage}")
            }
            return
        } else {
            callBack.onFail("不支持的翻译源类型$sourceType")
            return
        }

    } catch (e: IOException) {
        callBack.onFail("无法访问：\n${e.message}")
    }
}



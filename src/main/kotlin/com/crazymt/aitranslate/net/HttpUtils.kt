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
        when (sourceType) {
            SourceOllama -> {
                val url = URL(ollamaURL)
//                println(ollamaURL)

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
                        "system":"你是一位精通简体中文的专业翻译，我希望你能帮我将以下英文翻译成中文。  规则： - 这些英文和编程专业知识相关，这些英文来自java代码，注意翻译时术语的准确性 - 译文需要通俗、简洁、易懂。- 翻译时采用以下步骤 1. 第一步，按照字面意思直译翻译这一段文本内容 2. 第二步，参照第一步直译的结果，结合上下文，对内容进行意译"
                    }
                    """.trimIndent()
//                println(jsonInputString)
                val outputStream = OutputStreamWriter(connection.outputStream)
                outputStream.write(jsonInputString)
                outputStream.flush()

                if (connection.responseCode == 200) {
                    val ins = connection.inputStream

                    val content = StreamUtils.getStringFromStream(ins)
                    if (content.isNotBlank()) {
//                        println(content);
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
            }

            SourceGemini -> {
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
                              "text": "你是一位精通简体中文的专业翻译，我希望你能帮我将以下英文翻译成中文。  规则： - 这些英文和编程专业知识相关，这些英文来自 $file 代码，注意翻译时术语的准确性 - 译文需要通俗、简洁、易懂。- 翻译时采用以下步骤，直接告诉我第二步的结果，注意，直接告诉我最终结果，不需要其它修饰词。 1. 第一步，按照字面意思直译翻译这一段文本内容 2. 第二步，参照第一步直译的结果，结合上下文，对内容进行意译。 英文：${queryWord.replace(Regex("[*+\\- \r]+"), " ")}"
                            }
                          ]
                        }
                      ],
                      "generationConfig": {
                        "temperature": 1,
                        "topK": 1,
                        "topP": 1,
                        "maxOutputTokens": 2048,
                        "stopSequences": []
                      },
                      "safetySettings": [
                        {
                          "category": "HARM_CATEGORY_HARASSMENT",
                          "threshold": "BLOCK_MEDIUM_AND_ABOVE"
                        },
                        {
                          "category": "HARM_CATEGORY_HATE_SPEECH",
                          "threshold": "BLOCK_MEDIUM_AND_ABOVE"
                        },
                        {
                          "category": "HARM_CATEGORY_SEXUALLY_EXPLICIT",
                          "threshold": "BLOCK_MEDIUM_AND_ABOVE"
                        },
                        {
                          "category": "HARM_CATEGORY_DANGEROUS_CONTENT",
                          "threshold": "BLOCK_MEDIUM_AND_ABOVE"
                        }
                      ]
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
            }

            else -> {
                callBack.onFail("不支持的翻译源类型$sourceType")
                return
            }
        }

    } catch (e: IOException) {
        callBack.onFail("无法访问：\n${e.message}")
    }
}



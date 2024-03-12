package com.tracy.a8translateplus.net

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.tracy.a8translateplus.bean.OllamaBean
import com.tracy.a8translateplus.bean.BaiduTranslationBean
import com.tracy.a8translateplus.bean.NiuTranslationBean
import com.tracy.a8translateplus.bean.TranslateResult
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

import java.io.OutputStreamWriter

/**
 * @author cuishijie
 * *
 */

//翻译类型枚举，小牛
val SourceNiu = "小牛翻译"

//百度
val SourceBaidu = "百度翻译"

//百度
val SourceOllama = "Ollama(本地模型)"

//访问使用小牛翻译
private const val NIU_BASE_URL = "https://api.niutrans.com/NiuTransServer/translation?from=en&to=zh&apikey=430cd5a365eca28eec1571a1cd3a57d3&src_text="
private const val BAIDU_BASE_URL = "https://fanyi-api.baidu.com/api/trans/vip/translate?"

var sourceType = LocalData.read("sourceType")
var baiduAppId = LocalData.read("baiduAppId")
var baiduSecret = LocalData.read("baiduSecret")

var ollamaURL = LocalData.read("ollamaURL")
var modelName = LocalData.read("modelName")

/**
 * 请求网络数据
 * @author ice1000
 */
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
        var url: URL
        val queryStr = URLEncoder.encode(queryWord.replace(Regex("[*+\\- \r]+"), " "), "UTF-8")
        if (sourceType == SourceNiu) {
            url = URL("$NIU_BASE_URL${queryStr}")
        } else if (sourceType == SourceBaidu) {
            url = URL("$BAIDU_BASE_URL${buildBaiduParams(queryStr, "en", "zh")}")
        } else if (sourceType == SourceOllama) {
            // 指定请求的 URL
            val url = URL(ollamaURL)

            // 创建 HTTP 连接
            val connection = url.openConnection() as HttpURLConnection

            // 设置请求方法为 POST
            connection.requestMethod = "POST"
            connection.connectTimeout = 3000
            connection.readTimeout = 30000
            // 设置请求头
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")

            // 启用输出流，以便发送数据
            connection.doOutput = true
            // gemma:2b
            // 创建 JSON 请求体
            val jsonInputString = """
        {
            "model": "$modelName",
            "prompt": "${queryWord.replace(Regex("[*+\\- \r]+"), " ")}",
            "stream": false,
            "system":"#角色：你是一位精通简体中文的专业翻译。我希望你能帮我将以下英文翻译成中文。 # 规则：- 分成两次翻译，并且打印每一次结果：1. 根据内容直译，不要遗漏任何信息2. 根据第一次直译的结果重新意译，遵守原意的前提下让内容更通俗易懂，符合中文表达习惯。#开始 文本：${queryWord.replace(Regex("[*+\\- \r]+"), " ")}"
        }
    """.trimIndent()
            println(jsonInputString);

            // 发送请求体
            val outputStream = OutputStreamWriter(connection.outputStream)
            outputStream.write(jsonInputString)
            outputStream.flush()

            // 连接成功
            if (connection.responseCode == 200) {
                val ins = connection.inputStream

                // 获取到Json字符串
                val content = StreamUtils.getStringFromStream(ins)
                if (content.isNotBlank()) {
                    println(content);
                    var result: TranslateResult? = null;
                    if (sourceType == SourceNiu) {
                        result = Gson().fromJson(content, NiuTranslationBean::class.java).toTranslateResult()
                    } else if (sourceType == SourceBaidu) {
                        result = Gson().fromJson(content, BaiduTranslationBean::class.java).toTranslateResult()
                    } else if (sourceType == SourceOllama) {
                        result = Gson().fromJson(content, OllamaBean::class.java).toTranslateResult()
                    }
                    println(result);
                    callBack.onSuccess(result!!)
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

        val conn = url.openConnection() as HttpURLConnection

        conn.connectTimeout = 3000
        conn.readTimeout = 3000
        conn.requestMethod = conn.requestMethod

        // 连接成功
        if (conn.responseCode == 200) {
            val ins = conn.inputStream

            // 获取到Json字符串
            val content = StreamUtils.getStringFromStream(ins)
            if (content.isNotBlank()) {
                println(content);
                var result: TranslateResult? = null;
                if (sourceType == SourceNiu) {
                    result = Gson().fromJson(content, NiuTranslationBean::class.java).toTranslateResult()
                } else if (sourceType == SourceBaidu) {
                    result = Gson().fromJson(content, BaiduTranslationBean::class.java).toTranslateResult()
                }
                callBack.onSuccess(result!!)
                LocalData.store(queryWord, Gson().toJson(result))
            } else callBack.onFail("翻译接口返回为空")
        } else callBack.onFail("错误码：${conn.responseCode}\n错误信息：\n${conn.responseMessage}")
    } catch (e: IOException) {
        callBack.onFail("无法访问：\n${e.message}")
    }
}

private fun buildBaiduParams(query: String, from: String, to: String): String {
    val salt = System.currentTimeMillis().toString()
    val appid = baiduAppId ?: ""
    val src1 = appid + query + salt + baiduSecret
    val sign = MD5.md5(src1)
    return "q=$query&from=$from&to=$to&appid=$appid&salt=$salt&sign=$sign"
}




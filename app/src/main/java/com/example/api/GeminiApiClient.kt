package com.example.api

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiApiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun generateSalesCopy(title: String, category: String, price: Double, coupon: String, link: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "⚠️ Configure sua GEMINI_API_KEY nos segredos do AI Studio para gerar copys automáticas com inteligência artificial!"
        }

        val prompt = """
            Você é um Copywriter especialista em vendas e afiliação da Shopee no Brasil para a página de ofertas 'BASS COMPRE MAIS ACHADINHO'.
            Crie uma oferta/copy altamente persuasiva, divertida, cheia de emojis adequados, ideal para postar nos stories do Instagram ou status do WhatsApp para o seguinte produto:
            - Nome: $title
            - Categoria: $category
            - Preço promocional: R$ ${String.format("%.2f", price)}
            ${if (coupon.isNotEmpty()) "- Cupom disponível: $coupon" else ""}
            - Link Oficial de Afiliado: $link
            
            Instruções:
            1. Comece com uma chamada muito atraente, divertida e com emojis (ex: "SENTA QUE LÁ VEM ACHADINHO! 🚨", "ALERTA DE MINE-PREÇO! 😱").
            2. Descreva de forma rápida e engajante por que as pessoas necessitam desse produto em suas vidas cotidianas.
            3. Destaque o preço de forma atrativa.
            4. Se houver cupom, indique como economizar usando-o.
            5. Finalize convocando o leitor de forma irresistível a clicar no link de compra e inclua exatamente o seguinte link de afiliado ao fim: $link
            6. Mantenha sucinto, persuasivo, atraente e pronto para copiar e colar.
        """.trimIndent()

        val jsonRequest = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonRequest.toString().toRequestBody(mediaType)
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext "Erro ao conectar com a IA: Código ${response.code}"
                }
                val bodyString = response.body?.string() ?: return@withContext "Nenhuma resposta da IA."
                val responseJson = JSONObject(bodyString)
                val candidates = responseJson.getJSONArray("candidates")
                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.getJSONObject("content")
                val parts = content.getJSONArray("parts")
                val text = parts.getJSONObject(0).getString("text")
                text
            }
        } catch (e: Exception) {
            Log.e("GeminiApiClient", "Error calling Gemini", e)
            "Erro ao gerar copy com a IA: ${e.localizedMessage ?: "Verifique sua conexão de Internet"}"
        }
    }
}

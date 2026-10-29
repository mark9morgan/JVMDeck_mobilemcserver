package dev.alex.jvmdeck.model

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.concurrent.thread

object ModrinthApi {

    fun searchPlugins(
        query: String = "",
        mcVersion: String = "1.20.4",
        category: String? = null,
        projectType: String = "plugin", // "plugin" or "mod"
        loader: String? = null, // "fabric", "forge", "neoforge"
        onResult: (List<PluginItem>) -> Unit,
        onError: (String) -> Unit
    ) {
        thread(name = "modrinth-search") {
            try {
                val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
                var urlStr = "https://api.modrinth.com/v2/search?limit=25&query=$encodedQuery"

                val facetsList = mutableListOf<String>()
                facetsList.add("[\"project_type:$projectType\"]")
                if (mcVersion.isNotBlank()) {
                    facetsList.add("[\"versions:$mcVersion\"]")
                }
                if (!loader.isNullOrBlank()) {
                    facetsList.add("[\"loaders:$loader\"]")
                }

                if (!category.isNullOrBlank() && category != "Всі" && category != "Популярні") {
                    val catSlug = when (category.lowercase()) {
                        "адміністрування" -> "admin"
                        "оптимізація" -> "optimization"
                        "економіка" -> "economy"
                        "захист" -> "protection"
                        "контент" -> "content"
                        else -> category.lowercase()
                    }
                    if (catSlug.isNotEmpty()) {
                        facetsList.add("[\"categories:$catSlug\"]")
                    }
                }

                if (facetsList.isNotEmpty()) {
                    val facetsJson = "[${facetsList.joinToString(",")}]"
                    val encodedFacets = URLEncoder.encode(facetsJson, "UTF-8")
                    urlStr += "&facets=$encodedFacets"
                }

                val conn = URL(urlStr).openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("User-Agent", "JvmDeck-AndroidApp/1.0")
                conn.connectTimeout = 8000
                conn.readTimeout = 8000

                val jsonText = conn.inputStream.bufferedReader().readText()
                val json = JSONObject(jsonText)
                val hits = json.getJSONArray("hits")

                val plugins = mutableListOf<PluginItem>()
                for (i in 0 until hits.length()) {
                    val hit = hits.getJSONObject(i)
                    val categoriesArr = hit.optJSONArray("categories")
                    val cats = mutableListOf<String>()
                    if (categoriesArr != null) {
                        for (j in 0 until categoriesArr.length()) {
                            cats.add(categoriesArr.getString(j))
                        }
                    }

                    val iconUrl = if (hit.has("icon_url") && !hit.isNull("icon_url")) hit.getString("icon_url") else null

                    plugins.add(
                        PluginItem(
                            id = hit.getString("project_id"),
                            slug = hit.optString("slug", ""),
                            title = hit.optString("title", "Без назви"),
                            description = hit.optString("description", "Опис відсутній"),
                            iconUrl = iconUrl,
                            downloads = hit.optInt("downloads", 0),
                            author = hit.optString("author", "Автор"),
                            categories = cats
                        )
                    )
                }

                onResult(plugins)
            } catch (e: Exception) {
                onError("Помилка пошуку плагінів: ${e.message}")
            }
        }
    }

    fun getLatestVersion(
        projectId: String,
        mcVersion: String,
        onResult: (PluginVersionInfo) -> Unit,
        onError: (String) -> Unit
    ) {
        thread(name = "modrinth-version") {
            try {
                val urlStr = "https://api.modrinth.com/v2/project/$projectId/version"
                val conn = URL(urlStr).openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("User-Agent", "JvmDeck-AndroidApp/1.0")
                conn.connectTimeout = 8000
                conn.readTimeout = 8000

                val jsonText = conn.inputStream.bufferedReader().readText()
                val versionsArr = JSONArray(jsonText)

                if (versionsArr.length() == 0) {
                    onError("Не знайдено сумісних версій плагіна")
                    return@thread
                }

                var selectedVerObj: JSONObject? = null

                // Шукаємо версію, яка явно підтримує mcVersion
                for (i in 0 until versionsArr.length()) {
                    val vObj = versionsArr.getJSONObject(i)
                    val gVers = vObj.optJSONArray("game_versions")
                    if (gVers != null) {
                        for (j in 0 until gVers.length()) {
                            if (gVers.getString(j) == mcVersion) {
                                selectedVerObj = vObj
                                break
                            }
                        }
                    }
                    if (selectedVerObj != null) break
                }

                if (selectedVerObj == null) {
                    selectedVerObj = versionsArr.getJSONObject(0)
                }

                val filesArr = selectedVerObj.getJSONArray("files")
                if (filesArr.length() == 0) {
                    onError("Файли завантаження відсутні")
                    return@thread
                }

                var primaryFile: JSONObject? = null
                for (i in 0 until filesArr.length()) {
                    val fObj = filesArr.getJSONObject(i)
                    val fn = fObj.optString("filename", "").lowercase()
                    val isPrimary = fObj.optBoolean("primary", false)
                    if (isPrimary || fn.contains("spigot") || fn.contains("paper") || fn.endsWith(".jar")) {
                        primaryFile = fObj
                        if (fn.contains("spigot") || fn.contains("paper")) break
                    }
                }
                if (primaryFile == null) {
                    primaryFile = filesArr.getJSONObject(0)
                }

                val downloadUrl = primaryFile.getString("url")
                val fileName = primaryFile.getString("filename")
                val versionId = selectedVerObj.getString("id")
                val versionNumber = selectedVerObj.optString("version_number", "1.0.0")

                onResult(
                    PluginVersionInfo(
                        versionId = versionId,
                        versionNumber = versionNumber,
                        downloadUrl = downloadUrl,
                        fileName = fileName
                    )
                )
            } catch (e: Exception) {
                onError("Помилка визначення версії: ${e.message}")
            }
        }
    }
}

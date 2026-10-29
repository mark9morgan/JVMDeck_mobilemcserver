package dev.alex.jvmdeck.model

data class PluginItem(
    val id: String,
    val slug: String,
    val title: String,
    val description: String,
    val iconUrl: String?,
    val downloads: Int,
    val author: String,
    val categories: List<String> = emptyList(),
    val isInstalled: Boolean = false,
    val installedFileName: String? = null
)

data class PluginVersionInfo(
    val versionId: String,
    val versionNumber: String,
    val downloadUrl: String,
    val fileName: String
)

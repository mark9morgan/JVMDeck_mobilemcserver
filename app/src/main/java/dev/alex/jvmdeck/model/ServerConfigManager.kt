package dev.alex.jvmdeck.model

import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object ServerConfigManager {

    private fun getServersJsonFile(filesDir: File): File {
        return File(filesDir, "servers.json")
    }

    fun getServerDir(filesDir: File, serverId: String): File {
        val serversRoot = File(filesDir, "servers")
        serversRoot.mkdirs()
        return File(serversRoot, serverId).apply { mkdirs() }
    }

    fun loadAllServers(filesDir: File): List<ServerConfig> {
        val jsonFile = getServersJsonFile(filesDir)
        val servers = mutableListOf<ServerConfig>()

        if (jsonFile.exists()) {
            runCatching {
                val jsonText = jsonFile.readText(Charsets.UTF_8)
                val array = JSONArray(jsonText)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val cfg = ServerConfig(
                        id = obj.optString("id", "server_$i"),
                        name = obj.optString("name", "Сервер ${i + 1}"),
                        type = obj.optString("type", "Paper"),
                        version = obj.optString("version", "1.20.4"),
                        port = obj.optInt("port", 25565 + i),
                        maxPlayers = obj.optInt("maxPlayers", 20),
                        gamemode = obj.optString("gamemode", "Survival"),
                        difficulty = obj.optString("difficulty", "Normal"),
                        onlineMode = obj.optBoolean("onlineMode", true),
                        autoRestart = obj.optBoolean("autoRestart", false),
                        lanVisible = obj.optBoolean("lanVisible", true),
                        ramMb = obj.optInt("ramMb", 1024),
                        javaVersionMode = obj.optString("javaVersionMode", "auto")
                    )
                    servers.add(cfg)
                }
            }
        }

        return servers
    }

    fun saveAllServers(filesDir: File, servers: List<ServerConfig>) {
        val array = JSONArray()
        servers.forEach { cfg ->
            val obj = JSONObject()
            obj.put("id", cfg.id)
            obj.put("name", cfg.name)
            obj.put("type", cfg.type)
            obj.put("version", cfg.version)
            obj.put("port", cfg.port)
            obj.put("maxPlayers", cfg.maxPlayers)
            obj.put("gamemode", cfg.gamemode)
            obj.put("difficulty", cfg.difficulty)
            obj.put("onlineMode", cfg.onlineMode)
            obj.put("autoRestart", cfg.autoRestart)
            obj.put("lanVisible", cfg.lanVisible)
            obj.put("ramMb", cfg.ramMb)
            obj.put("javaVersionMode", cfg.javaVersionMode)
            array.put(obj)
        }
        getServersJsonFile(filesDir).writeText(array.toString(2), Charsets.UTF_8)
    }

    fun createNewServer(
        filesDir: File,
        name: String,
        type: String,
        version: String,
        javaMode: String,
        ramMb: Int = 1024
    ): ServerConfig {
        val currentServers = loadAllServers(filesDir)
        val newId = "server_${System.currentTimeMillis()}"
        val nextPort = 25565 + currentServers.size

        val newConfig = ServerConfig(
            id = newId,
            name = name.ifBlank { "Сервер ${currentServers.size + 1}" },
            type = type,
            version = version,
            port = nextPort,
            javaVersionMode = javaMode,
            ramMb = ramMb
        )

        val updatedList = currentServers + newConfig
        saveAllServers(filesDir, updatedList)
        savePropertiesForServer(getServerDir(filesDir, newConfig.id), newConfig)

        return newConfig
    }

    fun updateServerConfig(filesDir: File, config: ServerConfig) {
        val currentServers = loadAllServers(filesDir).toMutableList()
        val index = currentServers.indexOfFirst { it.id == config.id }
        if (index != -1) {
            currentServers[index] = config
        } else {
            currentServers.add(config)
        }
        saveAllServers(filesDir, currentServers)
        savePropertiesForServer(getServerDir(filesDir, config.id), config)
    }

    fun deleteServer(filesDir: File, serverId: String): List<ServerConfig> {
        val currentServers = loadAllServers(filesDir).toMutableList()
        currentServers.removeAll { it.id == serverId }
        saveAllServers(filesDir, currentServers)
        getServerDir(filesDir, serverId).deleteRecursively()
        return currentServers
    }

    private fun savePropertiesForServer(serverDir: File, config: ServerConfig) {
        serverDir.mkdirs()
        val propField = File(serverDir, "server.properties")
        val props = mutableMapOf<String, String>()

        if (propField.exists()) {
            runCatching {
                propField.readLines().forEach { line ->
                    val trimmed = line.trim()
                    if (!trimmed.startsWith("#") && trimmed.contains("=")) {
                        val parts = trimmed.split("=", limit = 2)
                        props[parts[0].trim()] = parts[1].trim()
                    }
                }
            }
        }

        props["server-port"] = config.port.toString()
        props["max-players"] = config.maxPlayers.toString()
        props["gamemode"] = config.gamemode.lowercase()
        props["difficulty"] = config.difficulty.lowercase()
        props["online-mode"] = config.onlineMode.toString()

        val content = props.map { "${it.key}=${it.value}" }.joinToString("\n") + "\n"
        propField.writeText(content, Charsets.UTF_8)
    }
}

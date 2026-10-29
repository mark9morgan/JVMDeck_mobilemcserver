package dev.alex.jvmdeck.model

data class ServerConfig(
    val id: String = "default_server",
    val name: String = "Мій сервер",
    val type: String = "Paper", // Vanilla, Paper, Spigot, Forge, Fabric
    val version: String = "1.20.4",
    val port: Int = 25565,
    val maxPlayers: Int = 20,
    val gamemode: String = "Survival", // Survival, Creative, Adventure, Spectator
    val difficulty: String = "Normal", // Peaceful, Easy, Normal, Hard
    val onlineMode: Boolean = true,
    val autoRestart: Boolean = false,
    val lanVisible: Boolean = true,
    val ramMb: Int = 1024,
    val javaVersionMode: String = "auto" // auto, 8, 17, 21, 25
)

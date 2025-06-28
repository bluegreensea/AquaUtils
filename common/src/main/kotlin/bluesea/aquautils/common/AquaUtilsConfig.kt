package bluesea.aquautils.common

import com.electronwill.nightconfig.core.Config
import com.electronwill.nightconfig.core.file.FileConfig
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID

class AquaUtilsConfig(configPath: Path) {
    companion object {
        var config: FileConfig? = null
    }

    private val config: FileConfig
    private val vote: FileConfig
    private val record: FileConfig

    init {
        val configFile = configPath.resolve("config.toml")
        if (!Files.exists(configFile)) {
            Files.createFile(configFile)
        }
        config = FileConfig.builder(configFile).autosave().build()
        val voteFile = configPath.resolve("vote.toml")
        if (!Files.exists(configFile)) {
            Files.createFile(configFile)
        }
        vote = FileConfig.builder(voteFile).autosave().build()
        val recordFile = configPath.resolve("record.toml")
        if (!Files.exists(configFile)) {
            Files.createFile(configFile)
        }
        record = FileConfig.builder(recordFile).autosave().build()
        AquaUtilsConfig.config = config
    }

    fun load() {
        config.load()
        vote.load()
        record.load()

        Controller.kick = config.getOrElse("kick", Controller.kick)
        Controller.discord = config.getOrElse("discord", Controller.discord)
        Controller.lobbyServer = config.getOrElse("lobby-server", Controller.lobbyServer)
        Controller.serversPanel = config.getOrElse("servers-panel", Controller.serversPanel)
        Controller.serverIpService = config.getOrElse("server-ip-service", Controller.serverIpService)
        Controller.fallbackServerIp = config.getOrElse("fallback-server-ip", Controller.fallbackServerIp)
        Controller.fallbackServerPort = config.getOrElse("fallback-server-port", Controller.fallbackServerPort)

        vote.get<List<String>>("vote-history")?.let { Controller.voteHistory.addAll(it) }
        vote.get<Config>("vote-data.player-votes")?.let { playerVotesConfig ->
            Controller.voteData.playerVotes.putAll(
                playerVotesConfig.entrySet().map { UUID.fromString(it.key) to it.getValue() as String }
            )
        }
        vote.get<List<String>>("vote-data.options")?.let { Controller.voteData.options.addAll(it) }

        record.get<String>("yt-live-id")?.let { Controller.ytLiveIdRecord = it }
    }

    private fun <T> Config.setRaw(path: String, value: T): T {
        return this.set(path, value)
    }

    fun save() {
        config.setRaw("kick", Controller.kick)
        config.setRaw("lobby-server", Controller.lobbyServer)
        config.setRaw("servers-panel", Controller.serversPanel)
        config.setRaw("server-ip-service", Controller.serverIpService)
        config.setRaw("fallback-server-ip", Controller.fallbackServerIp)
        config.setRaw("fallback-server-port", Controller.fallbackServerPort)

        vote.setRaw("vote-history", Controller.voteHistory.toList())
        vote.setRaw(
            "vote-data.player-votes",
            Config.of({ Controller.voteData.playerVotes.mapKeys { (k, _) -> "$k" } }, vote.configFormat())
        )
        vote.setRaw("vote-data.options", Controller.voteData.options)

        record.setRaw("yt-live-id", Controller.ytLiveIdRecord)

        config.close()
        vote.close()
        record.close()
    }
}

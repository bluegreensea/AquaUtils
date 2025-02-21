package bluesea.aquautils

import bluesea.aquautils.common.Controller
import net.fabricmc.api.EnvType
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.loader.api.FabricLoader
import org.incendo.cloud.SenderMapper
import org.incendo.cloud.execution.ExecutionCoordinator
import org.incendo.cloud.fabric.FabricServerCommandManager

class AquaUtilsFabric : ModInitializer {
    override fun onInitialize() {
        PayloadTypeRegistry.playS2C().register(VoteDataPacket.ID, VoteDataPacket.CODEC)
        PayloadTypeRegistry.playC2S().register(VoteDataPacket.ID, VoteDataPacket.CODEC)
        when (FabricLoader.getInstance().environmentType) {
            EnvType.SERVER -> onInitializeServer()
            EnvType.CLIENT -> onInitializeClient()
            else -> {}
        }
    }

    private fun onInitializeServer() {
        val path = FabricLoader.getInstance().configDir.resolve("aquautils.properties")
        val config = AquaUtilsConfig.load(path)
        config.store(path)
        Controller.kick = config.kick
        Controller.LOGGER.info("AquaUtils kick: " + Controller.kick)

        val audienceProvider = FabricAudienceProvider()
        val fabricCommandManager = FabricServerCommandManager(
            ExecutionCoordinator.builder<FabricAudience>().synchronizeExecution().build(),
            SenderMapper.create(audienceProvider::get, FabricAudience::source)
        )

        Controller.register(fabricCommandManager, audienceProvider)
    }

    private fun onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(VoteDataPacket.ID) { payload, context ->
            HudRenderCallback.EVENT.register(
                HudRenderCallback { guiGraphics, _ ->
                    val textRenderer = context.client().font
                    guiGraphics.drawString(textRenderer, "Text", 0, 0, 0xffffff, false)
                }
            )
        }
        Controller.LOGGER.info("Loaded")
    }
}

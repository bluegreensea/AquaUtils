package bluesea.aquautils

import bluesea.aquautils.common.Constants
import bluesea.aquautils.common.Controller
import bluesea.aquautils.common.parser.CommonPlayers
import java.util.Optional
import net.fabricmc.api.EnvType
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.client.rendering.v1.HudLayerRegistrationCallback
import net.fabricmc.fabric.api.client.rendering.v1.IdentifiedLayer
import net.fabricmc.fabric.api.client.rendering.v1.LayeredDrawerWrapper
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.loader.api.FabricLoader
import net.kyori.adventure.audience.Audience
import net.minecraft.network.chat.ChatType
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
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

        ServerWorldEvents.LOAD.register { server, world ->
            val audienceProvider = FabricAudienceProvider(server)
            val fabricCommandManager = FabricServerCommandManager(
                ExecutionCoordinator.builder<FabricAudience>().synchronizeExecution().build(),
                SenderMapper.create(audienceProvider::get, FabricAudience::source)
            )

            Controller.register(fabricCommandManager, audienceProvider)
        }

        ServerMessageEvents.CHAT_MESSAGE.register { message, player, bound ->
            if (bound.chatType == ChatType.CHAT) {
                val audience = player as Audience
                Controller.onPlayerMessage(
                    FabricAudience(player.createCommandSourceStack(), audience),
                    message.signedContent(),
                    Optional.empty(),
                    CommonPlayers(player.server.playerList.players.map {
                        FabricAudience(it.createCommandSourceStack(), it as Audience)
                    })
                )
                val kick = Controller.onPlayerDetectSpam(audience, message.signedContent())
                if (kick && !player.hasPermissions(5)) {
                    player.connection.disconnect(Component.nullToEmpty("已重複3次(含)以上"))
                }
            }
        }
    }

    private fun onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(VoteDataPacket.ID) { payload, context ->
            HudLayerRegistrationCallback.EVENT.register(
                HudLayerRegistrationCallback { layeredDrawer: LayeredDrawerWrapper? ->
                    layeredDrawer!!.attachLayerAfter(
                        IdentifiedLayer.MISC_OVERLAYS,
                        ResourceLocation.fromNamespaceAndPath(Constants.ID, "vote_overlay")
                    ) { guiGraphics, tickCounter ->
                        // TODO: voteData network
                        val textRenderer = context.client().font
                        guiGraphics.drawString(textRenderer, "Test Text", 0, 0, 0xffffff, false)
                    }
                }
            )
        }
        Controller.LOGGER.info("Loaded")
    }
}

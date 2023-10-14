package bluesea.aquautils

import bluesea.aquautils.common.Constants
import bluesea.aquautils.common.Controller
import cloud.commandframework.execution.AsynchronousCommandExecutionCoordinator
import cloud.commandframework.fabric.FabricServerCommandManager
import net.fabricmc.api.EnvType
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
import net.fabricmc.fabric.api.networking.v1.PacketSender
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.multiplayer.ClientPacketListener
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceLocation

class AquaUtilsFabric : ModInitializer {
    override fun onInitialize() {
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
            AsynchronousCommandExecutionCoordinator.builder<FabricAudience>().build(),
            audienceProvider::get,
            FabricAudience::source
        )

        Controller.register(fabricCommandManager, audienceProvider)
    }

    private fun onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(
            ResourceLocation(Constants.MOD_ID, "test")
        ) { client: Minecraft, _: ClientPacketListener, bytebuf: FriendlyByteBuf, _: PacketSender ->
            HudRenderCallback.EVENT.register(
                HudRenderCallback { guiGraphics: GuiGraphics, _: Float ->
                    val font = client.font
                    guiGraphics.drawString(font, "Text", 0, 0, 0xffffff)
                }
            )
        }
        Controller.LOGGER.info("Loaded")
    }
}

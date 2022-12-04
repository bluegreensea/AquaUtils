package bluesea.aquautils.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static bluesea.aquautils.AquaUtils.onPlayerMessage;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerPlayNetworkHandlerMixin {
    @Shadow @Final
    private MinecraftServer server;
    @Shadow
    public ServerPlayer player;
    @Shadow
    private int chatSpamTickCount;
    @Shadow
    public abstract void disconnect(Component component);

    @Inject(at = @At("HEAD"), method = "handleChat")
    private void onChatMessage(ServerboundChatPacket serverboundChatPacket, CallbackInfo ci) {
        if (onPlayerMessage(this.player, serverboundChatPacket.message()) && !this.player.hasPermissions(5)) {
            this.disconnect(Component.nullToEmpty("已重複3次(含)以上"));
        }
    }

    @Inject(at = @At("HEAD"), method = "performChatCommand")
    private void onCommandExecution(ServerboundChatCommandPacket serverboundChatCommandPacket, CallbackInfo ci) {
        if (this.chatSpamTickCount > 20) {
            this.chatSpamTickCount -= 20;
        }
    }
}

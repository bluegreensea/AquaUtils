package bluesea.aquautils.mixin;

import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static bluesea.aquautils.AquaUtils.onPlayerMessage;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin {

    @Shadow @Final
    private MinecraftServer server;
    @Shadow
    public ServerPlayerEntity player;
    @Shadow public abstract void disconnect(Text reason);


    @Inject(at = @At("HEAD"), method = "onChatMessage")
    private void onChatMessage(ChatMessageC2SPacket packet, CallbackInfo ci) {
        if (!this.server.getPlayerManager().isOperator(this.player.getGameProfile()) && onPlayerMessage(this.player, packet.chatMessage()))
            this.disconnect(Text.of("已重複3次(含)以上"));
    }
}
package bluesea.aquautils.mixin;

import net.minecraft.class_7648;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.network.packet.c2s.play.CommandExecutionC2SPacket;
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
    @Shadow
    private int messageCooldown;
    @Shadow
    public abstract void disconnect(Text reason);

    @Inject(at = @At("HEAD"), method = "sendPacket(Lnet/minecraft/network/Packet;Lnet/minecraft/class_7648;)V")
    private void onsendPacket(Packet<?> packet, class_7648 arg, CallbackInfo ci) {
        if (packet instanceof ChatMessageC2SPacket chatMessagePacket) {
            if (onPlayerMessage(this.player, chatMessagePacket.chatMessage()) && !this.server.getPlayerManager().isOperator(this.player.getGameProfile()))
                this.disconnect(Text.of("已重複3次(含)以上"));
        }
        if (packet instanceof CommandExecutionC2SPacket) {
            if (this.messageCooldown > 20) {
                this.messageCooldown -= 20;
            }
        }
    }
}

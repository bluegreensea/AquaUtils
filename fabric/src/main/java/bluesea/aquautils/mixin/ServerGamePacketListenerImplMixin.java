package bluesea.aquautils.mixin;

import bluesea.aquautils.FabricAudience;
import bluesea.aquautils.common.Controller;
import net.kyori.adventure.audience.Audience;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin extends ServerCommonPacketListenerImpl {
    @Shadow
    public ServerPlayer player;

    public ServerGamePacketListenerImplMixin(MinecraftServer minecraftServer, Connection connection, CommonListenerCookie commonListenerCookie) {
        super(minecraftServer, connection, commonListenerCookie);
    }

    @Inject(method = "handleChat", at = @At("HEAD"))
    private void onChatMessage(ServerboundChatPacket serverboundChatPacket, CallbackInfo ci) {
        Audience audience = (Audience) player;
        Controller.INSTANCE.onPlayerMessage(
            audience,
            serverboundChatPacket.message(),
            new FabricAudience(player.server.createCommandSourceStack(), audience)
        );
        boolean kick = Controller.INSTANCE.onPlayerDetectSpam(audience, serverboundChatPacket.message());
        if (kick && !player.hasPermissions(5)) {
            this.disconnect(Component.nullToEmpty("已重複3次(含)以上"));
        }
    }

    @Inject(method = "detectRateSpam", at = @At(value = "HEAD"), cancellable = true)
    private void onDetectRateSpam(CallbackInfo ci) {
        ci.cancel();
    }
}

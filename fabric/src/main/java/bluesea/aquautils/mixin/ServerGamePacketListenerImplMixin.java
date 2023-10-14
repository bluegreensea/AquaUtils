package bluesea.aquautils.mixin;

import bluesea.aquautils.FabricAudience;
import bluesea.aquautils.common.Controller;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.LastSeenMessages;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
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
    @Shadow
    private int chatSpamTickCount;

    public ServerGamePacketListenerImplMixin(MinecraftServer minecraftServer, Connection connection, CommonListenerCookie commonListenerCookie) {
        super(minecraftServer, connection, commonListenerCookie);
    }

    @Inject(at = @At("HEAD"), method = "handleChat")
    private void onChatMessage(ServerboundChatPacket serverboundChatPacket, CallbackInfo ci) {
        boolean kick = Controller.INSTANCE.onPlayerMessage(
            player,
            serverboundChatPacket.message(),
            new FabricAudience(player, player.server.createCommandSourceStack())
        );
        if (kick && !player.hasPermissions(5)) {
            this.disconnect(Component.nullToEmpty("已重複3次(含)以上"));
        }
    }

    @Inject(at = @At("HEAD"), method = "performChatCommand")
    private void onPerformChatCommand(ServerboundChatCommandPacket serverboundChatCommandPacket,
                                    LastSeenMessages lastSeenMessages, CallbackInfo ci) {
        if (this.chatSpamTickCount > 20) {
            this.chatSpamTickCount -= 20;
        }
    }
}

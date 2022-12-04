package bluesea.aquautils.mixin;

import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.OutgoingPlayerChatMessage;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class SentMessageMixin {
    @Mixin(OutgoingPlayerChatMessage.NotTracked.class)
    static class SentMessageProfilelessMixin {
        @Shadow @Final
        private PlayerChatMessage message;

        @Inject(method = "sendToPlayer", at = @At(value = "HEAD"), cancellable = true)
        public void send(ServerPlayer serverPlayer, boolean bl, ChatType.Bound bound, CallbackInfo ci) {
        }
    }
    @Mixin(OutgoingPlayerChatMessage.Tracked.class)
    static class SentMessageChatMixin {
        @Shadow @Final
        private PlayerChatMessage message;

        @Inject(method = "sendToPlayer", at = @At(value = "HEAD"), cancellable = true)
        public void send(ServerPlayer serverPlayer, boolean bl, ChatType.Bound bound, CallbackInfo ci) {
        }
    }
}

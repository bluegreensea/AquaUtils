package bluesea.aquautils.mixin;

import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.OutgoingChatMessage;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class OutgoingPlayerChatMessageMixin {
    @Mixin(OutgoingChatMessage.Disguised.class)
    static class NotTrackedMixin {
        @Shadow @Final
        private Component content;

        @Inject(method = "sendToPlayer", at = @At(value = "HEAD"), cancellable = true)
        public void send(ServerPlayer serverPlayer, boolean bl, ChatType.Bound bound, CallbackInfo ci) {

        }
    }

    @Mixin(OutgoingChatMessage.Player.class)
    static class TrackedMixin {
        @Shadow @Final
        private PlayerChatMessage message;

        @Inject(method = "sendToPlayer", at = @At(value = "HEAD"), cancellable = true)
        public void send(ServerPlayer serverPlayer, boolean bl, ChatType.Bound bound, CallbackInfo ci) {
        }
    }
}

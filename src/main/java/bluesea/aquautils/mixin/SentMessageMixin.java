package bluesea.aquautils.mixin;

import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SentMessage;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.registry.Registry;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class SentMessageMixin {
    @Mixin(SentMessage.Profileless.class)
    static class SentMessageProfilelessMixin {
        @Shadow @Final
        private SignedMessage message;

        @Inject(method = "send", at = @At(value = "HEAD"), cancellable = true)
        public void send(ServerPlayerEntity sender, boolean filterMaskEnabled, MessageType.Parameters params, CallbackInfo ci) {
        }
    }
    @Mixin(SentMessage.Chat.class)
    static class SentMessageChatMixin {
        @Shadow @Final
        private SignedMessage message;

        @Inject(method = "send", at = @At(value = "HEAD"), cancellable = true)
        public void send(ServerPlayerEntity sender, boolean filterMaskEnabled, MessageType.Parameters params, CallbackInfo ci) {
        }
    }
}

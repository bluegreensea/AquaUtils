package bluesea.aquautils;

import com.mojang.brigadier.Command;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.mojang.brigadier.arguments.BoolArgumentType.bool;
import static com.mojang.brigadier.arguments.BoolArgumentType.getBool;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static net.minecraft.commands.arguments.EntityArgument.getPlayers;
import static net.minecraft.commands.arguments.EntityArgument.players;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class AquaUtils implements DedicatedServerModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("Aqua Utils");

    private static final HashMap<Component, String> lastMessage = new HashMap<>();
    private static final HashMap<Component, Integer> lastTimes = new HashMap<>();
    public static Boolean voteLast = false;
    public static Boolean voteReset = false;
    public static String voteLastStr = null;
    public static final HashMap<Component, String> voteStrs = new HashMap<>();
    public static final ArrayList<String> optionStrs = new ArrayList<>(9);
    private static boolean kick;

    @Override
    public void onInitializeServer() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("aquautils.properties");
        AquaUtilsConfig config = AquaUtilsConfig.load(path);
        config.store(path);
        kick = config.kick;
        LOGGER.info("Aqua Utils kick: " + kick);
        
        /*ServerMessageDecoratorEvent.EVENT.register(
            ServerMessageDecoratorEvent.CONTENT_PHASE,
            ((sender, message) -> {
                String tmp = message.getString();
                String[] tmps = tmp.split("~~");
                Text text = Text.of(tmps[0]);
                while (tmps.length >= 3) {
                    text = text.copy().append("§m" + tmps[1]);

                    tmp = tmp.replaceFirst("~~", "").replaceFirst("~~", "");
                    tmps = tmp.split("~~");
                }
                /*if (message.getString().startsWith("~~")) {
                    String messagesp1 = message.getString().replaceFirst("~~", "");
                    if (messagesp1.endsWith("~~")) {
                        messagesp1 = messagesp1.replaceFirst("~~", "");
                        Text text = Text.of("§mtest" + messagesp1 + " :thinking:");
                        return CompletableFuture.completedFuture(text);
                    }
                }*//*
                return CompletableFuture.completedFuture(text);
            })
        );*/

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            if (environment.includeDedicated) {
                dispatcher.register(literal("aquautils").requires(CommandSourceStack::isPlayer)
                    .executes(ctx -> {
                        ctx.getSource().getPlayer().sendSystemMessage(
                            Component.nullToEmpty("version: " + FabricLoader.getInstance().getModContainer("aquautils").get().getMetadata().getVersion().getFriendlyString())
                        );
                        return Command.SINGLE_SUCCESS;
                    })
                    .then(argument("arg", string())
                    .executes(ctx -> {
                        ctx.getSource().getPlayer().sendSystemMessage(
                            Component.nullToEmpty("Aqua Utils kick: " + kick)
                        );

                        return Command.SINGLE_SUCCESS;
                    })
                    .then(argument("switch", bool())
                    .executes(ctx -> {
                        kick = getBool(ctx, "switch");
                        ctx.getSource().getPlayer().sendSystemMessage(
                            Component.nullToEmpty("Aqua Utils set kick: " + kick)
                        );
                        LOGGER.info("set kick: " + kick);

                        return Command.SINGLE_SUCCESS;
                    })))
                );
                dispatcher.register(literal("votelastget").requires(CommandSourceStack::isPlayer)
                    .executes(context -> {
                        context.getSource().getPlayer().sendSystemMessage(
                            Component.nullToEmpty("搶票結果: " + (voteLastStr == null ? "無記錄" : voteLastStr))
                        );

                        return Command.SINGLE_SUCCESS;
                    })
                );
                dispatcher.register(literal("voteget").requires(CommandSourceStack::isPlayer)
                    .executes(context -> {
                        LOGGER.debug(context.getSource().getPlayer().getName().getString() + " use voteget");

                        StringBuilder strings = new StringBuilder();
                        final HashMap<String, Integer> votes = new HashMap<>();
                        voteStrs.forEach((k, v) -> votes.put(v, votes.getOrDefault(v, 0) + 1));
                        AtomicReference<Integer> first = new AtomicReference<>(0);
                        AtomicReference<String> firstStr = new AtomicReference<>("");
                        votes.forEach((k, v) -> {
                            try {
                                Integer.parseInt(k.substring(1));
                                strings.append(k).append(optionStrs.get(Integer.parseInt(k.substring(1)) - 1))
                               .append(" 人數: ").append(v).append("\n");
                                if (v >= first.get()) {
                                    firstStr.set(k);
                                    first.set(v);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
                        voteStrs.forEach((k, v) -> {
                            try {
                                Integer.parseInt(v.substring(1));
                            } catch (Exception ignored) {
                                strings.append(k.getString()).append(" 投了 ").append(v).append("\n");
                            }
                        });

                        strings.insert(0, firstStr + "\n");

                        context.getSource().getPlayer().sendSystemMessage(
                            Component.nullToEmpty("投票結果:\n" + (strings.toString().equals("") ? "無記錄" : strings.toString()))
                        );

                        return Command.SINGLE_SUCCESS;
                    })
                    .then(argument("targets", players())
                    .executes(ctx -> {
                        StringBuilder strings = new StringBuilder();
                        getPlayers(ctx, "targets").forEach(player -> {
                            if (voteStrs.get(player.getName()) != null) {
                                strings.append(player.getName().getString()).append(" 的投票記錄: ")
                            .append(voteStrs.get(player.getName())).append("\n");
                            }
                        });
                        ctx.getSource().getPlayer().sendSystemMessage(
                            Component.nullToEmpty(strings.toString().equals("") ? "無記錄" : strings.toString())
                        );

                        return Command.SINGLE_SUCCESS;
                    }))
                );
                dispatcher.register(literal("votereset").requires(CommandSourceStack::isPlayer)
                    .requires(source -> source.hasPermission(4))
                    .then(argument("message", greedyString())
                    .executes(ctx -> {
                        String options = getString(ctx, "message");
                        StringBuilder strings = new StringBuilder();
                        int i = 0;
                        for (String option : options.split(" ")) {
                            i++;
                            strings.append("§").append(ChatFormatting.YELLOW.getChar())
                            .append("+").append(i)
                            .append("§").append(ChatFormatting.RESET.getChar())
                            .append(" ")
                            .append(option).append(" ");
                            optionStrs.add(option);
                            if (i >= 9) {
                                break;
                            }
                        }
                        voteStrs.clear();
                        voteReset = true;
                        ctx.getSource().getServer().getPlayerList().broadcastSystemMessage(
                            Component.translatable("chat.type.text", ctx.getSource().getPlayer().getDisplayName(), strings.toString()),
                            false
                        );
                        LOGGER.info(ctx.getSource().getPlayer().getName().getString() + " use votes clear");

                        return Command.SINGLE_SUCCESS;
                    }))
                );
            }
        });
    }

    public static Boolean onPlayerMessage(ServerPlayer player, String message) {
        Component index = player.getName();

        //LOGGER.info("Mixin things start");

        if (message.contains("+")) {
            if (message.startsWith("++") && player.hasPermissions(4)) {
                voteLastStr = null;
                voteLast = true;
                LOGGER.info(player.getName().getString() + " start vote");
            } else if (voteLast && message.startsWith("+")) {
                voteLastStr = message;
                voteLast = false;
            } else if (message.substring(message.indexOf("+")).length() >= 2 && voteReset) {
                String value = message.substring(message.indexOf("+"), message.indexOf("+") + 2);
                if (value.toCharArray()[1] >= '\ud800' && value.toCharArray()[1] <= '\ue000') {
                    value = message.substring(message.indexOf("+"), message.indexOf("+") + 3);
                }
                if (voteStrs.get(index) == null || !voteStrs.get(index).equals(value)) {
                    voteStrs.put(index, value);
                    lastMessage.remove(index);
                    //LOGGER.info("put " + index.getString() + " " + value);
                }
            }
        }

        if (!message.equals(lastMessage.getOrDefault(index, ""))) {
            lastMessage.put(index, message);
            lastTimes.put(index, 0);
            //LOGGER.info("put " + index + " " + message);
            //LOGGER.info("times " + lastTimes.get(index));
        } else {
            if (lastTimes.get(index) < 2) {
                lastTimes.put(index, lastTimes.get(index) + 1);
                //LOGGER.info("times " + lastTimes.get(index));
            } else {
                //player.sendMessage(Text.of("§c已重複3次(含)以上"));
                if (kick) {
                    lastTimes.put(index, 0);
                    LOGGER.info(player.getName().getString() + " 已重複3次(含)以上");
                    return true;
                }
            }
        }

        return false;
    }
}

package bluesea.aquautils;

import com.mojang.brigadier.Command;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;

import static com.mojang.brigadier.arguments.BoolArgumentType.bool;
import static com.mojang.brigadier.arguments.BoolArgumentType.getBool;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static net.minecraft.command.argument.EntityArgumentType.getPlayers;
import static net.minecraft.command.argument.EntityArgumentType.players;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class AquaUtils implements DedicatedServerModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("Aqua Utils");

    private static final HashMap<Text, String> lastMessage = new HashMap<>();
    private static final HashMap<Text, Integer> lastTimes = new HashMap<>();
    public static Boolean voteLast = false;
    public static Boolean voteReset = false;
    public static String voteLastStr = null;
    public static final HashMap<Text, String> voteStrs = new HashMap<>();
    public static final ArrayList<String> optionStrs = new ArrayList<>(9);
    private static boolean kick;

    @Override
    public void onInitializeServer() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("aquautils.properties");
        AquaUtilsConfig config = AquaUtilsConfig.load(path);
        config.saveProperties(path);
        kick = config.kick;
        LOGGER.info("Aqua Utils kick: " + kick);
        
        /*ServerMessageDecoratorEvent.EVENT.register(
            ServerMessageDecoratorEvent.CONTENT_PHASE,
            ((sender, message) -> {
                String tmp = message.getString();
                String[] tmps = tmp.split("~~");
                Text text = Text.of(tmps[0]);
                while (tmps.length >= 3) {
                    text = text.copy().append("\u00a7m" + tmps[1]);

                    tmp = tmp.replaceFirst("~~", "").replaceFirst("~~", "");
                    tmps = tmp.split("~~");
                }
                /*if (message.getString().startsWith("~~")) {
                    String messagesp1 = message.getString().replaceFirst("~~", "");
                    if (messagesp1.endsWith("~~")) {
                        messagesp1 = messagesp1.replaceFirst("~~", "");
                        Text text = Text.of("\u00a7mtest" + messagesp1 + " :thinking:");
                        return CompletableFuture.completedFuture(text);
                    }
                }*//*
                return CompletableFuture.completedFuture(text);
            })
        );*/

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
        if (environment.dedicated) {
            dispatcher.register(literal("aquautils").requires(ServerCommandSource::isExecutedByPlayer)
            .executes(ctx -> {
                ctx.getSource().getPlayer().sendMessage(
                    Text.of("version: " + FabricLoader.getInstance().getModContainer("aquautils").get().getMetadata().getVersion().getFriendlyString())
                );
                return Command.SINGLE_SUCCESS;
            })
            .then(argument("arg", string())
            .executes(ctx -> {
                ctx.getSource().getPlayer().sendMessage(
                    Text.of("Aqua Utils kick: " + kick)
                );

                return Command.SINGLE_SUCCESS;
            })
            .then(argument("switch", bool())
            .executes(ctx -> {
                kick = getBool(ctx, "switch");
                ctx.getSource().getPlayer().sendMessage(
                    Text.of("Aqua Utils set kick: " + kick)
                );
                LOGGER.info("set kick: " + kick);

                return Command.SINGLE_SUCCESS;
            })))
            );
            dispatcher.register(literal("votelastget").requires(ServerCommandSource::isExecutedByPlayer)
            .executes(context -> {
                context.getSource().getPlayer().sendMessage(
                    Text.of("搶票結果: " + (voteLastStr == null ? "無記錄" : voteLastStr))
                );

                return Command.SINGLE_SUCCESS;
            })
            );
            dispatcher.register(literal("voteget").requires(ServerCommandSource::isExecutedByPlayer)
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
                        strings.append(k).append(optionStrs.get(Integer.parseInt(k.substring(1))-1))
                               .append(" 人數: ").append(v).append("\n");
                        if (v >= first.get()) {
                            firstStr.set(k);
                            first.set(v);
                        }
                    } catch (Exception ignored) {}
                });
                voteStrs.forEach((k, v) -> {
                    try {
                        Integer.parseInt(v.substring(1));
                    } catch (Exception ignored) {
                        strings.append(k.getString()).append(" 投了 ").append(v).append("\n");
                    }
                });

                strings.insert(0, firstStr + "\n");

                context.getSource().getPlayer().sendMessage(
                    Text.of("投票結果:\n" + (strings.toString().equals("")? "無記錄" : strings.toString()))
                );

                return Command.SINGLE_SUCCESS;
            })
            .then(argument("targets", players()).requires(ServerCommandSource::isExecutedByPlayer)
            .executes(ctx -> {
                StringBuilder strings = new StringBuilder();
                getPlayers(ctx, "targets").forEach(player -> {
                if (voteStrs.get(player.getName()) != null)
                    strings.append(player.getName().getString()).append(" 的投票記錄: ").append(voteStrs.get(player.getName())).append("\n");
                });
                ctx.getSource().getPlayer().sendMessage(
                    Text.of(strings.toString().equals("")? "無記錄" : strings.toString())
                );

                return Command.SINGLE_SUCCESS;
            }))
            );
            dispatcher.register(literal("votereset")
            .requires(source -> source.hasPermissionLevel(4))
            .requires(ServerCommandSource::isExecutedByPlayer)
            .then(argument("message", greedyString())
            .executes(ctx -> {
                String options = getString(ctx, "message");
                StringBuilder Strings = new StringBuilder();
                int i = 0;
                for (String s : options.split(" ")) {
                    i++;
                    Strings.append("\u00a7e+").append(i).append("\u00a7r ").append(s).append(" ");
                    optionStrs.add(s);
                    if (i >= 9) break;
                }
                voteStrs.clear();
                voteReset = true;
                ctx.getSource().getServer().getPlayerManager().broadcast(
                    Text.translatable("chat.type.text", ctx.getSource().getPlayer().getDisplayName(), Strings.toString()),
                    false
                );
                LOGGER.info(ctx.getSource().getPlayer().getName().getString() + " use votes clear");

                return Command.SINGLE_SUCCESS;
            }))
            );
        }
    });
    }

    public static Boolean onPlayerMessage(ServerPlayerEntity player, String message) {
        Text index = player.getName();

        //LOGGER.info("Mixin things start");

        if (message.contains("+")) {
            if (message.startsWith("++") && player.hasPermissionLevel(4)) {
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
                //player.sendMessage(Text.of("\u00a7c已重複3次(含)以上"));
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

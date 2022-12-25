package bluesea.aquautils.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;

public class Controller {
    private static final HashMap<TextComponent, String> lastMessage = new HashMap<>();
    private static final HashMap<TextComponent, Integer> lastTimes = new HashMap<>();
    public static Boolean voteReset = false;
    public static final HashMap<TextComponent, String> voteStrs = new HashMap<>();
    public static final ArrayList<String> optionStrs = new ArrayList<>(9);
    private static boolean kick = true;

    public static void setkick(Boolean kick) {
        Controller.kick = kick;
    }

    public static void onCommand(Audience players, Audience player, String command, String[] args) {
        switch (command) {
            case "aquautils" -> player.sendMessage(Component.text("version: " + "${version}"));
            case "voteget" -> voteget(player);
            case "votereset" -> votereset(players, player, args);
        }
    }

    private static void voteget(Audience player) {
        TextComponent textComponent = Component.empty();
        final HashMap<String, Integer> allVotes = new HashMap<>();
        voteStrs.forEach((k, v) -> allVotes.put(v, allVotes.getOrDefault(v, 0) + 1));
        Integer firstVotes = 0;
        StringBuilder firstStr = new StringBuilder();
        for (HashMap.Entry<String, Integer> entry : allVotes.entrySet()) {
            String voteStr = entry.getKey();
            Integer votes = entry.getValue();
            try {
                Integer.parseInt(voteStr.substring(1));
            } catch (Exception e) {
                continue;
            }
            textComponent = textComponent
                .append(Component.newline())
                .append(Component.text(voteStr).color(NamedTextColor.YELLOW).appendSpace())
                .append(Component.text(optionStrs.get(Integer.parseInt(voteStr.substring(1)) - 1)))
                .append(Component.text(" 人數: " + votes));
            if (firstVotes.equals(votes)) {
                firstStr.append(", ").append(optionStrs.get(Integer.parseInt(voteStr.substring(1)) - 1));
            } else if (votes >= firstVotes) {
                firstStr = new StringBuilder(optionStrs.get(Integer.parseInt(voteStr.substring(1)) - 1));
                firstVotes = votes;
            }
        }
        for (Map.Entry<TextComponent, String> entry : voteStrs.entrySet()) {
            try {
                Integer.parseInt(entry.getValue().substring(1));
            } catch (Exception ignored) {
                textComponent = textComponent
                    .append(Component.newline())
                    .append(Component.text(entry.getKey().content() + " 投了 " + entry.getValue()));
            }
        }

        //strings.insert(0, firstStr + "\n");
        textComponent = Component.text(firstStr.toString()).append(textComponent);

        player.sendMessage(
            //Component.text("投票結果: " + (strings.toString().equals("\n") ? "無記錄" : strings.toString()))
            Component.text("投票結果: ")
                .append((textComponent.content().equals("") ? Component.text("無記錄") : textComponent))
        );
    }

    private static void votereset(Audience players, Audience player, String[] args) {
        TextComponent textComponent = Component.empty();
        int i = 0;
        for (String option : args) {
            i++;
            textComponent = textComponent
                .append(Component.text("+" + i).color(NamedTextColor.YELLOW))
                .append(Component.space())
                .append(Component.text(option + " "));
            optionStrs.add(option);
            if (i >= 9) {
                break;
            }
        }
        voteStrs.clear();
        voteReset = true;
        if (args.length > 0) {
            players.sendMessage(
                (player.get(Identity.DISPLAY_NAME).isPresent())
                    ? Component.translatable("chat.type.text", player.get(Identity.DISPLAY_NAME).get(), textComponent)
                    : Component.translatable("chat.type.text", Component.text("[Server]"), textComponent)
            );
        }
    }

    public static Boolean onPlayerMessage(Audience player, String message) {
        if (player.get(Identity.DISPLAY_NAME).isPresent()) {
            TextComponent index = (TextComponent) player.get(Identity.DISPLAY_NAME).get();

            if (message.contains("+")) {
                if (message.substring(message.indexOf("+")).length() >= 2 && voteReset) {
                    String value =
                        message.substring(message.indexOf("+"), message.indexOf("+") + 2);
                    if (value.toCharArray()[1] >= '\ud800' && value.toCharArray()[1] <= '\ue000') {
                        value = message.substring(message.indexOf("+"), message.indexOf("+") + 3);
                    }
                    if (voteStrs.get(index) == null || !voteStrs.get(index).equals(value)) {
                        voteStrs.put(index, value);
                        lastMessage.remove(index);
                    }
                }
            }

            if (!message.equals(lastMessage.getOrDefault(index, ""))) {
                lastMessage.put(index, message);
                lastTimes.put(index, 0);
            } else {
                if (lastTimes.get(index) < 2) {
                    lastTimes.put(index, lastTimes.get(index) + 1);
                } else {
                    if (kick) {
                        lastTimes.put(index, 0);
                        return true;
                    }
                }
            }
        }

        return false;
    }
}

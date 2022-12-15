package bluesea.aquautils.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;

public class Processor {
    private static final HashMap<UUID, String> lastMessage = new HashMap<>();
    private static final HashMap<UUID, Integer> lastTimes = new HashMap<>();
    public static Boolean voteLast = false;
    public static Boolean voteReset = false;
    public static String voteLastStr = null;
    public static final HashMap<UUID, String> voteStrs = new HashMap<>();
    public static final ArrayList<String> optionStrs = new ArrayList<>(9);
    private static boolean kick;

    public static void onCommand(Audience player, String command, String[] arguments) {
        player.sendMessage(Component.text("version: " + "${version}"));
    }

    public static Boolean onPlayerMessage(Audience audience, String message) {
        UUID index = audience.get(Identity.UUID).get();

        if (message.contains("+")) {
            if (message.startsWith("++") /*&& player.hasPermissions(4)*/) {
                voteLastStr = null;
                voteLast = true;
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

        return false;
    }
}

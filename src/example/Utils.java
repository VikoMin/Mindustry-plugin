package example;

import arc.Events;
import arc.struct.Seq;
import arc.util.Strings;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.gen.Groups;
import mindustry.gen.Player;

public class Utils {
    public static Seq<String> mutedPlayers = new Seq<>();
    public static void loadMute() {
        Vars.netServer.admins.addChatFilter((player, s) -> {
            if (mutedPlayers.contains(player.uuid())) {
                player.sendMessage("[#ff]You are muted!");
                return null;
            } else return s;
        });
        Events.on(EventType.PlayerLeave.class, it ->{
            if (mutedPlayers.contains(it.player.uuid())) {
                unmute(it.player.uuid());
            }
        });
    }

    public static void unmute(String uuid) {
        mutedPlayers.remove(uuid);
    }

    public static void mute(String uuid) {
        mutedPlayers.add(uuid);

    }
    public static Player findPlayer(String data) {
        return Groups.player.find(player -> player.plainName().contains(Strings.stripColors(data)) || player.uuid().equals(data) || player.ip().equals(data));
    }

    public static Seq<String> getFormattedData(String name) {
        return Seq.with(Vars.netServer.admins.findByName(name).toSeq().map(data -> "[stat]UUID: [] " + data.id
                + " | [stat]Name:[] " + data.lastName + " | [stat]IP:[white] " + data.lastIP + " | " + (data.admin?"[acid]":"[#df0b0c]") +"Admin: "
                + data.admin + " [white]| " + (data.banned?"[#ff]":"[lime]")  + "Banned: " + data.banned + "\n"));
    }

    public static boolean isMuted(String uuid) {return mutedPlayers.contains(uuid);}
}

package example;

import static example.Emoji.*;
import static mindustry.Vars.*;

import java.awt.Color;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Pattern;

import arc.Core;
import arc.Events;
import arc.math.Mathf;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import arc.util.CommandHandler;
import arc.util.CommandHandler.Command;
import arc.util.Log;
import arc.util.Strings;
import arc.util.Timer;
import mindustry.content.Blocks;
import mindustry.content.Items;
import mindustry.content.Liquids;
import mindustry.content.StatusEffects;
import mindustry.content.UnitTypes;
import mindustry.game.EventType.GameOverEvent;
import mindustry.game.EventType.PlayerJoin;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.gen.Unit;
import mindustry.maps.Map;
import mindustry.net.Administration.PlayerInfo;
import mindustry.net.Packets.KickReason;
import mindustry.type.Item;
import mindustry.type.Liquid;
import mindustry.type.UnitType;
import rhino.Context;
import rhino.NativeJavaObject;
import rhino.Scriptable;
import rhino.Undefined;

public class CommandsManager {
    public static ArrayList<String> adminCommands;

    public static SkipmapVoteSession currentlyMapSkipping = null;
    public static SkipwaveVoteSession currentlyWaveSkipping = null;
    public int waves = 0;
    public static ArrayList<String> extraStarsUIDD;
    public static boolean chatFilter;
    public static String discordLink = "";

    private static Team admin;

    private Thread jsThread;

    private boolean isServerAttackModeEnabled;
    private Seq<String> serverAttackModeRegex;

    public void init() {
        discordLink = Core.settings.getString(ExamplePlugin.PLUGIN_NAME + "-discord-link", null);
        chatFilter = Core.settings.getBool(ExamplePlugin.PLUGIN_NAME + "-chat-filter", false);
        isServerAttackModeEnabled = false;
        serverAttackModeRegex = new Seq<String>();
        extraStarsUIDD = new ArrayList<>();

        admin = Team.all[10];
        admin.name = "admin";

        adminCommands = new ArrayList<>();
        adminCommands.add("fillitems");
        adminCommands.add("dct");
        adminCommands.add("team");
        adminCommands.add("sandbox");
        adminCommands.add("unit");
        adminCommands.add("vanish");
        adminCommands.add("remove");
        adminCommands.add("bans");
        adminCommands.add("ban");
        adminCommands.add("unban");
        adminCommands.add("m");
        adminCommands.add("js");
        adminCommands.add("stats");
        adminCommands.add("stopjs");
        adminCommands.add("link");
        adminCommands.add("setdiscord");
        adminCommands.add("pardon");

        Events.on(PlayerJoin.class, e -> {
            if (e.player != null) checkPlayer(e.player);
        });
    }

    private void checkPlayer(Player player) {
        if (player.admin) return;
        if (!isServerAttackModeEnabled) return;
        String name = player.plainName().replaceAll(" ", "_");
        for (int i = 0; i < serverAttackModeRegex.size; i++) {
            if (Pattern.compile(serverAttackModeRegex.get(i)).matcher(name).find()) {
                player.kick(KickReason.serverClose, (5 * 60) * 1000); // 5 minutes
                Log.info(player.uuid() + " (" + name + ") was banned on 5 min by " + serverAttackModeRegex.get(i) + " reg");
                return;
            }
        }
    }

    public void registerClientCommands(CommandHandler handler) {
        handler.removeCommand("help");

        handler.<Player>register("help", "[страница]", "Список всех команд", (args, player) -> {
            boolean isAdmin = player.admin();
            int coummandsCount = handler.getCommandList().size;
            if (!isAdmin) coummandsCount -= adminCommands.size();

            if (args.length > 0 && !Strings.canParseInt(args[0])) {
                player.sendMessage("[scarlet]\"страница\" может быть только числом.");
                return;
            }
            final int commandsPerPage = 8;
            int page = args.length > 0 ? Strings.parseInt(args[0]) : 1;
            int pages = Mathf.ceil((float) coummandsCount / commandsPerPage);

            page--;

            if (page >= pages || page < 0) {
                player.sendMessage("[scarlet]\"страница\" должна быть числом между[orange] 1[] и[orange] " + pages + "[scarlet].");
                return;
            }

            StringBuilder result = new StringBuilder();
            result.append(Strings.format("[orange]-- Страница команд[lightgray] @[gray]/[lightgray]@[orange] --\n\n", (page + 1), pages));

            for (int i = commandsPerPage * page; i < Math.min(commandsPerPage * (page + 1), handler.getCommandList().size); i++) {
                Command command = handler.getCommandList().get(i);
                boolean isAdminCommand = adminCommands.contains(command.text);
                if (isAdminCommand && !isAdmin) continue;
                result.append("[orange] /").append(command.text).append("[white] ").append(command.paramText).append("[lightgray] - ").append(command.description + (isAdminCommand ? " [red] Только для администраторов" : "")).append("\n");
            }
            player.sendMessage(result.toString());
        });

        registerPlayersCommands(handler);
        registerAdminCommands(handler);

    }
    public void registerAdminCommands(CommandHandler handler) {

        handler.<Player>register("fillitems", "[item] [count]", "Заполните ядро предметами", (arg, player) -> {
            if (player.admin()) {
                try {
                    final Item[] serpuloItems = {Items.scrap, Items.copper, Items.lead, Items.graphite, Items.coal, Items.titanium, Items.thorium, Items.silicon, Items.plastanium, Items.phaseFabric, Items.surgeAlloy, Items.sporePod, Items.sand, Items.blastCompound, Items.pyratite, Items.metaglass};

                    final Item[] erekirOnlyItems = {Items.beryllium, Items.tungsten, Items.oxide, Items.carbide, Items.fissileMatter, Items.dormantCyst};

                    if (arg.length == 0) {
                        StringBuilder ruNames = new StringBuilder("Русские названия предметов: ");
                        for (int i = 0; i < serpuloItems.length; i++) {
                            ruNames.append(GameWork.getColoredLocalizedItemName(serpuloItems[i]));
                            ruNames.append(", ");
                        }
                        for (int i = 0; i < erekirOnlyItems.length; i++) {
                            ruNames.append(GameWork.getColoredLocalizedItemName(erekirOnlyItems[i]));
                            if (i + 1 < erekirOnlyItems.length) {
                                ruNames.append(", ");
                            }
                        }
                        player.sendMessage(ruNames.toString());
                        return;
                    }

                    Item item = null;
                    String itemname = arg[0].toLowerCase();

                    for (int i = 0; i < serpuloItems.length; i++) {
                        Item si = serpuloItems[i];
                        if (itemname.equalsIgnoreCase(si.name) || itemname.equalsIgnoreCase(si.localizedName)) {
                            item = si;
                            break;
                        }
                    }
                    if (item == null) {
                        for (int i = 0; i < erekirOnlyItems.length; i++) {
                            Item ei = erekirOnlyItems[i];
                            if (itemname.equalsIgnoreCase(ei.name) || itemname.equalsIgnoreCase(ei.localizedName)) {
                                item = ei;
                                break;
                            }
                        }
                    }
                    if (item == null) {
                        item = switch (itemname) {
                            case "\uf82a" -> Items.blastCompound;
                            case "\uf833" -> Items.coal;
                            case "\uf838" -> Items.copper;
                            case "\uf835" -> Items.graphite;
                            case "\uf837" -> Items.lead;
                            case "\uf836" -> Items.metaglass;
                            case "\uf82d" -> Items.phaseFabric;
                            case "\uf82e" -> Items.plastanium;
                            case "\uf829" -> Items.pyratite;
                            case "\uf834" -> Items.sand;
                            case "\uf830" -> Items.scrap;
                            case "\uf82f" -> Items.silicon;
                            case "\uf82b" -> Items.sporePod;
                            case "\uf82c" -> Items.surgeAlloy;
                            case "\uf831" -> Items.thorium;
                            case "\uf832" -> Items.titanium;
                            default -> item;
                        };
                    }               if (item == null) {
                        if (itemname.equalsIgnoreCase(Items.dormantCyst.name) || itemname.equalsIgnoreCase(Items.dormantCyst.localizedName)) {
                            item = Items.dormantCyst;
                        }
                        if (itemname.equalsIgnoreCase(Items.fissileMatter.name) || itemname.equalsIgnoreCase(Items.fissileMatter.localizedName)) {
                            item = Items.fissileMatter;
                        }
                    }
                    if (item != null) {
                        Team team = player.team();

                        int count = arg.length > 1 ? Integer.parseInt(arg[1]) : 0;

                        if (team.cores().size == 0) {
                            player.sendMessage("[red]У Вашей команды игроков нет ядер");
                            return;
                        }
                        team.cores().get(0).items.add(item, count);
                        player.sendMessage("Добавлено " + "[gold]x" + count + " [orange]" + item.name);
                    } else {
                        player.sendMessage("Предмет не найден");
                    }
                } catch (Exception e) {
                    player.sendMessage(e.getMessage());
                }
            }
        });

        handler.<Player>register("dct", "[time]", "Установить интервал (секунд/10) обновлений данных", (arg, player) -> {
            if (player.admin()) {
                if (arg.length == 0) {
                    player.sendMessage("Интервал обновлений: " + ExamplePlugin.dataCollect.getSleepTime() + " секунд/10");
                    return;
                }
                if (arg.length == 1) {
                    long count = 0;
                    try {
                        count = Long.parseLong(arg[0]);
                    } catch (Exception e) {
                        player.sendMessage("[red]Вводить можно только числа!");
                    }
                    count *= 1_00;

                    if (count <= 0) {
                        player.sendMessage("[red]Интервал не может быть меньше 1!");
                    }
                    ExamplePlugin.dataCollect.setSleepTime(count);
                    player.sendMessage("Установлен интервал: " + count + " ms");
                }
            } else {
                player.sendMessage("[red]Команда только для администраторов");
            }
        });

        handler.<Player>register("team", "[player] [team]", "Установить команду для игрока", (arg, player) -> {
            if (player.admin()) {
                if (arg.length < 1) {
                    StringBuilder teams = new StringBuilder();
                    for (int i = 0; i < Team.baseTeams.length; i++) {
                        teams.append(Team.baseTeams[i].name);
                        teams.append(", ");
                    }
                    for (int i = 0; i < Team.all.length; i++) {
                        teams.append(Team.all[i].name);
                        if (i != Team.all.length - 1) teams.append(", ");
                    }
                    player.sendMessage("Команды:\n" + teams.toString());
                }
                if (arg.length == 1) {
                    Player targetPlayer = Groups.player.find(p -> Strings.stripColors(p.name()).equalsIgnoreCase(Strings.stripColors(arg[0])));
                    if (targetPlayer == null) {
                        player.sendMessage("[red]Игрок не найден");
                        return;
                    }
                    player.sendMessage("Игрок состоить в команде: " + targetPlayer.team().name);
                    return;
                }
                if (arg.length == 2) {
                    Player targetPlayer = Groups.player.find(p -> Strings.stripColors(p.name()).equalsIgnoreCase(Strings.stripColors(arg[0])));
                    if (targetPlayer == null) {
                        player.sendMessage("[red]Игрок не найден");
                        return;
                    }
                    player.sendMessage("Игрок состоить в команде: " + targetPlayer.team().name);

                    Team team = null;
                    String targetTeam = arg[1].toLowerCase();
                    for (int i = 0; i < Team.baseTeams.length; i++) {
                        if (Team.baseTeams[i].name.equals(targetTeam.toLowerCase())) {
                            team = Team.baseTeams[i];
                        }
                    }
                    for (int i = 0; i < Team.all.length; i++) {
                        if (Team.all[i].name.equals(targetTeam.toLowerCase())) {
                            team = Team.all[i];
                        }
                    }
                    if (team == null) {
                        player.sendMessage("[red]Команда не найдена");
                    } else {
                        targetPlayer.team(team);
                        if (team.name.equals(Team.crux.name)) {
                            Log.info("crux");
                            targetPlayer.unit().healTime(.01f);
                            targetPlayer.unit().healthMultiplier(100);
                            targetPlayer.unit().maxHealth(1000f);
                            targetPlayer.unit().apply(StatusEffects.invincible, Float.MAX_VALUE);
                        }
                        if (team.name.equals(admin.name)) {
                            targetPlayer.unit().healTime(.01f);
                            targetPlayer.unit().healthMultiplier(100);
                            targetPlayer.unit().maxHealth(1000f);
                            targetPlayer.unit().hitSize(0);
                            targetPlayer.unit().apply(StatusEffects.invincible, Float.MAX_VALUE);

                            admin.rules().infiniteResources = true;
                            admin.rules().cheat = true;
                            admin.rules().infiniteAmmo = true;
                            admin.rules().blockDamageMultiplier = Float.MAX_VALUE;
                            admin.rules().blockHealthMultiplier = Float.MAX_VALUE;
                            admin.rules().buildSpeedMultiplier = 100;
                            admin.rules().unitDamageMultiplier = Float.MAX_VALUE;
                        }
                        player.sendMessage("Игрок " + targetPlayer.name() + " отправлен в команду [#" + team.color + "]" + team.name);
                        targetPlayer.sendMessage("Вы отправлены в команду [#" + team.color + "]" + team.name);
                    }

                    return;
                }
            } else {
                player.sendMessage("[red]Команда только для администраторов");
            }
        });

        handler.<Player>register("sandbox", "[on/off] [team]", "Бесконечные ресурсы", (arg, player) -> {
            if (player.admin()) {
                if (arg.length == 0) {
                    player.sendMessage("[gold]infiniteResources: [gray]" + state.rules.infiniteResources);

                } else {
                    Team team = null;
                    if (arg.length == 2) {
                        String targetTeam = arg[1].toLowerCase();
                        for (int i = 0; i < Team.baseTeams.length; i++) {
                            if (Team.baseTeams[i].name.equals(targetTeam.toLowerCase())) {
                                team = Team.baseTeams[i];
                            }
                        }
                        for (int i = 0; i < Team.all.length; i++) {
                            if (Team.all[i].name.equals(targetTeam.toLowerCase())) {
                                team = Team.all[i];
                            }
                        }
                    }

                    if (arg[0].equals("on")) {
                        if (team == null) {
                            state.rules.infiniteResources = true;
                            player.sendMessage("[green]Включено!");
                        } else {
                            team.rules().infiniteResources = true;
                            player.sendMessage("[green]Включено для команды [#" + team.color + "]" + team.name);
                        }
                        Call.setRules(player.con, state.rules);
                    } else if (arg[0].equals("off")) {
                        if (team == null) {
                            state.rules.infiniteResources = false;
                            player.sendMessage("[red]Выключено!");
                        } else {
                            team.rules().infiniteResources = false;
                            player.sendMessage("[red]Выключено для команды [#" + team.color + "]" + team.name);
                        }
                        Call.setRules(player.con, state.rules);
                    } else {
                        player.sendMessage("[red]Только on/off");
                    }
                }
            } else {
                player.sendMessage("[red]Команда только для администраторов");
            }
        });

        handler.<Player>register("unit", "[type] [inPlayer]", "Создает юнита, list для списка", (arg, player) -> {
            if (player.admin()) {
                String unitType = UnitTypes.gamma.name;
                Field[] fields = UnitTypes.class.getFields();

                if (arg.length > 0) {
                    if (arg[0].equals("list")) {
                        StringBuilder unitTypes = new StringBuilder("Типы юнитов");
                        for (int i = 0; i < fields.length; i++) {
                            String name = fields[i].getName();
                            if (name.equals(UnitTypes.block.name)) continue;

                            unitTypes.append(fields[i].getName());
                            if (i + 1 != fields.length) {
                                unitTypes.append(", ");
                            }
                        }
                        player.sendMessage(unitTypes.toString());
                        return;
                    }
                    unitType = arg[0];
                }
                try {
                    for (int i = 0; i < fields.length; i++) {
                        if (fields[i].getName().equals(unitType)) {
                            UnitType ut = (UnitType) fields[i].get(UnitTypes.class);
                            if (ut == null) continue;
                            if (ut.name.equals(UnitTypes.block.name)) {
                                continue;
                            }
                            Unit u = ut.spawn(player.team(), player.mouseX, player.mouseY);
                            if (arg.length > 1) {
                                if (arg[1].equals("true") || arg[1].equals("y") || arg[1].equals("t") || arg[1].equals("yes")) {
                                    player.unit(u);
                                }
                            }
                            player.sendMessage("Готово!");

                            if (!net.client()) {
                                u.add();
                            }
                            return;
                        }
                    }
                    player.sendMessage("[red]Юнит не найден [gold]/unit list");
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    e.printStackTrace();
                    player.sendMessage(e.getLocalizedMessage());
                }
            }
        });

        handler.<Player>register("vanish", "<name>", "make person in derelict(gray) team", (arg, player) -> {
            if (!player.admin) {
                player.sendMessage("[scarlet]You must be an admin");
            } else {
                Player targetPlayer = Groups.player.find(p -> Strings.stripColors(p.name()).equalsIgnoreCase(Strings.stripColors(arg[0])));
                targetPlayer.team(Team.derelict);
                targetPlayer.sendMessage("[scarlet]You has been moved to gray team");
                player.sendMessage("[yellow]WARN[white]: if player rejoin the game he won't be vanished");
            }
        });

        handler.<Player>register("remove", "<name>", "remove a person", (arg, player) -> {
            if (!player.admin) {
                player.sendMessage("[scarlet]You must be an admin");
            } else {
                Player targetPlayer = Groups.player.find(p -> Strings.stripColors(p.name()).equalsIgnoreCase(Strings.stripColors(arg[0])));
                targetPlayer.sendMessage("[scarlet]You has been removed");
                targetPlayer.remove();
                player.sendMessage("[yellow]WARN[white]: if player rejoin the game he will added back");
            }
        });

        handler.<Player>register("unban", "<ip/ID>", "Completely unban a person by IP or ID.", (arg, player) -> {
            if (player.admin()) {
                if (arg[0].equals("all")) {
                    player.sendMessage("[scarlet]Not allowed");
                } else {
                    if (netServer.admins.unbanPlayerIP(arg[0]) || netServer.admins.unbanPlayerID(arg[0])) {
                        player.sendMessage("[gold]Unbanned player: [white]" + arg[0]);
                    } else {
                        player.sendMessage("[red]That IP/ID is not banned!");
                    }
                }
            }
        });

        handler.<Player>register("ban", "<typeid/typeip> <ID/IP> [time_in_h]", "ban a person by IP or ID. /ban (ip/id) (ip/id of player)", (arg, player) -> {
            if (player.admin()) {
                if (arg[0].contains("id")) {
                    if (arg.length > 2) {
                        PlayerData data = PlayerData.getData(arg[1].toString());
                        long time;
                        try {
                            time = Long.parseLong(arg[2]);
                        } catch (NumberFormatException e) {
                            player.sendMessage("[red]Time is invalid.");
                            return;
                        }
                        data.ban(System.currentTimeMillis() + 3600000 * time);
                        data.save();
                    } else netServer.admins.banPlayerID(arg[1].toString());
                    player.sendMessage("[gold]Banned player: [white]" + arg[1]);
                } else if (arg[0].contains("ip")) {
                    if (arg.length > 2) {
                        PlayerData data = PlayerData.getData(netServer.admins.findByIP(arg[1].toString()).id);
                        long time;
                        try {
                            time = Long.parseLong(arg[2]);
                        } catch (NumberFormatException e) {
                            player.sendMessage("[red]Time is invalid.");
                            return;
                        }
                        data.ban(System.currentTimeMillis() + 3600000 * time);
                        data.save();
                    } else netServer.admins.banPlayerIP(arg[1]);
                    player.sendMessage("[gold]Banned player: [white]" + arg[1]);
                } else {
                    player.sendMessage("[scarlet]type must be ip or id![gold] Examples:[accent]/ban ip 127.0.0.1 , /ban id AAAaaa123789AAAaaa0== " + arg[0] + " " + arg[1]);
                }
            }
        });

        handler.<Player>register("bans", "List all banned IPs and IDs.", (arg, player) -> {
            if (player.admin()) {
                Thread thread = new Thread(() -> {
                    Seq<PlayerInfo> bans = netServer.admins.getBanned();

                    if (bans.size == 0) {
                        player.sendMessage("No ID-banned players have been found.");
                    } else {
                        player.sendMessage("Banned players [ID]:");
                        for (PlayerInfo info : bans) {
                            player.sendMessage(" " + info.id + " / Last known name: [gold]" + info.plainLastName());
                            try {
                                Thread.sleep(10);
                            } catch (InterruptedException e) {
                                Log.err(e.toString());
                            }
                        }
                    }

                    Seq<String> ipbans = netServer.admins.getBannedIPs();

                    if (ipbans.size == 0) {
                        player.sendMessage("No IP-banned players have been found.");
                    } else {
                        player.sendMessage("Banned players [IP]:");
                        for (String string : ipbans) {
                            PlayerInfo info = netServer.admins.findByIP(string);
                            if (info != null) {
                                player.sendMessage(" " + string + "   / Last known name: [gold]" + info.plainLastName() + "[] / ID: " + info.id);
                            } else {
                                player.sendMessage(" " + string + "   (No known name or info)");
                            }
                            try {
                                Thread.sleep(10);
                            } catch (InterruptedException e) {
                                Log.err(e.toString());
                            }
                        }
                    }
                    Thread.currentThread().stop();
                });
                thread.start();
            }
        });

        handler.<Player>register("stats", "[uuid]", "show your or stats of uuid", (arg, player) -> {
            if (player.admin()) {
                if (arg.length > 0) {
                    if (arg[0].endsWith("==")) {
                        PlayerData data = PlayerData.getData(arg[0]);
                        if (data == null) {
                            player.sendMessage("[red]UUID does not exist.");
                            return;
                        }
                        PlayerInfo info = netServer.admins.findByName(arg[0]).first();
                        player.sendMessage("Waves Survived: [gold]" + data.getWaves());
                        player.sendMessage("Blocks builded: [gold]" + data.getBuilded());
                        player.sendMessage("Blocks destroyed: [gold]" + data.getDestroyed());
                        player.sendMessage("Times joined: [gold]" + info.timesJoined);
                        player.sendMessage("Times kicked: [gold]" + info.timesKicked);
                        player.sendMessage("Last IP: [gold]" + info.lastIP);
                        player.sendMessage("Last name: [gold]" + info.plainLastName());
                        player.sendMessage("Banned: [gold]" + info.banned);
                        return;
                    }
                }
            }
            PlayerData data = PlayerData.getData(player.uuid());
            PlayerInfo info = netServer.admins.findByName(player.uuid()).first();
            player.sendMessage("Waves Survived: [gold]" + data.getWaves());
            player.sendMessage("Blocks builded: [gold]" + data.getBuilded());
            player.sendMessage("Blocks destroyed: [gold]" + data.getDestroyed());
            player.sendMessage("Times joined: [gold]" + info.timesJoined);
            player.sendMessage("UUID: [gold]" + player.uuid());
            return;
        });

        handler.<Player>register("js", "<script...>", "Запустить JS", (arg, player) -> {
            if (player.admin() && PlayerData.getData(player.uuid()).getJs()) {
                Scriptable scope = mods.getScripts().scope;
                jsThread = new Thread(() -> {
                    String out = null;
                    Context context = Context.enter();
                    try {
                        Object o = context.evaluateString(scope, arg[0], "console.js", 1);
                        NativeJavaObject n = new NativeJavaObject();
                        if (o instanceof NativeJavaObject)
                            o = n.unwrap();
                        if (o == null)
                            o = "null";
                        else if (o instanceof Undefined)
                            o = "undefined";
                        out = o.toString();
                        if (out == null) {
                            out = "null";
                        }
                    } catch (Throwable t) {
                        out = t.getClass().getSimpleName() + (t.getMessage() == null ? "" : ": " + t.getMessage());
                    }
                    ;
                    Context.exit();
                    player.sendMessage("[gold]" + out);
                    Thread.currentThread().stop();
                }, "js");
                jsThread.start();
            }
        });

        handler.<Player>register("stopjs", "Остановить весь JS", (arg, player) -> {
            if (player.admin() && PlayerData.getData(player.uuid()).getJs()) {
                Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
                Iterator<Thread> iterator = threadSet.iterator();
                int count = 0;
                while (iterator.hasNext()) {
                    Thread next = iterator.next();
                    if (next.getName() == "js") {
                        next.stop();
                        count++;
                    }
                }
                player.sendMessage(String.format("[gold]Stopped @ JS threads", count));
            }
        });

        handler.<Player>register("link", "<link> [player]", "Отправить ссылку всем/игроку", (arg, player) -> {
            if (player.admin()) {
                if (arg.length == 1) {
                    Call.openURI(arg[0]);
                } else if (arg.length == 2) {
                    Player targetPlayer = Groups.player.find(p -> Strings.stripColors(p.name()).equalsIgnoreCase(Strings.stripColors(arg[1])));
                    if (targetPlayer != null) {
                        Call.openURI(targetPlayer.con, arg[0]);
                        player.sendMessage("[gold]Готово!");
                    } else {
                        player.sendMessage("[red]Игрок не найден");
                    }
                }
            }
        });

        handler.<Player>register("setdiscord", "[link]", "\ue80d Сервера", (arg, player) -> {
            if (player.admin()) {
                if (arg.length == 1) {
                    discordLink = arg[0];
                    Core.settings.put(ExamplePlugin.PLUGIN_NAME + "-discord-link", discordLink);
                    player.sendMessage("[gold]\ue80d Готово!");
                }
            }
        });

        handler.<Player>register("pardon", "<ID> [index]", "Прощает игрока по ID и позволяет ему присоединиться снова.", (arg, player) -> {
            if (player.admin()) {

                int index = 0;
                if (arg.length >= 2) {
                    try {
                        index = Integer.parseInt(arg[1]);
                    } catch (Exception e) {
                        player.sendMessage("[red]" + e.getMessage());
                        return;
                    }
                }
                Seq<PlayerInfo> infos = netServer.admins.findByName(arg[0]).toSeq();
                if (index < 0) index = 0;
                if (index >= infos.size) index = infos.size - 1;
                PlayerInfo info = infos.get(index);

                if (info != null) {
                    info.lastKicked = 0;
                    netServer.admins.kickedIPs.remove(info.lastIP);
                    player.sendMessage("Pardoned player: " + info.plainLastName() + " [lightfray](of " + infos.size + " find)");
                } else {
                    player.sendMessage("[red]That ID can't be found");
                }
            }
        });
    }

    public void registerPlayersCommands(CommandHandler handler) {
        /**
         * List of server maps
         */
        handler.<Player>register("maps", "[all/custom/default]", "Показывает список доступных карт. Отображает все карты по умолчанию", (arg, player) -> {

            String types = "all";

            if (arg.length == 0) {
                types = maps.getShuffleMode().name();
            } else {
                types = arg[0];
            }

            boolean custom = types.equals("custom") || types.equals("all");
            boolean def = types.equals("default") || types.equals("all");

            if (!maps.all().isEmpty()) {
                Seq<Map> all = new Seq<>();

                if (custom) all.addAll(maps.customMaps());
                if (def) all.addAll(maps.defaultMaps());

                if (all.isEmpty()) {
                    player.sendMessage("Кастомных карт нет на этом сервере, используйте [gold]all []аргумет.");
                } else {
                    player.sendMessage("[white]Maps:");
                    for (Map map : all) {
                        String mapName = Strings.stripColors(map.name()).replace(' ', '_');
                        if (map.custom) {
                            player.sendMessage(" [gold]Кастомная [white]| " + mapName + " (" + map.width + "x" + map.height + ")");
                        } else {
                            player.sendMessage(" [gray]Дефолтная [white]| " + mapName + " (" + map.width + "x" + map.height + ")");
                        }
                    }
                }
            } else {
                player.sendMessage("Карты не найдены");
            }
        });

        handler.<Player>register("discord", "", "\ue80d Сервера", (arg, player) -> {
            if (discordLink == null) {
                player.sendMessage("[red]\ue80d Ссылка отсутствует");
            } else {
                if (discordLink.isEmpty()) {
                    player.sendMessage("[red]\ue80d Ссылка отсутствует");
                } else {
                    Call.openURI(player.con, discordLink);
                }
            }
        });

        /**
         * Map recourses statistic
         */
        handler.<Player>register("mapinfo", "Показывает статистику ресурсов карты", (arg, player) -> {

            final Item itemDrops[] = new Item[]{Items.copper, Items.lead, Items.scrap, Items.sand, Items.coal, Items.titanium, Items.thorium};

            final Liquid liquidDrops[] = new Liquid[]{Liquids.water, Liquids.oil, Liquids.slag, Liquids.cryofluid};

            int counter[] = new int[itemDrops.length];
            int lcounter[] = new int[liquidDrops.length];

            int summaryCounter = 0;
            int typesCounter = 0;

            for (int x = 0; x < world.width(); x++) {
                for (int y = 0; y < world.height(); y++) {
                    if (world.tile(x, y).block() != Blocks.air) continue;
                    Item floor = world.tile(x, y).floor().itemDrop;
                    Item overlay = world.tile(x, y).overlay().itemDrop;
                    Liquid lfloor = world.tile(x, y).floor().liquidDrop;
                    Liquid lowerLayer = world.tile(x, y).overlay().liquidDrop;

                    for (int i = 0; i < counter.length; i++) {
                        if (itemDrops[i] == overlay || itemDrops[i] == floor) {
                            if (counter[i] == 0) {
                                typesCounter++;
                            }
                            counter[i]++;
                            summaryCounter++;
                        }
                    }

                    for (int i = 0; i < liquidDrops.length; i++) {
                        if (liquidDrops[i] == lowerLayer || liquidDrops[i] == lfloor) {
                            lcounter[i]++;
                        }
                    }
                }
            }

            StringBuilder worldInfo = new StringBuilder();

            if (summaryCounter == 0) return;

            worldInfo.append("Информация о карте:\n");
            worldInfo.append("[gold]Название: [lightgray]" + state.map.name() + "\n");
            worldInfo.append("[gold]Рекорд: [lightgray]" + state.map.getHightScore() + "\n");
            worldInfo.append("[white]Ресурсы:\n");
            for (int i = 0; i < counter.length; i++) {
                float cv = ((float) counter[i]) * typesCounter / summaryCounter / 3f;
                if (cv > 1 / 3f) cv = 1 / 3f;
                int percent = (int) Math.ceil(counter[i] * 100d / summaryCounter);
                Color c = new Color(Color.HSBtoRGB(cv, 1, 1));
                worldInfo.append(oreBlocksEmoji[i]);
                worldInfo.append('[');
                worldInfo.append(String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue()));
                worldInfo.append("]: ");
                if (counter[i] > 0) {
                    worldInfo.append(counter[i]);
                    worldInfo.append(" (");
                    worldInfo.append(percent);
                    worldInfo.append("%)");
                } else {
                    worldInfo.append("-");
                }
                worldInfo.append("\n[white]");
            }

            worldInfo.append("Жидкости:");
            boolean isLFound = false;
            for (int i = 0; i < lcounter.length; i++) {
                if (lcounter[i] > 0) {
                    worldInfo.append("\n[white]");
                    worldInfo.append(liquidsEmoji[i]);
                    worldInfo.append("[lightgray]: ");
                    worldInfo.append(counter[i]);
                    isLFound = true;
                }
            }
            if (!isLFound) {
                worldInfo.append(" [red]нет");
            }
            player.sendMessage(worldInfo.toString());
        });

        handler.<Player>register("skipmap", "[y/n]", "Начать голосование за пропуск карты", (arg, player) -> {
            if (currentlyMapSkipping != null && arg.length == 0)
                player.sendMessage("[scarlet]Голосование уже идет. Чтобы проголосовать, используйте [gold]/skipmap <y/n>");

            else if (currentlyMapSkipping == null && arg.length > 0)
                player.sendMessage("[scarlet]Голосование ещё не идет. Для пропуска карты используйте [gold]/skipmap");

            else if (currentlyMapSkipping == null) {
                SkipmapVoteSession session = new SkipmapVoteSession(currentlyMapSkipping);
                session.vote(player, 1);
                currentlyMapSkipping = session;
            } else if (currentlyMapSkipping.voted.contains(player.uuid()) || currentlyMapSkipping.voted.contains(netServer.admins.getInfo(player.uuid()).lastIP))
                player.sendMessage("[scarlet]Вы уже проголосовали");

            else {
                String voteSign = arg[0].toLowerCase();

                int sign = 0;
                if (voteSign.equals("y")) sign = +1;
                if (voteSign.equals("n")) sign = -1;

                if (sign == 0) {
                    player.sendMessage("[scarlet]Голосуйте либо \"y\" (да), либо \"n\" (нет)");
                    return;
                }

                currentlyMapSkipping.vote(player, sign);
            }
        });

        handler.<Player>register("skipwave", "<count>", "Начать голосование за пропуск волн", (arg, player) -> {
            if (currentlyWaveSkipping == null) {
                SkipwaveVoteSession session = new SkipwaveVoteSession(currentlyWaveSkipping);
                if (Long.valueOf(arg[0]) < 5) {
                    session.waves = Byte.valueOf(arg[0]);
                    waves = Integer.valueOf(arg[0]);
                    session.vote(player, 1);
                    currentlyWaveSkipping = session;
                } else {
                    player.sendMessage("[scarlet]Можно пропустить не более 5 волн.");
                }
            } else {
                player.sendMessage("[scarlet]Голосование уже идёт: [gold]/swvote <y/n>");
            }
        });

        handler.<Player>register("swvote", "<y/n/c>", "Проголосовать за/против пропуска волн", (arg, player) -> {
            if (currentlyWaveSkipping == null) {
                player.sendMessage("[scarlet]Nobody is being voted on.");
            } else {
                if (arg[0].contains("c")) {
                    if (player.admin) {
                        stopSkipwaveVoteSession();
                        Call.sendMessage("Голосование за пропуск [scarlet]" + waves + " [white]волн отменено администратором " + player.name());
                    }
                }
                if (player.isLocal()) {
                    player.sendMessage("[scarlet]Локальные игроки не могут голосовать.");
                    return;
                }
                if ((currentlyWaveSkipping).voted.contains(player.uuid()) || (currentlyWaveSkipping).voted.contains((netServer.admins.getInfo(player.uuid())).lastIP)) {
                    player.sendMessage("[scarlet]Ты уже проголосовал. Молчи!");
                    return;
                }
                String voteSign = arg[0].toLowerCase();
                int sign = 0;
                if (voteSign.equals("y")) sign = 1;
                if (voteSign.equals("n")) sign = -1;
                if (sign == 0) {
                    player.sendMessage("[scarlet]Голосуйте либо \"y\" (да), либо \"n\" (нет)");
                    return;
                }
                currentlyWaveSkipping.vote(player, sign);
            }
        });
    }

    public class SkipwaveVoteSession {
        float voteDuration = 180.0F;

        ObjectSet<String> voted = new ObjectSet<>();

        SkipwaveVoteSession wave;

        Timer.Task task;

        int votes;

        public byte waves;

        int votesRequiredSkipwave;

        public SkipwaveVoteSession(SkipwaveVoteSession wave) {
            this.wave = wave;
            votesRequiredSkipwave = votesRequiredSkipwave();
            this.task = Timer.schedule(() -> {
                if (!checkPass()) {
                    Call.sendMessage("[lightgray]Голосование закончилось. Недостаточно голосов, чтобы пропустить волны");
                    task.cancel();
                    stopSkipwaveVoteSession();
                }
            }, voteDuration);
        }

        void vote(Player player, int d) {
            this.votes += d;
            this.voted.addAll(player.uuid());
            Call.sendMessage(Strings.format("[" + GameWork.colorToHex(player.color) + "]@[lightgray] проголосовал" + ((d > 0) ? "[green]за" : "[red]против") + "[white] пропуск[scarlet] " + this.waves + " [white]волн[accent] (@/@)\n[lightgray]напишите [orange]/swvote <y/n>[], [green]за[]/[red]против", new Object[]{player.name, Integer.valueOf(this.votes), Integer.valueOf(this.votesRequiredSkipwave)}));
            checkPass();
        }

        boolean checkPass() {
            if (votes >= votesRequiredSkipwave) {
                currentlyWaveSkipping = null;
                Call.sendMessage("[gold]Голосование закончилось. Волны успешно пропущены!");
                for (int i = 0; i < this.waves; ) {
                    logic.runWave();
                    i++;
                }
                wave = null;
                task.cancel();
                Call.sendMessage(currentlyWaveSkipping.toString());
                return true;
            }
            return false;
        }
    }

    public void stopSkipwaveVoteSession() {
        currentlyWaveSkipping = null;
    }


    public int votesRequiredSkipwave() {
        if (Groups.player.size() == 1) return 1;
        if (Groups.player.size() == 2) return 2;
        return (int) Math.ceil(Groups.player.size() * 0.45D);
    }

    public class SkipmapVoteSession {

        float voteDuration = 3 * 60;
        ObjectSet<String> voted = new ObjectSet<>();
        SkipmapVoteSession map;
        Timer.Task task;
        int votes;

        int votesRequiredSkipmap;

        public SkipmapVoteSession(SkipmapVoteSession map) {
            this.map = map;
            votesRequiredSkipmap = votesRequiredSkipmap();
            this.task = Timer.schedule(() -> {
                if (!checkPass()) {
                    Call.sendMessage("[lightgray]Голосование закончилось. Недостаточно голосов, чтобы пропустить карту");
                    stopSkipmapVoteSession();
                    task.cancel();
                }
            }, voteDuration);
        }

        void vote(Player player, int d) {
            votes += d;
            voted.addAll(player.uuid()); // FIXME: , Vars.netServer.admins.getInfo(player.uuid()).lastIP
            Call.sendMessage(Strings.format("[" + GameWork.colorToHex(player.color) + "]@[lightgray] проголосовал " + (d > 0 ? "[green]за" : "[red]против") + "[] пропуска карты[accent] (@/@)\n[lightgray]Напишите[orange] /smvote <y/n>[], чтобы проголосовать [green]за[]/[red]против", player.name, votes, votesRequiredSkipmap));
            checkPass();
        }

        boolean checkPass() {
            if (votes >= votesRequiredSkipmap) {
                Call.sendMessage("[gold]Голосование закончилось. Карта успешно пропущена!");
                Events.fire(new GameOverEvent(Team.derelict));
                map = null;
                task.cancel();
                return true;
            }
            return false;
        }
    }

    public void stopSkipmapVoteSession() {
        currentlyMapSkipping = null;
    }

    public int votesRequiredSkipmap() {
        if (Groups.player.size() == 1) return 1;
        if (Groups.player.size() == 2) return 2;
        return (int) Math.ceil(Groups.player.size() * 0.45);
    }
}

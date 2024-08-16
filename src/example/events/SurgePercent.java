package example.events;

import arc.Events;
import arc.files.Fi;
import arc.util.ArcRuntimeException;
import mindustry.content.Items;
import mindustry.core.GameState;
import mindustry.game.EventType.GameOverEvent;
import mindustry.game.EventType.PlayerJoin;
import mindustry.game.EventType.StateChangeEvent;
import mindustry.game.Team;
import mindustry.gen.Call;

public class SurgePercent extends ServerEvent {

    private Timer timer;
    private boolean won = false;

    public SurgePercent() {
        super("surge%");
        color = "#e8d174";
    }

    @Override
    public void init() {
        Events.on(StateChangeEvent.class, e -> {
            if(e.to == GameState.State.paused){timer.stop();}
            if(e.from == GameState.State.paused){timer.start();}
        });
    }

    @Override
    public void announce() {
        Call.announce("[#e8d174]Surge% activated!");
		Call.sendMessage("[#e8d174]Surge% activated!");
    }

    @Override
	public void playerJoin(PlayerJoin e) {
		if(e.player == null) return;
		e.player.sendMessage("GET [#e8d174]ONE SURGE [scarlet]AS QUICKLY AS POSSIBLE");
	}

    @Override
    public void update() {
        if(isGenerated){
            if(!won){
                if(Team.sharded.cores().first().items().get(Items.surgeAlloy) >= 1){
                    timer.stop();
                    record(timer.getCount());
                    Events.fire(new GameOverEvent(Team.sharded));
                    float time = timer.getCount()/1000;
                    Call.sendMessage("[accent]Sharded(yellow) []won! time:" + time + " seconds");
                    won = true;
                }
                try{
                    if(Team.crux.cores().first().items().get(Items.surgeAlloy) >= 1){
                        timer.stop();
                        record(timer.getCount());
                        Events.fire(new GameOverEvent(Team.crux));
                        float time = timer.getCount()/1000;
                        Call.sendMessage("[red]Crux(red) []won! time:" + time + " seconds");
                        won = true;
                    }
                } catch (IllegalStateException e){}
            }
        }
    }

    private void record(Long record) {
        Fi recordFile = new Fi("./record");
        long recordCurrent = Long.MAX_VALUE;
        try{
            try{
                recordCurrent = Long.parseLong(recordFile.readString());
            } catch (ArcRuntimeException e){}
        } catch (NumberFormatException e){}
        
        if((recordCurrent > record)){
            recordFile.writeString(record.toString(), false);
            float time = recordCurrent/1000;
            Call.sendMessage("[yellow]New record![white] Old record:" + time + " seconds");
        }
    }

    @Override
    public void generateWorld() {
        won = false;
        timer = new Timer();
        Call.sendMessage("GET [#e8d174]ONE SURGE [scarlet]AS QUICKLY AS POSSIBLE");
        isGenerated = true;
    }
}


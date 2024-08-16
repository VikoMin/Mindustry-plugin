package example.events;

import java.util.ArrayList;

import arc.Events;
import arc.util.Log;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.gen.Player;

public class ServerEventsManager {
  public enum ServerEvents {
    newYear(new NewYearEvent()),
    spaceDanger(new SpaceDangerEvent()),
    livingWorld(new LivingWorldEvent()),
    luckyPlace(new LuckyPlaceEvent()),
	  weirdUnits(new WeirdUnitsEvent()),
    totemOfBugs(new TotemOfBugs()),
    //siege(new SiegeMode()),    removed because OSPx said
  surge(new SurgePercent());
    
    ServerEvent event;
    
    ServerEvents(ServerEvent event) {
      this.event = event;
    }
    
    public String toString() {
      if (this.event == null)
        return super.toString(); 
      return this.event.getName();
    }
    
    public ServerEvent getEvent() {
      return this.event;
    }
    
    public String getName() {
      if (this.event == null)
        return null; 
      return this.event.getName();
    }
    
    public String getCommandName() {
      if (this.event == null)
        return null; 
      return this.event.getCommandName();
    }
  }
  
  public static int getServerEventsCount() {
    return (ServerEvents.values()).length;
  }
  
  public static ServerEvent getServerEvent(int id) {
    if (id < 0)
      return null; 
    if (id >= getServerEventsCount())
      return null; 
    return ServerEvents.values()[id].getEvent();
  }
  
  public static ServerEvents[] getServerEvents() {
    return ServerEvents.values();
  }
  
  public static long eventsTPS = 16L;
  
  public boolean[] isEventsOn;
  
  private ArrayList<ServerEvent> activeEvents;
  
  public boolean isLoaded = false;
  
  boolean isRunning;
  
  public ServerEventsManager() {
    this.isLoaded = false;
    this.isRunning = false;
	if(activeEvents == null)
		activeEvents = new ArrayList<>();
  }
  
  public void init() {
    //
    Events.on(EventType.WorldLoadBeginEvent.class, e -> {
      for(int i = 0; i < getServerEventsCount(); i++){
        if (getServerEvent(i).isRunning()){
          getServerEvent(i).isGenerated = false;
        }
      }
          this.isLoaded = false;
          Vars.world.setGenerating(true);
          for (int i = 0; i < this.activeEvents.size(); i++)
            ((ServerEvent)this.activeEvents.get(i)).isGenerated = false;
        });
  //set world to generated
	Events.on(EventType.WorldLoadEndEvent.class, e -> {
		worldLoadEnd(e);
	});

    //next Events.on used in event (like lucky place)

    Events.on(EventType.BlockBuildEndEvent.class, e -> {
      if (this.isLoaded)
      for(int i = 0; i < getServerEventsCount(); i++){
        if (getServerEvent(i).isRunning()){
          if(getServerEvent(i).isGenerated)
          getServerEvent(i).blockBuildEnd(e);
			}
		}
        });
    Events.on(EventType.UnitDestroyEvent.class, e -> {
      if (this.isLoaded)
      for(int i = 0; i < getServerEventsCount(); i++){
        if (getServerEvent(i).isRunning()){
          if(getServerEvent(i).isGenerated)
          getServerEvent(i).unitDestroy(e);
			}
    }
        });
    Events.on(EventType.DepositEvent.class, e -> {
    if (this.isLoaded)
		for(int i = 0; i < getServerEventsCount(); i++){
			if (getServerEvent(i).isRunning()){
        if(getServerEvent(i).isGenerated)
				getServerEvent(i).deposit(e);
			}
		}
        });
    Events.on(EventType.WithdrawEvent.class, e -> {
      if (this.isLoaded)
      for(int i = 0; i < getServerEventsCount(); i++){
        if (getServerEvent(i).isRunning()){
          if(getServerEvent(i).isGenerated)
          getServerEvent(i).withdraw(e);
        }
      }
        });
    Events.on(EventType.TapEvent.class, e -> {
      if (this.isLoaded)
      for(int i = 0; i < getServerEventsCount(); i++){
        if (getServerEvent(i).isRunning()){
          if(getServerEvent(i).isGenerated)
          getServerEvent(i).tap(e);
        }
      }
    });
  }

  //generate events in world
  public void worldLoadEnd(EventType.WorldLoadEndEvent e) {
		for(int i = 0; i < getServerEventsCount(); i++){
			if (getServerEvent(i).isRunning()){
        if(!getServerEvent(i).isGenerated)
        getServerEvent(i).generateWorld();
	}
}
    Vars.world.setGenerating(false);
    this.isLoaded = true;
  }
  //Callsed on every tick
  public void update() {
	//Call.sendMessage(activeEvents.toString());
    if (this.isLoaded)
      for (int i = 0; i < this.getServerEventsCount(); i++) {
		if(getServerEvent(i).isRunning() | getServerEvent(i).isGenerated){
			getServerEvent(i).update();
    }
	}
  }

  //like Events.on but called by ExamplePlugin

  public void playerJoin(EventType.PlayerJoin e) {
	for(int i = 0; i < getServerEventsCount(); i++){
		if (getServerEvent(i).isRunning()){
      if (this.isLoaded)
        getServerEvent(i).playerJoin(e);
		}
	}
  }
  
  public void buildBegin(EventType.BlockBuildBeginEvent e){
    if (this.isLoaded)
      for(int i = 0; i < getServerEventsCount(); i++){
        if (getServerEvent(i).isRunning()){
          getServerEvent(i).blockBuildBegin(e);
        }
      }
    }

  public void runEvent(String commandName) {
    ServerEvent event = getEventByCommandName(commandName);
    if (event == null) {
      Log.info("Event not found!");
      return;
    }
    Log.info(commandName + ": " + event.getName());
    if (this.activeEvents.contains(event)) {
      Log.info("Event already active!");
      return;
    }
    event.run();
    this.activeEvents.add(event);
  }
  
  public void stopEvent(String commandName) {
    ServerEvent event = getEventByCommandName(commandName);
    if (event == null) {
      Log.info("Event not found!");
      return;
    }
    if (event.isRunning()) {
      event.stop();
      event.fixworld();
      event.isGenerated = false;
    } else {
      Log.info("Event not active!");
      return;
    }
  }

  //this is not even working
  public void fastRunEvent(String commandName) {
    ServerEvent event = getEventByCommandName(commandName);
    if (event == null) {
      Log.info("Event not found!");
      return;
    } 
    if (this.activeEvents.contains(event)) {
      Log.info("Event already active!");
      return;
    } 
    event.run();
    event.generateWorld();
    event.isGenerated = true;
    this.isLoaded = true;
    this.activeEvents.add(event);
  }
  
  private ServerEvent getEventByCommandName(String commandName) {
    for (int i = 0; i < getServerEventsCount(); i++) {
      if (commandName.equals(getServerEvent(i).getCommandName()))
        return getServerEvent(i);
    } 
    return null;
  }
  
  //idk what is this
  public void trigger(Player player, String... args) {
	for(int i = 0; i < getServerEventsCount(); i++){
		if (getServerEvent(i).isRunning()){
			activeEvents.add(getServerEvent(i));
		}
	}
    for (int i = 0; i < this.activeEvents.size(); i++)
      ((ServerEvent)this.activeEvents.get(i)).trigger(player, args); 
  }
}

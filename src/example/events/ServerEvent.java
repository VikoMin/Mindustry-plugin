package example.events;

import mindustry.game.EventType;
import mindustry.gen.Player;

public abstract class ServerEvent {
  private String name;
  
  private String commandName;
  
  protected String color = "white";
  
  private boolean isRunning;
  
  public boolean isGenerated;
  
  public ServerEvent(String name) {
    this.name = name;
    this.commandName = name.toLowerCase().replaceAll(" ", "_");
  }
  
  public void run() {
    this.isRunning = true;
    announce();
  }
  
  public void stop() {
    this.isRunning = false;
  }
  
  public abstract void init();
  
  public abstract void announce();
  
  public abstract void update();
  
  public abstract void generateWorld();
  
  public boolean isRunning() {
    return this.isRunning;
  }
  
  public String getName() {
    return this.name;
  }
  
  public String getCommandName() {
    return this.commandName;
  }
  
  public String getColor() {
    return this.color;
  }
  
  public String toString() {
    return super.toString() + " " + getCommandName();
  }
  
  protected void blockBuildEnd(EventType.BlockBuildEndEvent e) {}
  
  public void unitDestroy(EventType.UnitDestroyEvent e) {}
  
  public void deposit(EventType.DepositEvent e) {}
  
  public void tap(EventType.TapEvent e) {}
  
  public void withdraw(EventType.WithdrawEvent e) {}
  
  public void playerJoin(EventType.PlayerJoin e) {}
  
  public void trigger(Player player, String... args) {}
}
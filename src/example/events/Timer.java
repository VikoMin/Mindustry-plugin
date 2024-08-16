package example.events;

import arc.Events;
import mindustry.game.EventType.Trigger;

public class Timer {
    private long startTime;
    private long countTime;
    private long count;
    private boolean stopped = false;
    public Timer(){
        this.startTime = System.currentTimeMillis();
        this.countTime = startTime;
        Events.run(Trigger.update, () -> {
            if(stopped){
                countTime += System.currentTimeMillis() - countTime;
            } else {
                count = System.currentTimeMillis() - countTime;
            }
        });
    }
    public void stop(){
        stopped = true;
    }
    public void start(){
        stopped = false;
    }
    public long getCount(){
        return count;
    }
    public long getStartTime(){
        return startTime;
    }
    public long getStopped(){
        return countTime - startTime;
    }
}

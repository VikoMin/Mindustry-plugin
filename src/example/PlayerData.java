package example;

import java.io.File;
import java.util.Calendar;

import arc.Events;
import arc.files.Fi;
import arc.util.serialization.JsonReader;
import arc.util.serialization.JsonValue;
import arc.util.serialization.SerializationException;
import mindustry.game.EventType.BlockBuildEndEvent;
import mindustry.game.EventType.PlayerJoin;
import mindustry.game.EventType.WaveEvent;
import mindustry.gen.Groups;

public class PlayerData {
    private long blocksBuilded = 0;
    private long blocksDestroyed = 0;
    private int wavesSurvived = 0;
    private long bannedUntil = 0;
    private boolean js = false;
    private boolean ultra = false;
    private String UUID = "";

    public PlayerData(long builded, long destroyed, int waves, String uuid){
        blocksBuilded = builded;
        blocksDestroyed = destroyed;
        wavesSurvived = waves;
        UUID = uuid;
    }
    public PlayerData(long builded, long destroyed, int waves, long banned, boolean js, boolean ultra, String uuid){
        blocksBuilded = builded;
        blocksDestroyed = destroyed;
        wavesSurvived = waves;
        bannedUntil = banned;
        this.ultra = ultra;
        this.js = js;
        UUID = uuid;
    }


    public long getBuilded(){
        return blocksBuilded;
    }
    public long getDestroyed(){
        return blocksDestroyed;
    }
    public int getWaves(){
        return wavesSurvived;
    }
    public long getBanned(){
        return bannedUntil;
    }
    public boolean getJs(){
        return js;
    }
    public String getUUID(){
        return UUID;
    }
    public static PlayerData getData(String uuid){
        Fi data = new Fi(new File("./config/data/" + uuid.replaceAll("/", "@") + ".json"));
        if(!data.exists()) return null;
        JsonReader parser = new JsonReader();
        JsonValue jsonData;
        try{
            jsonData = parser.parse(data);
        } catch(SerializationException e){return null;}
        int waves = 0;
        long destroyed = 0L;
        long builded = 0L;
        long banned = 0L;
        boolean js = false;
        boolean ultra = false;
        try{
            builded = jsonData.get("builded").asLong();
            destroyed = jsonData.get("destroyed").asLong();
            waves = jsonData.get("waves").asInt();
            banned = jsonData.get("banned").asLong();
            js = jsonData.get("js").asBoolean();
            ultra = jsonData.get("ultra").asBoolean();
        } catch(IllegalStateException e){return null;}
        return new PlayerData(builded, destroyed, waves, banned, js, ultra, uuid);
    }

    public void save(){
        Fi data = new Fi(new File("./config/data/" + this.UUID.replaceAll("/", "@") + ".json"));
        String write =
        "{\n" +
        "    \"builded\": " + Long.toString(blocksBuilded) + "\n" +
        "    \"destroyed\": " + Long.toString(blocksDestroyed) + "\n" +
        "    \"waves\": " + Integer.toString(wavesSurvived) + "\n" +
        "    \"banned\": " + Long.toString(bannedUntil) + "\n" +
        "    \"js\": " + Boolean.toString(js) + "\n" +
        "    \"ultra\": " + Boolean.toString(ultra) + "\n" +
        "}";
        data.writeString(write, false);
    }

    public void ban(long millis){
        bannedUntil = millis;
    }

    public static void init(){
        Fi dataDir = new Fi(new File("./config/data"));
        if(!dataDir.exists()) dataDir.mkdirs();
                Events.on(PlayerJoin.class, e -> {
            if(getData(e.player.uuid().replaceAll("/", "@")) == null) {
                if(getData(e.player.uuid()) == null)
                    new PlayerData(e.player.uuid().replaceAll("/", "@")).save();
                else {
                    PlayerData data = getData(e.player.uuid());
                    data.UUID = e.player.uuid().replaceAll("/", "@");
                    data.save();
                    new Fi(new File("./config/data" + e.player.uuid().split("/")[0])).emptyDirectory(true);
                }
            }
            if(getData(e.player.uuid()).getBanned() > System.currentTimeMillis()){
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(getData(e.player.uuid()).getBanned());
                String kick = "You are banned until " + calendar.get(Calendar.YEAR) + "." + (int)(calendar.get(Calendar.MONTH) + 1) + "." + calendar.get(Calendar.DAY_OF_MONTH) + " " + calendar.get(Calendar.HOUR_OF_DAY) + ":" + calendar.get(Calendar.MINUTE) + " UTC";
                e.player.kick(kick, 10);
            } else {
                PlayerData data = getData(e.player.uuid());
                data.bannedUntil = 0;
                data.save();
            }
        });
        //Events.on(PlayerLeave.class, e ->{
        //    if(getData(e.player.uuid()) != null) new PlayerData(0, 0, 0, e.player.uuid()).save();
        //});
        Events.on(BlockBuildEndEvent.class, e -> {
            PlayerData data;
            if(e.unit.getPlayer() == null) return;
            data = getData(e.unit.getPlayer().uuid());
            if(!e.breaking) data.blocksBuilded++;
            else data.blocksDestroyed++;
            data.save();
        });
        Events.on(WaveEvent.class, e -> {
            Groups.player.each(p -> {
                PlayerData data = getData(p.uuid());
                data.wavesSurvived++;
                data.save();
            });
        });
    }
}

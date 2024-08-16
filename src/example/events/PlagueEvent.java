package example.events;

import arc.math.geom.Point2;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.gen.Call;
import mindustry.world.Block;
import mindustry.world.Tile;

public class PlagueEvent extends ServerEvent{
    //Very WIP, don't use

    public static final Seq<Block> unplaguedBlocks = new Seq<Block>().addAll(
        Blocks.moss, Blocks.sporeMoss, Blocks.salt, Blocks.hotrock, Blocks.magmarock, Blocks.ferricStone, Blocks.ferricCraters, Blocks.redStone, Blocks.denseRedStone, Blocks.redStoneVent, Blocks.regolith, Blocks.rhyolite, Blocks.roughRhyolite, Blocks.rhyoliteCrater, Blocks.rhyoliteVent, Blocks.yellowStone, Blocks.yellowStonePlates, Blocks.yellowStoneVent, Blocks.carbonStone, Blocks.carbonVent, //floors
        Blocks.taintedWater, Blocks.deepTaintedWater, Blocks.darksandTaintedWater, Blocks.cryofluid, Blocks.slag, Blocks.arkyciteFloor, //liquids
        Blocks.sporeWall, Blocks.sporePine, Blocks.saltWall, Blocks.graphiticWall, Blocks.regolithWall, Blocks.yellowStoneWall, Blocks.rhyoliteWall, Blocks.carbonWall, Blocks.ferricStoneWall, Blocks.redStoneWall, Blocks.redDiamondWall, //Walls
        Blocks.crystalCluster, Blocks.vibrantCrystalCluster, Blocks.crystalBlocks, Blocks.crystalOrbs //crystal something (like boulder but cannot be removed)
        );

    private Tile[][] plague;
    private Tile plagueSource;
    private short sourceX, sourceY;

    public PlagueEvent() {
        super("plague");
        this.color = "purple";
    }

    @Override
    public void init() {
        
    }

    @Override
    public void announce() {
        Call.announce("[purple]Событие \"Заражение\" начнется на следующей карте!");
		Call.sendMessage("[purple]Событие \"Заражение\" начнется на следующей карте!");
    }

    @Override
    public void update() {
        
    }

    @Override
    public void generateWorld() {
        Seq<Point2> points = new Seq<>();
        Point2 point = points.random();
        plague = new Tile[Vars.world.width()][Vars.world.height()];
        while(true){
            point = points.random();
            if(!unplaguedBlocks.contains(Vars.world.tile(point.x, point.y).block()) && !Vars.world.tile(point.x, point.y).block().update && Vars.world.tile(point.x, point.y).block().equals(Blocks.air)){
                break;
            }
        }
        sourceX = (short)(point.x);
        sourceY = (short)(point.y);
        plagueSource = Vars.world.tile(sourceX, sourceY);
        plagueSource.setFloorNet(Blocks.moss);
        Vars.world.tile(sourceX, sourceY + 1).setFloorNet(Blocks.moss);
        Vars.world.tile(sourceX + 1, sourceY).setFloorNet(Blocks.moss);
        Vars.world.tile(sourceX, sourceY - 1).setFloorNet(Blocks.moss);
        Vars.world.tile(sourceX - 1, sourceY).setFloorNet(Blocks.moss);
    }
    
}

package example.events;

import static mindustry.Vars.*;

import arc.graphics.Color;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.content.Fx;
import mindustry.content.Liquids;
import mindustry.content.UnitTypes;
import mindustry.entities.bullet.BulletType;
import mindustry.game.EventType.BlockBuildEndEvent;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.world.modules.LiquidModule;

public class TotemOfBugs extends ServerEvent{

    public Seq<Target> targets = new Seq<>();
	public int spawn = 1;
	public float speed = 1;

    public TotemOfBugs(){
        super("totem_of_bugs");
        color = "#66ccff";
    }

    @Override
    public void init() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void announce() {
		Call.announce("[#66ccff]Событие \"Жучиный тотем\" начнётся на следующей карте!");
        Call.sendMessage("[#66ccff]Событие \"Жучиный тотем\" начнётся на следующей карте!");
    }

    @Override
    public void update() {
        for(int i = 0; i<targets.size; i++){
			Target target = targets.get(i);
			int x = target.x;
			int y = target.y;
			if(hasTarget(x, y)){target.active = true;}
			if(!target.active) continue;
			if(getHeaterPower(x, y-3)>=30 && getHeaterPower(x, y+3)>=30){
				Call.effect(Fx.dynamicSpikes,(float)(x*tilesize), (float)(y*tilesize), 128,Color.valueOf("ff4500"));
				world.tile(x, y-3).setNet(Blocks.surgeCrucible, Groups.player.first().team(), 0);
				world.tile(x, y+3).setNet(Blocks.surgeCrucible, Groups.player.first().team(), 0);
				for(int a = 0; a < spawn; a++){
					target.collarises.add(UnitTypes.collaris.spawn(Team.crux, x*tilesize, y*tilesize));
				} spawn*=2; target.spawned = true;
			}
			if(target.spawned){
				int d = 0;
				for(int b = 0; b < target.collarises.size; b++){
					if(target.collarises.get(i).dead){d++;}
				}
				if(d == target.collarises.size | d != 0){target.dead = true;}else{target.dead = false; target.spawned = false; target.collarises.size = 0;}
				for(int b = 0; b < target.collarises.size; b++){
				if(target.collarises.get(i).dead){target.collarises.remove(b);b--;}
				}
				if(target.dead){boost();}
				if(!hasTarget(x, y)){target.active = false; targets.remove(target);}
			}
		}
    }

    public void boost() {
		if(Math.random()<=0.33){
			//damage (bullets)
			for(int i = 0; i < Vars.content.bullets().size; i++){
				Vars.content.bullets().get(i).damage*=1.2;
			}
			Call.sendMessage("[red]Damage boots!");
		} else if(Math.random()<=0.66){
			//splash damage
			//fuck this again works only for host
			for(int i = 0; i < Vars.content.bullets().size; i++){
				BulletType bullet = Vars.content.bullets().get(i);
				if(bullet.splashDamage<=0){bullet.splashDamage=5;}
				if(bullet.splashDamageRadius<=0){bullet.splashDamageRadius=2;}
				bullet.splashDamagePierce=true;
				bullet.splashDamage*=1.2;
				bullet.splashDamageRadius*=1.2;
			}
			Call.sendMessage("[gold]Splash Damage boots!");
		} else {
			//this too
			for(int i = 0; i < Vars.content.bullets().size; i++){
				BulletType bullet = Vars.content.bullets().get(i);
				if(bullet.homingPower<=0){bullet.homingPower=0.01f;}
				if(bullet.homingRange<=0){bullet.homingRange=50f;}
				bullet.homingDelay=0;
				bullet.homingRange*=1.2;
				bullet.homingPower*=1.2;
			}
			Call.sendMessage("[gray]AutoAim boots!");
		}
		//shoot speed (applyBoost)
		//works only for host (dps will increase for everyone)
		/*speed*=1.2;
		for(int y = 0; y <= world.height(); y++){
			for(int x = 0; x <= world.width(); x++){
				Tile tile = world.tile(x, y);
				if(tile==null) continue;
				Building build = tile.build;
				if(build==null) continue;
				Object value = build.config();
				if(value==null) continue;
				Block block = tile.block();
				if(block==null) continue;
				if(!block.attacks) continue;
				Team team = tile.build.team;
				if(team==null) continue;
				int rotation = tile.build.rotation;
				build.applyBoost(speed, Integer.MAX_VALUE);
				SetTileCallPacket packet = new SetTileCallPacket();
				packet.tile = tile;
				packet.block = block;
				packet.team = team;
				packet.rotation = rotation;
				Vars.net.send(packet, true);
				TileConfigCallPacket config = new TileConfigCallPacket();
				for(int i = 0; i < Groups.player.size(); i++){
					config.player = Groups.player.index(i);
					config.build = build;
					config.value = value;
					Groups.player.index(i).con.send(config, true);
				}
			}
		}
		//P.S. applyBoost is a overdrive
*/
	}

	@Override
    public void generateWorld() {
	}

    public boolean isOut(int x, int y) {
		if(x < 0) return true;
		if(y < 0) return true;
		if(x >= world.width()) return true;
		if(y >= world.height()) return true;
		return false;
	}

    public boolean hasMerui(int x, int y) {
		if(isOut(x, y)) return false;
		if(world.tile(x, y).block().name.equals(Blocks.mechFabricator.name)) {
			return true;
		}
		return false;
	}

    public boolean hasCleroi(int x, int y) {
		if(isOut(x, y)) return false;
		if(world.tile(x, y).block().name.equals(Blocks.mechRefabricator.name)) {
			return true;
		}
		return false;
	}

    public boolean hasCarbide(int x, int y) {
		if(isOut(x, y)) return false;
		if(world.tile(x, y).block().name.equals(Blocks.carbideCrucible.name)) {
			return true;
		}
		return false;
	}

	public boolean hasFuse(int x, int y) {
		if(isOut(x, y)) return false;
		if(world.tile(x, y).block().name.equals(Blocks.fuse.name)) {
			return true;
		}
		return false;
	}

	public boolean hasHeater(int x, int y) {
		if(isOut(x, y)) return false;
		if(world.tile(x, y).block().name.equals(Blocks.slagHeater.name)) {
			return true;
		}
		return false;
	}

    public boolean hasTarget(int xx, int yy) {
		if(isOut(xx, yy)) return false;
		if(world.tile(xx, yy).block().name.equals(Blocks.carbideCrucible.name)) {
			int x = world.tile(xx, yy).build.tile.centerX();
			int y = world.tile(xx, yy).build.tile.centerY();
			return hasCleroi(x+3, y) && hasCleroi(x-3, y) && hasMerui(x+3, y-3) && hasMerui(x-3, y+3)
                && hasHeater(x, y-3) && hasHeater(x, y+3)
				&& hasFuse(x+3, y+3) && hasFuse(x-3, y-3);
		}
		return false;
	}

	public float getHeaterPower(int x, int y) {
		if(isOut(x, y)) return 0;

		if(world.tile(x, y).block().name.equals(Blocks.slagHeater.name)) {
			Building build = world.tile(x, y).build;
			if(build == null) return 0;
			LiquidModule liquids = build.liquids();
			if(liquids == null) return 0;
			return liquids.get(Liquids.slag);
		}
		return 0;
	}

    @Override
	public void blockBuildEnd(BlockBuildEndEvent e) {
		int x = 0;
		int y = 0;
		if(!e.breaking)
		x = e.tile.centerX();
		y = e.tile.centerY();
		if(hasMerui(x, y) || hasCleroi(x, y) || hasHeater(x, y) || hasFuse(x, y) || hasCarbide(x, y)){
			if(hasTarget(x, y) || hasTarget(x, y-3) || hasTarget(x-3, y-3) || hasTarget(x-3, y) || hasTarget(x-3, y+3) || hasTarget(x, y+3) || hasTarget(x+3, y+3) || hasTarget(x+3, y) || hasTarget(x+3, y-3)){
				if(hasCarbide(x, y)) targets.add(new Target(x, y));
				if(hasCarbide(x, y-3)) targets.add(new Target(x, y-3));
				if(hasCarbide(x-3, y-3)) targets.add(new Target(x-3, y-3));
				if(hasCarbide(x-3, y)) targets.add(new Target(x-3, y));
				if(hasCarbide(x-3, y+3)) targets.add(new Target(x-3, y+3));
				if(hasCarbide(x, y+3)) targets.add(new Target(x, y+3));
				if(hasCarbide(x+3, y+3)) targets.add(new Target(x+3, y+3));
				if(hasCarbide(x+3, y)) targets.add(new Target(x+3, y));
				if(hasCarbide(x+3, y-3)) targets.add(new Target(x+3, y-3));
			}
		}
	}
    
    public class Target {
		Seq<Unit> collarises = new Seq<>();
		private int x, y;
		public boolean dead = false;
		public boolean active = false;
		public boolean spawned = false;
		public Target(int x, int y) {
			this.x = x;
			this.y = y;
            Call.effect(Fx.dynamicSpikes,(float)(x*tilesize), (float)(y*tilesize), 126f ,Color.gray);
			active = true;
		}
	}
}


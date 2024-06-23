package example.events;

import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.content.Liquids;
import mindustry.content.StatusEffects;
import mindustry.core.World;
import mindustry.game.EventType.PlayerJoin;
import mindustry.gen.Building;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.gen.Unit;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.environment.Floor;
import mindustry.world.modules.PowerModule;

public class NewYearEvent extends ServerEvent {
  private Floor[][] floor;
  
  private Floor[][] overlay;
  
  private Block[][] blocks;
  
  private byte[][] cold;
  
  public NewYearEvent() {
    super("New year");
    this.color = "sky";
  }

  @Override
  public void playerJoin(PlayerJoin e) {
	if(e.player == null) return;
	StringBuffer infotitle = new StringBuffer();
		final String infoTilteString = "Новогоднее событие!".toUpperCase();
		for (int i = 0; i < infoTilteString.length(); i++) {
			char ch = infoTilteString.charAt(i);
			if(i%2 == 0) {
				infotitle.append("[white]");
			} else {
				if(i%4 == 1) {
					infotitle.append("[red]");
				}
				if(i%4 == 3) {
					infotitle.append("[green]");
				}
			}
			infotitle.append(ch);
		}
		infotitle.append("[sky]\n");
		
        Call.sendMessage(infotitle.toString() 
        + "Карта покрылась снегом и льдом Некоторые руды оказались под снегом.\n"
        + "Найдите способ растопить снег и лед, и добраться до занесенных снегом руд\n\n"
        + "[lightgray](Используйте [gold]/mapinfo[lightgray] для статистики ресурсов)");
	}
  
  public void init() {}
  
  public void update() {
	if(cold == null){cold = new byte[Vars.world.width()][Vars.world.height()];}
    Vars.content.createModContent();
    for (int i = 0; i < Groups.unit.size(); i++) {
      Unit unit = (Unit)Groups.unit.index(i);
      if (!unit.isFlying() && unit.tileOn() != null) {
        Tile tile = unit.tileOn();
        int tileX = unit.tileX();
        int tileY = unit.tileY();
        if (tileX < 0 || tileY < 0 || tileX + 1 >= Vars.world.width() || tileY + 1 >= Vars.world.height())
    continue;
        if (this.cold[tileX][tileY] > 50)
        if (Math.random() < 0.25D) {
            unit.apply(StatusEffects.freezing, 300.0F);
        } else if (Math.random() < (1.0F / unit.bounds())) {
            unit.apply(StatusEffects.unmoving, 30.0F);
        }  
        if ((tile.floor()).isLiquid || (tile.overlay()).isLiquid) {
            this.cold[tileX][tileY] = 0;
            returnTileBack(tile, unit.tileX(), unit.tileY());
        } else if (Math.random() > (10.0F / unit.bounds()) && Math.random() < 0.05D) {
            this.cold[tileX][tileY] = 0;
            returnTileBack(tile, unit.tileX(), unit.tileY());
        } else {
            continue;
        }
        double randAlign = Math.random() * Math.toRadians(360.0D);
        double randHypot = Math.random() * unit.bounds() / 20.0D;
        int tx = (int)(unit.tileX() + randHypot * Math.cos(randAlign));
        int ty = (int)(unit.tileY() + randHypot * Math.sin(randAlign));
        if (tx >= 0 && 
          ty >= 0 && 
          tx + 1 < Vars.world.width() && 
          ty + 1 < Vars.world.height()) {
          this.cold[tx][ty] = 0;
          if (Vars.world.tile(tx, ty) != null)
            returnTileBack(Vars.world.tile(tx, ty), tx, ty); 
        } 
      } 
      continue;
    } 
    World world = Vars.world;
    for (int j = 0; j < Groups.player.size(); j++) {
      Player player = (Player)Groups.player.index(j);
      double randAlign = Math.random() * Math.toRadians(360.0D);
      double randHypot = Math.random() * 10.0D;
      int tileX = (int)(player.tileX() + randHypot * Math.cos(randAlign));
      int tileY = (int)(player.tileY() + randHypot * Math.sin(randAlign));
      if (tileX >= 0 && 
        tileY >= 0 && 
        tileX + 1 < world.width() && 
        tileY + 1 < world.height()) {
        if (this.cold[tileX][tileY] >= 50 && 
          !player.unit().hasEffect(StatusEffects.freezing)) {
          player.unit().damage(1.0F, true);
          player.unit().apply(StatusEffects.freezing, 180.0F);
        } 
        Tile tileBuilding = world.tileBuilding(tileX, tileY);
        if (tileBuilding != null) {
          Building building = tileBuilding.build;
          if (building != null) {
            PowerModule powerModule = building.power();
            if (powerModule != null) {
              int r = (int)((world.tile(tileX, tileY).block()).lightRadius / 10.0F * powerModule.status);
              for (int y = tileY - r; y <= tileY + r; y++) {
                for (int x = tileX - r; x <= tileX + r; x++) {
                  double hypot = Math.hypot((tileX - x), (tileY - y));
                  if (hypot <= r && 
                    x >= 0 && 
                    y >= 0 && 
                    x + 1 < world.width() && 
                    y + 1 < world.height()) {
                    Tile tile = world.tile(x, y);
                    if (tile != null && 
                      Math.random() > hypot / r) {
                      int remove = 25 - (int)(hypot * 25.0D / r);
                      remove = (int)(remove * Math.random());
                      if (remove > this.cold[x][y]) {
                        this.cold[x][y] = 0;
                      } else {
                        this.cold[x][y] = (byte)(this.cold[x][y] - remove);
                      } 
                      if (this.cold[x][y] <= 0)
                        returnTileBack(tile, x, y); 
                    } 
                  } 
                } 
              } 
            } 
          } 
        } 
      } 
    } 
  }
  
  public void generateWorld() {
	if(!isGenerated){
    int w = Vars.world.width();
    int h = Vars.world.height();
    this.floor = new Floor[w][h];
    this.overlay = new Floor[w][h];
    this.blocks = new Block[w][h];
    this.cold = new byte[w][h];
    StringBuffer infotitle = new StringBuffer();
		final String infoTilteString = "Новогоднее событие!".toUpperCase();
		for (int i = 0; i < infoTilteString.length(); i++) {
			char ch = infoTilteString.charAt(i);
			if(i%2 == 0) {
				infotitle.append("[white]");
			} else {
				if(i%4 == 1) {
					infotitle.append("[red]");
				}
				if(i%4 == 3) {
					infotitle.append("[green]");
				}
			}
			infotitle.append(ch);
		}
		infotitle.append("[sky]\n");
		
        Call.sendMessage(infotitle.toString() 
        + "Карта покрылась снегом и льдом Некоторые руды оказались под снегом.\n"
        + "Найдите способ растопить снег и лед, и добраться до занесенных снегом руд\n\n"
        + "[lightgray](Используйте [gold]/mapinfo[lightgray] для статистики ресурсов)");
    for (int y = 0; y < h; y++) {
		for (int x = 0; x < w; x++) {
			Tile tile = Vars.world.tile(x, y);
			this.floor[x][y] = tile.floor();
			this.overlay[x][y] = tile.overlay();
			this.blocks[x][y] = tile.block();
		}
	}
	for (int y = 0; y < h; y++) {
      for (int x = 0; x < w; x++) {
        Tile tile = Vars.world.tile(x, y);
        this.cold[x][y] = 0;
        if (!tile.floor().hasBuilding() && !tile.overlay().hasBuilding() && !tile.block().hasBuilding() && !(tile.block()).emitLight && !(tile.overlay()).emitLight && !(tile.floor()).emitLight) {
          if (!tile.block().isAir())
            if ((tile.block()).name.indexOf("boulder") != -1) {
              tile.setBlock(Blocks.snowBoulder);
              this.cold[x][y] = 100;
            } else {
              tile.setBlock(Blocks.snowWall);
              this.cold[x][y] = 100;
            }  
          boolean isIce = false;
          Floor floor = tile.floor();
          Floor overlay = tile.overlay();
          if ((tile.overlay()).liquidDrop == Liquids.water) {
            overlay = (Floor)Blocks.ice;
            overlay.isLiquid = true;
            overlay.liquidDrop = Liquids.water;
            floor.supportsOverlay = false;
            floor.speedMultiplier = 2.0F;
            floor.needsSurface = true;
            tile.setFloor(floor);
            tile.setOverlay((Block)overlay);
            this.cold[x][y] = 100;
            isIce = true;
          } 
          if ((tile.floor()).liquidDrop == Liquids.water) {
            floor = (Floor)Blocks.ice;
            floor.isLiquid = true;
            floor.liquidDrop = Liquids.water;
            floor.supportsOverlay = false;
            floor.speedMultiplier = 2.0F;
            floor.needsSurface = true;
            this.cold[x][y] = 100;
            tile.setFloor(floor);
            tile.setOverlay((Block)overlay);
            isIce = true;
          } 
          if (overlay != Blocks.ice && 
            overlay.itemDrop != null && 
            Math.random() > 0.5D)
            overlay = (Floor)Blocks.air; 
          if (!isIce && 
            floor != Blocks.space && overlay != Blocks.space)
            if (floor == Blocks.sand || floor == Blocks.darksand) {
              tile.setFloor((Floor)Blocks.iceSnow);
              tile.setOverlay((Block)overlay);
              this.cold[x][y] = 110;
            } else {
              tile.setFloor((Floor)Blocks.snow);
              tile.setOverlay((Block)overlay);
              this.cold[x][y] = 120;
            }  
        } 
      } 
    }
	isGenerated = true;
  }
  }
  
  public void announce() {
    Call.announce("[royal]Новогоднее событие начнётся на следующей карте");
    Call.sendMessage("[sky]Новогоднее событие начнётся на следующей карте");
  }
  
  private void returnTileBack(Tile tile, int x, int y) {
    this.cold[x][y] = 0;
    if ((tile.floor()).name.equals(Blocks.ice.name)) {
      Building building = tile.build;
      if (building != null) {
        building.damage(10.0F);
        Call.buildDestroyed(building);
        return;
      } 
    } 
    tile.setFloor(this.floor[x][y]);
    tile.setOverlay((Block)this.overlay[x][y]);
    tile.setFloorNet((Block)this.floor[x][y], (Block)this.overlay[x][y]);
    if (!tile.block().isAir() && !tile.floor().hasBuilding() && !tile.overlay().hasBuilding() && !tile.block().hasBuilding()) {
      tile.setBlock(this.blocks[x][y]);
      tile.setNet(this.blocks[x][y]);
    }
  }
}

package objects;

import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import objects.Fight.Fighter;

import common.SocketManager;
import common.World;

public class Percepteur
{
	private static Map<Integer,Percepteur> 	_perco	= new TreeMap<Integer,Percepteur>();
	private int _guid;
	private short _MapID;
	private int _cellID;
	private byte _orientation;
	private int _GuildID = 0;
	private short _N1 = 0;
	private short _N2 = 0;
	private byte _inFight = 0;
	private int _inFightID = -1;
	
	public Percepteur(int guid, short map, int cellID, byte orientation, int GuildID, short N1, short N2)
	{
		_guid = guid;
		_MapID = map;
		_cellID = cellID;
		_orientation = orientation;
		_GuildID = GuildID;
		_N1 = N1;
		_N2 = N2;
	}
	
	public static String parseGM(Carte map)
	{
		String sock = "GM|";
		boolean isFirst = true;
		for(Entry<Integer, Percepteur> perco :  _perco.entrySet())
		{
			if(perco.getValue()._inFight > 0) continue;//On affiche pas le perco si il est en combat
			if(perco.getValue()._MapID == map.get_id())
			{
				if(!isFirst) sock += "|";
				sock += "+";
				sock += perco.getValue()._cellID+";";
				sock += perco.getValue()._orientation+";";
				sock += "0"+";";
				sock += perco.getValue()._guid+";";
				sock += (perco.getValue()._N1+","+perco.getValue()._N2+";");
				sock += "-6"+";";
				sock += "6000^100;";
				Guild G = World.getGuild(perco.getValue()._GuildID);
				sock += G.get_lvl()+";";
				sock += G.get_name()+";"+G.get_emblem();
				isFirst = false;
			}else
			{
				continue;
			}
		}
		return sock;
	}
	
	public int get_guildID() {
		return _GuildID;
	}
	
	public static void addPerco(Percepteur perco)
	{
		_perco.put(perco._guid, perco);
	}
	
	public static Percepteur GetPerco(int percoGuid)
	{
		return _perco.get(percoGuid);
	}
	
	public void DelPerco(int percoGuid)
	{
		_perco.remove(percoGuid);
	}

	public Map<Integer, Percepteur> get_PercobyID(int id) {
		_perco.get(id);
		return _perco;
	}
	
	public int get_inFight()
	{
		return _inFight;
	}
	
	public void set_inFight(byte fight)
	{
		_inFight = fight;
	}
	
	public int getGuid()
	{
		return _guid;
	}
	
	public int get_cellID()
	{
		return _cellID;
	}
	
	public void set_inFightID(int ID)
	{
		_inFightID = ID;
	}
	
	public int get_inFightID()
	{
		return _inFightID;
	}
	
	public int get_mapID()
	{
		return _MapID;
	}
	
	public int get_N1()
	{
		return _N1;
	}
	
	public int get_N2()
	{
		return _N2;
	}
	
	public static String parsetoGuild(int GuildID)
	{
		String packet ="+";
		boolean isFirst = true;
		for(Entry<Integer, Percepteur> perco : _perco.entrySet())
		{
			 if(perco.getValue().get_guildID() == GuildID)
    		 {
	    			if(!isFirst) packet += "|";
	    			packet += perco.getValue()._guid+";"+perco.getValue()._N1+","+perco.getValue()._N2+";";
	    			
	    			packet += Integer.toString(World.getCarte(perco.getValue()._MapID).get_id(), 36)+","+World.getCarte(perco.getValue()._MapID).getX()+","+World.getCarte(perco.getValue()._MapID).getY()+";";//perco.getValue()._MapID+
	    			packet += perco.getValue()._inFight+";";
	    			if(perco.getValue()._inFight == 1)
	    			{
	    				//TODO : Temps restant du combat, les timer ce n'est pas mon fort.
	    				packet += "45000;";//TimerActuel
	    				packet += "45000;";//TimerInit
	    				packet += "7;";//Nombre de place maximum FIXME : En fonction de la map
	    				packet += "?,?,";//?
	    			}else
	    			{
	    				packet += "0;";
	    				packet += "45000;";
	    				packet += "7;";
	    				packet += "?,?,";
	    			}
	    			packet += "1,2,3,4,5";
	    			
	    			//	?,?,callername,startdate(Base 10),lastHarvesterName,lastHarvestDate(Base 10),nextHarvestDate(Base 10)
	    			isFirst = false;
    		 }else
    		 {
    			 continue;
    		 }
   	 	}

		if(packet.length() == 1) packet = null;
		return packet;
		
	}
	
	public static int GetPercoGuildID(int _id) {
		
		for(Entry<Integer, Percepteur> perco : _perco.entrySet())
		{
			if(perco.getValue().get_mapID() == _id)
			{
				return perco.getValue().get_guildID();
			}
		}
		return 0;
	}
	
	public static int CountPercoGuild(int GuildID) {
		int i = 0;
		for(Entry<Integer, Percepteur> perco : _perco.entrySet())
		{
			if(perco.getValue().get_guildID() == GuildID)
			{
				i++;
			}
		}
		return i;
	}
	
	public static void parseAttaque(Personnage perso, int guildID)
	{
		for(Entry<Integer, Percepteur> perco : _perco.entrySet()) 
		{
			if(perco.getValue()._inFight > 0 && perco.getValue()._GuildID == guildID)
			{
				SocketManager.GAME_SEND_gITp_PACKET(perso, parseAttaqueToGuild(perco.getValue()._guid, perco.getValue()._MapID, perco.getValue()._inFightID));
			}
		}
	}
	
	public static void parseDefense(Personnage perso, int guildID)
	{
		for(Entry<Integer, Percepteur> perco : _perco.entrySet()) 
		{
			if(perco.getValue()._inFight > 0 && perco.getValue()._GuildID == guildID)
			{
				SocketManager.GAME_SEND_gITP_PACKET(perso, parseDefenseToGuild(perco.getValue()._guid, perco.getValue()._MapID, perco.getValue()._inFightID));
			}
		}
	}
	
	public static String parseAttaqueToGuild(int guid, short mapid, int fightid)
	{	
		String str = "+";
		str += guid;
			
		for(Entry<Integer, Fight> F : World.getCarte(mapid).get_fights().entrySet())
		{
			//Je boucle les combats de la map bien qu'inutile :/
			//Mais cela éviter le bug F.getValue().getFighters(1) == null
				if(F.getValue().get_id() == fightid)
				{
					for(Fighter f : F.getValue().getFighters(1))//Attaque
					{
						str += "|";
						str += Integer.toString(f.getPersonnage().get_GUID(), 36)+";";
						str += f.getPersonnage().get_name()+";";
						str += f.getPersonnage().get_lvl()+";";
						str += "0;";
					}
				}
		}
		return str;
	}
	
	public static String parseDefenseToGuild(int guid, short mapid, int fightid)
	{	
		String str = "+";
		str += guid;
			
		for(Entry<Integer, Fight> F : World.getCarte(mapid).get_fights().entrySet())
		{
			//Je boucle les combats de la map bien qu'inutile :/
			//Mais cela éviter le bug F.getValue().getFighters(2) == null
				if(F.getValue().get_id() == fightid)
				{
					for(Fighter f : F.getValue().getFighters(2))//Defense
					{
						if(f.getPersonnage() == null) continue;//On sort le percepteur
						str += "|";
						str += Integer.toString(f.getPersonnage().get_GUID(), 36)+";";
						str += f.getPersonnage().get_name()+";";
						str += f.getPersonnage().get_gfxID()+";";
						str += f.getPersonnage().get_lvl()+";";
						str += Integer.toString(f.getPersonnage().get_color1(), 36)+";";
						str += Integer.toString(f.getPersonnage().get_color2(), 36)+";";
						str += Integer.toString(f.getPersonnage().get_color3(), 36)+";";
						str += "0;";
					}
				}
		}
		return str;
	}
}
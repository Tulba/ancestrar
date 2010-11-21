package objects;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import objects.Personnage.Stats;

import org.joda.time.LocalDate;
import org.joda.time.Days;
import common.SQLManager;
import common.World;
import common.Constants;

public class Guild {
	private int _id;
	private String _name = "";
	private String _emblem = "";
	private Map<Integer,GuildMember> _members = new TreeMap<Integer,GuildMember>();
	private int _lvl;
	private long _xp;
	
	//Percepteur
	private int _capital = 0;
	private int _nbrPerco = 0;
	private Map<Integer, Integer> sorts = new TreeMap<Integer, Integer>();	//<ID, Level>
	private Map<Integer, Integer> stats = new TreeMap<Integer, Integer>(); //<Effet, Quantit�>
	//Stats en combat
	private Map<Integer,Integer> statsFight = new TreeMap<Integer,Integer>();
	
	public static class GuildMember
	{
		private int _guid;
		private Guild _guild;
		private String _name;
		private int _level;
		private int _gfx;
		private byte _align;
		private int _rank = 0;
		private byte _pXpGive = 0;
		private long _xpGave = 0;
		private int _rights = 0;
		private String _lastCo;
		
		//Droit
		private Map<Integer,Boolean> haveRight = new TreeMap<Integer,Boolean>();

		public GuildMember(int gu,Guild g,String name,int lvl,int gfx,int r,long x,byte pXp,int ri,byte a,String lastCo)
		{
			_guid = gu;
			_guild = g;
			_name = name;
			_level = lvl;
			_gfx = gfx;
			_rank = r;
			_xpGave = x;
			_pXpGive = pXp;
			_rights = ri;
			_align = a;
			_lastCo = lastCo;
			parseIntToRight(_rights);
		}
		
		public int getAlign()
		{
			return _align;
		}
		
		public int getGfx()
		{
			return _gfx;
		}
		
		public int getLvl()
		{
			return _level;
		}
		
		public String getName()
		{
			return _name;
		}
		
		public int getGuid()
		{
			return _guid;
		}
		public int getRank()
		{
			return _rank;
		}
		
		public Guild getGuild()
		{
			return _guild;
		}

		public String parseRights()
		{
			return Integer.toString(_rights,36);
		}

		public int getRights()
		{
			return _rights;
		}

		public long getXpGave() {
			return _xpGave;
		}

		public int getPXpGive()
		{
			return _pXpGive;
		}
		
		public String getLastCo()
		{
			return _lastCo;
		}
		
		public int getHoursFromLastCo()
		{
			String[] strDate = _lastCo.toString().split("~");
			
			LocalDate lastCo = new LocalDate(Integer.parseInt(strDate[0]),Integer.parseInt(strDate[1]),Integer.parseInt(strDate[2]));
			LocalDate now = new LocalDate();
			
			return Days.daysBetween(lastCo,now).getDays()*24;
		}

		public Personnage getPerso()
		{
			return World.getPersonnage(_guid);
		}

		public boolean canDo(int rightValue)
		{
			if(this._rights == 1)
				return true;
			
			return haveRight.get(rightValue);
		}

		public void setRank(int i)
		{
			_rank = i;
		}
		
		public void setAllRights(int rank,byte xp,int right)
		{
			if(rank == -1)
				rank = this._rank;
			
			if(xp < 0)
				xp = this._pXpGive;
			if(xp > 90)
				xp = 90;
			
			if(right == -1)
				right = this._rights;
			
			this._rank = rank;
			this._pXpGive = xp;
			
			if(right != this._rights && right != 1)	//V�rifie si les droits sont pareille ou si des droits de meneur; pour ne pas faire la conversion pour rien
				parseIntToRight(right);
			this._rights = right;
			
			SQLManager.UPDATE_GUILDMEMBER(this);
		}

		
		public void setLevel(int lvl)
		{
			this._level = lvl;
		}
		
		public void giveXpToGuild(long xp)
		{
			this._xpGave+=xp;
			this._guild.addXp(xp);
		}
		
		public void initRight()
		{
			haveRight.put(Constants.G_BOOST,false);
			haveRight.put(Constants.G_RIGHT,false);
			haveRight.put(Constants.G_INVITE,false);
			haveRight.put(Constants.G_BAN,false);
			haveRight.put(Constants.G_ALLXP,false);
			haveRight.put(Constants.G_HISXP,false);
			haveRight.put(Constants.G_RANK,false);
			haveRight.put(Constants.G_POSPERCO,false);
			haveRight.put(Constants.G_COLLPERCO,false);
			haveRight.put(Constants.G_USEENCLOS,false);
			haveRight.put(Constants.G_AMENCLOS,false);
			haveRight.put(Constants.G_OTHDINDE,false);
		}
		
		public void parseIntToRight(int total)
		{
			if(haveRight.isEmpty())
			{
				initRight();
			}
			if(total == 1)
				return;
			
			if(haveRight.size() > 0)	//Si les droits contiennent quelque chose -> Vidage (M�me si le TreeMap supprimerais les entr�es doublon lors de l'ajout)
				haveRight.clear();
				
			initRight();	//Remplissage des droits
			
			Integer[] mapKey = haveRight.keySet().toArray(new Integer[haveRight.size()]);	//R�cup�re les clef de map dans un tableau d'Integer
			
			while(total > 0)
			{
				for (int i = haveRight.size()-1; i < haveRight.size(); i--)
				{
					if(mapKey[i].intValue() <= total)
					{
						total ^= mapKey[i].intValue();
						haveRight.put(mapKey[i],true);
						break;
					}
				}
			}
		}
		
		public void setLastCo(String lastCo)
		{
			_lastCo = lastCo;
		}
	}

	public Guild(Personnage owner,String name,String emblem)
	{
		_id = World.getNextHighestGuildID();
		_name = name;
		_emblem = emblem;
		_lvl = 1;
		_xp= 0;
		decompileSpell("462;0|461;0|460;0|459;0|458;0|457;0|456;0|455;0|454;0|453;0|452;0|451;0|"); 
		decompileStats("176;100|158;1000|124;100|"); 
	}
	public Guild(int id,String name, String emblem,int lvl,long xp,
			int capital, int nbrmax,
			String sorts, String stats)
	{
		_id = id;
		_name = name;
		_emblem = emblem;
		_xp = xp;
		_lvl = lvl;
		_capital = capital;
		_nbrPerco = nbrmax;
		decompileSpell(sorts);
		decompileStats(stats);
		//Mise en place des stats
		statsFight.clear();
		statsFight.put(Constants.STATS_ADD_FORC, _lvl);
		statsFight.put(Constants.STATS_ADD_SAGE, get_Stats(Constants.STATS_ADD_SAGE));
		statsFight.put(Constants.STATS_ADD_INTE, _lvl);
		statsFight.put(Constants.STATS_ADD_CHAN, _lvl);
		statsFight.put(Constants.STATS_ADD_AGIL, _lvl);
		statsFight.put(Constants.STATS_ADD_RP_NEU, (int)Math.floor(get_lvl()/2));
		statsFight.put(Constants.STATS_ADD_RP_FEU, (int)Math.floor(get_lvl()/2));
		statsFight.put(Constants.STATS_ADD_RP_EAU, (int)Math.floor(get_lvl()/2));
		statsFight.put(Constants.STATS_ADD_RP_AIR, (int)Math.floor(get_lvl()/2));
		statsFight.put(Constants.STATS_ADD_RP_TER, (int)Math.floor(get_lvl()/2));
		statsFight.put(Constants.STATS_ADD_AFLEE, (int)Math.floor(get_lvl()/2));
		statsFight.put(Constants.STATS_ADD_MFLEE, (int)Math.floor(get_lvl()/2));
	}

	public GuildMember addMember(int guid,String name,int lvl,int gfx,int r,byte pXp,long x,int ri,byte a,String lastCo)
	{
		GuildMember GM = new GuildMember(guid,this,name,lvl,gfx,r,x,pXp,ri,a,lastCo);
		_members.put(guid,GM);
		return GM;
	}
	public GuildMember addNewMember(Personnage p)
	{
		GuildMember GM = new GuildMember(p.get_GUID(),this,p.get_name(),p.get_lvl(),p.get_gfxID(),0,0,(byte) 0,0,p.get_align(),p.get_compte().getLastConnectionDate());
		_members.put(p.get_GUID(),GM);
		return GM;
	}

	public int get_id()
	{
		return _id;
	}
	
	public int get_nbrPerco()
	{
		return _nbrPerco;
	}
	public void set_nbrPerco(int nbr)
	{
		_nbrPerco = nbr;
	}
	
	public int get_Capital()
	{
		return _capital;
	}
	public void set_Capital(int nbr)
	{
		_capital = nbr;
	}
	
	public Map<Integer, Integer> getSorts() {
		return sorts;
	}
	public Map<Integer, Integer> getStats() {
		return stats;
	}
	public void addStat(int stat, int qte)
	{
		int old = stats.get(stat);
		
		stats.put(stat, old + qte);
	}
	public void boostSort(int ID)
	{
		int old = sorts.get(ID);
		
		sorts.put(ID, old + 1);
	}
	public Stats getStatsFight()
	{
		return new Stats(statsFight);
	}
	
	public String get_name() {
		return _name;
	}
	public String get_emblem()
	{
		return _emblem;
	}
	public long get_xp()
	{
		return _xp;
	}
	public int get_lvl()
	{
		return _lvl;
	}
	public int getSize()
	{
		return _members.size();
	}
	public String parseMembersToGM()
	{
		String str = "";
		for(GuildMember GM : _members.values())
		{
			String online = "0";
			if(GM.getPerso() != null)if(GM.getPerso().isOnline())online = "1";
			if(str.length() != 0)str += "|";
			str += GM.getGuid()+";";
			str += GM.getName()+";";
			str += GM.getLvl()+";";
			str += GM.getGfx()+";";
			str += GM.getRank()+";";
			str += GM.getXpGave()+";";
			str += GM.getPXpGive()+";";
			str += GM.getRights()+";";
			str += online+";";
			str += GM.getAlign()+";";
			str += GM.getHoursFromLastCo();
		}
		return str;
	}
	public ArrayList<Personnage> getMembers()
	{
		ArrayList<Personnage> a = new ArrayList<Personnage>();
		for(GuildMember GM : _members.values())a.add(GM.getPerso());
		return a;
	}
	public GuildMember getMember(int guid)
	{
		return _members.get(guid);
	}
	public void removeMember(Personnage perso)
	{
		/*if(_members.get(guid).getRank() == 1 && _members.size() > 1)	//Si c'est le meneur et qu'il y a d'autre personne dans la guilde
		{
			GuildMember newMeneur = null;
			for(GuildMember curGm : _members.values())
			{
				if(curGm.getGuid() == guid)continue;
				
				if(newMeneur == null)
				{
					newMeneur = curGm;
					continue;
				}
				if(curGm.getRank() == 2)	//Si bras droit
				{
					newMeneur = curGm;
					break;
				}
				if(curGm.getXpGave() > newMeneur.getXpGave())
					newMeneur = curGm;
			}
			if(newMeneur != null)
				newMeneur.setRank(1);
		}*/
		House h = House.get_HouseByPerso(perso);
		if(h != null)
		{
			if(House.HouseOnGuild(_id) > 0)
			{
				SQLManager.HOUSE_GUILD(h, 0, 0);
			}
		}
		_members.remove(perso.get_GUID());
		SQLManager.DEL_GUILDMEMBER(perso.get_GUID());
	}
	
	public void addXp(long xp)
	{
		this._xp+=xp;
		
		while(_xp >= World.getGuildXpMax(_lvl) && _lvl<200)
			levelUp();
	}
	
	public void levelUp()
	{
		this._lvl++;
		this._capital = this._capital+5;
	}
	
	public void decompileSpell(String spellStr) //ID;lvl|ID;lvl|...
	{
		int id;
		int lvl;
		
		for(String split : spellStr.split("\\|"))
		{
			id = Integer.parseInt(split.split(";")[0]);
			lvl = Integer.parseInt(split.split(";")[1]);
			
			sorts.put(id, lvl);
		}
	}
	
	public void decompileStats(String statsStr) //ID;lvl|ID;lvl|...
	{
		int id;
		int value;
		
		for(String split : statsStr.split("\\|"))//pp pod sagesse
		{
			id = Integer.parseInt(split.split(";")[0]);
			value = Integer.parseInt(split.split(";")[1]);
			
			stats.put(id, value);
		}
	}
	
	public String compileSpell()
	{
		String toReturn = "";
		boolean isFirst = true;
		
		for(Entry<Integer, Integer> curSpell : sorts.entrySet())
		{
			if(!isFirst)
				toReturn += "|";
			
			toReturn += curSpell.getKey() + ";" + curSpell.getValue();
			
			isFirst = false;
		}
		
		return toReturn;
	}
	public String compileStats()
	{
		String toReturn = "";
		boolean isFirst = true;
		
		for(Entry<Integer, Integer> curStats : stats.entrySet())
		{
			if(!isFirst)
				toReturn += "|";
			
			toReturn += curStats.getKey() + ";" + curStats.getValue();
			
			isFirst = false;
		}
		
		return toReturn;
	}
	
	public void upgrade_Stats(int statsid, int add)
	{
		int actual = stats.get(statsid).intValue();
		stats.put(statsid, (actual+add));
	}
	
	public int get_Stats(int statsid)
	{
		int value = 0;
		for(Entry<Integer, Integer> curStats : stats.entrySet())
		{
			if(curStats.getKey() == statsid)
			{
				value = curStats.getValue();
			}
		}
		return value;
	}
	
	public String parsePercotoGuild()
	{
		//Percomax|0|100*level|level|perco_add_pods|perco_prospection|perco_sagesse|perco_max|perco_boost|1000+10*level|perco_spells
		String packet = get_nbrPerco()+"|"+Percepteur.CountPercoGuild(get_id())+"|"+100*get_lvl()+"|"+get_lvl()+"|"+get_Stats(158)+"|"+get_Stats(176)+"|"+get_Stats(124)+"|"+get_nbrPerco()+"|"+get_Capital()+"|"+(1000+(10*get_lvl()))+"|"+compileSpell();
		return packet;
	}
}

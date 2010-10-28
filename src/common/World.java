package common;

import game.GameServer;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import objects.*;
import objects.NPC_tmpl.*;
import objects.Objet.ObjTemplate;
import objects.Personnage.Stats;

public class World {

	private static Map<Integer,Compte> 	Comptes	= new TreeMap<Integer,Compte>();
	private static Map<Integer,Personnage> 	Persos	= new TreeMap<Integer,Personnage>();
	private static Map<Short,Carte> 	Cartes	= new TreeMap<Short,Carte>();
	private static Map<Integer,Objet> 	Objets	= new TreeMap<Integer,Objet>();
	private static Map<Integer,ExpLevel> ExpLevels = new TreeMap<Integer, ExpLevel>();
	private static Map<Integer,Sort>	Sorts = new TreeMap<Integer,Sort>();
	private static Map<Integer,ObjTemplate> ObjTemplates = new TreeMap<Integer,ObjTemplate>();
	private static Map<Integer,Monstre> MobTemplates = new TreeMap<Integer,Monstre>();
	private static Map<Integer,NPC_tmpl> NPCTemplates = new TreeMap<Integer,NPC_tmpl>();
	private static Map<Integer,NPC_question> NPCQuestions = new TreeMap<Integer,NPC_question>();
	private static Map<Integer,NPC_reponse> NPCReponses = new TreeMap<Integer,NPC_reponse>();
	private static Map<Integer,IOTemplate> IOTemplate = new TreeMap<Integer,IOTemplate>();
	private static Map<Integer,Dragodinde> Dragodindes = new TreeMap<Integer,Dragodinde>();
	private static Map<Integer,SuperArea> SuperAreas = new TreeMap<Integer,SuperArea>();
	private static Map<Integer,Area> Areas = new TreeMap<Integer,Area>();
	private static Map<Integer,SubArea> SubAreas = new TreeMap<Integer,SubArea>();
	private static Map<Integer,Metier> Jobs = new TreeMap<Integer,Metier>();
	private static Map<Integer,ArrayList<Couple<Integer,Integer>>> Crafts = new TreeMap<Integer,ArrayList<Couple<Integer,Integer>>>();
	private static Map<Integer,ItemSet> ItemSets = new TreeMap<Integer,ItemSet>();
	private static Map<Integer,Guild> Guildes = new TreeMap<Integer,Guild>();
	private static Map<Integer,Percepteur> Percepteur = new TreeMap<Integer,Percepteur>();
	private static Map<Integer,House> House = new TreeMap<Integer,House>();
	
	private static int saveTry = 1;
	//Statut du serveur 1: accesible 0: inaccesible
	private static short _state = 1;
	
	public static class Drop
	{
		private int _itemID;
		private int _prosp;
		private float _taux;
		private int _max;
		
		public Drop(int itm,int p,float t,int m)
		{
			_itemID = itm;
			_prosp = p;
			_taux = t;
			_max = m;
		}
		public void setMax(int m)
		{
			_max = m;
		}
		public int get_itemID() {
			return _itemID;
		}

		public int getMinProsp() {
			return _prosp;
		}

		public float get_taux() {
			return _taux;
		}

		public int get_max() {
			return _max;
		}
	}

	public static class ItemSet
	{
		private int _id;
		private ArrayList<ObjTemplate> _itemTemplates = new ArrayList<ObjTemplate>();
		private ArrayList<Stats> _bonuses = new ArrayList<Stats>();
		
		public ItemSet (int id,String items, String bonuses)
		{
			_id = id;
			//parse items String
			for(String str : items.split(","))
			{
				try
				{
					ObjTemplate t = World.getObjTemplate(Integer.parseInt(str.trim()));
					if(t == null)continue;
					_itemTemplates.add(t);
				}catch(Exception e){};
			}
			
			//on ajoute un bonus vide pour 1 item
			_bonuses.add(new Stats());
			//parse bonuses String
			for(String str : bonuses.split(";"))
			{
				Stats S = new Stats();
				//s�paration des bonus pour un m�me nombre d'item
				for(String str2 : str.split(","))
				{
					try
					{
						String[] infos = str2.split(":");
						int stat = Integer.parseInt(infos[0]);
						int value = Integer.parseInt(infos[1]);
						//on ajoute a la stat
						S.addOneStat(stat, value);
					}catch(Exception e){};
				}
				//on ajoute la stat a la liste des bonus
				_bonuses.add(S);
			}
		}

		public int getId()
		{
			return _id;
		}
		
		public Stats getBonusStatByItemNumb(int numb)
		{
			if(numb>_bonuses.size())return new Stats();
			return _bonuses.get(numb-1);
		}
		
		public ArrayList<ObjTemplate> getItemTemplates()
		{
			return _itemTemplates;
		}
	}
	
	public static class SuperArea
	{
		private int _id;
		private ArrayList<Area> _areas = new ArrayList<Area>();
		
		public SuperArea(int a_id)
		{
			_id = a_id;
		}
		
		public void addArea(Area A)
		{
			_areas.add(A);
		}
		
		public int get_id()
		{
			return _id;
		}
	}
	
	public static class Area
	{
		private int _id;
		private SuperArea _superArea;
		private String _name;
		private ArrayList<SubArea> _subAreas = new ArrayList<SubArea>();
		
		public Area(int id, int superArea,String name)
		{
			_id = id;
			_name = name;
			_superArea = World.getSuperArea(superArea);
			//Si le continent n'est pas encore cr�er, on le cr�er et on l'ajoute au monde
			if(_superArea == null)
			{
				_superArea = new SuperArea(superArea);
				World.addSuperArea(_superArea);
			}
		}
		public String get_name()
		{
			return _name;
		}
		public int get_id()
		{
			return _id;
		}
		
		public SuperArea get_superArea()
		{
			return _superArea;
		}
		
		public void addSubArea(SubArea sa)
		{
			_subAreas.add(sa);
		}
		
		public ArrayList<Carte> getMaps()
		{
			ArrayList<Carte> maps = new ArrayList<Carte>();
			for(SubArea SA : _subAreas)maps.addAll(SA.getMaps());
			return maps;
		}
	}
	
	public static class SubArea
	{
		private int _id;
		private Area _area;
		private int _alignement;
		private String _name;
		private ArrayList<Carte> _maps = new ArrayList<Carte>();
		
		public SubArea(int id, int areaID, int alignement,String name)
		{
			_id = id;
			_name = name;
			_area =  World.getArea(areaID);
			_alignement = alignement;
		}
		
		public String get_name()
		{
			return _name;
		}
		public int get_id() {
			return _id;
		}
		public Area get_area() {
			return _area;
		}
		public int get_alignement() {
			return _alignement;
		}
		public ArrayList<Carte> getMaps() {
			return _maps;
		}

		public void addMap(Carte carte)
		{
			_maps.add(carte);
		}
		
	}
	
	public static class Couple<L,R>
	{
	    public L first;
	    public R second;

	    public Couple(L s, R i)
	    {
	         this.first = s;
	         this.second = i;
	    }
	}

	public static class IOTemplate
	{
		private int _id;
		private int _respawnTime;
		private int _duration;
		private int _unk;
		private boolean _walkable;
		
		public IOTemplate(int a_i,int a_r,int a_d,int a_u, boolean a_w)
		{
			_id = a_i;
			_respawnTime = a_r;
			_duration = a_d;
			_unk = a_u;
			_walkable = a_w;
		}
		
		public int getId() {
			return _id;
		}	
		public boolean isWalkable() {
			return _walkable;
		}

		public int getRespawnTime() {
			return _respawnTime;
		}
		public int getDuration() {
			return _duration;
		}
		public int getUnk() {
			return _unk;
		}
	}
	
	public static class Exchange
	{
		private Personnage perso1;
		private Personnage perso2;
		private long kamas1 = 0;
		private long kamas2 = 0;
		private ArrayList<Couple<Integer,Integer>> items1 = new ArrayList<Couple<Integer,Integer>>();
		private ArrayList<Couple<Integer,Integer>> items2 = new ArrayList<Couple<Integer,Integer>>();
		private boolean ok1;
		private boolean ok2;
		
		public Exchange(Personnage p1, Personnage p2)
		{
			perso1 = p1;
			perso2 = p2;
		}
		
		synchronized public long getKamas(int guid)
		{
			int i = 0;
			if(perso1.get_GUID() == guid)
				i = 1;
			else if(perso2.get_GUID() == guid)
				i = 2;
			
			if(i == 1)
				return kamas1;
			else if (i == 2)
				return kamas2;
			return 0;
		}
		
		synchronized public void toogleOK(int guid)
		{
			int i = 0;
			if(perso1.get_GUID() == guid)
				i = 1;
			else if(perso2.get_GUID() == guid)
				i = 2;
			
			if(i == 1)
			{
				ok1 = !ok1;
				SocketManager.GAME_SEND_EXCHANGE_OK(perso1.get_compte().getGameThread().get_out(),ok1,guid);
				SocketManager.GAME_SEND_EXCHANGE_OK(perso2.get_compte().getGameThread().get_out(),ok1,guid);
			}
			else if (i == 2)
			{
				ok2 = !ok2;
				SocketManager.GAME_SEND_EXCHANGE_OK(perso1.get_compte().getGameThread().get_out(),ok2,guid);
				SocketManager.GAME_SEND_EXCHANGE_OK(perso2.get_compte().getGameThread().get_out(),ok2,guid);
			}
			else 
				return;
			
			
			if(ok1 && ok2)
				apply();
		}
		
		synchronized public void setKamas(int guid, long k)
		{
			ok1 = false;
			ok2 = false;
			
			int i = 0;
			if(perso1.get_GUID() == guid)
				i = 1;
			else if(perso2.get_GUID() == guid)
				i = 2;
			SocketManager.GAME_SEND_EXCHANGE_OK(perso1.get_compte().getGameThread().get_out(),ok1,perso1.get_GUID());
			SocketManager.GAME_SEND_EXCHANGE_OK(perso2.get_compte().getGameThread().get_out(),ok1,perso1.get_GUID());
			SocketManager.GAME_SEND_EXCHANGE_OK(perso1.get_compte().getGameThread().get_out(),ok2,perso2.get_GUID());
			SocketManager.GAME_SEND_EXCHANGE_OK(perso2.get_compte().getGameThread().get_out(),ok2,perso2.get_GUID());
			
			if(i == 1)
			{
				kamas1 = k;
				SocketManager.GAME_SEND_EXCHANGE_MOVE_OK(perso1, 'G', "", k+"");
				SocketManager.GAME_SEND_EXCHANGE_OTHER_MOVE_OK(perso2.get_compte().getGameThread().get_out(), 'G', "", k+"");
			}else if (i == 2)
			{
				kamas2 = k;
				SocketManager.GAME_SEND_EXCHANGE_OTHER_MOVE_OK(perso1.get_compte().getGameThread().get_out(), 'G', "", k+"");
				SocketManager.GAME_SEND_EXCHANGE_MOVE_OK(perso2, 'G', "", k+"");	
			}
		}
		
		synchronized public void cancel()
		{
			if(perso1.get_compte() != null)if(perso1.get_compte().getGameThread() != null)SocketManager.GAME_SEND_EV_PACKET(perso1.get_compte().getGameThread().get_out());
			if(perso2.get_compte() != null)if(perso2.get_compte().getGameThread() != null)SocketManager.GAME_SEND_EV_PACKET(perso2.get_compte().getGameThread().get_out());
			perso1.set_isTradingWith(0);
			perso2.set_isTradingWith(0);
			perso1.setCurExchange(null);
			perso2.setCurExchange(null);
		}
		
		synchronized public void apply()
		{
			//Gestion des Kamas
			perso1.addKamas((-kamas1+kamas2));
			perso2.addKamas((-kamas2+kamas1));
			for(Couple<Integer, Integer> couple : items1)
			{
				if(couple.second == 0)continue;
				if(!perso1.hasItemGuid(couple.first))//Si le perso n'a pas l'item (Ne devrait pas arriver)
				{
					couple.second = 0;//On met la quantit� a 0 pour �viter les problemes
					continue;
				}	
				Objet obj = World.getObjet(couple.first);
				if((obj.getQuantity() - couple.second) <1)//S'il ne reste plus d'item apres l'�change
				{
					perso1.removeItem(couple.first);
					couple.second = obj.getQuantity();
					SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(perso1, couple.first);
					if(!perso2.addObjet(obj, true))//Si le joueur avait un item similaire
						World.removeItem(couple.first);//On supprime l'item inutile
				}else
				{
					obj.setQuantity(obj.getQuantity()-couple.second);
					SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(perso1, obj);
					Objet newObj = Objet.getCloneObjet(obj, couple.second);
					if(perso2.addObjet(newObj, true))//Si le joueur n'avait pas d'item similaire
						World.addObjet(newObj,true);//On ajoute l'item au World
				}
			}
			for(Couple<Integer, Integer> couple : items2)
			{
				if(couple.second == 0)continue;
				if(!perso2.hasItemGuid(couple.first))//Si le perso n'a pas l'item (Ne devrait pas arriver)
				{
					couple.second = 0;//On met la quantit� a 0 pour �viter les problemes
					continue;
				}	
				Objet obj = World.getObjet(couple.first);
				if((obj.getQuantity() - couple.second) <1)//S'il ne reste plus d'item apres l'�change
				{
					perso2.removeItem(couple.first);
					couple.second = obj.getQuantity();
					SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(perso2, couple.first);
					if(!perso1.addObjet(obj, true))//Si le joueur avait un item similaire
						World.removeItem(couple.first);//On supprime l'item inutile
				}else
				{
					obj.setQuantity(obj.getQuantity()-couple.second);
					SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(perso2, obj);
					Objet newObj = Objet.getCloneObjet(obj, couple.second);
					if(perso1.addObjet(newObj, true))//Si le joueur n'avait pas d'item similaire
						World.addObjet(newObj,true);//On ajoute l'item au World
				}
			}
			//Fin
			perso1.set_isTradingWith(0);
			perso2.set_isTradingWith(0);
			perso1.setCurExchange(null);
			perso2.setCurExchange(null);
			SocketManager.GAME_SEND_Ow_PACKET(perso1);
			SocketManager.GAME_SEND_Ow_PACKET(perso2);
			SocketManager.GAME_SEND_STATS_PACKET(perso1);
			SocketManager.GAME_SEND_STATS_PACKET(perso2);
			SocketManager.GAME_SEND_EXCHANGE_VALID(perso1.get_compte().getGameThread().get_out(),'a');
			SocketManager.GAME_SEND_EXCHANGE_VALID(perso2.get_compte().getGameThread().get_out(),'a');	
			SQLManager.SAVE_PERSONNAGE(perso1,true);
			SQLManager.SAVE_PERSONNAGE(perso2,true);
		}

		synchronized public void addItem(int guid, int qua, int pguid)
		{
			ok1 = false;
			ok2 = false;
			
			Objet obj = World.getObjet(guid);
			int i = 0;
			
			if(perso1.get_GUID() == pguid) i = 1;
			if(perso2.get_GUID() == pguid) i = 2;
			
			if(qua == 1) qua = 1;
			String str = guid+"|"+qua;
			if(obj == null)return;
			String add = "|"+obj.getTemplate().getID()+"|"+obj.parseStatsString();
			SocketManager.GAME_SEND_EXCHANGE_OK(perso1.get_compte().getGameThread().get_out(),ok1,perso1.get_GUID());
			SocketManager.GAME_SEND_EXCHANGE_OK(perso2.get_compte().getGameThread().get_out(),ok1,perso1.get_GUID());
			SocketManager.GAME_SEND_EXCHANGE_OK(perso1.get_compte().getGameThread().get_out(),ok2,perso2.get_GUID());
			SocketManager.GAME_SEND_EXCHANGE_OK(perso2.get_compte().getGameThread().get_out(),ok2,perso2.get_GUID());
			if(i == 1)
			{
				Couple<Integer,Integer> couple = getCoupleInList(items1,guid);
				if(couple != null)
				{
					couple.second += qua;
					SocketManager.GAME_SEND_EXCHANGE_MOVE_OK(perso1, 'O', "+", ""+guid+"|"+couple.second);
					SocketManager.GAME_SEND_EXCHANGE_OTHER_MOVE_OK(perso2.get_compte().getGameThread().get_out(), 'O', "+", ""+guid+"|"+couple.second+add);
					return;
				}
				SocketManager.GAME_SEND_EXCHANGE_MOVE_OK(perso1, 'O', "+", str);
				SocketManager.GAME_SEND_EXCHANGE_OTHER_MOVE_OK(perso2.get_compte().getGameThread().get_out(), 'O', "+", str+add);	
				items1.add(new Couple<Integer,Integer>(guid,qua));
			}else if(i == 2)
			{
				Couple<Integer,Integer> couple = getCoupleInList(items2,guid);
				if(couple != null)
				{
					couple.second += qua;
					SocketManager.GAME_SEND_EXCHANGE_MOVE_OK(perso2, 'O', "+", ""+guid+"|"+couple.second);
					SocketManager.GAME_SEND_EXCHANGE_OTHER_MOVE_OK(perso1.get_compte().getGameThread().get_out(), 'O', "+", ""+guid+"|"+couple.second+add);
					return;
				}
				SocketManager.GAME_SEND_EXCHANGE_MOVE_OK(perso2, 'O', "+", str);
				SocketManager.GAME_SEND_EXCHANGE_OTHER_MOVE_OK(perso1.get_compte().getGameThread().get_out(), 'O', "+", str+add);
				items2.add(new Couple<Integer,Integer>(guid,qua));
			}
		}

		
		synchronized public void removeItem(int guid, int qua, int pguid)
		{
			int i = 0;
			if(perso1.get_GUID() == pguid)
				i = 1;
			else if(perso2.get_GUID() == pguid)
				i = 2;
			ok1 = false;
			ok2 = false;
			
			SocketManager.GAME_SEND_EXCHANGE_OK(perso1.get_compte().getGameThread().get_out(),ok1,perso1.get_GUID());
			SocketManager.GAME_SEND_EXCHANGE_OK(perso2.get_compte().getGameThread().get_out(),ok1,perso1.get_GUID());
			SocketManager.GAME_SEND_EXCHANGE_OK(perso1.get_compte().getGameThread().get_out(),ok2,perso2.get_GUID());
			SocketManager.GAME_SEND_EXCHANGE_OK(perso2.get_compte().getGameThread().get_out(),ok2,perso2.get_GUID());
			
			Objet obj = World.getObjet(guid);
			if(obj == null)return;
			String add = "|"+obj.getTemplate().getID()+"|"+obj.parseStatsString();
			if(i == 1)
			{
				Couple<Integer,Integer> couple = getCoupleInList(items1,guid);
				int newQua = couple.second - qua;
				if(newQua <1)//Si il n'y a pu d'item
				{
					items1.remove(couple);
					SocketManager.GAME_SEND_EXCHANGE_MOVE_OK(perso1, 'O', "-", ""+guid);
					SocketManager.GAME_SEND_EXCHANGE_OTHER_MOVE_OK(perso2.get_compte().getGameThread().get_out(), 'O', "-", ""+guid);
				}else
				{
					couple.second = newQua;
					SocketManager.GAME_SEND_EXCHANGE_MOVE_OK(perso1, 'O', "+", ""+guid+"|"+newQua);
					SocketManager.GAME_SEND_EXCHANGE_OTHER_MOVE_OK(perso2.get_compte().getGameThread().get_out(), 'O', "+", ""+guid+"|"+newQua+add);
				}
			}else if(i ==2)
			{
				Couple<Integer,Integer> couple = getCoupleInList(items2,guid);
				int newQua = couple.second - qua;
				
				if(newQua <1)//Si il n'y a pu d'item
				{
					items2.remove(couple);
					SocketManager.GAME_SEND_EXCHANGE_OTHER_MOVE_OK(perso1.get_compte().getGameThread().get_out(), 'O', "-", ""+guid);
					SocketManager.GAME_SEND_EXCHANGE_MOVE_OK(perso2, 'O', "-", ""+guid);
				}else
				{
					couple.second = newQua;
					SocketManager.GAME_SEND_EXCHANGE_OTHER_MOVE_OK(perso1.get_compte().getGameThread().get_out(), 'O', "+", ""+guid+"|"+newQua+add);
					SocketManager.GAME_SEND_EXCHANGE_MOVE_OK(perso2, 'O', "+", ""+guid+"|"+newQua);
				}
			}
		}

		synchronized private Couple<Integer, Integer> getCoupleInList(ArrayList<Couple<Integer, Integer>> items,int guid)
		{
			for(Couple<Integer, Integer> couple : items)
			{
				if(couple.first == guid)
					return couple;
			}
			return null;
		}
		
		public synchronized int getQuaItem(int itemID, int playerGuid)
		{
			ArrayList<Couple<Integer, Integer>> items;
			if(perso1.get_GUID() == playerGuid)
				items = items1;
			else
				items = items2;
			
			for(Couple<Integer, Integer> curCoupl : items)
			{
				if(curCoupl.first == itemID)
				{
					return curCoupl.second;
				}
			}
			
			return 0;
		}
		
	}

	public static class ExpLevel
	{
		public long perso;
		public int metier;
		public int dinde;
		public int pvp;
		public long guilde;
		
		public ExpLevel(long c, int m, int d, int p)
		{
			perso = c;
			metier = m;
			dinde = d;
			pvp = p;
			guilde = perso*10;
		}
		
	}
	
	public static void createWorld()
	{
		System.out.println("====>Donn�es statique<====");
		System.out.println("Chargement des niveaux d'exp�riences:");
		SQLManager.LOAD_EXP();
		System.out.println(ExpLevels.size()+" niveaux ont ete charg�s");
		System.out.println("Chargement des sorts:");
		SQLManager.LOAD_SORTS();
		System.out.println(Sorts.size()+" sorts ont ete charg�s");
		System.out.println("Chargement des templates de monstre:");
		SQLManager.LOAD_MOB_TEMPLATE();
		System.out.println(MobTemplates.size()+" templates de monstre ont ete charg�es");
		System.out.println("Chargement des templates d'objet:");
		SQLManager.LOAD_OBJ_TEMPLATE();
		System.out.println(ObjTemplates.size()+" templates d'objet ont ete charg�es");
		System.out.println("Chargement des templates de NPC:");
		SQLManager.LOAD_NPC_TEMPLATE();
		System.out.println(NPCTemplates.size()+" templates de NPC ont ete charg�es");
		System.out.println("Chargement des questions de NPC:");
		SQLManager.LOAD_NPC_QUESTIONS();
		System.out.println(NPCQuestions.size()+" questions de NPC ont ete charges");
		System.out.println("Chargement des r�ponses de NPC:");
		SQLManager.LOAD_NPC_ANSWERS();
		System.out.println(NPCReponses.size()+" r�ponses de NPC ont ete chargees");
		System.out.println("Chargement des zones:");
		SQLManager.LOAD_AREA();
		System.out.println(Areas.size()+" zones ont ete charg�es");
		System.out.println("Chargement des sous-zone:");
		SQLManager.LOAD_SUBAREA();
		System.out.println(SubAreas.size()+" sous-zones ont ete charg�es");
		System.out.println("Chargement des template d'objet interactifs:");
		SQLManager.LOAD_IOTEMPLATE();
		System.out.println(IOTemplate.size()+" template d'IO ont ete charges");
		System.out.println("Chargement des recettes:");
		SQLManager.LOAD_CRAFTS();
		System.out.println(Crafts.size()+" recettes ont ete chargses");
		System.out.println("Chargement des metiers:");
		SQLManager.LOAD_JOBS();
		System.out.println(Jobs.size()+" metiers ont ete charges");
		System.out.println("Chargement des panolies:");
		SQLManager.LOAD_ITEMSETS();
		System.out.println(ItemSets.size()+" panoplies ont ete charg�es");
		System.out.println("Chargement des maps:");
		SQLManager.LOAD_MAPS();
		System.out.println(Cartes.size()+" maps ont ete charg�es");
		System.out.println("Chargement des Triggers:");
		int nbr = SQLManager.LOAD_TRIGGERS();
		System.out.println(nbr+" triggers ont ete charges");
		System.out.println("Chargement des actions de fin de combat:");
		nbr = SQLManager.LOAD_ENDFIGHT_ACTIONS();
		System.out.println(nbr+" actions ont ete charges");
		System.out.println("Chargement des npcs:");
		nbr = SQLManager.LOAD_NPCS();
		System.out.println(nbr+" npcs ont ete charg�es");
		System.out.println("Chargement des actions des objets:");
		nbr = SQLManager.LOAD_ITEM_ACTIONS();
		System.out.println(nbr+" actions ont ete chargees");
		System.out.print("Chargement des Drops: ");
		SQLManager.LOAD_DROPS();
		System.out.println("Ok !");
		
		System.out.println("====>Donn�es dynamique<====");
		
		System.out.print("Mise a 0 des logged: ");
		SQLManager.LOGGED_ZERO();
		System.out.println("Ok !");

		System.out.println("Chargement des items:");
		SQLManager.LOAD_ITEMS_FULL();
		System.out.println("OK");
		
		System.out.println("Chargement des comptes:");
		SQLManager.LOAD_COMPTES();
		System.out.println(Comptes.size()+" comptes charges");
		
		System.out.println("Chargement des personnages:");//Obligatoire pour les HDV, sinon le seller peut etre innexistant
		SQLManager.LOAD_PERSOS();
		System.out.println(Persos.size()+" personnages charges");

	    System.out.print("Chargement des guildes:");
		SQLManager.LOAD_GUILDS();
		System.out.println(Guildes.size()+" guildes ont ete chargees");

		System.out.print("Chargement des dragodindes:");
		SQLManager.LOAD_MOUNTS();
		System.out.println(Dragodindes.size()+" dragodindes ont ete chargees");
		
		System.out.print("Chargement des membres de guildes:");
		SQLManager.LOAD_GUILD_MEMBERS();
		System.out.println("Ok !");
		
		System.out.print("Chargement des donnees d'enclos:");
		SQLManager.LOAD_MOUNTPARKS();
		System.out.println("Ok !");
		
		System.out.print("Chargement des percepteurs:");
		nbr = SQLManager.LOAD_PERCEPTEURS();
		System.out.println(nbr+" percepteurs charges");
		
		System.out.print("Chargement des maisons:");
		nbr = SQLManager.LOAD_HOUSES();
		System.out.println(nbr+" maisons chargees");
		
		System.out.print("Chargement des zaaps:");
		nbr = SQLManager.LOAD_ZAAPS();
		System.out.println(nbr+" zaaps chargees");
	}
	
	public static Area getArea(int areaID)
	{
		return Areas.get(areaID);
	}

	public static SuperArea getSuperArea(int areaID)
	{
		return SuperAreas.get(areaID);
	}
	
	public static SubArea getSubArea(int areaID)
	{
		return SubAreas.get(areaID);
	}
	
	public static void addArea(Area area)
	{
		Areas.put(area.get_id(), area);
	}
	
	public static void addSuperArea(SuperArea SA)
	{
		SuperAreas.put(SA.get_id(), SA);
	}
	
	public static void addSubArea(SubArea SA)
	{
		SubAreas.put(SA.get_id(), SA);
	}
	
	public static void addNPCreponse(NPC_reponse rep)
	{
		NPCReponses.put(rep.get_id(), rep);
	}
	
	public static NPC_reponse getNPCreponse(int guid)
	{
		return NPCReponses.get(guid);
	}
	
	public static void addExpLevel(int lvl,ExpLevel exp)
	{
		ExpLevels.put(lvl, exp);
	}
	
	public static Compte getCompte(int guid)
	{
		return Comptes.get(guid);
	}
	
	public static void addNPCQuestion(NPC_question quest)
	{
		NPCQuestions.put(quest.get_id(), quest);
	}
	
	public static NPC_question getNPCQuestion(int guid)
	{
		return NPCQuestions.get(guid);
	}
	public static NPC_tmpl getNPCTemplate(int guid)
	{
		return NPCTemplates.get(guid);
	}
	
	public static void addNpcTemplate(NPC_tmpl temp)
	{
		NPCTemplates.put(temp.get_id(), temp);
	}
	
	public static Carte getCarte(short id)
	{
		return Cartes.get(id);
	}
	
	public static  void addCarte(Carte map)
	{
		if(!Cartes.containsKey(map.get_id()))
			Cartes.put(map.get_id(),map);
	}
	
	public static void delCarte(Carte map) 
	{
		  if (Cartes.containsKey(map.get_id()))
			  Cartes.remove(map.get_id());
	}
	
	public static Compte getCompteByName(String name)
	{
		for(int a = 0; a< Comptes.keySet().size(); a++)
		{
			if(Comptes.get(Comptes.keySet().toArray()[a]).get_name().equalsIgnoreCase(name))
				return Comptes.get(Comptes.keySet().toArray()[a]);
		}
		return null;
	}
	
	public static Personnage getPersonnage(int guid)
	{
		return Persos.get(guid);
	}
	
	public static void addAccount(Compte compte)
	{
		Comptes.put(compte.get_GUID(), compte);
	}

	public static void addPersonnage(Personnage perso)
	{
		Persos.put(perso.get_GUID(), perso);
	}

	public static Personnage getPersoByName(String name)
	{
		ArrayList<Personnage> Ps = new ArrayList<Personnage>();
		Ps.addAll(Persos.values());
		for(Personnage P : Ps)if(P.get_name().equalsIgnoreCase(name))return P;
		return null;
	}

	public static void deletePerso(Personnage perso)
	{
		Persos.remove(perso.get_GUID());
		SQLManager.DELETE_PERSO_IN_BDD(perso);
	}

	public static String getSousZoneStateString()
	{
		String data = "";
		/* TODO: Sous Zone Alignement */
		return data;
	}
	
	public static long getPersoXpMin(int _lvl)
	{
		if(_lvl > 200) 	_lvl = 200;
		if(_lvl < 1) 	_lvl = 1;
		return ExpLevels.get(_lvl).perso;
	}
	
	public static long getPersoXpMax(int _lvl)
	{
		if(_lvl >= 200) 	_lvl = 199;
		if(_lvl <= 1)	 	_lvl = 1;
		return ExpLevels.get(_lvl+1).perso;
	}
	
	public static void addSort(Sort sort)
	{
		Sorts.put(sort.getSpellID(), sort);
	}

	public static void addObjTemplate(ObjTemplate obj)
	{
		ObjTemplates.put(obj.getID(), obj);
	}
	
	public static Sort getSort(int id)
	{
		return Sorts.get(id);
	}

	public static ObjTemplate getObjTemplate(int id)
	{
		return ObjTemplates.get(id);
	}
	
	public synchronized static int getNewItemGuid()
	{
		int id = 0;
		for(Entry<Integer,Objet> entry : Objets.entrySet())
		{
			if(entry.getKey() > id)
				id = entry.getKey();
		}
		id++;
		return id;
	}

	public static void addMobTemplate(int id,Monstre mob)
	{
		MobTemplates.put(id, mob);
	}

	public static Monstre getMonstre(int id)
	{
		return MobTemplates.get(id);
	}

	public static List<Personnage> getOnlinePersos()
	{
		List<Personnage> online = new ArrayList<Personnage>();
		for(Entry<Integer,Personnage> perso : Persos.entrySet())
		{
			if(perso.getValue().isOnline() && perso.getValue().get_compte().getGameThread() != null)
			{
				if(perso.getValue().get_compte().getGameThread().get_out() != null)
				{
					online.add(perso.getValue());
				}
			}
		}
		return online;
	}

	public static void addObjet(Objet item, boolean saveSQL)
	{
		Objets.put(item.getGuid(), item);
		if(saveSQL)
			SQLManager.SAVE_NEW_ITEM(item);
	}
	public static Objet getObjet(int guid)
	{
		return Objets.get(guid);
	}

	public static void removeItem(int guid)
	{
		Objets.remove(guid);
		SQLManager.DELETE_ITEM(guid);
	}

	public static void addIOTemplate(IOTemplate IOT)
	{
		IOTemplate.put(IOT.getId(), IOT);
	}
	
	public static Dragodinde getDragoByID(int id)
	{
		return Dragodindes.get(id);
	}
	public static void addDragodinde(Dragodinde DD)
	{
		Dragodindes.put(DD.get_id(), DD);
	}
	public static void saveAll(Personnage saver)
	{
		PrintWriter _out = null;
		if(saver != null)
		_out = saver.get_compte().getGameThread().get_out();
		
		set_state((short)0);

		try
		{
			Ancestra.isSaving = true;
			
			SQLManager.commitTransacts();
			SQLManager.TIMER(false);	//Arr�te le timer d'enregistrement SQL
			
			GameServer.addToLog("Sauvegarde des personnages...");
			for(Personnage perso : Persos.values())
			{
				if(!perso.isOnline())continue;
				SQLManager.SAVE_PERSONNAGE(perso,true);//sauvegarde des persos et de leurs items
			}
			GameServer.addToLog("Sauvegarde des guildes...");
			for(Guild guilde : Guildes.values())
			{
				SQLManager.UPDATE_GUILD(guilde);
			}
			GameServer.addToLog("Sauvegarde des percepteurs...");
			for(Percepteur perco : Percepteur.values())
			{
				if(perco.get_inFight()>0)continue;
				SQLManager.UPDATE_PERCO(perco);
			}
			GameServer.addToLog("Sauvegarde des maisons...");
			for(House house : House.values())
			{
				if(house.get_owner_id() > 0)
				{
					SQLManager.UPDATE_HOUSE(house);
				}
			}
			GameServer.addToLog("Sauvegarde effectuee !");
			try
			{
				Thread.sleep(10000);
			}catch(ConcurrentModificationException e){}
			
			set_state((short)1);
			//TODO : Rafraichir 
			
		}catch(ConcurrentModificationException e)
		{
			if(saveTry < 10)
			{
				GameServer.addToLog("Nouvelle tentative de sauvegarde");
				if(saver != null && _out != null)
					SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Erreur. Nouvelle tentative de sauvegarde");
				saveTry++;
				saveAll(saver);
			}
			else
			{
				set_state((short)1);
				//TODO : Rafraichir 
				String mess = "�chec de la sauvegarde apr�s " + saveTry + " tentatives";
				if(saver != null && _out != null)
					SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
				GameServer.addToLog(mess);
			}
				
		}catch(Exception e)
		{
			GameServer.addToLog("Erreur lors de la sauvegarde : " + e.getMessage());
			e.printStackTrace();
		}
		finally
		{
			SQLManager.commitTransacts();
			SQLManager.TIMER(true); //Red�marre le timer d'enregistrement SQL
			Ancestra.isSaving = false;
			saveTry = 1;
		}
	}
	public static void RefreshAllMob()
	{
		SocketManager.GAME_SEND_MESSAGE_TO_ALL("Recharge des Mobs en cours, des latences peuvent survenir.", Ancestra.CONFIG_MOTD_COLOR);
		for(Carte map : Cartes.values())
		{
			map.refreshSpawns();
		}
		SocketManager.GAME_SEND_MESSAGE_TO_ALL("Recharge des Mobs finie. La prochaine recharge aura lieu dans 5heures.", Ancestra.CONFIG_MOTD_COLOR);
	}

	public static ExpLevel getExpLevel(int lvl)
	{
		return ExpLevels.get(lvl);
	}
	public static IOTemplate getIOTemplate(int id)
	{
		return IOTemplate.get(id);
	}
	public static Metier getMetier(int id)
	{
		return Jobs.get(id);
	}

	public static void addJob(Metier metier)
	{
		Jobs.put(metier.getId(), metier);
	}

	public static void addCraft(int id, ArrayList<Couple<Integer, Integer>> m)
	{
		Crafts.put(id,m);
	}
	
	public static ArrayList<Couple<Integer,Integer>> getCraft(int i)
	{
		return Crafts.get(i);
	}

	public static int getObjectByIngredientForJob( ArrayList<Integer> list, Map<Integer, Integer> ingredients)
	{
		if(list == null)return -1;
		for(int tID : list)
		{
			ArrayList<Couple<Integer,Integer>> craft = World.getCraft(tID);
			if(craft == null)
			{
				GameServer.addToLog("/!\\Recette pour l'objet "+tID+" non existante !");
				continue;
			}
			if(craft.size() != ingredients.size())continue;
			boolean ok = true;
			for(Couple<Integer,Integer> c : craft)
			{
				//si ingredient non pr�sent ou mauvaise quantit�
				if(ingredients.get(c.first) != c.second)ok = false;
			}
			if(ok)return tID;
		}
		return -1;
	}
	public static Compte getCompteByPseudo(String p)
	{
		for(Compte C : Comptes.values())if(C.get_pseudo().equals(p))return C;
		return null;
	}

	public static void addItemSet(ItemSet itemSet)
	{
		ItemSets.put(itemSet.getId(), itemSet);
	}

	public static ItemSet getItemSet(int tID)
	{
		return ItemSets.get(tID);
	}

	public static int getItemSetNumber()
	{
		return ItemSets.size();
	}

	public static int getNextIdForMount()
	{
		int max = 1;
		for(int a : Dragodindes.keySet())if(a > max)max = a;
		return max+1;
	}

	public static Carte getCarteByPosAndCont(int mapX, int mapY, int contID)
	{
		for(Carte map : Cartes.values())
		{
			if( map.getX() == mapX
			&&	map.getY() == mapY
			&&	map.getSubArea().get_area().get_superArea().get_id() == contID)
				return map;
		}
		return null;
	}
	public static void addGuild(Guild g,boolean save)
	{
		Guildes.put(g.get_id(), g);
		if(save)SQLManager.SAVE_NEWGUILD(g);
	}
	public static int getNextHighestGuildID()
	{
		if(Guildes.size() == 0)return 1;
		int n = 0;
		for(int x : Guildes.keySet())if(n<x)n = x;
		return n+1;
	}

	public static boolean guildNameIsUsed(String name)
	{
		for(Guild g : Guildes.values())if(g.get_name().equalsIgnoreCase(name))return true;
		return false;
	}
	public static boolean guildEmblemIsUsed(String emb)
	{
		for(Guild g : Guildes.values())
		{
			if(g.get_emblem().equals(emb))return true;
		}
		return false;
	}
	public static Guild getGuild(int i)
	{
		return Guildes.get(i);
	}
	public static long getGuildXpMax(int _lvl)
	{
		if(_lvl >= 100) 	_lvl = 100-1;
		if(_lvl <= 1)	 	_lvl = 1;
		return ExpLevels.get(_lvl+1).guilde;
	}
	public static void ReassignAccountToChar(Compte C)
	{
		C.get_persos().clear();
		SQLManager.LOAD_PERSO_BY_ACCOUNT(C.get_GUID());
		for(Personnage P : Persos.values())
		{
			if(P.getAccID() == C.get_GUID())
			{
				C.addPerso(P);
				P.setAccount(C);
			}
		}
	}
	public static int getZaapCellIdByMapId(short i)
	{
		for(Entry<Integer, Integer> zaap : Constants.ZAAPS.entrySet())
		{
			if(zaap.getKey() == i)return zaap.getValue();
		}
		return -1;
	}
	public static int getEncloCellIdByMapId(short i)
	{
		if(World.getCarte(i).getMountPark() != null)
		{
			if(World.getCarte(i).getMountPark().get_cellid() > 0)
			{
				return World.getCarte(i).getMountPark().get_cellid();
			}
		}
		
		return -1;
	}

	public static void delDragoByID(int getId)
	{
		Dragodindes.remove(getId);
	}

	public static void removeGuild(int id)
	{
		SQLManager.HOUSE_GUILD_REMOVE(id);
		Guildes.remove(id);
		SQLManager.DEL_GUILD(id);
	}

	public static boolean ipIsUsed(String ip)
	{
		for(Compte c : Comptes.values())if(c.get_curIP() == ip)return true;
		return false;
	}

	public static void unloadPerso(int g)
	{
		Persos.remove(g);
	}
	public static boolean isArenaMap(int mapID)
	{
		for(int curID : Ancestra.arenaMap)
		{
			if(curID == mapID)
				return true;
		}
		return false;
	}
	public static Objet newObjet(int Guid, int template,int qua, int pos, String strStats)
	{
		if(World.getObjTemplate(template).getType() == 85)
			return new PierreAme(Guid, qua, template, pos, strStats);
		else
			return new Objet(Guid, template, qua, pos, strStats);
	}
	
	
	public static short get_state()
	{
		return _state;
	}
	
	public static void set_state(short state)
	{
		_state = state;
	}

}
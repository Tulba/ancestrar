package objects;

import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import common.Ancestra;
import common.Constants;
import common.SQLManager;
import common.SocketManager;
import common.World;

public class House
{
	private static Map<Integer,House> _house = new TreeMap<Integer,House>();
	private int _id;
	private short _map_id;
	private int _cell_id;
	private int _owner_id;
	private int _sale;
	private int _guild_id;
	private int _guildRights;
	private int _access;
	private String _key;
	private int _mapid;
	private int _caseid;
	private static int _selectedHouse;
	
	//Droits de chaques maisons double tableau a améliorer ?
	private static Map<Integer, Map<Integer,Boolean>> houseRight = new TreeMap<Integer, Map<Integer,Boolean>>();
	private static Map<Integer,Boolean> haveRight = new TreeMap<Integer,Boolean>();

	
	public House(int id, short map_id, int cell_id, int owner_id, int sale,
			int guild_id, int access, String key, int guildrights, int mapid, int caseid) 
	{
		_id = id;
		_map_id = map_id;
		_cell_id = cell_id;
		_owner_id = owner_id;
		_sale = sale;
		_guild_id = guild_id;
		_access = access;
		_key = key;
		_guildRights = guildrights;
		parseIntToRight(id, guildrights);
		_mapid = mapid;
		_caseid = caseid;
	}

	public static void addHouse(House house)
	{
		_house.put(house._id, house);
	}
	
	public static House get_selectedHouse()
	{
		return _house.get(_selectedHouse);
	}
	
	public static House get_HouseID(int id)
	{
		return _house.get(id);
	}
	
	public int get_id()
	{
		return _id;
	}
	
	public short get_map_id()
	{
		return _map_id;
	}
	
	public int get_cell_id()
	{
		return _cell_id;
	}
	
	public int get_owner_id()
	{
		return _owner_id;
	}
	
	public void set_owner_id(int id)
	{
		_owner_id = id;
	}
	
	public int get_sale()
	{
		return _sale;
	}
	
	public void set_sale(int price)
	{
		_sale = price;
	}
	
	public int get_guild_id()
	{
		return _guild_id;
	}
	
	public void set_guild_id(int GuildID)
	{
		_guild_id = GuildID;
	}
	
	public int get_guild_rights()
	{
		return _guildRights;
	}
	
	public void set_guild_rights(int GuildRights)
	{
		_guildRights = GuildRights;
	}
	
	public int get_access()
	{
		return _access;
	}
	
	public void set_access(int access)
	{
		_access = access;
	}
	
	public String get_key()
	{
		return _key;
	}
	
	public void set_key(String key)
	{
		_key = key;
	}
	
	public int get_mapid()
	{
		return _mapid;
	}
	
	public int get_caseid()
	{
		return _caseid;
	}
	
	public static House get_house_id_by_coord(int map_id, int cell_id)
	{
		for(Entry<Integer, House> house : _house.entrySet())
		{
			if(house.getValue().get_map_id() == map_id && house.getValue().get_cell_id() == cell_id)
			{
				_selectedHouse = house.getValue().get_id();
				return house.getValue();
			}
		}
		return null;
	}
	
	public static void LoadHouse(Personnage P, int newMapID)//Affichage des maison + Blason
	{
		
		for(Entry<Integer, House> house : _house.entrySet())
		{
			if(house.getValue().get_map_id() == newMapID)
			{
				String packet = "P"+house.getValue().get_id()+"|";
				if(house.getValue().get_owner_id() > 0)
				{
					packet += World.getCompte(house.getValue().get_owner_id()).get_name()+";";
				}else
				{
					packet+=";";
				}
				if(house.getValue().get_sale() > 0)//Si prix > 0
				{
					packet+="1";//Achetable
				}else
				{
					packet+="0";//Non achetable
				}
				if(house.getValue().get_guild_id() > 0) //Maison de guilde
				{
					Guild G = World.getGuild(house.getValue().get_guild_id());
					String Gname = G.get_name();
					String Gemblem = G.get_emblem();
					if(G.getMembers().size() < 10)//Ce n'est plus une maison de guilde
					{
						SQLManager.HOUSE_GUILD(house.getValue(), 0, 0) ;
					}
					
					//Affiche le blason pour les membre de guilde OU Affiche le blason pour les non membre de guilde
					if(P.get_guild() != null && P.get_guild().get_id() == house.getValue().get_guild_id() && house.getValue().canDo(house.getValue().get_id(), Constants.H_GBLASON) && G.getMembers().size() > 9)//meme guilde
					{
						packet+=";"+Gname+";"+Gemblem;
					}
					else if(house.getValue().canDo(house.getValue().get_id(), Constants.H_OBLASON) && G.getMembers().size() > 9)//Pas de guilde/guilde-différente
					{
						packet+=";"+Gname+";"+Gemblem;
					}
				}
				SocketManager.GAME_SEND_hOUSE(P, packet);

				if(house.getValue().get_owner_id() == P.getAccID())
				{
					String packet1 = "L+|"+house.getValue().get_id()+";"+house.getValue().get_access()+";";
					
					if(house.getValue().get_sale() <= 0)
					{
						packet1 +="0;"+house.getValue().get_sale();
					}
					else if(house.getValue().get_sale() > 0)
					{
						packet1 +="1;"+house.getValue().get_sale();
					}
					SocketManager.GAME_SEND_hOUSE(P, packet1);
				}
			}
		}
	}

	public void HopIn(Personnage P)//Entrer dans la maison
	{
		// En gros si il fait quelque chose :)
		if(P.get_fight() != null ||
		   P.get_isTalkingWith() != 0 ||
		   P.get_isTradingWith() != 0 ||
		   P.getCurJobAction() != null ||
		   P.get_curExchange() != null)
		{
			return;
		}
		
		House h = get_selectedHouse();
		if(h == null) return;
		if(h.get_owner_id() == P.getAccID() || (P.get_guild() != null && P.get_guild().get_id() == h.get_guild_id() && canDo(h.get_id(), Constants.H_GNOCODE)))//C'est sa maison ou même guilde + droits entrer sans pass
		{
			OpenHouse(P, "-", true);
		}
		else if(h.get_owner_id() > 0) //Une personne autre la acheter, il faut le code pour rentrer
		{
			SocketManager.GAME_SEND_KODE(P, "CK0|8");//8 étant le nombre de chiffre du code	
		}
		else if(h.get_owner_id() == 0) //Maison non acheter, mais achetable, on peut rentrer sans code
		{
			OpenHouse(P, "-", false);
		}else
		{
			return;
		}
	}

	public static void OpenHouse(Personnage P, String packet, boolean isHome)//Ouvrir une maison ;o
	{
		
		House h = get_selectedHouse();
		
		SQLManager.SAVE_PERSONNAGE(P, true);
		if((!h.canDo(h.get_id(), Constants.H_OCANTOPEN) && (packet.compareTo(h.get_key()) == 0)) || isHome)//Si c'est chez lui ou que le mot de passe est bon
		{
			P.teleport((short)h.get_mapid(), h.get_caseid());
			System.out.println(">>>>>>>>>> ENTRER");
			closeCode(P);
		}else if((packet.compareTo(h.get_key()) != 0) || h.canDo(h.get_id(), Constants.H_OCANTOPEN))//Mauvais code
		{
			SocketManager.GAME_SEND_KODE(P, "KE");
			SocketManager.GAME_SEND_KODE(P, "V");
		}
	}
	
	public void BuyIt(Personnage P)//Acheter une maison
	{
		House h = get_selectedHouse();
		String str = "CK"+h.get_id()+"|"+h.get_sale();//ID + Prix
		SocketManager.GAME_SEND_hOUSE(P, str);
	}

	public static void HouseAchat(Personnage P)//Acheter une maison
	{
		House h = get_selectedHouse();

		if(AlreadyHaveHouse(P))
		{
			SocketManager.GAME_SEND_MESSAGE(P, "Vous ne pouvez pas acheter plus d'une maison.", Ancestra.CONFIG_MOTD_COLOR);
			return;
		}
		//On enleve les kamas
		if(P.get_kamas() < h.get_sale()) return;
		long newkamas = P.get_kamas()-h.get_sale();
		P.set_kamas(newkamas);

		//Ajoute des kamas dans la banque du vendeur
		if(h.get_owner_id() > 0)
		{
			Compte Seller = World.getCompte(h.get_owner_id());
			long newbankkamas = Seller.getBankKamas()+h.get_sale();
			Seller.setBankKamas(newbankkamas);
			//Petit message pour le prévenir si il est on?
			if(Seller.get_curPerso() != null)
			{
				SocketManager.GAME_SEND_MESSAGE(Seller.get_curPerso(), "Une maison vous appartenant a ete vendu "+h.get_sale()+" kamas.", Ancestra.CONFIG_MOTD_COLOR);
				SQLManager.SAVE_PERSONNAGE(Seller.get_curPerso(), true);
			}
		}
		
		//On save l'acheteur
		SQLManager.SAVE_PERSONNAGE(P, true);
		SocketManager.GAME_SEND_STATS_PACKET(P);
		closeBuy(P);

		//Achat de la maison
		SQLManager.HOUSE_BUY(P, h);

		//Rafraichir la map aprés l'achat
		for(Personnage z:P.get_curCarte().getPersos())
		{
			LoadHouse(z, z.get_curCarte().get_id());
		}
		
		
		
	}
	
	public void SellIt(Personnage P)//Vendre une maison
	{
		House h = get_selectedHouse();
		if(isHouse(P, h))
		{
			String str = "CK"+h.get_id()+"|"+h.get_sale();//ID + Prix
			SocketManager.GAME_SEND_hOUSE(P, str);
				return;
		}else
		{
			return;
		}
	}
	
	public static void SellPrice(Personnage P, String packet)//Vendre une maison
	{
		House h = get_selectedHouse();
		int price = Integer.parseInt(packet);	
		if(h.isHouse(P, h))
		{
			SQLManager.SAVE_PERSONNAGE(P, true);
			SocketManager.GAME_SEND_hOUSE(P, "V");
			SocketManager.GAME_SEND_hOUSE(P, "SK"+h.get_id()+"|"+price);
				
			//Vente de la maison
			SQLManager.HOUSE_SELL(h, price);

			//Rafraichir la map aprés la mise en vente
			for(Personnage z:P.get_curCarte().getPersos())
			{
				LoadHouse(z, z.get_curCarte().get_id());
			}
				
			return;
		}else
		{
			return;
		}
	}

	public boolean isHouse(Personnage P, House h)//Savoir si c'est sa maison
	{
		if(h.get_owner_id() == P.getAccID()) return true;
		else return false;
	}
	
	public static void closeCode(Personnage P)
	{
		SocketManager.GAME_SEND_KODE(P, "V");
	}
	
	public static void closeBuy(Personnage P)
	{
		SocketManager.GAME_SEND_hOUSE(P, "V");
	}
	
	public void Lock(Personnage P) 
	{
		SocketManager.GAME_SEND_KODE(P, "CK1|8");
	}
	
	public static void LockHouse(Personnage P, String packet) 
	{
		House h = get_selectedHouse();
		if(h.isHouse(P, h))
		{
			SQLManager.HOUSE_CODE(P, h, packet);//Change le code
			SQLManager.SAVE_PERSONNAGE(P, true);
			closeCode(P);
			return;
		}else
		{
			SQLManager.SAVE_PERSONNAGE(P, true);
			closeCode(P);
			return;
		}
	}
	
	public static String parseHouseToGuild(Personnage P)
	{
		//TODO : Compétences ...
		boolean isFirst = true;
		String packet = "+";
		for(Entry<Integer, House> house : _house.entrySet())
		{
			if(house.getValue().get_guild_id() == P.get_guild().get_id() && house.getValue().get_guild_rights() > 0)
			{
				if(isFirst)
				{
					packet += house.getValue().get_id()+";"+World.getPersonnage(house.getValue().get_owner_id()).get_compte().get_pseudo()+";"+World.getCarte((short)house.getValue().get_mapid()).getX()+","+World.getCarte((short)house.getValue().get_mapid()).getY()+";0;"+house.getValue().get_guild_rights();	
					isFirst = false;
				}else
				{
					packet += "|"+house.getValue().get_id()+";"+World.getPersonnage(house.getValue().get_owner_id()).get_compte().get_pseudo()+";"+World.getCarte((short)house.getValue().get_mapid()).getX()+","+World.getCarte((short)house.getValue().get_mapid()).getY()+";0;"+house.getValue().get_guild_rights();	
				}
			}
		}
			return packet;
	}
	
	public static boolean AlreadyHaveHouse(Personnage P)
	{
		for(Entry<Integer, House> house : _house.entrySet())
		{
			if(house.getValue().get_owner_id() == P.getAccID())
			{
				return true;
			}
		}
		return false;
	}
	
	public static void parseHG(Personnage P, String packet)
	{
		House h = get_selectedHouse();
		
		if(P.get_guild() == null) return;
		
		if(packet != null)
		{
			if(packet.charAt(0) == '+')
			{
				//Ajoute en guilde
				byte HouseMaxOnGuild = (byte) Math.floor(P.get_guild().get_lvl()/10);
				if(HouseOnGuild(P.get_guild().get_id()) >= HouseMaxOnGuild) return;
				if(P.get_guild().getMembers().size() < 10) return;
				SQLManager.HOUSE_GUILD(h, P.get_guild().get_id(), 0);
				parseHG(P, null);
			}
			else if(packet.charAt(0) == '-')
			{
				//Retire de la guilde
				SQLManager.HOUSE_GUILD(h, 0, 0);
				parseHG(P, null);
			}
			else
			{
				SQLManager.HOUSE_GUILD(h, h.get_guild_id(), Integer.parseInt(packet));
				parseIntToRight(h.get_id(), Integer.parseInt(packet));
			}
		}
		else if(packet == null)
		{
		if(h.get_guild_id() <= 0)
		{
			SocketManager.GAME_SEND_hOUSE(P, "G"+h.get_id());
		}else if(h.get_guild_id() > 0)
		{
			SocketManager.GAME_SEND_hOUSE(P, "G"+h.get_id()+";"+P.get_guild().get_name()+";"+P.get_guild().get_emblem()+";"+h.get_guild_rights());
		}
		}
	}
	
	static byte HouseOnGuild(int GuildID) 
	{
		byte i = 0;
		for(Entry<Integer, House> house : _house.entrySet())
		{
			if(house.getValue().get_guild_id() == GuildID)
			{
				i++;
			}
		}
		return i;
	}

	public boolean canDo(int houseid, int rightValue)
	{	
		return houseRight.get(houseid).get(rightValue);
	}
	
	public static void initRight(int houseid)
	{
		haveRight.put(Constants.H_GBLASON, false);
		haveRight.put(Constants.H_OBLASON,false);
		haveRight.put(Constants.H_GNOCODE,false);
		haveRight.put(Constants.H_OCANTOPEN,false);
		haveRight.put(Constants.C_GNOCODE,false);
		haveRight.put(Constants.C_OCANTOPEN,false);
		haveRight.put(Constants.H_GREPOS,false);
		haveRight.put(Constants.H_GTELE,false);
		houseRight.put(houseid, haveRight);
	}
	
	public static void parseIntToRight(int houseid, int total)
	{
		initRight(houseid);

		if(total == 1)
			return;

		if(haveRight.size() > 0)	//Si les droits contiennent quelque chose -> Vidage (Même si le TreeMap supprimerais les entrées doublon lors de l'ajout)
			haveRight.clear();

		initRight(houseid);	//Remplissage des droits

		Integer[] mapKey = haveRight.keySet().toArray(new Integer[haveRight.size()]);	//Récupère les clef de map dans un tableau d'Integer

		while(total > 0)
		{
			for (int i = haveRight.size()-1; i < haveRight.size(); i--)
			{
				if(mapKey[i].intValue() <= total)
				{
					total ^= mapKey[i].intValue();
					haveRight.put(mapKey[i],true);
					houseRight.put(houseid, haveRight);
					break;
				}
			}
		}
	}
	
	public static void Leave(Personnage P, String packet)
	{
		House h = get_selectedHouse();
		if(!h.isHouse(P, h)) return;
		int Pguid = Integer.parseInt(packet);
		Personnage Target = World.getPersonnage(Pguid);
		if(Target == null || !Target.isOnline() || Target.get_fight() != null || Target.get_curCarte().get_id() != P.get_curCarte().get_id()) return;
		Target.teleport(h.get_map_id(), h.get_cell_id());
		SocketManager.GAME_SEND_Im_PACKET(Target, "018;"+P.get_name());
	}
	
	
	public static House get_HouseByPerso(Personnage P)//Connaitre la MAPID + CELLID de ça maison
	{
		for(Entry<Integer, House> house : _house.entrySet())
		{
			if(house.getValue().get_owner_id() == P.getAccID())
			{
				return House.get_HouseID(house.getValue().get_id());
			}
		}
		return null;
	}
}
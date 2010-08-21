package objects;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.TreeMap;

import common.Ancestra;
import common.Constants;
import common.SQLManager;
import common.SocketManager;
import common.World;

public class House
{
	
	private static Map<Integer,Boolean> haveRight = new TreeMap<Integer,Boolean>();
	private static int price;
	private static int sellerid;
	public static short CcellID;//Détermine la cell_id de la maison => S'obtient dans le GA packet
	public static short CcarteID;//Détermine la map ou ce trouve le perso
	public static short isMapID;//La mapid de la maison
	public static short isCellID;//La cellid de la maison
	public static short isGuild;//La guilde de la maison

	public static void LoadHouse(Personnage P, int newMapID)//Affichage des maison + Blason
	{
		ResultSet RS;
		try {
		RS = SQLManager.executeQuery("SELECT * from `houses` WHERE `map_id`='"+newMapID+"';",Ancestra.OTHER_DB_NAME);
		while(RS.next())
		{
			parseIntToRight(RS.getInt("guild_rights"));

			String packet = "P"+RS.getInt("id")+"|";
			
			if(RS.getInt("owner_id") > 0)//Affiche "nom de compte du joueur" a qui appartient la maison
			{
				packet+=World.getCompte(RS.getInt("owner_id")).get_name()+";";
			}else//La maison n'appartient a personne
			{
					packet+=";";
			}
			
			if(RS.getInt("sale") > 0)//Si prix > 0
			{
				packet+="1";//Achetable
			}else
			{
				packet+="0";//Non achetable
			}
			
			if(RS.getInt("guild_id") > 0) //Maison de guilde
			{
				Guild G = World.getGuild(RS.getInt("guild_id"));
				String Gname = "";
				String Gemblem = "";
				ResultSet RS2;
				try {
				RS2 = SQLManager.executeQuery("SELECT * from `guilds` WHERE `id`='"+RS.getInt("guild_id")+"';",Ancestra.OTHER_DB_NAME);
						while(RS2.next())
						{
							Gname = RS2.getString("name");
							Gemblem = RS2.getString("emblem");
						}
					} catch (SQLException e) {
				e.printStackTrace();
				}
					if(G.getMembers().size() < 10)//Ce n'est p^lus une maison de guilde
					{
						CcellID = RS.getShort("cell_id");
						CcarteID = RS.getShort("map_id");
						SQLManager.HOUSE_GUILD(P, 0) ;
					}
				//Affiche le blason pour les membre de guilde OU Affiche le blason pour les non membre de guilde
				if(P.get_guild() != null && P.get_guild().get_id() == RS.getInt("guild_id") && canDo(Constants.H_GBLASON) && G.getMembers().size() > 9)//meme guilde
				{
					packet += ";"+Gname+";"+Gemblem;
				}
				else if(canDo(Constants.H_OBLASON) && G.getMembers().size() > 9)//Pas de guilde/guilde-différente
				{
						packet += ";"+Gname+";"+Gemblem;
				}
			}
			SocketManager.GAME_SEND_hOUSE(P, packet);

			if(RS.getInt("owner_id") == P.getAccID())
			{
				String packet1 = "L+|"+RS.getInt("id")+";"+RS.getInt("access")+";";
				
				if(RS.getInt("sale") <= 0)
				{
					packet1 +="0;"+RS.getInt("sale");
				}
				else if(RS.getInt("sale") > 0)
				{
					packet1 +="1;"+RS.getInt("sale");
				}
				SocketManager.GAME_SEND_hOUSE(P, packet1);
			}
		}
			} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public static void HopIn(Personnage P)//Entrer dans la maison
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
			ResultSet RS;
			try {
				RS = SQLManager.executeQuery("SELECT * from `houses` WHERE `map_id`='"+CcarteID+"' AND `cell_id`='"+CcellID+"';",Ancestra.OTHER_DB_NAME);
				while(RS.next())
				{
					parseIntToRight(RS.getInt("guild_rights"));
					if(RS.getInt("owner_id") == P.getAccID() || (P.get_guild() != null && P.get_guild().get_id() == RS.getInt("guild_id") && canDo(Constants.H_GNOCODE)))//C'est sa maison ou même guilde + droits entrer sans pass
					{
						OpenHouse(P, "-", true);
					}
					else if(RS.getInt("owner_id") > 0) //Une personne autre la acheter, il faut le code pour rentrer
					{
						SocketManager.GAME_SEND_KODE(P, "CK0|8");//8 étant le nombre de chiffre du code	
					}
					else if(RS.getInt("owner_id") == 0) //Maison non acheter, mais achetable, on peut rentrer sans code
					{
						OpenHouse(P, "-", false);
					}
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
					return;
		}

	public static void OpenHouse(Personnage P, String packet, boolean isHome)//Ouvrir une maison ;o
	{
		ResultSet RS;
		try {
			RS = SQLManager.executeQuery("SELECT * from `houses` WHERE `map_id`='"+CcarteID+"' AND `cell_id`='"+CcellID+"';",Ancestra.OTHER_DB_NAME);
			while(RS.next())
			{
				SQLManager.SAVE_PERSONNAGE(P, true);
				if((!canDo(Constants.H_OCANTOPEN) && (packet.compareTo(RS.getString("key")) == 0)) || isHome)//Si c'est chez lui ou que le mot de passe est bon
				{
					P.teleport(RS.getShort("mapid"), RS.getInt("caseid"));
					System.out.println(">>>>>>>>>> ENTRER");
					closeCode(P);
				}else if(RS.getString("key") != packet || canDo(Constants.H_OCANTOPEN))//Mauvais code
				{
					SocketManager.GAME_SEND_KODE(P, "KE");
					SocketManager.GAME_SEND_KODE(P, "V");
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public static void BuyIt(Personnage P)//Acheter une maison
	{
		ResultSet RS;
		try {
			RS = SQLManager.executeQuery("SELECT * from `houses` WHERE `map_id`='"+CcarteID+"' AND `cell_id`='"+CcellID+"';",Ancestra.OTHER_DB_NAME);
			while(RS.next())
			{
				//Ne devrait retourner qu'une maison
				String str = "CK"+RS.getInt("id")+"|"+RS.getInt("sale");//ID + Prix
				SocketManager.GAME_SEND_hOUSE(P, str);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public static void HouseAchat(Personnage P)//Acheter une maison
	{
		ResultSet RS;
		try {
			RS = SQLManager.executeQuery("SELECT * from `houses` WHERE `map_id`='"+CcarteID+"' AND `cell_id`='"+CcellID+"';",Ancestra.OTHER_DB_NAME);
			while(RS.next())
			{
				price = RS.getInt("sale");
				sellerid = RS.getInt("owner_id");
			}
		} catch (SQLException e) {
			System.out.println("SQL ERROR: "+e.getMessage());
			e.printStackTrace();
		}
		
		if(AlreadyHaveHouse(P))
		{
			SocketManager.GAME_SEND_MESSAGE(P, "Vous ne pouvez pas acheter plus d'une maison.", Ancestra.CONFIG_MOTD_COLOR);
			return;
		}
		//On enleve les kamas
		if(P.get_kamas() < price) return;
		long newkamas = P.get_kamas()-price;
		P.set_kamas(newkamas);

		//Ajoute des kamas dans la banque du vendeur
		if(sellerid > 0)
		{
			Compte Seller = World.getCompte(sellerid);
			long newbankkamas = Seller.getBankKamas()+price;
			Seller.setBankKamas(newbankkamas);
			//Petit message pour le prévenir si il est on?
			if(Seller.get_curPerso() != null)
			{
				SocketManager.GAME_SEND_MESSAGE(Seller.get_curPerso(), "Une maison vous appartenant a ete vendu "+price+" kamas.", Ancestra.CONFIG_MOTD_COLOR);
				SQLManager.SAVE_PERSONNAGE(Seller.get_curPerso(), true);
			}
		}
		
		//On save l'acheteur
		SQLManager.SAVE_PERSONNAGE(P, true);
		SocketManager.GAME_SEND_STATS_PACKET(P);
		closeBuy(P);

		//Achat de la maison
		SQLManager.HOUSE_BUY(P);

		//Rafraichir la map aprés l'achat
		for(Personnage z:P.get_curCarte().getPersos())
		{
			LoadHouse(z, z.get_curCarte().get_id());
		}
		
		
		
	}
	
	public static void SellIt(Personnage P)//Vendre une maison
	{
		ResultSet RS;
		try {
			RS = SQLManager.executeQuery("SELECT * from `houses` WHERE `map_id`='"+CcarteID+"' AND `cell_id`='"+CcellID+"';",Ancestra.OTHER_DB_NAME);
			while(RS.next())
			{
			if(isHouse(P))
			{
			//Ne devrait retourner qu'une maison
			String str = "CK"+RS.getInt("id")+"|"+RS.getInt("sale");//ID + Prix
			SocketManager.GAME_SEND_hOUSE(P, str);
				return;
			}else
			{
				return;
			}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public static void SellPrice(Personnage P, String packet)//Vendre une maison
	{
		int price = Integer.parseInt(packet);
		ResultSet RS;
		try {
			RS = SQLManager.executeQuery("SELECT * from `houses` WHERE `map_id`='"+CcarteID+"' AND `cell_id`='"+CcellID+"';",Ancestra.OTHER_DB_NAME);
			while(RS.next())
			{
			if(isHouse(P))
			{
				//Ne devrait retourner qu'une maison
				SQLManager.SAVE_PERSONNAGE(P, true);
				SocketManager.GAME_SEND_hOUSE(P, "V");
				SocketManager.GAME_SEND_hOUSE(P, "SK"+RS.getInt("id")+"|"+price);
				
				//Vente de la maison
				SQLManager.HOUSE_SELL(P, price);

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
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public static boolean isHouse(Personnage P)//Savoir si c'est sa maison
	{
		ResultSet RS;
		try {
			RS = SQLManager.executeQuery("SELECT * from `houses` WHERE `map_id`='"+CcarteID+"' AND `cell_id`='"+CcellID+"';",Ancestra.OTHER_DB_NAME);
			while(RS.next())
			{
			if(RS.getInt("owner_id") == P.getAccID()) return true;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public static void closeCode(Personnage P)
	{
		SocketManager.GAME_SEND_KODE(P, "V");
	}
	
	public static void closeBuy(Personnage P)
	{
		SocketManager.GAME_SEND_hOUSE(P, "V");
	}
	
	public static void Lock(Personnage P) 
	{
		SocketManager.GAME_SEND_KODE(P, "CK1|8");
	}
	
	public static void LockHouse(Personnage P, String packet) 
	{
			if(isHouse(P))
			{
				SQLManager.HOUSE_CODE(P, packet);//Change le code
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
		String packet = "+";
		ResultSet RS;
		try {
			RS = SQLManager.executeQuery("SELECT * from `houses` WHERE `guild_id`='"+P.get_guild().get_id()+"' AND `guild_rights`>0;",Ancestra.OTHER_DB_NAME);
			while(RS.next())
			{
				if(RS.isFirst())
				{
					packet+= RS.getInt("id")+";"+World.getPersonnage(RS.getInt("owner_id")).get_name()+";"+World.getCarte(RS.getShort("mapid")).getX()+","+World.getCarte(RS.getShort("mapid")).getY()+";0;"+RS.getInt("guild_rights");	
				}else
				{
					packet+= "|"+RS.getInt("id")+";"+World.getPersonnage(RS.getInt("owner_id")).get_name()+";"+World.getCarte(RS.getShort("mapid")).getX()+","+World.getCarte(RS.getShort("mapid")).getY()+";0;"+RS.getInt("guild_rights");
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
			return packet;
	}
	
	public static boolean AlreadyHaveHouse(Personnage P)
	{
		ResultSet RS;
		byte i = 0;
		try {
			RS = SQLManager.executeQuery("SELECT * from `houses` WHERE `owner_id`='"+P.getAccID()+"';",Ancestra.OTHER_DB_NAME);
			while(RS.next())
			{
				i++;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		if(i > 0) return true; else return false;
	}
	
	public static void parseHG(Personnage P, String packet)
	{
		short HouseID = 0;
		int GuildID = 0;
		int GuildRights = 0;
		
		if(P.get_guild() == null) return;
		
		ResultSet RS;
		try {
			RS = SQLManager.executeQuery("SELECT * from `houses` WHERE `map_id`='"+CcarteID+"' AND `cell_id`='"+CcellID+"';",Ancestra.OTHER_DB_NAME);
			while(RS.next())
			{
				HouseID = RS.getShort("id");
				GuildID = RS.getInt("guild_id");
				GuildRights = RS.getInt("guild_rights");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		if(packet != null)
		{
			if(packet.charAt(0) == '+')
			{
				//Ajoute en guilde
				byte HouseMaxOnGuild = (byte) Math.floor(P.get_guild().get_lvl()/10);
				if(HouseOnGuild(P.get_guild().get_id()) >= HouseMaxOnGuild) return;
				if(P.get_guild().getMembers().size() < 10) return;
				SQLManager.HOUSE_GUILD(P, P.get_guild().get_id());
				parseHG(P, null);
			}
			else if(packet.charAt(0) == '-')
			{
				//Retire de la guilde
				SQLManager.HOUSE_GUILD(P, 0);
				parseHG(P, null);
			}
			else
			{
				SQLManager.HOUSE_GUILD_RIGHTS(P, Integer.parseInt(packet));
			}
		}
		else if(packet == null)
		{
		if(GuildID <= 0)
		{
			SocketManager.GAME_SEND_hOUSE(P, "G"+HouseID);
		}else if(GuildID > 0)
		{
			SocketManager.GAME_SEND_hOUSE(P, "G"+HouseID+";"+P.get_guild().get_name()+";"+P.get_guild().get_emblem()+";"+GuildRights);
		}
		}
	}
	
	private static byte HouseOnGuild(int GuildID) 
	{
		byte i = 0;
		ResultSet RS;
		try {
			RS = SQLManager.executeQuery("SELECT * from `houses` WHERE `guild_id`='"+GuildID+"';",Ancestra.OTHER_DB_NAME);
			while(RS.next())
			{
				i++;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return i;
	}

	public static boolean canDo(int rightValue)
	{		
		return haveRight.get(rightValue);
	}
	
	public static void initRight()
	{
		haveRight.put(Constants.H_GBLASON,false);
		haveRight.put(Constants.H_OBLASON,false);
		haveRight.put(Constants.H_GNOCODE,false);
		haveRight.put(Constants.H_OCANTOPEN,false);
		haveRight.put(Constants.C_GNOCODE,false);
		haveRight.put(Constants.C_OCANTOPEN,false);
		haveRight.put(Constants.H_GREPOS,false);
		haveRight.put(Constants.H_GTELE,false);
	}
	
	public static void parseIntToRight(int total)
	{
		if(haveRight.size() == 0)
		{
			initRight();
		}
		if(total == 1)
			return;
		
		if(haveRight.size() > 0)	//Si les droits contiennent quelque chose -> Vidage (Même si le TreeMap supprimerais les entrées doublon lors de l'ajout)
			haveRight.clear();
			
		initRight();	//Remplissage des droits
		
		Integer[] mapKey = haveRight.keySet().toArray(new Integer[haveRight.size()]);	//Récupère les clef de map dans un tableau d'Integer
		
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
	
	public static void Leave(Personnage P, String packet)
	{
		if(!isHouse(P)) return;
		int Pguid = Integer.parseInt(packet);
		Personnage Target = World.getPersonnage(Pguid);
		if(Target == null || Target.get_fight() != null || Target.get_curCarte().get_id() != P.get_curCarte().get_id()) return;
		Target.teleport(CcarteID, CcellID);
	}
	
	public static void HouseCoordByPerso(Personnage P)//Connaitre la MAPID + CELLID de ça maison
	{
		ResultSet RS;
		try {
			RS = SQLManager.executeQuery("SELECT * from `houses` WHERE `owner_id`='"+P.getAccID()+"';",Ancestra.OTHER_DB_NAME);
			while(RS.next())
			{
				isMapID = RS.getShort("mapid");
				isCellID = RS.getShort("caseid");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public static void HouseCoordByID(int HouseID)//Connaitre la MAPID + CELLID de ça maison
	{
		ResultSet RS;
		try {
			RS = SQLManager.executeQuery("SELECT * from `houses` WHERE `id`='"+HouseID+"';",Ancestra.OTHER_DB_NAME);
			while(RS.next())
			{
				isMapID = RS.getShort("mapid");
				isCellID = RS.getShort("caseid");
				parseIntToRight(RS.getInt("guild_rights"));
				isGuild = RS.getShort("guild_id");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
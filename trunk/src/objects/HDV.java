package objects;

import java.sql.ResultSet;
import java.sql.SQLException;

import common.Ancestra;
import common.Constants;
import common.SQLManager;
import common.SocketManager;
import common.World;


public class HDV
{
	/*
	 * Travail désastreux oeuvrer par le grand DeathDown :)
	 * Y a de nombreuses chose à optimiser, je vous laisse le faire, je n'ai pas le talent d'un grand Dev :(
	 * TODO : Le temps de vente.
	 */
	public static boolean _isHdv;
	public static short _HdvType;
	public static String _HdvXvar;
	
	public static void StartSellHdv(Personnage P, int HdvId)
	{
		P.set_isTradingWith(-1);
		SQLManager.SAVE_PERSONNAGE(P, true);
		SocketManager.GAME_SEND_SELL(P, HdvCat(_HdvType), _HdvXvar);
		LoadSell(P, HdvId);
	}
	public static void LoadSell(Personnage P, int HdvId)
	{
	String packet = "";
	ResultSet RS;
	try {
	RS = SQLManager.executeQuery("SELECT * from `hdv_ventes` WHERE `hdv_type`='"+HdvId+"' AND `seller`='"+P.get_GUID()+"';",Ancestra.OTHER_DB_NAME);
	if(RS.next())
	{
	int guid = RS.getInt("guid");// GUID de l'objet
	int quantity = RS.getInt("quantity");// 1 ou 10 ou 100
	int templateid = RS.getInt("templateid");//Cf : items (BDD)
	String stats = RS.getString("stats");//Cf : items (BDD)
	int KamasSell = RS.getInt("KamasSell");//Prix de vente
	int TimeSell = RS.getInt("TimeSell");//En heure
	packet += guid+";"+quantity+";"+templateid+";"+stats+";"+KamasSell+";"+TimeSell;
	}
	while(RS.next())
	{
	int guid = RS.getInt("guid");// GUID de l'objet
	int quantity = RS.getInt("quantity");// 1 ou 10 ou 100
	int templateid = RS.getInt("templateid");//Cf : items (BDD)
	String stats = RS.getString("stats");//Cf : items (BDD)
	int KamasSell = RS.getInt("KamasSell");//Prix de vente
	int TimeSell = RS.getInt("TimeSell");//En heure
	packet += "|"+guid+";"+quantity+";"+templateid+";"+stats+";"+KamasSell+";"+TimeSell;
	}
	} catch (SQLException e) {
		System.out.println("ERREUR SQL :"+e.getMessage());
		e.printStackTrace();
	}
	SocketManager.GAME_SEND_LISTING_SELL(P, packet);
	}
	public static void MakeSell(Personnage P, int HdvId, int qua, int price, int guid)
	{
		// Limite des HDVs
		int counter = 0;
		ResultSet RS;
		try {
		RS = SQLManager.executeQuery("SELECT COUNT(*) from `hdv_ventes` WHERE `hdv_type`='"+HdvId+"' AND `seller`='"+P.get_GUID()+"';",Ancestra.OTHER_DB_NAME);
		RS.next();
		counter = RS.getInt(1);
		} catch (SQLException e) {
			System.out.println("ERREUR SQL :"+e.getMessage());
			e.printStackTrace();
		}
		HdvVar(P, HdvId);
		String Var = _HdvXvar;
		String[] infos = Var.split("\\;");
		if(counter > ((Integer.parseInt(infos[2]))-1))
		{
			SocketManager.GAME_SEND_MESSAGE(P, "Limite de vente atteinte.", Ancestra.CONFIG_MOTD_COLOR);
			//Je ne possède pas le packet associé.
			return;
		}
		//Taxe
		int Time = Integer.parseInt(infos[4]);
		int taxe_infos = Integer.parseInt(Var.split("\\.")[0]);
		float Taxe = (taxe_infos*price)/100;
		int Taxe_int = (int) Math.ceil(Taxe);
		if(P.get_kamas() < Taxe_int)
		{
			SocketManager.GAME_SEND_MESSAGE(P, "Vous ne disposez pas d'assez de kamas.", Ancestra.CONFIG_MOTD_COLOR);
			//Je ne possède pas le packet associé.
			return;
		}
		if(qua < 1 || qua > 3)
		{
			return;
		}
		if(qua == 1)
		{
			qua=1;
		}
		if(qua == 2)
		{
			qua=10;
		}
		if(qua == 3)
		{
			qua=100;
		}
		Objet objvendu = World.getObjet(guid);
		//Item innexistant, quantité insuffisante
		if(objvendu == null || objvendu.getQuantity() < qua) return;
		String packetEmK = "+"+objvendu.getGuid()+"|"+qua+"|"+World.getObjet(guid).getTemplate().getID()+"|"+objvendu.getStats().parseToItemSetStats()+"|"+price+"|72";
		SocketManager.GAME_SEND_EmK(P, packetEmK);
		
		if(qua == objvendu.getQuantity())
		{
			P.removeItem(objvendu.getGuid());
			SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(P, objvendu.getGuid());
			SQLManager.SAVE_ITEM(objvendu);//On sauvegarde l'item
			SQLManager.MakeSell(P, objvendu, qua, price, Time, HdvId);
		}else
		{
			int newQua = objvendu.getQuantity() - qua;
			objvendu.setQuantity(newQua);
			SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(P, objvendu);
			Objet objclone = Objet.getCloneObjet(objvendu, qua);
			objclone.setPosition(Constants.ITEM_POS_NO_EQUIPED);
			SQLManager.SAVE_NEW_ITEM(objclone);//On sauvegarde l'item
			SQLManager.SAVE_ITEM(objvendu);//On sauvegarde l'item
			SQLManager.LOAD_ITEMS(objclone.getGuid()+"");//On charge juste le clone
			SQLManager.MakeSell(P, objclone, qua, price, Time, HdvId);
		}
		long Taxe_Kamas = P.get_kamas()-Taxe_int;
		P.set_kamas(Taxe_Kamas);//Retrait des kamas de la taxe
		SQLManager.SAVE_PERSONNAGE(P, true);//Sauvegarde du perso, pour éviter les duplications
		SocketManager.GAME_SEND_STATS_PACKET(P);
		SocketManager.GAME_SEND_Ow_PACKET(P);
		LoadSell(P, HdvId);//On recharge l'HDV
	}

	public static void UnMakeSell(Personnage P, int HdvId, int guid)
	{
		int Sellerid = 0;
		String packetEmK = "-"+guid;
		Objet obj = World.getObjet(guid);
		if(obj == null) return;
		ResultSet RS;
		try {
		RS = SQLManager.executeQuery("SELECT * from `hdv_ventes` WHERE `hdv_type`='"+HdvId+"' AND `guid`='"+obj.getGuid()+"';",Ancestra.OTHER_DB_NAME);
		while(RS.next())
		{
			Sellerid = RS.getInt("seller");
		}
		}catch (SQLException e) {
			System.out.println("ERREUR SQL :"+e.getMessage());
			e.printStackTrace();
		}
		//Protection avec le sellerid
		if(Sellerid != P.get_GUID())
		{
			return;
		}
		Objet SimObj = getSimilarItem(obj, P);
		if(SimObj == null)
		{
			P.addObjet(obj, false);
		}else
		{
			//La mode dite plus "classique" ne fonctionnant pas ... On fait la méthode bourrin.
			ResultSet RS2;
			int SimObjQuantity = SimObj.getQuantity();
			try {
			RS2 = SQLManager.executeQuery("SELECT * from `items` WHERE `guid`='"+SimObj.getGuid()+"';",Ancestra.OTHER_DB_NAME);
			while(RS2.next())
			{
				SimObjQuantity = RS2.getInt("qua");
			}
			}catch (SQLException e) {
				System.out.println("ERREUR SQL :"+e.getMessage());
				e.printStackTrace();
			}
			int newQua = SimObjQuantity+obj.getQuantity();
			SimObj.setQuantity(newQua);
			SQLManager.SAVE_ITEM(SimObj);
			SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(P, SimObj);
			World.removeItem(obj.getGuid());//On supprime l'item inutile
		}
		SocketManager.GAME_SEND_EmK(P, packetEmK);
		SocketManager.GAME_SEND_Ow_PACKET(P);
		SQLManager.DelSell(Sellerid, obj.getGuid(), HdvId, -1 ,-1);//On supprime la vente
		SQLManager.SAVE_PERSONNAGE(P, true);
		LoadSell(P, HdvId);//On recharge l'HDV
	}
	private static Objet getSimilarItem(Objet obj, Personnage P)
	{
		for(Objet value : P.getItems().values())
		{
			if(value.getTemplate().getID() == obj.getTemplate().getID() && value.getStats().isSameStats(obj.getStats()))
				return value;
		}
		return null;
	}
	public static void StartBuyHdv(Personnage P, int HdvId)
	{
		P.set_isTradingWith(-1);
		SQLManager.SAVE_PERSONNAGE(P, true);
		SocketManager.GAME_SEND_BUY(P, HdvCat(_HdvType), _HdvXvar);
	}
	public static void LoadBuy(Personnage P, int HdvId, int itemtype)
	{

		String packet = itemtype+"|";
		ResultSet RS;
		try {
		RS = SQLManager.executeQuery("SELECT DISTINCT(templateid) from `hdv_ventes` WHERE `hdv_type`='"+HdvId+"' AND `item_type`='"+itemtype+"';",Ancestra.OTHER_DB_NAME);
		while(RS.next())
		{
		int templateid = RS.getInt("templateid");
		if(RS.isFirst())
		{
			packet += templateid;
		}else
		{
		packet += ";"+templateid;
		}
		}
		} catch (SQLException e) {
			System.out.println("ERREUR SQL :"+e.getMessage());
			e.printStackTrace();
		}
		SocketManager.GAME_SEND_EHL(P, packet);
	}
	public static void LoadBuyItem(Personnage P, int templateitem, int HdvId)
	{
		//FIXME : Si meme STATS, meme ligne ;O ?
		String packet = "";
		ResultSet RS;
		try {
			RS = SQLManager.executeQuery("SELECT * from `hdv_ventes` WHERE `hdv_type`='"+HdvId+"' AND `templateid`='"+templateitem+"' ORDER BY `KamasSell` ASC;",Ancestra.OTHER_DB_NAME);
			while(RS.next())
			{
				if(RS.isFirst())
				{
					int templateid = RS.getInt("templateid");
					packet = templateid+"";
				}
				RS.getInt("templateid");
				int guid = RS.getInt("guid");
				int quantity = RS.getInt("quantity");
				int KamasSell = RS.getInt("KamasSell");
				Objet obj = World.getObjet(guid);
				if(obj == null) return;
				
				packet+= "|"+guid+";"+obj.parseStatsString()+";";
				if(quantity == 1)
				{
					packet+=KamasSell+";;";
				}
				if(quantity == 10)
				{
					packet+=";"+KamasSell+";";
				}
				if(quantity == 100)
				{
					packet+=";;"+KamasSell;
				}
		}
		} catch (SQLException e) {
			System.out.println("ERREUR SQL :"+e.getMessage());
			e.printStackTrace();
		}
		SocketManager.GAME_SEND_EHl(P, packet);
	}
	public static void FinalizeBuy(Personnage P, int guid, int qua, int price, int HdvId)
	{
		if(P.get_kamas() >= price)
		{
			ResultSet RS;
			int SellerId = 0;
			//Protection des quantités
			if(qua < 1 || qua > 3)
			{
				qua = 1;
			}
			if(qua == 1)
			{
				qua = 1;
			}
			if(qua == 2)
			{
				qua = 10;
			}
			if(qua == 3)
			{
				qua = 100;
			}
			try {
				RS = SQLManager.executeQuery("SELECT * from `hdv_ventes` WHERE `hdv_type`='"+HdvId+"' AND `guid`='"+guid+"' AND `KamasSell`='"+price+"' AND `quantity`='"+qua+"';",Ancestra.OTHER_DB_NAME);
				while(RS.next())
				{
					SellerId = RS.getInt("seller");
					price = RS.getInt("KamasSell");//Protection du prix d'achat
				}
				}catch (SQLException e) {
					System.out.println("ERREUR SQL :"+e.getMessage());
					e.printStackTrace();
				}
				SQLManager.DelSell(-1, guid, HdvId, price, qua);//On supprime la vente
				
			//On paye le vendeur
			Personnage Seller = World.getPersonnage(SellerId);
			if(Seller == null) return;
			long newBankkamas = Seller.getBankKamas()+price;
			World.getPersonnage(SellerId).setBankKamas(newBankkamas);
			//On escroque l'acheteur ;O
			long newkamas = P.get_kamas()-price;
			P.set_kamas(newkamas);

			Objet obj = World.getObjet(guid);
			if(obj == null || obj.getQuantity() < qua) return;
			
			Objet SimObj = getSimilarItem(obj, P);
			if(SimObj == null)
			{
				P.addObjet(obj, false);
			}else
			{
				//La mode dite plus "classique" ne fonctionnant pas ... On fait la méthode bourrin.
				ResultSet RS2;
				int SimObjQuantity = SimObj.getQuantity();
				try {
				RS2 = SQLManager.executeQuery("SELECT * from `items` WHERE `guid`='"+SimObj.getGuid()+"';",Ancestra.OTHER_DB_NAME);
				while(RS2.next())
				{
					SimObjQuantity = RS2.getInt("qua");
				}
				}catch (SQLException e) {
					System.out.println("ERREUR SQL :"+e.getMessage());
					e.printStackTrace();
				}
				int newQua = SimObjQuantity+obj.getQuantity();
				SimObj.setQuantity(newQua);
				SQLManager.SAVE_ITEM(SimObj);
				SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(P, SimObj);
				World.removeItem(obj.getGuid());//On supprime l'item inutile 
			}
			if(Seller.get_compte().get_curPerso() != null)
			{
				SocketManager.GAME_SEND_Im_PACKET(Seller.get_compte().get_curPerso(),
						"065;"+price+"~"+
						obj.getTemplate().getID()+"~"+
						obj.getTemplate().getID()+"~1");
			}
			LoadBuyItem(P, obj.getTemplate().getID(), HdvId);
			SocketManager.GAME_SEND_STATS_PACKET(P);
			SocketManager.GAME_SEND_Ow_PACKET(P);
			SocketManager.GAME_SEND_Im_PACKET(P, "068");
		}
		
	}
	public static void MiddlePrice(Personnage P, int templateid, int HdvId)
	{
		// Prix moyen des items en vente @ leCirconcit
		int[]tab; //declaration 
		tab=new int[10]; // allocation 
		tab[0] = 1; //initialisation 
		int i = 1; 
		int somme = 0;
		int moyenne = 0; 
		ResultSet RS; 
		try { 
			RS = SQLManager.executeQuery("SELECT * from `hdv_ventes` WHERE `hdv_type`='"+HdvId+"' AND `templateid`='"+templateid+"';",Ancestra.OTHER_DB_NAME); 
			while(RS.next()) 
			{ 
				if(RS.getInt("quantity") == 1)
				{
					tab[i-1] = RS.getInt("KamasSell"); 
				}
				else if(RS.getInt("quantity") == 10)
				{
					tab[i-1] = RS.getInt("KamasSell")/10; 
				}
				else if(RS.getInt("quantity") == 100)
				{
					tab[i-1] = RS.getInt("KamasSell")/100; 
				}			
				somme+= tab[i-1];
				i++; 

				
			} 
			if(i==1) 
			{
				moyenne = 0; 
			}
			else
			{
				moyenne = (int)Math.ceil(somme/(i-1)); 
			}
			}catch (SQLException e) 
			{ 
				System.out.println("ERREUR SQL :"+e.getMessage()); e.printStackTrace(); 
			} 
		SocketManager.GAME_SEND_EHP(P, templateid, moyenne);
	}
	//Variables propres aux différents HDV
	public static void HdvVar(Personnage P, int curMap)
	{
		if(P.get_curCarte().getSubArea().get_area().get_id() == 7)//BONTA
		{
			if(P.get_align() == 1)
			{
			_HdvXvar = "1.0;1000;20;-1;320";
			}else
			{
			_HdvXvar = "2.0;1000;20;-1;320";
			}
			
			if(P.get_curCarte().get_id() == 8757)//Rune
			{
			_HdvType = 822;
			}
			else if(P.get_curCarte().get_id() == 8759)//Animaux
			{
			_HdvType = 821;
			}
			else if(P.get_curCarte().get_id() == 4247)//Pecheur/Poisson
			{
			_HdvType = 811;
			}
			else if(P.get_curCarte().get_id() == 4216)//Bijoutier
			{
			_HdvType = 82;
			}
			else if(P.get_curCarte().get_id() == 4271)//Alchimiste
			{
			_HdvType = 81;
			}
			else if(P.get_curCarte().get_id() == 4262)//Ressources
			{
			_HdvType = 813;
			}
			else if(P.get_curCarte().get_id() == 4178)//Bucherons
			{
			_HdvType = 86;
			}
			else if(P.get_curCarte().get_id() == 4287)//Viandes
			{
			_HdvType = 83;
			}
			else if(P.get_curCarte().get_id() == 4299)//Paysans
			{
			_HdvType = 810;
			}
			else if(P.get_curCarte().get_id() == 4183)//Coordonier
			{
			_HdvType = 87;
			}
			else if(P.get_curCarte().get_id() == 4179)//Mineurs
			{
			_HdvType = 89;
			}
			else if(P.get_curCarte().get_id() == 4098)//Forgerons
			{
			_HdvType = 88;
			}
			else if(P.get_curCarte().get_id() == 2221)//Boulanger
			{
			_HdvType = 85;
			}
			else if(P.get_curCarte().get_id() == 8760)//Documents
			{
			_HdvType = 823;
			}
			else if(P.get_curCarte().get_id() == 4172)//Tailleur
			{
			_HdvType = 815;
			}
			else if(P.get_curCarte().get_id() == 4232)//Bricoleur
			{
			_HdvType = 816;
			}
			else if(P.get_curCarte().get_id() == 10129)//Pierre d'ame
			{
			_HdvType = 819;
			}
			else if(P.get_curCarte().get_id() == 6159)//Parcho
			{
			_HdvType = 820;
			}
			else if(P.get_curCarte().get_id() == 4174)//Sculpteur
			{
			_HdvType = 814;
			}
			
		}
		else if(P.get_curCarte().getSubArea().get_area().get_id() == 11)//BRAKMAR
		{
			if(P.get_align() == 2)
			{
			_HdvXvar = "1.0;1000;20;-1;320";
			}else
			{
			_HdvXvar = "2.0;1000;20;-1;320";
			}
			
			if(P.get_curCarte().get_id() == 8756)//Rune
			{
			_HdvType = 922;
			}
			else if(P.get_curCarte().get_id() == 8753)//Animaux
			{
			_HdvType = 921;
			}
			else if(P.get_curCarte().get_id() == 8754)//Documents
			{
			_HdvType = 923;
			}
			else if(P.get_curCarte().get_id() == 4595)//Viandes
			{
			_HdvType = 93;
			}
			else if(P.get_curCarte().get_id() == 4607)//Alchimiste
			{
			_HdvType = 91;
			}
			else if(P.get_curCarte().get_id() == 4622)//Bijoutier
			{
			_HdvType = 92;
			}
			else if(P.get_curCarte().get_id() == 4629)//Paysans
			{
			_HdvType = 910;
			}
			else if(P.get_curCarte().get_id() == 4646)//Ressources
			{
			_HdvType = 913;
			}
			else if(P.get_curCarte().get_id() == 4630)//Boulanger
			{
			_HdvType = 95;
			}
			else if(P.get_curCarte().get_id() == 4562)//Coordonier
			{
			_HdvType = 97;
			}
			else if(P.get_curCarte().get_id() == 4588)//Tailleur
			{
			_HdvType = 915;
			}
			else if(P.get_curCarte().get_id() == 4618)//Sculpteur
			{
			_HdvType = 914;
			}
			else if(P.get_curCarte().get_id() == 5112)//Bucherons
			{
			_HdvType = 96;
			}
			else if(P.get_curCarte().get_id() == 5317)//Forgerons
			{
			_HdvType = 98;
			}
			else if(P.get_curCarte().get_id() == 5311)//Mineurs
			{
			_HdvType = 99;
			}
			else if(P.get_curCarte().get_id() == 4615)//Pecheur/Poisson
			{
			_HdvType = 911;
			}
			else if(P.get_curCarte().get_id() == 4627)//Bricoleur
			{
			_HdvType = 916;
			}
			else if(P.get_curCarte().get_id() == 8482)//Pierre d'ame
			{
			_HdvType = 919;
			}
			else if(P.get_curCarte().get_id() == 6167)//Parcho lié
			{
			_HdvType = 920;
			}
		}
		else
		{
			_HdvXvar = "5.0;35;5;-1;72";
			if(P.get_curCarte().get_id() == 12262)//Bricoleur
			{
			_HdvType = 16;
			}
			else if(P.get_curCarte().get_id() == 7516)//Alchimiste
			{
			_HdvType = 1;
			}
			else if(P.get_curCarte().get_id() == 7413)//Ressources
			{
			_HdvType = 13;
			}
			else if(P.get_curCarte().get_id() == 7511)//Forgerons
			{
			_HdvType = 8;
			}
			else if(P.get_curCarte().get_id() == 7514)//Bijoutier
			{
			_HdvType = 2;
			}
			else if(P.get_curCarte().get_id() == 7443)//Mineur
			{
			_HdvType = 9;
			}
			else if(P.get_curCarte().get_id() == 7289)//Bucheron
			{
			_HdvType = 6;
			}
			else if(P.get_curCarte().get_id() == 7512)//Sculpteurs
			{
			_HdvType = 14;
			}			
			else if(P.get_curCarte().get_id() == 7397)//Paysan
			{
			_HdvType = 10;
			}
			else if(P.get_curCarte().get_id() == 7510)//Boulanger
			{
			_HdvType = 5;
			}
			else if(P.get_curCarte().get_id() == 7515)//Boucher
			{
			_HdvType = 3;
			}
			else if(P.get_curCarte().get_id() == 7350)//Chasseur
			{
			_HdvType = 4;
			}
			else if(P.get_curCarte().get_id() == 7501)//Poissonnier
			{
			_HdvType = 11;
			}
			else if(P.get_curCarte().get_id() == 7348)//Pecheur
			{
			_HdvType = 12;
			}
			else if(P.get_curCarte().get_id() == 7602)//Coordonier
			{
			_HdvType = 7;
			}
			else if(P.get_curCarte().get_id() == 7513)//Tailleur
			{
			_HdvType = 15;
			}
			else if(P.get_curCarte().get_id() == 3412)//Artifices
			{
			_HdvType = 18;
			}
			else if(P.get_curCarte().get_id() == 8039)//Bouclier
			{
			_HdvType = 17;
			}
			
		}
	}
	public static String HdvCat(int HdvType)
	{
		if(HdvType == 1 || HdvType == 81 || HdvType == 91)//Alchimistes
		{
			return "71,66,14,12,43,44,45,26,70;";
		}
		if(HdvType == 2 || HdvType == 82 || HdvType == 92)//Bijoutiers
		{
			return "1,9;";
		}
		if(HdvType == 3 || HdvType == 83 || HdvType == 93)//Bouchers
		{
			return "63,64,69,28;";
		}
		if(HdvType == 4 || HdvType == 84 || HdvType == 94)//Chasseurs
		{
			return "63,64,69,28;";
		}
		if(HdvType == 5 || HdvType == 85 || HdvType == 95)//Boulangers
		{
			return "33,42;";
		}
		if(HdvType == 6 || HdvType == 86 || HdvType == 96)//Bûcherons
		{
			return "38,96,95,98,108;";
		}
		if(HdvType == 7 || HdvType == 87 || HdvType == 97)//Cordonniers
		{
			return "10,11;";
		}
		if(HdvType == 8 || HdvType == 88 || HdvType == 98)//Forgerons
		{
			return "5,22,19,7,8,21,6;";
		}
		if(HdvType == 9 || HdvType == 89 || HdvType == 99)//Mineurs
		{
			return "40,39,51,88,50;";
		}
		if(HdvType == 10 || HdvType == 810 || HdvType == 910)//Paysans
		{
			return "34,52,60;";
		}
		if(HdvType == 11 || HdvType == 811 || HdvType == 911)//Poissonniers
		{
			return "41,49,62,28;";
		}
		if(HdvType == 12 || HdvType == 812 || HdvType == 912)//Pêcheurs
		{
			return "41,49,62,28;";
		}
		if(HdvType == 13 || HdvType == 813 || HdvType == 913)//Ressources
		{
			return "35,36,46,47,48,53,54,55,56,57,58,59,65,15,68,103,104,105,106,107,109,110,111;";
		}
		if(HdvType == 14 || HdvType == 814 || HdvType == 914)//Sculpteurs
		{
			return "2,3,4;";
		}
		if(HdvType == 15 || HdvType == 815 || HdvType == 915)//Tailleurs
		{
			return "16,17,81;";
		}
		if(HdvType == 16 || HdvType == 816 || HdvType == 916)//Bricoleurs
		{
			return "114,84,93,112;";
		}
		if(HdvType == 17 || HdvType == 817 || HdvType == 917)//Boucliers
		{
			return "82;";
		}
		if(HdvType == 18 || HdvType == 818 || HdvType == 918)//Fées d'Artifices
		{
			return "74;";
		}
		if(HdvType == 19 || HdvType == 819 || HdvType == 919)//Pierres d'Âmes
		{
			return "83,85;";
		}
		if(HdvType == 20 || HdvType == 820 || HdvType == 920)//Parchemins Liés
		{
			return "87;";
		}
		if(HdvType == 21 || HdvType == 821 || HdvType == 921)//Animaux
		{
			return "77,97,18,90,113,116;";
		}
		if(HdvType == 22 || HdvType == 822 || HdvType == 922)//Runes
		{
			return "78;";
		}
		if(HdvType == 23 || HdvType == 823 || HdvType == 923)//Documents
		{
			return "25,73,13,76,75;";
		}
		return null;
	}
}
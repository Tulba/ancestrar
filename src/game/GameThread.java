package game;

import game.GameServer.SaveThread;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;

import objects.*;
import objects.Carte.*;
import objects.Fight.Fighter;
import objects.Guild.GuildMember;
import objects.Metier.StatsMetier;
import objects.Monstre.MobGroup;
import objects.NPC_tmpl.*;
import objects.Objet.ObjTemplate;
import objects.Personnage.Group;
import objects.Sort.SortStats;
import common.*;
import common.World.ItemSet;

public class GameThread implements Runnable
{
	private long _LastDateFonction = 0;
	private BufferedReader _in;
	private Thread _t;
	private PrintWriter _out;
	private Socket _s;
	private Compte _compte;
	private Personnage _perso;
	private Map<Integer,GameAction> _actions = new TreeMap<Integer,GameAction>();
	private long _timeLastTradeMsg = 0, _timeLastRecrutmentMsg = 0, _timeLastsave = 0;
	
	public static class GameAction
	{
		public int _id;
		public int _actionID;
		public String _packet;
		public String _args;
		
		public GameAction(int aId, int aActionId,String aPacket)
		{
			_id = aId;
			_actionID = aActionId;
			_packet = aPacket;
		}
	}
	
	public GameThread(Socket sock)
	{
		try
		{
			_s = sock;
			_in = new BufferedReader(new InputStreamReader(_s.getInputStream()));
			_out = new PrintWriter(_s.getOutputStream());
			_t = new Thread(this);
			_t.setDaemon(true);
			_t.start();
		}
		catch(IOException e)
		{
			try {
				GameServer.addToLog(e.getMessage());
				if(!_s.isClosed())_s.close();
			} catch (IOException e1) {e1.printStackTrace();}
		}
	}
	
	public void run()
	{
		try
    	{
			String packet = "";
			char charCur[] = new char[1];
			SocketManager.GAME_SEND_HELLOGAME_PACKET(_out);
	    	while(_in.read(charCur, 0, 1)!=-1 && Ancestra.isRunning)
	    	{
	    		if (charCur[0] != '\u0000' && charCur[0] != '\n' && charCur[0] != '\r')
		    	{
	    			packet += charCur[0];
		    	}else if(packet != "")
		    	{
		    		GameServer.addToSockLog("Game: Recv << "+packet);
		    		parsePacket(packet);
		    		packet = "";
		    	}
	    	}
    	}catch(IOException e)
    	{
    		try
    		{
    			GameServer.addToLog(e.getMessage());
	    		_in.close();
	    		_out.close();
	    		if(_compte != null)
	    		{
	    			_compte.setCurPerso(null);
	    			_compte.setGameThread(null);
	    			_compte.setRealmThread(null);
	    		}
	    		if(!_s.isClosed())_s.close();
	    	}catch(IOException e1){e1.printStackTrace();};
    	}catch(Exception e)
    	{
    		e.printStackTrace();
    		GameServer.addToLog(e.getMessage());
    	}
    	finally
    	{
    		kick();
    	}
	}

	private void parsePacket(String packet)
	{
		if(packet.length()>3 && packet.substring(0,4).equalsIgnoreCase("ping"))
		{
			SocketManager.GAME_SEND_PONG(_out);
			return;
		}
		if(packet.length()>4 && packet.substring(0,5).equalsIgnoreCase("qping"))
		{
			SocketManager.GAME_SEND_QPONG(_out);
			return;
		}
		
		switch(packet.charAt(0))
		{
			case 'A':
				parseAccountPacket(packet);
			break;
			case 'B':
				parseBasicsPacket(packet);
			break;
			case 'c':
				parseChanelPacket(packet);
			break;
			case 'D':
				parseDialogPacket(packet);
			break;
			case 'E':
				parseExchangePacket(packet);
			break;
			case 'e':
				parse_environementPacket(packet);
			break;
			case 'F':
				parse_friendPacket(packet);
			break;
			case 'f':
				parseFightPacket(packet);
			break;
			case 'G':
				parseGamePacket(packet);
			break;
			case 'g':
				parseGuildPacket(packet);
			break;
			case 'h':
				parseHouseSecPacket(packet);
			break;
			case 'i':
				parseEnemyPacket(packet);
			break;
			case 'K':
				parseHousePacket(packet);
			break;
			case 'O':
				parseObjectPacket(packet);
			break;
			case 'P':
				parseGroupPacket(packet);
			break;
			case 'R':
				parseMountPacket(packet);
			break;
			case 'S':
				parseSpellPacket(packet);
			break;
			case 'W':
				parseWaypointPacket(packet);
			break;
		}
	}
	
	private void parseHouseSecPacket(String packet)
	{
		switch(packet.charAt(1))
		{
		case 'B'://Acheter la maison
			packet = packet.substring(2);
			House.HouseAchat(_perso);
		break;
		case 'G'://Maison de guilde
			packet = packet.substring(2);
			if(packet.isEmpty()) packet = null;
			House.parseHG(_perso, packet);
		break;
		case 'Q'://Quitter/Expulser de la maison
			packet = packet.substring(2);
			House.Leave(_perso, packet);
		break;
		case 'S'://Modification du prix de vente
			packet = packet.substring(2);
			House.SellPrice(_perso, packet);
		break;
		case 'V'://Fermer fenetre d'achat
			House.closeBuy(_perso);
		break;
		}
	}
	
	private void parseHousePacket(String packet)
	{
		switch(packet.charAt(1))
		{
		case 'V'://Fermer fenetre du code
			House.closeCode(_perso);
		break;
		case 'K'://Envoi du code
			House_code(packet);
		break;
		}
	}
	
	private void House_code(String packet)
	{
		switch(packet.charAt(2))
		{
		case '0'://Envoi du code
			packet = packet.substring(4);
			House.OpenHouse(_perso, packet, false);
		break;
		case '1'://Changement du code.
			packet = packet.substring(4);
			House.LockHouse(_perso, packet);
		break;
		}
	}
	
	private void parseEnemyPacket(String packet)
	{
		switch(packet.charAt(1))
		{
		case 'A'://Ajouter
			Enemy_add(packet);
			break;
		case 'D'://Delete
			Enemy_delete(packet);
			break;
		case 'L'://Liste
			SocketManager.GAME_SEND_ENEMY_LIST(_perso);
			break;
		}
	}
	
	private void Enemy_add(String packet)
	{
		if(_perso == null)return;
		int guid = -1;
		switch(packet.charAt(2))
		{
			case '%'://Nom du joueurs
				packet = packet.substring(3);
				Personnage P = World.getPersoByName(packet);
				if(P == null)
				{
					SocketManager.GAME_SEND_FD_PACKET(_perso, "Ef");
					return;
				}
				guid = P.getAccID();
				
			break;
			case '*'://Nom de compte
				packet = packet.substring(3);
				Compte C = World.getCompteByPseudo(packet);
				if(C==null)
				{
					SocketManager.GAME_SEND_FD_PACKET(_perso, "Ef");
					return;
				}
				guid = C.get_GUID();
			break;
			default:
				packet = packet.substring(2);
				Personnage Pr = World.getPersoByName(packet);
				if(Pr == null?true:!Pr.isOnline())
				{
					SocketManager.GAME_SEND_FD_PACKET(_perso, "Ef");
					return;
				}
				guid = Pr.get_compte().get_GUID();
			break;
		}
		/*
		 if(guid == -1 || !_compte.isEnemyWith(guid))
		
		{
			SocketManager.GAME_SEND_FD_PACKET(_perso, "Ef");
			GameServer.addToLog("STEP 7");
			return;
		}
		*/
			_compte.addEnemy(packet, guid);
	}

	private void Enemy_delete(String packet)
	{
	if(_perso == null)return;
	int guid = -1;
	switch(packet.charAt(2))
	{
		case '%'://Nom du joueurs
			packet = packet.substring(3);
			Personnage P = World.getPersoByName(packet);
			if(P == null)
			{
				SocketManager.GAME_SEND_FD_PACKET(_perso, "Ef");
				return;
			}
			guid = P.getAccID();
			
		break;
		case '*'://Nom de compte
			packet = packet.substring(3);
			Compte C = World.getCompteByName(packet);
			if(C==null)
			{
				SocketManager.GAME_SEND_FD_PACKET(_perso, "Ef");
				return;
			}
			guid = C.get_GUID();
		break;
		default:
			packet = packet.substring(2);
			Personnage Pr = World.getPersoByName(packet);
			if(Pr == null?true:!Pr.isOnline())
			{
				SocketManager.GAME_SEND_FD_PACKET(_perso, "Ef");
				return;
			}
			guid = Pr.get_compte().get_GUID();
		break;
	}
	/*
	if(guid == -1 || !_compte.isEnemyWith(guid))
	{
		SocketManager.GAME_SEND_FD_PACKET(_perso, "Ef");
		return;
	}
	*/
	_compte.removeEnemy(guid);
}
	
	private void parseWaypointPacket(String packet)
	{
		switch(packet.charAt(1))
		{
			case 'U'://Use
				Waypoint_use(packet);
			break;
			case 'u'://use zaapi
				Zaapi_use(packet);
			break;
			case 'v'://quitter zaapi
				Zaapi_close();
			break;
			case 'V'://Quitter
				Waypoint_quit();
			break;
		}
	}

	private void Zaapi_close()
	{
		_perso.Zaapi_close();
	}
	
	private void Zaapi_use(String packet)
	{
		_perso.Zaapi_use(packet);
	}
	
	private void Waypoint_quit()
	{
		_perso.stopZaaping();
	}

	private void Waypoint_use(String packet)
	{
		short id = -1;
		try
		{
			id = Short.parseShort(packet.substring(2));
		}catch(Exception e){};
		if( id == -1)return;
		_perso.useZaap(id);
	}
	private void parseGuildPacket(String packet)
	{
		switch(packet.charAt(1))
		{
			case 'C'://Creation
				guild_create(packet);
			break;
			case 'f'://Téléportation enclo de guilde
				guild_enclo(packet.substring(2));
			break;
			case 'F'://Retirer percepteur
				guild_remove_perco(packet.substring(2));
			break;
			case 'h'://Téléportation maison de guilde
				guild_house(packet.substring(2));
			break;
			case 'H'://Poser un percepteur
				guild_add_perco();
			break;
			case 'I'://Infos
				guild_infos(packet.charAt(2));
			break;
			case 'J'://Join
				guild_join(packet.substring(2));
			break;
			case 'K'://Kick
				guild_kick(packet.substring(2));
			break;
			case 'P'://Promote
				guild_promote(packet.substring(2));
			break;
			case 'T'://attaque sur percepteur
				guild_perco_join_fight(packet.substring(2));
			break;
			case 'V'://Ferme le panneau de création de guilde
				guild_CancelCreate();
			break;
		}
	}
	
	private void guild_perco_join_fight(String packet) 
	{
		switch(packet.charAt(0))
		{
			case 'J'://Rejoindre
				String PercoID = Integer.toString(Integer.parseInt(packet.substring(1)), 36);
				int TiD = Integer.parseInt(PercoID);
				Percepteur perco = Percepteur.GetPerco(TiD);
				int FightID = perco.get_inFightID();
				short MapID = World.getCarte((short)perco.get_mapID()).getFight(FightID).get_map().get_id();
				int CellID = perco.get_cellID();
				_perso.teleport(MapID, CellID);
				World.getCarte(MapID).getFight(FightID).joinPercepteurFight(_perso,_perso.get_GUID(), TiD);
			break;
		}
	}

	private void guild_remove_perco(String packet) 
	{
		if(_perso.get_guild() == null)return;
		if(!_perso.getGuildMember().canDo(Constants.G_POSPERCO))return;//On peut le retirer si on a le droit de le poser
		byte IDPerco = Byte.parseByte(packet);
		Percepteur perco = Percepteur.GetPerco(IDPerco);
		if(perco.get_inFight() > 0) return;
		SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(_perso.get_curCarte(), IDPerco);
		SQLManager.DELETE_PERCO(perco.getGuid());
		perco.DelPerco(perco.getGuid());
	}

	private void guild_add_perco() 
	{
		if(_perso.get_guild() == null)return;
		if(!_perso.getGuildMember().canDo(Constants.G_POSPERCO))return;//Pas le droit de le poser
		if(_perso.get_guild().getMembers().size() < 10)return;//Guilde invalide
		short price = (short)(1000+10*_perso.get_guild().get_lvl());//Calcul du prix du percepteur
		if(_perso.get_kamas() < price) return;//Kamas insuffisants
		if(Percepteur.GetPercoGuildID(_perso.get_curCarte().get_id()) > 0)return;//La carte possède un perco
		if(_perso.get_curCarte().get_placesStr().length() < 5)return;//La map ne possède pas de "places"
		if(((int)Math.floor(_perso.get_guild().get_lvl()/10))-Percepteur.CountPercoGuild(_perso.get_guild().get_id()) < 1) return;
		//FIXME : Don't Works
		short lower1 = 1;
		short higher1 = 129;
		short random1 = (short) ((short)(Math.random() * (higher1-lower1)) + lower1);
		short lower2 = 1;
		short higher2 = 227;
		short random2 = (short) ((short)(Math.random() * (higher2-lower2)) + lower2);
		//Ajout du Perco.
		int id = SQLManager.GetNewIDPercepteur();
		Percepteur perco = new Percepteur(id, _perso.get_curCarte().get_id(), _perso.get_curCell().getID(), (byte)3, _perso.get_guild().get_id(), random1, random2);
		Percepteur.addPerco(perco);
		SocketManager.GAME_SEND_ADD_PERCO_TO_MAP(_perso.get_curCarte());
		SQLManager.ADD_PERCO_ON_MAP(id, _perso.get_curCarte().get_id(), _perso.get_guild().get_id(), _perso.get_curCell().getID(), 3, random1, random2);
	}

	private void guild_enclo(String packet)
	{
		if(_perso.get_guild() == null)return;
		
		short MapID = Short.parseShort(packet);
		MountPark MP = World.getCarte(MapID).getMountPark();
		if(MP.get_guild().get_id() != _perso.get_guild().get_id()) return;
		int CellID = World.getEncloCellIdByMapId(MapID);
		if (_perso.hasItemTemplate(9035, 1))
		{
			_perso.removeByTemplateID(9035,1);
			_perso.teleport(MapID, CellID);
		}
	}
	
	private void guild_house(String packet)
	{
		if(_perso.get_guild() == null)return;
		int HouseID = Integer.parseInt(packet);
		House.HouseCoordByID(HouseID);
		if(_perso.get_guild().get_id() != House.isGuild) return;
		if(!House.canDo(Constants.H_GTELE)) return;
		if (_perso.hasItemTemplate(8883, 1))
		{
			_perso.removeByTemplateID(8883,1);
			_perso.teleport(House.isMapID, House.isCellID);
		}
	}
	
	private void guild_promote(String packet)
	{
		if(_perso.get_guild() == null)return;	//Si le personnage envoyeur n'a même pas de guilde
		
		String[] infos = packet.split("\\|");
		
		int guid = Integer.parseInt(infos[0]);
		int rank = Integer.parseInt(infos[1]);
		byte xpGive = Byte.parseByte(infos[2]);
		int right = Integer.parseInt(infos[3]);
		
		Personnage p = World.getPersonnage(guid);	//Cherche le personnage a qui l'on change les droits dans la mémoire
		GuildMember toChange;
		GuildMember changer = _perso.getGuildMember();
		
		//Récupération du personnage à changer, et verification de quelques conditions de base
		if(p == null)	//Arrive lorsque le personnage n'est pas chargé dans la mémoire
		{
			int guildId = SQLManager.isPersoInGuild(guid);	//Récupère l'id de la guilde du personnage qui n'est pas dans la mémoire
			
			if(guildId < 0)return;	//Si le personnage à qui les droits doivent être modifié n'existe pas ou n'a pas de guilde
			
			
			if(guildId != _perso.get_guild().get_id())					//Si ils ne sont pas dans la même guilde
			{
				SocketManager.GAME_SEND_gK_PACKET(_perso, "Ed");
				return;
			}
			toChange = World.getGuild(guildId).getMember(guid);
		}
		else
		{
			if(p.get_guild() == null)return;	//Si la personne à qui changer les droits n'a pas de guilde
			if(_perso.get_guild().get_id() != p.get_guild().get_id())	//Si ils ne sont pas de la meme guilde
			{
				SocketManager.GAME_SEND_gK_PACKET(_perso, "Ea");
				return;
			}
			
			toChange = p.getGuildMember();
		}
		
		//Vérifie ce que le personnage changeur à le droit de faire
		
		if(changer.getRank() == 1)	//Si c'est le meneur
		{
			if(changer.getGuid() == toChange.getGuid())	//Si il se modifie lui même, reset tout sauf l'XP
			{
				rank = -1;
				right = -1;
			}
			else //Si il modifie un autre membre
			{
				if(rank == 1) //Si il met un autre membre "Meneur"
				{
					changer.setAllRights(2, (byte) -1, 29694);	//Met le meneur "Bras droit" avec tout les droits
					
					//Défini les droits à mettre au nouveau meneur
					rank = 1;
					xpGive = -1;
					right = 1;
				}
			}
		}
		else	//Sinon, c'est un membre normal
		{
			if(toChange.getRank() == 1)	//S'il veut changer le meneur, reset tout sauf l'XP
			{
				rank = -1;
				right = -1;
			}
			else	//Sinon il veut changer un membre normal
			{
				if(!changer.canDo(Constants.G_RANK) || rank == 1)	//S'il ne peut changer les rang ou qu'il veut mettre meneur
					rank = -1; 	//"Reset" le rang
				
				if(!changer.canDo(Constants.G_RIGHT) || right == 1)	//S'il ne peut changer les droits ou qu'il veut mettre les droits de meneur
					right = -1;	//"Reset" les droits
				
				if(!changer.canDo(Constants.G_HISXP) && !changer.canDo(Constants.G_ALLXP) && changer.getGuid() == toChange.getGuid())	//S'il ne peut changer l'XP de personne et qu'il est la cible
					xpGive = -1; //"Reset" l'XP
			}
			
			if(!changer.canDo(Constants.G_ALLXP) && !changer.equals(toChange))	//S'il n'a pas le droit de changer l'XP des autres et qu'il n'est pas la cible
				xpGive = -1; //"Reset" L'XP
		}

		toChange.setAllRights(rank,xpGive,right);
		
		SocketManager.GAME_SEND_gS_PACKET(_perso,_perso.getGuildMember());
		
		if(p != null && p.get_GUID() != _perso.get_GUID())
			SocketManager.GAME_SEND_gS_PACKET(p,p.getGuildMember());
	}
	
	private void guild_CancelCreate()
	{
		SocketManager.GAME_SEND_gV_PACKET(_perso);
	}

	private void guild_kick(String name)
	{
		if(_perso.get_guild() == null)return;
		Personnage P = World.getPersoByName(name);
		int guid = -1,guildId = -1;
		Guild toRemGuild;
		GuildMember toRemMember;
		
		if(P == null)
		{
			int infos[] = SQLManager.isPersoInGuild(name);
			guid = infos[0];
			guildId = infos[1];
			if(guildId < 0 || guid < 0)return;
			toRemGuild = World.getGuild(guildId);
			toRemMember = toRemGuild.getMember(guid);
		}
		else
		{
			toRemGuild = P.get_guild();
			toRemMember = toRemGuild.getMember(P.get_GUID());
		}
		//si pas la meme guilde
		if(toRemGuild.get_id() != _perso.get_guild().get_id())
		{
			SocketManager.GAME_SEND_gK_PACKET(_perso, "Ea");
			return;
		}
		//S'il n'a pas le droit de kick, et que ce n'est pas lui même la cible
		if(!_perso.getGuildMember().canDo(Constants.G_BAN) && _perso.getGuildMember().getGuid() != toRemMember.getGuid())
		{
			SocketManager.GAME_SEND_gK_PACKET(_perso, "Ed");
			return;
		}
		//Si différent : Kick 
		if(_perso.getGuildMember().getGuid() != toRemMember.getGuid())
		{
			if(toRemMember.getRank() == 1) //S'il veut kicker le meneur
				return;
			
			toRemGuild.removeMember(toRemMember.getGuid());
			if(P != null)
				P.setGuildMember(null);
			
			SocketManager.GAME_SEND_gK_PACKET(_perso, "K"+_perso.get_name()+"|"+name);
			if(P != null)
				SocketManager.GAME_SEND_gK_PACKET(P, "K"+_perso.get_name());
		}else//si quitter
		{
			Guild G = _perso.get_guild();
			if(_perso.getGuildMember().getRank() == 1 && G.getMembers().size() > 1)	//Si le meneur veut quitter la guilde mais qu'il reste d'autre joueurs
			{
				//TODO : Envoyer le message qu'il doit mettre un autre membre meneur (Pas vraiment....)
				return;
			}
			G.removeMember(_perso.get_GUID());
			_perso.setGuildMember(null);
			//S'il n'y a plus personne
			if(G.getMembers().size() == 0)World.removeGuild(G.get_id());
			SocketManager.GAME_SEND_gK_PACKET(_perso, "K"+name+"|"+name);
		}
	}
	
	private void guild_join(String packet)
	{
		switch(packet.charAt(0))
		{
		case 'R'://Nom perso
			Personnage P = World.getPersoByName(packet.substring(1));
			if(P == null || _perso.get_guild() == null)
			{
				SocketManager.GAME_SEND_gJ_PACKET(_perso, "Eu");
				return;
			}
			if(!P.isOnline())
			{
				SocketManager.GAME_SEND_gJ_PACKET(_perso, "Eu");
				return;
			}
			if(P.is_away())
			{
				SocketManager.GAME_SEND_gJ_PACKET(_perso, "Eo");
				return;
			}
			if(P.get_guild() != null)
			{
				SocketManager.GAME_SEND_gJ_PACKET(_perso, "Ea");
				return;
			}
			if(!_perso.getGuildMember().canDo(Constants.G_INVITE))
			{
				SocketManager.GAME_SEND_gJ_PACKET(_perso, "Ed");
				return;
			}
			
			_perso.setInvitation(P.get_GUID());
			P.setInvitation(_perso.get_GUID());

			SocketManager.GAME_SEND_gJ_PACKET(_perso,"R"+packet.substring(1));
			SocketManager.GAME_SEND_gJ_PACKET(P,"r"+_perso.get_GUID()+"|"+_perso.get_name()+"|"+_perso.get_guild().get_name());
		break;
		case 'E'://ou Refus
			if(packet.substring(1).equalsIgnoreCase(_perso.getInvitation()+""))
			{
				Personnage p = World.getPersonnage(_perso.getInvitation());
				if(p == null)return;//Pas censé arriver
				SocketManager.GAME_SEND_gJ_PACKET(p,"Ec");
			}
		break;
		case 'K'://Accepte
			if(packet.substring(1).equalsIgnoreCase(_perso.getInvitation()+""))
			{
				Personnage p = World.getPersonnage(_perso.getInvitation());
				if(p == null)return;//Pas censé arriver
				Guild G = p.get_guild();
				GuildMember GM = G.addNewMember(_perso);
				SQLManager.UPDATE_GUILDMEMBER(GM);
				_perso.setGuildMember(GM);
				_perso.setInvitation(-1);
				p.setInvitation(-1);
				//Packet
				SocketManager.GAME_SEND_gJ_PACKET(p,"Ka"+_perso.get_name());
				SocketManager.GAME_SEND_gS_PACKET(_perso, GM);
				SocketManager.GAME_SEND_gJ_PACKET(_perso,"Kj");
			}
		break;
		}
	}

	private void guild_infos(char c)
	{
		switch(c)
		{
		case 'B'://Perco
			String packet = _perso.get_guild().get_lvl()+"|"+Percepteur.CountPercoGuild(_perso.get_guild().get_id())+"|"+100*_perso.get_guild().get_lvl()+"|"+_perso.get_guild().get_lvl()+"|1000|100|0|1|0|"+(1000+(10*_perso.get_guild().get_lvl()))+"|462;0|461;0|460;0|459;0|458;0|457;0|456;0|455;0|454;0|453;0|452;0|451;0";
			SocketManager.GAME_SEND_gIB_PACKET(_perso, packet);
			//Percomax|0|100*level|level|perco_add_pods|perco_prospection|perco_sagesse|perco_max|perco_boost|1000+10*level|perco_spells
		break;
		case 'F'://Enclos
			SocketManager.GAME_SEND_gIF_PACKET(_perso, SQLManager.parseMPtoGuild(_perso.get_guild().get_id()));
		break;
		case 'G'://General
			SocketManager.GAME_SEND_gIG_PACKET(_perso, _perso.get_guild());
		break;
		case 'H'://House
			SocketManager.GAME_SEND_gIH_PACKET(_perso, House.parseHouseToGuild(_perso));
		break;
		case 'M'://Members
			SocketManager.GAME_SEND_gIM_PACKET(_perso, _perso.get_guild(),'+');
		break;
		case 'T'://Perco
			SocketManager.GAME_SEND_gITM_PACKET(_perso, Percepteur.parsetoGuild(_perso.get_guild().get_id()));
			Percepteur.parseAttaque(_perso, _perso.get_guild().get_id());
			Percepteur.parseDefense(_perso, _perso.get_guild().get_id());
		break;
		}
	}

	private void guild_create(String packet)
	{
		if(_perso == null)return;
		if(_perso.get_guild() != null || _perso.getGuildMember() != null)
		{
			SocketManager.GAME_SEND_gC_PACKET(_perso, "Ea");
			return;
		}
		if(_perso.get_fight() != null )return;
		try
		{
			String[] infos = packet.substring(2).split("\\|");
			//base 10 => 36
			String bgID = Integer.toString(Integer.parseInt(infos[0]),36);
			String bgCol = Integer.toString(Integer.parseInt(infos[1]),36);
			String embID =  Integer.toString(Integer.parseInt(infos[2]),36);
			String embCol =  Integer.toString(Integer.parseInt(infos[3]),36);
			String name = infos[4];
			if(World.guildNameIsUsed(name))
			{
				SocketManager.GAME_SEND_gC_PACKET(_perso, "Ean");
				return;
			}
			
			//Validation du nom de la guilde
			String tempName = name.toLowerCase();
			boolean isValid = true;
			//Vérifie d'abord si il contient des termes définit
			if(tempName.length() > 20
					|| tempName.contains("mj")
					|| tempName.contains("modo")
					|| tempName.contains("admin"))
			{
				isValid = false;
			}
			//Si le nom passe le test, on vérifie que les caractère entré sont correct.
			if(isValid)
			{
				int tiretCount = 0;
				for(char curLetter : tempName.toCharArray())
				{
					if(!(	(curLetter >= 'a' && curLetter <= 'z')
							|| curLetter == '-'))
					{
						isValid = false;
						break;
					}
					if(curLetter == '-')
					{
						if(tiretCount >= 2)
						{
							isValid = false;
							break;
						}
						else
						{
							tiretCount++;
						}
					}
				}
			}
			//Si le nom est invalide
			if(!isValid)
			{
				SocketManager.GAME_SEND_gC_PACKET(_perso, "Ean");
				return;
			}
			//FIN de la validation
			String emblem = bgID+","+bgCol+","+embID+","+embCol;//9,6o5nc,2c,0;
			if(World.guildEmblemIsUsed(emblem))
			{
				SocketManager.GAME_SEND_gC_PACKET(_perso, "Eae");
				return;
			}
			if(_perso.get_curCarte().get_id() == 2196)//Temple de création de guilde
			{
				if(!_perso.hasItemTemplate(1575,1))//Guildalogemme
				{
					SocketManager.GAME_SEND_Im_PACKET(_perso, "14");
					return;
				}
				_perso.removeByTemplateID(1575, 1);
			}
			Guild G = new Guild(_perso,name,emblem);
			GuildMember gm = G.addNewMember(_perso);
			gm.setAllRights(1,(byte) 0,1);//1 => Meneur (Tous droits)
			_perso.setGuildMember(gm);//On ajoute le meneur
			World.addGuild(G, true);
			SQLManager.UPDATE_GUILDMEMBER(gm);
			//Packets
			SocketManager.GAME_SEND_gS_PACKET(_perso, gm);
			SocketManager.GAME_SEND_gC_PACKET(_perso,"K");
			SocketManager.GAME_SEND_gV_PACKET(_perso);
		}catch(Exception e){return;};
	}

	private void parseChanelPacket(String packet)
	{
		switch(packet.charAt(1))
		{
			case 'C'://Changement des Canaux
				Chanels_change(packet);
			break;
		}
	}

	private void Chanels_change(String packet)
	{
		String chan = packet.charAt(3)+"";
		switch(packet.charAt(2))
		{
			case '+'://Ajout du Canal
				_perso.addChanel(chan);
			break;
			case '-'://Desactivation du canal
				_perso.removeChanel(chan);
			break;
		}
		SQLManager.SAVE_PERSONNAGE(_perso, false);
	}

	private void parseMountPacket(String packet)
	{
		switch(packet.charAt(1))
		{
			case 'b'://Achat d'un enclos
				SocketManager.GAME_SEND_R_PACKET(_perso, "v");//Fermeture du panneau
				MountPark MP = _perso.get_curCarte().getMountPark();
				Personnage Seller = World.getPersonnage(MP.get_owner());
				if(_perso.get_guild() == null)
				{
					SocketManager.GAME_SEND_MESSAGE(_perso, "Vous ne possedez pas de guilde.", Ancestra.CONFIG_MOTD_COLOR);
					return;
				}
				byte enclosMax = (byte)Math.floor(_perso.get_guild().get_lvl()/10);
				byte TotalEncloGuild = SQLManager.TotalMPGuild(_perso.get_guild().get_id()); 
				if(TotalEncloGuild >= enclosMax)
				{
					SocketManager.GAME_SEND_MESSAGE(_perso, "Votre guilde ne possede pas le niveau necessaire.", Ancestra.CONFIG_MOTD_COLOR);
					return;
				}
				if(_perso.get_kamas() < MP.get_price())
				{
					SocketManager.GAME_SEND_MESSAGE(_perso, "Vous ne possedez pas assez de kamas.", Ancestra.CONFIG_MOTD_COLOR);
					return;
				}
				long NewKamas = _perso.get_kamas()-MP.get_price();
				_perso.set_kamas(NewKamas);
				if(Seller != null)
				{
					long NewSellerBankKamas = Seller.getBankKamas()+MP.get_price();
					Seller.setBankKamas(NewSellerBankKamas);
					if(Seller.isOnline())
					{
						SocketManager.GAME_SEND_MESSAGE(_perso, "Un enclo a été vendu a "+MP.get_price()+".", Ancestra.CONFIG_MOTD_COLOR);
					}
				}
				MP.set_price(0);//On vide le prix
				MP.set_owner(_perso.get_GUID());
				MP.set_guild(_perso.get_guild());
				SQLManager.SAVE_MOUNTPARK(MP);
				SQLManager.SAVE_PERSONNAGE(_perso, true);
				//On rafraichit l'enclo
				for(Personnage z:_perso.get_curCarte().getPersos())
				{
					SocketManager.GAME_SEND_Rp_PACKET(z, MP);
				}
			break;
		
			case 'd'://Demande Description
				Mount_description(packet);
			break;
			
			case 'n'://Change le nom
				Mount_name(packet.substring(2));
			break;
			
			case 'r'://Monter sur la dinde
				Mount_ride();
			break;
			case 's'://Vendre l'enclo
				SocketManager.GAME_SEND_R_PACKET(_perso, "v");//Fermeture du panneau
				int price = Integer.parseInt(packet.substring(2));
				MountPark MP1 = _perso.get_curCarte().getMountPark();
				MP1.set_price(price);
				SQLManager.SAVE_MOUNTPARK(MP1);
				SQLManager.SAVE_PERSONNAGE(_perso, true);
				//On rafraichit l'enclo
				for(Personnage z:_perso.get_curCarte().getPersos())
				{
					SocketManager.GAME_SEND_Rp_PACKET(z, MP1);
				}
			break;
			case 'v'://Fermeture panneau d'achat
				SocketManager.GAME_SEND_R_PACKET(_perso, "v");
			break;
			case 'x'://Change l'xp donner a la dinde
				Mount_changeXpGive(packet);
			break;
		}
	}

	private void Mount_changeXpGive(String packet)
	{
		try
		{
			int xp = Integer.parseInt(packet.substring(2));
			if(xp <0)xp = 0;
			if(xp >90)xp = 90;
			_perso.setMountGiveXp(xp);
			SocketManager.GAME_SEND_Rx_PACKET(_perso);
		}catch(Exception e){};
	}

	private void Mount_name(String name)
	{
		if(_perso.getMount() == null)return;
		_perso.getMount().setName(name);
		SocketManager.GAME_SEND_Rn_PACKET(_perso, name);
	}

	private void Mount_ride()
	{
		if(_perso.get_lvl()<60 || _perso.getMount() == null || !_perso.getMount().isMountable())
		{
			SocketManager.GAME_SEND_Re_PACKET(_perso,"Er", null);
			return;
		}
		_perso.toogleOnMount();
	}

	private void Mount_description(String packet)
	{
		int DDid = -1;
		try
		{
			DDid = Integer.parseInt(packet.substring(2).split("\\|")[0]);
			//on ignore le temps?
		}catch(Exception e){};
		if(DDid == -1)return;
		Dragodinde DD = World.getDragoByID(DDid);
		if(DD == null)return;
		SocketManager.GAME_SEND_MOUNT_DESCRIPTION_PACKET(_perso,DD);
	}

	private void parse_friendPacket(String packet)
	{
		switch(packet.charAt(1))
		{
			case 'A'://Ajouter
				Friend_add(packet);
			break;
			case 'D'://Effacer un ami
				Friend_delete(packet);
			break;
			case 'L'://Liste
				SocketManager.GAME_SEND_FRIENDLIST_PACKET(_perso);
			break;
			case 'O':
				switch(packet.charAt(2))
				{
				case '-':
					 _perso.SetSeeFriendOnline(false);
					 SocketManager.GAME_SEND_BN(_perso);
					 break;
				 case'+':
					 _perso.SetSeeFriendOnline(true);
					 SocketManager.GAME_SEND_BN(_perso);
					 break;
				}
		break;
		}
	}

	private void Friend_delete(String packet) {
		if(_perso == null)return;
		int guid = -1;
		switch(packet.charAt(2))
		{
			case '%'://nom de perso
				packet = packet.substring(3);
				Personnage P = World.getPersoByName(packet);
				if(P == null)//Si P est nul, ou si P est nonNul et P offline
				{
					SocketManager.GAME_SEND_FD_PACKET(_perso, "Ef");
					return;
				}
				guid = P.getAccID();
				
			break;
			case '*'://Pseudo
				packet = packet.substring(3);
				Compte C = World.getCompteByPseudo(packet);
				if(C==null)
				{
					SocketManager.GAME_SEND_FD_PACKET(_perso, "Ef");
					return;
				}
				guid = C.get_GUID();
			break;
			default:
				packet = packet.substring(2);
				Personnage Pr = World.getPersoByName(packet);
				if(Pr == null?true:!Pr.isOnline())//Si P est nul, ou si P est nonNul et P offline
				{
					SocketManager.GAME_SEND_FD_PACKET(_perso, "Ef");
					return;
				}
				guid = Pr.get_compte().get_GUID();
			break;
		}
		if(guid == -1 || !_compte.isFriendWith(guid))
		{
			SocketManager.GAME_SEND_FD_PACKET(_perso, "Ef");
			return;
		}
		_compte.removeFriend(guid);
	}

	private void Friend_add(String packet)
	{
		if(_perso == null)return;
		int guid = -1;
		switch(packet.charAt(2))
		{
			case '%'://nom de perso
				packet = packet.substring(3);
				Personnage P = World.getPersoByName(packet);
				if(P == null?true:!P.isOnline())//Si P est nul, ou si P est nonNul et P offline
				{
					SocketManager.GAME_SEND_FA_PACKET(_perso, "Ef");
					return;
				}
				guid = P.getAccID();
			break;
			case '*'://Pseudo
				packet = packet.substring(3);
				Compte C = World.getCompteByPseudo(packet);
				if(C==null?true:!C.isOnline())
				{
					SocketManager.GAME_SEND_FA_PACKET(_perso, "Ef");
					return;
				}
				guid = C.get_GUID();
			break;
			default:
				packet = packet.substring(2);
				Personnage Pr = World.getPersoByName(packet);
				if(Pr == null?true:!Pr.isOnline())//Si P est nul, ou si P est nonNul et P offline
				{
					SocketManager.GAME_SEND_FA_PACKET(_perso, "Ef");
					return;
				}
				guid = Pr.get_compte().get_GUID();
			break;
		}
		if(guid == -1)
		{
			SocketManager.GAME_SEND_FA_PACKET(_perso, "Ef");
			return;
		}
		_compte.addFriend(guid);
	}

	private void parseGroupPacket(String packet)
	{
		switch(packet.charAt(1))
		{
			case 'A'://Accepter invitation
				group_accept(packet);
			break;
			
			case 'I'://inviation
				group_invite(packet);
			break;
			
			case 'R'://Refuse
				group_refuse();
			break;
			
			case 'V'://Quitter
				group_quit(packet);
			break;
		}
	}

	private void group_quit(String packet)
	{
		if(_perso == null)return;
		Group g = _perso.getGroup();
		if(g == null)return;
		if(packet.length() == 2)//Si aucun guid est spécifié, alors c'est que le joueur quitte
		{
			 g.leave(_perso);
			 SocketManager.GAME_SEND_PV_PACKET(_out,"");
		}else if(g.isChief(_perso.get_GUID()))//Sinon, c'est qu'il kick un joueur du groupe
		{
			int guid = -1;
			try
			{
				guid = Integer.parseInt(packet.substring(2));
			}catch(NumberFormatException e){return;};
			if(guid == -1)return;
			Personnage t = World.getPersonnage(guid);
			g.leave(t);
			SocketManager.GAME_SEND_PV_PACKET(t.get_compte().getGameThread().get_out(),""+_perso.get_GUID());
		}
	}

	private void group_invite(String packet)
	{
		if(_perso == null)return;
		String name = packet.substring(2);
		Personnage target = World.getPersoByName(name);
		if(target == null)return;
		if(!target.isOnline())
		{
			SocketManager.GAME_SEND_GROUP_INVITATION_ERROR(_out,"n"+name);
			return;
		}
		if(target.getGroup() != null)
		{
			SocketManager.GAME_SEND_GROUP_INVITATION_ERROR(_out, "a"+name);
			return;
		}
		if(_perso.getGroup() != null && _perso.getGroup().getPersosNumber() == 8)
		{
			SocketManager.GAME_SEND_GROUP_INVITATION_ERROR(_out, "f");
			return;
		}
		target.setInvitation(_perso.get_GUID());	
		_perso.setInvitation(target.get_GUID());
		SocketManager.GAME_SEND_GROUP_INVITATION(_out,_perso.get_name(),name);
		SocketManager.GAME_SEND_GROUP_INVITATION(target.get_compte().getGameThread().get_out(),_perso.get_name(),name);
	}

	private void group_refuse()
	{
		if(_perso == null)return;
		if(_perso.getInvitation() == 0)return;
		Personnage t = World.getPersonnage(_perso.getInvitation());
		_perso.setInvitation(0);
		t.setInvitation(0);
		SocketManager.GAME_SEND_BN(_out);
		SocketManager.GAME_SEND_PR_PACKET(t);
	}

	private void group_accept(String packet)
	{
		if(_perso == null)return;
		if(_perso.getInvitation() == 0)return;
		Personnage t = World.getPersonnage(_perso.getInvitation());
		Group g = t.getGroup();
		if(g == null)
		{
			g = new Group(t,_perso);
			SocketManager.GAME_SEND_GROUP_CREATE(_out,g);
			SocketManager.GAME_SEND_PL_PACKET(_out,g);
			SocketManager.GAME_SEND_GROUP_CREATE(t.get_compte().getGameThread().get_out(),g);
			SocketManager.GAME_SEND_PL_PACKET(t.get_compte().getGameThread().get_out(),g);
			t.setGroup(g);
			SocketManager.GAME_SEND_ALL_PM_ADD_PACKET(t.get_compte().getGameThread().get_out(),g);
		}
		else
		{
			SocketManager.GAME_SEND_GROUP_CREATE(_out,g);
			SocketManager.GAME_SEND_PL_PACKET(_out,g);
			SocketManager.GAME_SEND_PM_ADD_PACKET_TO_GROUP(g, _perso);
			g.addPerso(_perso);
		}
		_perso.setGroup(g);
		SocketManager.GAME_SEND_ALL_PM_ADD_PACKET(_out,g);
		SocketManager.GAME_SEND_PR_PACKET(t);
	}

	private void parseObjectPacket(String packet)
	{
		switch(packet.charAt(1))
		{
			case 'd'://Supression d'un objet
				Object_delete(packet);
			break;
			case 'D'://Depose l'objet au sol
				Object_drop(packet);
			break;
			case 'M'://Bouger un objet (Equiper/déséquiper)
				Object_move(packet);
			break;
			
			case 'U'://Utiliser un objet (potions)
				Object_use(packet);
			break;
		}
	}

	private void Object_drop(String packet)
	{
		int guid = -1;
		int qua = -1;
		try
		{
			guid = Integer.parseInt(packet.substring(2).split("\\|")[0]);
			qua = Integer.parseInt(packet.split("\\|")[1]);
		}catch(Exception e){};
		if(guid == -1 || qua <= 0 || !_perso.hasItemGuid(guid))return;
		Objet obj = World.getObjet(guid);
		
		if(qua >= obj.getQuantity())
		{
			_perso.removeItem(guid);
			_perso.get_curCell().addDroppedItem(obj);
			obj.setPosition(Constants.ITEM_POS_NO_EQUIPED);
			SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(_perso, guid);
		}else
		{
			obj.setQuantity(obj.getQuantity() - qua);
			Objet obj2 = Objet.getCloneObjet(obj, qua);
			obj2.setPosition(Constants.ITEM_POS_NO_EQUIPED);
			_perso.get_curCell().addDroppedItem(obj2);
			SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(_perso, obj);
		}
		
		SocketManager.GAME_SEND_Ow_PACKET(_perso);
		SocketManager.GAME_SEND_GDO_PACKET_TO_MAP(_perso.get_curCarte(),'+',_perso.get_curCell().getID(),obj.getTemplate().getID(),0);
		SocketManager.GAME_SEND_STATS_PACKET(_perso);
	}

	private void Object_use(String packet)
	{
		int guid = -1;
		try
		{
			String[] infos = packet.substring(2).split("\\|");
			guid = Integer.parseInt(infos[0]);
		}catch(Exception e){return;};
		//Si le joueur n'a pas l'objet
		if(!_perso.hasItemGuid(guid))return;
		Objet obj = World.getObjet(guid);
		ObjTemplate T = obj.getTemplate();
		if(!obj.getTemplate().getConditions().equalsIgnoreCase("") && !ConditionParser.validConditions(_perso,obj.getTemplate().getConditions()))
		{
			SocketManager.GAME_SEND_Im_PACKET(_perso, "119|43");
			return;
		}
		T.applyAction(_perso, guid);
	}

	private synchronized void Object_move(String packet)
	{
		String[] infos = packet.substring(2).split(""+(char)0x0A)[0].split("\\|");
		try
		{
			int guid = Integer.parseInt(infos[0]);
			int pos = Integer.parseInt(infos[1]);
			Objet obj = World.getObjet(guid);
			if(!_perso.hasItemGuid(guid) || obj == null)
				return;
			if(_perso.get_fight() != null)return;
			if(!Constants.isValidPlaceForItem(obj.getTemplate(),pos) && pos != Constants.ITEM_POS_NO_EQUIPED)
				return;
			
			if(!obj.getTemplate().getConditions().equalsIgnoreCase("") && !ConditionParser.validConditions(_perso,obj.getTemplate().getConditions()))
			{
				SocketManager.GAME_SEND_Im_PACKET(_perso, "119|43");
				return;
			}
			
			Objet exObj = _perso.getObjetByPos(pos);
			if(exObj != null)//S'il y avait déja un objet => Ne devrait pas arriver, le client envoie déséquiper avant
			{
				Objet obj2;
				if(( obj2 = _perso.getSimilarItem(exObj)) != null)
				{
					obj2.setQuantity(obj2.getQuantity()+1);
					SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(_perso, obj2);
					World.removeItem(exObj.getGuid());
					_perso.removeItem(exObj.getGuid());
					SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(_perso, exObj.getGuid());
				}
				else
				{
					exObj.setPosition(Constants.ITEM_POS_NO_EQUIPED);
					SocketManager.GAME_SEND_OBJET_MOVE_PACKET(_perso,exObj);
				}
				if(_perso.getObjetByPos(Constants.ITEM_POS_ARME) == null)
					SocketManager.GAME_SEND_OT_PACKET(_out, -1);
				
				//Si objet de panoplie
				if(exObj.getTemplate().getPanopID() > 0)SocketManager.GAME_SEND_OS_PACKET(_perso,exObj.getTemplate().getPanopID());
			}//getNumbEquipedItemOfPanoplie(exObj.getTemplate().getPanopID()
			if(obj.getTemplate().getLevel() > _perso.get_lvl())
			{
				SocketManager.GAME_SEND_OAEL_PACKET(_out);
				return;
			}
			//On ne peut équiper 2 items de panoplies identiques, ou 2 Dofus identiques
			if(pos != Constants.ITEM_POS_NO_EQUIPED && (obj.getTemplate().getPanopID() != -1 || obj.getTemplate().getType() == Constants.ITEM_TYPE_DOFUS )&& _perso.hasEquiped(obj.getTemplate().getID()))
			return;
			
			Objet obj2;
			if(( obj2 = _perso.getSimilarItem(obj)) != null)
			{
				obj2.setQuantity(obj2.getQuantity()+1);
				SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(_perso, obj2);
				World.removeItem(obj.getGuid());
				_perso.removeItem(obj.getGuid());
				SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(_perso, obj.getGuid());
			}
			else
			{
				obj.setPosition(pos);
				SocketManager.GAME_SEND_OBJET_MOVE_PACKET(_perso,obj);
				if(obj.getQuantity() > 1)
				{
					int newItemQua = obj.getQuantity()-1;
					Objet newItem = Objet.getCloneObjet(obj,newItemQua);
					_perso.addObjet(newItem,false);
					World.addObjet(newItem,true);
					obj.setQuantity(1);
					SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(_perso, obj);
				}
			}
			SocketManager.GAME_SEND_Ow_PACKET(_perso);
			_perso.refreshStats();
			if(_perso.getGroup() != null)
			{
				SocketManager.GAME_SEND_PM_MOD_PACKET_TO_GROUP(_perso.getGroup(),_perso);
			}
			SocketManager.GAME_SEND_STATS_PACKET(_perso);
			if( pos == Constants.ITEM_POS_ARME 		||
				pos == Constants.ITEM_POS_COIFFE 	||
				pos == Constants.ITEM_POS_FAMILIER 	||
				pos == Constants.ITEM_POS_CAPE		||
				pos == Constants.ITEM_POS_BOUCLIER	||
				pos == Constants.ITEM_POS_NO_EQUIPED)
				SocketManager.GAME_SEND_ON_EQUIP_ITEM(_perso.get_curCarte(), _perso);
		
			//Si familier
			if(pos == Constants.ITEM_POS_FAMILIER && _perso.isOnMount())_perso.toogleOnMount();
			//Verif pour les outils de métier
			if(pos == Constants.ITEM_POS_NO_EQUIPED && _perso.getObjetByPos(Constants.ITEM_POS_ARME) == null)
				SocketManager.GAME_SEND_OT_PACKET(_out, -1);
			
			if(pos == Constants.ITEM_POS_ARME)
			{
				int ID = _perso.getObjetByPos(Constants.ITEM_POS_ARME).getTemplate().getID();
				for(Entry<Integer,StatsMetier> e : _perso.getMetiers().entrySet())
				{
					if(e.getValue().getTemplate().isValidTool(ID))
						SocketManager.GAME_SEND_OT_PACKET(_out,e.getValue().getTemplate().getId());
				}
			}
			//Si objet de panoplie
			if(obj.getTemplate().getPanopID() > 0)SocketManager.GAME_SEND_OS_PACKET(_perso,obj.getTemplate().getPanopID());
			
		}catch(Exception e)
		{
			e.printStackTrace();
			SocketManager.GAME_SEND_DELETE_OBJECT_FAILED_PACKET(_out);
		}
	}

	private void Object_delete(String packet)
	{
		String[] infos = packet.substring(2).split("\\|");
		try
		{
			int guid = Integer.parseInt(infos[0]);
			int qua = 1;
			try
			{
				qua = Integer.parseInt(infos[1]);
			}catch(Exception e){};
			Objet obj = World.getObjet(guid);
			if(obj == null || !_perso.hasItemGuid(guid) || qua <= 0)
			{
				SocketManager.GAME_SEND_DELETE_OBJECT_FAILED_PACKET(_out);
				return;
			}
			int newQua = obj.getQuantity()-qua;
			if(newQua <=0)
			{
				_perso.removeItem(guid);
				World.removeItem(guid);
				SQLManager.DELETE_ITEM(guid);
				SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(_perso, guid);
			}else
			{
				obj.setQuantity(newQua);
				SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(_perso, obj);
			}
			SocketManager.GAME_SEND_STATS_PACKET(_perso);
			SocketManager.GAME_SEND_Ow_PACKET(_perso);
		}catch(Exception e)
		{
			SocketManager.GAME_SEND_DELETE_OBJECT_FAILED_PACKET(_out);
		}
	}

	private void parseDialogPacket(String packet)
	{
		switch(packet.charAt(1))
		{
			case 'C'://Demande de l'initQuestion
				Dialog_start(packet);
			break;
			
			case 'R'://Réponse du joueur
				Dialog_response(packet);
			break;
			
			case 'V'://Fin du dialog
				Dialog_end();
			break;
		}
	}

	private void Dialog_response(String packet)
	{
		String[] infos = packet.substring(2).split("\\|");
		try
		{
			int qID = Integer.parseInt(infos[0]);
			int rID = Integer.parseInt(infos[1]);
			NPC_question quest = World.getNPCQuestion(qID);
			NPC_reponse rep = World.getNPCreponse(rID);
			if(quest == null || rep == null || !rep.isAnotherDialog())
			{
				SocketManager.GAME_SEND_END_DIALOG_PACKET(_out);
				_perso.set_isTalkingWith(0);
			}
			rep.apply(_perso);
		}catch(Exception e)
		{
			SocketManager.GAME_SEND_END_DIALOG_PACKET(_out);
		}
	}

	private void Dialog_end()
	{
		SocketManager.GAME_SEND_END_DIALOG_PACKET(_out);
		if(_perso.get_isTalkingWith() != 0)
			_perso.set_isTalkingWith(0);
	}

	private void Dialog_start(String packet)
	{
		try
		{
			int npcID = Integer.parseInt(packet.substring(2).split((char)0x0A+"")[0]);
			NPC npc = _perso.get_curCarte().getNPC(npcID);
			if( npc == null)return;
			SocketManager.GAME_SEND_DCK_PACKET(_out,npcID);
			int qID = npc.get_template().get_initQuestionID();
			NPC_question quest = World.getNPCQuestion(qID);
			if(quest == null)
			{
				SocketManager.GAME_SEND_END_DIALOG_PACKET(_out);
				return;
			}
			SocketManager.GAME_SEND_QUESTION_PACKET(_out,quest.parseToDQPacket(_perso));
			_perso.set_isTalkingWith(npcID);
		}catch(NumberFormatException e){};
	}

	private void parseExchangePacket(String packet)
	{	
		switch(packet.charAt(1))
		{
			case 'A'://Accepter demande d'échange
				Exchange_accept();
			break;
			case 'B'://Achat
				Exchange_onBuyItem(packet);
			break;
			
			case 'H'://Demande prix moyen + catégorie
				Exchange_BuySystem(packet);
			break;
			
			case 'K'://Ok
				Exchange_isOK();
			break;
			case 'L'://jobAction : Refaire le craft précedent
				Exchange_doAgain();
			break;
			
			case 'M'://Move (Ajouter//retirer un objet a l'échange)
				Exchange_onMoveItem(packet);
			break;
			
			case 'r'://Rides => Monture
				Exchange_mountPark(packet);
			break;
			
			case 'R'://liste d'achat NPC
				Exchange_start(packet);
			break;
			case 'S'://Vente
				Exchange_onSellItem(packet);
			break;
			
			case 'V'://Fin de l'échange
				Exchange_finish_buy(packet);
			break;
		}
	}
	
	private void Exchange_BuySystem(String packet)
	{
		HDV.HdvVar(_perso, _perso.get_curCarte().get_id());
		switch(packet.charAt(2))
		{
		case 'B':
			int itemguid = 0, qua = 0, price = 0;
			String[] infos = packet.substring(3).split("\\|");
			try
			{
				itemguid = Integer.parseInt(infos[0]);
				qua = Integer.parseInt(infos[1]);
				price = Integer.parseInt(infos[2]);
			}catch(Exception e){}
			HDV.FinalizeBuy(_perso, itemguid, qua, price, HDV._HdvType);
		break;
		case 'P':
			int itemid = Integer.parseInt(packet.substring(3));
			HDV.MiddlePrice(_perso, itemid, HDV._HdvType);
			break;
		case 'T':
			int catid = Integer.parseInt(packet.substring(3));
			HDV.LoadBuy(_perso, HDV._HdvType, catid);
		break;
		case 'l':
			int templateitem = Integer.parseInt(packet.substring(3));
			HDV.LoadBuyItem(_perso, templateitem, HDV._HdvType);
		break;
		case 'S':
			int Template_ID = 0;
			String[] Search_Packet = packet.substring(3).split("\\|");
			try
			{
				Template_ID = Integer.parseInt(Search_Packet[1]);
			}catch(Exception e){}
			HDV.LoadBuyItem(_perso, Template_ID, HDV._HdvType);
		break;
		}
		
	}
	
	private void Exchange_mountPark(String packet)
	{
		//Si dans un enclos
		if(_perso.getInMountPark() != null)
		{
			char c = packet.charAt(2);
			packet = packet.substring(3);
			int guid = -1;
			try
			{
				guid = Integer.parseInt(packet);
			}catch(Exception e){};
			switch(c)
			{
				case 'C'://Parcho => Etable (Stocker)
					if(guid == -1 || !_perso.hasItemGuid(guid))return;
					Objet obj = World.getObjet(guid);
					
					//on prend la DD demandée
					int DDid = obj.getStats().getEffect(995);
					Dragodinde DD = World.getDragoByID(DDid);
					//FIXME mettre return au if pour ne pas créer des nouvelles dindes
					if(DD == null)
					{
						int color = Constants.getMountColorByParchoTemplate(obj.getTemplate().getID());
						if(color <1)return;
						DD = new Dragodinde(color);
					}
					
					//On enleve l'objet du Monde et du Perso
					_perso.removeItem(guid);
					World.removeItem(guid);
					//on ajoute la dinde a l'étable
					_compte.getStable().add(DD);
					
					//On envoie les packet
					SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(_perso,obj.getGuid());
					SocketManager.GAME_SEND_Ee_PACKET(_perso, '+', DD.parse());
				break;
				case 'c'://Etable => Parcho(Echanger)
					Dragodinde DD1 = World.getDragoByID(guid);
					//S'il n'a pas la dinde
					if(!_compte.getStable().contains(DD1) || DD1 == null)return;
					//on retire la dinde de l'étable
					_compte.getStable().remove(DD1);
					
					//On créer le parcho
					ObjTemplate T = Constants.getParchoTemplateByMountColor(DD1.get_color());
					Objet obj1 = T.createNewItem(1, false);
					//On efface les stats
					obj1.clearStats();
					//on ajoute la possibilité de voir la dinde
					obj1.getStats().addOneStat(995, DD1.get_id());
					obj1.addTxtStat(996, _perso.get_name());
					obj1.addTxtStat(997, DD1.get_nom());
					
					//On ajoute l'objet au joueur
					World.addObjet(obj1, true);
					_perso.addObjet(obj1, false);//Ne seras jamais identique de toute
					
					//Packets
					SocketManager.GAME_SEND_Ow_PACKET(_perso);
					SocketManager.GAME_SEND_Ee_PACKET(_perso,'-',DD1.get_id()+"");
				break;
				case 'g'://Equiper
					Dragodinde DD3 = World.getDragoByID(guid);
					//S'il n'a pas la dinde
					if(!_compte.getStable().contains(DD3) || DD3 == null || _perso.getMount() != null)return;
					
					_compte.getStable().remove(DD3);
					_perso.setMount(DD3);
					
					//Packets
					SocketManager.GAME_SEND_Re_PACKET(_perso, "+", DD3);
					SocketManager.GAME_SEND_Ee_PACKET(_perso,'-',DD3.get_id()+"");
					SocketManager.GAME_SEND_Rx_PACKET(_perso);
				break;
				case 'p'://Equipé => Stocker
					//Si c'est la dinde équipé
					if(_perso.getMount()!=null?_perso.getMount().get_id() == guid:false)
					{
						//Si le perso est sur la monture on le fait descendre
						if(_perso.isOnMount())_perso.toogleOnMount();
						//Si ca n'a pas réussie, on s'arrete là (Items dans le sac ?)
						if(_perso.isOnMount())return;
						
						Dragodinde DD2 = _perso.getMount();
						_compte.getStable().add(DD2);
						_perso.setMount(null);
						
						//Packets
						SocketManager.GAME_SEND_Ee_PACKET(_perso,'+',DD2.parse());
						SocketManager.GAME_SEND_Re_PACKET(_perso, "-", null);
						SocketManager.GAME_SEND_Rx_PACKET(_perso);
					}else//Sinon...
					{
						
					}
				break;
			}
		}
	}

	private void Exchange_doAgain()
	{
		if(_perso.getCurJobAction() != null)
			_perso.getCurJobAction().putLastCraftIngredients();
	}

	private void Exchange_isOK()
	{
		if(_perso.getCurJobAction() != null)
		{
			//Si pas action de craft, on s'arrete la
			if(!_perso.getCurJobAction().isCraft())return;
			_perso.getCurJobAction().startCraft(_perso);
		}
		if(_perso.get_curExchange() == null)return;
		_perso.get_curExchange().toogleOK(_perso.get_GUID());
	}

	private void Exchange_onMoveItem(String packet)
	{
		//Metier
		if(HDV._isHdv)
		{
			if(packet.charAt(2) == 'O')//Ajout d'objet
			{
				if(packet.charAt(3) == '+')
				{
					//FIXME gerer les packets du genre  EMO+173|5+171|5+172|5 (split sur '+' ?:/)
					String[] infos = packet.substring(4).split("\\|");
					try
					{
						int guid = Integer.parseInt(infos[0]);
						int qua  = Integer.parseInt(infos[1]);
						int price  = Integer.parseInt(infos[2]);
						if(!_perso.hasItemGuid(guid))return;
						Objet obj = World.getObjet(guid);
						if(obj == null)return;
						if(obj.getQuantity()<qua) qua = obj.getQuantity();
						
						HDV.HdvVar(_perso, _perso.get_curCarte().get_id());
						HDV.MakeSell(_perso, HDV._HdvType, qua, price, guid);
					}catch(NumberFormatException e){};
				}else
				{
					String[] infos = packet.substring(4).split("\\|");
					try
					{
						int guid = Integer.parseInt(infos[0]);
						SQLManager.LOAD_ITEMS(guid+"");
						Objet obj = World.getObjet(guid);
						if(obj == null)return;
						
						HDV.HdvVar(_perso, _perso.get_curCarte().get_id());
						HDV.UnMakeSell(_perso, HDV._HdvType, guid);
						
					}catch(NumberFormatException e){};
				}
			
			}
		}
		if(_perso.getCurJobAction() != null)
		{
			//Si pas action de craft, on s'arrete la
			if(!_perso.getCurJobAction().isCraft())return;
			if(packet.charAt(2) == 'O')//Ajout d'objet
			{
				if(packet.charAt(3) == '+')
				{
					//FIXME gerer les packets du genre  EMO+173|5+171|5+172|5 (split sur '+' ?:/)
					String[] infos = packet.substring(4).split("\\|");
					try
					{
						int guid = Integer.parseInt(infos[0]);
						int qua  = Integer.parseInt(infos[1]);
						if(qua <= 0)return;
						if(!_perso.hasItemGuid(guid))return;
						Objet obj = World.getObjet(guid);
						if(obj == null)return;
						if(obj.getQuantity()<qua)
							qua = obj.getQuantity();
							_perso.getCurJobAction().modifIngredient(_perso,guid,qua);
					}catch(NumberFormatException e){};
				}else
				{
					String[] infos = packet.substring(4).split("\\|");
					try
					{
						int guid = Integer.parseInt(infos[0]);
						int qua  = Integer.parseInt(infos[1]);
						if(qua <= 0)return;
						Objet obj = World.getObjet(guid);
						if(obj == null)return;
						_perso.getCurJobAction().modifIngredient(_perso,guid,-qua);
					}catch(NumberFormatException e){};
				}
				
			}else
			if(packet.charAt(2) == 'R')
			{
				try
				{
					int c = Integer.parseInt(packet.substring(3));
					_perso.getCurJobAction().repeat(c,_perso);
				}catch(Exception e){};
			}
			return;
		}
		//Banque
		if(_perso.isInBank())
		{
			if(_perso.get_curExchange() != null)return;
			switch(packet.charAt(2))
			{
				case 'G'://Kamas
					long kamas = 0;
					try
					{
							kamas = Integer.parseInt(packet.substring(3));
					}catch(Exception e){};
					if(kamas == 0)return;
					
					if(kamas > 0)//Si On ajoute des kamas a la banque
					{
						if(_perso.get_kamas() < kamas)kamas = _perso.get_kamas();
						_perso.setBankKamas(_perso.getBankKamas()+kamas);//On ajoute les kamas a la banque
						_perso.set_kamas(_perso.get_kamas()-kamas);//On retire les kamas du personnage
						SocketManager.GAME_SEND_STATS_PACKET(_perso);
						SocketManager.GAME_SEND_EsK_PACKET(_perso,"G"+_perso.getBankKamas());
					}else
					{
						kamas = -kamas;//On repasse en positif
						if(_perso.getBankKamas() < kamas)kamas = _perso.getBankKamas();
						_perso.setBankKamas(_perso.getBankKamas()-kamas);//On retire les kamas de la banque
						_perso.set_kamas(_perso.get_kamas()+kamas);//On ajoute les kamas du personnage
						SocketManager.GAME_SEND_STATS_PACKET(_perso);
						SocketManager.GAME_SEND_EsK_PACKET(_perso,"G"+_perso.getBankKamas());
					}
				break;
				
				case 'O'://Objet
					int guid = 0;
					int qua = 0;
					try
					{
						guid = Integer.parseInt(packet.substring(4).split("\\|")[0]);
						qua = Integer.parseInt(packet.substring(4).split("\\|")[1]);
					}catch(Exception e){};
					if(guid == 0 || qua == 0)return;
					
					switch(packet.charAt(3))
					{
						case '+'://Ajouter a la banque
							_perso.addInBank(guid,qua);
						break;
						
						case '-'://Retirer de la banque
							_perso.removeFromBank(guid,qua);
						break;
					}
				break;
			}
			return;
		}
		if(_perso.get_curExchange() == null)return;
		switch(packet.charAt(2))
		{
			case 'O'://Objet ?
				long m;
				if((m = System.currentTimeMillis() - _LastDateFonction) < 2000)
				{
					m = (2000  - m)/10;//On calcul la différence en secondes
					return;
				}else
				{
				_LastDateFonction = System.currentTimeMillis();
				if(packet.charAt(3) == '+')
				{
					String[] infos = packet.substring(4).split("\\|");
					try
					{
						
						int guid = Integer.parseInt(infos[0]);
						int qua  = Integer.parseInt(infos[1]);
						if(!_perso.hasItemGuid(guid))return;
						Objet obj = World.getObjet(guid);
						if(obj == null)return;
						if(obj.getQuantity()<qua)
						{
							qua = obj.getQuantity();
						}
						_perso.get_curExchange().addItem(guid,qua,_perso.get_GUID());
					}catch(NumberFormatException e){};
				}else
				{
					String[] infos = packet.substring(4).split("\\|");
					try
					{
						int guid = Integer.parseInt(infos[0]);
						int qua  = Integer.parseInt(infos[1]);
						Objet obj = World.getObjet(guid);
						if(obj == null)return;
						_perso.get_curExchange().removeItem(guid,qua,_perso.get_GUID());
					}catch(NumberFormatException e){};
				}
				}
			break;
			
			case 'G'://Kamas
				try
				{
					long numb = Integer.parseInt(packet.substring(3));
					if(_perso.get_kamas() < numb)
						numb = _perso.get_kamas();
					_perso.get_curExchange().setKamas(_perso.get_GUID(), numb);
				}catch(NumberFormatException e){};
			break;
		}
	}

	private void Exchange_accept()
	{
		if(_perso.get_isTradingWith() == 0)return;
		Personnage target = World.getPersonnage(_perso.get_isTradingWith());
		if(target == null)return;
		SocketManager.GAME_SEND_EXCHANGE_CONFIRM_OK(_out,1);
		SocketManager.GAME_SEND_EXCHANGE_CONFIRM_OK(target.get_compte().getGameThread().get_out(),1);
		World.Exchange echg = new World.Exchange(target,_perso);
		_perso.setCurExchange(echg);
		_perso.set_isTradingWith(target.get_GUID());
		target.setCurExchange(echg);
		target.set_isTradingWith(_perso.get_GUID());
	}

	private void Exchange_onSellItem(String packet)
	{
		try
		{
			String[] infos = packet.substring(2).split("\\|");
			int guid = Integer.parseInt(infos[0]);
			int qua = Integer.parseInt(infos[1]);
			if(!_perso.hasItemGuid(guid))
			{
				SocketManager.GAME_SEND_SELL_ERROR_PACKET(_out);
				return;
			}
			_perso.sellItem(guid, qua);
		}catch(Exception e)
		{
			SocketManager.GAME_SEND_SELL_ERROR_PACKET(_out);
		}
	}

	private void Exchange_onBuyItem(String packet)
	{
		String[] infos = packet.substring(2).split("\\|");
		try
		{
			int tempID = Integer.parseInt(infos[0]);
			int qua = Integer.parseInt(infos[1]);
			
			if(qua <= 0) return;
			
			ObjTemplate template = World.getObjTemplate(tempID);
			if(template == null)//Si l'objet demandé n'existe pas(ne devrait pas arrivé)
			{
				GameServer.addToLog(_perso.get_name()+" tente d'acheter l'itemTemplate "+tempID+" qui est inexistant");
				SocketManager.GAME_SEND_BUY_ERROR_PACKET(_out);
				return;
			}
			if(!_perso.get_curCarte().getNPC(_perso.get_isTradingWith()).get_template().haveItem(tempID))//Si le PNJ ne vend pas l'objet voulue
			{
				GameServer.addToLog(_perso.get_name()+" tente d'acheter l'itemTemplate "+tempID+" que le présent PNJ ne vend pas");
				SocketManager.GAME_SEND_BUY_ERROR_PACKET(_out);
				return;
			}
			int prix = template.getPrix() * qua;
			if(_perso.get_kamas()<prix)//Si le joueur n'a pas assez de kamas
			{
				GameServer.addToLog(_perso.get_name()+" tente d'acheter l'itemTemplate "+tempID+" mais n'a pas l'argent nécessaire");
				SocketManager.GAME_SEND_BUY_ERROR_PACKET(_out);
				return;
			}
			Objet newObj = template.createNewItem(qua,false);
			long newKamas = _perso.get_kamas() - prix;
			_perso.set_kamas(newKamas);
			if(_perso.addObjet(newObj,true))//Return TRUE si c'est un nouvel item
				World.addObjet(newObj,true);
			SocketManager.GAME_SEND_BUY_OK_PACKET(_out);
			SocketManager.GAME_SEND_STATS_PACKET(_perso);
			SocketManager.GAME_SEND_Ow_PACKET(_perso);
		}catch(Exception e)
		{
			e.printStackTrace();
			SocketManager.GAME_SEND_BUY_ERROR_PACKET(_out);
			return;
		};
	}

	private void Exchange_finish_buy(String packet)
	{
		if(_perso.get_curExchange() == null &&
				!_perso.isInBank() &&
				_perso.getCurJobAction() == null &&
				_perso.get_isTradingWith() == 0 &&
				_perso.getInMountPark() == null)return;
		//Si échange avec un personnage
		if(	_perso.get_curExchange() != null)
		{
			_perso.get_curExchange().cancel();
			_perso.set_isTradingWith(0);
			_perso.set_away(false);
			return;
		}
		//Si métier
		if(_perso.getCurJobAction() != null)
		{
			_perso.getCurJobAction().resetCraft();
		}
		//Si dans un enclos
		if(_perso.getInMountPark() != null)_perso.leftMountPark();
		//prop d'echange avec un joueur
		if(_perso.get_isTradingWith() > 0)
		{
			Personnage p = World.getPersonnage(_perso.get_isTradingWith());
			if(p != null)
			{
				if(p.isOnline())
				{
					PrintWriter out = p.get_compte().getGameThread().get_out();
					SocketManager.GAME_SEND_EV_PACKET(out);
					p.set_isTradingWith(0);
				}
			}
		}
		if(_perso.get_isTradingWith() == -1)
		{
			HDV._isHdv = false;
		}
		
		SQLManager.SAVE_PERSONNAGE(_perso,true);
		SocketManager.GAME_SEND_EV_PACKET(_out);
		_perso.set_isTradingWith(0);
		_perso.set_away(false);
		//Sauvagarde du perso et des item aprés.
		_perso.setInBank(false);
	}

	private void Exchange_start(String packet)
	{
		switch(packet.charAt(2))
		{
			case '0'://Si NPC
				try
				{
					int npcID = Integer.parseInt(packet.substring(4));
					NPC_tmpl.NPC npc = _perso.get_curCarte().getNPC(npcID);
					if(npc == null)return;
					SocketManager.GAME_SEND_ECK_PACKET(_out, 0, npcID+"");
					SocketManager.GAME_SEND_ITEM_VENDOR_LIST_PACKET(_out,npc);
					_perso.set_isTradingWith(npcID);
				}catch(NumberFormatException e){};
			break;
			case '1'://Si joueur
				Exchange_more(packet);
			break;
		}
	}
	
	private void Exchange_more(String packet)
	{
		switch(packet.charAt(3))
		{
		case '0':
			if(HDV._isHdv)
			{
				SocketManager.GAME_SEND_EV_PACKET(_out);
			}
			HDV._isHdv = true;
			HDV.HdvVar(_perso, _perso.get_curCarte().get_id());
			HDV.StartSellHdv(_perso, HDV._HdvType);
		break;
		case '1':
			if(HDV._isHdv)
			{
				SocketManager.GAME_SEND_EV_PACKET(_out);
			}
			HDV._isHdv = true;
			HDV.HdvVar(_perso, _perso.get_curCarte().get_id());
			HDV.StartBuyHdv(_perso, HDV._HdvType);
		break;
		default:
			try
			{
			int guidTarget = Integer.parseInt(packet.substring(4));
			Personnage target = World.getPersonnage(guidTarget);
			if(target == null )
			{
				SocketManager.GAME_SEND_EXCHANGE_REQUEST_ERROR(_out,'E');
				return;
			}
			if(target.get_curCarte()!= _perso.get_curCarte() || !target.isOnline())//Si les persos ne sont pas sur la meme map
			{
				SocketManager.GAME_SEND_EXCHANGE_REQUEST_ERROR(_out,'E');
				return;
			}
			if(target.is_away() || _perso.is_away() || target.get_isTradingWith() != 0)
			{
				SocketManager.GAME_SEND_EXCHANGE_REQUEST_ERROR(_out,'O');
				return;
			}
			SocketManager.GAME_SEND_EXCHANGE_REQUEST_OK(_out, _perso.get_GUID(), guidTarget,1);
			SocketManager.GAME_SEND_EXCHANGE_REQUEST_OK(target.get_compte().getGameThread().get_out(),_perso.get_GUID(), guidTarget,1);
			_perso.set_isTradingWith(guidTarget);
			target.set_isTradingWith(_perso.get_GUID());
		}catch(NumberFormatException e){}
		break;
		}
	}

	private void parse_environementPacket(String packet)
	{
		switch(packet.charAt(1))
		{
			case 'D'://Change direction
				Environement_change_direction(packet);
			break;
			
			case 'U'://Emote
				Environement_emote(packet);
			break;
		}
	}

	private void Environement_emote(String packet)
	{
		int emote = -1;
		try
		{
			emote = Integer.parseInt(packet.substring(2));
		}catch(Exception e){};
		if(emote == -1)return;
		if(_perso == null)return;
		if(_perso.get_fight() != null)return;//Pas d'émote en combat
		
		switch(emote)//effets spéciaux des émotes
		{
			case 19://s'allonger 
			case 1:// s'asseoir
				_perso.setSitted(!_perso.isSitted());
			break;
		}
		if(_perso.emoteActive() == emote)_perso.setEmoteActive(0);
		else _perso.setEmoteActive(emote);
		
		System.out.println("Set Emote "+_perso.emoteActive());
		System.out.println("Is sitted "+_perso.isSitted());
		
		SocketManager.GAME_SEND_eUK_PACKET_TO_MAP(_perso.get_curCarte(), _perso.get_GUID(), _perso.emoteActive());
	}

	private void Environement_change_direction(String packet)
	{
		try
		{
			if(_perso.get_fight() != null)return;
			int dir = Integer.parseInt(packet.substring(2));
			_perso.set_orientation(dir);
			SocketManager.GAME_SEND_eD_PACKET_TO_MAP(_perso.get_curCarte(),_perso.get_GUID(),dir);
		}catch(NumberFormatException e){return;};
	}

	private void parseSpellPacket(String packet)
	{
		switch(packet.charAt(1))
		{
			case 'B':
				boostSort(packet);
			break;
			case 'F'://Oublie de sort
				forgetSpell(packet);
			break;
			case'M':
				addToSpellBook(packet);
			break;
		}
	}

	private void addToSpellBook(String packet)
	{
		try
		{
			int SpellID = Integer.parseInt(packet.substring(2).split("\\|")[0]);
			int Position = Integer.parseInt(packet.substring(2).split("\\|")[1]);
			SortStats Spell = _perso.getSortStatBySortIfHas(SpellID);
			
			if(Spell != null)
			{
				_perso.set_SpellPlace(SpellID, CryptManager.getHashedValueByInt(Position));
			}
				
			SocketManager.GAME_SEND_BN(_out);
		}catch(Exception e){};
	}

	private void boostSort(String packet)
	{
		try
		{
			int id = Integer.parseInt(packet.substring(2));
			GameServer.addToLog("Info: "+_perso.get_name()+": Tente BOOST sort id="+id);
			if(_perso.boostSpell(id))
			{
				GameServer.addToLog("Info: "+_perso.get_name()+": OK pour BOOST sort id="+id);
				SocketManager.GAME_SEND_SPELL_UPGRADE_SUCCED(_out, id, _perso.getSortStatBySortIfHas(id).getLevel());
				SocketManager.GAME_SEND_STATS_PACKET(_perso);
			}else
			{
				GameServer.addToLog("Info: "+_perso.get_name()+": Echec BOOST sort id="+id);
				SocketManager.GAME_SEND_SPELL_UPGRADE_FAILED(_out);
				return;
			}
		}catch(NumberFormatException e){SocketManager.GAME_SEND_SPELL_UPGRADE_FAILED(_out);return;};
	}

	private void forgetSpell(String packet)
	{
		if(!_perso.isForgetingSpell())return;
		
		int id = Integer.parseInt(packet.substring(2));
		
		if(Ancestra.CONFIG_DEBUG) GameServer.addToLog("Info: "+_perso.get_name()+": Tente Oublie sort id="+id);
		
		if(_perso.forgetSpell(id))
		{
			if(Ancestra.CONFIG_DEBUG) GameServer.addToLog("Info: "+_perso.get_name()+": OK pour Oublie sort id="+id);
			SocketManager.GAME_SEND_SPELL_UPGRADE_SUCCED(_out, id, _perso.getSortStatBySortIfHas(id).getLevel());
			SocketManager.GAME_SEND_STATS_PACKET(_perso);
			_perso.setisForgetingSpell(false);
		}
	}

	private void parseFightPacket(String packet)
	{
		try
		{
			switch(packet.charAt(1))
			{
				case 'D'://Détails d'un combat (liste des combats)
					int key = -1;
					try
					{
						key = Integer.parseInt(packet.substring(2).replace(((int)0x0)+"", ""));
					}catch(Exception e){};
					if(key == -1)return;
					SocketManager.GAME_SEND_FIGHT_DETAILS(_out,_perso.get_curCarte().get_fights().get(key));
				break;
				
				case 'H'://Aide
					if(_perso.get_fight() == null)return;
					_perso.get_fight().toggleHelp(_perso.get_GUID());
				break;
				
				case 'L'://Lister les combats
					SocketManager.GAME_SEND_FIGHT_LIST_PACKET(_out, _perso.get_curCarte());
				break;
				case 'N'://Bloquer le combat
					if(_perso.get_fight() == null)return;
					_perso.get_fight().toggleLockTeam(_perso.get_GUID());
				break;
				case 'P'://Seulement le groupe
					if(_perso.get_fight() == null || _perso.getGroup() == null)return;
					_perso.get_fight().toggleOnlyGroup(_perso.get_GUID());
				break;
				case 'S'://Bloquer les specs
					if(_perso.get_fight() == null)return;
					_perso.get_fight().toggleLockSpec(_perso.get_GUID());
				break;
				
			}
		}catch(Exception e){e.printStackTrace();};
	}

	private void parseBasicsPacket(String packet)
	{
		switch(packet.charAt(1))
		{
			case 'A'://Console
				Basic_console(packet);
			break;
			case 'D':
				Basic_send_Date_Hour();
			break;
			case 'M':
				Basic_chatMessage(packet);
			break;
			case 'W':
				Basic_infosmessage(packet);
			break;
			case 'S':
				_perso.emoticone(packet.substring(2));
			break;
			case 'Y':
				Basic_state(packet);
			break;
		}
	}
	public void Basic_state(String packet)
	{
		switch(packet.charAt(2))
		{
			case 'A': //Absent
				if(_perso.isAbsent)
				{

					SocketManager.GAME_SEND_Im_PACKET(_perso, "038");

					_perso.isAbsent = false;
				}
				else

				{
					SocketManager.GAME_SEND_Im_PACKET(_perso, "037");
					_perso.isAbsent = true;
				}
			break;
			case 'I': //Invisible
				if(_perso.isInvisible)
				{
					SocketManager.GAME_SEND_Im_PACKET(_perso, "051");
					_perso.isInvisible = false;
				}
				else
				{
					SocketManager.GAME_SEND_Im_PACKET(_perso, "050");
					_perso.isInvisible = true;
				}
			break;
		}
	}
	
	public Personnage getPerso()
	{
		return _perso;
	}
	
	private void Basic_console(String packet)
	{
		if(_compte.get_gmLvl() == 0)
		{
			closeSocket();
			return;
		}
		String msg = packet.substring(2);
		String[] infos = msg.split(" ");
		if(infos.length == 0)return;
		String command = infos[0];
		if(Ancestra.canLog)
		{
			Ancestra.addToMjLog(_compte.get_curIP()+": "+_compte.get_name()+" "+_perso.get_name()+"=>"+msg);
		}
		if(command.equalsIgnoreCase("EXIT"))
		{
			if(_compte.get_gmLvl() < 3)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous n'avez pas le niveau MJ requis");
				return;
			}
			System.exit(0);
		}else
		if(command.equalsIgnoreCase("INFOS"))
		{
			long uptime = System.currentTimeMillis() - Ancestra.gameServer.getStartTime();
			int jour = (int) (uptime/(1000*3600*24));
			uptime %= (1000*3600*24);
			int hour = (int) (uptime/(1000*3600));
			uptime %= (1000*3600);
			int min = (int) (uptime/(1000*60));
			uptime %= (1000*60);
			int sec = (int) (uptime/(1000));
			
			String mess =	"===========\n"
				+       	"Ancestra-R v. "+Constants.SERVER_VERSION+" par "+Constants.SERVER_MAKER+"\n"
				+			"\n"
				+			"Uptime: "+jour+"j "+hour+"h "+min+"m "+sec+"s\n"
				+			"Joueurs en lignes: "+Ancestra.gameServer.getPlayerNumber()+"\n"
				+			"Record de connexion: "+Ancestra.gameServer.getMaxPlayer()+"\n"
				+			"===========";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
			return;
		}else
		if(command.equalsIgnoreCase("REFRESHMOBS"))
		{
			_perso.get_curCarte().refreshSpawns();
			String mess = "Mob Spawn refreshed!";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
			return;
		}
		else
		if(command.equalsIgnoreCase("SAVE") && !Ancestra.isSaving)
		{
			if(_compte.get_gmLvl() < 3)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous n'avez pas le niveau MJ requis");
				return;
			}
			Thread t = new Thread(new SaveThread());
			t.start();
			String mess = "Sauvegarde lancee!";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
			return;
		}
		else
		if(command.equalsIgnoreCase("MAPINFO"))
		{
			String mess = 	"==========\n"
						+	"Liste des Npcs de la carte:";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
			Carte map = _perso.get_curCarte();
			for(Entry<Integer,NPC> entry : map.get_npcs().entrySet())
			{
				mess = entry.getKey()+" "+entry.getValue().get_template().get_id()+" "+entry.getValue().get_cellID()+" "+entry.getValue().get_template().get_initQuestionID();
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
			}
			mess = "Liste des groupes de monstres:";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
			for(Entry<Integer,MobGroup> entry : map.getMobGroups().entrySet())
			{
				mess = entry.getKey()+" "+entry.getValue().getCellID()+" "+entry.getValue().getAlignement()+" "+entry.getValue().getSize();
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
			}
			mess = "==========";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
			return;
		}
		else
		if(command.equalsIgnoreCase("WHO"))
		{
			String mess = 	"==========\n"
				+			"Liste des joueurs en ligne:";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
			int diff = Ancestra.gameServer.getClients().size() -  30;
			for(byte b = 0; b < 30; b++)
			{
				if(b == Ancestra.gameServer.getClients().size())break;
				GameThread GT = Ancestra.gameServer.getClients().get(b);
				Personnage P = GT.getPerso();
				if(P == null)continue;
				mess = P.get_name()+"("+P.get_GUID()+") ";
				
				switch(P.get_classe())
				{
					case Constants.CLASS_FECA:
						mess += "Fec";
					break;
					case Constants.CLASS_OSAMODAS:
						mess += "Osa";
					break;
					case Constants.CLASS_ENUTROF:
						mess += "Enu";
					break;
					case Constants.CLASS_SRAM:
						mess += "Sra";
					break;
					case Constants.CLASS_XELOR:
						mess += "Xel";
					break;
					case Constants.CLASS_ECAFLIP:
						mess += "Eca";
					break;
					case Constants.CLASS_ENIRIPSA:
						mess += "Eni";
					break;
					case Constants.CLASS_IOP:
						mess += "Iop";
					break;
					case Constants.CLASS_CRA:
						mess += "Cra";
					break;
					case Constants.CLASS_SADIDA:
						mess += "Sad";
					break;
					case Constants.CLASS_SACRIEUR:
						mess += "Sac";
					break;
					case Constants.CLASS_PANDAWA:
						mess += "Pan";
					break;
					default:
						mess += "Unk";
				}
				mess += " ";
				mess += (P.get_sexe()==0?"M":"F")+" ";
				mess += P.get_lvl()+" ";
				mess += P.get_curCarte().get_id()+"("+P.get_curCarte().getX()+"/"+P.get_curCarte().getY()+") ";
				mess += P.get_fight()==null?"":"Combat ";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
			}
			if(diff >0)
			{
				mess = 	"Et "+diff+" autres personnages";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
			}
			mess = 	"==========\n";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
			return;
		}
		else
		if(command.equalsIgnoreCase("SHOWFIGHTPOS"))
		{
			String mess = "Liste des StartCell [teamID][cellID]:";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
			String places = _perso.get_curCarte().get_placesStr();
			if(places.indexOf('|') == -1 || places.length() <2)
			{
				mess = "Les places n'ont pas ete definies";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
				return;
			}
			String team0 = "",team1 = "";
			String[] p = places.split("\\|");
			try
			{
				team0 = p[0];
			}catch(Exception e){};
			try
			{
				team1 = p[1];
			}catch(Exception e){};
			mess = "Team 0:\n";
			for(int a = 0;a <= team0.length()-2; a+=2)
			{
				String code = team0.substring(a,a+2);
				mess += CryptManager.cellCode_To_ID(code);
			}
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
			mess = "Team 1:\n";
			for(int a = 0;a <= team1.length()-2; a+=2)
			{
				String code = team1.substring(a,a+2);
				mess += CryptManager.cellCode_To_ID(code)+" , ";
			}
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, mess);
			return;
		}else
		if(command.equalsIgnoreCase("DELFIGHTPOS"))
		{
			if(_compte.get_gmLvl() < 3)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous n'avez pas le niveau MJ requis");
				return;
			}
			int cell = -1;
			try
			{
				cell = Integer.parseInt(infos[2]);
			}catch(Exception e){};
			if(cell < 0 || _perso.get_curCarte().getCase(cell) == null)
			{
				cell = _perso.get_curCell().getID();
			}
			String places = _perso.get_curCarte().get_placesStr();
			String[] p = places.split("\\|");
			String newPlaces = "";
			String team0 = "",team1 = "";
			try
			{
				team0 = p[0];
			}catch(Exception e){};
			try
			{
				team1 = p[1];
			}catch(Exception e){};
			
			for(int a = 0;a<=team0.length()-2;a+=2)
			{
				String c = p[0].substring(a,a+2);
				if(cell == CryptManager.cellCode_To_ID(c))continue;
				newPlaces += c;
			}
			newPlaces += "|";
			for(int a = 0;a<=team1.length()-2;a+=2)
			{
				String c = p[1].substring(a,a+2);
				if(cell == CryptManager.cellCode_To_ID(c))continue;
				newPlaces += c;
			}
			_perso.get_curCarte().setPlaces(newPlaces);
			if(!SQLManager.SAVE_MAP_DATA(_perso.get_curCarte()))return;
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,"Les places ont ete modifiees ("+newPlaces+")");
			return;
		}
		else
		if(command.equalsIgnoreCase("CREATEGUILD"))
		{
			Personnage perso = _perso;
			if(infos.length >1)
			{
				perso = World.getPersoByName(infos[1]);
			}
			if(perso == null)
			{
				String mess = "Le personnage n'existe pas.";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
				return;
			}
			
			if(!perso.isOnline())
			{
				String mess = "Le personnage "+perso.get_name()+" n'etait pas connecte";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
				return;
			}
			if(perso.get_guild() != null || perso.getGuildMember() != null)
			{
				String mess = "Le personnage "+perso.get_name()+" a deja une guilde";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
				return;
			}
			SocketManager.GAME_SEND_gn_PACKET(perso);
			String mess = perso.get_name()+": Panneau de creation de guilde ouvert";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
			return;
		}
		else
		if(command.equalsIgnoreCase("TOOGLEAGGRO"))
		{
			Personnage perso = _perso;
			String name = infos[1];
			
			perso = World.getPersoByName(name);
			if(perso == null)
			{
				String mess = "Le personnage n'existe pas.";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
				return;
			}
			
			perso.set_canAggro(!perso.canAggro());
			String mess = perso.get_name();
			if(perso.canAggro()) mess += " peut maintenant etre aggresser";
			else mess += " ne peut plus etre agresser";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
			
			if(!perso.isOnline())
			{
				mess = "(Le personnage "+perso.get_name()+" n'etait pas connecte)";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
			}
		}
		else
		if(infos.length <2)
		{
			String mess = "Commande non reconnue ou incomplete";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
			return;
		}
		//Commandes avec 1 argument
		infos = msg.split(" ");

		if(command.equalsIgnoreCase("ANNOUNCE"))
		{
			infos = msg.split(" ",2);
			SocketManager.GAME_SEND_MESSAGE_TO_ALL(infos[1], Ancestra.CONFIG_MOTD_COLOR);
			return;
		}
		else
		if(command.equalsIgnoreCase("NAMEANNOUNCE"))
		{
			infos = msg.split(" ",2);
			String prefix = "["+_perso.get_name()+"]";
			SocketManager.GAME_SEND_MESSAGE_TO_ALL(prefix+infos[1], Ancestra.CONFIG_MOTD_COLOR);
			return;
		}
		else
		if(command.equalsIgnoreCase("BAN"))
		{
			if(_compte.get_gmLvl() < 3)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous n'avez pas le niveau MJ requis");
				return;
			}
			Personnage P = World.getPersoByName(infos[1]);
			if(P == null)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Personnage non trouve");
				return;
			}
			if(P.get_compte() == null)SQLManager.LOAD_ACCOUNT_BY_GUID(P.getAccID());
			if(P.get_compte() == null)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Erreur");
				return;
			}
			P.get_compte().setBanned(true);
			SQLManager.UPDATE_ACCOUNT_DATA(P.get_compte());
			if(P.get_compte().getGameThread() == null)P.get_compte().getGameThread().kick();
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous avez banni "+P.get_name());
			return;
		}
		else
		if(command.equalsIgnoreCase("UNBAN"))
		{
			if(_compte.get_gmLvl() < 3)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous n'avez pas le niveau MJ requis");
				return;
			}
			Personnage P = World.getPersoByName(infos[1]);
			if(P == null)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Personnage non trouve");
				return;
			}
			if(P.get_compte() == null)SQLManager.LOAD_ACCOUNT_BY_GUID(P.getAccID());
			if(P.get_compte() == null)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Erreur");
				return;
			}
			P.get_compte().setBanned(false);
			SQLManager.UPDATE_ACCOUNT_DATA(P.get_compte());
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous avez debanni "+P.get_name());
			return;
		}
		else
		if(command.equalsIgnoreCase("ADDFIGHTPOS"))
		{
			if(_compte.get_gmLvl() < 3)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous n'avez pas le niveau MJ requis");
				return;
			}
			int team = -1;
			int cell = -1;
			try
			{
				team = Integer.parseInt(infos[1]);
				cell = Integer.parseInt(infos[2]);
			}catch(Exception e){};
			if( team < 0 || team>1)
			{
				String str = "Team ou cellID incorects";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			if(cell <0 || _perso.get_curCarte().getCase(cell) == null || !_perso.get_curCarte().getCase(cell).isWalkable(true))
			{
				cell = _perso.get_curCell().getID();
			}
			String places = _perso.get_curCarte().get_placesStr();
			String[] p = places.split("\\|");
			boolean already = false;
			String team0 = "",team1 = "";
			try
			{
				team0 = p[0];
			}catch(Exception e){};
			try
			{
				team1 = p[1];
			}catch(Exception e){};
			
			//Si case déjà utilisée
			System.out.println("0 => "+team0+"\n1 =>"+team1+"\nCell: "+CryptManager.cellID_To_Code(cell));
			for(int a = 0; a <= team0.length()-2;a+=2)if(cell == CryptManager.cellCode_To_ID(team0.substring(a,a+2)))already = true;
			for(int a = 0; a <= team1.length()-2;a+=2)if(cell == CryptManager.cellCode_To_ID(team1.substring(a,a+2)))already = true;
			if(already)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,"La case est deja dans la liste");
				return;
			}
			if(team == 0)team0 += CryptManager.cellID_To_Code(cell);
			else if(team == 1)team1 += CryptManager.cellID_To_Code(cell);
			
			String newPlaces = team0+"|"+team1;
			
			_perso.get_curCarte().setPlaces(newPlaces);
			if(!SQLManager.SAVE_MAP_DATA(_perso.get_curCarte()))return;
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,"Les places ont ete modifiees ("+newPlaces+")");
			return;
		}
		else
		if(command.equalsIgnoreCase("SETMAXGROUP"))
		{
			if(_compte.get_gmLvl() < 3)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous n'avez pas le niveau MJ requis");
				return;
			}
			infos = msg.split(" ",4);
			byte id = -1;
			try
			{
				id = Byte.parseByte(infos[1]);
			}catch(Exception e){};
			if(id == -1)
			{
				String str = "Valeur invalide";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			String mess = "Le nombre de groupe a ete fixe";
			_perso.get_curCarte().setMaxGroup(id);
			boolean ok = SQLManager.SAVE_MAP_DATA(_perso.get_curCarte());
			if(ok)mess += " et a ete sauvegarder a la BDD";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
		}else
		if(command.equalsIgnoreCase("ADDREPONSEACTION"))
		{
			if(_compte.get_gmLvl() < 3)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous n'avez pas le niveau MJ requis");
				return;
			}
			infos = msg.split(" ",4);
			int id = -30;
			int repID = 0;
			String args = infos[3];
			try
			{
				repID = Integer.parseInt(infos[1]);
				id = Integer.parseInt(infos[2]);
			}catch(Exception e){};
			NPC_reponse rep = World.getNPCreponse(repID);
			if(id == -30 || rep == null)
			{
				String str = "Au moins une des valeur est invalide";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			String mess = "L'action a ete ajoute";
			
			rep.addAction(new Action(id,args,""));
			boolean ok = SQLManager.ADD_REPONSEACTION(repID,id,args);
			if(ok)mess += " et ajoute a la BDD";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
		}else
		if(command.equalsIgnoreCase("SETINITQUESTION"))
		{
			if(_compte.get_gmLvl() < 3)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous n'avez pas le niveau MJ requis");
				return;
			}
			infos = msg.split(" ",4);
			int id = -30;
			int q = 0;
			try
			{
				q = Integer.parseInt(infos[2]);
				id = Integer.parseInt(infos[1]);
			}catch(Exception e){};
			if(id == -30)
			{
				String str = "NpcID invalide";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			String mess = "L'action a ete ajoute";
			NPC_tmpl npc = World.getNPCTemplate(id);
			
			npc.setInitQuestion(q);
			boolean ok = SQLManager.UPDATE_INITQUESTION(id,q);
			if(ok)mess += " et ajoute a la BDD";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
		}else
		if(command.equalsIgnoreCase("ADDENDFIGHTACTION"))
		{
			if(_compte.get_gmLvl() < 3)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous n'avez pas le niveau MJ requis");
				return;
			}
			infos = msg.split(" ",4);
			int id = -30;
			int type = 0;
			String args = infos[3];
			String cond = infos[4];
			try
			{
				type = Integer.parseInt(infos[1]);
				id = Integer.parseInt(infos[2]);
				
			}catch(Exception e){};
			if(id == -30)
			{
				String str = "Au moins une des valeur est invalide";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			String mess = "L'action a ete ajoute";
			_perso.get_curCarte().addEndFightAction(type, new Action(id,args,cond));
			boolean ok = SQLManager.ADD_ENDFIGHTACTION(_perso.get_curCarte().get_id(),type,id,args,cond);
			if(ok)mess += " et ajoute a la BDD";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
			return;
		}else
		if(command.equalsIgnoreCase("MUTE"))
		{
			if(_compte.get_gmLvl() < 1)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous n'avez pas le niveau MJ requis");
				return;
			}
			Personnage perso = _perso;
			String name = infos[1];
			int time = 0;
			try
			{
				time = Integer.parseInt(infos[2]);
			}catch(Exception e){};
			
			perso = World.getPersoByName(name);
			if(perso == null || time < 0)
			{
				String mess = "Le personnage n'existe pas ou la duree est invalide.";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
				return;
			}
			String mess = "Vous avez mute "+perso.get_name()+" pour "+time+" secondes";
			if(perso.get_compte() == null)
			{
				mess = "(Le personnage "+perso.get_name()+" n'etait pas connecte)";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
				return;
			}
			perso.get_compte().mute(true,time);
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
			
			if(!perso.isOnline())
			{
				mess = "(Le personnage "+perso.get_name()+" n'etait pas connecte)";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
			}else
			{
				SocketManager.GAME_SEND_Im_PACKET(perso, "1124;"+time);
			}
			return;
		}
		else
		if(command.equalsIgnoreCase("UNMUTE"))
		{
			Personnage perso = _perso;
			String name = infos[1];
			
			perso = World.getPersoByName(name);
			if(perso == null)
			{
				String mess = "Le personnage n'existe pas.";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
				return;
			}
			
			perso.get_compte().mute(false,0);
			String mess = "Vous avez unmute "+perso.get_name();
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
			
			if(!perso.isOnline())
			{
				mess = "(Le personnage "+perso.get_name()+" n'etait pas connecte)";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
			}
		}
		else
		if(command.equalsIgnoreCase("KICK"))
		{
			if(_compte.get_gmLvl() < 2)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous n'avez pas le niveau MJ requis");
				return;
			}
			Personnage perso = _perso;
			String name = infos[1];
			perso = World.getPersoByName(name);
			if(perso == null)
			{
				String mess = "Le personnage n'existe pas.";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
				return;
			}
			if(perso.isOnline())
			{
				perso.get_compte().getGameThread().kick();
				String mess = "Vous avez kick "+perso.get_name();
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
			}
			else
			{
				String mess = "Le personnage "+perso.get_name()+" n'est pas connecte";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
			}
		}
		else
		if(command.equalsIgnoreCase("SPELLPOINT"))
		{
			if(_compte.get_gmLvl() < 2)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous n'avez pas le niveau MJ requis");
				return;
			}
			int pts = -1;
			try
			{
				pts = Integer.parseInt(infos[1]);
			}catch(Exception e){};
			if(pts == -1)
			{
				String str = "Valeur invalide";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			Personnage target = _perso;
			if(infos.length > 2)//Si un nom de perso est spécifié
			{
				target = World.getPersoByName(infos[2]);
				if(target == null)
				{
					String str = "Le personnage n'a pas ete trouve";
					SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
					return;
				}
			}
			target.addSpellPoint(pts);
			SocketManager.GAME_SEND_STATS_PACKET(target);
			String str = "Le nombre de point de sort a ete modifiee";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
		}else
		if(command.equalsIgnoreCase("LEARNSPELL"))
		{
			if(_compte.get_gmLvl() < 2)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous n'avez pas le niveau MJ requis");
				return;
			}
			int spell = -1;
			try
			{
				spell = Integer.parseInt(infos[1]);
			}catch(Exception e){};
			if(spell == -1)
			{
				String str = "Valeur invalide";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			Personnage target = _perso;
			if(infos.length > 2)//Si un nom de perso est spécifié
			{
				target = World.getPersoByName(infos[2]);
				if(target == null)
				{
					String str = "Le personnage n'a pas ete trouve";
					SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
					return;
				}
			}
			
			target.learnSpell(spell, 1, true,true);
			
			String str = "Le sort a ete appris";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
		}else
		if(command.equalsIgnoreCase("SETALIGN"))
		{
			if(_compte.get_gmLvl() < 2)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous n'avez pas le niveau MJ requis");
				return;
			}
			byte align = -1;
			try
			{
				align = Byte.parseByte(infos[1]);
			}catch(Exception e){};
			if(align < Constants.ALIGNEMENT_NEUTRE || align >Constants.ALIGNEMENT_MERCENAIRE)
			{
				String str = "Valeur invalide";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			Personnage target = _perso;
			if(infos.length > 2)//Si un nom de perso est spécifié
			{
				target = World.getPersoByName(infos[2]);
				if(target == null)
				{
					String str = "Le personnage n'a pas ete trouve";
					SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
					return;
				}
			}
			
			target.modifAlignement(align);
			
			String str = "L'alignement du joueur a ete modifie";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
		}else
		if(command.equalsIgnoreCase("SETREPONSES"))
		{
			if(_compte.get_gmLvl() < 2)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous n'avez pas le niveau MJ requis");
				return;
			}
			if(infos.length <3)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,"Il manque un/des arguments");
				return;
			}
			int id = 0;
			try
			{
				id = Integer.parseInt(infos[1]);
			}catch(Exception e){};
			String reps = infos[2];
			NPC_question Q = World.getNPCQuestion(id);
			String str = "";
			if(id == 0 || Q == null)
			{
				str = "QuestionID invalide";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			Q.setReponses(reps);
			boolean a= SQLManager.UPDATE_NPCREPONSES(id,reps);
			str = "Liste des reponses pour la question "+id+": "+Q.getReponses();
			if(a)str += "(sauvegarde dans la BDD)";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
			return;
		}else
		if(command.equalsIgnoreCase("SHOWREPONSES"))
		{
			if(_compte.get_gmLvl() < 2)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous n'avez pas le niveau MJ requis");
				return;
			}
			int id = 0;
			try
			{
				id = Integer.parseInt(infos[1]);
			}catch(Exception e){};
			NPC_question Q = World.getNPCQuestion(id);
			String str = "";
			if(id == 0 || Q == null)
			{
				str = "QuestionID invalide";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			str = "Liste des reponses pour la question "+id+": "+Q.getReponses();
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
			return;
		}else
		if(command.equalsIgnoreCase("HONOR"))
		{
			if(_compte.get_gmLvl() < 2)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous n'avez pas le niveau MJ requis");
				return;
			}
			int honor = 0;
			try
			{
				honor = Integer.parseInt(infos[1]);
			}catch(Exception e){};
			Personnage target = _perso;
			if(infos.length > 2)//Si un nom de perso est spécifié
			{
				target = World.getPersoByName(infos[2]);
				if(target == null)
				{
					String str = "Le personnage n'a pas ete trouve";
					SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
					return;
				}
			}
			String str = "Vous avez ajouter "+honor+" honneur a "+target.get_name();
			if(target.get_align() == Constants.ALIGNEMENT_NEUTRE)
			{
				str = "Le joueur est neutre ...";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			target.addHonor(honor);
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
			
		}else
		if(command.equalsIgnoreCase("ADDJOBXP"))
		{
			if(_compte.get_gmLvl() < 2)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous n'avez pas le niveau MJ requis");
				return;
			}
			int job = -1;
			int xp = -1;
			try
			{
				job = Integer.parseInt(infos[1]);
				xp = Integer.parseInt(infos[2]);
			}catch(Exception e){};
			if(job == -1 || xp < 0)
			{
				String str = "Valeurs invalides";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			Personnage target = _perso;
			if(infos.length > 3)//Si un nom de perso est spécifié
			{
				target = World.getPersoByName(infos[3]);
				if(target == null)
				{
					String str = "Le personnage n'a pas ete trouve";
					SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
					return;
				}
			}
			StatsMetier SM = target.getMetierByID(job);
			if(SM== null)
			{
				String str = "Le joueur ne connais pas le métier demandé";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
				
			SM.addXp(target, xp);
			
			String str = "Le metier a ete experimenter";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
		}else
		if(command.equalsIgnoreCase("LEARNJOB"))
		{
			if(_compte.get_gmLvl() < 2)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous n'avez pas le niveau MJ requis");
				return;
			}
			int job = -1;
			try
			{
				System.out.println(infos[1]);
				job = Integer.parseInt(infos[1]);
			}catch(Exception e){};
			if(job == -1 || World.getMetier(job) == null)
			{
				String str = "Valeur invalide";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			Personnage target = _perso;
			if(infos.length > 2)//Si un nom de perso est spécifié
			{
				target = World.getPersoByName(infos[2]);
				if(target == null)
				{
					String str = "Le personnage n'a pas ete trouve";
					SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
					return;
				}
			}
			
			target.learnJob(World.getMetier(job));
			
			String str = "Le metier a ete appris";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
		}else
		if(command.equalsIgnoreCase("CAPITAL"))
		{
			if(_compte.get_gmLvl() < 2)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous n'avez pas le niveau MJ requis");
				return;
			}
			int pts = -1;
			try
			{
				pts = Integer.parseInt(infos[1]);
			}catch(Exception e){};
			if(pts == -1)
			{
				String str = "Valeur invalide";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			Personnage target = _perso;
			if(infos.length > 2)//Si un nom de perso est spécifié
			{
				target = World.getPersoByName(infos[2]);
				if(target == null)
				{
					String str = "Le personnage n'a pas ete trouve";
					SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
					return;
				}
			}
			target.addCapital(pts);
			SocketManager.GAME_SEND_STATS_PACKET(target);
			String str = "Le capital a ete modifiee";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
		}else
		if(command.equalsIgnoreCase("SPAWNFIX"))
		{
			if(_compte.get_gmLvl() < 3)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous n'avez pas le niveau MJ requis");
				return;
			}
			String groupData = infos[1];

			_perso.get_curCarte().addStaticGroup(_perso.get_curCell().getID(), groupData);
			String str = "Le grouppe a ete fixe";
			//Sauvegarde DB de la modif
			if(SQLManager.SAVE_NEW_FIXGROUP(_perso.get_curCarte().get_id(),_perso.get_curCell().getID(), groupData))
				str += " et a ete sauvegarde dans la BDD";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
			return;
		}
		if(command.equalsIgnoreCase("SIZE"))
		{
			if(_compte.get_gmLvl() < 2)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous n'avez pas le niveau MJ requis");
				return;
			}
			int size = -1;
			try
			{
				size = Integer.parseInt(infos[1]);
			}catch(Exception e){};
			if(size == -1)
			{
				String str = "Taille invalide";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			Personnage target = _perso;
			if(infos.length > 2)//Si un nom de perso est spécifié
			{
				target = World.getPersoByName(infos[2]);
				if(target == null)
				{
					String str = "Le personnage n'a pas ete trouve";
					SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
					return;
				}
			}
			target.set_size(size);
			SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(target.get_curCarte(), target.get_GUID());
			SocketManager.GAME_SEND_ADD_PLAYER_TO_MAP(target.get_curCarte(), target);
			String str = "La taille du joueur a ete modifiee";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
		}else
		if(command.equalsIgnoreCase("SETADMIN"))
		{
			if(_compte.get_gmLvl() < 4)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous n'avez pas le niveau MJ requis");
				return;
			}
			int gmLvl = -100;
			try
			{
				gmLvl = Integer.parseInt(infos[1]);
			}catch(Exception e){};
			if(gmLvl == -100)
			{
				String str = "Valeur incorrecte";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			Personnage target = _perso;
			if(infos.length > 2)//Si un nom de perso est spécifié
			{
				target = World.getPersoByName(infos[2]);
				if(target == null)
				{
					String str = "Le personnage n'a pas ete trouve";
					SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
					return;
				}
			}
			target.get_compte().setGmLvl(gmLvl);
			SQLManager.UPDATE_ACCOUNT_DATA(target.get_compte());
			String str = "Le niveau GM du joueur a ete modifie";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
		}else
		if(command.equalsIgnoreCase("DEMORPH"))
		{
			Personnage target = _perso;
			if(infos.length > 1)//Si un nom de perso est spécifié
			{
				target = World.getPersoByName(infos[1]);
				if(target == null)
				{
					String str = "Le personnage n'a pas ete trouve";
					SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
					return;
				}
			}
			int morphID = target.get_classe()*10 + target.get_sexe();
			target.set_gfxID(morphID);
			SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(target.get_curCarte(), target.get_GUID());
			SocketManager.GAME_SEND_ADD_PLAYER_TO_MAP(target.get_curCarte(), target);
			String str = "Le joueur a ete transformé";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
		}
		else
		if(command.equalsIgnoreCase("MORPH"))
		{
			if(_compte.get_gmLvl() < 2)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous n'avez pas le niveau MJ requis");
				return;
			}
			int morphID = -1;
			try
			{
				morphID = Integer.parseInt(infos[1]);
			}catch(Exception e){};
			if(morphID == -1)
			{
				String str = "MorphID invalide";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			Personnage target = _perso;
			if(infos.length > 2)//Si un nom de perso est spécifié
			{
				target = World.getPersoByName(infos[2]);
				if(target == null)
				{
					String str = "Le personnage n'a pas ete trouve";
					SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
					return;
				}
			}
			target.set_gfxID(morphID);
			SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(target.get_curCarte(), target.get_GUID());
			SocketManager.GAME_SEND_ADD_PLAYER_TO_MAP(target.get_curCarte(), target);
			String str = "Le joueur a ete transformé";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
		}
		else
		if(command.equalsIgnoreCase("GONAME") || command.equalsIgnoreCase("JOIN"))
		{
			Personnage P = World.getPersoByName(infos[1]);
			if(P == null)
			{
				String str = "Le personnage n'existe pas";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			short mapID = P.get_curCarte().get_id();
			int cellID = P.get_curCell().getID();
			
			Personnage target = _perso;
			if(infos.length > 2)//Si un nom de perso est spécifié 
			{
				target = World.getPersoByName(infos[2]);
				if(target == null)
				{
					String str = "Le personnage n'a pas ete trouve";
					SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
					return;
				}
				if(target.get_fight() != null)
				{
					String str = "La cible est en combat";
					SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
					return;
				}
			}
			target.teleport(mapID, cellID);
			String str = "Le joueur a ete teleporte";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
		}
		else
		if(command.equalsIgnoreCase("NAMEGO"))
		{
			Personnage target = World.getPersoByName(infos[1]);
			if(target == null)
			{
				String str = "Le personnage n'existe pas";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			if(target.get_fight() != null)
			{
				String str = "La cible est en combat";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			Personnage P = _perso;
			if(infos.length > 2)//Si un nom de perso est spécifié
			{
				P = World.getPersoByName(infos[2]);
				if(P == null)
				{
					String str = "Le personnage n'a pas ete trouve";
					SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
					return;
				}
			}
			short mapID = P.get_curCarte().get_id();
			int cellID = P.get_curCell().getID();
			target.teleport(mapID, cellID);
			String str = "Le joueur a ete teleporte";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
		}else
		if(command.equalsIgnoreCase("ADDNPC"))
		{
			if(_compte.get_gmLvl() < 3)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous n'avez pas le niveau MJ requis");
				return;
			}
			int id = 0;
			try
			{
				id = Integer.parseInt(infos[1]);
			}catch(Exception e){};
			if(id == 0 || World.getNPCTemplate(id) == null)
			{
				String str = "NpcID invalide";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			NPC npc = _perso.get_curCarte().addNpc(id, _perso.get_curCell().getID(), _perso.get_orientation());
			SocketManager.GAME_SEND_ADD_NPC_TO_MAP(_perso.get_curCarte(), npc);
			String str = "Le PNJ a ete ajoute";
			if(_perso.get_orientation() == 0
					|| _perso.get_orientation() == 2
					|| _perso.get_orientation() == 4
					|| _perso.get_orientation() == 6)
						str += " mais est invisible (orientation diagonale invalide).";
			
			if(SQLManager.ADD_NPC_ON_MAP(_perso.get_curCarte().get_id(), id, _perso.get_curCell().getID(), _perso.get_orientation()))
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
			else
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,"Erreur au moment de sauvegarder la position");
		}else
		if(command.equalsIgnoreCase("DELNPC"))
		{
			if(_compte.get_gmLvl() < 3)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous n'avez pas le niveau MJ requis");
				return;
			}
			int id = 0;
			try
			{
				id = Integer.parseInt(infos[1]);
			}catch(Exception e){};
			NPC npc = _perso.get_curCarte().getNPC(id);
			if(id == 0 || npc == null)
			{
				String str = "Npc GUID invalide";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			int exC = npc.get_cellID();
			//on l'efface de la map
			SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(_perso.get_curCarte(), id);
			_perso.get_curCarte().removeNpcOrMobGroup(id);
			
			String str = "Le PNJ a ete supprime";
			if(SQLManager.DELETE_NPC_ON_MAP(_perso.get_curCarte().get_id(),exC))
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
			else
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,"Erreur au moment de sauvegarder la position");
		}else
		if(command.equalsIgnoreCase("MOVENPC"))
		{
			if(_compte.get_gmLvl() < 2)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous n'avez pas le niveau MJ requis");
				return;
			}
			int id = 0;
			try
			{
				id = Integer.parseInt(infos[1]);
			}catch(Exception e){};
			NPC npc = _perso.get_curCarte().getNPC(id);
			if(id == 0 || npc == null)
			{
				String str = "Npc GUID invalide";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			int exC = npc.get_cellID();
			//on l'efface de la map
			SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(_perso.get_curCarte(), id);
			//on change sa position/orientation
			npc.setCellID(_perso.get_curCell().getID());
			npc.setOrientation((byte)_perso.get_orientation());
			//on envoie la modif
			SocketManager.GAME_SEND_ADD_NPC_TO_MAP(_perso.get_curCarte(),npc);
			String str = "Le PNJ a ete deplace";
			if(_perso.get_orientation() == 0
			|| _perso.get_orientation() == 2
			|| _perso.get_orientation() == 4
			|| _perso.get_orientation() == 6)
				str += " mais est devenu invisible (orientation diagonale invalide).";
			if(SQLManager.DELETE_NPC_ON_MAP(_perso.get_curCarte().get_id(),exC)
			&& SQLManager.ADD_NPC_ON_MAP(_perso.get_curCarte().get_id(),npc.get_template().get_id(),_perso.get_curCell().getID(),_perso.get_orientation()))
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
			else
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,"Erreur au moment de sauvegarder la position");
		}else
		if(command.equalsIgnoreCase("DELTRIGGER"))
		{
			if(_compte.get_gmLvl() < 3)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous n'avez pas le niveau MJ requis");
				return;
			}
			int cellID = -1;
			try
			{
				cellID = Integer.parseInt(infos[1]);
			}catch(Exception e){};
			if(cellID == -1 || _perso.get_curCarte().getCase(cellID) == null)
			{
				String str = "CellID invalide";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			
			_perso.get_curCarte().getCase(cellID).clearOnCellAction();
			boolean success = SQLManager.REMOVE_TRIGGER(_perso.get_curCarte().get_id(),cellID);
			String str = "";
			if(success)	str = "Le trigger a ete retire";
			else 		str = "Le trigger n'a pas ete retire";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
		}else
		if(command.equalsIgnoreCase("ADDTRIGGER"))
		{
			if(_compte.get_gmLvl() < 3)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous n'avez pas le niveau MJ requis");
				return;
			}
			int actionID = -1;
			String args = "",cond = "";
			try
			{
				actionID = Integer.parseInt(infos[1]);
				args = infos[2];
				cond = infos[3];
			}catch(Exception e){};
			if(args.equals("") || actionID <= -3)
			{
				String str = "Valeur invalide";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			
			_perso.get_curCell().addOnCellStopAction(actionID,args, cond);
			boolean success = SQLManager.SAVE_TRIGGER(_perso.get_curCarte().get_id(),_perso.get_curCell().getID(),actionID,1,args,cond);
			String str = "";
			if(success)	str = "Le trigger a ete ajoute";
			else 		str = "Le trigger n'a pas ete ajoute";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
		}
		else
		if(command.equalsIgnoreCase("TELEPORT"))
		{
			short mapID = -1;
			int cellID = -1;
			try
			{
				mapID = Short.parseShort(infos[1]);
				cellID = Integer.parseInt(infos[2]);
			}catch(Exception e){};
			if(mapID == -1 || cellID == -1 || World.getCarte(mapID) == null)
			{
				String str = "MapID ou cellID invalide";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			if(World.getCarte(mapID).getCase(cellID) == null)
			{
				String str = "MapID ou cellID invalide";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			Personnage target = _perso;
			if(infos.length > 3)//Si un nom de perso est spécifié
			{
				target = World.getPersoByName(infos[3]);
				if(target == null  || target.get_fight() != null)
				{
					String str = "Le personnage n'a pas ete trouve ou est en combat";
					SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
					return;
				}
			}
			target.teleport(mapID, cellID);
			String str = "Le joueur a ete teleporte";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
		}
		else
		if(command.equalsIgnoreCase("DELNPCITEM"))
		{
			if(_compte.get_gmLvl() <3)return;
			int npcGUID = 0;
			int itmID = -1;
			try
			{
				npcGUID = Integer.parseInt(infos[1]);
				itmID = Integer.parseInt(infos[2]);
			}catch(Exception e){};
			NPC_tmpl npc =  _perso.get_curCarte().getNPC(npcGUID).get_template();
			if(npcGUID == 0 || itmID == -1 || npc == null)
			{
				String str = "NpcGUID ou itmID invalide";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			
			
			String str = "";
			if(npc.delItemVendor(itmID))str = "L'objet a ete retire";
			else str = "L'objet n'a pas ete retire";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
		}
		else
		if(command.equalsIgnoreCase("ADDNPCITEM"))
		{
			if(_compte.get_gmLvl() <3)return;
			int npcGUID = 0;
			int itmID = -1;
			try
			{
				npcGUID = Integer.parseInt(infos[1]);
				itmID = Integer.parseInt(infos[2]);
			}catch(Exception e){};
			NPC_tmpl npc =  _perso.get_curCarte().getNPC(npcGUID).get_template();
			ObjTemplate item =  World.getObjTemplate(itmID);
			if(npcGUID == 0 || itmID == -1 || npc == null || item == null)
			{
				String str = "NpcGUID ou itmID invalide";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			
			
			String str = "";
			if(npc.addItemVendor(item))str = "L'objet a ete rajoute";
			else str = "L'objet n'a pas ete rajoute";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
		}
		else
		if(command.equalsIgnoreCase("GOMAP"))
		{
			int mapX = 0;
			int mapY = 0;
			int cellID = 0;
			int contID = 0;//Par défaut Amakna
			try
			{
				mapX = Integer.parseInt(infos[1]);
				mapY = Integer.parseInt(infos[2]);
				cellID = Integer.parseInt(infos[3]);
				contID = Integer.parseInt(infos[4]);
			}catch(Exception e){};
			Carte map = World.getCarteByPosAndCont(mapX,mapY,contID);
			if(map == null)
			{
				String str = "Position ou continent invalide";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			if(map.getCase(cellID) == null)
			{
				String str = "CellID invalide";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			Personnage target = _perso;
			if(infos.length > 5)//Si un nom de perso est spécifié
			{
				target = World.getPersoByName(infos[5]);
				if(target == null || target.get_fight() != null)
				{
					String str = "Le personnage n'a pas ete trouve ou est en combat";
					SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
					return;
				}
				if(target.get_fight() != null)
				{
					String str = "La cible est en combat";
					SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
					return;
				}
			}
			target.teleport(map.get_id(), cellID);
			String str = "Le joueur a ete teleporte";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
		}
		else
		if(command.equalsIgnoreCase("ADDMOUNTPARK"))
		{
			if(_compte.get_gmLvl() < 3)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous n'avez pas le niveau MJ requis");
				return;
			}
			int size = -1;
			int owner = -2;
			int price = -1;
			try
			{
				size = Integer.parseInt(infos[1]);
				owner = Integer.parseInt(infos[2]);
				price = Integer.parseInt(infos[3]);
				if(price > 20000000)price = 20000000;
				if(price <0)price = 0;
			}catch(Exception e){};
			if(size == -1 || owner == -2 || price == -1 || _perso.get_curCarte().getMountPark() != null)
			{
				String str = "Infos invalides ou map deja config.";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
				return;
			}
			MountPark MP = new MountPark(owner, _perso.get_curCarte(), size, "", -1, price);
			_perso.get_curCarte().setMountPark(MP);
			SQLManager.SAVE_MOUNTPARK(MP);
			String str = "L'enclos a ete config. avec succes";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
		}else
		if(command.equalsIgnoreCase("ITEM") || command.equalsIgnoreCase("!getitem"))
		{
			boolean isOffiCmd = command.equalsIgnoreCase("!getitem");
			if(_compte.get_gmLvl() < 2)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous n'avez pas le niveau MJ requis");
				return;
			}
			int tID = 0;
			try
			{
				tID = Integer.parseInt(infos[1]);
			}catch(Exception e){};
			if(tID == 0)
			{
				String mess = "Le template "+tID+" n'existe pas ";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
				return;
			}
			int qua = 1;
			if(infos.length == 3)//Si une quantité est spécifiée
			{
				try
				{
					qua = Integer.parseInt(infos[2]);
				}catch(Exception e){};
			}
			boolean useMax = false;
			if(infos.length == 4 && !isOffiCmd)//Si un jet est spécifiée
			{
				if(infos[3].equalsIgnoreCase("MAX"))useMax = true;
			}
			ObjTemplate t = World.getObjTemplate(tID);
			if(t == null)
			{
				String mess = "Le template "+tID+" n'existe pas ";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
				return;
			}
			if(qua <1)qua =1;
			Objet obj = t.createNewItem(qua,useMax);
			if(_perso.addObjet(obj, true))//Si le joueur n'avait pas d'item similaire
				World.addObjet(obj,true);
			String str = "Creation de l'item "+tID+" reussie";
			if(useMax) str += " avec des stats maximums";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
		}
		else
		if(command.equalsIgnoreCase("ITEMSET"))
		{
			if(_compte.get_gmLvl() < 2)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous n'avez pas le niveau MJ requis");
				return;
			}
			int tID = 0;
			try
			{
				tID = Integer.parseInt(infos[1]);
			}catch(Exception e){};
			ItemSet IS = World.getItemSet(tID);
			if(tID == 0 || IS == null)
			{
				String mess = "La panoplie "+tID+" n'existe pas ";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
				return;
			}
			boolean useMax = false;
			if(infos.length == 3)useMax = infos[2].equals("MAX");//Si un jet est spécifiée

			
			for(ObjTemplate t : IS.getItemTemplates())
			{
				Objet obj = t.createNewItem(1,useMax);
				if(_perso.addObjet(obj, true))//Si le joueur n'avait pas d'item similaire
					World.addObjet(obj,true);
			}
			String str = "Creation de la panoplie "+tID+" reussie";
			if(useMax) str += " avec des stats maximums";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,str);
		}
		else
		if(command.equalsIgnoreCase("LEVEL"))
		{
			if(_compte.get_gmLvl() < 2)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous n'avez pas le niveau MJ requis");
				return;
			}
			int count = 0;
			try
			{
				count = Integer.parseInt(infos[1]);
				if(count < 1)	count = 1;
				if(count > 200)	count = 200;
				Personnage perso = _perso;
				if(infos.length == 3)//Si le nom du perso est spécifier
				{
					String name = infos[2];
					perso = World.getPersoByName(name);
					if(perso == null)
						perso = _perso;
				}
				if(perso.get_lvl() < count)
				{
					while(perso.get_lvl() < count)
					{
						perso.levelUp(false,true);
					}
					if(perso.isOnline())
					{
						SocketManager.GAME_SEND_NEW_LVL_PACKET(perso.get_compte().getGameThread().get_out(),perso.get_lvl());
						SocketManager.GAME_SEND_STATS_PACKET(perso);
					}
				}
				String mess = "Vous avez fixer le niveau de "+perso.get_name()+" a "+count;
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
			}catch(Exception e)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Valeur incorecte");
				return;
			};
		}
		else
		if(command.equalsIgnoreCase("PDVPER"))
		{
			if(_compte.get_gmLvl() < 2)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous n'avez pas le niveau MJ requis");
				return;
			}
			int count = 0;
			try
			{
				count = Integer.parseInt(infos[1]);
				if(count < 0)	count = 0;
				if(count > 100)	count = 100;
				Personnage perso = _perso;
				if(infos.length == 3)//Si le nom du perso est spécifié
				{
					String name = infos[2];
					perso = World.getPersoByName(name);
					if(perso == null)
						perso = _perso;
				}
				int newPDV = perso.get_PDVMAX() * count / 100;
				perso.set_PDV(newPDV);
				if(perso.isOnline())
					SocketManager.GAME_SEND_STATS_PACKET(perso);
				String mess = "Vous avez fixer le pourcentage de pdv de "+perso.get_name()+" a "+count;
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
			}catch(Exception e)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Valeur incorecte");
				return;
			};
		}else
		if(command.equalsIgnoreCase("KAMAS"))
		{
			if(_compte.get_gmLvl() < 2)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous n'avez pas le niveau MJ requis");
				return;
			}
			int count = 0;
			try
			{
				count = Integer.parseInt(infos[1]);
			}catch(Exception e)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Valeur incorecte");
				return;
			};
			if(count == 0)return;
			
			Personnage perso = _perso;
			if(infos.length == 3)//Si le nom du perso est spécifier
			{
				String name = infos[2];
				perso = World.getPersoByName(name);
				if(perso == null)
					perso = _perso;
			}
			long curKamas = perso.get_kamas();
			long newKamas = curKamas + count;
			if(newKamas <0) newKamas = 0;
			if(newKamas > 1000000000) newKamas = 1000000000;
			perso.set_kamas(newKamas);
			if(perso.isOnline())
				SocketManager.GAME_SEND_STATS_PACKET(perso);
			String mess = "Vous avez ";
			mess += (count<0?"retirer":"ajouter")+" ";
			mess += Math.abs(count)+" kamas a "+perso.get_name();
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
		}else if (command.equalsIgnoreCase("DOACTION"))
		{
			//DOACTION NAME TYPE ARGS COND
			if(infos.length < 4)
			{
				String mess = "Nombre d'argument de la commande incorect !";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
				return;
			}
			int type = -100;
			String args = "",cond = "";
			Personnage perso = _perso;
			try
			{
				perso = World.getPersoByName(infos[1]);
				if(perso == null)perso = _perso;
				type = Integer.parseInt(infos[2]);
				args = infos[3];
				if(infos.length >4)
				cond = infos[4];
			}catch(Exception e)
			{
				String mess = "Arguments de la commande incorect !";
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
				return;
			}
			(new Action(type,args,cond)).apply(perso, -1);
			String mess = "Action effectuee !";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
		}else if (command.equalsIgnoreCase("SPAWN"))
		{
			if(_compte.get_gmLvl() < 2)
			{
				SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out, "Vous n'avez pas le niveau MJ requis");
				return;
			}
			System.out.println("GROUP DATA : "+infos[1]);
			_perso.get_curCarte().spawnGroupOnCommand(_perso.get_curCell().getID(), infos[1]);
		}
		else
		{
			String mess = "Commande non reconnue";
			SocketManager.GAME_SEND_CONSOLE_MESSAGE_PACKET(_out,mess);
		}
	}

	public void closeSocket()
	{
		try {
			this._s.close();
		} catch (IOException e) {}
	}

	private void Basic_chatMessage(String packet)
	{
		String msg = "";
		if(_perso.isMuted())
		{
			SocketManager.GAME_SEND_MESSAGE(_perso,"Vous etes mute.", Ancestra.CONFIG_MOTD_COLOR);
			return;
		}
		packet = packet.replace("<", "");
		packet = packet.replace(">", "");
		if(packet.length() == 3)return;
		switch(packet.charAt(2))
		{
			case '*'://Canal noir
				if(!_perso.get_canaux().contains(packet.charAt(2)+""))return;
				msg = packet.split("\\|",2)[1];
				
				//Commandes joueurs
				if(msg.charAt(0) == '.')
				{
					//Retour au point de sauvegarde
					if(msg.length() > 7 && msg.substring(1, 8).equalsIgnoreCase("command"))
					{
						SocketManager.GAME_SEND_MESSAGE(_perso, "Commandes Disponibles : \n.start\n.infos\n.save", Ancestra.CONFIG_MOTD_COLOR);
						return;
					}
					
					if(msg.length() > 5 && msg.substring(1, 6).equalsIgnoreCase("start"))
					{
						if(_perso.get_fight() != null)return;
						_perso.warpToSavePos();
						return;
					}
					
					if(msg.length() > 5 && msg.substring(1, 6).equalsIgnoreCase("infos"))
					{
						long uptime = System.currentTimeMillis() - Ancestra.gameServer.getStartTime();
						int jour = (int) (uptime/(1000*3600*24));
						uptime %= (1000*3600*24);
						int hour = (int) (uptime/(1000*3600));
						uptime %= (1000*3600);
						int min = (int) (uptime/(1000*60));
						uptime %= (1000*60);
						int sec = (int) (uptime/(1000));
						
						String mess =	"===========\n"
							+       	"Ancestra-R v. "+Constants.SERVER_VERSION+" par "+Constants.SERVER_MAKER+"\n"
							+			"Uptime: "+jour+"j "+hour+"h "+min+"m "+sec+"s\n"
							+			"Joueurs en lignes: "+Ancestra.gameServer.getPlayerNumber()+"\n"
							+			"Record de connexion: "+Ancestra.gameServer.getMaxPlayer()+"\n"
							+			"===========";
						SocketManager.GAME_SEND_MESSAGE(_perso, mess, Ancestra.CONFIG_MOTD_COLOR);
						return;
					}
					
					if(msg.length() > 4 && msg.substring(1, 5).equalsIgnoreCase("save"))
					{				
						long k;
						if((k = System.currentTimeMillis() - _timeLastsave) < 360000)
						{
							k = (Ancestra.FLOOD_TIME  - k)/10;//On calcul la différence en secondes
							return;
						}
						_timeLastsave = System.currentTimeMillis();
						if(_perso.get_fight() != null)return;
						SQLManager.SAVE_PERSONNAGE(_perso,true);
						SocketManager.GAME_SEND_MESSAGE(_perso,  _perso.get_name()+" sauvegarde.", Ancestra.CONFIG_MOTD_COLOR);
						return;
					}
				}
				if(_perso.get_fight() == null)
					SocketManager.GAME_SEND_cMK_PACKET_TO_MAP(_perso.get_curCarte(), "", _perso.get_GUID(), _perso.get_name(), msg);
				else
					SocketManager.GAME_SEND_cMK_PACKET_TO_FIGHT(_perso.get_fight(), 7, "", _perso.get_GUID(), _perso.get_name(), msg);
			break;
			case '#'://Canal Equipe
				if(!_perso.get_canaux().contains(packet.charAt(2)+""))return;
				if(_perso.get_fight() != null)
				{
					msg = packet.split("\\|",2)[1];
					int team = _perso.get_fight().getTeamID(_perso.get_GUID());
					if(team == -1)return;
					SocketManager.GAME_SEND_cMK_PACKET_TO_FIGHT(_perso.get_fight(), team, "#", _perso.get_GUID(), _perso.get_name(), msg);
				}
			break;
			case '$'://Canal groupe
				if(!_perso.get_canaux().contains(packet.charAt(2)+""))return;
				if(_perso.getGroup() == null)break;
				msg = packet.split("\\|",2)[1];
				SocketManager.GAME_SEND_cMK_PACKET_TO_GROUP(_perso.getGroup(), "$", _perso.get_GUID(), _perso.get_name(), msg);
			break;
			
			case ':'://Canal commerce
				if(!_perso.get_canaux().contains(packet.charAt(2)+""))return;
				long l;
				if((l = System.currentTimeMillis() - _timeLastTradeMsg) < Ancestra.FLOOD_TIME)
				{
					l = (Ancestra.FLOOD_TIME  - l)/10;//On calcul la différence en secondes
					
					return;
				}
				_timeLastTradeMsg = System.currentTimeMillis();
				msg = packet.split("\\|",2)[1];
				SocketManager.GAME_SEND_cMK_PACKET_TO_ALL(":", _perso.get_GUID(), _perso.get_name(), msg);
			break;
			case '@'://Canal Admin
				if(_perso.get_compte().get_gmLvl() ==0)return;
				msg = packet.split("\\|",2)[1];
				SocketManager.GAME_SEND_cMK_PACKET_TO_ADMIN("@", _perso.get_GUID(), _perso.get_name(), msg);
			break;
			case '?'://Canal recrutement
				if(!_perso.get_canaux().contains(packet.charAt(2)+""))return;
				long j;
				if((j = System.currentTimeMillis() - _timeLastRecrutmentMsg) < Ancestra.FLOOD_TIME)
				{
					j = (Ancestra.FLOOD_TIME  - j)/10;//On calcul la différence en secondes
					
					return;
				}
				_timeLastRecrutmentMsg = System.currentTimeMillis();
				msg = packet.split("\\|",2)[1];
				SocketManager.GAME_SEND_cMK_PACKET_TO_ALL("?", _perso.get_GUID(), _perso.get_name(), msg);
			break;
			case '%'://Canal guilde
				if(!_perso.get_canaux().contains(packet.charAt(2)+""))return;
				if(_perso.get_guild() == null)return;
				msg = packet.split("\\|",2)[1];
				SocketManager.GAME_SEND_cMK_PACKET_TO_GUILD(_perso.get_guild(), "%", _perso.get_GUID(), _perso.get_name(), msg);
			break;
			case 0xC2://Canal 
			break;
			default:
				String nom = packet.substring(2).split("\\|")[0];
				msg = packet.split("\\|",2)[1];
				if(nom.length() <= 1)
					GameServer.addToLog("ChatHandler: Chanel non géré : "+nom);
				else
				{
					Personnage target = World.getPersoByName(nom);
					if(target == null)//si le personnage n'existe pas
					{
						SocketManager.GAME_SEND_CHAT_ERROR_PACKET(_out, nom);
						return;
					}
					if(target.get_compte() == null)
					{
						SocketManager.GAME_SEND_CHAT_ERROR_PACKET(_out, nom);
						return;
					}
					if(target.get_compte().getGameThread() == null)//si le perso n'est pas co
					{
						SocketManager.GAME_SEND_CHAT_ERROR_PACKET(_out, nom);
						return;
					}
					if(target.get_compte().isEnemyWith(_perso.get_compte().get_GUID()) == true || !target.isDispo(_perso))
					{
						SocketManager.GAME_SEND_Im_PACKET(_perso, "114;"+target.get_name());
						return;
					}
					SocketManager.GAME_SEND_cMK_PACKET(target, "F", _perso.get_GUID(), _perso.get_name(), msg);
					SocketManager.GAME_SEND_cMK_PACKET(_perso, "T", target.get_GUID(), target.get_name(), msg);
				}
			break;
		}
	}

	private void Basic_send_Date_Hour()
	{
		SocketManager.GAME_SEND_SERVER_DATE(_out);
		SocketManager.GAME_SEND_SERVER_HOUR(_out);
	}
	
	private void Basic_infosmessage(String packet)
	{
			packet = packet.substring(2);
			Personnage T = World.getPersoByName(packet);
			SocketManager.GAME_SEND_BWK(_perso, T.get_compte().get_name()+"|1|"+T.get_name()+"|-1");
	}

	private void parseGamePacket(String packet)
	{
		switch(packet.charAt(1))
		{
			case 'A':
				if(_perso == null)return;
					parseGameActionPacket(packet);
			break;
			case 'C':
				if(_perso == null)return;
				_perso.sendGameCreate();
			break;
			case 'f':
				Game_on_showCase(packet);
			break;
			case 'I':
				Game_on_GI_packet();
			break;
			case 'K':
				Game_on_GK_packet(packet);
			break;
			case 'P'://PvP Toogle
				_perso.toogleWings(packet.charAt(2));
			break;
			case 'p':
				Game_on_ChangePlace_packet(packet);
			break;
			case 'Q':
				Game_onLeftFight();
			break;
			case 'R':
				Game_on_Ready(packet);
			break;
			case 't':
				if(_perso.get_fight() == null)return;
				_perso.get_fight().playerPass(_perso);
			break;
		}
	}

	
	private void Game_onLeftFight()
	{
		if(_perso.get_fight() == null)return;
		_perso.get_fight().leftFight(_perso);
	}

	private void Game_on_showCase(String packet)
	{
		if(_perso == null)return;
		if(_perso.get_fight() == null)return;
		if(_perso.get_fight().get_state() != Constants.FIGHT_STATE_ACTIVE)return;
		int cellID = -1;
		try
		{
			cellID = Integer.parseInt(packet.substring(2));
		}catch(Exception e){};
		if(cellID == -1)return;
		_perso.get_fight().showCaseToTeam(_perso.get_GUID(),cellID);
	}

	private void Game_on_Ready(String packet)
	{
		if(_perso.get_fight() == null)return;
		if(_perso.get_fight().get_state() != Constants.FIGHT_STATE_PLACE)return;
		_perso.set_ready(packet.substring(2).equalsIgnoreCase("1"));
		_perso.get_fight().verifIfAllReady();
		SocketManager.GAME_SEND_FIGHT_PLAYER_READY_TO_FIGHT(_perso.get_fight(),3,_perso.get_GUID(),packet.substring(2).equalsIgnoreCase("1"));
	}

	private void Game_on_ChangePlace_packet(String packet)
	{
		if(_perso.get_fight() == null)return;
		try
		{
			int cell = Integer.parseInt(packet.substring(2));
			_perso.get_fight().changePlace( _perso, cell);
		}catch(NumberFormatException e){return;};
	}

	private void Game_on_GK_packet(String packet)
	{	
		int GameActionId = -1;
		String[] infos = packet.substring(3).split("\\|");
		try
		{
			GameActionId = Integer.parseInt(infos[0]);
		}catch(Exception e){return;};
		if(GameActionId == -1)return;
		GameAction GA = _actions.get(GameActionId);
		if(GA == null)return;
		boolean isOk = packet.charAt(2) == 'K';
		
		switch(GA._actionID)
		{
			case 1://Deplacement
				if(isOk)
				{
					//Hors Combat
					if(_perso.get_fight() == null)
					{
						_perso.get_curCell().removePlayer(_perso.get_GUID());
						SocketManager.GAME_SEND_BN(_out);
						String path = GA._args;
						//On prend la case ciblée
						Case nextCell = _perso.get_curCarte().getCase(CryptManager.cellCode_To_ID(path.substring(path.length()-2)));
						Case targetCell = _perso.get_curCarte().getCase(CryptManager.cellCode_To_ID(GA._packet.substring(GA._packet.length()-2)));
						
						//On définie la case et on ajoute le personnage sur la case
						_perso.set_curCell(nextCell);
						_perso.set_orientation(CryptManager.getIntByHashedValue(path.charAt(path.length()-3)));
						_perso.get_curCell().addPerso(_perso);
						_perso.set_away(false);
						if(targetCell.getObject() != null)
						{
							//Si c'est une "borne" comme Emotes, ou Création guilde
							if(targetCell.getObject().getID() == 1324)
							{
								Constants.applyPlotIOAction(_perso,_perso.get_curCarte().get_id(),targetCell.getID());
							}
						}
						_perso.get_curCarte().onPlayerArriveOnCell(_perso,_perso.get_curCell().getID());
					}
					else//En combat
					{
						_perso.get_fight().onGK(_perso);
						return;
					}
					
				}
				else
				{
					//Si le joueur s'arrete sur une case
					int newCellID = -1;
					try
					{
						newCellID = Integer.parseInt(infos[1]);
					}catch(Exception e){return;};
					if(newCellID == -1)return;
					String path = GA._args;
					_perso.get_curCell().removePlayer(_perso.get_GUID());
					_perso.set_curCell(_perso.get_curCarte().getCase(newCellID));
					_perso.set_orientation(CryptManager.getIntByHashedValue(path.charAt(path.length()-3)));
					_perso.get_curCell().addPerso(_perso);
					SocketManager.GAME_SEND_BN(_out);
				}
			break;
			
			case 500://Action Sur Map
				_perso.finishActionOnCell(GA);
			break;

		}
		removeAction(GA);
	}

	private void Game_on_GI_packet() 
	{
		if(_perso.get_fight() != null)
		{
			//Only percepteur
			SocketManager.GAME_SEND_MAP_GMS_PACKETS(_perso.get_curCarte(), _perso);
			SocketManager.GAME_SEND_GDK_PACKET(_out);
			return;
		}
		//Enclos
		SocketManager.GAME_SEND_Rp_PACKET(_perso, _perso.get_curCarte().getMountPark());
		//Maisons
		House.LoadHouse(_perso, _perso.get_curCarte().get_id());
		//Objets sur la carte
		SocketManager.GAME_SEND_MAP_GMS_PACKETS(_perso.get_curCarte(), _perso);
		SocketManager.GAME_SEND_MAP_MOBS_GMS_PACKETS(_perso.get_compte().getGameThread().get_out(), _perso.get_curCarte());
		SocketManager.GAME_SEND_MAP_NPCS_GMS_PACKETS(_out,_perso.get_curCarte());
		SocketManager.GAME_SEND_MAP_PERCO_GMS_PACKETS(_out,_perso.get_curCarte());
		SocketManager.GAME_SEND_MAP_OBJECTS_GDS_PACKETS(_out,_perso.get_curCarte());
		SocketManager.GAME_SEND_GDK_PACKET(_out);
		SocketManager.GAME_SEND_MAP_FIGHT_COUNT(_out, _perso.get_curCarte());
		//Les drapeau de combats
		Fight.FightStateAddFlag(_perso.get_curCarte(), _perso);
		//items au sol
		_perso.get_curCarte().sendFloorItems(_perso);
	}

	private void parseGameActionPacket(String packet)
	{
		int actionID;
		try
		{
			actionID = Integer.parseInt(packet.substring(2,5));
		}catch(NumberFormatException e){return;};
		
		int nextGameActionID = 0;
		if(_actions.size() > 0)
		{
			//On prend le plus haut GameActionID + 1
			nextGameActionID = (Integer)(_actions.keySet().toArray()[_actions.size()-1])+1;
		}
		GameAction GA = new GameAction(nextGameActionID,actionID,packet);
		
		switch(actionID)
		{
			case 1://Deplacement
				game_parseDeplacementPacket(GA);
			break;
			
			case 300://Sort
				game_tryCastSpell(packet);
			break;
			
			case 303://Attaque CaC
				game_tryCac(packet);
			break;
			
			case 500://Action Sur Map
				game_action(GA);
			break;
			
			case 507://Panneau intérieur de la maison
				house_action(packet);
			break;
			
			case 900://Demande Defie
				game_ask_duel(packet);
			break;
			case 901://Accepter Defie
				game_accept_duel(packet);
			break;
			case 902://Refus/Anuler Defie
				game_cancel_duel(packet);
			break;
			case 903://Rejoindre combat
				game_join_fight(packet);
			break;
			case 906://Agresser
				game_aggro(packet);
			break;
			case 909://Perco
				game_perco(packet);
			break;
		}	
	}

	private void house_action(String packet)
	{
		int actionID = Integer.parseInt(packet.substring(5));
		switch(actionID)
		{
			case 81://Vérouiller maison
				House.Lock(_perso);
			break;
			case 97://Acheter maison
				House.BuyIt(_perso);
			break;
			case 98://Vendre
			case 108://Modifier prix de vente
				House.SellIt(_perso);
			break;
		}
	}
	
	
	private void game_perco(String packet)
	{
		try
		{
			if(_perso == null)return;
			if(_perso.get_fight() != null)return;
			if(_perso.get_isTalkingWith() != 0 ||
			   _perso.get_isTradingWith() != 0 ||
			   _perso.getCurJobAction() != null ||
			   _perso.get_curExchange() != null)
					{
						return;
					}
			int id = Integer.parseInt(packet.substring(5));
			Percepteur target = Percepteur.GetPerco(id);
			if(target.get_inFight() > 0) return;
			SocketManager.GAME_SEND_GA_PACKET_TO_MAP(_perso.get_curCarte(),"", 909, _perso.get_GUID()+"", id+"");
			_perso.get_curCarte().startFigthVersusPercepteur(_perso, target);
		}catch(Exception e){};
	}
	
	private void game_aggro(String packet)
	{
		try
		{
			if(_perso == null)return;
			if(_perso.get_fight() != null)return;
			int id = Integer.parseInt(packet.substring(5));
			Personnage target = World.getPersonnage(id);
			if(target == null || !target.isOnline() || target.get_fight() != null
			|| target.get_curCarte().get_id() != _perso.get_curCarte().get_id()
			|| target.get_align() == _perso.get_align()
			|| _perso.get_curCarte().get_placesStr().equalsIgnoreCase("|"))
				return;
			/*
			 * 
			 * || target.get_align() == 0 : Impossible d'agresser les joueurs neutres
			 * 
			 */

			_perso.toogleWings('+');
			SocketManager.GAME_SEND_GA_PACKET_TO_MAP(_perso.get_curCarte(),"", 906, _perso.get_GUID()+"", id+"");
			_perso.get_curCarte().newFight(_perso, target, Constants.FIGHT_TYPE_AGRESSION);
		}catch(Exception e){};
	}

	private void game_action(GameAction GA)
	{
		String packet = GA._packet.substring(5);
		int cellID = -1;
		int actionID = -1;
		
		try
		{
			cellID = Integer.parseInt(packet.split(";")[0]);
			actionID = Integer.parseInt(packet.split(";")[1]);
		}catch(Exception e){}
		//Si packet invalide, ou cellule introuvable
		if(cellID == -1 || actionID == -1 || _perso == null || _perso.get_curCarte() == null ||
				_perso.get_curCarte().getCase(cellID) == null)
			return;
		GA._args = cellID+";"+actionID;
		_perso.get_compte().getGameThread().addAction(GA);
		_perso.startActionOnCell(GA);
	}

	private void game_tryCac(String packet)
	{
		try
		{
			if(_perso.get_fight() ==null)return;
			int cellID = -1;
			try
			{
				cellID = Integer.parseInt(packet.substring(5));
			}catch(Exception e){return;};
			
			_perso.get_fight().tryCaC(_perso,cellID);
		}catch(Exception e){};
	}

	private void game_tryCastSpell(String packet)
	{
		try
		{
			String[] splt = packet.split(";");
			int spellID = Integer.parseInt(splt[0].substring(5));
			int caseID = Integer.parseInt(splt[1]);
			if(_perso.get_fight() != null)
			{
				SortStats SS = _perso.getSortStatBySortIfHas(spellID);
				if(SS == null)return;
				_perso.get_fight().tryCastSpell(_perso.get_fight().getFighterByPerso(_perso),SS,caseID);
			}
		}catch(NumberFormatException e){return;};
	}

	private void game_join_fight(String packet)
	{
		System.out.println("Pack "+packet);
		String[] infos = packet.substring(5).split(";");
		if(infos.length == 1)
		{
			try
			{
				Fight F = _perso.get_curCarte().getFight(Integer.parseInt(infos[0]));
				F.joinAsSpect(_perso);
			}catch(Exception e){return;};
		}else
		{
			try
			{
				int guid = Integer.parseInt(infos[1]);
				if(_perso.is_away()){SocketManager.GAME_SEND_GA903_ERROR_PACKET(_out,'o',guid);return;};
				if(World.getPersonnage(guid) == null)return;
				Fight F = _perso.get_curCarte().getFight(Integer.parseInt(infos[0]));
				if(F == null)
				{
					int Fid = Fight.getFightIDByFighter(_perso.get_curCarte(), Integer.parseInt(infos[1]));
					F = _perso.get_curCarte().getFight(Fid);
				}
				if(_perso.get_guild() != null)
				{
					if(F != null && F.get_guildID() == _perso.get_guild().get_id()) 
					{
						return;
					}
				}
				World.getPersonnage(guid).get_fight().joinFight(_perso,guid);
			}catch(Exception e){return;};
		}
	}

	private void game_accept_duel(String packet)
	{
		int guid = -1;
		try{guid = Integer.parseInt(packet.substring(5));}catch(NumberFormatException e){return;};
		if(_perso.get_duelID() != guid || _perso.get_duelID() == -1)return;
		SocketManager.GAME_SEND_MAP_START_DUEL_TO_MAP(_perso.get_curCarte(),_perso.get_duelID(),_perso.get_GUID());
		Fight fight = _perso.get_curCarte().newFight(World.getPersonnage(_perso.get_duelID()),_perso,Constants.FIGHT_TYPE_CHALLENGE);
		_perso.set_fight(fight);
		World.getPersonnage(_perso.get_duelID()).set_fight(fight);
		
	}

	private void game_cancel_duel(String packet)
	{
		try
		{
			if(_perso.get_duelID() == -1)return;
			SocketManager.GAME_SEND_CANCEL_DUEL_TO_MAP(_perso.get_curCarte(),_perso.get_duelID(),_perso.get_GUID());
			World.getPersonnage(_perso.get_duelID()).set_away(false);
			World.getPersonnage(_perso.get_duelID()).set_duelID(-1);
			_perso.set_away(false);
			_perso.set_duelID(-1);	
		}catch(NumberFormatException e){return;};
	}

	private void game_ask_duel(String packet)
	{
		if(_perso.get_curCarte().get_placesStr().equalsIgnoreCase("|"))
		{
			SocketManager.GAME_SEND_DUEL_Y_AWAY(_out, _perso.get_GUID());
			return;
		}
		try
		{
			int guid = Integer.parseInt(packet.substring(5));
			if(_perso.is_away() || _perso.get_fight() != null){SocketManager.GAME_SEND_DUEL_Y_AWAY(_out, _perso.get_GUID());return;}
			if(World.getPersonnage(guid) == null) return;
			if(World.getPersonnage(guid).is_away() || World.getPersonnage(guid).get_fight() != null || World.getPersonnage(guid).get_curCarte().get_id() != _perso.get_curCarte().get_id()){SocketManager.GAME_SEND_DUEL_E_AWAY(_out, _perso.get_GUID());return;}
			_perso.set_duelID(guid);
			_perso.set_away(true);
			World.getPersonnage(guid).set_duelID(_perso.get_GUID());
			World.getPersonnage(guid).set_away(true);
			SocketManager.GAME_SEND_MAP_NEW_DUEL_TO_MAP(_perso.get_curCarte(),_perso.get_GUID(),guid);
		}catch(NumberFormatException e){return;}
	}

	private void game_parseDeplacementPacket(GameAction GA)
	{
		String path = GA._packet.substring(5);
		if(_perso.get_fight() == null)
		{
			AtomicReference<String> pathRef = new AtomicReference<String>(path);
			int result = Pathfinding.isValidPath(_perso.get_curCarte(),_perso.get_curCell().getID(),pathRef, null);
			
			//Si déplacement inutile
			if(result == 0)
			{
				SocketManager.GAME_SEND_GA_PACKET(_out,"", "0", "", "");
				removeAction(GA);
				return;
			}
			if(result != -1000 && result < 0)result = -result;
			
			//On prend en compte le nouveau path
			path = pathRef.get();
			//Si le path est invalide
			if(result == -1000)
			{
				GameServer.addToLog(_perso.get_name()+"("+_perso.get_GUID()+") Tentative de  déplacement avec un path invalide");
				path = CryptManager.getHashedValueByInt(_perso.get_orientation())+CryptManager.cellID_To_Code(_perso.get_curCell().getID());	
			}
			//On sauvegarde le path dans la variable
			GA._args = path;
			
			SocketManager.GAME_SEND_GA_PACKET_TO_MAP(_perso.get_curCarte(), ""+GA._id, 1, _perso.get_GUID()+"", "a"+CryptManager.cellID_To_Code(_perso.get_curCell().getID())+path);
			addAction(GA);
			if(_perso.isSitted())_perso.setSitted(false);
			_perso.set_away(true);
		}else
		{
			Fighter F = _perso.get_fight().getFighterByPerso(_perso);
			if(F == null)return;
			GA._args = path;
			_perso.get_fight().fighterDeplace(F,GA);
		}
	}

	public PrintWriter get_out() {
		return _out;
	}
	
	public void kick()
	{
		try
		{
			Ancestra.gameServer.delClient(this);
			
    		if(_compte != null)
    		{
    			_compte.deconnexion();
    		}
    		if(!_s.isClosed())
    		_s.close();
    		_in.close();
    		_out.close();
    		_t.interrupt();
		}catch(IOException e1){e1.printStackTrace();};
	}

	private void parseAccountPacket(String packet)
	{
		switch(packet.charAt(1))
		{
			case 'A':
				String[] infos = packet.substring(2).split("\\|");
				if(SQLManager.persoExist(infos[0]))
				{
					SocketManager.GAME_SEND_NAME_ALREADY_EXIST(_out);
					return;
				}
				//Validation du nom du personnage
				boolean isValid = true;
				String name = infos[0].toLowerCase();
				//Vérifie d'abord si il contient des termes définit
				if(name.length() > 20
						|| name.contains("mj")
						|| name.contains("modo")
						|| name.contains("admin"))
				{
					isValid = false;
				}
				//Si le nom passe le test, on vérifie que les caractère entré sont correct.
				if(isValid)
				{
					int tiretCount = 0;
					for(char curLetter : name.toCharArray())
					{
						if(!(	(curLetter >= 'a' && curLetter <= 'z')
								|| curLetter == '-'))
						{
							isValid = false;
							break;
						}
						if(curLetter == '-')
						{
							if(tiretCount >= 2)
							{
								isValid = false;
								break;
							}
							else
							{
								tiretCount++;
							}
						}
					}
				}
				//Si le nom est invalide
				if(!isValid)
				{
					SocketManager.GAME_SEND_NAME_ALREADY_EXIST(_out);
					return;
				}
				if(_compte.GET_PERSO_NUMBER() >= Ancestra.CONFIG_MAX_PERSOS)
				{
					SocketManager.GAME_SEND_CREATE_PERSO_FULL(_out);
					return;
				}
				if(_compte.createPerso(infos[0], Integer.parseInt(infos[2]), Integer.parseInt(infos[1]), Integer.parseInt(infos[3]),Integer.parseInt(infos[4]), Integer.parseInt(infos[5])))
				{
					SocketManager.GAME_SEND_CREATE_OK(_out);
					SocketManager.GAME_SEND_PERSO_LIST(_out, _compte.get_persos());
				}else
				{
					SocketManager.GAME_SEND_CREATE_FAILED(_out);
				}
				
			break;
			
			case 'B':
				int stat = -1;
				try
				{
					stat = Integer.parseInt(packet.substring(2).split("/u000A")[0]);
					_perso.boostStat(stat);
				}catch(NumberFormatException e){return;};
			break;
			case 'D':
				String[] split = packet.substring(2).split("\\|");
				int GUID = Integer.parseInt(split[0]);
				String reponse = split.length>1?split[1]:"";
				
				if(_compte.get_persos().containsKey(GUID))
				{
					if(_compte.get_persos().get(GUID).get_lvl() <20 ||(_compte.get_persos().get(GUID).get_lvl() >=20 && reponse.equals(_compte.get_reponse())))
					{
						_compte.deletePerso(GUID);
						SocketManager.GAME_SEND_PERSO_LIST(_out, _compte.get_persos());
					}
					else
						SocketManager.GAME_SEND_DELETE_PERSO_FAILED(_out);
				}else
					SocketManager.GAME_SEND_DELETE_PERSO_FAILED(_out);
			break;
			
			case 'f':
				int queueID = 1;
				int position = 1;
				SocketManager.MULTI_SEND_Af_PACKET(_out,position,1,1,0,queueID);
			break;
			
			case 'i':
				_compte.setClientKey(packet.substring(2));
			break;
			
			case 'L':
				SocketManager.GAME_SEND_PERSO_LIST(_out, _compte.get_persos());
				//SocketManager.GAME_SEND_HIDE_GENERATE_NAME(_out);
			break;
			
			case 'S':
				int charID = Integer.parseInt(packet.substring(2));
				if(_compte.get_persos().get(charID) != null)
				{
					_compte.setGameThread(this);
					_perso = _compte.get_persos().get(charID);
					if(_perso != null)
					{
						_perso.OnJoinGame();
						return;
					}
				}
				SocketManager.GAME_SEND_PERSO_SELECTION_FAILED(_out);
			break;
				
			case 'T':
				int guid = Integer.parseInt(packet.substring(2));
				_compte = Ancestra.gameServer.getWaitingCompte(guid);
				if(_compte != null)
				{
					String ip = _s.getInetAddress().getHostAddress();
					
					_compte.setGameThread(this);
					_compte.setCurIP(ip);
					Ancestra.gameServer.delWaitingCompte(_compte);
					SocketManager.GAME_SEND_ATTRIBUTE_SUCCESS(_out);
				}else
				{
					SocketManager.GAME_SEND_ATTRIBUTE_FAILED(_out);
				}
			break;
			
			case 'V':
				SocketManager.GAME_SEND_AV0(_out);
			break;
			
			case 'P':
				SocketManager.REALM_SEND_REQUIRED_APK(_out);
				break;
		}
	}

	public Thread getThread()
	{
		return _t;
	}

	public void removeAction(GameAction GA)
	{
		//* DEBUG
		System.out.println("Supression de la GameAction id = "+GA._id);
		//*/
		_actions.remove(GA._id);
	}
	
	public void addAction(GameAction GA)
	{
		_actions.put(GA._id, GA);
		//* DEBUG
		System.out.println("Ajout de la GameAction id = "+GA._id);
		System.out.println("Packet: "+GA._packet);
		//*/
	}

}

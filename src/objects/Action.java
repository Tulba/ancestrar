package objects;

import java.io.PrintWriter;

import objects.NPC_tmpl.NPC_question;
import objects.Objet.ObjTemplate;

import common.ConditionParser;
import common.Constants;
import common.Formulas;
import common.SQLManager;
import common.SocketManager;
import common.World;

import game.GameServer;

public class Action {

	private int ID;
	private String args;
	private String cond;
	
	public Action(int id, String args, String cond)
	{
		this.ID = id;
		this.args = args;
		this.cond = cond;
	}


	public void apply(Personnage perso, int itemID)
	{
		if(perso == null)return;
		if(!cond.equalsIgnoreCase("") && !cond.equalsIgnoreCase("-1")&& !ConditionParser.validConditions(perso,cond))
		{
			SocketManager.GAME_SEND_Im_PACKET(perso, "119");
			return;
		}
		PrintWriter out = perso.get_compte().getGameThread().get_out();	
		switch(ID)
		{
			case -2://cr�er guilde
				if(perso.is_away())return;
				if(perso.get_guild() != null || perso.getGuildMember() != null)
				{
					SocketManager.GAME_SEND_gC_PACKET(perso, "Ea");
					return;
				}
				SocketManager.GAME_SEND_gn_PACKET(perso);
			break;
			case -1://Ouvrir banque
				//Sauvagarde du perso et des item avant.
				SQLManager.SAVE_PERSONNAGE(perso,true);
				int cost = perso.getBankCost();
				if(cost > 0)
				{
					long nKamas = perso.get_kamas() - cost;
					if(nKamas <0)//Si le joueur n'a pas assez de kamas pour ouvrir la banque
					{
						SocketManager.GAME_SEND_MESSAGE(perso, "Vous n'avez pas assez de kamas pour ouvrir la banque.", "009900");
						return;
					}
					perso.set_kamas(nKamas);
					SocketManager.GAME_SEND_STATS_PACKET(perso);
					SocketManager.GAME_SEND_Im_PACKET(perso, "020;"+cost);
				}
				SocketManager.GAME_SEND_ECK_PACKET(perso.get_compte().getGameThread().get_out(), 5, "");
				SocketManager.GAME_SEND_EL_BANK_PACKET(perso);
				perso.set_away(true);
				perso.setInBank(true);
			break;
			
			case 0://T�l�portation
				try
				{
					short newMapID = Short.parseShort(args.split(",",2)[0]);
					int newCellID = Integer.parseInt(args.split(",",2)[1]);

					perso.teleport(newMapID,newCellID);	
				}catch(Exception e ){return;};
			break;
			
			case 1://Discours NPC
				out = perso.get_compte().getGameThread().get_out();
				if(args.equalsIgnoreCase("DV"))
				{
					SocketManager.GAME_SEND_END_DIALOG_PACKET(out);
					perso.set_isTalkingWith(0);
				}else
				{
					int qID = -1;
					try
					{
						qID = Integer.parseInt(args);
					}catch(NumberFormatException e){};
					
					NPC_question  quest = World.getNPCQuestion(qID);
					if(quest == null)
					{
						SocketManager.GAME_SEND_END_DIALOG_PACKET(out);
						perso.set_isTalkingWith(0);
						return;
					}
					SocketManager.GAME_SEND_QUESTION_PACKET(out, quest.parseToDQPacket(perso));
				}
			break;
			
			case 4://Kamas
				try
				{
					int count = Integer.parseInt(args);
					long curKamas = perso.get_kamas();
					long newKamas = curKamas + count;
					if(newKamas <0) newKamas = 0;
					perso.set_kamas(newKamas);
					
					//Si en ligne (normalement oui)
					if(perso.isOnline())
						SocketManager.GAME_SEND_STATS_PACKET(perso);
				}catch(Exception e){GameServer.addToLog(e.getMessage());};
			break;
			case 5://objet
				try
				{
					int tID = Integer.parseInt(args.split(",")[0]);
					int count = Integer.parseInt(args.split(",")[1]);
					boolean send = true;
					if(args.split(",").length >2)send = args.split(",")[2].equals("1");
					
					//Si on ajoute
					if(count > 0)
					{
						ObjTemplate T = World.getObjTemplate(tID);
						if(T == null)return;
						Objet O = T.createNewItem(count, false);
						//Si retourne true, on l'ajoute au monde
						if(perso.addObjet(O, true))
							World.addObjet(O, true);
					}else
					{
						perso.removeByTemplateID(tID,-count);
					}
					//Si en ligne (normalement oui)
					if(perso.isOnline())//on envoie le packet qui indique l'ajout//retrait d'un item
					{
						SocketManager.GAME_SEND_Ow_PACKET(perso);
						if(send)
						{
							if(count >= 0){
								SocketManager.GAME_SEND_Im_PACKET(perso, "021;"+count+"~"+tID);
							}
							else if(count < 0){
								SocketManager.GAME_SEND_Im_PACKET(perso, "022;"+-count+"~"+tID);
							}
						}
					}
				}catch(Exception e){GameServer.addToLog(e.getMessage());};
			break;
			case 6://Apprendre un m�tier
				try
				{
					int mID = Integer.parseInt(args);
					if(World.getMetier(mID) == null)return;
					// Si c'est un m�tier 'basic' :
					if(mID == 	2 || mID == 11 ||
					   mID == 13 || mID == 14 ||
					   mID == 15 || mID == 16 ||
					   mID == 17 || mID == 18 ||
					   mID == 19 || mID == 20 ||
					   mID == 24 || mID == 25 ||
					   mID == 26 || mID == 27 ||
					   mID == 28 || mID == 31 ||
					   mID == 36 || mID == 41 ||
					   mID == 56 || mID == 58 ||
					   mID == 60 || mID == 65)
					{
						//On compte les m�tiers d�ja acquis si c'est sup�rieur a 2 on ignore
						if(perso.totalJobBasic() > 2)
						{
							SocketManager.GAME_SEND_MESSAGE(perso, "Vous ne pouvez pas apprendre plus de 3 metiers.", "009900");
						}
						//Si c'est < ou = � 2 on apprend
						else
						{
							perso.learnJob(World.getMetier(mID));
						}
					}
					// Si c'est une specialisations 'FM' :
					if(mID == 	43 || mID == 44 ||
					   mID == 45 || mID == 46 ||
					   mID == 47 || mID == 48 ||
					   mID == 49 || mID == 50 ||
					   mID == 62 || mID == 63 ||
					   mID == 64)
					{
						//M�tier simple level 60 n�cessaire
						if(perso.getMetierByID(17) != null && perso.getMetierByID(17).get_lvl() >= 60 && mID == 43
						|| perso.getMetierByID(11) != null && perso.getMetierByID(11).get_lvl() >= 60 && mID == 44
						|| perso.getMetierByID(14) != null && perso.getMetierByID(14).get_lvl() >= 60 && mID == 45
						|| perso.getMetierByID(20) != null && perso.getMetierByID(20).get_lvl() >= 60 && mID == 46
						|| perso.getMetierByID(31) != null && perso.getMetierByID(31).get_lvl() >= 60 && mID == 47
						|| perso.getMetierByID(13) != null && perso.getMetierByID(13).get_lvl() >= 60 && mID == 48
						|| perso.getMetierByID(19) != null && perso.getMetierByID(19).get_lvl() >= 60 && mID == 49
						|| perso.getMetierByID(18) != null && perso.getMetierByID(18).get_lvl() >= 60 && mID == 50
						|| perso.getMetierByID(15) != null && perso.getMetierByID(15).get_lvl() >= 60 && mID == 62
						|| perso.getMetierByID(16) != null && perso.getMetierByID(16).get_lvl() >= 60 && mID == 63
						|| perso.getMetierByID(27) != null && perso.getMetierByID(27).get_lvl() >= 60 && mID == 64)
						{
							//On compte les specialisations d�ja acquis si c'est sup�rieur a 2 on ignore
							if(perso.totalJobFM() > 2)
							{
								SocketManager.GAME_SEND_MESSAGE(perso, "Vous ne pouvez pas apprendre plus de 3 specialisations.", "009900");
							}
							//Si c'est < ou = � 2 on apprend
							else
							{
								perso.learnJob(World.getMetier(mID));
								perso.getMetierByID(mID).addXp(perso, 582000);//Level 100 direct
							}	
						}else
						{
							SocketManager.GAME_SEND_MESSAGE(perso, "Vous ne pouvez pas apprendre cette specialisation (Niveau de metier insuffisant).", "009900");
						}
					}
				}catch(Exception e){GameServer.addToLog(e.getMessage());};
			break;
			case 7://retour au point de sauvegarde
				perso.warpToSavePos();
			break;
			case 8://Ajouter une Stat
				try
				{
					int statID = Integer.parseInt(args.split(",",2)[0]);
					int number = Integer.parseInt(args.split(",",2)[1]);
					perso.get_baseStats().addOneStat(statID, number);
					SocketManager.GAME_SEND_STATS_PACKET(perso);
					int messID = 0;
					switch(statID)
					{
						case Constants.STATS_ADD_INTE: messID = 14;break;
					}
					if(messID>0)
						SocketManager.GAME_SEND_Im_PACKET(perso, "0"+messID+";"+number);
				}catch(Exception e ){return;};
			break;
			case 9://Apprendre un sort
				try
				{
					int sID = Integer.parseInt(args);
					if(World.getSort(sID) == null)return;
					perso.learnSpell(sID,1, true,true);
				}catch(Exception e){GameServer.addToLog(e.getMessage());};
			break;
			case 10://Pain/potion/viande/poisson
				try
				{
					int min = Integer.parseInt(args.split(",",2)[0]);
					int max = Integer.parseInt(args.split(",",2)[1]);
					if(max == 0) max = min;
					int val = Formulas.getRandomValue(min, max);
					if(perso.get_PDV() + val > perso.get_PDVMAX())val = perso.get_PDVMAX()-perso.get_PDV();
					perso.set_PDV(perso.get_PDV()+val);
					SocketManager.GAME_SEND_STATS_PACKET(perso);
				}catch(Exception e){GameServer.addToLog(e.getMessage());};
			break;
			case 11://Definir l'alignement
				try
				{
					byte newAlign = Byte.parseByte(args.split(",",2)[0]);
					boolean replace = Integer.parseInt(args.split(",",2)[1]) == 1;
					//Si le perso n'est pas neutre, et qu'on doit pas remplacer, on passe
					if(perso.get_align() != Constants.ALIGNEMENT_NEUTRE && !replace)return;
					perso.modifAlignement(newAlign);
				}catch(Exception e){GameServer.addToLog(e.getMessage());};
			break;
			/* TODO: autres actions */
			case 12://Spawn d'un groupe de monstre
				try
				{
					boolean delObj = args.split(",")[0].equals("true");
					boolean inArena = args.split(",")[1].equals("true");

					if(inArena && !World.isArenaMap(perso.get_curCarte().get_id()))return;	//Si la map du personnage n'est pas class� comme �tant dans l'ar�ne

					PierreAme pierrePleine = (PierreAme)World.getObjet(itemID);

					String groupData = pierrePleine.parseGroupData();
					String condition = "MiS = "+perso.get_GUID();	//Condition pour que le groupe ne soit lan�able que par le personnage qui � utiliser l'objet
					perso.get_curCarte().spawnNewGroup(true, perso.get_curCell().getID(), groupData,condition);

					if(delObj)
					{
						perso.removeItem(itemID, 1, true, true);
					}
				}catch(Exception e){GameServer.addToLog(e.getMessage());};
			break;
		    case 13: //Reset Carac
		        try
		        {
		          perso.get_baseStats().addOneStat(125, -perso._baseStats.getEffect(125));
		          perso.get_baseStats().addOneStat(124, -perso._baseStats.getEffect(124));
		          perso.get_baseStats().addOneStat(118, -perso._baseStats.getEffect(118));
		          perso.get_baseStats().addOneStat(123, -perso._baseStats.getEffect(123));
		          perso.get_baseStats().addOneStat(119, -perso._baseStats.getEffect(119));
		          perso.get_baseStats().addOneStat(126, -perso._baseStats.getEffect(126));
		          perso.addCapital((perso.get_lvl() - 1) * 5 - perso.get_capital());

		          SocketManager.GAME_SEND_STATS_PACKET(perso);
		        }catch(Exception e){GameServer.addToLog(e.getMessage());};
		    break;
		    case 14://Ouvrir l'interface d'oublie de sort
				perso.setisForgetingSpell(true);
				SocketManager.GAME_SEND_FORGETSPELL_INTERFACE('+', perso);
			break;
			case 15://T�l�portation donjon
				try
				{
					short newMapID = Short.parseShort(args.split(",")[0]);
					int newCellID = Integer.parseInt(args.split(",")[1]);
					int ObjetNeed = Integer.parseInt(args.split(",")[2]);
					int MapNeed = Integer.parseInt(args.split(",")[3]);;
					if(ObjetNeed == 0)
					{
						//T�l�portation sans objets
						perso.teleport(newMapID,newCellID);
					}else if(ObjetNeed > 0)
					{
					if(MapNeed == 0)
					{
						//T�l�portation sans map
						perso.teleport(newMapID,newCellID);
					}else if(MapNeed > 0)
					{
					if (perso.hasItemTemplate(ObjetNeed, 1) && perso.get_curCarte().get_id() == MapNeed)
					{
						//Le perso a l'item
						//Le perso est sur la bonne map
						//On t�l�porte, on supprime apr�s
						perso.teleport(newMapID,newCellID);
						perso.removeByTemplateID(ObjetNeed, 1);
						SocketManager.GAME_SEND_Ow_PACKET(perso);
					}
					else if(perso.get_curCarte().get_id() != MapNeed)
					{
						//Le perso n'est pas sur la bonne map
						SocketManager.GAME_SEND_MESSAGE(perso, "Vous n'etes pas sur la bonne map du donjon pour etre teleporter.", "009900");
					}
					else
					{
						//Le perso ne poss�de pas l'item
						SocketManager.GAME_SEND_MESSAGE(perso, "Vous ne possedez pas la clef necessaire.", "009900");
					}
					}
					}
				}catch(Exception e ){return;};
			break;
			case 16://Ajout d'honneur HonorValue
				if(perso.get_align() != 0)
				{
					int AddHonor = Integer.parseInt(args);
					int ActualHonor = perso.get_honor();
					perso.set_honor(ActualHonor+AddHonor);
				}
			break;
			case 17://Xp m�tier JobID,XpValue
				int JobID = Integer.parseInt(args.split(",")[0]);
				int XpValue = Integer.parseInt(args.split(",")[1]);
				if(perso.getMetierByID(JobID) != null)
				{
					perso.getMetierByID(JobID).addXp(perso, XpValue);
				}
			break;
			case 18://T�l�portation chez sois
				if(House.AlreadyHaveHouse(perso))//Si il a une maison
				{
					Objet obj = World.getObjet(itemID);
					if (perso.hasItemTemplate(obj.getTemplate().getID(), 1))
					{
						perso.removeByTemplateID(obj.getTemplate().getID(),1);
						House.HouseCoordByPerso(perso);
						perso.teleport(House.isMapID, House.isCellID);
					}
				}
			break;
			case 19://T�l�portation maison de guilde (ouverture du panneau de guilde)
				//TODO : Il manque un paquet pour ouvrir la panneau de guilde. /!\ Non fonctionnel
				/*
				SocketManager.GAME_SEND_gIG_PACKET(perso, perso.get_guild());
				SocketManager.GAME_SEND_gIM_PACKET(perso, perso.get_guild(), '+');
				SocketManager.GAME_SEND_gIH_PACKET(perso, House.parseHouseToGuild(perso));
				*/
			break;
			case 20://+Points de sorts
				int pts = Integer.parseInt(args);
				if(pts < 1) return;
				perso.addSpellPoint(pts);
				SocketManager.GAME_SEND_STATS_PACKET(perso);
			break;
			default:
				GameServer.addToLog("Action ID="+ID+" non implant�e");
			break;
		}
	}


	public int getID()
	{
		return ID;
	}
}

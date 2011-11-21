package objects;

import java.util.Map;
import java.util.TreeMap;

import common.*;
import realm.RealmThread;


public class Account {

	private int _GUID;
	private String _name;
	private String _pass;
	private String _pseudo;
	private String _lastIP = "";
	private String _question;
	private String _reponse;
	private boolean _banned = false;
	private int _gmLvl = 0;
	private int _subscriber = 0;//Time en minute
	private String _curIP = "";
	private String _lastConnectionDate = "";
	
	private RealmThread _realmThread = null;
	private Map<Integer, Integer> _persos = new TreeMap<Integer, Integer>();
	
	public Account(int aGUID,String aName,String aPass, String aPseudo,String aQuestion,String aReponse,int aGmLvl, int subscriber, boolean aBanned, String aLastIp, String aLastConnectionDate)
	{
		this._GUID 					= aGUID;
		this._name 					= aName;
		this._pass					= aPass;
		this._pseudo 				= aPseudo;
		this._question				= aQuestion;
		this._reponse				= aReponse;
		this._gmLvl					= aGmLvl;
		this._subscriber			= subscriber;
		this._banned				= aBanned;
		this._lastIP				= aLastIp;
		this._lastConnectionDate 	= aLastConnectionDate;
	}
	
	public void addPersonnage(int guid)
	{
		if (_persos.get(guid) == null)
			_persos.put(guid, 1);
		else
		{
			int g = _persos.get(guid);
			_persos.remove(guid);
			_persos.put(guid, g++);
		}
	}
	
	public int getNumberPersosOnThisServer(int guid)
	{
		if (_persos.get(guid) == null)
			return 0;
		return _persos.get(guid);
	}
	
	public String parseToStringNumber()
	{
		StringBuilder toreturn = new StringBuilder();
		for (GameServer G : Realm.GameServers.values())
		{
			if (_persos.get(G.getID()) == null)
				continue;
			toreturn.append(G.getID()).append(",").append(_persos.get(G.getID())).append(";");
		}
		toreturn.substring(toreturn.length()-1, toreturn.length());
		return toreturn.toString();
	}
	
	public void setCurIP(String ip)
	{
		_curIP = ip;
	}
	
	public String getLastConnectionDate() 
	{
		return _lastConnectionDate;
	}
	
	public void setLastIP(String _lastip) 
	{
		_lastIP = _lastip;
	}
	
	public String getLastIP()
	{
		return _lastIP;
	}
	
	public void setLastConnectionDate(String connectionDate) 
	{
		_lastConnectionDate = connectionDate;
	}
	
	public void setRealmThread(RealmThread thread)
	{
		_realmThread = thread;
	}
	
	public RealmThread getRealmThread()
	{
		return _realmThread;
	}
	
	public boolean isValidPass(String pass, String hash) 
	{
		return pass.equals(CryptManager.CryptPassword(hash, _pass));
	}
	
	public int get_GUID() 
	{
		return _GUID;
	}
	
	public String get_name() 
	{
		return _name;
	}
	
	public String get_pass() 
	{
		return _pass;
	}
	
	public String get_pseudo() 
	{
		return _pseudo;
	}
	
	public int get_subscriber() 
	{
		return _subscriber;
	}
	
	public String get_lastIP() 
	{
		return _lastIP;
	}
	
	public String get_question() 
	{
		return _question;
	}
	
	public String get_reponse() 
	{
		return _reponse;
	}
	
	public boolean isBanned() 
	{
		return _banned;
	}
	
	public void setBanned(boolean banned) 
	{
		_banned = banned;
	}
	
	public int get_gmLvl() 
	{
		return _gmLvl;
	}
	
	public String get_curIP() 
	{
		return _curIP;
	}
	
	public void setGmLvl(int gmLvl)
	{
		_gmLvl = gmLvl;
	}
}

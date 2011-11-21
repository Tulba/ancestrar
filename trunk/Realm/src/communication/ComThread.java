package communication;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;



import objects.Account;
import objects.GameServer;
import common.Ancestra;
import common.SQLManager;
import common.Realm;


public class ComThread implements Runnable {
	private BufferedReader _in;
	private Thread _t;
	private PrintWriter _out;
	private Socket _s;
	private GameServer _server = null;

	public ComThread(Socket sock) 
	{
		try
		{
			_s = sock;
			_in = new BufferedReader(new InputStreamReader(_s.getInputStream()));
			_out = new PrintWriter(_s.getOutputStream());
			_t = new Thread(this);
			_t.setDaemon(true);
			_t.start();
		}catch(IOException e)
		{
			try
			{
				if (!_s.isClosed())
					_s.close();
			}catch(IOException e1){}
		}finally
		{
			
		}
	}
	
	public void sendDeco(int guid)
	{
		System.out.println("ComThread: Send>>LO"+guid);
		System.out.println("ComThread : Envoi du paquet de LoginOut ...");
		try
		{
			_out.print("LO"+guid+(char)0x00);//LogOut
			_out.flush();
			System.out.println("ComThread : Envoi OK.");
		}catch(Exception e)
		{
			System.out.println("ComThread : Erreur d'envoi.");
		}
	}
	
	public void sendAddWaiting(String str)
	{
		System.out.println("ComThread: Send>>AW"+str);
		System.out.println("ComThread : Envoi du paquet d'ajout de compte.");
		try
		{
			_out.print("AW"+str+(char)0x00);//AddWaiting
			_out.flush();
			System.out.println("ComThread : Envoi OK.");
		}catch(Exception e)
		{
			System.out.println("ComThread : Erreur d'envoi.");
		}
	}
	
	public void parsePacket(String packet)
	{
		switch (packet.charAt(0))
		{
		case 'G'://Game
			switch (packet.charAt(1))
			{
			case 'A'://Add
				Ancestra.addToComLog("ComThread : Packet GA recu, ajout d'un serveur...");
				System.out.println("ComThread : Packet GA recu, ajout d'un serveur...");
				String key = packet.substring(2);
				Ancestra.addToComLog("ComThread : Serveur KEY : "+key);
				System.out.println("ComThread : Serveur KEY : "+key);
				for(GameServer G : Realm.GameServers.values())
				{
					if(key.equalsIgnoreCase(G.getKey()))
						_server = G;
				}
				if(_server == null)
				{
					kick();
					return;
				}
				_server.setThread(this);
				_server.setState(1);
				Ancestra.addToComLog("ComThread : Serveur OK!");
				System.out.println("ComThread : Serveur OK!");
			break;
			}
		break;
		case 'S'://Server
			if (_server == null)
			{
				kick();
				return;
			}
			switch (packet.charAt(1))
			{
			case 'O'://Open
				Ancestra.addToComLog("ComThread : Packet SO recu, changement d'etat : 1.");
				_server.setState(1);
			break;
			case 'S'://Save
				Ancestra.addToComLog("ComThread : Packet SS recu, changement d'etat : 2.");
				_server.setState(2);
			break;
			case 'D'://Disconnected
				Ancestra.addToComLog("ComThread : Packet SD recu, changement d'etat : 0.");
				_server.setState(0);
			break;
			}
		break;
		case 'R'://RealmThread
			if (_server == null)
			{
				kick();
				return;
			}
			switch (packet.charAt(1))
			{
			case 'G'://GMLEVEL BLOCK, arg : int[level]
				Ancestra.addToComLog("ComThread : Packet RG recu, blocage du serveur au GMlevels < "+Integer.parseInt(packet.substring(2)));
				_server.setBlockLevel(Integer.parseInt(packet.substring(2)));
			break;
			case 'A'://ADD BANIP, arg : String[ip]
				Ancestra.addToComLog("ComThread : Packet RA recu, ban de l'IP : "+packet.substring(2));
				SQLManager.ADD_BANIP(packet.substring(2));
				Realm.BAN_IP += packet.substring(2)+",";
			break;
			}
		break;
		}
		for (Account r : Realm.getAccountsMap().values())
		{
			r.getRealmThread().refresh();
		}
		
		
	}
	
	public void run() 
	{
		try
		{
			String packet = "";
			char charCur[] = new char[1];
			
			while(_in.read(charCur, 0, 1)!=-1 && Ancestra.isRunning)
	    	{
				if (charCur[0] != '\u0000' && charCur[0] != '\n' && charCur[0] != '\r')
		    	{
	    			packet += charCur[0];
		    	}else if(!packet.isEmpty())
		    	{
		    		if(Ancestra.REALM_DEBUG) System.out.println("ComThread: Recv << "+packet);
		    		parsePacket(packet);
		    		packet = "";
		    	}
	    	}
		}catch(IOException e)
		{
			try
			{
				_in.close();
				_out.close();
				
				if (!_s.isClosed())_s.close();
				_t.interrupt();
			}catch(IOException e1){}
		}finally
		{
			try
			{
				_in.close();
				_out.close();
				kick();
				if (!_s.isClosed())_s.close();
				_t.interrupt();
			}catch(IOException e1){}
		}
	}
	
	public void kick()
	{
		try
		{
			Ancestra.addToComLog("ComThread : Server kicked by the Realm.");
			System.out.println("ComThread : Server kicked by the Realm.");
			_in.close();
			_out.close();
			if (_server != null)
			{	
				_server.setState(0);
				Ancestra.addToComLog("ComThread : Server Kicked.");
				System.out.println("ComThread : Server Kicked.");
			for (Account acc : Realm.getAccountsMap().values())
			{
				if(acc.getRealmThread() == null) continue;
				acc.getRealmThread().refresh();
			}
				_server.setThread(null);
			}
			if (!_s.isClosed())
				_s.close();
		}catch(IOException e)
		{
			System.out.println("ComThreadKick : "+e.getMessage());
			Ancestra.addToComLog("ComThreadKick : "+e.getMessage());
		}
	}
}
package communication;

import java.net.*;
import java.io.*;

import objects.Compte;
import common.Ancestra;
import common.World;
 
public class ComServer implements Runnable {
 
	private Socket _s;
	private BufferedReader _in;
	private PrintWriter _out;
	private Thread _t;
 
        
        public ComServer() {
        	try {
    			_s = new Socket(Ancestra.REALM_IP, Ancestra.COM_PORT);
    			_t = new Thread(this);
    			_t.setDaemon(true);
    			_t.start();
    			_in = new BufferedReader(new InputStreamReader(_s.getInputStream()));
    			_out = new PrintWriter(_s.getOutputStream());
    		} catch (Exception e) {
        		System.out.println("\nComServer : Connection au Realm impossible");
        		System.out.println(e.getMessage());
        		Ancestra.com_Running = false;
        		Ancestra.try_ComServer();
    		}
        }
        
        public void run() {
        	try{
        		String packet ="";
        		char[] charCur = new char[1];
        		Ancestra.com_Running = true;
        		try{
        			_out.print("GA"+Ancestra.AUTH_KEY+(char)0x00);
        			_out.flush();
        		}catch (Exception e)
        		{
        			try {
						Thread.sleep(1000);
						System.out.println("ComServer : Erreur d'envoi du GA, renvoi ...");
						_out.print("GA"+Ancestra.AUTH_KEY+(char)0x00);
						_out.flush();
					}catch (Exception e1)
					{
						System.out.println("ComServer : Erreur d'envoi du GA : "+e1.getMessage());
						Ancestra.com_Running = false;
						Ancestra.closeServers();
					}
        		}
        		while (_in.read(charCur, 0, 1) != -1 && Ancestra.isRunning)
        		{
        			if (charCur[0] != '\u0000' && charCur[0] != '\n' && charCur[0] != '\r')
        	    	{
            			packet += charCur[0];
            		
        	    	}else if(!packet.isEmpty())
        	    	{
        	    		if(Ancestra.CONFIG_DEBUG) System.out.println("Exchange: Recv << "+packet);
        	    		parsePacket(packet);
        	    		packet = "";
        	    	}
        		}
        	}catch(IOException e)
        	{
        		System.out.println("\nComServer : Serveur d'echange inlancable");
        		System.out.println(e.getMessage());
        		Ancestra.com_Running = false;
        		Ancestra.try_ComServer();
        	}
        }
        
    	public void sendChangeState(char c)
    	{
    		_out.print("S"+c+(char)0x00);
    		_out.flush();
    	}
    	
    	public void addBanIP(String ip)
    	{
    		_out.print("RA"+ip+(char)0x00);
    		_out.flush();
    	}
    	
    	public void lockGMlevel(int level)
    	{
    		_out.print("RG"+level+(char)0x00);
    		_out.flush();
    	}
    	
    	public void sendGetOnline(String str)
    	{
    		_out.print("GO"+str+(char)0x00);
    		_out.flush();
    	}
        
        public void parsePacket(String packet)
        {
        	switch (packet.charAt(0))
        	{
        	case 'A'://ADD
        		switch(packet.charAt(1))
        		{
        		case 'W'://WAITING
        			Compte acc = null;
        			System.out.println("Ajout d'un compte au GameThread ...");
        			try
        			{
        				String[] AD = packet.substring(2).split("\\|");
    					acc = new Compte(Integer.parseInt(AD[0]), AD[1], AD[2], AD[3], AD[4], AD[5], Integer.parseInt(AD[6]), Integer.parseInt(AD[7]), (Integer.parseInt(AD[8])!=0?true:false), AD[9], AD[10], AD[11]);
        			}catch (Exception e) 
        			{
        				System.out.println("Creation du compte echouee :"+e.getMessage());
        			}
        			if(acc != null && Ancestra.gameServer.getWaitingCompte(acc.get_GUID()) == null)
        			{
        				System.out.println("Ajout du compte");
        				Ancestra.gameServer.addWaitingCompte(acc);
        			}else if(acc != null && Ancestra.gameServer.getWaitingCompte(acc.get_GUID()) != null)
        			{
        				System.out.println("Supression du compte");
        				Ancestra.gameServer.delWaitingCompte(acc);
        				System.out.println("Ajout du compte");
        				Ancestra.gameServer.addWaitingCompte(acc);
        			}
        			System.out.println("Ajout d'un compte au GameThread Termine");
        		break;
        		}
        	break;
        	case 'L'://LOG
        		switch(packet.charAt(1))
        		{
	        		case 'O'://OUT
	        			int guid = Integer.parseInt(packet.substring(2));
	        			Compte acc = World.getCompte(guid);
	        			System.out.println("Verification connexion GameThread ...");
	        			if (acc != null)
	        			{
	        				System.out.println("Compte existant, on le kick");
	        				if(acc.getGameThread() != null)
	        					acc.getGameThread().kick();
	        			}
	        			System.out.println("Verification connexion GameThread Termine");
	        		break;
        		}
        	break;
        	case 'G'://GET
        		switch(packet.charAt(1))
        		{
	        		case 'O'://ONLINE
	        			String data = Ancestra.CONFIG_PLAYER_LIMIT+";"+World.getComptes().size();
	        			sendGetOnline(data);
	        		break;
        		}
        	break;
        	}
        }
}
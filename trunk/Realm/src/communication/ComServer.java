package communication;

import java.io.IOException;
import java.net.ServerSocket;

import common.Ancestra;

public class ComServer implements Runnable {
	
	private ServerSocket _SS;
	private Thread _t;
	
	public ComServer() 
	{
		try
		{
			_SS = new ServerSocket(Ancestra.REALM_COM_PORT);
			_t = new Thread(this);
			_t.setDaemon(true);
			_t.start();
		}catch(IOException e)
		{
			System.out.println("ComServer : "+e.getMessage());
			Ancestra.addToErrorLog("ComServer : "+e.getMessage());
			Ancestra.closeServers();
		}
	}
	
	public void run() 
	{
		while(Ancestra.isRunning)// bloque sur _SS.accept()
		{
			try
			{
				new ComThread(_SS.accept());
			}catch(IOException e)
			{
				System.out.println("ComServerRun : "+e.getMessage());
			}
		}
	}
	
	public void kickAll()
	{
		try
		{
			_SS.close();
		}catch(Exception e)
		{
			System.out.println("ComServerKickAll : "+e.getMessage());
		}
	}
	
	public Thread getThread() 
	{
		return _t;
	}
}

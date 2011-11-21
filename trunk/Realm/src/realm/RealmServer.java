package realm;

import java.io.IOException;
import java.net.ServerSocket;
import common.Ancestra;

public class RealmServer implements Runnable {

	private ServerSocket _SS;
	private Thread _t;

	public RealmServer()
	{
		try
		{
			_SS = new ServerSocket(Ancestra.REALM_PORT);
			_t = new Thread(this);
			_t.setDaemon(true);
			_t.start();
		}catch(IOException e)
		{
			System.out.println("RealmServer : "+e.getMessage());
			Ancestra.addToErrorLog("RealmServer : "+e.getMessage());
			Ancestra.closeServers();
		}
	}

	public void run()
	{
		while(Ancestra.isRunning)
		{
			try
			{
				new RealmThread(_SS.accept());
			}catch(IOException e)
			{
				System.out.println("RealmServerRun : "+e.getMessage());
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
			System.out.println("RealmServerKickAll : "+e.getMessage());
		}
	}

	public Thread getThread()
	{
		return _t;
	}
}

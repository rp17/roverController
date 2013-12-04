package rover.netclient;

import java.io.IOException;
import java.net.UnknownHostException;

public interface IPIDClient extends Runnable {
	public boolean serverConnect(String serverIP, int port) throws UnknownHostException, IOException;
}


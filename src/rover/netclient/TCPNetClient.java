package rover.netclient;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.net.InetAddress;
import java.net.DatagramSocket;
import java.net.Socket;
import java.net.DatagramPacket;
import java.net.UnknownHostException;

import rover.control.ServoControlActivity;

public class TCPNetClient implements IPIDClient {
	public volatile int SPEED = 1500;
	public volatile int cmd = ServoControlActivity.MANUAL;
	private static final int TIME_OUT = 5000;   // 5 secs
    // timeout used when waiting in receive()
	private static final int PACKET_SIZE = 1024;  // max size of a message
	
	volatile boolean active = true;
	private Socket socket = null;
	private int SERVER_PORT = 5000;
	private String SERVER_IP = null;
	private InetAddress serverAddr = null;
	
	private PrintWriter out;  // output to the server
	private BufferedReader in;
	
	public boolean serverConnect(String serverIP, int port) throws UnknownHostException, IOException {
		SERVER_IP = serverIP;
		SERVER_PORT = port;
		if(SERVER_IP != null) {
			serverAddr = InetAddress.getByName(SERVER_IP);
			socket = new Socket(SERVER_IP, SERVER_PORT);
			socket.setSoTimeout(TIME_OUT);
			in  = new BufferedReader(new InputStreamReader( socket.getInputStream() ) );
	        out = new PrintWriter( socket.getOutputStream(), true );
			return true;
		}
		else return false;
	}
	/*
	void sendServerMessage(String msg)
	  // Send message to NameServer
	  {
		//if(socket == null) return;
	    try {
	      DatagramPacket sendPacket =
	          new DatagramPacket( msg.getBytes(), msg.length(), 
	   						serverAddr, SERVER_PORT);
	      socket.send( sendPacket );
	    }
	    catch(IOException ioe)
	    {  System.out.println(ioe);  }
	  } // end of sendServerMessage()
	  */
	public void run() {
		while(active) {
			/*
			int azimut = PIDNetActivity.avgAzimut;
			sendServerMessage(Integer.toString(azimut));
			try {
				Thread.sleep(10);
			}
			catch(InterruptedException ex){
				active = false;
			}
			*/
		}
		try {
			in.close();
			out.close();
			socket.close();
		}
		catch(IOException ex) {
			System.out.println(ex);
		}
	}
}

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

public class UDPNetClient implements IPIDClient {
	public volatile int SPEED = 1500;
	public volatile int cmd = ServoControlActivity.MANUAL;
	private static final int TIME_OUT = 5000;   // 5 secs
    // timeout used when waiting in receive()
	private static final int PACKET_SIZE = 1024;  // max size of a message
	
	volatile boolean active = true;
	private DatagramSocket socket = null;
	private int SERVER_PORT = 5000;
	private String SERVER_IP = null;
	private InetAddress serverAddr = null;
	
	//private PrintWriter out;  // output to the server
	//private BufferedReader in;
	
	public boolean serverConnect(String serverIP, int port) throws UnknownHostException, IOException {
		SERVER_IP = serverIP;
		SERVER_PORT = port;
		if(SERVER_IP != null) {
			serverAddr = InetAddress.getByName(SERVER_IP);
			socket = new DatagramSocket();
			//socket.setSoTimeout(TIME_OUT);
			
			return true;
		}
		else return false;
	}
	
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
	  
	  private String readServerMessage()
	  /* Read a message sent from the NameServer (when
	     it arrives). There is a time-out associated with receive() */
	  {
	    String msg = null;
	    try {
	      byte[] data = new byte[PACKET_SIZE];
	      DatagramPacket receivePacket = new DatagramPacket(data, data.length);

	      socket.receive( receivePacket );  // wait for a packet
	      msg = new String( receivePacket.getData(), 0,
	                           receivePacket.getLength() );
	    }
	    catch(IOException ioe)
	    {  System.out.println(ioe);  }

	    return msg;
	  }  // end of readServerMessage()
	    
	public void run() {
		while(active) {
			int azimut = ServoControlActivity.avgAzimut;
			sendServerMessage(Integer.toString(azimut));
			/*
			try {
				Thread.sleep(10);
			}
			catch(InterruptedException ex){
				active = false;
			}
			*/
			String msg = readServerMessage();
			if(msg == null) {
				System.out.println("Received null from server");
				continue;
			}
			try {
				cmd = Integer.parseInt(msg);
			}
			catch(NumberFormatException ex) {
				System.out.println(ex.getMessage());
			}
			
			
			
		}
		
			//in.close();
			//out.close();
			socket.close();
	}
}

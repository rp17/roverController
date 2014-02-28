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

import android.os.Bundle;
import android.os.Message;

import rover.control.ServoControlActivity;

public class UDPNetClient implements IPIDClient {
	// public volatile int SPEED = 1500;
	public volatile int cmd = ServoControlActivity.MANUAL;
	public volatile int turn = ServoControlActivity.TURN;
	private static final int TIME_OUT = 5000;   // 5 secs
    // timeout used when waiting in receive()
	private static final int PACKET_SIZE = 1024;  // max size of a message
	
	ServoControlActivity sca; 
	
	volatile boolean active = true;
	DatagramSocket socket = null;
	private int SERVER_PORT = 5000;
	private String SERVER_IP = null;
	private InetAddress serverAddr = null;
	
	//private PrintWriter out;  // output to the server
	//private BufferedReader in;
	
	public UDPNetClient(ServoControlActivity context) {
		sca = context;
	}
	
	public boolean serverConnect(String serverIP, int port) throws UnknownHostException, IOException {
		SERVER_IP = serverIP;
		SERVER_PORT = port;
		if(SERVER_IP != null) {
			serverAddr = InetAddress.getByName(SERVER_IP);
			socket = new DatagramSocket(SERVER_PORT);
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
	          new DatagramPacket(msg.getBytes(), msg.length(), serverAddr, SERVER_PORT);
	      socket.send( sendPacket );
	    
	    }
	    catch(IOException ioe) {  
	    	System.out.println(ioe);  
	    }
	  } // end of sendServerMessage()
	  
	  private String readServerMessage()
	  /* Read a message sent from the NameServer (when
	     it arrives). There is a time-out associated with receive() */
	  {
	    String msg = null;
	    try {
	      byte[] data = new byte[PACKET_SIZE];
	      DatagramPacket receivePacket = new DatagramPacket(data, data.length);

	      socket.receive(receivePacket);  // wait for a packet
	      msg = new String(receivePacket.getData(), 0, receivePacket.getLength());
	    }
	    catch(IOException ioe) {  
	    	System.out.println(ioe);  
	    }

	    return msg;
	  }  // end of readServerMessage()
	    
	public void run() {
		
		while(active) {
			int azimut = ServoControlActivity.avgAzimut;
			
			//double androidLat = ServoControlActivity.android_GPS_Lat;
			//double androidLon = ServoControlActivity.android_GPS_Lon;
			
			//double skyhookLat = ServoControlActivity.skyhook_GPS_Lat;
			//double skyhookLon = ServoControlActivity.skyhook_GPS_Lon;
			
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
			else {
		
				String param[] = msg.split(" ");
				
				// remote command and Speed Request
				if(param.length == 2) {
					
					// set command
					cmd = Integer.parseInt(param[0]);
					
					// set speed
					int s = Integer.parseInt(param[1]);
					sca.setSpeed(s);					
					
					Message mesg = sca.handler.obtainMessage();
					Bundle bundle = new Bundle();
					bundle.putInt("command", cmd);
					bundle.putInt("speed", s);
					mesg.setData(bundle);
					sca.handler.sendMessage(mesg);
					
				}
				// remote command, Speed, and Turn request
				if(param.length == 3) {
					
					// set command
					cmd = Integer.parseInt(param[0]);
					
					// set speed
					int s = Integer.parseInt(param[1]);
					sca.setSpeed(s);
					
					// set turn
					turn = 1;
					int turnPercentage = Integer.parseInt(param[2]);
					sca.setTurn(turnPercentage);
					
					
					Message mesg = sca.handler.obtainMessage();
					Bundle bundle = new Bundle();
					bundle.putInt("command", cmd);
					bundle.putInt("speed", s);
					bundle.putFloat("turn", turnPercentage);
					mesg.setData(bundle);
					sca.handler.sendMessage(mesg);
					
				}
				// only setting to remote command
				else {
					try {
						
						// set command
						cmd = Integer.parseInt(msg);						
						
						Message mesg = sca.handler.obtainMessage();
						Bundle bundle = new Bundle();
						bundle.putInt("command", cmd);
						mesg.setData(bundle);
						sca.handler.sendMessage(mesg);
						
						
					}
					catch(NumberFormatException ex) {
						System.out.println(ex.getMessage());
					}
				}				
			}
		}
		
			//in.close();
			//out.close();
			socket.close();
	}
}

package cl.signpost.narseo.com;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.text.DecimalFormat;
import java.util.Random;

import cl.signpost.narseo.com.TestsSignpost.Messages;

import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

public class SigcommDemoAndroidService extends Service implements Runnable{

	public static final String TAG = "SIGNPOSTSERV";
	private static SigcommDemoAndroidActivity MAIN_ACTIVITY;
	private static PowerManager.WakeLock wl = null;
	private static PowerManager pm =null;
	
	public static final Boolean DEBUG = true;
	
	public static float udpSeqNumber = 0.0f;

	public static final String REFRESH_DATA = "SIGNPOST_VALUE";
	public static final String SHOW_ERROR = "ERROR";
	public static final String ERROR_INTENT = "ERROR_MESSAGE";
	public static final String REFRESH_RTT_INTENT = "LATENCYUPSTREAM";
	public static final String REFRESH_JITTER_INTENT = "JITTER";
	public static final String REFRESH_GOODPUTUPSTREAM_INTENT = "GOODPUTUPSTREAM";
	public static final String REFRESH_GOODPUTDOWNSTREAM_INTENT = "GOODPUTDOWNSTREAM";
	
	
	public static final int RTT_ID = 0;
	public static final int GOODPUT_UPSTREAM_ID = 2;
	public static final int GOODPUT_DOWNSTREAM_ID = 3;
	public static final int JITTER_ID = 4;
	
	public static final String SIGNPOST_SERVER_DOMAIN = "home.d1.signpo.st";
	
	public static long now = 0;
	public static long prev = 0;
	
	//Data stream transmitted to server
	public static final String SEQ_TO_SERVER = "abcdefghijklmnop";
	
	//Used to kill the thread
	public static boolean testAlive = false;

	//Default DevName
	public static String devName = "Android";
	

	public static InetAddress address = null;
	
	//Default values
	//public static int [] SERVER = {192, 168, 1, 1};

	//public static int [] SERVER = {10, 20, 1, 118};
	public static int TCP_PORT = 7777;
	public static int UDP_LOCAL_PORT = 5522;
	

	static SenderThread sender  = null;

	
	/*
	 * Configures parameters and links to main activity
	 */
	public static void setName(String dnsName){
		//Also used to identify server name
		devName = dnsName;
	}

	  
	/*
	 * Configures parameters and links to main activity
	 */
	public static void setMainActivity(SigcommDemoAndroidActivity activity, /*int [] server, */ int tcpPort){
		Log.e(TAG, "Activity added. Task ID: "+activity.getTaskId());
		MAIN_ACTIVITY=activity;
		testAlive = true;
		//SERVER = server;
		TCP_PORT = tcpPort;
	}

	/*
	 * Stops connection thread
	 */
	public static void stopThread(){
		Log.i(TAG, "Attempting to stop the thread");
		testAlive = false;
		
	}
	

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		Log.e(TAG, "OnBind()");
		return new LocalBinder<SigcommDemoAndroidService>(this);
	}

	/*
	 * Called when all activities are unbound.
	 * 
	 * Not really needed
	 * (non-Javadoc)
	 * @see android.app.Service#onUnbind(android.content.Intent)
	 */
	public boolean onUnbind(Intent intent){
		return false;
	}
	
	@Override
	public void onDestroy () {
		Log.e(TAG, "OnDestroy()");
		super.onDestroy();
	}


	
	@Override
	public void onCreate(){
		super.onCreate();
		Log.e(TAG, "OnCreate()");
		Log.i(TAG, "Device Name: "+devName);
		pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "WAKELOCK");
		wl.acquire();
		//devName = android.os.Build.DEVICE+":"+android.os.Build.MODEL+ " ("+ android.os.Build.PRODUCT + ")";
		Log.e(TAG, "WakeLock acquired by service");
		Thread th = new Thread(this);
		th.start();
	}
	
	public void callFinalize(){
		Log.i(TAG, "callFinalize");
		System.exit(0);
	}
	
	

	
	public void notifyActivity (int value, int caseId){
		Intent i = new Intent(this.REFRESH_DATA);
		String extraVal = null;
		switch(caseId){
			case GOODPUT_DOWNSTREAM_ID:
				extraVal = REFRESH_GOODPUTDOWNSTREAM_INTENT;
				break;
			case GOODPUT_UPSTREAM_ID:
				extraVal = REFRESH_GOODPUTUPSTREAM_INTENT;
				break;
			case RTT_ID:
				extraVal = REFRESH_RTT_INTENT;
				break;
			case JITTER_ID:
				extraVal = REFRESH_JITTER_INTENT;
				break;
			default:
				Log.i(TAG, "Unknown value");
				return;				
		}

		i.putExtra(extraVal, value);
		sendBroadcast(i);
	}
	
	public void notifyErrorActivity (String errorMsg){
		Log.i(TAG, "Error to be notified "+errorMsg);
		Intent i = new Intent(this.SHOW_ERROR);
		i.putExtra(this.ERROR_INTENT, errorMsg);
		sendBroadcast(i);
	}
	
	
	//Thread! 
	//If it has to be improved, would be nice to use some event-based libraries
	public void run (){
		try{
			//Connects to server
			long startTest = System.currentTimeMillis();
			int UDP_SERVER_PORT = -1;	
			Socket clientSocket = new Socket(); 
			//byte[] ipAddr = new byte[]{(byte) SERVER[0], (byte) SERVER[1], (byte) SERVER[2], (byte) SERVER[3]};
			//InetAddress addr = InetAddress.getByAddress(ipAddr);
			Log.i(TAG, "Dev name: "+devName);
			String serverUrl = "";
			try{
				String [] splitName = devName.split("\\.");
				serverUrl= splitName[0]+"."+SIGNPOST_SERVER_DOMAIN;
			}
			catch(Exception e){
				Log.e(TAG, "EXCEPTION: "+e.getMessage());
			}
			Log.i(TAG, "Server URL: "+serverUrl);
			InetSocketAddress isockAddress = null;
			address = InetAddress.getByName(serverUrl);
			isockAddress = new InetSocketAddress(address, TCP_PORT);	

			Log.i(TAG, "Name for: "+address.getHostAddress());


			clientSocket.connect(isockAddress);	
		    //Handshake is blocking!
			DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
			BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			if (DEBUG) Log.i(TAG, "Connection succeeded");
			//3-way handshake. Exchange device type, get udp port
			outToServer.writeBytes(devName+"\r\n"+UDP_LOCAL_PORT+"\r\n");
			String in = inFromServer.readLine();
			UDP_SERVER_PORT = Integer.parseInt(in);			
			if (DEBUG) Log.i(TAG, "Server line (string): "+in+" - Server UDP port (int): "+UDP_SERVER_PORT);
			

			//Starting UDP receiver and sender thread (non-blocking)
		    SenderThread sender = new SenderThread(address, UDP_SERVER_PORT);
		    sender.start();
		    
			/*
			 * TCP Latency measurement is deprecated but still done.
			 * The latency plotted is the RTT measured with UDP thread
			 */
			while (testAlive){
				//Ping message
				long startTime = System.currentTimeMillis();
				outToServer.writeBytes(Messages.PING+ "\r\n");
				if (DEBUG) Log.i(TAG, "C->S"+Messages.PING+ "\r\n");
				
				
				//Listen for num bytes from server and estimate latency.
				in = inFromServer.readLine();
				int numBytes = Integer.parseInt(in);				
				long latency = (System.currentTimeMillis()-startTime)*1000/2;							
				//notifyActivity(latency, LATENCY_UPSTREAM_ID);
				
				Log.i(TAG, "Packet Length (string): "+in+" - Packet Length (int): "+numBytes);
				long startDownloadTime = System.currentTimeMillis();                
				//Timer starts now but this value is 
				//substracted later latency/2 
				//(not sure why it is done like that in the server)
				outToServer.writeBytes(latency+"\r\n");
				outToServer.flush();
				
				
				//Get Data so we can estimate downstream GOODPUT	
				char data[] = new char[1024];
                int overall = 0;
                while (overall<numBytes)
                {
                	overall += inFromServer.read(data, 0, 1024);                	
                }
                long downloadTime = (System.currentTimeMillis() - startDownloadTime)-latency/1000*2;
                Log.i(TAG, "Download time: "+downloadTime);
                int goodputDownstream = 8*numBytes/(int)downloadTime; //in kbps
                if (goodputDownstream<0){
                	Log.e(TAG, "ERROR!!! GOODPUT<0. Bits: "+8*numBytes+" - Download Time (int) "+(int)downloadTime);
                }
                Log.e(TAG, "DOWNSTREAM (kbps): "+goodputDownstream);
                notifyActivity(goodputDownstream, GOODPUT_DOWNSTREAM_ID);
				outToServer.writeBytes(goodputDownstream*1000+ "\r\n"); //Sent to server as bps
				
				//Wait for server latency
				int serverLatencyInt = Integer.parseInt(inFromServer.readLine());
				
				//GET UPSTREAM GOODPUT
				long overallUpstream=0;
				while (overallUpstream<numBytes)
                {
					outToServer.writeBytes(SEQ_TO_SERVER);
                	overallUpstream+=SEQ_TO_SERVER.length();
                }
				int upstreamGoodputInt = Integer.parseInt(inFromServer.readLine())/1000;
				notifyActivity(upstreamGoodputInt, GOODPUT_UPSTREAM_ID);
				
				if (DEBUG) Log.i(TAG, "Upstream test finished. " + overallUpstream+" bytes sent");	
				if ((System.currentTimeMillis()-startTest) > 120*1000){
					testAlive = false;
				}
			}
			
			clientSocket.close();
		}
		catch(Exception e){
			Log.i(TAG, "EXCEPTION OPENING CONNECTION: "+e.getMessage());
			notifyErrorActivity("SERVER NOT AVAILABLE");
		}
		stopThread();
		Log.i(TAG, "FINISHED!");
		
		
		try {
			wl.release();
			this.finalize();
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	/*
	 * UDP Test thread
	 */
	class SenderThread extends Thread {
		  private InetAddress server;
		  private boolean stopped = false;
		  private int port;

	      byte[] sendData = new byte[100];
	      byte[] receiveData = new byte[100];

		  public SenderThread(InetAddress address, int port) throws SocketException {		  	
			  	this.server = address;
		    	this.port = port;
		    	Log.i(TAG, "UDP Sender sending to "+address.getHostName()+":"+port);
		  }


		  public void run() {
			Log.e(TAG, "Starting UDP Server Thread");
			udpSeqNumber = 0;
		      
		    try {
				DatagramSocket clientSocket = new DatagramSocket();

		      while (testAlive) {
		    	  try{
		    	  	//Info sent to server (hostname;timestamp;jitter) + -1 indicating start RTT test

			        long t1 = System.currentTimeMillis();
			        
		    	  	String p0 = devName+";"+0+";"+t1+";";

			        byte[] data = p0.getBytes();
			        DatagramPacket dpSend = new DatagramPacket(data, data.length, server, port);
			        clientSocket.send(dpSend);
			        
			        /*Wait for server response and client estimate RTT*/
			        DatagramPacket dpReceive = new DatagramPacket(receiveData, receiveData.length);			         
			        clientSocket.receive(dpReceive);
			        long t2 = System.currentTimeMillis();
			        String r1 = new String(dpReceive.getData(), 0, dpReceive.getLength());
			        
			        /*Send response to server*/
			        long t3 = System.currentTimeMillis();			    
			        String p1 = devName+";"+1+";"+t3+";";

			        data = p1.getBytes();
			        dpSend = new DatagramPacket(data, data.length,server, port);			        
			        clientSocket.send(dpSend);

			        //Wait for final server response
			        dpReceive = new DatagramPacket(receiveData, receiveData.length);			         
			        clientSocket.receive(dpReceive);
			        
			        /*
			         * Compute jitter and RTT
			         */
			        long t4 = System.currentTimeMillis();
			        String r2 = new String(dpReceive.getData(), 0, dpReceive.getLength());
			        
			        long rtt=((t4-t3)+(t2-t1))/2;
			        String [] serverResp1 = r1.split(";");
			        String [] serverResp2 = r2.split(";");			        
			        
			        long servTimestamp1 = Long.parseLong(serverResp1[2]);
			        long servTimestamp2 = Long.parseLong(serverResp2[2]);
			        long deltaRemote = servTimestamp2-servTimestamp1;
			        long deltaLocal = t3-t1;
			        
			        long jitter = Math.abs(deltaLocal-deltaRemote);
			        notifyActivity((int)rtt*1000, RTT_ID);
			        notifyActivity((int)jitter*1000, JITTER_ID);
			        
			        Log.e(TAG, "UDP Measurement. RTT: "+rtt+"\tJitter: "+jitter);			        
			        long error = System.currentTimeMillis()-t1;
			        
			        //Sleep thread
			        Thread.sleep(2000-error);
			        
			    }
			    catch(Exception e){
			    	Log.e(TAG, "Test failed: "+e.getMessage());
			    }
		      }
		      clientSocket.close();
		    }
		    catch (Exception ex) {
		      Log.e(TAG, "Exception on UdpSender thread> "+ex.getMessage());
		    }
		  }
		}
}

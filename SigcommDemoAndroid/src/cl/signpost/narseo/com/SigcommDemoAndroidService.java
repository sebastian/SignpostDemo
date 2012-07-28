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
import java.util.Random;

import cl.signpost.narseo.com.TestsSignpost.Messages;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

public class SigcommDemoAndroidService extends Service implements Runnable{

	public static final String TAG = "SIGNPOSTSERV";
	private static SigcommDemoAndroidActivity MAIN_ACTIVITY;
	private static PowerManager.WakeLock wl = null;
	private static PowerManager pm =null;
	
	public static final Boolean DEBUG = true;
	

	public static final String REFRESH_DATA = "SIGNPOST_VALUE";
	public static final String REFRESH_LATENCYUPSTREAM_INTENT = "LATENCYUPSTREAM";
	public static final String REFRESH_LATENCYDOWNSTREAM_INTENT = "LATENCYDOWNSTREAM";
	public static final String REFRESH_JITTER_INTENT = "JITTER";
	public static final String REFRESH_GOODPUTUPSTREAM_INTENT = "GOODPUTUPSTREAM";
	public static final String REFRESH_GOODPUTDOWNSTREAM_INTENT = "GOODPUTDOWNSTREAM";
	
	
	public static final int LATENCY_UPSTREAM_ID = 0;
	public static final int LATENCY_DOWNSTREAM_ID = 1;
	public static final int GOODPUT_UPSTREAM_ID = 2;
	public static final int GOODPUT_DOWNSTREAM_ID = 3;
	public static final int JITTER_ID = 4;
	
	//Data stream transmitted to server
	public static final String SEQ_TO_SERVER = "abcdefghijklmnop";
	
	//Used to kill the thread
	public static boolean testAlive = false;

	//Default values
	public static int [] SERVER = {192, 168, 1, 1};
	public static int TCP_PORT = 7777;
	public static int UDP_LOCAL_PORT = 5522;
	

	static SenderThread sender  = null;
	static ReceiverThread receiver = null;
	
	/*
	 * Configures parameters and links to main activity
	 */
	public static void setMainActivity(SigcommDemoAndroidActivity activity, int [] server, int tcpPort){
		Log.e(TAG, "Activity added. Task ID: "+activity.getTaskId());
		MAIN_ACTIVITY=activity;
		testAlive = true;
		SERVER = server;
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
		pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "WAKELOCK");
		wl.acquire();
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
			case LATENCY_UPSTREAM_ID:
				extraVal = REFRESH_LATENCYUPSTREAM_INTENT;
				break;
			case LATENCY_DOWNSTREAM_ID:
				extraVal = REFRESH_LATENCYDOWNSTREAM_INTENT;
				break;
			default:
				Log.i(TAG, "Unknown value");
				return;				
		}

		i.putExtra(extraVal, value);
		sendBroadcast(i);
	}
	
	
	/*
	 * Test thread (periodically sends a new value)
	 * (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 *//*
	public void run(){
		int counter = 0;
		while (testAlive){
			try{
				Thread.sleep(1000);
				counter++;
				Random r = new Random();
				notifyActivity((r.nextInt()%1000)*1000, LATENCY_DOWNSTREAM_ID);
				notifyActivity((r.nextInt()%1000)*1000, LATENCY_UPSTREAM_ID);
				notifyActivity((r.nextInt()%20)*1000, GOODPUT_DOWNSTREAM_ID);
				notifyActivity((r.nextInt()%20)*1000, GOODPUT_UPSTREAM_ID);
			}
			catch(Exception e){
				Log.i(TAG, "ERROR: "+e.getMessage());
			}
		}
	}*/
	
	
	//Thread! 
	//If it has to be improved, would be nice to use some event-based libraries
	public void run (){
		try{
			//Connects to server
			int UDP_SERVER_PORT = -1;	
			Socket clientSocket = new Socket(); 
			byte[] ipAddr = new byte[]{(byte) SERVER[0], (byte) SERVER[1], (byte) SERVER[2], (byte) SERVER[3]};
			InetAddress addr = InetAddress.getByAddress(ipAddr);
		    InetSocketAddress isockAddress = new InetSocketAddress(addr, TCP_PORT);
		    clientSocket.connect(isockAddress);	
		    //Handshake is blocking!
			DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
			BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			if (DEBUG) Log.i(TAG, "Connection succeeded");
			//3-way handshake. Exchange device type, get udp port
			outToServer.writeBytes( android.os.Build.DEVICE+":"+android.os.Build.MODEL+ " ("+ android.os.Build.PRODUCT + ")"+"\r\n"+UDP_LOCAL_PORT+"\r\n");
			String in = inFromServer.readLine();
			UDP_SERVER_PORT = Integer.parseInt(in);			
			if (DEBUG) Log.i(TAG, "Server line (string): "+in+" - Server UDP port (int): "+UDP_SERVER_PORT);
			
			//Starting UDP receiver and sender thread (non-blocking)
		    SenderThread sender = new SenderThread(addr, UDP_SERVER_PORT);
		    sender.start();
		    Thread receiver = new ReceiverThread(sender.getSocket());
		    receiver.start();
		    
			
			while (testAlive){
				//Ping message
				long startTime = System.currentTimeMillis();
				outToServer.writeBytes(Messages.PING+ "\r\n");
				if (DEBUG) Log.i(TAG, "C->S"+Messages.PING+ "\r\n");
				
				
				//Listen for num bytes from server and estimate latency.
				in = inFromServer.readLine();
				int numBytes = Integer.parseInt(in);				
				int latency = (int)(System.currentTimeMillis()-startTime)*1000/2;							
				notifyActivity(latency, LATENCY_UPSTREAM_ID);
				
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
                int downloadTime = (int)(System.currentTimeMillis() - startDownloadTime)-latency/1000*2;
                int goodputDownstream = 8*numBytes/downloadTime; //in kbps
                Log.e(TAG, "DOWNSTREAM (kbps): "+goodputDownstream);
                notifyActivity(goodputDownstream, GOODPUT_DOWNSTREAM_ID);
				outToServer.writeBytes(goodputDownstream*1000+ "\r\n"); //Sent to server as bps
				
				//Wait for server latency
				int serverLatencyInt = Integer.parseInt(inFromServer.readLine());
				notifyActivity (serverLatencyInt, LATENCY_DOWNSTREAM_ID);				
				
				
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
			}
			
			clientSocket.close();
		}
		catch(Exception e){
			Log.i(TAG, "EXCEPTION OPENING CONNECTION: "+e.getMessage());
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
	
	
	class SenderThread extends Thread {

		  private InetAddress server;

		  private DatagramSocket socket;

		  private boolean stopped = false;

		  private int port;

		  public SenderThread(InetAddress address, int port) throws SocketException {
			  	
			  	this.server = address;
		    	this.port = port;
		    	this.socket = new DatagramSocket();
		    	this.socket.connect(server, port);
		    	Log.i(TAG, "UDP Sender sending to "+address.getHostName()+":"+port);
		  }

		  public void halt() {
			  	this.stopped = true;
		  }

		  public DatagramSocket getSocket() {		    
			  return this.socket;
		  }

		  public void run() {
			Log.e(TAG, "Starting UDP Server Thread");
			  
		    try {
		      BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
		      while (testAlive) {
		        
		        //Info sent to server (hostname;timestamp;jitter)
		        String theLine = "me\r\n"+(float)System.currentTimeMillis()+"\r\n"+10.0f+"\r\n";
		        Log.e(TAG, "Sending UDP: "+theLine);
		        byte[] data = theLine.getBytes();
		        DatagramPacket output = new DatagramPacket(data, data.length, server, port);
		        socket.send(output);
		        
		        Thread.sleep(50);
		      }
		    }
		    catch (Exception ex) {
		      Log.e(TAG, "Exception on UdpSender thread> "+ex.getMessage());
		    }
		  }
		}

		class ReceiverThread extends Thread {
		  DatagramSocket socket;

		  private boolean stopped = false;

		  public ReceiverThread(DatagramSocket ds) throws SocketException {
		    Log.i(TAG, "UDP Receiver ready to listen on port "+ds.getLocalPort());
		    this.socket = ds;
		  }

		  public void halt() {
		    this.stopped = true;
		  }

		  public void run() {
			Log.i(TAG, "Receiver Thread Running ");
		    byte[] buffer = new byte[1024];
		    while (testAlive) {

		      DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
		      try {
		        socket.receive(dp);
		        Log.i(TAG, "udp receiver got smthing");
		        String s = new String(dp.getData(), 0, dp.getLength());
		        Log.i(TAG, "UDP Receiver> "+s);
		        Thread.yield();
		      } catch (Exception ex) {
			      Log.e(TAG, "Exception on UdpReceiver thread > "+ex.getMessage());
		      }
		    }
		  }
		}
	
}

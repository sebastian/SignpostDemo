package cl.signpost.narseo.com;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
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
		wl.release();
		pm = null;
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
	public void run (){
		try{
			//Connects to server
			int UDP_SERVER_PORT = -1;	
			Socket clientSocket = new Socket(); 
			byte[] ipAddr = new byte[]{(byte) SERVER[0], (byte) SERVER[1], (byte) SERVER[2], (byte) SERVER[3]};
			InetAddress addr = InetAddress.getByAddress(ipAddr);
		    InetSocketAddress isockAddress = new InetSocketAddress(addr, TCP_PORT);
		    clientSocket.connect(isockAddress);			
			DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
			BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			String in = null;
			if (DEBUG) Log.i(TAG, "Connection succeeded");
			
			
			//3-way handshake. Exchange device type, get udp port
			outToServer.writeBytes( android.os.Build.DEVICE+":"+android.os.Build.MODEL+ " ("+ android.os.Build.PRODUCT + ")"+"\r\n"+UDP_LOCAL_PORT+"\r\n");
			UDP_SERVER_PORT = Integer.parseInt(inFromServer.readLine());			
			if (DEBUG) Log.i(TAG, "Server line (string): "+in+" - Server UDP port (int): "+UDP_SERVER_PORT);
			
			while (testAlive){
				//Ping message
				long startTime = System.currentTimeMillis();
				outToServer.writeBytes(Messages.PING+ "\r\n");
				if (DEBUG) Log.i(TAG, "C->S"+Messages.PING+ "\r\n");
				
				
				//Listen for num bytes from server and estimate latency.
				int numBytes = Integer.parseInt(inFromServer.readLine());				
				int latency = (int)(System.currentTimeMillis()-startTime)*1000/2;							
				notifyActivity(latency, LATENCY_UPSTREAM_ID);
				
				Log.i(TAG, "Packet Length (string): "+in+" - Packet Length (int): "+numBytes);
				long startDownloadTime = System.currentTimeMillis();                
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
                int goodputDownstream = 8*numBytes/downloadTime;
                notifyActivity(goodputDownstream, GOODPUT_DOWNSTREAM_ID);
				outToServer.writeBytes(numBytes/downloadTime+ "\r\n");
				
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
				int upstreamGoodputInt = Integer.parseInt(inFromServer.readLine());
				notifyActivity(upstreamGoodputInt, GOODPUT_UPSTREAM_ID);
				
				if (DEBUG) Log.i(TAG, "Upstream test finished. " + overallUpstream+" bytes sent");				
			}
			
			clientSocket.close();
		}
		catch(Exception e){
			Log.i(TAG, "EXCEPTION OPENING CONNECTION: "+e.getMessage());
		}		  

	}
	
	
}

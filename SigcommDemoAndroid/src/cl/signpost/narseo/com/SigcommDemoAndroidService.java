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
	
	public static final Boolean DEBUG = false;
	

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
	
	public static boolean testAlive = false;

	//Default values
	public static String SERVER = null;
	public static int TCP_PORT = 7777;
	public static int UDP_PORT = 7777;
	
	
	public static void setMainActivity(SigcommDemoAndroidActivity activity, String server, int tcpPort){
		Log.e(TAG, "Activity added. Task ID: "+activity.getTaskId());
		MAIN_ACTIVITY=activity;
		testAlive = true;
		SERVER = server;
		TCP_PORT = tcpPort;
	}

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
		// TODO Auto-generated method stub
		Log.e(TAG, "OnDestroy()");
		super.onDestroy();
		//Stop the service
		wl.release();
		pm = null;
	}

	@Override
	public void onCreate(){
		super.onCreate();
		Log.e(TAG, "OnCreate()");
		//Create wakelock
		pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		Log.e(TAG, "Power Manager created");
		//Get Full wakelock to keep screen at full brightness (to make sure we can collect cell ID)
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
	 */
	public void run(){
		int counter = 0;
		while (testAlive){
			try{
				Thread.sleep(1000);
				counter++;
				notifyActivity(counter, LATENCY_DOWNSTREAM_ID);
				Random r = new Random();
				int bwidth = (r.nextInt()%100)*1000;
				notifyActivity(bwidth, GOODPUT_DOWNSTREAM_ID);
				
				
			}
			catch(Exception e){
				Log.i(TAG, "ERROR: "+e.getMessage());
			}
		}
	}
	
	
	//Thread!
	/*
	public void run (){
		//Open TCP Connection to server
		try{
			TCP_PORT=7777;
			int UDP_LOCAL_PORT=5522;
			int UDP_SERVER_PORT = -1;
			
			//To be modified. Add IP dynamically
			byte[] ipAddr = new byte[]{(byte) 192, (byte) 168, (byte)15, (byte) 215};
			InetAddress addr = InetAddress.getByAddress(ipAddr);
		    InetSocketAddress isockAddress = new InetSocketAddress(addr, TCP_PORT);
		    
			//InetSocketAddress host = new InetSocketAddress(SERVER, TCP_PORT);
			if (DEBUG) Log.i(TAG, "Trying to reach server (InetSocketAddress): "+isockAddress.getHostName()+":"+isockAddress.getPort());
			
			Socket clientSocket = new Socket(); 
			clientSocket.connect(isockAddress);
			
			if (DEBUG) Log.i(TAG, "Connection succeeded");
			DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
			BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			String in = null;
			
			//Send port to Server
			outToServer.writeBytes( android.os.Build.DEVICE+":"+android.os.Build.MODEL+ " ("+ android.os.Build.PRODUCT + ")"+"\r\n"+UDP_LOCAL_PORT+"\r\n");

			if (DEBUG) Log.i(TAG, "Local UDP port: "+UDP_LOCAL_PORT+" notified to server");		
			in = inFromServer.readLine();
			UDP_SERVER_PORT = Integer.parseInt(in);
			
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
				//Notify latency to server
				outToServer.writeBytes(latency+"\r\n");
				outToServer.flush();
				//Get Data			
				char data[] = new char[1024];
                int overall = 0;
                while (overall<numBytes)
                {
                	overall += inFromServer.read(data, 0, 1024);                	
                }
                
                //in ms
                int downloadTime = (int)(System.currentTimeMillis() - startDownloadTime)-latency/1000*2;
                Log.i(TAG, "DOWNLOAD TIME: "+downloadTime);
                //in kbps
                int goodputDownstream = 8*numBytes/downloadTime;
                notifyActivity(goodputDownstream, GOODPUT_DOWNSTREAM_ID);
                //Notify goodput
				outToServer.writeBytes(numBytes/downloadTime+ "\r\n");
				Log.i(TAG, "Updatad goodput: "+numBytes/downloadTime+" mbps");
				//Wait for server latency
				int serverLatencyInt = Integer.parseInt(inFromServer.readLine());
				notifyActivity (serverLatencyInt, LATENCY_DOWNSTREAM_ID);				
				
				//Send same amount of bytes. Stimate upstream
				String seqToServer = "abcdefghabcdefghabcdefghabcdefghabcdefghabcdefgh";
				long overallUpstream=0;
				while (overallUpstream<numBytes)
                {
					outToServer.writeBytes(seqToServer);
                	overallUpstream+=seqToServer.length();
                }
				Log.i(TAG, "Upstream test finished. " + overallUpstream+" bytes sent");
				//Wait for server report
				int upstreamGoodputInt = Integer.parseInt(inFromServer.readLine());
				notifyActivity(upstreamGoodputInt, GOODPUT_UPSTREAM_ID);
			}
			
			clientSocket.close();
		}
		catch(Exception e){
			Log.i(TAG, "EXCEPTION OPENING CONNECTION: "+e.getMessage());
		}
		  

	}*/
	
	
	
	

}

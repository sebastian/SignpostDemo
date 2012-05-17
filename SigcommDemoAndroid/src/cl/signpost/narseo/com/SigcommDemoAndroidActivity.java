package cl.signpost.narseo.com;

import com.google.gson.Gson;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;




//SignpostDemoAndroid
//
//Created by Narseo Vallina on 08/05/2012.
//Copyright (c) 2012 narseo@gmail.com. All rights reserved.
//

public class SigcommDemoAndroidActivity extends Activity implements OnClickListener{
	
	private static final String TAG = "SIGPST";
	public PowerManager pm = null;
	private PowerManager.WakeLock mWakeLock;
	private static Button startButton = null;
	private static Button stopButton = null;
	private static EditText latencyUpstream = null;
	private static EditText latencyDownstream = null;
	private static EditText goodputUpstream = null;
	private static EditText goodputDownstream = null;
	private static EditText jitter = null;
	private static Intent mServiceIntent;
	private DataUpdateReceiver dataUpdateReceiver;

	private static WebView  myWebView;
	
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

	//public static final String IP_ADDRESS = "192.168.";
	public static final int TCP_PORT = 7777;
	
	public static final int MAX_HISTORIC_VALS = 5;
	public static int [] arrayDownstreamBwidth = {0, 0, 0, 0, 0};
	public static int [] arrayDownstreamLatency = {0, 0, 0, 0, 0};
	
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        startButton =(Button) findViewById(R.id.startTest);        
        startButton.setOnClickListener(this);   
        stopButton =(Button) findViewById(R.id.stopTest);        
        stopButton.setOnClickListener(this);  
        latencyUpstream = (EditText) findViewById (R.id.latencyTextboxUpstream);
        latencyDownstream = (EditText)findViewById(R.id.latencyTextboxDownstream);
        goodputUpstream = (EditText)findViewById(R.id.goodputTextboxUpstream);
        goodputDownstream = (EditText) findViewById(R.id.goodputTextboxDownstream);
        //jitter = (EditText) findViewById(R.id.jitterTextbox);
        
        
        /*
         * Copy web content to sdcard
         */
        
        
        Log.i(TAG, "Signpost Demo OnCreate()");
        
        //Get lock manager
        pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, "My Tag");
        mWakeLock.acquire();
        
        if (dataUpdateReceiver == null) dataUpdateReceiver = new DataUpdateReceiver();
        IntentFilter intentFilter = new IntentFilter(REFRESH_DATA);
        registerReceiver(dataUpdateReceiver, intentFilter);
        
        //Create references to textBoxes and buttons
    }
    
    
    public void copyWebContent(){
    	
    }
    
    
    public void onDestroy(){
    	super.onDestroy();
    	
    	Log.i(TAG, "OnDestroy: release wakelock");
    	mWakeLock.release();
    }


    public void onPause(){
    	super.onDestroy();
    	//if (dataUpdateReceiver != null) unregisterReceiver(dataUpdateReceiver);
    }
    
    
	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
	    switch (v.getId()){
	    	case R.id.startTest:
	    		Log.i(TAG, "Start button pressed");
	    		//Start Background service

	    		
	    		SigcommDemoAndroidService.setMainActivity(this, "192.168.15.215", 7777);
	    		//SigcommDemoAndroidService.setMainActivity(this, "192.168.15.36", 7777);
	    		//Intent intent = new Intent(this, TrackingService.class);
	    		//startService(intent);
	    		mServiceIntent = new Intent(this, SigcommDemoAndroidService.class);
	    		bindService (mServiceIntent, mConnection, Context.BIND_AUTO_CREATE);
	    		
	    		break;
	    	case R.id.stopTest:
	    		Log.i(TAG, "Stop button pressed");
	    		//Stop background service
	    		Log.i(TAG, "Stop button pressed");
	    		
	    		try{
	    			if (dataUpdateReceiver != null) unregisterReceiver(dataUpdateReceiver);
		    		
	    		}
	    		catch(Exception e){
	    			Log.e(TAG, "Exception Waiting for service to die: "+e.getMessage());
	    		}
	    		SigcommDemoAndroidService.stopThread();
	        	
	    		
	    		try{
	            	unbindService(mConnection);        			
	    		}		
	    		catch(Exception e){
	    			Log.i(TAG, "ERROR UNBINDING SERVICE: "+e.getMessage());
	    		}
	    		this.finish();
	    		break;
	    	default:
	    		Log.i(TAG, "ERROR");
	    }
	}
	
    private ServiceConnection mConnection = new ServiceConnection(){
    	public void onServiceConnected (ComponentName className, IBinder service){
    		SigcommDemoAndroidService mService = ((LocalBinder<SigcommDemoAndroidService>) service).getService();
    		
    	}
    	public void onServiceDisconnected(ComponentName className){
    		
    	}
    };
    
    private class DataUpdateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(REFRESH_DATA)) {
            	
            //Do stuff - maybe update my view based on the changed DB contents
            	int valueLatencyUpstream = intent.getIntExtra(REFRESH_LATENCYUPSTREAM_INTENT, -1);
            	if (valueLatencyUpstream>-1){
                	Log.i(TAG, "Received Latency Upstream: "+valueLatencyUpstream);
                	latencyUpstream.setText(valueLatencyUpstream+" microsec.");
            	}

            	int valueLatencyDownstream = intent.getIntExtra(REFRESH_LATENCYDOWNSTREAM_INTENT, -1);
            	if (valueLatencyDownstream>-1){
                	Log.i(TAG, "Received Latency Downstream: "+valueLatencyDownstream);
                	latencyDownstream.setText(valueLatencyDownstream+" microsec.");
                	arrayDownstreamLatency = updateHistoricVal(arrayDownstreamLatency, valueLatencyDownstream);
                	
                	printVals(arrayDownstreamLatency);
                	
                	/*Update JSON file with value in sdcard*/
                	try{

                    	updateJson(arrayDownstreamLatency, LATENCY_DOWNSTREAM_ID);
                	}
                	catch(Exception e){
                		Log.e(TAG, "ERROR WITH GSON parsing: "+e.getMessage());
                	}
            	}
            	

            	int goodputUpstreamVal = intent.getIntExtra(REFRESH_GOODPUTUPSTREAM_INTENT, -1);
            	if (goodputUpstreamVal>-1){
                	Log.i(TAG, "Received Goodput Downstream: "+goodputUpstreamVal);
                	goodputUpstream.setText(goodputUpstreamVal+" kbps.");
            	}
            	
            	int goodputDownstreamVal = intent.getIntExtra(REFRESH_GOODPUTDOWNSTREAM_INTENT, -1);
            	if (goodputDownstreamVal>-1){
                	Log.i(TAG, "Received Goodput Downstream: "+goodputDownstreamVal);
                	arrayDownstreamBwidth = updateHistoricVal(arrayDownstreamBwidth, goodputDownstreamVal);
                	printVals(arrayDownstreamBwidth);
                	goodputDownstream.setText(goodputDownstreamVal+" kbps.");
                	/*Update JSON file with value in sdcard*/
                	try{

                    	updateJson(arrayDownstreamBwidth, GOODPUT_DOWNSTREAM_ID);
                	}
                	catch(Exception e){
                		Log.e(TAG, "ERROR WITH GSON parsing: "+e.getMessage());
                	}
                	
                	
            	}
            	//TODO: Extend to the other types
            }
        }
    }
    
    public void printVals(int [] array){
    	String arrayVals = "";
    	for (int i=0; i<array.length; i++){
    		arrayVals+=array[i]+", ";
    	}
		Log.i(TAG, "Array Vals: "+arrayVals);
    }
    
    public int [] updateHistoricVal (int [] array, int newval ){
    	for (int i=0; i<array.length-1; i++){
    		array[i]= array[i+1];
    	}
    	array[array.length-1]=newval;
    	return array;
    }
    
    public void updateJson (int [] values, int caseId){

		Gson gson = new Gson();
		String json = gson.toJson(values);
		
    	switch(caseId){
			case LATENCY_DOWNSTREAM_ID:
				//Update source file for latency downstream
				Log.i(TAG, "SERIALIZED DOWNSTREAM LATENCY: "+json);
				
				break;
			case GOODPUT_DOWNSTREAM_ID:
				Log.i(TAG, "SERIALIZED DOWNSTREAM BWIDTH: "+json);
				
			default:
				Log.i(TAG, "Couldn't update json. Not supported case");
				break;
    	}
    	//Refresh view!
    }
    
    /*
    public static final String REFRESH_DATA = "SIGNPOST_VALUE";
	public static final String REFRESH_LATENCYUPSTREAM_INTENT = "LATENCYUPSTREAM";
	public static final String REFRESH_LATENCYDOWNSTREAM_INTENT = "LATENCYDOWNSTREAM";
	public static final String REFRESH_JITTER_INTENT = "JITTER";
	public static final String REFRESH_GOODPUTUPSTREAM_INTENT = "GOODPUTUPSTREAM";
	public static final String REFRESH_GOODPUTDOWNSTREAM_INTENT = "GOODPUTDOWNSTREAM";
	

	private static EditText latencyUpstream = null;
	private static EditText latencyDownstream = null;
	private static EditText goodputUpstream = null;
	private static EditText goodputDownstream = null;
	private static EditText jitter = null;
	*/
}
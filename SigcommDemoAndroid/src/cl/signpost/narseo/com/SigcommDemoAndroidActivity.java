package cl.signpost.narseo.com;

import java.util.Random;

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
import android.widget.TextView;

import uk.ac.cam.cl.dtg.snowdon.*;



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
	private static TextView errorView = null;
	private static Intent mServiceIntent;
	private DataUpdateReceiver dataUpdateReceiver;


	private GraphView mGoodputGraph;
	private GraphView mLatencyGraph;
	private GraphView mJitterGraph;
	
	public static final String REFRESH_DATA = "SIGNPOST_VALUE";
	public static final String SHOW_ERROR = "ERROR";
	public static final String ERROR_INTENT = "ERROR_MESSAGE";
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
	public static final int UPSTREAM = 0;
	public static final int DOWNSTREAM = 1;


	public static final int TCP_PORT = 7777;
	
	//public static final int [] IP_ADDR = {10, 20, 1, 118};
	public static final int [] IP_ADDR = {192, 168, 1, 94};
	//192.168.1.80
	//public static final int [] IP_ADDR = {192, 168, 1, 80};
	//public static final int [] IP_ADDR = {192, 168, 43, 7};
	
	private static final int MAX_HISTORIC_VALS = 40;
	private static float [] arrayDownstreamBandwidth = new float[MAX_HISTORIC_VALS];
	private static float [] arrayUpstreamBandwidth = new float[MAX_HISTORIC_VALS];
	private static float [] arrayDownstreamLatency = new float[MAX_HISTORIC_VALS];
	private static float [] arrayUpstreamLatency = new float[MAX_HISTORIC_VALS];
	private static float [] arrayJitter = new float[MAX_HISTORIC_VALS];
	private static float [] timestampDownstreamBandwidth = new float[MAX_HISTORIC_VALS];
	private static float [] timestampUpstreamBandwidth = new float[MAX_HISTORIC_VALS];
	private static float [] timestampDownstreamLatency = new float[MAX_HISTORIC_VALS];
	private static float [] timestampUpstreamLatency = new float[MAX_HISTORIC_VALS];
	private static float [] timestampJitter = new float[MAX_HISTORIC_VALS];
	private static float maxTimestampBandwidth = 0.0f;
	private static float minTimestampBandwidth = 0.0f;
	
	//Parameters about the configuration of the plots, ticks, axis, etc
    private static String [] yTicksLabelsBandwidth = new String []{"0", "10", "20", "30", "40"};
    private static float [] yTicksPosBandwidth = new float []{0.0f, 0.25f, 0.5f, 0.75f, 1.0f};
    private static String [] yTicksLabelsLatency = new String []{"0", "0.1", "0.2", "0.3", "0.4"};
    private static String [] yTicksLabelsJitter = new String []{"0", "0.1", "0.2", "0.3", "0.4", "0.5"};
    private static float [] yTicksPosJitter = new float []{0.0f, 0.2f, 0.4f, 0.6f, 0.8f, 1.0f};
    //Default values
    private static String [] xTicksLabelsTime = new String []{"0s"};
    private static float [] xTicksPosBandwidth = new float []{1.0f};
	private static long startTime = 0;
	private static float maxValBandwidth=0.0f;
	private static float minValBandwidth=0.0f;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main2);
        
        errorView = (TextView) findViewById(R.id.editText1);
        startButton =(Button) findViewById(R.id.startTest);        
        startButton.setOnClickListener(this);   
        stopButton =(Button) findViewById(R.id.stopTest);        
        stopButton.setOnClickListener(this);  
        
        for (int i=0; i<MAX_HISTORIC_VALS; i++){
        	arrayDownstreamBandwidth [i] = 0.0f;
        	arrayUpstreamBandwidth [i] = 0.0f;
        	arrayJitter[i]=0.0f;
        	timestampDownstreamBandwidth [i] = 0.0f;
        	timestampUpstreamBandwidth [i] = 0.0f;
        	timestampDownstreamLatency [i] = 0.0f;
        	timestampUpstreamLatency [i] = 0.0f;
        	timestampJitter [i] = 0.0f;
        }

        mGoodputGraph = (GraphView) findViewById(R.id.graphGoodput);
        mLatencyGraph = (GraphView) findViewById (R.id.graphLatency);
        mJitterGraph = (GraphView) findViewById (R.id.graphJitter);
        
        mGoodputGraph.setYAxisLabel("Goodput (Mbps)");
        //mGoodputGraph.setXAxisLabel("Elapsed Time (s)");
        
        mLatencyGraph.setYAxisLabel("RTT (s)");
        //mLatencyGraph.setXAxisLabel("Elapsed Time (s)");
        
        mJitterGraph.setYAxisLabel("Jitter (s)");
        mJitterGraph.setXAxisLabel("Elapsed Time (s)");
        
        
        
        //Set the labels for the x and y axis. 
        //X axis (time) only one initially for 0s in 0.5f
        mGoodputGraph.setYLabels(yTicksLabelsBandwidth);
        mGoodputGraph.setYLabelPositions(yTicksPosBandwidth);        
        mGoodputGraph.setXLabels(xTicksLabelsTime);
        mGoodputGraph.setXLabelPositions(xTicksPosBandwidth);
        
        //Same axis as jitter
        mLatencyGraph.setYLabels(yTicksLabelsJitter);
        mLatencyGraph.setYLabelPositions(yTicksPosJitter);    
        mLatencyGraph.setXLabels(xTicksLabelsTime);
        mLatencyGraph.setXLabelPositions(xTicksPosBandwidth);   
        
        mJitterGraph.setYLabels(yTicksLabelsJitter);
        mJitterGraph.setYLabelPositions(yTicksPosJitter);    
        mJitterGraph.setXLabels(xTicksLabelsTime);
        mJitterGraph.setXLabelPositions(xTicksPosBandwidth);
        
        //Get lock manager
        pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, "My Tag");
        mWakeLock.acquire();
        
        //Register receiver to obtain data from service
        if (dataUpdateReceiver == null) dataUpdateReceiver = new DataUpdateReceiver();
        IntentFilter intentFilter = new IntentFilter(REFRESH_DATA);
        registerReceiver(dataUpdateReceiver, intentFilter);    
        IntentFilter intentFilterErrorr = new IntentFilter(SHOW_ERROR);
        registerReceiver(dataUpdateReceiver, intentFilterErrorr);    
            
        Log.i(TAG, "Signpost Demo Activity. GUI created and resources allocated");
    }
    
    
    
    public void onDestroy(){
    	super.onDestroy();    	
    	Log.i(TAG, "OnDestroy: release wakelock");
    	mWakeLock.release();
    }


    public void onPause(){
    	super.onDestroy();
    }
    
    
	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
	    switch (v.getId()){
	    	case R.id.startTest:
	    		Log.i(TAG, "Start button pressed");
	    		//Start Background service
            	errorView.setText("");
            	
	    		SigcommDemoAndroidService.setMainActivity(this, IP_ADDR, TCP_PORT);
	    		mServiceIntent = new Intent(this, SigcommDemoAndroidService.class);
	    		bindService (mServiceIntent, mConnection, Context.BIND_AUTO_CREATE);
	    		
	    		startTime = System.currentTimeMillis();
	    		break;
	    	case R.id.stopTest:
	    		Log.i(TAG, "Stop button pressed");
	    		//Stop background service
	    		
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
            if (intent.getAction().equals(SHOW_ERROR)) {
            	Log.i(TAG, "Error received: ");
            	String message = intent.getStringExtra(ERROR_INTENT); 
            	errorView.setText("ERROR: "+message);
            	//stop server
            	try{
            		SigcommDemoAndroidService.stopThread();
            	}
            	catch(Exception e){
            		Log.i(TAG, "Error: "+e.getMessage());
            	}
				//DESTROY SERVICE		
	    		try{
	    			
	            	unbindService(mConnection);        			
	    		}		
	    		catch(Exception e){
	    			Log.i(TAG, "ERROR UNBINDING SERVICE: "+e.getMessage());
	    		}
            }
            if (intent.getAction().equals(REFRESH_DATA)) {
            	int valueLatencyUpstream = intent.getIntExtra(REFRESH_LATENCYUPSTREAM_INTENT, -1);
            	if (valueLatencyUpstream>-1){
                	Log.i(TAG, "Received Latency Upstream: "+valueLatencyUpstream);
                	float elapsedTime = (float)(System.currentTimeMillis()-startTime)/1000.0f;
                	updateTimestampArray(timestampUpstreamLatency,elapsedTime);                	
                	arrayUpstreamLatency = updateHistoricValFloat(arrayUpstreamLatency, (float)valueLatencyUpstream/1000.0f); 
                	//plotLatencyPairs(timestampDownstreamLatency, arrayDownstreamLatency, timestampUpstreamLatency, arrayUpstreamLatency);
            	}

            	int valueLatencyDownstream = intent.getIntExtra(REFRESH_LATENCYDOWNSTREAM_INTENT, -1);
            	if (valueLatencyDownstream>-1){
                	Log.i(TAG, "Received Latency Downstream: "+valueLatencyDownstream);
                	float elapsedTime = (float)(System.currentTimeMillis()-startTime)/1000.0f;
                	updateTimestampArray(timestampDownstreamLatency,elapsedTime);                	
                	arrayDownstreamLatency = updateHistoricValFloat(arrayDownstreamLatency, (float)valueLatencyDownstream/1000.0f); 
                	//plotLatencyPairs(timestampDownstreamLatency, arrayDownstreamLatency, timestampUpstreamLatency, arrayUpstreamLatency);
            	}
            	int goodputUpstreamVal = intent.getIntExtra(REFRESH_GOODPUTUPSTREAM_INTENT, -1);
            	if (goodputUpstreamVal>-1){
                	Log.i(TAG, "Received Goodput Upstream: "+goodputUpstreamVal);
                	float elapsedTime = (float)(System.currentTimeMillis()-startTime)/1000.0f;
                	updateTimestampArray(timestampUpstreamBandwidth,elapsedTime);                	
                	arrayUpstreamBandwidth = updateHistoricValFloat(arrayUpstreamBandwidth, (float)goodputUpstreamVal/1000.0f); 
                	//Everything updated at the same time, otherwise time 
                	//intervals do not match and they might look ugly
                	plotBandwidthPairs(timestampDownstreamBandwidth, arrayDownstreamBandwidth, timestampUpstreamBandwidth, arrayUpstreamBandwidth);
                	plotLatencySingle(timestampDownstreamLatency, arrayDownstreamLatency);
                	plotJitterSingle(timestampJitter, arrayJitter);
            	}
            	
            	int goodputDownstreamVal = intent.getIntExtra(REFRESH_GOODPUTDOWNSTREAM_INTENT, -1);
            	if (goodputDownstreamVal>-1){
                	Log.i(TAG, "Received Goodput Downstream: "+goodputDownstreamVal);
                	float elapsedTime = (float)(System.currentTimeMillis()-startTime)/1000.0f;
                	updateTimestampArray(timestampDownstreamBandwidth,elapsedTime);                	
                	arrayDownstreamBandwidth = updateHistoricValFloat(arrayDownstreamBandwidth, (float)goodputDownstreamVal/1000.0f); 
            	}
            	int jitterVal = intent.getIntExtra(REFRESH_JITTER_INTENT, -1);
            	if (jitterVal>-1){
                	Log.i(TAG, "Received Jitter: "+jitterVal);
                	float elapsedTime = (float)(System.currentTimeMillis()-startTime)/1000.0f;
                	updateTimestampArray(timestampJitter,elapsedTime);                	
                	arrayJitter = updateHistoricValFloat(arrayJitter, (float)jitterVal/1000.0f); 
            	}
            }
        }
    }
    
    public float[] updateTimestampArray (float [] array, float newval){
    	for (int i=0; i<array.length-1; i++){
    		array[i]= array[i+1];
    	}
    	array[array.length-1]=newval;
    	if (newval>maxValBandwidth){
        	maxValBandwidth = newval;    		
    	}
    	
    	if (newval>30.0f){
        	minValBandwidth = newval-30.0f;
    	}
    	else{
        	if (array[0]>minValBandwidth){
            	minValBandwidth = array[0];
        	}    		
    	}
    	return array;
    	
    }
    

    public float [] updateHistoricValFloat (float [] array, float newval ){
    	for (int i=0; i<array.length-1; i++){
    		array[i]= array[i+1];
    	}
    	array[array.length-1]=newval;
    	return array;
    }
    

    public void plotJitterSingle (float [] timestampsDownstream, float [] arrayLatencyDownstream){
    	mJitterGraph.redraw();        

        float[][] data1 = {timestampsDownstream, arrayLatencyDownstream};
        
        mJitterGraph.setData(new float[][][]{data1}, minValBandwidth,  timestampsDownstream[timestampsDownstream.length-1], 0, 1000);
        //mLatencyGraph.addData(data2, minValBandwidth, Math.min(timestampsUpstream[timestampsUpstream.length-1], timestampsDownstream[timestampsDownstream.length-1]), 0, 1000);

        float midTime = (maxValBandwidth-minValBandwidth)/2.0f+minValBandwidth;
        float mid = (float)Math.round(midTime*10)/10;
        float firstquarterTime = (float)Math.round(((midTime-minValBandwidth)/2.0f+minValBandwidth)*10)/10;
        float secondquarterTime = (float)Math.round(((maxValBandwidth-midTime)/2.0f+midTime)*10)/10;
        float max = (float)Math.round(maxValBandwidth*10)/10;
        float min = (float)Math.round(minValBandwidth*10)/10;
        
        String [] xTicksLabelsLatency = new String []{String.valueOf(min), String.valueOf(firstquarterTime), String.valueOf(mid), String.valueOf(secondquarterTime), String.valueOf(max)};
        float [] xTicksPosLatency = new float []{0.0f, 0.25f, 0.5f, 0.75f, 1.0f};

        mJitterGraph.setXLabels(xTicksLabelsLatency);
        mJitterGraph.setXLabelPositions(xTicksPosLatency);
    }
    
    public void plotLatencySingle (float [] timestampsDownstream, float [] arrayLatencyDownstream){
    	mLatencyGraph.redraw();        

        float[][] data1 = {timestampsDownstream, arrayLatencyDownstream};
        
        mLatencyGraph.setData(new float[][][]{data1}, minValBandwidth,  timestampsDownstream[timestampsDownstream.length-1], 0, 1000);
        //mLatencyGraph.addData(data2, minValBandwidth, Math.min(timestampsUpstream[timestampsUpstream.length-1], timestampsDownstream[timestampsDownstream.length-1]), 0, 1000);

        float midTime = (maxValBandwidth-minValBandwidth)/2.0f+minValBandwidth;
        float mid = (float)Math.round(midTime*10)/10;
        float firstquarterTime = (float)Math.round(((midTime-minValBandwidth)/2.0f+minValBandwidth)*10)/10;
        float secondquarterTime = (float)Math.round(((maxValBandwidth-midTime)/2.0f+midTime)*10)/10;
        float max = (float)Math.round(maxValBandwidth*10)/10;
        float min = (float)Math.round(minValBandwidth*10)/10;
        
        String [] xTicksLabelsLatency = new String []{String.valueOf(min), String.valueOf(firstquarterTime), String.valueOf(mid), String.valueOf(secondquarterTime), String.valueOf(max)};
        float [] xTicksPosLatency = new float []{0.0f, 0.25f, 0.5f, 0.75f, 1.0f};

        mLatencyGraph.setXLabels(xTicksLabelsLatency);
        mLatencyGraph.setXLabelPositions(xTicksPosLatency);
    }
    
    
    public void plotLatencyPairs (float [] timestampsDownstream, float [] arrayLatencyDownstream, float[] timestampsUpstream, float[] arrayLatencyUpstream){
    	mLatencyGraph.redraw();        

        float[][] data1 = {timestampsDownstream, arrayLatencyDownstream};
        float[][] data2={timestampsUpstream, arrayLatencyUpstream};
        
        mLatencyGraph.setData(new float[][][]{data1}, minValBandwidth, Math.min(timestampsUpstream[timestampsUpstream.length-1], timestampsDownstream[timestampsDownstream.length-1]), 0, 1000);
        mLatencyGraph.addData(data2, minValBandwidth, Math.min(timestampsUpstream[timestampsUpstream.length-1], timestampsDownstream[timestampsDownstream.length-1]), 0, 1000);

        float midTime = (maxValBandwidth-minValBandwidth)/2.0f+minValBandwidth;
        float mid = (float)Math.round(midTime*10)/10;
        float firstquarterTime = (float)Math.round(((midTime-minValBandwidth)/2.0f+minValBandwidth)*10)/10;
        float secondquarterTime = (float)Math.round(((maxValBandwidth-midTime)/2.0f+midTime)*10)/10;
        float max = (float)Math.round(maxValBandwidth*10)/10;
        float min = (float)Math.round(minValBandwidth*10)/10;
        
        String [] xTicksLabelsLatency = new String []{String.valueOf(min), String.valueOf(firstquarterTime), String.valueOf(mid), String.valueOf(secondquarterTime), String.valueOf(max)};
        float [] xTicksPosLatency = new float []{0.0f, 0.25f, 0.5f, 0.75f, 1.0f};

        mLatencyGraph.setXLabels(xTicksLabelsLatency);
        mLatencyGraph.setXLabelPositions(xTicksPosLatency);
    }
    
    public void plotBandwidthPairs (float [] timestampsDownstream, float [] arrayBandwidthDownstream, float[] timestampsUpstream, float[] arrayBandwidthUpstream){

    	// The first dataset must be inputted into the graph using setData to replace the placeholder data already there
        //mGraph.addData(data1, Float.NaN, Float.NaN, Float.NaN, Float.NaN);
        mGoodputGraph.redraw();
        

        float[][] data1 = {timestampsDownstream, arrayBandwidthDownstream};
        float[][] data2={timestampsUpstream, arrayBandwidthUpstream};
        
        mGoodputGraph.setData(new float[][][]{data1}, minValBandwidth, Math.min(timestampsUpstream[timestampsUpstream.length-1], timestampsDownstream[timestampsDownstream.length-1]), 0, 20);
        mGoodputGraph.addData(data2, minValBandwidth, Math.min(timestampsUpstream[timestampsUpstream.length-1], timestampsDownstream[timestampsDownstream.length-1]), 0, 20);

        float midTime = (maxValBandwidth-minValBandwidth)/2.0f+minValBandwidth;
        float mid = (float)Math.round(midTime*10)/10;
        float firstquarterTime = (float)Math.round(((midTime-minValBandwidth)/2.0f+minValBandwidth)*10)/10;
        float secondquarterTime = (float)Math.round(((maxValBandwidth-midTime)/2.0f+midTime)*10)/10;
        float max = (float)Math.round(maxValBandwidth*10)/10;
        float min = (float)Math.round(minValBandwidth*10)/10;
        
        String [] xTicksLabelsBandwidth = new String []{String.valueOf(min), String.valueOf(firstquarterTime), String.valueOf(mid), String.valueOf(secondquarterTime), String.valueOf(max)};
        float [] xTicksPosBandwidth = new float []{0.0f, 0.25f, 0.5f, 0.75f, 1.0f};

        mGoodputGraph.setXLabels(xTicksLabelsBandwidth);
        mGoodputGraph.setXLabelPositions(xTicksPosBandwidth);
    }
    
    
    public void printVals(int [] array){
    	String arrayVals = "";
    	for (int i=0; i<array.length; i++){
    		arrayVals+=array[i]+", ";
    	}
		Log.i(TAG, "Array Vals: "+arrayVals);
    }
    
    public int [] updateHistoricValInt (int [] array, int newval ){
    	for (int i=0; i<array.length-1; i++){
    		array[i]= array[i+1];
    	}
    	array[array.length-1]=newval;
    	return array;
    }
}
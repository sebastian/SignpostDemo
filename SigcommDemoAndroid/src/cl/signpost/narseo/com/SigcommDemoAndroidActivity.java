package cl.signpost.narseo.com;

import java.text.DecimalFormat;
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
	private static Button rttButton = null;
	private static Button jitterButton = null;
	private static Button goodputButton = null;
	private static TextView errorView = null;
	//Title for the plot (looks better than adding a label for Y Axis which
	//is likely to overlap with the tick labels
	private static TextView title = null;
	private GraphView graph;
	
	private static Intent mServiceIntent;
	private DataUpdateReceiver dataUpdateReceiver;


	//Used to inform which one is the current active plot
	private static int currentPlot = 0;
	private static final int PLOT_RTT = 0;
	private static final int PLOT_JITTER = 1;
	private static final int PLOT_GOODPUT = 2;

	/*Used to identify the intents sent from the service*/
	public static final String REFRESH_DATA = "SIGNPOST_VALUE";
	public static final String SHOW_ERROR = "ERROR";
	public static final String ERROR_INTENT = "ERROR_MESSAGE";
	public static final String REFRESH_RTT_INTENT = "LATENCYUPSTREAM";
	public static final String REFRESH_JITTER_INTENT = "JITTER";
	public static final String REFRESH_GOODPUTUPSTREAM_INTENT = "GOODPUTUPSTREAM";
	public static final String REFRESH_GOODPUTDOWNSTREAM_INTENT = "GOODPUTDOWNSTREAM";
	//Type of data
	public static final int RTT_ID = 0;
	public static final int GOODPUT_UPSTREAM_ID = 2;
	public static final int GOODPUT_DOWNSTREAM_ID = 3;
	public static final int JITTER_ID = 4;
	public static final int UPSTREAM = 0;
	public static final int DOWNSTREAM = 1;


	public static final int TCP_PORT = 7777;
	
	//TODO: To be changed for a domain
	//public static final int [] IP_ADDR = {192, 168, 1, 94};


	
	//Labels for plots
	private static final String RTT_LABEL = "RTT (ms)";
	private static final String JITTER_LABEL = "JITTER (ms)";
	private static final String GOODPUT_LABEL = "Goodput (Mbps)";
	
	//Arrays for storing variables
	private static final int MAX_HISTORIC_VALS = 100; //Memory
	private static float [] arrayDownstreamBandwidth = new float[MAX_HISTORIC_VALS];
	private static float [] arrayUpstreamBandwidth = new float[MAX_HISTORIC_VALS];
	private static float [] arrayRtt = new float[MAX_HISTORIC_VALS];
	private static float [] arrayJitter = new float[MAX_HISTORIC_VALS];
	private static float [] timestampDownstreamBandwidth = new float[MAX_HISTORIC_VALS];
	private static float [] timestampUpstreamBandwidth = new float[MAX_HISTORIC_VALS];
	private static float [] timestampRtt = new float[MAX_HISTORIC_VALS];
	private static float [] timestampJitter = new float[MAX_HISTORIC_VALS];
	private static float maxTimestampBandwidth = 0.0f;
	private static float minTimestampBandwidth = 0.0f;
	
	//Parameters about the configuration of the plots, ticks, axis, etc
    private static float [] yTicksPosition = new float []{0.0f, 0.25f, 0.5f, 0.75f, 1.0f};
    
    //Default values. They should be restarted whenever the stop button is clicked
    //so the plot is refreshed
    private static String [] yTicksLabelsGoodputDefault = new String []{"0", "10", "20", "30", "40"};
    private static String [] yTicksLabelsRttDefault = new String []{"0", "50", "100", "150", "200"};
    private static String [] yTicksLabelsJitterDefault = new String []{"0", "50", "100", "150", "200"};
    private static String [] xTicksLabelsTime = new String []{"0s"};
    private static float [] xTicksPosGoodput = new float []{1.0f};    
	private static long startTime = 0;
	private static float maxXAxisJitter=0.0f;
	private static float minXAxisJitter=0.0f;
	private static float maxXAxisLatency=0.0f;
	private static float minXAxisLatency=0.0f;
	private static float maxXAxisGoodput=0.0f;
	private static float minXAxisGoodput=0.0f;
	private static float maxValGoodputYAxis=0.0f;
	private static float minValGoodputYAxis=0.0f;
	private static float maxValJitterYAxis=0.0f;
	private static float minValJitterYAxis=0.0f;
	private static float maxValRttYAxis=0.0f;
	private static float minValRttYAxis=0.0f;
	
	/*
	 * Used for smoothing the raw data. Default value.
	 * Can be customised calling smoothSignal(float[], int)
	 */
	private static final int SMOOTH_WINDOW = 2;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main2);
        
        errorView = (TextView) findViewById(R.id.editText1);
        title = (TextView) findViewById(R.id.title);
        startButton =(Button) findViewById(R.id.startTest);        
        startButton.setOnClickListener(this);   
        stopButton =(Button) findViewById(R.id.stopTest);        
        stopButton.setOnClickListener(this);  
        rttButton =(Button) findViewById(R.id.rtt);        
        rttButton.setOnClickListener(this);  
        jitterButton =(Button) findViewById(R.id.jitter);        
        jitterButton.setOnClickListener(this); 
        goodputButton =(Button) findViewById(R.id.goodput);        
        goodputButton.setOnClickListener(this); 
        
        


        for (int i=0; i<MAX_HISTORIC_VALS; i++){
        	arrayDownstreamBandwidth [i] = 0.0f;
        	arrayUpstreamBandwidth [i] = 0.0f;
        	arrayJitter[i]=0.0f;
        	timestampDownstreamBandwidth [i] = 0.0f;
        	timestampUpstreamBandwidth [i] = 0.0f;
        	timestampRtt [i] = 0.0f;
        	timestampJitter [i] = 0.0f;
        }
        title.setText("RTT (ms)");        

        graph = (GraphView) findViewById(R.id.graph);
        
        graph.setXAxisLabel("Elapsed Time (s)");
        graph.setYAxisLabel("");
        graph.setYLabels(yTicksLabelsRttDefault);
        graph.setYLabelPositions(yTicksPosition);        
        graph.setXLabels(xTicksLabelsTime);
        graph.setXLabelPositions(xTicksPosGoodput);
        
        //Get power manager and wakeLock
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
    
    /*
     * Called when the button stop is pressed.
     * Refreshes data stored and also plots
     */
    public void restartValues(){
    	
        for (int i=0; i<MAX_HISTORIC_VALS; i++){
        	arrayDownstreamBandwidth [i] = 0.0f;
        	arrayUpstreamBandwidth [i] = 0.0f;
        	arrayJitter[i]=0.0f;
        	timestampDownstreamBandwidth [i] = 0.0f;
        	timestampUpstreamBandwidth [i] = 0.0f;
        	timestampRtt [i] = 0.0f;
        	timestampJitter [i] = 0.0f;
        }
        title.setText("RTT (s)");        
        graph.setXAxisLabel("Elapsed Time (s)");
        graph.setYAxisLabel("");
        graph.setYLabels(yTicksLabelsRttDefault);
        graph.setYLabelPositions(yTicksPosition);        
        graph.setXLabels(xTicksLabelsTime);
        graph.setXLabelPositions(xTicksPosGoodput);
        
        
    	startTime = 0;
    	maxXAxisJitter=0.0f;
    	minXAxisJitter=0.0f;
    	maxXAxisLatency=0.0f;
    	minXAxisLatency=0.0f;
    	maxXAxisGoodput=0.0f;
    	minXAxisGoodput=0.0f;
    	maxValGoodputYAxis=0.0f;
    	minValGoodputYAxis=0.0f;
    	maxValJitterYAxis=0.0f;
    	minValJitterYAxis=0.0f;
    	maxValRttYAxis=0.0f;
    	minValRttYAxis=0.0f;
    }
    
    
    public void onDestroy(){
    	super.onDestroy();    	
    	Log.i(TAG, "OnDestroy: release wakelock");
    	mWakeLock.release();
    }


    public void onPause(){
    	super.onDestroy();
    }
    
    
    /*
     * Handler for click events
     * (non-Javadoc)
     * @see android.view.View.OnClickListener#onClick(android.view.View)
     */
	@Override
	public void onClick(View v) {
	    switch (v.getId()){
			//Start Background service
	    	case R.id.startTest:
	    		Log.i(TAG, "Start button pressed");
            	errorView.setText("");
	    		SigcommDemoAndroidService.setMainActivity(this, TCP_PORT);
	    		mServiceIntent = new Intent(this, SigcommDemoAndroidService.class);
	    		bindService (mServiceIntent, mConnection, Context.BIND_AUTO_CREATE);	    		
	    		startTime = System.currentTimeMillis();
	    		break;
	    	//Stop background service
	    	case R.id.stopTest:
	    		Log.i(TAG, "Stop button pressed");
	    		restartValues();
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
	    	//Show RTT plot
	    	case R.id.rtt:
	    		currentPlot=PLOT_RTT;
	    		Log.i(TAG, "CURRENT PLOT: "+currentPlot);
	    		break;
	    	//Show JITTER plot
	    	case R.id.jitter:
	    		currentPlot=PLOT_JITTER;
	    		Log.i(TAG, "CURRENT PLOT: "+currentPlot);
	    		break;
	    	//Show Goodput (uplink/downlink) plot
	    	case R.id.goodput:
	    		currentPlot=PLOT_GOODPUT;
	    		Log.i(TAG, "CURRENT PLOT: "+currentPlot);
	    		break;
	    	default:
	    		Log.i(TAG, "ERROR");
	    }
	}
	
	/*
	 * Controls the background service collecting the measurements
	 */
    private ServiceConnection mConnection = new ServiceConnection(){
    	public void onServiceConnected (ComponentName className, IBinder service){
    		SigcommDemoAndroidService mService = ((LocalBinder<SigcommDemoAndroidService>) service).getService();
    	}
    	public void onServiceDisconnected(ComponentName className){
    		
    	}
    };
    
    /*
     * Handles updates from the background service
     */
    private class DataUpdateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
        	//Server not available. Stop!
            if (intent.getAction().equals(SHOW_ERROR)) {
            	Log.i(TAG, "Error received: ");
            	String message = intent.getStringExtra(ERROR_INTENT); 
            	errorView.setText("ERROR: "+message);
            	try{
            		SigcommDemoAndroidService.stopThread();
            	}
            	catch(Exception e){
            		Log.i(TAG, "Error: "+e.getMessage());
            	}	
	    		try{	    			
	            	unbindService(mConnection);        			
	    		}		
	    		catch(Exception e){
	    			Log.i(TAG, "ERROR UNBINDING SERVICE: "+e.getMessage());
	    		}
            }
            //New data
            // Units required: 
            // Jitter and RTT -> ms
            // Goodput -> Mbps
            // The conversion should happen in the service. It is completely dumb
            if (intent.getAction().equals(REFRESH_DATA)) {
            	float valRTT = intent.getFloatExtra(REFRESH_RTT_INTENT, -1.0f);
            	if (valRTT>=0.0f){
                	Log.i(TAG, "Latency to be plotted (ms): "+valRTT);
                	float elapsedTime = (float)(System.currentTimeMillis()-startTime)/1000.0f;
                	updateTimestampLatencyArray(timestampRtt,elapsedTime);                	
                	arrayRtt = updateHistoricValFloat(arrayRtt, valRTT);                 	
                	/*if (valRTT>maxValRttYAxis){
                		maxValRttYAxis = valRTT;
                	}*/
            	}
            	float goodputUpstreamVal = intent.getFloatExtra(REFRESH_GOODPUTUPSTREAM_INTENT, -1.0f);
            	if (goodputUpstreamVal>=0.0f){
                	Log.i(TAG, "Goodput Upstream to be plotted (kbps): "+goodputUpstreamVal);
                	float elapsedTime = (float)(System.currentTimeMillis()-startTime)/1000.0f;
                	updateTimestampArrayGoodput(timestampUpstreamBandwidth,elapsedTime);                	
                	arrayUpstreamBandwidth = updateHistoricValFloat(arrayUpstreamBandwidth, goodputUpstreamVal); 
                	//Everything updated at the same time, otherwise time 
                	//intervals do not match and they might look ugly
                	/*if (goodputUpstreamVal>maxValGoodputYAxis){
                		maxValGoodputYAxis = goodputUpstreamVal;
                	}*/
            	}
            	float goodputDownstreamVal = intent.getFloatExtra(REFRESH_GOODPUTDOWNSTREAM_INTENT, -1.0f);
            	if (goodputDownstreamVal>=0.0f){
                	Log.i(TAG, "Goodput Downstream to be plotted (kbps): "+goodputDownstreamVal);
                	float elapsedTime = (float)(System.currentTimeMillis()-startTime)/1000.0f;
                	updateTimestampArrayGoodput(timestampDownstreamBandwidth,elapsedTime);                	
                	arrayDownstreamBandwidth = updateHistoricValFloat(arrayDownstreamBandwidth, goodputDownstreamVal); 
                	/*if (goodputDownstreamVal>maxValGoodputYAxis){
                		maxValGoodputYAxis = goodputDownstreamVal;
                	}*/
            	}
            	float jitterVal = intent.getFloatExtra(REFRESH_JITTER_INTENT, -1.0f);
            	if (jitterVal>=0.0f){
                	Log.i(TAG, "Jitter to be plotted (ms): "+jitterVal);
                	float elapsedTime = (float)(System.currentTimeMillis()-startTime)/1000.0f;
                	updateTimestampJitterArray(timestampJitter,elapsedTime);                	
                	arrayJitter = updateHistoricValFloat(arrayJitter,  jitterVal); 
                	/*
                	if (jitterVal>maxValJitterYAxis){
                		maxValJitterYAxis = jitterVal;
                	}*/
            	}
            	
            	//Whenever new data arrives, update the active plot
            	switch(currentPlot){
	            	case PLOT_RTT:
	                    title.setText(RTT_LABEL);	                    
	                    graph.setXAxisLabel("Elapsed Time (s)");
	                    graph.setYAxisLabel("");
	                    graph.setYLabels(yTicksLabelsRttDefault);
	                    graph.setYLabelPositions(yTicksPosition);        
	                    graph.setXLabels(xTicksLabelsTime);
	                    graph.setXLabelPositions(xTicksPosGoodput);	                    
	            		plotLatency(timestampRtt, smoothSignal(arrayRtt), minXAxisLatency, maxXAxisLatency);	                	
	            		break;
	            	case PLOT_JITTER:
	                    title.setText(JITTER_LABEL);	                    
	                    graph.setXAxisLabel("Elapsed Time (s)");
	                    graph.setYAxisLabel("");
	                    graph.setYLabels(yTicksLabelsJitterDefault);
	                    graph.setYLabelPositions(yTicksPosition);        
	                    graph.setXLabels(xTicksLabelsTime);
	                    graph.setXLabelPositions(xTicksPosGoodput);	                    
	            		plotJitter(timestampJitter, smoothSignal(arrayJitter), minXAxisJitter, maxXAxisJitter);     	
	            		break;
	            	case PLOT_GOODPUT:
	            		//Bi-dimensional (uplink-downlink)
	                    title.setText(GOODPUT_LABEL);	                    
	                    graph.setXAxisLabel("Elapsed Time (s)");
	                    graph.setYAxisLabel("");
	                    graph.setYLabels(yTicksLabelsGoodputDefault);
	                    graph.setYLabelPositions(yTicksPosition);        
	                    graph.setXLabels(xTicksLabelsTime);
	                    graph.setXLabelPositions(xTicksPosGoodput);	                    
	            		plot(timestampDownstreamBandwidth, smoothSignal(arrayDownstreamBandwidth, 4), timestampUpstreamBandwidth, smoothSignal(arrayUpstreamBandwidth,4), minXAxisGoodput, maxXAxisGoodput);                	
	            		break;
            	}
            }
        }
    }
   
    /*
     * Updates time series (x) for Goodput
     */
    public float[] updateTimestampArrayGoodput (float [] array, float newval){
    	for (int i=0; i<array.length-1; i++){
    		array[i]= array[i+1];
    	}
    	array[array.length-1]=newval;
    	if (newval>maxXAxisGoodput){
    		maxXAxisGoodput = newval;    		
    	}
    	
    	if (newval>30.0f){
        	minXAxisGoodput = newval-30.0f;
    	}
    	else{
        	if (array[0]>minXAxisGoodput){
        		minXAxisGoodput = array[0];
        	}    		
    	}
    	return array;    	
    }
    

    /*
     * Updates time series (x) for Jitter
     */
    public float[] updateTimestampJitterArray (float [] array, float newval){
    	for (int i=0; i<array.length-1; i++){
    		array[i]= array[i+1];
    	}
    	array[array.length-1]=newval;
    	if (newval>maxXAxisJitter){
    		maxXAxisJitter = newval;    		
    	}    	
    	if (newval>30.0f){
        	minXAxisJitter = newval-30.0f;
    	}
    	else{
        	if (array[0]>minXAxisJitter){
        		minXAxisJitter = array[0];
        	}    		
    	}
    	return array;    	
    }
    

    /*
     * Updates time series (x) for Latency
     */
    public float[] updateTimestampLatencyArray (float [] array, float newval){
    	for (int i=0; i<array.length-1; i++){
    		array[i]= array[i+1];
    	}
    	array[array.length-1]=newval;
    	if (newval>maxXAxisLatency){
    		maxXAxisLatency = newval;    		
    	}    	
    	if (newval>30.0f){
        	minXAxisLatency = newval-30.0f;
    	}
    	else{
        	if (array[0]>minXAxisLatency){
        		minXAxisLatency = array[0];
        	}    		
    	}
    	return array;    	
    }
    
    public float getMax (float [] array){
    	float tmpMax = 0.0f;
    	for (int i=0; i<array.length; i++){
    		tmpMax = Math.max(array[i], tmpMax);
    	}
    	return tmpMax;
    }
    
    public float[] smoothSignal(float [] array, int windowSize){
    	Log.i(TAG, "Smoothing signal");
    	float [] smoothSignal = new float[array.length];
    	for (int i=0; i<array.length; i++){
    		if (i<windowSize){
    			float aggregateVal = 0.0f;
    			for (int j=0; j<=i; j++){
    				aggregateVal += array[j];
    			}
    			smoothSignal[i] = aggregateVal/(float)(i+1);
    		}
    		else {
    			float aggregateVal = 0.0f;
    			for (int j=i-windowSize; j<=i; j++){
    				aggregateVal += array[j];
    			}
    			smoothSignal[i] = aggregateVal/(float)windowSize;    			
    		}
        	Log.i(TAG, "Smoothing signal: "+i+" Original "+array[i]+" Smoothed "+smoothSignal[i]);
    	}
    	return smoothSignal;
    }
    
    public float[] smoothSignal(float [] array){
    	Log.i(TAG, "Smoothing signal");
    	float [] smoothSignal = new float[array.length];
    	for (int i=0; i<array.length; i++){
    		if (i<SMOOTH_WINDOW){
    			float aggregateVal = 0.0f;
    			for (int j=0; j<=i; j++){
    				aggregateVal += array[j];
    			}
    			smoothSignal[i] = aggregateVal/(float)(i+1);
    		}
    		else {
    			float aggregateVal = 0.0f;
    			for (int j=i-SMOOTH_WINDOW; j<=i; j++){
    				aggregateVal += array[j];
    			}
    			smoothSignal[i] = aggregateVal/(float)SMOOTH_WINDOW;    			
    		}
        	Log.i(TAG, "Smoothing signal: "+i+" Original "+array[i]+" Smoothed "+smoothSignal[i]);
    	}
    	return smoothSignal;
    }
    

    /*
     * Updates time values (y) for a given variable
     * whenever new data arrives
     */
    public float [] updateHistoricValFloat (float [] array, float newval ){
    	for (int i=0; i<array.length-1; i++){
    		array[i]= array[i+1];
    	}
    	array[array.length-1]=newval;
    	return array;
    }
    
    public float roundFloat (float val){
        return (float)(Math.floor(val * 100) / 100);
    }

    /*
     * Plots jitter
     */
    public void plotJitter (float [] timestamps, float [] values, float minX, float maxX){

    	float[][] data1 = {timestamps, values};

        float midTime = (maxX-minX)/2.0f+minX;
        float mid = (float)Math.round(midTime*10)/10;
        float firstquarterTime = (float)Math.round(((midTime-minX)/2.0f+minX)*10)/10;
        float secondquarterTime = (float)Math.round(((maxX-midTime)/2.0f+midTime)*10)/10;
        float max = (float)Math.round(maxX*10)/10;
        float min = (float)Math.round(minX*10)/10;  
        
        float maxArray = getMax(values);
        maxValJitterYAxis = Math.max(maxArray, maxValJitterYAxis);

        float maxYVal = (float)Math.round(maxValJitterYAxis*1.2f*10)/10;
        float midYVal = (float)Math.round(maxYVal*0.5f*10)/10;
        float firstquarterYVal = (float)Math.round(maxYVal*0.25f*10)/10;
        float secondquarterYVal = (float)Math.round(maxYVal*0.75f*10)/10;
        
        String [] xTicksLabelsJitter = new String []{String.valueOf(min), String.valueOf(firstquarterTime), String.valueOf(mid), String.valueOf(secondquarterTime), String.valueOf(max)};
        float [] xTicksPosJitter = new float []{0.0f, 0.25f, 0.5f, 0.75f, 1.0f};

        String [] yTicksLabelsJitter = new String []{String.valueOf(0.0f), String.valueOf(firstquarterYVal), String.valueOf(midYVal), String.valueOf(secondquarterYVal), String.valueOf(maxYVal)};
        float [] yTicksPosJitter = new float []{0.0f, 0.25f, 0.5f, 0.75f, 1.0f};

        /* 
         * THIS IS WHERE SMOOTHING CAN BE DONE. SUBSTITUTE ORIGINAL ARRAY
         * BY AN SMOOTHED ONE
         */
        graph.setXLabels(xTicksLabelsJitter);
        graph.setXLabelPositions(xTicksPosJitter);
        graph.setYLabels(yTicksLabelsJitter);
        graph.setYLabelPositions(yTicksPosJitter);
        
        graph.setData(new float[][][]{data1}, minX,  timestamps[timestamps.length-1], 0.0f, maxYVal);
        graph.redraw();
    }
    
    /*
     * Plots latency
     */
    public void plotLatency (float [] timestamps, float [] values, float minX, float maxX){

    	float[][] data1 = {timestamps, values};

        //Set x-axis values
        float midTime = (maxX-minX)/2.0f+minX;
        float mid = (float)Math.round(midTime*10)/10;
        float firstquarterTime = (float)Math.round(((midTime-minX)/2.0f+minX)*10)/10;
        float secondquarterTime = (float)Math.round(((maxX-midTime)/2.0f+midTime)*10)/10;
        float max = (float)Math.round(maxX*10)/10;
        float min = (float)Math.round(minX*10)/10;


        float maxArray = getMax(values);
        maxValRttYAxis = Math.max(maxArray, maxValRttYAxis);
        
        float maxYVal = (float)Math.round(maxValRttYAxis*1.2f*10)/10;
        float midYVal = (float)Math.round(maxYVal*0.5f*10)/10;
        float firstquarterYVal = (float)Math.round(maxYVal*0.25f*10)/10;
        float secondquarterYVal = (float)Math.round(maxYVal*0.75f*10)/10;
        
        String [] xTicksLabelsLatency = new String []{String.valueOf(min), String.valueOf(firstquarterTime), String.valueOf(mid), String.valueOf(secondquarterTime), String.valueOf(max)};
        float [] xTicksPosLatency = new float []{0.0f, 0.25f, 0.5f, 0.75f, 1.0f};

        String [] yTicksLabelsLatency = new String []{String.valueOf(0.0f), String.valueOf(firstquarterYVal), String.valueOf(midYVal), String.valueOf(secondquarterYVal), String.valueOf(maxYVal)};
        float [] yTicksPosLatency = new float []{0.0f, 0.25f, 0.5f, 0.75f, 1.0f};


        /* 
         * THIS IS WHERE SMOOTHING CAN BE DONE. SUBSTITUTE ORIGINAL ARRAY
         * BY AN SMOOTHED ONE
         */
        graph.setXLabels(xTicksLabelsLatency);
        graph.setXLabelPositions(xTicksPosLatency);
        graph.setYLabels(yTicksLabelsLatency);
        graph.setYLabelPositions(yTicksPosLatency);
        
        graph.setData(new float[][][]{data1}, minX,  timestamps[timestamps.length-1], 0.0f, maxYVal);

        graph.redraw();
    }

    /*
     * Plots goodput (bi-dimensional)
     */
    public void plot (float [] timestamps, float [] values, float[] timestamps2, float[] values2, float minX, float maxX){
    	float[][] data1 = {timestamps, values};
        float[][] data2={timestamps2, values2};
        
        //Set x-axis values
        //Needs to get the minimum for the timestamp as the measurements are not sync-ed
        float midTime = (maxX-minX)/2.0f+minX;
        float mid = (float)Math.round(midTime*10)/10;
        float firstquarterTime = (float)Math.round(((midTime-minX)/2.0f+minX)*10)/10;
        float secondquarterTime = (float)Math.round(((maxX-midTime)/2.0f+midTime)*10)/10;
        float max = (float)Math.round(maxX*10)/10;
        float min = (float)Math.round(minX*10)/10;
        

        //Get maximum from arrays
        float maxArray = Math.max(getMax(values), getMax(values2));
        maxValGoodputYAxis = Math.max(maxArray, maxValGoodputYAxis);

        float maxYVal = (float)Math.round(maxValGoodputYAxis*1.2f*10)/10;
        float midYVal = (float)Math.round(maxYVal*0.5f*10)/10;
        float firstquarterYVal = (float)Math.round(maxYVal*0.25f*10)/10;
        float secondquarterYVal = (float)Math.round(maxYVal*0.75f*10)/10;
        
        String [] xTicksLabelsLatency = new String []{String.valueOf(min), String.valueOf(firstquarterTime), String.valueOf(mid), String.valueOf(secondquarterTime), String.valueOf(max)};
        float [] xTicksPosLatency = new float []{0.0f, 0.25f, 0.5f, 0.75f, 1.0f};

        String [] yTicksLabelsLatency = new String []{String.valueOf(0.0f), String.valueOf(firstquarterYVal), String.valueOf(midYVal), String.valueOf(secondquarterYVal), String.valueOf(maxYVal)};
        float [] yTicksPosLatency = new float []{0.0f, 0.25f, 0.5f, 0.75f, 1.0f};

        /* 
         * THIS IS WHERE SMOOTHING CAN BE DONE. SUBSTITUTE ORIGINAL ARRAY
         * BY AN SMOOTHED ONE
         */
        graph.setXLabels(xTicksLabelsLatency);
        graph.setXLabelPositions(xTicksPosLatency);
        graph.setYLabels(yTicksLabelsLatency);
        graph.setYLabelPositions(yTicksPosLatency);
        
        graph.redraw();

        graph.setData(new float[][][]{data1}, minX,  Math.min(timestamps[timestamps.length-1],timestamps2[timestamps2.length-1]), 0.0f, maxYVal);
        graph.addData(data2, minX, Math.min(timestamps[timestamps.length-1], timestamps2[timestamps2.length-1]), 0.0f,maxYVal);

    }
    
    /*
     * Aux method for debugging
     */
    public void printVals(int [] array){
    	String arrayVals = "";
    	for (int i=0; i<array.length; i++){
    		arrayVals+=array[i]+", ";
    	}
		Log.i(TAG, "Array Vals: "+arrayVals);
    }
    
    /*
     * Deprecated. Currently values are float
     */
    public int [] updateHistoricValInt (int [] array, int newval ){
    	for (int i=0; i<array.length-1; i++){
    		array[i]= array[i+1];
    	}
    	array[array.length-1]=newval;
    	return array;
    }
    

}
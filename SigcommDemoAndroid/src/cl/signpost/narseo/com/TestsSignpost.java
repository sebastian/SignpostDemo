package cl.signpost.narseo.com;

import java.util.Date;


//SignpostDemoAndroid
//
//Created by Narseo Vallina on 08/05/2012.
//Copyright (c) 2012 narseo@gmail.com. All rights reserved.
//

public class TestsSignpost {
	
	// How many jitter messages to retain
	public static final int JITTERMESSAGECOUNT = 60; // slightly more than one second, given 50msg/s
	public static final long INTERVAL_BETWEEN_JITTER_MESSAGES = 20000000; // in nanoseconds. i.e. 50 times per second.
	
	public static final String PING = "PING";
	public static final String PANG = "PANG";
	public static final String PONG = "PONG";
	public static final String PENG = "PENG";
	public static final String DATAFROMSERVER ="DATAFROMSERVER";
	public static final String DATAFROMCLIENT ="DATAFROMCLIENT";
	public static final String DOWNSTREAM_BW ="DOWNSTREAM_BW";
	public static final String UPSTREAM_BW ="UPSTREAM_BW";
	public static final String CLIENTJITTERPORT ="CLIENTJITTERPORT";
	public static final String SERVERJITTERPORT ="SERVERJITTERPORT";
	public static final String JITTERMESSAGE ="JITTERMESSAGE";
	  
	
	
	
	public enum Messages {
		  /* Sent from client
		   * When sending, the client initiates RTT tests.
		   */
		  PING, 
		  
		  /* Sent from server in response to PING
		   * When sending, the server initiates RTT tests.
		   * When received by the client, the client concludes RTT test.
		   */
		  PANG, 
		  
		  /* Sent from client in response to PANG
		   * Upon receiving, the server concludes RTT test
		   * Upon sending, the client starts timer for goodput test.
		   */
		  PONG, 
		  
		  /* Sent from server in response to PONG
		   * DATASIZE bytes sent by server.
		   * The client uses this to calculate the goodput.
		   */
		  DATAFROMSERVER,
		  
		  /* Sent from client in response to DATAFROMSERVER
		   * The downstream bandwidth, as seen by the client.
		   */
		  DOWNSTREAM_BW,
		  
		  /* Sent from server in response to DOWNSTREAM_BW
		   * Upon sending, the server starts timers for UPSTREAM goodput test.
		   */
		  PENG, 
		  
		  /* Sent by client in response to PENG
		   * Upon receiving, the server concludes UPSTREAM goodput test.
		   */
		  DATAFROMCLIENT,
		  
		  /* Sent by server in response to DATAFROMCLIENT
		   * UPSTREAM gootput as seen by the server.
		   */
		  UPSTREAM_BW,
		  
		////////////////////////////////////////////////////////////////////////////////////////////////////
		  
		  /* Sent by the client to server, to advertise what UDP port the client is listening to
		   * for jitter measurements.
		   */
		  CLIENTJITTERPORT,
		  
		  /* Sent by the server in response to a CLIENTJITTERPORT message. The message contains
		   * the UDP port number the server will be listening to for jitter messages from the client.
		   */  
		  SERVERJITTERPORT,
		  
		  /* A message sent by the client or the server to the others jitter UDP port.
		   * The message contains the parties current jitter estimate.
		   */
		  JITTERMESSAGE 
	}
	
	


	
	
}

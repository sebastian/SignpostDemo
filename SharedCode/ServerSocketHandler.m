//
//  SocketHandler.m
//  SignpostDemoServer
//
//  Created by Sebastian Eide on 18/05/2012.
//  Copyright (c) 2012 Kle.io. All rights reserved.
//

#import "ServerSocketHandler.h"
#import "GCDAsyncSocket.h"
#import "GCDAsyncUdpSocket.h"
#import "SharedCode.h"
#import "ClientData.h"

@implementation ServerSocketHandler

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
#pragma mark - Setup
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

- (id)initWithLogHandlerMessage:(LogHandler)message logHandlerError:(LogHandler)error logHandlerInfo:(LogHandler)info
{
  self = [super init];
  if (self)
  {
    // Setting callbacks so we can get back to the client interface
    callbackLogMessage = message;
    callbackLogError = error;
    callbackLogInfo = info;
    
    // Setup queues needed
    socketQueue = dispatch_queue_create("socketQueue", NULL);
    jitterSocketQueue = dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0);
    latencyAccessQueue = dispatch_queue_create("jitterSocketQueue", NULL); 

    // Setup socket for TCP connection that will be used for goodput and latency measurements
    listenSocket = [[GCDAsyncSocket alloc] initWithDelegate:self delegateQueue:socketQueue];
    connectedSockets = [[NSMutableArray alloc] initWithCapacity:1];
    
    // Setup UDP socket that will be used for jitter measurements
    jitterSocket = [[GCDAsyncUdpSocket alloc] initWithDelegate:self delegateQueue:jitterSocketQueue];
    
    isRunning = NO;
    
    // Get access to shared SignpostDemo functionality
    commFunc = [[SharedCode alloc] init];
    
    // Data we will store about active user connections
    userData = [[NSMutableDictionary alloc] init];
    
    // Setup and start listening to the jitter port
    [self setupJitterMechanics];
    
    [self performSelectorInBackground:@selector(backgroundJitterUpdateCalculations) withObject:nil];
  }
  return self;
}

- (void)setupJitterMechanics
{
  NSError *jitterError = nil;
  if (![jitterSocket bindToPort:0 error:&jitterError])
  {
    callbackLogError(FORMAT(@"Error setting up jitter port: %@", jitterError));
    return;
  }
  int jitterPort = [jitterSocket localPort];
  callbackLogInfo(FORMAT(@"Listening for jitter info on port %i", jitterPort));
  if (![jitterSocket beginReceiving:&jitterError]) 
  {
    callbackLogError(FORMAT(@"Error beginning receiving on jitter UDP stream: %@", jitterError));
    return;  
  }
}

- (void)setJitterCallbackUpdate:(JitterUpdatesCallback)callback
{
  jitterUpdatesCallback = callback;
}


////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
#pragma mark - Misc functionality (...yay for good names...)
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

- (BOOL)startStopSocketForPort:(NSUInteger)port andNumKBytes:(NSUInteger)numKBytesToUse
{
  if(!isRunning)
	{
    // Adjust how many bytes we are going to use for the bandwidth measurements
    if (numKBytesToUse == 0)
      numKBytesToUse = 5120; // 500kbytes
    [commFunc setNumberOfBytesForDataMeasurements:(numKBytesToUse * 1000)];
    
		if (port > 65535)
		{
			port = 0;
		}
		
		NSError *error = nil;
		if(![listenSocket acceptOnPort:port error:&error])
		{
      callbackLogError(FORMAT(@"Error starting server: %@", error));
			return NO;
		}
    NSString *hostname = FORMAT(@"%@:%i", [listenSocket localHost], [listenSocket localPort]); 
    commFunc.hostname = hostname;
    
    callbackLogInfo(FORMAT(@"Signpost demo server started on port %hu", [listenSocket localPort]));
		isRunning = YES;
    
    return YES;
  }
	else
	{
		// Stop accepting connections
		[listenSocket disconnect];
		
		// Stop any client connections
		@synchronized(connectedSockets)
		{
			NSUInteger i;
			for (i = 0; i < [connectedSockets count]; i++)
			{
				// Call disconnect on the socket,
				// which will invoke the socketDidDisconnect: method,
				// which will remove the socket from the list.
				[[connectedSockets objectAtIndex:i] disconnect];
			}
		}
		
    callbackLogInfo(@"Stopped Signpost demo server");
		isRunning = NO;

		return NO;
	}
}

- (void)backgroundJitterUpdateCalculations {
  @autoreleasepool 
  {
    struct timespec a;
    NSInteger val = 400000000;
    a.tv_nsec = val;
    a.tv_sec = 0;
    
    ClientData *cd;
    
    double localJitter = 0.0;
    double clientJitter = 0.0;
    NSInteger numClients;
    
    while (YES)
    {
      nanosleep(&a, NULL);
      
      localJitter = 0.0;
      clientJitter = 0.0;
      
      @synchronized(userData) {
        numClients = [userData count];
        
        NSEnumerator *enumerator = [userData keyEnumerator];
        NSString *host;
        while ((host = [enumerator nextObject])) {
          cd = [userData objectForKey:host];
          
          double localJitterForHost = [[commFunc currentJitterForHost:host] doubleValue];
          
          localJitter += (double) localJitterForHost / (double)numClients;
          clientJitter += (double) cd.clientPerceivedJitter / (double)numClients;          
        }        
      }
      jitterUpdatesCallback(localJitter, clientJitter);
    }
  }
}

- (void)executeBlockWithSynchronizedUserData:(void (^)(NSMutableDictionary *, SharedCode *sc))block
{
  @synchronized(userData) {
    block(userData, commFunc);
  }
}


////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
#pragma mark - GCDAsyncSocket delegate methods
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

- (void)socket:(GCDAsyncSocket *)sock didAcceptNewSocket:(GCDAsyncSocket *)newSocket
{
	// This method is executed on the socketQueue (not the main thread)
	
	@synchronized(connectedSockets)
	{
		[connectedSockets addObject:newSocket];
	}
	
	NSString *host = [newSocket connectedHost];
	UInt16 port = [newSocket connectedPort];
	
  callbackLogInfo(FORMAT(@"Accepted client %@:%hu", host, port));
  [newSocket readDataToData:[GCDAsyncSocket CRLFData] withTimeout:15 tag:CLIENTID];
  [newSocket readDataToData:[GCDAsyncSocket CRLFData] withTimeout:15 tag:CLIENTJITTERPORT];
}

- (void)socket:(GCDAsyncSocket *)sock didWriteDataWithTag:(long)tag
{
	// This method is executed on the socketQueue (not the main thread)
	switch (tag) {
    case PANG:
    {
      NSDate *startTimeLatency = [commFunc startLatencyMeasurement];
      @synchronized(userData) {
        NSString *id = sock.userData;
        ClientData *cd = [userData objectForKey:id];
        if (cd == nil)
          return;
        cd.startTimeLatency = startTimeLatency;
        [userData setObject:cd forKey:id];          
      }
      break;
    }
    case DATAFROMSERVER:
    {
      // Send the data to the user... the user can now do a bandwidth test
      NSLog(@"Sent DATAFROMSERVER");
      break;
    }
    case PENG:
    {
      NSDate *startTimeBandwidth = [commFunc startBandwidthMeasurement];
      @synchronized(userData) {
        NSString *id = sock.userData;
        ClientData *cd = [userData objectForKey:id];
        if (cd == nil)
          return;
        cd.startTimeBandwidth = startTimeBandwidth;
        [userData setObject:cd forKey:id];          
      }
      break;
    }
    case UPSTREAM_BW:
    {
      // Sent the upstream bandwidth data back to the client;
      break;
    } 
    case SERVERJITTERPORT:
    {
      // We sent the client the jitter port we are listening to
      break;
    } 
    default:
      break;
  }
}

- (void)socket:(GCDAsyncSocket *)sock didReadData:(NSData *)data withTag:(long)tag
{
	// This method is executed on the socketQueue (not the main thread)
	
  switch (tag) {
    case PING:
    {
      // In this first exchange, we want to send the num of kBytes the client
      // should expect, but also send!
      [sock writeData:[SharedCode intToData:[commFunc numBytesToReadForData]] withTimeout:-1 tag:PANG];
      [sock readDataToData:[GCDAsyncSocket CRLFData] withTimeout:READ_TIMEOUT tag:PONG];
      break;
    }
      
    case PONG:
    {
      ClientData *cd;
      @synchronized(userData) {
        NSString *id = sock.userData;
        cd = [userData objectForKey:id];
        if (cd == nil)
          return;
        double doubleLatency = [commFunc concludeLatencyMeasurementForDate:cd.startTimeLatency];
        cd.serverLatency = doubleLatency;
        cd.clientLatency = (double) [SharedCode dataToInt:data] / 1000.0;
        [userData setObject:cd forKey:id];
        callbackLogInfo(FORMAT(@"Latency to client: %f. Latency from client: %f", cd.serverLatency, cd.clientLatency));
      }
      
      [sock writeData:[commFunc dataPayload] withTimeout:-1 tag:DATAFROMSERVER];
      [sock readDataToData:[GCDAsyncSocket CRLFData] withTimeout:-1 tag:DOWNSTREAM_BW];

      break;
    }
      
    case DOWNSTREAM_BW:
    {
      ClientData *cd;
      @synchronized(userData) {
        cd = [userData objectForKey:sock.userData];
        if (cd == nil)
          return;
      }
      NSInteger serverLatencyInMicroSeconds = cd.serverLatency * 1000;
      [sock writeData:[SharedCode intToData:serverLatencyInMicroSeconds] withTimeout:-1 tag:PENG];
      [sock readDataToLength:[commFunc numBytesToReadForData] withTimeout:-1 tag:DATAFROMCLIENT];
      
      dispatch_async(dispatch_get_main_queue(), ^{
        NSInteger downstreamBandwidthInKb = [SharedCode dataToInt:data];
        double downstreamBandwidth = (double) downstreamBandwidthInKb / 1000.0;
        @synchronized(userData) {
          ClientData *cd = [userData objectForKey:sock.userData];
          if (cd == nil)
            return;
          cd.downstreamBandwidth = downstreamBandwidth;
          [userData setObject:cd forKey:sock.userData];
        }
        callbackLogInfo(FORMAT(@"Downstream bandwidth: %f", downstreamBandwidth));
      });
      break;
    }
      
    case DATAFROMCLIENT:
    {
      double upstreamBandwidth;
      @synchronized(userData) {
        NSString *id = sock.userData;
        ClientData *cd = [userData objectForKey:id];
        if (cd == nil)
          return;
        upstreamBandwidth = [commFunc getBandwidthInMegabitsPerSecondForStartTime:cd.startTimeBandwidth];
        cd.upstreamBandwidth = upstreamBandwidth;
        [userData setObject:cd forKey:id];
      }
      
      NSInteger upstreamBandwidthInkb = (NSInteger) upstreamBandwidth * 1000.0;
      NSData *data = [SharedCode intToData:upstreamBandwidthInkb];
      [sock writeData:data withTimeout:-1 tag:UPSTREAM_BW];
      [sock readDataToData:[GCDAsyncSocket CRLFData] withTimeout:-1 tag:PING];
      callbackLogInfo(FORMAT(@"Upstream bandwidth: %f", upstreamBandwidth));
      break;
    }
      
    case CLIENTID:
    {
      ClientData *cd = [[ClientData alloc] init];
      NSString *hostname = [SharedCode stringFromData:data];
      sock.userData = hostname;

      @synchronized(userData) {
        [userData setObject:cd forKey:hostname];
      }
      callbackLogInfo(FORMAT(@"Client with hostname '%@' connected", hostname));
      break;
    }
    case CLIENTJITTERPORT:
    {
      // We need to setup a jitter listening port for the client, and listen to it too.
      [sock writeData:[SharedCode intToData:[jitterSocket localPort]] withTimeout:-1 tag:SERVERJITTERPORT];
      
      // We are done with handshaking, so the next packet we should be expecting is a ping packet.
      [sock readDataToData:[GCDAsyncSocket CRLFData] withTimeout:-1 tag:PING];
      
      dispatch_async(dispatch_get_main_queue(), ^{
        NSInteger portNum = [SharedCode dataToInt:data];
        NSString *host = [sock connectedHost];
        
        callbackLogInfo(FORMAT(@"Sending jitter to port %@:%i", host, portNum));
        
        NSArray *objects = [NSArray arrayWithObjects:host, [NSNumber numberWithInt:portNum], sock.userData, jitterSocket, sock, nil];
        NSArray *keys = [NSArray arrayWithObjects:@"host", @"port", @"hostname", @"sendSocket", @"receiveSocket", nil];
        NSDictionary *infoDict = [NSDictionary dictionaryWithObjects:objects forKeys:keys];
        [commFunc performSelectorInBackground:@selector(performJitterMeasurements:) withObject:infoDict];
      });
      break;
    }
    default:
      break;
  }
}

/**
 * This method is called if a read has timed out.
 * It allows us to optionally extend the timeout.
 * We use this method to issue a warning to the user prior to disconnecting them.
 **/
- (NSTimeInterval)socket:(GCDAsyncSocket *)sock shouldTimeoutReadWithTag:(long)tag
                 elapsed:(NSTimeInterval)elapsed
               bytesDone:(NSUInteger)length
{

  NSString *host = [sock connectedHost];
  UInt16 port = [sock connectedPort];
  callbackLogInfo(FORMAT(@"Client timed out: %@:%i", host, port));
	return 0.0;
}

- (void)socketDidDisconnect:(GCDAsyncSocket *)sock withError:(NSError *)err
{
	if (sock != listenSocket)
	{
    callbackLogInfo(FORMAT(@"Client Disconnected"));
		
		@synchronized(connectedSockets) {
			[connectedSockets removeObject:sock];
		}
    
    @synchronized(userData) {
      [userData removeObjectForKey:sock.userData];
    }
	}
}


////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
#pragma mark - GCDAsyncUDPSocket delegate methods
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

- (void)udpSocket:(GCDAsyncUdpSocket *)sock didReceiveData:(NSData *)data
      fromAddress:(NSData *)address
withFilterContext:(id)filterContext 
{
  double timeDiff = [SharedCode msFromTimestampData:data];
  NSString *host = [SharedCode hostFromData:data];
  double clientJitter = [[SharedCode hostJitterFromData:data] doubleValue];
  [commFunc addJitterMeasurement:timeDiff forHost:host];
  @synchronized(userData) {
    ClientData *cd = [userData objectForKey:host];
    if (cd == nil)
      return;
    cd.clientPerceivedJitter = clientJitter;
    [userData setObject:cd forKey:host];      
  }
}

@end
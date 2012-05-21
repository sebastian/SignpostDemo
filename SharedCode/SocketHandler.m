//
//  SocketHandler.m
//  SignpostDemoClient
//
//  Created by Sebastian Eide on 18/05/2012.
//  Copyright (c) 2012 Kle.io. All rights reserved.
//

#import "SocketHandler.h"
#import "GCDAsyncSocket.h"
#import "GCDAsyncUdpSocket.h"
#import "SharedCode.h"

@implementation SocketHandler


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
    
    socketQueue = dispatch_queue_create("socketQueue", NULL);
    socket = [[GCDAsyncSocket alloc] initWithDelegate:self delegateQueue:socketQueue];
    
    jitterSocketQueue = dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0);
    jitterSocket = [[GCDAsyncUdpSocket alloc] initWithDelegate:self delegateQueue:jitterSocketQueue];
    
    commonFunc = [[SharedCode alloc] init];
    
    // Setup and start listening to the jitter port
    [self setupJitterMechanics];
    
    [self performSelectorInBackground:@selector(backgroundJitterUpdateCalculations) withObject:nil];
  }
  return self;
}

- (void) setupJitterMechanics
{
  NSError *jitterError = nil;
  if (![jitterSocket bindToPort:0 error:&jitterError])
    callbackLogError(FORMAT(@"Error setting up jitter port: %@", jitterError));

  int jitterPort = [jitterSocket localPort];
  callbackLogInfo(FORMAT(@"Listening for jitter info on port %i", jitterPort));
  if (![jitterSocket beginReceiving:&jitterError]) 
    callbackLogError(FORMAT(@"Error beginning receiving on jitter UDP stream: %@", jitterError));
}

- (void)startStopSocketForHost:(NSString*)host port:(NSUInteger)port
{
  if (port == 0)
    port = 7777;
  
  if ([host isEqualToString:@""])
    host = @"localhost";
  
  if(![socket isConnected])
	{
    NSError *error = nil;
    if (![socket connectToHost:host onPort:port error:&error]) {
      callbackLogError(FORMAT(@"Error connecting: %@", error));
      return;
    }    
    controlsToggleCallback(YES);
	}
	else
	{
		// Stop accepting connections
		[socket disconnect];
    
    callbackLogInfo(FORMAT(@"Disconnected from %@:%i.", host, port));
    controlsToggleCallback(NO);
	}
}

- (void)setControlsToggleCallback:(ControlsToggle)callback 
{
  controlsToggleCallback = callback;
}

- (void)setJitterCallbackUpdate:(JitterUpdatesCallback)callback 
{
  jitterUpdatesCallback = callback;
}

- (void)setGoodputLatencyCallback:(GoodputLatency)callback
{
  goodputLatencyCallback = callback;
}


////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
#pragma mark - Action GOOOO
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

- (void) startPingPangPongData 
{
  [socket writeData:[SharedCode payloadForString:@"ping"] withTimeout:-1 tag:PING];
  [socket readDataToData:[GCDAsyncSocket CRLFData] withTimeout:READ_TIMEOUT tag:PANG];
}

- (void)backgroundJitterUpdateCalculations {
  @autoreleasepool 
  {
    struct timespec a;
    NSInteger val = 200000000;
    a.tv_nsec = val;
    a.tv_sec = 0;
    
    while (YES)
    {
      nanosleep(&a, NULL);
      double localJitter = [[commonFunc currentJitterForHost:serverhost] doubleValue];
      jitterUpdatesCallback(localJitter, serverJitter);
    }
  }
}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
#pragma mark - GCDAsyncSocket delegate methods
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * This method is called if a read has timed out.
 * It allows us to optionally extend the timeout.
 * We use this method to issue a warning to the user prior to disconnecting them.
 **/
- (NSTimeInterval)socket:(GCDAsyncSocket *)sock shouldTimeoutReadWithTag:(long)tag
                 elapsed:(NSTimeInterval)elapsed
               bytesDone:(NSUInteger)length
{
  callbackLogError(@"Read timed out...");
  return 0;
}

- (void)socketDidDisconnect:(GCDAsyncSocket *)sock withError:(NSError *)err
{
  callbackLogInfo(FORMAT(@"Socket was closed."));
  controlsToggleCallback(NO);
}

- (void)socket:(GCDAsyncSocket *)sock didConnectToHost:(NSString *)host port:(uint16_t)port {
  callbackLogInfo(FORMAT(@"Connected to %@:%i", host, port));
  controlsToggleCallback(YES);
  
#if TARGET_OS_IPHONE
  // Compiling for iOS
  NSString *hostname = FORMAT(@"iOS:%@:%i", [sock localHost], [sock localPort]);
#else
  // Compiling for Mac OS X
  NSString *hostname = [[NSHost currentHost] localizedName];  
#endif

  commonFunc.hostname = hostname;
  
  // Tell the other end that we want to start jitter measurements on the port we are listening to.
  [socket writeData:[SharedCode payloadForString:hostname] withTimeout:-1 tag:CLIENTID];
  [socket writeData:[SharedCode intToData:[jitterSocket localPort]] withTimeout:-1 tag:CLIENTJITTERPORT];
}

- (void)socket:(GCDAsyncSocket *)sock didWriteDataWithTag:(long)tag {
  switch (tag) {
    case PING:
      startTimerLatency = [commonFunc startLatencyMeasurement];
      break;
      
    case PONG:
      startTimerBandwidth = [commonFunc startBandwidthMeasurement];
      break;
      
    case DOWNSTREAM_BW:
      // We sent the downstream bandwidth number to the server
      break;
      
    case DATAFROMCLIENT:
      // We sent the data payload to measure upstream bandwidth
      break;
      
    case CLIENTID:
      break;
      
    case CLIENTJITTERPORT:
      [socket readDataToData:[GCDAsyncSocket CRLFData] withTimeout:-1 tag:SERVERJITTERPORT];
      // We told the server which port we are listening on.
      break;
      
    default:
      break;
  }
}

- (void)socket:(GCDAsyncSocket *)sock didReadData:(NSData *)data withTag:(long)tag {
  switch (tag) {
    case PANG:
    {
      clientLatency = [commonFunc concludeLatencyMeasurementForDate:startTimerLatency];
      
      NSInteger clientLatencyInMicroseconds = clientLatency * 1000;
      [socket writeData:[SharedCode intToData:clientLatencyInMicroseconds] withTimeout:-1 tag:PONG];
      
      // The server sent us how many bytes to expect.
      NSInteger numBytesToExpect = [SharedCode dataToInt:data];
      [commonFunc setNumberOfBytesForDataMeasurements:numBytesToExpect];
      [socket readDataToLength:numBytesToExpect withTimeout:READ_TIMEOUT tag:DATAFROMSERVER];
      
      callbackLogInfo(FORMAT(@"Latency: %fms", clientLatency));
      break;
    } 
      
    case DATAFROMSERVER: 
    {
      double mbitsPerSecond = [commonFunc getBandwidthInMegabitsPerSecondForStartTime:startTimerBandwidth];
      NSInteger downstreamBandwidthInKb = mbitsPerSecond * 1000;
      NSData *data = [SharedCode intToData:downstreamBandwidthInKb];
      [socket writeData:data withTimeout:-1 tag:DOWNSTREAM_BW];
      [socket readDataToData:[GCDAsyncSocket CRLFData] withTimeout:READ_TIMEOUT tag:PENG];
      
      callbackLogInfo(FORMAT(@"Downstream bandwidth: %f", mbitsPerSecond));
      
      // Keep the measurement for later
      downstreamBandwidth = mbitsPerSecond;
      break;
    }
      
    case PENG:
    {      
      // The server now wants us to send data back.
      [socket writeData:[commonFunc dataPayload] withTimeout:-1 tag:DATAFROMCLIENT];
      [socket readDataToData:[GCDAsyncSocket CRLFData] withTimeout:-1 tag:UPSTREAM_BW];
      
      serverLatency = (double) [SharedCode dataToInt:data] / 1000.0;
      callbackLogInfo(FORMAT(@"Server latency: %f", serverLatency));

      break;
    }
      
    case UPSTREAM_BW:
    {
      NSInteger upstreamBandwidthInKbit = [SharedCode dataToInt:data];
      upstreamBandwidth = (double) upstreamBandwidthInKbit / 1000.0;
      callbackLogInfo(FORMAT(@"Upstream bandwidth: %f", upstreamBandwidth));
      goodputLatencyCallback(downstreamBandwidth, clientLatency, upstreamBandwidth, serverLatency);
      
      // Start the next round of measurements
      dispatch_async(dispatch_get_main_queue(), ^{
        [self startPingPangPongData];
      });
      
      break;
    }
      
    case SERVERJITTERPORT:
    {
      serverJitterPort = [SharedCode dataToInt:data];

      callbackLogInfo(FORMAT(@"Server jitter port: %i", serverJitterPort));
      NSString *host = [sock connectedHost];
      callbackLogInfo(FORMAT(@"Sending jitter to port %@:%i", host, serverJitterPort));

      NSArray *objects = [NSArray arrayWithObjects:host, [NSNumber numberWithInt:serverJitterPort], @"server", jitterSocket, sock, nil];
      NSArray *keys = [NSArray arrayWithObjects:@"host", @"port", @"hostname", @"sendSocket", @"receiveSocket", nil];
      NSDictionary *infoDict = [NSDictionary dictionaryWithObjects:objects forKeys:keys];
      [commonFunc performSelectorInBackground:@selector(performJitterMeasurements:) withObject:infoDict];        

      dispatch_async(dispatch_get_main_queue(), ^{
        [self startPingPangPongData];
      });

      break;
    }
      
    default:
      break;
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
  serverhost = @"server";
  [commonFunc addJitterMeasurement:timeDiff forHost:serverhost];
  serverJitter = [[SharedCode hostJitterFromData:data] doubleValue];
}



@end

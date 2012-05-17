//
//  AppDelegate.m
//  SignpostDemoClient
//
//  Created by Sebastian Eide on 07/05/2012.
//  Copyright (c) 2012 Kle.io. All rights reserved.
//

#import "AppDelegate.h"
#import "GCDAsyncSocket.h"
#import "GCDAsyncUdpSocket.h"
#import "SharedCode.h"

@implementation AppDelegate

@synthesize window = _window;

@synthesize connectButton = _connectButton;
@synthesize hostField = _hostField;
@synthesize portField = _portField;
@synthesize statusView = _statusView;
@synthesize jitterLabel = _jitterLabel;

- (void)applicationDidFinishLaunching:(NSNotification *)aNotification
{
  socketQueue = dispatch_queue_create("socketQueue", NULL);
  socket = [[GCDAsyncSocket alloc] initWithDelegate:self delegateQueue:socketQueue];
  
  jitterSocketQueue = dispatch_queue_create("jitterSocketQeueue", NULL);
  jitterSocket = [[GCDAsyncUdpSocket alloc] initWithDelegate:self delegateQueue:jitterSocketQueue];
  
  isConnected = NO;
  [self.statusView setString:@""];
    
  commonFunc = [[SharedCode alloc] init];
  
  [self setupJitterRecevingComms];
}

- (void) setupJitterRecevingComms
{
  NSError *jitterError = nil;
  if (![jitterSocket bindToPort:0 error:&jitterError])
  {
    [self logError:FORMAT(@"Error setting up jitter port: %@", jitterError)];
    return;
  }
  int jitterPort = [jitterSocket localPort];
  [self logInfo:FORMAT(@"Listening for jitter info on port %i", jitterPort)];
  if (![jitterSocket beginReceiving:&jitterError]) 
  {
    [self logError:FORMAT(@"Error beginning receiving on jitter UDP stream: %@", jitterError)];
    return;  
  }
}

- (IBAction)connectToHostButtonClicked:(id)sender 
{
  int port = [self.portField intValue];
  if (port == 0)
    port = 7777;
  
  NSString *host = [self.hostField stringValue];
  if ([host isEqualToString:@""])
    host = @"localhost";
  
  if(!isConnected)
	{
    // Many things need to get done:
    // 1) Connect to the server for TCP type measurements:
    // - Latency
    // - Goodput
    // 2) Open a UDP socket for jitter measurements
    // 3) Tell the server what port we are listening on for jitter measurements
    
    // 1) Opening TCP socket to server
    NSError *error = nil;
    if (![socket connectToHost:host onPort:port error:&error]) {
			[self logError:FORMAT(@"Error connecting: %@", error)];
      return;
    }
    
    // 2) Opening listening UDP socket
    // Done elsewhere
    
    // 3) Tell the server what our jitter port is.
    //    We do this over TCP when the channel has been established.
    
    // 4) General mechanics    
    [self setIsConnectedAndDisableControls];
	}
	else
	{
		// Stop accepting connections
		[socket disconnect];
    [jitterSocket close];
				
		[self logInfo:FORMAT(@"Disconnected from %@:%i.", host, port)];
    [self setIsDisconnectedAndEnableControls];
	}
}

- (IBAction)runPingPangPongData:(id)sender {
  [self startPingPangPongData];
}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
#pragma mark - Misc for logging and displaying info
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

- (void) setIsConnectedAndDisableControls 
{
  isConnected = YES;
  [self.portField setEnabled:NO];
  [self.hostField setEnabled:NO];
  [self.connectButton setTitle:@"Disconnect"];
  
}

- (void) setIsDisconnectedAndEnableControls 
{
  isConnected = NO;
  [jitterSocket close];
  [self.portField setEnabled:YES];
  [self.hostField setEnabled:YES];
  [self.connectButton setTitle:@"Connect"];

}

- (void) startPingPangPongData 
{
  [socket writeData:[SharedCode payloadForString:@"ping"] withTimeout:-1 tag:PING];
  [socket readDataToData:[GCDAsyncSocket CRLFData] withTimeout:READ_TIMEOUT tag:PANG];
}

- (void)logError:(NSString *)msg
{
	NSString *paragraph = [NSString stringWithFormat:@"%@\n", msg];
	
	NSMutableDictionary *attributes = [NSMutableDictionary dictionaryWithCapacity:1];
	[attributes setObject:[NSColor redColor] forKey:NSForegroundColorAttributeName];
	
	NSAttributedString *as = [[NSAttributedString alloc] initWithString:paragraph attributes:attributes];
	
	[[self.statusView textStorage] appendAttributedString:as];
}

- (void)logInfo:(NSString *)msg
{
	NSString *paragraph = [NSString stringWithFormat:@"%@\n", msg];
	
	NSMutableDictionary *attributes = [NSMutableDictionary dictionaryWithCapacity:1];
	[attributes setObject:[NSColor purpleColor] forKey:NSForegroundColorAttributeName];
	
	NSAttributedString *as = [[NSAttributedString alloc] initWithString:paragraph attributes:attributes];
	
	[[self.statusView textStorage] appendAttributedString:as];
}

- (void)logMessage:(NSString *)msg
{  
	NSString *paragraph = [NSString stringWithFormat:@"%@\n", msg];
	
	NSMutableDictionary *attributes = [NSMutableDictionary dictionaryWithCapacity:1];
	[attributes setObject:[NSColor blackColor] forKey:NSForegroundColorAttributeName];
	
	NSAttributedString *as = [[NSAttributedString alloc] initWithString:paragraph attributes:attributes];
	
	[[self.statusView textStorage] appendAttributedString:as];
}

- (void)updateJitterLabel {
  @autoreleasepool 
  {
    struct timespec a;
    NSInteger val = 400000000;
    a.tv_nsec = val;
    a.tv_sec = 0;
    

    while ([socket isConnected])
    {
      nanosleep(&a, NULL);
      double localJitter = [[commonFunc currentJitterForHost:serverhost] doubleValue];
      [self.jitterLabel setStringValue:[NSString stringWithFormat:@"seen locally: %fms, seen on server: %fms", localJitter, serverJitter]];      
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
  [self logError:@"Read timed out..."];
  return 0;
}

- (void)socketDidDisconnect:(GCDAsyncSocket *)sock withError:(NSError *)err
{
  [self logInfo:FORMAT(@"Socket was closed.")];
  [self setIsDisconnectedAndEnableControls];
}

- (void)socket:(GCDAsyncSocket *)sock didConnectToHost:(NSString *)host port:(uint16_t)port {
  [self logInfo:FORMAT(@"Connected to %@:%i", host, port)];
  [self performSelectorInBackground:@selector(updateJitterLabel) withObject:nil];
  
  NSString *hostname = FORMAT(@"%@:%i", [sock localHost], [sock localPort]);
  commonFunc.hostname = hostname;
  
  // Tell the other end that we want to start jitter measurements on the port we are listening to.
  [socket writeData:[SharedCode payloadForString:hostname] withTimeout:-1 tag:CLIENTID];
  [socket writeData:[SharedCode intToData:[jitterSocket localPort]] withTimeout:-1 tag:CLIENTJITTERPORT];
  NSLog(@"Write clientjitterport in didConnectToHost: %@:%i", host, port);
}

- (void)socket:(GCDAsyncSocket *)sock didWriteDataWithTag:(long)tag {
  switch (tag) {
    case PING:
      startTimerLatency = [commonFunc startLatencyMeasurement];
      NSLog(@"Sent ping");
      break;
      
    case PONG:
      startTimerBandwidth = [commonFunc startBandwidthMeasurement];
      NSLog(@"Sent pong");
      break;
    
    case DOWNSTREAM_BW:
      // We sent the downstream bandwidth number to the server
      NSLog(@"Sent downstream bandwidth");
      break;
    
    case DATAFROMCLIENT:
      // We sent the data payload to measure upstream bandwidth
      NSLog(@"Sent data from client");
      break;

    case CLIENTID:
      NSLog(@"Sent clientId");
      break;
      
    case CLIENTJITTERPORT:
      [socket readDataToData:[GCDAsyncSocket CRLFData] withTimeout:-1 tag:SERVERJITTERPORT];
      NSLog(@"Registered to read SERVERJITTERPORT after sending CLIENTJITTERPORT");
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

      NSLog(@"Received pang, sent pong, waiting for datafromserver");
      
      dispatch_async(dispatch_get_main_queue(), ^{
        @autoreleasepool {[self logInfo:FORMAT(@"Latency: %fms", clientLatency)];}
      });
      break;
    } 

    case DATAFROMSERVER: 
    {
      double mbitsPerSecond = [commonFunc getBandwidthInMegabitsPerSecondForStartTime:startTimerBandwidth];
      NSInteger downstreamBandwidthInKb = mbitsPerSecond * 1000;
      NSData *data = [SharedCode intToData:downstreamBandwidthInKb];
      [socket writeData:data withTimeout:-1 tag:DOWNSTREAM_BW];
      [socket readDataToData:[GCDAsyncSocket CRLFData] withTimeout:READ_TIMEOUT tag:PENG];
      
      NSLog(@"Received datafromserver, sent downstream_bw, waiting for peng");
      
      dispatch_async(dispatch_get_main_queue(), ^{
        @autoreleasepool 
        {
          [self logInfo:FORMAT(@"Downstream bandwidth: %f", mbitsPerSecond)];
        }
      });
      break;
    }

    case PENG:
    {      
      // The server now wants us to send data back.
      [socket writeData:[commonFunc dataPayload] withTimeout:-1 tag:DATAFROMCLIENT];
      [socket readDataToData:[GCDAsyncSocket CRLFData] withTimeout:-1 tag:UPSTREAM_BW];
      
      serverLatency = (double) [SharedCode dataToInt:data] / 1000.0;
      dispatch_async(dispatch_get_main_queue(), ^{
        @autoreleasepool 
        {
          [self logInfo:FORMAT(@"Server latency: %f", serverLatency)];
        }
      });
      
      NSLog(@"Received peng, sent datafromclient, waiting for upstrambw");
      break;
    }

    case UPSTREAM_BW:
    {
      NSInteger upstreamBandwidthInKbit = [SharedCode dataToInt:data];
      double upstreamBandwidth = (double) upstreamBandwidthInKbit / 1000.0;
      dispatch_async(dispatch_get_main_queue(), ^{
        @autoreleasepool {[self logInfo:FORMAT(@"Upstream bandwidth: %f", upstreamBandwidth)];}
      });
      NSLog(@"Received upstream_bw, did nothing in return");
      break;
    }
    
    case SERVERJITTERPORT:
    {
      serverJitterPort = [SharedCode dataToInt:data];

      dispatch_async(dispatch_get_main_queue(), ^{
        @autoreleasepool {[self logInfo:FORMAT(@"Server jitter port: %i", serverJitterPort)];}
        
        NSString *host = [sock connectedHost];
        [self logInfo:FORMAT(@"Sending jitter to port %@:%i", host, serverJitterPort)];
        
        NSArray *objects = [NSArray arrayWithObjects:host, [NSNumber numberWithInt:serverJitterPort], @"server", jitterSocket, sock, nil];
        NSArray *keys = [NSArray arrayWithObjects:@"host", @"port", @"hostname", @"sendSocket", @"receiveSocket", nil];
        NSDictionary *infoDict = [NSDictionary dictionaryWithObjects:objects forKeys:keys];
        [commonFunc performSelectorInBackground:@selector(performJitterMeasurements:) withObject:infoDict];
      });
      
      NSLog(@"Reveived serverjitterport, did nothing in return. (%lu)", serverJitterPort);
      // Initiate the server jitter sending on a separate thread, but from the main thread.
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
//  serverhost = [SharedCode hostFromData:data];
  serverhost = @"server";
  NSLog(@"received jitter probe from: %@", serverhost);
  [commonFunc addJitterMeasurement:timeDiff forHost:serverhost];
  serverJitter = [[SharedCode hostJitterFromData:data] doubleValue];
}

@end

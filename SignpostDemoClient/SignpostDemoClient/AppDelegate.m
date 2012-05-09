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

#define READ_TIMEOUT 15.0
#define READ_TIMEOUT_EXTENSION 10.0

#define FORMAT(format, ...) [NSString stringWithFormat:(format), ##__VA_ARGS__]


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
  NSString *ping = @"ping";
  NSData *data = [ping dataUsingEncoding:NSUTF8StringEncoding];
  [socket writeData:data withTimeout:-1 tag:PING];
  [socket readDataToLength:4 withTimeout:READ_TIMEOUT tag:PANG];
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

  commonFunc.hostname = FORMAT(@"%@:%i", [sock localHost], [sock localPort]);
  
  // Tell the other end that we want to start jitter measurements on the port we are listening to.
  NSString *connString = FORMAT(@"%i\r\n", [jitterSocket localPort]);
  NSData *portData = [connString dataUsingEncoding:NSUTF8StringEncoding];
  [socket writeData:portData withTimeout:-1 tag:CLIENTJITTERPORT];
  NSLog(@"Write clientjitterport in didConnectToHost: %@:%i", host, port);
}

- (void)socket:(GCDAsyncSocket *)sock didWriteDataWithTag:(long)tag {
  switch (tag) {
    case PING:
      [commonFunc startLatencyMeasurement];
      break;
      
    case PONG:
      [commonFunc startBandwidthMeasurement];
      break;
    
    case DOWNSTREAM_BW:
      // We sent the downstream bandwidth number to the server
      break;
    
    case DATAFROMCLIENT:
      // We sent the data payload to measure upstream bandwidth
      break;

    case CLIENTJITTERPORT:
      [socket readDataToData:[GCDAsyncSocket CRLFData] withTimeout:-1 tag:SERVERJITTERPORT];
      NSLog(@"Registered to read SERVERJITTERPORT after sending clientjitterport");
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
      [commonFunc concludeLatencyMeasurement];
      double latency = [commonFunc latency];
      
      NSData *data = [SharedCode payloadForString:@"pong"];
      [socket writeData:data withTimeout:-1 tag:PONG];
      [socket readDataToLength:DATASIZE withTimeout:READ_TIMEOUT tag:DATAFROMSERVER];

      NSLog(@"Received pang, sent pong, waiting for datafromserver");
      
      dispatch_async(dispatch_get_main_queue(), ^{
        @autoreleasepool {[self logInfo:FORMAT(@"Latency: %fms", latency)];}
      });
      break;
    } 

    case DATAFROMSERVER: 
    {
      NSInteger mbitsPerSecond = [commonFunc getBandwidthInMegabitsPerSecond];
      NSData *data = [SharedCode intToData:mbitsPerSecond];
      [socket writeData:data withTimeout:-1 tag:DOWNSTREAM_BW];
      [socket readDataToLength:4 withTimeout:READ_TIMEOUT tag:PENG];
      
      NSLog(@"Received datafromserver, sent downstream_bw, waiting for peng");
      
      dispatch_async(dispatch_get_main_queue(), ^{
        @autoreleasepool {[self logInfo:FORMAT(@"Downstream bandwidth: %i", mbitsPerSecond)];}
      });
      break;
    }

    case PENG:
    {      
      // The server now wants us to send data back.
      [socket writeData:[commonFunc dataPayload] withTimeout:-1 tag:DATAFROMCLIENT];
      [socket readDataToData:[GCDAsyncSocket CRLFData] withTimeout:-1 tag:UPSTREAM_BW];
      NSLog(@"Received peng, sent datafromclient, waiting for upstrambw");
      break;
    }

    case UPSTREAM_BW:
    {
      NSInteger upstreamBandwith = [SharedCode dataToInt:data];
      dispatch_async(dispatch_get_main_queue(), ^{
        @autoreleasepool {[self logInfo:FORMAT(@"Upstream bandwidth: %i", upstreamBandwith)];}
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
        
        NSArray *objects = [NSArray arrayWithObjects:host, [NSNumber numberWithInt:serverJitterPort], jitterSocket, sock, nil];
        NSArray *keys = [NSArray arrayWithObjects:@"host", @"port", @"sendSocket", @"receiveSocket", nil];
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
  NSString *host = [SharedCode hostFromData:data];
  [commonFunc addJitterMeasurement:timeDiff forHost:host];
  double localJitter = [[commonFunc currentJitterForHost:host] doubleValue];
  double serverJitter = [[SharedCode hostJitterFromData:data] doubleValue];
  [self.jitterLabel setStringValue:[NSString stringWithFormat:@"seen locally: %fms, seen on server: %fms", localJitter, serverJitter]];
}

@end

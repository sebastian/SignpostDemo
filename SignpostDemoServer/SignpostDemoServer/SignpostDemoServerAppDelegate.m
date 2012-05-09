//
//  AppDelegate.m
//  SignpostDemoServer
//
//  Created by Sebastian Eide on 07/05/2012.
//  Copyright (c) 2012 Kle.io. All rights reserved.
//

#import "SignpostDemoServerAppDelegate.h"
#import "GCDAsyncSocket.h"
#import "GCDAsyncUdpSocket.h"
#import "SharedCode.h"

#define INTERMEDIATE_READ_TIMEOUT 10.0

@implementation SignpostDemoServerAppDelegate

@synthesize window = _window;
@synthesize port = _port;
@synthesize statusMessages = _statusMessages;
@synthesize startStopButton = _startStopButton;
@synthesize jitterLabel = _jitterLabel;


////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
#pragma mark - Setting up connection
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

- (void)applicationDidFinishLaunching:(NSNotification *)aNotification
{
  socketQueue = dispatch_queue_create("socketQueue", NULL);
  listenSocket = [[GCDAsyncSocket alloc] initWithDelegate:self delegateQueue:socketQueue];
  connectedSockets = [[NSMutableArray alloc] initWithCapacity:1];
  
  jitterSocketQueue = dispatch_queue_create("jitterSocketQueue", NULL);
  jitterSocket = [[GCDAsyncUdpSocket alloc] initWithDelegate:self delegateQueue:jitterSocketQueue];

  isRunning = NO;
  [self.statusMessages setString:@""];
  
  // Set the data values
  commFunc = [[SharedCode alloc] init];
}

- (IBAction) pushedStartStopButton:(id)sender 
{
	if(!isRunning)
	{
		int port = [self.port intValue];
		
		if (port < 0 || port > 65535)
		{
			[self.port setStringValue:@""];
			port = 0;
		}
		
		NSError *error = nil;
		if(![listenSocket acceptOnPort:port error:&error])
		{
			[self logError:FORMAT(@"Error starting server: %@", error)];
			return;
		}
    commFunc.hostname = FORMAT(@"%@:%i", [listenSocket localHost], [listenSocket localPort]);

    [self logInfo:FORMAT(@"Signpost demo server started on port %hu", [listenSocket localPort])];
		isRunning = YES;
    
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

		[self.port setEnabled:NO];
		[self.startStopButton setTitle:@"Stop"];
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
		
		[self logInfo:@"Stopped Signpost demo server"];
		isRunning = false;
		
		[self.port setEnabled:YES];
		[self.startStopButton setTitle:@"Start"];
	}  
}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
#pragma mark - Misc for logging and displaying info
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

- (void)scrollToBottom
{
	NSScrollView *scrollView = [self.statusMessages enclosingScrollView];
	NSPoint newScrollOrigin;  
	
	if ([[scrollView documentView] isFlipped])
		newScrollOrigin = NSMakePoint(0.0F, NSMaxY([[scrollView documentView] frame]));
	else
		newScrollOrigin = NSMakePoint(0.0F, 0.0F);
	
	[[scrollView documentView] scrollPoint:newScrollOrigin];
}

- (void)logError:(NSString *)msg
{
	NSString *paragraph = [NSString stringWithFormat:@"%@\n", msg];
	
	NSMutableDictionary *attributes = [NSMutableDictionary dictionaryWithCapacity:1];
	[attributes setObject:[NSColor redColor] forKey:NSForegroundColorAttributeName];
	
	NSAttributedString *as = [[NSAttributedString alloc] initWithString:paragraph attributes:attributes];
	
	[[self.statusMessages textStorage] appendAttributedString:as];
	[self scrollToBottom];
}

- (void)logInfo:(NSString *)msg
{
	NSString *paragraph = [NSString stringWithFormat:@"%@\n", msg];
	
	NSMutableDictionary *attributes = [NSMutableDictionary dictionaryWithCapacity:1];
	[attributes setObject:[NSColor purpleColor] forKey:NSForegroundColorAttributeName];
	
	NSAttributedString *as = [[NSAttributedString alloc] initWithString:paragraph attributes:attributes];
	
	[[self.statusMessages textStorage] appendAttributedString:as];
	[self scrollToBottom];
}

- (void)logMessage:(NSString *)msg
{
	NSString *paragraph = [NSString stringWithFormat:@"%@\n", msg];
	
	NSMutableDictionary *attributes = [NSMutableDictionary dictionaryWithCapacity:1];
	[attributes setObject:[NSColor blackColor] forKey:NSForegroundColorAttributeName];
	
	NSAttributedString *as = [[NSAttributedString alloc] initWithString:paragraph attributes:attributes];
	
	[[self.statusMessages textStorage] appendAttributedString:as];
	[self scrollToBottom];
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
	
	dispatch_async(dispatch_get_main_queue(), ^{
		@autoreleasepool {
			[self logInfo:FORMAT(@"Accepted client %@:%hu", host, port)];
		}
	});
  [newSocket readDataToData:[GCDAsyncSocket CRLFData] withTimeout:15 tag:CLIENTJITTERPORT];
}

- (void)socket:(GCDAsyncSocket *)sock didWriteDataWithTag:(long)tag
{
	// This method is executed on the socketQueue (not the main thread)
	switch (tag) {
    case PANG:
      [commFunc startLatencyMeasurement];
      break;
      
    case DATAFROMSERVER:
      // Send the data to the user... the user can now do a bandwidth test
      break;
    
    case PENG:
      [commFunc startBandwidthMeasurement];
      break;
      
    case UPSTREAM_BW:
      // Sent the upstream bandwidth data back to the client;
      break;
      
    case SERVERJITTERPORT:
      // We sent the client the jitter port we are listening to
      break;
      
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
      [sock writeData:[SharedCode payloadForString:@"pang"] withTimeout:-1 tag:PANG];
      [sock readDataToLength:4 withTimeout:INTERMEDIATE_READ_TIMEOUT tag:PONG];
      NSLog(@"Received ping, wrote pang, waiting for pong");
      break;
    }

    case PONG:
    {
      [commFunc concludeLatencyMeasurement];
      double latency = [commFunc latency];
    
      [sock writeData:[commFunc dataPayload] withTimeout:-1 tag:DATAFROMSERVER];
      [sock readDataToData:[GCDAsyncSocket CRLFData] withTimeout:-1 tag:DOWNSTREAM_BW];
      NSLog(@"Received pong, sent datafromserver, waiting for downstream_bw");

      dispatch_async(dispatch_get_main_queue(), ^{
        @autoreleasepool {[self logInfo:FORMAT(@"Latency to client: %f", latency)];}
      });
      break;
    }

    case DOWNSTREAM_BW:
    {
      [sock writeData:[SharedCode payloadForString:@"PENG"] withTimeout:-1 tag:PENG];
      [sock readDataToLength:DATASIZE withTimeout:-1 tag:DATAFROMCLIENT];
      
      dispatch_async(dispatch_get_main_queue(), ^{
        NSInteger downstreamBandwidth = [SharedCode dataToInt:data];
        @autoreleasepool {[self logInfo:FORMAT(@"Downstream bandwidth: %i", downstreamBandwidth)];}
      });
      break;
    }

    case DATAFROMCLIENT:
    {
      NSInteger upstreamBandwidth = [commFunc getBandwidthInMegabitsPerSecond];
      NSData *data = [SharedCode intToData:upstreamBandwidth];
      [sock writeData:data withTimeout:-1 tag:UPSTREAM_BW];
      [sock readDataToLength:4 withTimeout:-1 tag:PING];
      dispatch_async(dispatch_get_main_queue(), ^{
        @autoreleasepool {[self logInfo:FORMAT(@"Upstream bandwidth: %i", upstreamBandwidth)];}
      });
      break;
    }

    case CLIENTJITTERPORT:
    {
      // We need to setup a jitter listening port for the client, and listen to it too.
      [sock writeData:[SharedCode intToData:[jitterSocket localPort]] withTimeout:-1 tag:SERVERJITTERPORT];

      // We are done with handshaking, so the next packet we should be expecting is a ping packet.
      [sock readDataToLength:4 withTimeout:-1 tag:PING];
      
      dispatch_async(dispatch_get_main_queue(), ^{
        NSInteger portNum = [SharedCode dataToInt:data];
        NSString *host = [sock connectedHost];
        [self logInfo:FORMAT(@"Sending jitter to port %@:%i", host, portNum)];
        
        NSArray *objects = [NSArray arrayWithObjects:host, [NSNumber numberWithInt:portNum], jitterSocket, sock, nil];
        NSArray *keys = [NSArray arrayWithObjects:@"host", @"port", @"sendSocket", @"receiveSocket", nil];
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
  dispatch_async(dispatch_get_main_queue(), ^{
    @autoreleasepool {
      NSString *host = [sock connectedHost];
      UInt16 port = [sock connectedPort];
      [self logInfo:FORMAT(@"Client timed out: %@:%i", host, port)];
    }
  });	
	return 0.0;
}

- (void)socketDidDisconnect:(GCDAsyncSocket *)sock withError:(NSError *)err
{
	if (sock != listenSocket)
	{
		dispatch_async(dispatch_get_main_queue(), ^{
			@autoreleasepool {
        
				[self logInfo:FORMAT(@"Client Disconnected")];
        
			}
		});
		
		@synchronized(connectedSockets)
		{
			[connectedSockets removeObject:sock];
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
  [commFunc addJitterMeasurement:timeDiff forHost:host];
  double jitter = [[commFunc currentJitterForHost:host] doubleValue];
  [self.jitterLabel setStringValue:[NSString stringWithFormat:@"%f ms", jitter]];
}

@end

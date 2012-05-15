//
//  ViewController.m
//  SignpostDemoiPhone
//
//  Created by Sebastian Eide on 09/05/2012.
//  Copyright (c) 2012 Kle.io. All rights reserved.
//

#import "ViewController.h"

#import "GCDAsyncSocket.h"
#import "GCDAsyncUdpSocket.h"
#import "SharedCode.h"

#import "Meter.h"

@interface ViewController ()

@end

@implementation ViewController

@synthesize connectButton = _connectButton;
@synthesize hostField = _hostField;
@synthesize portField = _portField;
@synthesize jitterLabel = _jitterLabel;


////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
#pragma mark - UIViewController
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

- (id) initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil
{
  self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
  if (self)
  {
    NSLog(@"In the init");
    socketQueue = dispatch_queue_create("socketQueue", NULL);
    socket = [[GCDAsyncSocket alloc] initWithDelegate:self delegateQueue:socketQueue];
    
    jitterSocketQueue = dispatch_queue_create("jitterSocketQeueue", NULL);
    jitterSocket = [[GCDAsyncUdpSocket alloc] initWithDelegate:self delegateQueue:jitterSocketQueue];
    
    isConnected = NO;
    
    commonFunc = [[SharedCode alloc] init];    
    
    averageJitter = 0.0;
  }
  return self;
}

- (void)viewDidLoad
{
  [super viewDidLoad];
  
  meter = [[Meter alloc] init];
  CGRect meterFrame = CGRectMake(0, 200, 200, 200);
  meter.view.frame = meterFrame;
  [self.view addSubview:meter.view];
  [meter setMaxValue:300.0];
}

- (void)viewDidUnload
{
  [super viewDidUnload];
}

- (BOOL)shouldAutorotateToInterfaceOrientation:(UIInterfaceOrientation)interfaceOrientation
{
  return (interfaceOrientation != UIInterfaceOrientationPortraitUpsideDown);
}


////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
#pragma mark - IBActions
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

- (IBAction)rotateNeedle:(id)sender
{
  __unsafe_unretained Meter *m = meter;
  [meter setCurrentValue:40 withCallback:^{
    __unsafe_unretained Meter *m2 = m;
    [m setCurrentValue:0 withCallback:^{
      __unsafe_unretained Meter *m3 = m2;      
      [m2 setCurrentValue:100 withCallback:^{
        [m3 setCurrentValue:50 withCallback:^{
          NSLog(@"Round complete");
        }];
      }];
    }];
  }];
}

- (IBAction)connectToHostButtonClicked:(id)sender 
{
  int port = [[self.portField text] intValue];
  if (port == 0)
    port = 7777;
  
  NSString *host = [self.hostField text];
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
			NSLog(@"Error connecting: %@", [error debugDescription]);
      return;
    }
    
    // 2) Opening listening UDP socket
    NSError *jitterError = nil;
    if (![jitterSocket bindToPort:0 error:&jitterError])
    {
      NSLog(@"Error setting up jitter port: %@", [jitterError debugDescription]);
      return;
    }
    int jitterPort = [jitterSocket localPort];
    NSLog(@"Listening for jitter info on port %i", jitterPort);
    if (![jitterSocket beginReceiving:&jitterError]) 
    {
      NSLog(@"Error beginning receiving on jitter UDP stream: %@", jitterError);
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
    
		NSLog(@"Disconnected from %@:%i.", host, port);
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
  self.connectButton.titleLabel.text = @"Disconnect";
}

- (void) setIsDisconnectedAndEnableControls 
{
  isConnected = NO;
  [jitterSocket close];
  [self.portField setEnabled:YES];
  [self.hostField setEnabled:YES];
  self.connectButton.titleLabel.text = @"Connect";
}

- (void) startPingPangPongData 
{
  [socket writeData:[SharedCode payloadForString:@"ping"] withTimeout:-1 tag:PING];
  [socket readDataToData:[GCDAsyncSocket CRLFData] withTimeout:READ_TIMEOUT tag:PANG];
}

- (void) updateJitterDisplay
{
  @autoreleasepool 
  {
    struct timespec a;
    NSInteger val = 40000000;
    a.tv_nsec = val;
    a.tv_sec = 0;
        
    while ([socket isConnected])
    {
      nanosleep(&a, NULL);
      double localJitter = [[commonFunc currentJitterForHost:jitterHost] doubleValue];
      dispatch_async(dispatch_get_main_queue(), ^{
        @autoreleasepool {
          self.jitterLabel.text = [NSString stringWithFormat:@"seen locally: %fms, seen on server: %fms", localJitter, serverJitter];
          [meter setCurrentValue:averageJitter withCallback:^{}];
        }
      });
      [self.jitterLabel setNeedsLayout];
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
  NSLog(@"Read timed out...");
  return 0;
}

- (void)socketDidDisconnect:(GCDAsyncSocket *)sock withError:(NSError *)err
{
  NSLog(@"Socket was closed.");
  [self setIsDisconnectedAndEnableControls];
}

- (void)socket:(GCDAsyncSocket *)sock didConnectToHost:(NSString *)host port:(uint16_t)port {
  NSLog(@"Connected to %@:%i", host, port);
  
  [self performSelectorInBackground:@selector(updateJitterDisplay) withObject:nil];
  
  commonFunc.hostname = FORMAT(@"%@:%i", [sock localHost], [sock localPort]);
  
  // Tell the other end that we want to start jitter measurements on the port we are listening to.
  [socket writeData:[SharedCode intToData:[jitterSocket localPort]] withTimeout:-1 tag:CLIENTJITTERPORT];
  NSLog(@"Write clientjitterport in didConnectToHost: %@:%i", host, port);
}

- (void)socket:(GCDAsyncSocket *)sock didWriteDataWithTag:(long)tag {
  switch (tag) {
    case PING:
      [commonFunc startLatencyMeasurement];
      NSLog(@"Write PING on iPhone");
      break;
      
    case PONG:
      [commonFunc startBandwidthMeasurement];
      NSLog(@"Write PONG on iPhone");
      break;
      
    case DOWNSTREAM_BW:
      // We sent the downstream bandwidth number to the server
      NSLog(@"Write DOWNSTREAM_BW on iPhone");
      break;
      
    case DATAFROMCLIENT:
      // We sent the data payload to measure upstream bandwidth
      NSLog(@"Wrote DATAFROMCLIENT on iPhone");
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
      NSLog(@"Received PANG on iPhone");
      [commonFunc concludeLatencyMeasurement];
      clientLatency = [commonFunc latency];
     
      NSInteger clientLatencyInMicroseconds = clientLatency * 1000;
      [socket writeData:[SharedCode intToData:clientLatencyInMicroseconds] withTimeout:-1 tag:PONG];
      
      // The server sent us how many bytes to expect.
      NSInteger numBytesToExpect = [SharedCode dataToInt:data];
      [commonFunc setNumberOfBytesForDataMeasurements:numBytesToExpect];
      [socket readDataToLength:numBytesToExpect withTimeout:READ_TIMEOUT tag:DATAFROMSERVER];

      NSLog(@"Received pang, sent pong, waiting for datafromserver");
      NSLog(@"client latency: %f", clientLatency);
      
      break;
    } 

    case DATAFROMSERVER: 
    {
      double mbitsPerSecond = [commonFunc getBandwidthInMegabitsPerSecond];
      NSInteger downstreamBandwidthInKb = mbitsPerSecond * 1000;
      NSData *data = [SharedCode intToData:downstreamBandwidthInKb];
      [socket writeData:data withTimeout:-1 tag:DOWNSTREAM_BW];
      [socket readDataToData:[GCDAsyncSocket CRLFData] withTimeout:READ_TIMEOUT tag:PENG];
      
      NSLog(@"Received datafromserver, sent downstream_bw, waiting for peng");
      
      NSLog(@"Downstream bandwidth: %f", mbitsPerSecond);
      break;
    }

    case PENG:
    {      
      // The server now wants us to send data back.
      [socket writeData:[commonFunc dataPayload] withTimeout:-1 tag:DATAFROMCLIENT];
      [socket readDataToData:[GCDAsyncSocket CRLFData] withTimeout:-1 tag:UPSTREAM_BW];
      
      serverLatency = (double) [SharedCode dataToInt:data] / 1000.0;
      NSLog(@"Server latency: %f", serverLatency);
      NSLog(@"Received peng, sent datafromclient, waiting for upstrambw");
      break;
    }

    case UPSTREAM_BW:
    {
      NSInteger upstreamBandwidthInKbit = [SharedCode dataToInt:data];
      double upstreamBandwidth = (double) upstreamBandwidthInKbit / 1000.0;
      NSLog(@"Upstream bandwidth: %f", upstreamBandwidth);
      NSLog(@"Received upstream_bw, did nothing in return");
      break;
    }
    
    case SERVERJITTERPORT:
    {
      serverJitterPort = [SharedCode dataToInt:data];

      dispatch_async(dispatch_get_main_queue(), ^{
        NSLog(@"Server jitter port: %i", serverJitterPort);
        
        NSString *host = [sock connectedHost];
        NSLog(@"Sending jitter to port %@:%i", host, serverJitterPort);
        
        NSArray *objects = [NSArray arrayWithObjects:host, [NSNumber numberWithInt:serverJitterPort], jitterSocket, sock, nil];
        NSArray *keys = [NSArray arrayWithObjects:@"host", @"port", @"sendSocket", @"receiveSocket", nil];
        NSDictionary *infoDict = [NSDictionary dictionaryWithObjects:objects forKeys:keys];
        [commonFunc performSelectorInBackground:@selector(performJitterMeasurements:) withObject:infoDict];
      });
      
      NSLog(@"Reveived serverjitterport, did nothing in return. (%u)", serverJitterPort);
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
  jitterHost = [SharedCode hostFromData:data];
  [commonFunc addJitterMeasurement:timeDiff forHost:jitterHost];
  serverJitter = [[SharedCode hostJitterFromData:data] doubleValue];
  double clientJitter = [[commonFunc currentJitterForHost:jitterHost] doubleValue];
  averageJitter = (serverJitter + clientJitter) / 2.0;
}


////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
#pragma mark - UITextFieldDelegate
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

- (BOOL) textFieldShouldEndEditing:(UITextField *) textfield {
  return YES;
}

- (BOOL)textFieldShouldReturn:(UITextField *)textField {
  [textField resignFirstResponder];
  return YES;
}

@end

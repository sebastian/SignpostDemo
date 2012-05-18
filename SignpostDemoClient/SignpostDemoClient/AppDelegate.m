//
//  AppDelegate.m
//  SignpostDemoClient
//
//  Created by Sebastian Eide on 07/05/2012.
//  Copyright (c) 2012 Kle.io. All rights reserved.
//

#import "AppDelegate.h"
#import "SocketHandler.h"

@implementation AppDelegate

@synthesize window = _window;

@synthesize connectButton = _connectButton;
@synthesize hostField = _hostField;
@synthesize portField = _portField;
@synthesize statusView = _statusView;
@synthesize jitterLabel = _jitterLabel;

- (void)applicationDidFinishLaunching:(NSNotification *)aNotification
{
  socketHandler = [[SocketHandler alloc] initWithLogHandlerMessage:^(NSString *msg) {
    dispatch_async(dispatch_get_main_queue(), ^{
      @autoreleasepool {
        [self logMessage:msg];
      }
    });
  } logHandlerError:^(NSString *errorStr) {
    dispatch_async(dispatch_get_main_queue(), ^{
      @autoreleasepool {
        [self logError:errorStr];
      }
    });
  } logHandlerInfo:^(NSString *infoStr) {
    dispatch_async(dispatch_get_main_queue(), ^{
      @autoreleasepool {
        [self logInfo:infoStr];
      }
    });    
  }];
  [socketHandler setJitterCallbackUpdate:^(double localJitter, double serverJitter) {
    dispatch_async(dispatch_get_main_queue(), ^{
      @autoreleasepool {
        [self.jitterLabel setStringValue:[NSString stringWithFormat:@"seen locally: %fms, seen on server: %fms", localJitter, serverJitter]];      
      }
    });
  }];
  [socketHandler setControlsToggleCallback:^(BOOL enableControls) {
    dispatch_async(dispatch_get_main_queue(), ^{
      @autoreleasepool {
        [self.hostField setEnabled:enableControls];
        [self.portField setEnabled:enableControls];
        if (enableControls)
          [self.connectButton setTitle:@"Connect"];
        else 
          [self.connectButton setTitle:@"Disconnect"];
      }
    });    
  }];
  [socketHandler setGoodputLatencyCallback:^(double downstreamBandwidth, double clientLatency, double upstreamBandwidth, double serverLatency) {
    dispatch_async(dispatch_get_main_queue(), ^{
      NSLog(@"Got measurements and stuff");
    });
  }];
  
  [self.statusView setString:@""];
}

- (IBAction)connectToHostButtonClicked:(id)sender 
{
  NSUInteger port = [self.portField intValue];  
  NSString *host = [self.hostField stringValue];
  [socketHandler startStopSocketForHost:host port:port];
}

- (IBAction)runPingPangPongData:(id)sender {
  [socketHandler startPingPangPongData];
}


////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
#pragma mark - Misc for logging and displaying info
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

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

@end

//
//  AppDelegate.m
//  SignpostDemoServer
//
//  Created by Sebastian Eide on 07/05/2012.
//  Copyright (c) 2012 Kle.io. All rights reserved.
//

#import "SignpostDemoServerAppDelegate.h"
#import "MetricHTTPServer.h"
#import "ServerSocketHandler.h"
#import "ClientData.h"
#import "SharedCode.h"

@implementation SignpostDemoServerAppDelegate

@synthesize window = _window;
@synthesize port = _port;
@synthesize numBytes = _numBytes;
@synthesize statusMessages = _statusMessages;
@synthesize startStopButton = _startStopButton;
@synthesize jitterLabel = _jitterLabel;


////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
#pragma mark - Setting up connection
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

- (void)applicationDidFinishLaunching:(NSNotification *)aNotification
{
  socketHandler = [[ServerSocketHandler alloc] initWithLogHandlerMessage:^(NSString *msg) {
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
  [socketHandler setJitterCallbackUpdate:^(double localJitter, double clientJitter) {
    dispatch_async(dispatch_get_main_queue(), ^{
      @autoreleasepool {
        [self.jitterLabel setStringValue:[NSString stringWithFormat:@"seen locally: %fms, seen by clients: %fms", localJitter, clientJitter]];      
      }
    });
  }];
  
  [self.statusMessages setString:@""];
  [MetricHTTPServer startHTTPServerForDelegate:self];
}


- (IBAction) pushedStartStopButton:(id)sender 
{
  NSUInteger port = [self.port integerValue];
  NSUInteger numKBytes = [self.numBytes integerValue];
  
  if ([socketHandler startStopSocketForPort:port andNumKBytes:numKBytes]) 
  {
    [self.numBytes setEnabled:NO];
    [self.port setEnabled:NO];
    [self.startStopButton setTitle:@"Stop"];  
  }
  else 
  {
    [self.numBytes setEnabled:YES];
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
#pragma mark - MetricHTTPServer delegate methods
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

- (NSData *)dataResponseForHTTPServer:(id)sender
{
  NSArray *keys = [NSArray arrayWithObjects:@"timestamp", @"clients", nil];
  NSDate *date = [NSDate date];

  NSMutableArray *usersData = [[NSMutableArray alloc] init];
  
  [socketHandler executeBlockWithSynchronizedUserData:^(NSMutableDictionary *userData, SharedCode *cf) {
    NSEnumerator *enumerator = [userData keyEnumerator];
    NSString *hostid;
    while (hostid = [enumerator nextObject]) {
      ClientData *cd = [userData objectForKey:hostid];
      
      NSNumber *downstreamBw = [NSNumber numberWithFloat:cd.downstreamBandwidth];
      NSNumber *upstreamBw = [NSNumber numberWithFloat:cd.upstreamBandwidth];
      NSNumber *clientLatency = [NSNumber numberWithFloat:cd.clientLatency];
      NSNumber *serverLatency = [NSNumber numberWithFloat:cd.serverLatency];
      NSNumber *serverJitter = [cf currentJitterForHost:hostid];
      NSNumber *clientJitter = [NSNumber numberWithFloat:cd.clientPerceivedJitter];
      
      NSArray *uk = [NSArray arrayWithObjects:@"id", @"downstream-bandwidth", @"upstream-bandwidth", @"latency-as-seen-by-client", @"latency-as-seen-by-server", @"jitter-as-seen-by-server", @"jitter-as-seen-by-client", nil];
      NSArray *ud = [NSArray arrayWithObjects:hostid, downstreamBw, upstreamBw, clientLatency, serverLatency, serverJitter, clientJitter, nil];
      
      NSDictionary *dataForUser = [NSDictionary dictionaryWithObjects:ud forKeys:uk];
      [usersData addObject:dataForUser];
    }     
  }];

  NSArray *vals = [NSArray arrayWithObjects:[date description], usersData, nil];
  NSDictionary *dataDict = [NSDictionary dictionaryWithObjects:vals forKeys:keys];

  NSError *jsonError;
  NSData *returnData = [NSJSONSerialization dataWithJSONObject:dataDict options:NSJSONWritingPrettyPrinted error:&jsonError];
  if (jsonError)
  {
    NSLog(@"Error converting data to JSON: %@", [jsonError description]);
    return nil;
  }
  return returnData;
}

@end

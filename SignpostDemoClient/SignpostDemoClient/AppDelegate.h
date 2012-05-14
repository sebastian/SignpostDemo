//
//  AppDelegate.h
//  SignpostDemoClient
//
//  Created by Sebastian Eide on 07/05/2012.
//  Copyright (c) 2012 Kle.io. All rights reserved.
//

#import <Cocoa/Cocoa.h>

@class GCDAsyncSocket;
@class GCDAsyncUdpSocket;
@class SharedCode;

@interface AppDelegate : NSObject <NSApplicationDelegate> {
  // Latency and goodput
  dispatch_queue_t socketQueue;
	GCDAsyncSocket *socket;

  // Jitter
  dispatch_queue_t jitterSocketQueue;
	GCDAsyncUdpSocket *jitterSocket;

  // General bookkeeping
	BOOL isConnected;
    
  SharedCode *commonFunc;
  
  // For calculating jitter
  NSUInteger serverJitterPort;
  NSMutableArray *interarrivalTimesOfJitterMessages;
  NSDate *lastReceivedMessage;
  
  // The jitter seen by the server
  double serverJitter;
  NSString *serverhost;
  
  double clientLatency;
  double serverLatency;
}

@property (assign) IBOutlet NSWindow *window;
@property (assign) IBOutlet NSButton *connectButton;
@property (assign) IBOutlet NSTextField *hostField;
@property (assign) IBOutlet NSTextField *portField;
@property (assign) IBOutlet NSTextView *statusView;
@property (assign) IBOutlet NSTextField *jitterLabel;

- (IBAction)connectToHostButtonClicked:(id)sender;
- (IBAction)runPingPangPongData:(id)sender;

@end

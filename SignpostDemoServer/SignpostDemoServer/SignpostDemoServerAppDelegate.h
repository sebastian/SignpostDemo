//
//  AppDelegate.h
//  SignpostDemoServer
//
//  Created by Sebastian Eide on 07/05/2012.
//  Copyright (c) 2012 Kle.io. All rights reserved.
//

#import <Cocoa/Cocoa.h>

@class GCDAsyncSocket;
@class GCDAsyncUdpSocket;
@class SharedCode;

@interface SignpostDemoServerAppDelegate : NSObject <NSApplicationDelegate> {
  // TCP measurement server socket
  dispatch_queue_t socketQueue;
	GCDAsyncSocket *listenSocket;
	NSMutableArray *connectedSockets;

	// Jitter test socket
  dispatch_queue_t jitterSocketQueue;
	GCDAsyncUdpSocket *jitterSocket;
  
	BOOL isRunning;

  // Code for common functionality... nasty design.
  SharedCode *commFunc;
  
  double serverLatency;
  double clientLatency;
}

@property (assign) IBOutlet NSWindow *window;
@property (assign) IBOutlet NSTextField *port;
@property (assign) IBOutlet NSTextField *numBytes;
@property (assign) IBOutlet NSTextView *statusMessages;
@property (assign) IBOutlet NSButton *startStopButton;
@property (assign) IBOutlet NSTextField *jitterLabel;

- (IBAction) pushedStartStopButton:(id)sender;

@end

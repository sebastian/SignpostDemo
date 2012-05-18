//
//  AppDelegate.h
//  SignpostDemoServer
//
//  Created by Sebastian Eide on 07/05/2012.
//  Copyright (c) 2012 Kle.io. All rights reserved.
//

#import <Cocoa/Cocoa.h>
#import "MetricHTTPServer.h"

@class ServerSocketHandler;

@interface SignpostDemoServerAppDelegate : NSObject <NSApplicationDelegate, MetricHTTPServerDelegate> {
  ServerSocketHandler *socketHandler;
}

@property (assign) IBOutlet NSWindow *window;
@property (assign) IBOutlet NSTextField *port;
@property (assign) IBOutlet NSTextField *numBytes;
@property (assign) IBOutlet NSTextView *statusMessages;
@property (assign) IBOutlet NSButton *startStopButton;
@property (assign) IBOutlet NSTextField *jitterLabel;

- (IBAction) pushedStartStopButton:(id)sender;

// MetricHTTPServerDelegate
- (NSData *)dataResponseForHTTPServer:(id)sender;
@end

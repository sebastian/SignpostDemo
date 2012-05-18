//
//  AppDelegate.h
//  SignpostDemoClient
//
//  Created by Sebastian Eide on 07/05/2012.
//  Copyright (c) 2012 Kle.io. All rights reserved.
//

#import <Cocoa/Cocoa.h>

@class SocketHandler;

@interface AppDelegate : NSObject <NSApplicationDelegate> 
{
  SocketHandler *socketHandler;
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

//
//  ViewController.h
//  SignpostDemoiPhone
//
//  Created by Sebastian Eide on 09/05/2012.
//  Copyright (c) 2012 Kle.io. All rights reserved.
//

#import <UIKit/UIKit.h>

@class SharedCode;
@class Meter;
@class LatencyGoodputView;
@class SocketHandler;

@interface ViewController : UIViewController <UITextFieldDelegate>
{
  SocketHandler *socketHandler;
  Meter *meter;
}

@property (assign) IBOutlet UIButton *connectButton;
@property (assign) IBOutlet UITextField *hostField;
@property (assign) IBOutlet UITextField *portField;
@property (assign) IBOutlet UIView *connectView;
@property (assign) IBOutlet UIView *connectViewControls;
@property (assign) IBOutlet UIView *connectViewFadeout;
@property (assign) IBOutlet LatencyGoodputView *latencyGoodputView;

- (IBAction)connectToHostButtonClicked:(id)sender;
@end

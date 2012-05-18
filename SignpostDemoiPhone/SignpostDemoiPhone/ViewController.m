//
//  ViewController.m
//  SignpostDemoiPhone
//
//  Created by Sebastian Eide on 09/05/2012.
//  Copyright (c) 2012 Kle.io. All rights reserved.
//

#import "ViewController.h"
#import "LatencyGoodputView.h"
#import "SocketHandler.h"
#import "Meter.h"

@interface ViewController ()

@end

@implementation ViewController

@synthesize connectButton = _connectButton;
@synthesize hostField = _hostField;
@synthesize portField = _portField;
@synthesize connectView = _connectView;
@synthesize connectViewControls = _connectViewControls;
@synthesize connectViewFadeout = _connectViewFadeout;
@synthesize latencyGoodputView = _latencyGoodputView;


////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
#pragma mark - UIViewController
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

- (id) initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil
{
  self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
  if (self)
  {
    // ...
  }
  return self;
}

- (void)viewDidLoad
{
  [super viewDidLoad];
  [self addMeterAndConnectionView];
  if (socketHandler == nil) 
  {
    NSLog(@"Setting up socket handler");
    socketHandler = [[SocketHandler alloc] initWithLogHandlerMessage:^(NSString *msg) {
      dispatch_async(dispatch_get_main_queue(), ^{
        @autoreleasepool {
          NSLog(@"MESSAGE: %@", msg);
        }
      });
    } logHandlerError:^(NSString *errorStr) {
      dispatch_async(dispatch_get_main_queue(), ^{
        @autoreleasepool {
          NSLog(@"ERROR: %@", errorStr);
        }
      });
    } logHandlerInfo:^(NSString *infoStr) {
      dispatch_async(dispatch_get_main_queue(), ^{
        @autoreleasepool {
          NSLog(@"INFO: %@", infoStr);
        }
      });    
    }];
    __unsafe_unretained Meter *m = meter;
    [socketHandler setJitterCallbackUpdate:^(double localJitter, double serverJitter) {
      dispatch_async(dispatch_get_main_queue(), ^{
        @autoreleasepool {
          double averageJitter = (localJitter + serverJitter) / 2;
          [m setCurrentValue:averageJitter withCallback:^{}];
        }
      });
    }];
    __unsafe_unretained ViewController *vc = self;
    [socketHandler setControlsToggleCallback:^(BOOL enableControls) {
      dispatch_async(dispatch_get_main_queue(), ^{
        @autoreleasepool {
          if (enableControls)
            [vc hideConnectView];
          else 
            [vc showConnectView];
        }
      });    
    }];
    [socketHandler setGoodputLatencyCallback:^(double downstreamBandwidth, double clientLatency, double upstreamBandwidth, double serverLatency) {
      dispatch_async(dispatch_get_main_queue(), ^{
        [vc.latencyGoodputView showMeasuredGoodPut:downstreamBandwidth latency:clientLatency];
      });
    }];
  }
}

- (void)viewDidUnload
{
  [super viewDidUnload];
}

- (BOOL)shouldAutorotateToInterfaceOrientation:(UIInterfaceOrientation)interfaceOrientation
{
  switch (interfaceOrientation) {
    case UIInterfaceOrientationPortrait:
      return YES;
      break;
      
    default:
      return NO;
      break;
  }
}


////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
#pragma mark - IBActions
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

- (IBAction)connectToHostButtonClicked:(id)sender 
{
  NSUInteger port = [self.portField.text integerValue];  
  NSString *host = self.hostField.text;
  [socketHandler startStopSocketForHost:host port:port];
  [self.hostField resignFirstResponder];
  [self.portField resignFirstResponder];
}


////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
#pragma mark - Misc for logging and displaying info
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

- (void)addMeterAndConnectionView
{
  UIImage *needle = [UIImage imageNamed:@"needle.png"];
  meter = [[Meter alloc] initWithNeedleImage:needle];
  
  CGRect screenFrame = [UIScreen mainScreen].bounds;
  CGFloat screenHeight = screenFrame.size.height;
  
  CGFloat needleWidth = needle.size.width / 2.0;
  CGFloat needleHeight = needle.size.height / 2.0;
  
  CGFloat meterX = (screenFrame.size.width / 2.0) -  needleWidth - 1;
  CGFloat meterY = screenHeight - needleHeight - 125;
  
  CGRect meterFrame = CGRectMake(meterX, meterY, needleWidth, needleHeight);
  meter.view.frame = meterFrame;
  [self.view addSubview:meter.view];
  [meter setMaxValue:1000.0];
  
  [self.view addSubview:self.connectView];
  [self showConnectView];
}

- (void)showConnectView
{
  NSLog(@"Showing connection view");
  [UIView animateWithDuration:1 animations:^{
    CGRect newFrame = self.connectViewControls.frame;
    newFrame.origin.y = 0;
    self.connectViewControls.frame = newFrame;    
    
    self.connectViewFadeout.alpha = 0.5;
  }];  
}

- (void)hideConnectView
{
  NSLog(@"Hiding connection view");
  [UIView animateWithDuration:1 animations:^{
    CGRect newFrame = self.connectViewControls.frame;
    newFrame.origin.y = -300;
    self.connectViewControls.frame = newFrame;    
    
    self.connectViewFadeout.alpha = 0.0;  
  }];
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
//
//  Meter.m
//  SignpostDemoiPhone
//
//  Created by Sebastian Eide on 11/05/2012.
//  Copyright (c) 2012 Kle.io. All rights reserved.
//

#import "Meter.h"

@implementation Meter

////////////////////////////////////////////////////////////////////////////////////////////////////
#pragma mark - Setup
////////////////////////////////////////////////////////////////////////////////////////////////////

// If need init etc...


////////////////////////////////////////////////////////////////////////////////////////////////////
#pragma mark - Setup
////////////////////////////////////////////////////////////////////////////////////////////////////

- (void) viewDidLoad
{
  NSLog(@"Adding view");
  
  // We want to add the a layer for the needle  
  outerNeedleLayer = [[UIView alloc] init];
  outerNeedleLayer.frame = CGRectMake(100, 100, 100, 100);
  outerNeedleLayer.backgroundColor = [UIColor greenColor];

  innerNeedleLayer = [[UIView alloc] init];
  innerNeedleLayer.frame = CGRectMake(10, 10, 80, 80);
  innerNeedleLayer.backgroundColor = [UIColor redColor];

  [outerNeedleLayer addSubview:innerNeedleLayer];
  [self.view addSubview:outerNeedleLayer];
  
  CGRect frame = CGRectMake(0, 0, 200, 200);
  self.view.frame = frame;
}


////////////////////////////////////////////////////////////////////////////////////////////////////
#pragma mark - And ACTION...
////////////////////////////////////////////////////////////////////////////////////////////////////

- (void) setNeedleSpinning:(id)sender
{
  NSLog(@"In the set needle spinning action");
  [UIView animateWithDuration:10.0 animations: ^{
    outerNeedleLayer.transform = CGAffineTransformMakeRotation(90);
    innerNeedleLayer.transform = CGAffineTransformMakeRotation(-90);
  }];
}

@end

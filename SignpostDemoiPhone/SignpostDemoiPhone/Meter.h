//
//  Meter.h
//  SignpostDemoiPhone
//
//  Created by Sebastian Eide on 11/05/2012.
//  Copyright (c) 2012 Kle.io. All rights reserved.
//

#import <UIKit/UIKit.h>

@interface Meter : UIViewController
{
  UIView *outerNeedleLayer, *innerNeedleLayer;
}

- (void) setNeedleSpinning:(id)sender;

@end

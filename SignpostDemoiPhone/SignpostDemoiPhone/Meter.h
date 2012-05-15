//
//  Meter.h
//  SignpostDemoiPhone
//
//  Created by Sebastian Eide on 11/05/2012.
//  Copyright (c) 2012 Kle.io. All rights reserved.
//

#import <UIKit/UIKit.h>
#import <QuartzCore/QuartzCore.h>

@protocol MeterDelegate;


@interface Meter : UIViewController <UIAccelerometerDelegate>
{
  UIView *needleView;
  UIView *accelerationResultView;
  CGFloat maxValue;
  
  NSMutableArray *previousMeasurements;
  
  CGFloat currentEndDegreeValueOfNeedle;
  BOOL currentlyAnimatingAcceleration;
}

@property (nonatomic, assign) id<MeterDelegate> delegate;
@property (nonatomic, retain) UIAccelerometer *accelerometer;

- (void)setMaxValue:(CGFloat)maxValue;
- (void)setCurrentValue:(CGFloat)currentValue;
- (void)setCurrentValue:(CGFloat)currentValue withCallback:(void (^)())callback;

@end


@protocol MeterDelegate
- (void)meterFinishedTransition:(Meter *)meter;
@end

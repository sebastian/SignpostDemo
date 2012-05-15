//
//  Meter.m
//  SignpostDemoiPhone
//
//  Created by Sebastian Eide on 11/05/2012.
//  Copyright (c) 2012 Kle.io. All rights reserved.
//

#import "Meter.h"

#define DT 0.01
#define DERIVATIVE_GAIN 4.0
#define ACCELERATION_THRESHOLD 100
#define NORMALIZING_FACTOR 300
#define NORMALIZING_FACTOR_Y 160
#define ACCELERATION_HISTORY_SIZE 3


@implementation Meter

@synthesize delegate = _delegate;
@synthesize accelerometer = _accelerometer;

////////////////////////////////////////////////////////////////////////////////////////////////////
#pragma mark - Setup
////////////////////////////////////////////////////////////////////////////////////////////////////

- (id) init
{
  self = [super init];
  if (self)
  {
    maxValue = 100.0;
    previousMeasurements = [[NSMutableArray alloc] initWithCapacity:ACCELERATION_HISTORY_SIZE];
    currentlyAnimatingAcceleration = NO;
  }
  return self;
}

- (void) viewDidLoad
{
  NSLog(@"Adding view");
  
  self.accelerometer = [UIAccelerometer sharedAccelerometer];
  self.accelerometer.updateInterval = DT;
  self.accelerometer.delegate = self;
      
  // The view that is from the acceleration
  accelerationResultView = [[UIView alloc] initWithFrame:CGRectMake(100, 100, 100, 100)];
  accelerationResultView.backgroundColor = [UIColor greenColor];
  accelerationResultView.layer.anchorPoint = CGPointMake(0.5, 1.0);

  [self.view addSubview:accelerationResultView];
  
  CGRect frame = accelerationResultView.frame;
  CGFloat needleWidth = 10;
  CGFloat needleX = (frame.size.width / 2) - (needleWidth / 2);
  CGFloat needleHeight = frame.size.height;
  CGFloat needleY = frame.origin.y;

  // The needle itself
  CGRect needleFrame = CGRectMake(needleX, needleY, needleWidth, needleHeight);
  needleView = [[UIView alloc] initWithFrame:needleFrame];
  needleView.backgroundColor = [UIColor blackColor];
  needleView.layer.anchorPoint = CGPointMake(0.5, 1.0);

  
  [accelerationResultView addSubview:needleView];

  [self setCurrentValue:50 withCallback:^{}];
}


////////////////////////////////////////////////////////////////////////////////////////////////////
#pragma mark - And ACTION...
////////////////////////////////////////////////////////////////////////////////////////////////////

- (void)setMaxValue:(CGFloat)_maxValue
{
  maxValue = _maxValue;
}

- (void)setCurrentValue:(CGFloat)currentValue
{
  __unsafe_unretained Meter *mt = self;
  [self setCurrentValue:currentValue withCallback:^{
    [self.delegate meterFinishedTransition:mt];
  }];
}

- (void)setCurrentValue:(CGFloat)currentValue withCallback:(void (^)())callback
{  
  CGFloat fractionOfMax = currentValue / maxValue;
  if (fractionOfMax > 1.0)
    fractionOfMax = 1.0;
  if (fractionOfMax < 0.0)
    fractionOfMax = 0.0;
  
  CGFloat normalizedDegrees = (180.0 * fractionOfMax) - 90.0;
  CGFloat normalizedRadians = normalizedDegrees * M_PI / 180.0;
  
  [UIView animateWithDuration:0.3 animations: ^{
    needleView.transform = CGAffineTransformMakeRotation(normalizedRadians);
  } completion:^(BOOL completed) {
    NSLog(@"Animation completed: %i", completed);
    callback();
  }];
  
  currentEndDegreeValueOfNeedle = normalizedDegrees;
  NSLog(@"currentEndDegreeValueOfNeedle: %f", currentEndDegreeValueOfNeedle);
}

- (void)setAcceleration:(CGFloat)acceleration
{  
  if (currentlyAnimatingAcceleration)
    return;
  currentlyAnimatingAcceleration = YES;
  
  /***
   * A normalizedAcceleration of 1, should be enough to flip the
   * needle completely from centre to the right of left.
   **/
  CGFloat normalizedAcceleration;
  if (acceleration < 0.0)
    normalizedAcceleration = (acceleration + ACCELERATION_THRESHOLD) / NORMALIZING_FACTOR;    
  if (acceleration > 0.0)
    normalizedAcceleration = (acceleration - ACCELERATION_THRESHOLD) / NORMALIZING_FACTOR;

  if (normalizedAcceleration > 1.0)
    normalizedAcceleration = 1.0;
  if (normalizedAcceleration < -1.0)
    normalizedAcceleration = -1.0;
  
  CGFloat normalizedDegrees = 90.0 * normalizedAcceleration;
  CGFloat withCurrentValue = normalizedDegrees + currentEndDegreeValueOfNeedle;
  
  if (withCurrentValue > 90.0)
  {
    normalizedDegrees = 90 - currentEndDegreeValueOfNeedle;
  }
  if (withCurrentValue < -90.0)
  {
    normalizedDegrees = 90 + currentEndDegreeValueOfNeedle;
  }
  NSLog(@"Normalized degrees: %f", normalizedDegrees);
  
  CGFloat normalizedRadians = normalizedDegrees * M_PI / 180.0;
  
  [UIView animateWithDuration:0.1 animations: ^{
    accelerationResultView.transform = CGAffineTransformMakeRotation(normalizedRadians);
  } completion:^(BOOL completed) {
    [UIView animateWithDuration:0.5 animations: ^{
      accelerationResultView.transform = CGAffineTransformMakeRotation(0);
    } completion:^(BOOL completed) {
      currentlyAnimatingAcceleration = NO;
    }];
  }];
}


////////////////////////////////////////////////////////////////////////////////////////////////////
#pragma mark - And ACTION...
////////////////////////////////////////////////////////////////////////////////////////////////////

- (void)accelerometer:(UIAccelerometer *)accelerometer didAccelerate:(UIAcceleration *)acceleration {
  double change = 0.0;

  double lastValueX = [self latestForCallback:^(UIAcceleration *acc) { return acc.x; }];
  double lastValueY = [self latestForCallback:^(UIAcceleration *acc) { return acc.y; }];

  UIAccelerationValue currentValueX = acceleration.x;
  UIAccelerationValue currentValueY = acceleration.y;
    
  double differenceX = lastValueX - currentValueX;
    
  /***
   * We want the Y component to reinforce whatever
   * the X component is doing. The downward movement
   * should only make the needle pop further in a given
   * direction.
   *
   * This means that if the phone is being moved downwards,
   * we multiply the value by a positive factor + 1,
   * and if it is moved up, we multiply by 1 - positive factor
   **/
  double differenceY = currentValueY - lastValueY;
  double normalizedY = differenceY / NORMALIZING_FACTOR_Y;
  double factorY = 1 + normalizedY;

  double difference = differenceX * factorY;
  
  change = (difference / DT) * DERIVATIVE_GAIN;
  
  if ((change > ACCELERATION_THRESHOLD) || (change < ((-1)*ACCELERATION_THRESHOLD)))
  {
    NSLog(@"Change: %f", change);
    [self setAcceleration:change];
  }
    
  [self addMeasurement:acceleration];
}

- (double)latestForCallback:(double (^)(UIAcceleration *))selector
{
  double acc = 0.0;
  NSInteger numElements = [previousMeasurements count];
  for (UIAcceleration *acceleration in previousMeasurements)
  {
    acc += selector(acceleration) / (double) numElements;
  }
  return acc;
}

- (void)addMeasurement:(UIAcceleration *)acceleration
{
  [previousMeasurements addObject:acceleration];
  if ([previousMeasurements count] > ACCELERATION_HISTORY_SIZE)
  {
    [previousMeasurements removeObjectAtIndex:0];
  }
}
@end

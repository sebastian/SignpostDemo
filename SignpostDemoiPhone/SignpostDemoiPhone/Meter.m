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
#define ACCELERATION_THRESHOLD 500
#define NORMALIZING_FACTOR 700
#define NORMALIZING_FACTOR_Y 350
#define ACCELERATION_HISTORY_SIZE 3


@implementation Meter

@synthesize delegate = _delegate;
@synthesize accelerometer = _accelerometer;

////////////////////////////////////////////////////////////////////////////////////////////////////
#pragma mark - Setup
////////////////////////////////////////////////////////////////////////////////////////////////////

- (id) initWithNeedleImage:(UIImage *)needle
{
  self = [super init];
  if (self)
  {
    maxValue = 100.0;
    previousMeasurements = [[NSMutableArray alloc] initWithCapacity:ACCELERATION_HISTORY_SIZE];
    currentlyAnimatingAcceleration = NO;
    needleImage = needle;
  }
  return self;
}

- (void) viewDidLoad
{
  self.accelerometer = [UIAccelerometer sharedAccelerometer];
  self.accelerometer.updateInterval = DT;
  self.accelerometer.delegate = self;
        
  // The view that is from the acceleration
  accelerationResultView = [[UIView alloc] initWithFrame:CGRectMake(10, 10, needleImage.size.width / 2, needleImage.size.height / 2)];
  accelerationResultView.backgroundColor = [UIColor clearColor];
  accelerationResultView.layer.anchorPoint = CGPointMake(0.5, 0.0);

  [self.view addSubview:accelerationResultView];
  
  CGRect frame = accelerationResultView.frame;
  CGFloat needleWidth = needleImage.size.width / 2;
  CGFloat needleHeight = needleImage.size.height / 2;
  CGFloat needleX = (frame.size.width / 2) - (needleWidth / 2);
  CGFloat needleY = frame.origin.y - needleHeight - 2;

  // The needle itself
//  CGRect needleFrame = CGRectMake(needleX, needleY, needleWidth, needleHeight);
  CGRect needleFrame = CGRectMake(needleX, needleY, needleWidth, needleHeight);
  needleView = [[UIImageView alloc] initWithFrame:needleFrame];
  needleView.layer.anchorPoint = CGPointMake(0.5, 0.05);
  needleView.backgroundColor = [UIColor clearColor];
  needleView.image = [UIImage imageNamed:@"needle.png"];


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
  
  CGFloat normalizedDegrees = 180 - (180.0 * fractionOfMax) - 90.0;
  CGFloat normalizedRadians = normalizedDegrees * M_PI / 180.0;
  
  [UIView animateWithDuration:0.3 animations: ^{
    needleView.transform = CGAffineTransformMakeRotation(normalizedRadians);
  } completion:^(BOOL completed) {
    callback();
  }];
  
  currentEndDegreeValueOfNeedle = normalizedDegrees;
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
  
  CGFloat normalizedRadians = normalizedDegrees * M_PI / 180.0;
  
  [UIView animateWithDuration:0.1 animations: ^{
    accelerationResultView.transform = CGAffineTransformMakeRotation(normalizedRadians);
  } completion:^(BOOL completed) {
    [UIView animateWithDuration:0.3 animations: ^{
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

  [self addMeasurement:acceleration];
  
  double lastValueX = [self diffsFor:^(UIAcceleration *acc) { return acc.x; }];
  double lastValueY = [self diffsFor:^(UIAcceleration *acc) { return acc.y; }];

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
    [self setAcceleration:(-1.0)*change];
  }
}

- (double)diffsFor:(double (^)(UIAcceleration *))selector
{
  double acc = 0.0;
  UIAcceleration *previous = nil;
  for (UIAcceleration *acceleration in previousMeasurements)
  {
    if (previous == nil)
    {
      previous = acceleration;
    }
    else 
    {
      double prev = selector(previous);
      double current = selector(acceleration);
      acc += prev - current;
    }
//    acc += selector(acceleration) / (double) numElements;
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

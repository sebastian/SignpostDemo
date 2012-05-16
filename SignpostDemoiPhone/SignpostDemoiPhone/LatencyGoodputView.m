//
//  LatencyGoodput.m
//  SignpostDemoiPhone
//
//  Created by Sebastian Eide on 16/05/2012.
//  Copyright (c) 2012 Kle.io. All rights reserved.
//

#import "LatencyGoodputView.h"
#import "CircleView.h"

@implementation LatencyGoodputView

- (void)showMeasuredGoodPut:(CGFloat)goodput latency:(CGFloat)latency
{
  CGFloat viewWidth = self.frame.size.width;
  CGFloat viewHeight = self.frame.size.height;
  
  CGFloat normalizedGoodput = goodput / 22.0;
  if (normalizedGoodput > 1.0)
  {
    // That is over 20 is infinity, and 22 is infinity :D
    normalizedGoodput = 1.0;
  }
  
  CGFloat normalizedLatency = latency / 80.0;
  if (normalizedLatency > 1.0)
  {
    // We can only show up to 70, over is infinity, or 80 if you wish
    normalizedLatency = 1.0;
  }
  
  double goodputPosition = normalizedGoodput * viewWidth;
  double latencyPosition = normalizedLatency * viewHeight;
  
  CircleView *circle = [[CircleView alloc] init];
  CGRect circleFrame = circle.frame;
  circleFrame.origin.y = viewHeight - latencyPosition + circleFrame.size.height / 2;
  circleFrame.origin.x = goodputPosition - circleFrame.size.width / 2;
  circle.frame = circleFrame;
  [self addSubview:circle];
  [UIView animateWithDuration:5.0 animations:^{
    circle.alpha = 0.0;    
  } completion:^(BOOL complete) {
    [circle removeFromSuperview];
  }];
}

@end

//
//  CircleView.m
//  SignpostDemoiPhone
//
//  Created by Sebastian Eide on 16/05/2012.
//  Copyright (c) 2012 Kle.io. All rights reserved.
//

#import "CircleView.h"

#define CIRCLESIZE 10

@implementation CircleView

- (id) init
{
  CGRect frame = CGRectMake(0, 0, CIRCLESIZE, CIRCLESIZE);
  return [self initWithFrame:frame];
}

- (id)initWithFrame:(CGRect)frame
{
  self = [super initWithFrame:frame];
  if (self)
  {
    self.backgroundColor = [UIColor clearColor];
  }
  return self;
}

- (void)drawRect:(CGRect)rect
{
  CGContextRef graphicsContext = UIGraphicsGetCurrentContext();
  CGContextSetFillColorWithColor(graphicsContext, [UIColor greenColor].CGColor);
  CGRect circleRect = CGRectMake(0, 0, CIRCLESIZE, CIRCLESIZE);
  CGContextFillEllipseInRect(graphicsContext, circleRect);
}

@end

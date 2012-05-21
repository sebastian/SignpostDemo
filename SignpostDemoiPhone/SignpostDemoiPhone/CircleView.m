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

- (id) initForDownstream:(BOOL)ds;
{
  CGRect frame = CGRectMake(0, 0, CIRCLESIZE, CIRCLESIZE);
  downstream = ds;
  if (downstream)
  {
    colour = [UIColor greenColor];
  }
  else
  {
    colour = [UIColor redColor];
  }
    

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
    
  CGContextSetFillColorWithColor(graphicsContext, colour.CGColor);
  CGRect circleRect;

  CGColorRef black = [UIColor blackColor].CGColor;
  CGContextSetStrokeColorWithColor(graphicsContext, black);
  CGContextSetLineWidth(graphicsContext, 1.0);
  
  if (downstream) 
  {
    circleRect = CGRectMake(0, 0, CIRCLESIZE, CIRCLESIZE);
    CGContextFillEllipseInRect(graphicsContext, circleRect);
    
    // Horizontal line
    CGContextMoveToPoint(graphicsContext, 0, CIRCLESIZE/2);
    CGContextAddLineToPoint(graphicsContext, CIRCLESIZE, CIRCLESIZE/2);

    // Vertical line
    CGContextMoveToPoint(graphicsContext, CIRCLESIZE/2, 0);
    CGContextAddLineToPoint(graphicsContext, CIRCLESIZE/2, CIRCLESIZE);
    
    CGContextStrokePath(graphicsContext);
  }
  else
  {
    circleRect = CGRectMake(1, 1, CIRCLESIZE-2, CIRCLESIZE-2);
    CGContextStrokePath(graphicsContext);
    CGContextStrokeEllipseInRect(graphicsContext, circleRect);
    CGContextFillEllipseInRect(graphicsContext, circleRect);
  }

}

@end

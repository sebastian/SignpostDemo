//
//  CircleView.h
//  SignpostDemoiPhone
//
//  Created by Sebastian Eide on 16/05/2012.
//  Copyright (c) 2012 Kle.io. All rights reserved.
//

#import <UIKit/UIKit.h>

@interface CircleView : UIView
{
  BOOL downstream;
  UIColor *colour;
  UIImage *cross;
}

- (id) initForDownstream:(BOOL)downstream;

@end

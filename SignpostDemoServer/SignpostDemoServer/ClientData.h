//
//  Latency.h
//  SignpostDemoServer
//
//  Created by Sebastian Eide on 17/05/2012.
//  Copyright (c) 2012 Kle.io. All rights reserved.
//

#import <Foundation/Foundation.h>

@interface ClientData : NSObject

@property (nonatomic, assign) double clientLatency;
@property (nonatomic, assign) double serverLatency;

@property (nonatomic, assign) double downstreamBandwidth;
@property (nonatomic, assign) double upstreamBandwidth;

@property (nonatomic, assign) double clientPerceivedJitter;

// For latency timings. Need to retain the start time of
// the measurement
@property (nonatomic, retain) NSDate *startTimeLatency;
@property (nonatomic, retain) NSDate *startTimeBandwidth;

@end

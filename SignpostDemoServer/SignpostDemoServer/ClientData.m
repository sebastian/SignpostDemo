//
//  Latency.m
//  SignpostDemoServer
//
//  Created by Sebastian Eide on 17/05/2012.
//  Copyright (c) 2012 Kle.io. All rights reserved.
//

#import "ClientData.h"

@implementation ClientData

// Latency
@synthesize  clientLatency = _clientLatency;
@synthesize serverLatency = _serverLatency;

// Goodput
@synthesize downstreamBandwidth = _downstreamBandwidth;
@synthesize upstreamBandwidth = _upstreamBandwidth;

// Jitter
@synthesize clientPerceivedJitter = _clientPerceivedJitter;

// Timers...
@synthesize startTimeBandwidth = _startTimeBandwidth;
@synthesize startTimeLatency = _startTimeLatency;

@end

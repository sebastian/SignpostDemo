//
//  SocketHandler.h
//  SignpostDemoClient
//
//  Created by Sebastian Eide on 18/05/2012.
//  Copyright (c) 2012 Kle.io. All rights reserved.
//

#import <Foundation/Foundation.h>

typedef void(^ControlsToggle)(BOOL);
typedef void(^LogHandler)(NSString *);
// The block is called with jitterSeenLocally, jitterSeenByServer
typedef void(^JitterUpdatesCallback)(double, double);
// Called with downstream bandwidth, client latency, upstream bandwidth, server latency
typedef void(^GoodputLatency)(double, double, double, double);

@class GCDAsyncSocket;
@class GCDAsyncUdpSocket;
@class SharedCode;

@interface SocketHandler : NSObject
{
  // Latency and goodput
  dispatch_queue_t socketQueue;
	GCDAsyncSocket *socket;
  
  // Jitter
  dispatch_queue_t jitterSocketQueue;
	GCDAsyncUdpSocket *jitterSocket;
  
  // General bookkeeping
	BOOL isConnected;
  
  SharedCode *commonFunc;
  
  // For calculating jitter
  NSUInteger serverJitterPort;
  NSMutableArray *interarrivalTimesOfJitterMessages;
  NSDate *lastReceivedMessage;
  
  // The jitter seen by the server
  double serverJitter;
  NSString *serverhost;
  
  // Goodput
  double downstreamBandwidth;
  double upstreamBandwidth;
  
  // Latency
  double clientLatency;
  double serverLatency;
  
  // Timers for latency and bandwidth measurements
  NSDate *startTimerLatency, *startTimerBandwidth;
  
  LogHandler callbackLogMessage;
  LogHandler callbackLogError;
  LogHandler callbackLogInfo;
  JitterUpdatesCallback jitterUpdatesCallback;
  ControlsToggle controlsToggleCallback;
  GoodputLatency goodputLatencyCallback;
}

- (id)initWithLogHandlerMessage:(LogHandler)message logHandlerError:(LogHandler)error logHandlerInfo:(LogHandler)info;
- (void)setJitterCallbackUpdate:(JitterUpdatesCallback)callback;
- (void)setControlsToggleCallback:(ControlsToggle)callback;
- (void)setGoodputLatencyCallback:(GoodputLatency)callback;

- (void)startStopSocketForHost:(NSString*)host port:(NSUInteger)port;
- (void) startPingPangPongData;

@end

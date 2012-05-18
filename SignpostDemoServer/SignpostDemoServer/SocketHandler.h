//
//  SocketHandler.h
//  SignpostDemoServer
//
//  Created by Sebastian Eide on 18/05/2012.
//  Copyright (c) 2012 Kle.io. All rights reserved.
//

#import <Foundation/Foundation.h>

@class SharedCode;
@class GCDAsyncSocket;
@class GCDAsyncUdpSocket;
@class ClientData;

typedef void(^LogHandler)(NSString *);
// The block is called with jitterSeenLocally, jitterSeenByClient
typedef void(^JitterUpdatesCallback)(double, double);

@interface SocketHandler : NSObject {
  // TCP measurement server socket
  dispatch_queue_t socketQueue;
	GCDAsyncSocket *listenSocket;
	NSMutableArray *connectedSockets;
  
	// Jitter test socket
  dispatch_queue_t jitterSocketQueue;
	GCDAsyncUdpSocket *jitterSocket;
  
	BOOL isRunning;
  
  // Code for common functionality... nasty design.
  SharedCode *commFunc;
  
  NSMutableDictionary *userData;
  dispatch_queue_t latencyAccessQueue;
  
  LogHandler callbackLogMessage;
  LogHandler callbackLogError;
  LogHandler callbackLogInfo;
  JitterUpdatesCallback jitterUpdatesCallback;
}

- (id)initWithLogHandlerMessage:(LogHandler)message logHandlerError:(LogHandler)error logHandlerInfo:(LogHandler)info;

- (void)setJitterCallbackUpdate:(JitterUpdatesCallback)callback;

// Returns YES if is running as a result, and NO if it isn't.
- (BOOL)startStopSocketForPort:(NSUInteger)port andNumKBytes:(NSUInteger)numKBytesToUse;

- (void)executeBlockWithSynchronizedUserData:(void (^)(NSMutableDictionary *, SharedCode *sc))block;

@end

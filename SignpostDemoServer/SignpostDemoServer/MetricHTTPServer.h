//
//  MetricHTTPServer.h
//  SignpostDemoServer
//
//  Created by Sebastian Eide on 17/05/2012.
//  Copyright (c) 2012 Kle.io. All rights reserved.
//

#import "HTTPConnection.h"

@protocol MetricHTTPServerDelegate
- (NSData *)dataResponseForHTTPServer:(id)self;
@end

@interface MetricHTTPServer : HTTPConnection
+ (void)startHTTPServerForDelegate:(id<MetricHTTPServerDelegate>)delegate;
@end

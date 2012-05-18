//
//  MetricHTTPServer.m
//  SignpostDemoServer
//
//  Created by Sebastian Eide on 17/05/2012.
//  Copyright (c) 2012 Kle.io. All rights reserved.
//

#import "MetricHTTPServer.h"
#import "HTTPServer.h"
#import "HTTPDataResponse.h"
#import "SignpostDemoServerAppDelegate.h"

@implementation MetricHTTPServer

static id<MetricHTTPServerDelegate> instance;
static HTTPServer *httpServer;

- (NSObject<HTTPResponse> *)httpResponseForMethod:(NSString *)method URI:(NSString *)path
{
  NSData *jsonData = [instance dataResponseForHTTPServer:(id)self];
  return [[HTTPDataResponse alloc] initWithData:jsonData];
}

+ (void)startHTTPServerForDelegate:(id<MetricHTTPServerDelegate>)delegate
{
  instance = delegate;
  
  httpServer = [[HTTPServer alloc] init];
  [httpServer setType:@"_http._tcp."];
  [httpServer setName:@"SignpostDemoServer"];
  [httpServer setPort:8899];
  [httpServer setConnectionClass:[MetricHTTPServer class]];
  
	NSError *error;
	BOOL success = [httpServer start:&error];
	if(!success)
	{
		NSLog(@"Error starting HTTP Server: %@", [error description]);
	}
}

@end

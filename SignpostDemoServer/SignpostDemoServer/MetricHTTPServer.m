//
//  MetricHTTPServer.m
//  SignpostDemoServer
//
//  Created by Sebastian Eide on 17/05/2012.
//  Copyright (c) 2012 Kle.io. All rights reserved.
//

#import "MetricHTTPServer.h"
#import "HTTPDataResponse.h"
#import "SignpostDemoServerAppDelegate.h"

@implementation MetricHTTPServer

- (NSObject<HTTPResponse> *)httpResponseForMethod:(NSString *)method URI:(NSString *)path
{
  SignpostDemoServerAppDelegate *dg = (SignpostDemoServerAppDelegate *) [SignpostDemoServerAppDelegate getMe];
  NSData *jsonData = [dg dataResponseForHTTPServer:(id)self];
  return [[HTTPDataResponse alloc] initWithData:jsonData];
}

@end

//
//  PacketHelper.m
//  SignpostDemoServer
//
//  Created by Sebastian Eide on 08/05/2012.
//  Copyright (c) 2012 Kle.io. All rights reserved.
//

#import "PacketHelper.h"
#import "GCDAsyncSocket.h"

@implementation PacketHelper

+ (NSData *)dictToDataSendableData:(NSDictionary *)dict error:(NSError **)error
{
  NSMutableData *json = [NSMutableData dataWithData:[NSJSONSerialization dataWithJSONObject:dict options:NSJSONWritingPrettyPrinted error:error]];
  [json appendData:[GCDAsyncSocket CRLFData]];
  if (error != nil)
    return nil;
  else 
    return json;
}

+ (NSDictionary *)dataToDict:(NSData *)data error:(NSError **)error
{
  NSDictionary *dict = [NSJSONSerialization JSONObjectWithData:data options:NSJSONReadingAllowFragments error:error];
  if (error != nil)
    return nil;
  else
    return dict;  
}

@end

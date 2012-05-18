//
//  DataBuilder.m
//  SignpostDemoServer
//
//  Created by Sebastian Eide on 08/05/2012.
//  Copyright (c) 2012 Kle.io. All rights reserved.
//

#import "NetworkData.h"
#import "PacketHelper.h"

@implementation NetworkData

- (id) init 
{
  self = [super init];
  if (self) 
  {
    dataDict = [[NSMutableDictionary alloc] init];
  }
  return self;
}

- (id) initWithData:(NSData *)data 
{
  self = [self init];
  if (self) 
  {
    NSError *error = nil;
    [dataDict addEntriesFromDictionary:[PacketHelper dataToDict:data error:&error]];
    if (error)
    {
      NSLog(@"ERROR initialing network data: %@", error);
      return nil;
    }
  }
  return self;
}

- (void) setValue:(id)value forKey:(NSString *)key 
{
  [dataDict setValue:value forKey:key];
}

- (id) getValueForKey:(NSString *)key
{
  return [dataDict objectForKey:key];
}

- (NSData *) getData 
{
  NSError *error = nil;
  NSData *data = [PacketHelper dictToDataSendableData:dataDict error:&error];
  if (error)
  {
    NSLog(@"ERROR converting to sendable data: %@", error);
    return nil;
  }
  return data;
}


@end

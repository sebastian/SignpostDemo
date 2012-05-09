//
//  PacketHelper.h
//  SignpostDemoServer
//
//  Created by Sebastian Eide on 08/05/2012.
//  Copyright (c) 2012 Kle.io. All rights reserved.
//

#import <Foundation/Foundation.h>

@interface PacketHelper : NSObject

+ (NSData *)dictToDataSendableData:(NSDictionary *)dict error:(NSError **)error;
+ (NSDictionary *)dataToDict:(NSData *)data error:(NSError **)error;

@end

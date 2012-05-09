//
//  DataBuilder.h
//  SignpostDemoServer
//
//  Created by Sebastian Eide on 08/05/2012.
//  Copyright (c) 2012 Kle.io. All rights reserved.
//

#import <Foundation/Foundation.h>

@interface NetworkData : NSObject {
  NSMutableDictionary *dataDict;
}

- (id) init;
- (id) initWithData:(NSData *)data;
- (void) setValue:(id)value forKey:(NSString *)key;
- (id) getValueForKey:(NSString *)key;
- (NSData *) getData;

@end

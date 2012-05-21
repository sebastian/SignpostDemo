//
//  SharedCode.m
//  SignpostDemoServer
//
//  Created by Sebastian Eide on 08/05/2012.
//  Copyright (c) 2012 Kle.io. All rights reserved.
//

#import "SharedCode.h"
#import "GCDAsyncSocket.h"
#import "GCDAsyncUdpSocket.h"

@implementation SharedCode

@synthesize hostname = _hostname;

////////////////////////////////////////////////////////////////////////////////////////////////////
#pragma mark - Setup
////////////////////////////////////////////////////////////////////////////////////////////////////

- (id)init 
{
  self = [super init];
  if (self)
  {
    dataPayload = nil;
    latency = 0.0;
    jitterMeasurements = [[NSMutableDictionary alloc] init];
    jitterCache = [[NSMutableDictionary alloc] init];
    jitterCalcQueue = dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_LOW, 0);
    numBytesForData = 1000000;
  }
  return self;
}


////////////////////////////////////////////////////////////////////////////////////////////////////
#pragma mark - Latency related functionality
////////////////////////////////////////////////////////////////////////////////////////////////////

- (void)setNumberOfBytesForDataMeasurements:(NSInteger)numBytes
{
  if (numBytesForData != numBytes)
  {
    // The amount of data to send/expect has changed.
    numBytesForData = numBytes;
    // Reset the data payload so the right one is used the next time.
    dataPayload = nil;
  }
}

- (NSInteger)numBytesToReadForData
{
  return numBytesForData;
}


////////////////////////////////////////////////////////////////////////////////////////////////////
#pragma mark - Latency related functionality
////////////////////////////////////////////////////////////////////////////////////////////////////

- (NSDate *)startLatencyMeasurement {
  return [NSDate date];
}

- (double)concludeLatencyMeasurementForDate:(NSDate *)startTimeLatency {
  return ([startTimeLatency timeIntervalSinceNow] * -1000.0)/2.0; // We divide by two to get latency, rather than RTT.
}


////////////////////////////////////////////////////////////////////////////////////////////////////
#pragma mark - Bandwidth related functionality
////////////////////////////////////////////////////////////////////////////////////////////////////

- (NSDate *)startBandwidthMeasurement
{
  return [NSDate date];
}

- (double) getBandwidthInMegabitsPerSecondForStartTime:(NSDate *)startTimeBandwidth
{
  double transmissionTime = [startTimeBandwidth timeIntervalSinceNow] * (-1000.0) - (2 * latency); // in ms
  double numPerSecond = (1 / transmissionTime) * 1000;
  double bytesPerSecond = numPerSecond * numBytesForData;
  NSUInteger bytesPerMegabit = 131072.0;
  double mbitPerSecond = bytesPerSecond / bytesPerMegabit;        
  return mbitPerSecond;
}


////////////////////////////////////////////////////////////////////////////////////////////////////
#pragma mark - Encoding and decoding data for the wire
////////////////////////////////////////////////////////////////////////////////////////////////////

- (NSData *) dataPayload 
{
  if (dataPayload == nil)
  {
    // Set the data values
    NSMutableString * tempString = [NSMutableString stringWithCapacity:numBytesForData];
    for (int i = 0; i < (numBytesForData / 10); i++) {
      [tempString appendString:@"abcdefghij"];
    }
    dataPayload = [tempString dataUsingEncoding:NSUTF8StringEncoding];  
  }
  return dataPayload;
}

+ (NSData *) intToData:(NSInteger)integerValue 
{
  NSString *stringValue = FORMAT(@"%i\r\n", integerValue);
  return [stringValue dataUsingEncoding:NSUTF8StringEncoding];
}

+ (NSInteger) dataToInt:(NSData *)data 
{
  NSString *stringData = [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
  NSInteger integerValue = [stringData integerValue];
  return integerValue;
}

+ (NSData *) payloadForString:(NSString *)stringVal
{
  return [[stringVal stringByAppendingFormat:@"\r\n"] dataUsingEncoding:NSUTF8StringEncoding];  
}

+ (NSString *) stringFromData:(NSData *)data
{
  NSString *string = [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
  return [string stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceAndNewlineCharacterSet]];
}

// This method takes a host and a port. The host and port serve to provide 
// a unique name for the other party to identity packages coming from this host,
// so that multiple jitter measurements can be done simultaneously.
- (NSData *) jitterPayloadContainingJitter:(NSNumber *)jitter
{
  NSDate * date = [NSDate date];
  NSTimeInterval time = [date timeIntervalSince1970];
  NSString *hostname = self.hostname;
  double jitterFloat = [jitter floatValue];
  
  /***
   * The format of the message sent:
   * "hostname\r\ntimeSince1970\r\njitterFloat"
   **/
  NSString *jitterPayload = [NSString stringWithFormat:@"%@\r\n%f\r\n%f\r\n", hostname, time, jitterFloat];
  return [jitterPayload dataUsingEncoding:NSUTF8StringEncoding];
}

+ (NSArray *)partsFromData:(NSData *)data
{
  NSString *stringData = [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
  return [stringData componentsSeparatedByString:@"\r\n"];
}

+ (double) msFromTimestampData:(NSData *)data 
{
  NSArray *parts = [self partsFromData:data];
  NSTimeInterval timeInterval = [(NSString *)[parts objectAtIndex:1] doubleValue];
  NSDate *date = [NSDate dateWithTimeIntervalSince1970:timeInterval];
  return [date timeIntervalSinceNow] * -1000.0;
}

+ (NSString *) hostFromData:(NSData *)data 
{
  NSArray *parts = [self partsFromData:data];
  return [parts objectAtIndex:0];
}

+ (NSNumber *) hostJitterFromData:(NSData *)data 
{
  NSArray *parts = [self partsFromData:data];
  double jitter = [(NSString *)[parts objectAtIndex:2] doubleValue];
  return [NSNumber numberWithFloat:jitter];
}


////////////////////////////////////////////////////////////////////////////////////////////////////
#pragma mark - Jitter measurements
////////////////////////////////////////////////////////////////////////////////////////////////////

- (NSNumber *)meanOf:(NSArray *)array
{
  double runningTotal = 0.0;
  for(NSNumber *number in array)
  {
    runningTotal += [number doubleValue];
  }
  return [NSNumber numberWithDouble:(runningTotal / [array count])];
}

- (void)addJitterMeasurement:(double)measurement forHost:(NSString*)host
{
  @synchronized(jitterMeasurements)
  {
    NSMutableArray *measurements = [jitterMeasurements objectForKey:host];
    if (measurements == nil) 
    {
      measurements = [[NSMutableArray alloc] init];
    }
    [measurements addObject:[NSNumber numberWithFloat:measurement]];
    if ([measurements count] > JITTERMESSAGECOUNT)
    {
      [measurements removeObjectAtIndex:0];
    }
    [jitterMeasurements setObject:measurements forKey:host];
  }
  
  // Calculate the jitter
  dispatch_async(jitterCalcQueue, ^{
    @synchronized(jitterMeasurements)
    {
      NSMutableArray *measurements = [jitterMeasurements objectForKey:host];
      double mean = [[self meanOf:measurements] doubleValue];
      double sumOfSquaredDifferences = 0.0;
      for(NSNumber *number in measurements)
      {
        double valueOfNumber = [number doubleValue];
        double difference = valueOfNumber - mean;
        sumOfSquaredDifferences += difference * difference;
      }
      NSNumber *currentJitter = [NSNumber numberWithDouble:(sumOfSquaredDifferences / [measurements count])];
      [jitterCache setValue:currentJitter forKey:host];
    }
  });
}

- (NSNumber *)currentJitterForHost:(NSString *)host
{
  @synchronized(jitterMeasurements) {
    return (NSNumber *) [jitterCache valueForKey:host];
  }
}

- (void)performJitterMeasurements:(NSDictionary*)infoDict {
  @autoreleasepool 
  {
    NSString *host = (NSString *) [infoDict valueForKey:@"host"];
    NSInteger port = [(NSNumber *) [infoDict valueForKey:@"port"] integerValue];
    NSString *hostname = [infoDict valueForKey:@"hostname"];
    GCDAsyncSocket *socket = (GCDAsyncSocket *) [infoDict valueForKey:@"receiveSocket"];
    GCDAsyncUdpSocket *jitterSocket = (GCDAsyncUdpSocket *) [infoDict valueForKey:@"sendSocket"];
    
    struct timespec a;
    a.tv_nsec = INTERVAL_BETWEEN_JITTER_MESSAGES;
    a.tv_sec = 0;
    
    NSNumber *currentJitter;
    NSData *payload;
    
    while ([socket isConnected])
    {
      currentJitter = [self currentJitterForHost:hostname];
      if (currentJitter == nil)
        currentJitter = [NSNumber numberWithInt:0];
      payload = [self jitterPayloadContainingJitter:currentJitter];
      nanosleep(&a, NULL);
      @synchronized(jitterSocket)
      {
        [jitterSocket sendData:payload toHost:host port:port withTimeout:-1 tag:JITTERMESSAGE];
      }
    }
  }
}

@end

//
//  main.m
//  SignpostDemo
//
//  Created by Sebastian Eide on 27/04/2012.
//  Copyright (c) 2012 Kle.io. All rights reserved.
//

#import <Cocoa/Cocoa.h>

#import <MacRuby/MacRuby.h>

int main(int argc, char *argv[])
{
  return macruby_main("rb_main.rb", argc, argv);
}

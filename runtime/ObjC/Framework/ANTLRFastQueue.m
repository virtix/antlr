//
//  ANTLRFastQueue.m
//  ANTLR
//
//  Created by Ian Michell on 26/04/2010.
// [The "BSD licence"]
// Copyright (c) 2010 Ian Michell 2010 Alan Condit
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
// 1. Redistributions of source code must retain the above copyright
//    notice, this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright
//    notice, this list of conditions and the following disclaimer in the
//    documentation and/or other materials provided with the distribution.
// 3. The name of the author may not be used to endorse or promote products
//    derived from this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
// IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
// OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
// IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
// INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
// NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
// DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
// THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
// THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

#import "ANTLRFastQueue.h"
#import "ANTLRError.h"
#import "ANTLRRuntimeException.h"

@implementation ANTLRFastQueue

//@synthesize pool;
@synthesize data;
@synthesize p;
@synthesize range;

+ (id) newANTLRFastQueue
{
    return [[ANTLRFastQueue alloc] init];
}

- (id) init
{
	if ((self = [super init]) != nil ) {
//		pool = [NSAutoreleasePool new];
		data = [[NSMutableArray arrayWithCapacity:100] autorelease];
		p = 0;
		range = -1;
	}
	return self;
}

-(void) dealloc
{
//	[pool drain];
	[super dealloc];
}

- (id) copyWithZone:(NSZone *)aZone
{
    ANTLRFastQueue *copy;
    
    copy = [[[self class] allocWithZone:aZone] init];
//    copy.pool = pool;
    copy.data = [data copyWithZone:nil];
    copy.p = p;
    return copy;
}

// FIXME: Java code has this, it doesn't seem like it needs to be there... Then again a lot of the code in the java runtime is not great...
-(void) reset
{
	[self clear];
}

-(void) clear
{
	p = 0;
	[data removeAllObjects];
}

-(id) remove
{
	id o = [self objectAtIndex:0];
	p++;
	// check to see if we have hit the end of the buffer
	if (p == [data count])
	{
		// if we have, then we need to clear it out
		[self clear];
	}
	return o;
}

-(void) addObject:(id) o
{
	[data addObject:o];
}

-(NSInteger) size
{
	return [data count] - p;
}

- (NSInteger) range
{
    return range;
}

-(NSInteger) count
{
	return [data count];
}

-(id) head
{
	return [self objectAtIndex:0];
}

-(id) objectAtIndex:(NSInteger) k
{
    NSInteger absIndex;

    absIndex = p + k;
	if (absIndex >= [data count]) {
		@throw [ANTLRRuntimeException newANTLRNoSuchElementException:[NSString stringWithFormat:@"queue index %d > last index %d", absIndex, [data count]-1]];
	}
	if (absIndex < 0) {
	    @throw [ANTLRRuntimeException newANTLRNoSuchElementException:[NSString stringWithFormat:@"queue index %d < 0", absIndex]];
	}
	if ( absIndex > range ) range = absIndex;
	return [data objectAtIndex:absIndex];
}

-(NSString *) description
{
	NSMutableString *buf = [NSMutableString stringWithCapacity:30];
	NSInteger n = [self size];
	for (NSInteger k = 0; k < n; k++) {
		[buf appendString:[[self objectAtIndex:k] description]];
		if ((k + 1) < n) {
			[buf appendString:@" "];
		}
	}
	return buf;
}

#ifdef DONTUSENOMO
- (NSAutoreleasePool *)getPool
{
    return pool;
}

- (void)setPool:(NSAutoreleasePool *)aPool
{
    pool = aPool;
}
#endif

- (NSMutableArray *)getData
{
    return data;
}

- (void)setData:(NSMutableArray *)myData
{
    data = myData;
}


- (NSInteger) getP
{
    return p;
}

- (void) setP:(NSInteger) anInt
{
    p = anInt;
}

- (NSString *) toString
{
    return [self description];
}

@end

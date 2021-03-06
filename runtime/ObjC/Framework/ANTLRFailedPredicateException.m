// [The "BSD licence"]
// Copyright (c) 2006-2007 Kay Roepke 20110 Alan Condit
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

#import "ANTLRFailedPredicateException.h"


@implementation ANTLRFailedPredicateException

+ (ANTLRFailedPredicateException *) exceptionWithRuleName:(NSString *)theRuleName predicate:(NSString *)thePredicate stream:(id<ANTLRIntStream>)theStream
{
	return [[ANTLRFailedPredicateException alloc] initWithRuleName:theRuleName predicate:thePredicate stream:theStream];
}

- (ANTLRFailedPredicateException *) initWithRuleName:(NSString *)theRuleName predicate:(NSString *)thePredicate stream:(id<ANTLRIntStream>)theStream
{
	if ((self = [super initWithStream:theStream])) {
		[self setPredicate:thePredicate];
		[self setRuleName:theRuleName];
	}
	return self;
}

- (void) dealloc
{
	[self setPredicate:nil];
	[self setRuleName:nil];
	[super dealloc];
}

- (NSString *) description
{
	NSMutableString *desc = (NSMutableString *)[super description];
	[desc appendFormat:@" rule: %@ predicate failed: %@", ruleName, predicate];
	return desc;
}

#ifdef DONTUSEYET
- (NSString *) getPredicate
{
	return predicate;
}

- (void) setPredicate:(NSString *)thePredicate
{
	if (thePredicate != predicate) {
		[thePredicate retain];
		[predicate release];
		predicate = thePredicate;
	}
}

- (NSString *) getRuleName
{
	return ruleName;
}

- (void) setRuleName:(NSString *)theRuleName
{
	if (theRuleName != ruleName) {
		[theRuleName retain];
		[ruleName release];
		ruleName = theRuleName;
	}
}
#endif

@end

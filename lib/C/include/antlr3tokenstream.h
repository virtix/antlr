/** \file
 * Defines teh interface for an ANTLR3 common token stream. Custom token streams shuld creaet
 * one of these and then override any funcitnos by installing their own pointers
 * to implement the various functions.
 */
#ifndef	_ANTLR3_TOKENSTREAM_H
#define	_ANTLR3_TOKENSTREAM_H

#include    <antlr3defs.h>
#include    <antlr3string.h>
#include    <antlr3collections.h>
#include    <antlr3input.h>
#include    <antlr3commontoken.h>
#include    <antlr3bitset.h>

/** Definition of a token source, which has a pointer to a function that 
 *  returns the next token (using a token factory if it is going to be
 *  efficient) and a pointer to an ANTLR3_INPUT_STREAM. This is slightly
 *  differnt to the Java interface because we have no way to implement
 *  multiple interfaces without defining them in the interface structure
 *  or casting (void *), which is too convoluted.
 */
typedef struct ANTLR3_TOKEN_SOURCE_struct
{
    /** Pointer to a function that returns the next token in the stream. 
     */
    pANTLR3_COMMON_TOKEN    (*nextToken)(struct ANTLR3_TOKEN_SOURCE_struct * tokenSource);

    /** Whoever is providing tokens, needs to provide a string factory too
     */
    pANTLR3_STRING_FACTORY  strFactory;

    /** Whatever is supplying the token source interface, needs a pointer to 
     *  itself so that this pointer can be passed to it when the nextToken
     *  function is called.
     */
    void		    * super;
}
    ANTLR3_TOKEN_SOURCE;

/** Definition of the ANTLR3 commont token stream interface.
 * \remark
 * Much of the documentation for tihs interface is stolen from Ter's Java implementation.
 */
typedef	struct ANTLR3_TOKEN_STREAM_struct
{
    /** Pointer to the token source for this stream
     */
    pANTLR3_TOKEN_SOURCE    tokenSource;

    /** Whatever is providing this interface needs a pointer to itself
     *  so that this can be passed back to it whenever the api functions
     *  are called.
     */
    void	      * super;

    /** All input streams implement the ANTLR3_INT_STREAM interface...
     */
    pANTLR3_INT_STREAM	    istream;

    /** Get Token at current input pointer + i ahead where i=1 is next Token.
     *  i<0 indicates tokens in the past.  So -1 is previous token and -2 is
     *  two tokens ago. LT(0) is undefined.  For i>=n, return Token.EOFToken.
     *  Return null for LT(0) and any index that results in an absolute address
     *  that is negative.
     */
    pANTLR3_COMMON_TOKEN    (*_LT)		(struct ANTLR3_TOKEN_STREAM_struct * tokenStream, ANTLR3_INT64 k);

    /** Get a token at an absolute index i; 0..n-1.  This is really only
     *  needed for profiling and debugging and token stream rewriting.
     *  If you don't want to buffer up tokens, then this method makes no
     *  sense for you.  Naturally you can't use the rewrite stream feature.
     *  I believe DebugTokenStream can easily be altered to not use
     *  this method, removing the dependency.
     */
    pANTLR3_COMMON_TOKEN    (*get)		(struct ANTLR3_TOKEN_STREAM_struct * tokenStream, ANTLR3_UINT64 i);

    /** Where is this stream pulling tokens from?  This is not the name, but
     *  a pointer into an interface that contains a ANTLR3_TOKEN_SOURCE interface.
     *  The Token Source interface contains a pointer to the input stream and a pointer
     *  to a function that retusn the next token.
     */
    pANTLR3_TOKEN_SOURCE    (*getTokenSource)	(struct ANTLR3_TOKEN_STREAM_struct * tokenStream);

    /** Function that installs a token source for teh stream
     */
    void		    (*setTokenSource)	(struct ANTLR3_TOKEN_STREAM_struct * tokenStream,
						 pANTLR3_TOKEN_SOURCE		   tokenSource);

    /** Return the text of all the tokens in the stream, as the old tramp in 
     *  Leeds market used to say; "Get the lot!"
     */
    pANTLR3_STRING	    (*toString)		(struct ANTLR3_TOKEN_STREAM_struct * tokenStream);

    /** Return the text of all tokens from start to stop, inclusive.
     *  If the stream does not buffer all the tokens then it can just
     *  return an empty ANTLR3_STRING or NULL;  Grammars should not access $ruleLabel.text in
     *  an action in that case.
     */
    pANTLR3_STRING	    (*toStringSS)	(struct ANTLR3_TOKEN_STREAM_struct * tokenStream, ANTLR3_UINT64 start, ANTLR3_UINT64 stop);

    /** Because the user is not required to use a token with an index stored
     *  in it, we must provide a means for two token objects themselves to
     *  indicate the start/end location.  Most often this will just delegate
     *  to the other toString(int,int).  This is also parallel with
     *  the pTREENODE_STREAM->toString(Object,Object).
     */
    pANTLR3_STRING	    (*toStringTT)	(struct ANTLR3_TOKEN_STREAM_struct * tokenStream, pANTLR3_COMMON_TOKEN start, pANTLR3_COMMON_TOKEN stop);

    /** Function that knows how to free the memory for an ANTLR3_TOKEN_STREAM
     */
    void		    (*free)		(struct ANTLR3_TOKEN_STREAM_struct * tokenStream);
}
    ANTLR3_TOKEN_STREAM;

/** Common token stream is an implementation of ANTLR_TOKEN_STREAM for the default
 *  parsers and recognizers. You may of course build your own implementation if
 *  you are so inclined.
 */
typedef	struct	ANTLR3_COMMON_TOKEN_STREAM_struct
{
    /** The ANTLR3_TOKEN_STREAM interface implementation, which also includes
     *  the intstream implementation. We coudl duplicate the pANTLR_INT_STREAM
     *  in this interface and initialize it to a copy, but this could be confusing
     *  it just results in one more level of indirection and I think that with
     *  judicial use of 'const' later, the optimizer will do decent job.
     */
    pANTLR3_TOKEN_STREAM    tstream;

    /** Whatever is supplying the COMMON_TOKEN_STREAM needs a pointer to itself
     *  so that this can be accessed by any of the API functions which it implements.
     */
    void		    * super;

    /** Records every single token pulled from the source indexed by the token index.
     *  There might be more efficient ways to do this, such as referencing directly in to
     *  the token factory pools, but for now this is convienent and the ANTLR3_LIST is not
     *  a huge overhead as it only stores pointers anyway, but allows for iterations and 
     *  so on.
     */
    pANTLR3_LIST	    tokens;

    /** Override map of tokens. If a token type has an entry in here, then
     *  the pointer in the table points to an int, being the override channel number
     *  that should always be used for this token type.
     */
    pANTLR3_LIST	    channelOverrides;

    /** Discared set. If a token has an entry in this table, then it is thrown
     *  away (data pointer is always NULL).
     */
    pANTLR3_LIST	    discardSet;

    /* The channel number that this token stream is tuned to. For instance, whitespace
     * is usually tuned to channel 99, which no token stream would normally tune to and
     * so it is thrown away.
     */
    ANTLR3_UINT32	    channel;

    /** If this flag is set to ANTLR3_TRUE, then tokens that the stream sees that are not
     *  in thechannel that this stream is tuned to, are not tracked in the
     *  tokens table. When set to false, ALL tokens are added to the tracking.
     */
    ANTLR3_BOOLEAN	    discardOffChannel;

    /** The index into the tokens list of the current token (the next one that will be
     *  consumed. p = -1 indicates that the token list is empty.
     */
    ANTLR3_INT64	    p;

    /** A simple filter mechanism whereby you can tell this token stream
     *  to force all tokens of type ttype to be on channel.  For example,
     *  when interpreting, we cannot exec actions so we need to tell
     *  the stream to force all WS and NEWLINE to be a different, ignored
     *  channel.
     */
    void		    (*setTokenTypeChannel)  (struct ANTLR3_COMMON_TOKEN_STREAM_struct * tokenStream, 
							ANTLR3_UINT32 ttype, ANTLR3_UINT32 channel);

    /** Add a particular token type to the discard set. If a token is found to belong 
     *  to this set, then it is skipped/thrown away
     */
    void		    (*discardTokenType)	    (struct ANTLR3_COMMON_TOKEN_STREAM_struct * tokenStream, ANTLR3_INT32 ttype);

    /** Signal to discard off channel tokens from here on in.
     */
    void		    (*discardOffChannelToks)(struct ANTLR3_COMMON_TOKEN_STREAM_struct * tokenStream, ANTLR3_BOOLEAN discard);

    /** Function that returns a pointer to the ANTLR3_LIST of all tokens
     *  in the stream (this causes the buffer to fill if we have not get any yet)
     */
    pANTLR3_LIST	    (*getTokens)	    (struct ANTLR3_COMMON_TOKEN_STREAM_struct * tokenStream);

    /** Function that returns all the tokens between a start and a stop index.
     *  TODO: This is a new list (Ack! Maybe this is a reason to have factories for LISTS adn HASHTABLES etc :-( come back to this)
     */
    pANTLR3_LIST	    (*getTokenRange)	    (struct ANTLR3_COMMON_TOKEN_STREAM_struct * tokenStream, ANTLR3_UINT64 start, ANTLR3_UINT64 stop);

    /** Function that returns all the tokens indicated by the specified bitset, within a range of tokens
     */
    pANTLR3_LIST	    (*getTokensSet)	    (struct ANTLR3_COMMON_TOKEN_STREAM_struct * tokenStream, 
							ANTLR3_UINT64 start, ANTLR3_UINT64 stop, pANTLR3_BITSET types);
    
    /** Function that retruns all the tokens indicated by being a member of the supplied List
     */
    pANTLR3_LIST	    (*getTokensList)	    (struct ANTLR3_COMMON_TOKEN_STREAM_struct * tokenStream, 
							ANTLR3_UINT64 start, ANTLR3_UINT64 stop, pANTLR3_LIST list);

    /** Function that returns all tokens of a certain type within a range.
     */
    pANTLR3_LIST	    (*getTokensType)	    (struct ANTLR3_COMMON_TOKEN_STREAM_struct * tokenStream, 
							ANTLR3_UINT64 start, ANTLR3_UINT64 stop, ANTLR3_UINT32 type);

    /** Function that knows how to free an ANTLR3_COMMON_TOKEN_STREAM
     */
    void		    (*free)		    (struct ANTLR3_COMMON_TOKEN_STREAM_struct * tokenStream);
}
    ANTLR3_COMMON_TOKEN_STREAM;


#endif

/*
 [The "BSD licence"]
 Copyright (c) 2005 Terence Parr
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions
 are met:
 1. Redistributions of source code must retain the above copyright
    notice, this list of conditions and the following disclaimer.
 2. Redistributions in binary form must reproduce the above copyright
    notice, this list of conditions and the following disclaimer in the
    documentation and/or other materials provided with the distribution.
 3. The name of the author may not be used to endorse or promote products
    derived from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package org.antlr.runtime;

/** A lexer is recognizer that draws input symbols from a character stream.
 *  lexer grammars result in a subclass of this object. A Lexer object
 *  uses simplified match() and error recovery mechanisms in the interest
 *  of speed.
 */
public abstract class Lexer extends BaseRecognizer implements TokenSource {
	/** Where is the lexer drawing characters from? */
    protected CharStream input;

	/** The goal of all lexer rules/methods is to create a token object.
	 *  This is an instance variable as multiple rules may collaborate to
	 *  create a single token.  For example, NUM : INT | FLOAT ;
	 *  In this case, you want the INT or FLOAT rule to set token and not
	 *  have it reset to a NUM token in rule NUM.
	 */
    protected Token token;

	/** What character index in the stream did the current token start at?
	 *  Needed, for example, to get the text for current token.  Set at
	 *  the start of nextToken.
 	 */
	protected int tokenStartCharIndex = -1;

	public Lexer() {
	}

	public Lexer(CharStream input) {
		this.input = input;
	}

	/** Return a token from this source; i.e., match a token on the char
	 *  stream.
	 */
    public Token nextToken() {
        token=null;
		tokenStartCharIndex = getCharIndex();
		while (true) {
            if ( input.LA(1)==CharStream.EOF ) {
                return Token.EOF_TOKEN;
            }
            try {
                mTokens();
                return token;
            }
            catch (RecognitionException re) {
                reportError(re);
                recover(re);
            }
        }
    }

	/** This is the lexer entry point that sets instance var 'token' */
	public abstract void mTokens() throws RecognitionException;

	/** Set the char stream and reset the lexer */
	public void setCharStream(CharStream input) {
		this.input = input;
		token = null;
		tokenStartCharIndex = -1;
	}

	public void emit(Token token) {
		this.token = token;
	}

	public void emit(int tokenType,
					 int line, int charPosition,
					 int channel,
					 int start, int stop) {
		Token token = new CommonToken(input, tokenType, channel, start, stop);
		token.setLine(line);
		token.setCharPositionInLine(charPosition);
		emit(token);
	}

    public void match(String s) throws MismatchedTokenException {
        int i = 0;
        while ( i<s.length() ) {
            if ( input.LA(1)!=s.charAt(i) ) {
				if ( backtracking>0 ) {
					failed = true;
					return;
				}
				MismatchedTokenException mte =
					new MismatchedTokenException(s.charAt(i), input);
				recover(mte);
				throw mte;
            }
            i++;
            input.consume();
			failed = false;
        }
    }

    public void matchAny() {
        input.consume();
    }

    public void match(int c) throws MismatchedTokenException {
        if ( input.LA(1)!=c ) {
			if ( backtracking>0 ) {
				failed = true;
				return;
			}
			MismatchedTokenException mte =
				new MismatchedTokenException(c, input);
			recover(mte);
			throw mte;
        }
        input.consume();
		failed = false;
    }

    public void matchRange(int a, int b)
		throws MismatchedRangeException
	{
        if ( input.LA(1)<a || input.LA(1)>b ) {
			if ( backtracking>0 ) {
				failed = true;
				return;
			}
            MismatchedRangeException mre =
				new MismatchedRangeException(a,b,input);
			recover(mre);
			throw mre;
        }
        input.consume();
		failed = false;
    }

    public int getLine() {
        return input.getLine();
    }

    public int getCharPositionInLine() {
        return input.getCharPositionInLine();
    }

	/** What is the index of the current character of lookahead? */
	public int getCharIndex() {
		return input.index();
	}

	/** Return the text matched so far for the current token. */
	public String getText() {
		return input.substring(tokenStartCharIndex,getCharIndex()-1);
	}

	/** Report a recognition problem.  Java is not polymorphic on the
	 *  argument types so you have to check the type of exception yourself.
	 *  That's not very clean but it's better than generating a bunch of
	 *  catch clauses in each rule and makes it easy to extend with
	 *  more exceptions w/o breaking old code.
	 */
	public void reportError(RecognitionException e) {
		System.err.print(Parser.getRuleInvocationStack(e,getClass().getName())+
						 ": line "+input.getLine()+" ");
		if ( e instanceof MismatchedTokenException ) {
			MismatchedTokenException mte = (MismatchedTokenException)e;
			System.err.println("mismatched char: '"+
							   (char)input.LA(1)+
							   "' on line "+getLine()+
							   "; expecting char '"+(char)mte.expecting+"'");
		}
		else if ( e instanceof NoViableAltException ) {
			NoViableAltException nvae = (NoViableAltException)e;
			System.err.println(nvae.grammarDecisionDescription+
							   " state "+nvae.stateNumber+
							   " (decision="+nvae.decisionNumber+
							   ") no viable alt line "+getLine()+":"+getCharPositionInLine()+"; char='"+
							   (char)input.LA(1)+"'");
		}
		else if ( e instanceof EarlyExitException ) {
			EarlyExitException eee = (EarlyExitException)e;
			System.err.println("required (...)+ loop (decision="+
							   eee.decisionNumber+
							   ") did not match anything; on line "+
							   getLine()+":"+getCharPositionInLine()+" char="+
							   (char)input.LA(1)+"'");
		}
		else if ( e instanceof MismatchedSetException ) {
			MismatchedSetException mse = (MismatchedSetException)e;
			System.err.println("mismatched char: '"+
							   (char)input.LA(1)+
							   "' on line "+getLine()+
							   ":"+getCharPositionInLine()+
							   "; expecting set "+mse.expecting);
		}
		else if ( e instanceof MismatchedNotSetException ) {
			MismatchedSetException mse = (MismatchedSetException)e;
			System.err.println("mismatched char: '"+
							   (char)input.LA(1)+
							   "' on line "+getLine()+
							   ":"+getCharPositionInLine()+
							   "; expecting set "+mse.expecting);
		}
		else if ( e instanceof MismatchedRangeException ) {
			MismatchedRangeException mre = (MismatchedRangeException)e;
			System.err.println("mismatched char: '"+
							   (char)input.LA(1)+
							   "' on line "+getLine()+
							   ":"+getCharPositionInLine()+
							   "; expecting set '"+(char)mre.a+"'..'"+
							   (char)mre.b+"'");
		}
		else if ( e instanceof FailedPredicateException ) {
			FailedPredicateException fpe = (FailedPredicateException)e;
			System.err.println("rule "+fpe.ruleName+" failed predicate: {"+
							   fpe.predicateText+"}?");			
		}
	}

	/** Lexers can normally match any char in it's vocabulary after matching
	 *  a token, so do the easy thing and just kill a character and hope
	 *  it all works out.  You can instead use the rule invocation stack
	 *  to do sophisticated error recovery if you are in a fragment rule.
	 */
	public void recover(RecognitionException re) {
		//System.out.println("consuming char "+(char)input.LA(1)+" during recovery");
		//re.printStackTrace();
		input.consume();
	}

}

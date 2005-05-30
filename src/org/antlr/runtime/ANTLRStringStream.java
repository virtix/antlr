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

import java.util.List;
import java.util.ArrayList;

public class ANTLRStringStream implements CharStream {
    protected String input;

	/** 0..n-1 index into string of next char */
	protected int p=0;

	/** line number 1..n within the input */
	protected int line = 1;

	/** The index of the character relative to the beginning of the line 0..n-1 */
	protected int charPositionInLine = 0;

	protected List markers;

    public ANTLRStringStream(String input) {
        this.input = input;
		markers = new ArrayList(1);
		markers.add(new CharStreamState());
    }

    public void consume() {
        if ( p< input.length() ) {
			charPositionInLine++;
			if ( input.charAt(p)=='\n' ) {
				line++;
				charPositionInLine=0;
			}
			p++;
        }
    }

    public int LA(int i) {
        if ( p >= input.length() ) {
            //System.out.println("LA("+i+")=EOF; p="+p);
            return CharStream.EOF;
        }
        //System.out.println("LA("+i+")="+input.charAt(p+i-1)+"; p="+p);
        return input.charAt(p+i-1);
    }

    public int LA(int marker, int i) {
        return input.charAt(marker+i-1);
    }

    public int mark() {
		CharStreamState state = (CharStreamState)markers.get(0);
		state.p = p;
		state.line = line;
		state.charPositionInLine = charPositionInLine;
        return 0;
    }

    /** Return the current input symbol index 0..n where n indicates the
     *  last symbol has been read.
     */
    public int index() {
        return p;
    }

	public int size() {
		return input.length();
	}

    public void rewind(int m) {
		CharStreamState state = (CharStreamState)markers.get(m);
		p = state.p;
		line = state.line;
		charPositionInLine = state.charPositionInLine;
    }

	public String substring(int start, int stop) {
		return input.substring(start,stop+1);
	}

	public int getLine() {
		return line;
	}

	public int getCharPositionInLine() {
		return charPositionInLine;
	}

	public void setLine(int line) {
		this.line = line;
	}

	public void setCharPositionInLine(int pos) {
		this.charPositionInLine = pos;
	}

	public String getSourceName() {
		return "<string>";
	}	
}

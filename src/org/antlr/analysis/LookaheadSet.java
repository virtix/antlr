package org.antlr.tool;

import org.antlr.misc.BitSet;

/** An LL(1) lookahead set; contains a set of token types and a "hasEOF"
 *  condition when the set contains EOF.  Since EOF is -1 everywhere and -1
 *  cannot be stored in my BitSet, I set a condition here.  There may be other
 *  reasons in the future to abstract a LookaheadSet over a raw BitSet.
 */
public class LookaheadSet {
	public BitSet tokenTypeSet;
	public boolean hasEOF;

	public LookaheadSet() {
	}

	public LookaheadSet(BitSet s) {
		tokenTypeSet = s;
	}

	public void orInPlace(LookaheadSet other) {
		if ( this.tokenTypeSet==null ) {
			this.tokenTypeSet = other.tokenTypeSet;
		}
		else {
			this.tokenTypeSet.orInPlace(other.tokenTypeSet);
		}
		this.hasEOF = this.hasEOF || other.hasEOF;
	}

	public String toString(Grammar g) {
		if ( tokenTypeSet==null ) {
			if ( hasEOF ) {
				return "EOF";
			}
			return "";
		}
		String r = tokenTypeSet.toString(g);
		if ( hasEOF ) {
			return r+"+EOF";
		}
		return r;
	}

	public static LookaheadSet EOF() {
		LookaheadSet eof = new LookaheadSet();
		eof.hasEOF = true;
		return eof;
	}

	public String toString() {
		return toString(null);
	}
}

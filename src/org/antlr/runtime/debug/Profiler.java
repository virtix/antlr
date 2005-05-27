package org.antlr.runtime.debug;

import org.antlr.runtime.Token;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.IntStream;
import org.antlr.tool.GrammarReport;

import java.util.StringTokenizer;

/** Using the debug event interface, track what is happening in the parser
 *  and record statistics about the runtime.
 */
public class Profiler implements DebugEventListener {
	/** Because I may change the stats, I need to track that for later
	 *  computations to be consistent.
	 */
	public static final String Version = "1";
	public static final String RUNTIME_STATS_FILENAME = "runtime.stats";
	public static final int NUM_RUNTIME_STATS = 12;

	//public IntStream input;

	public DebugParser parser = null;

	// working variables

	public int level = 0;
	private boolean inDecision = false;
	public int maxLookaheadInDecision = 0;

	// stats below

	public int numRuleInvocations = 0;
	public int numFixedDecisions = 0;
	public int numCyclicDecisions = 0;
	public int[] decisionMaxFixedLookaheads = new int[200];
	public int[] decisionMaxCyclicLookaheads = new int[200];

	/*
	public Profiler(IntStream input) {
		this.input = input;
	}
	*/

	public Profiler(DebugParser parser) {
		this.parser = parser;
	}

	public void enterRule(String ruleName) {
		level++;
		numRuleInvocations++;
	}

	public void exitRule(String ruleName) {
		level--;
	}

	public void enterAlt(int alt) {;}
	public void enterSubRule(int d) {;}
	public void exitSubRule(int d) {;}

	public void enterDecision(int decisionNumber) {
		inDecision=true;
	}

	public void exitDecision(int decisionNumber) {
		if ( parser.isCyclicDecision ) {
			numCyclicDecisions++;
		}
		else {
			numFixedDecisions++;
		}
		inDecision=false;
		if ( parser.isCyclicDecision ) {
			// cyclic decisions use number of consumes + 1; we track consumes
			// so add one to the max lookahead.  For example, if it's LL(1)
			// it will use LA(1) but will not consume.
			maxLookaheadInDecision++;
			if ( numCyclicDecisions>=decisionMaxCyclicLookaheads.length ) {
				int[] bigger = new int[decisionMaxCyclicLookaheads.length*2];
				System.arraycopy(decisionMaxCyclicLookaheads,0,bigger,0,decisionMaxCyclicLookaheads.length);
				decisionMaxCyclicLookaheads = bigger;
			}
			decisionMaxCyclicLookaheads[numCyclicDecisions-1] = maxLookaheadInDecision;
		}
		else {
			if ( numFixedDecisions>=decisionMaxFixedLookaheads.length ) {
				int[] bigger = new int[decisionMaxFixedLookaheads.length*2];
				System.arraycopy(decisionMaxFixedLookaheads,0,bigger,0,decisionMaxFixedLookaheads.length);
				decisionMaxFixedLookaheads = bigger;
			}
			decisionMaxFixedLookaheads[numFixedDecisions-1] = maxLookaheadInDecision;
		}
		parser.isCyclicDecision = false;
		maxLookaheadInDecision = 0;
	}

	public void location(int line, int pos) {;}

	public void consumeToken(Token token) {
		if ( inDecision && parser.isCyclicDecision ) {
			maxLookaheadInDecision++;
		}
	}
	public void consumeHiddenToken(Token token) {;}

	public void LT(int i, Token t) {
		if ( inDecision && !parser.isCyclicDecision ) {
			if ( i>maxLookaheadInDecision ) {
				maxLookaheadInDecision = i;
			}
		}
	}

	public void mark(int i) {;}
	public void rewind(int i) {;}
	public void recognitionException(RecognitionException e) {;}
	public void beginResync() {;}
	public void endResync() {;}

	public void commence() {;}

	public void terminate() {
		System.out.println(toString());
		GrammarReport.writeReport(RUNTIME_STATS_FILENAME,toNotifyString());
	}

	// R E P O R T I N G

	public String toNotifyString() {
		decisionMaxFixedLookaheads = trim(decisionMaxFixedLookaheads, numFixedDecisions);
		decisionMaxCyclicLookaheads = trim(decisionMaxCyclicLookaheads, numCyclicDecisions);
		StringBuffer buf = new StringBuffer();
		buf.append(Version);
		buf.append('\t');
		buf.append(numRuleInvocations);
		buf.append('\t');
		buf.append(numFixedDecisions);
		buf.append('\t');
		buf.append(GrammarReport.min(decisionMaxFixedLookaheads));
		buf.append('\t');
		buf.append(GrammarReport.max(decisionMaxFixedLookaheads));
		buf.append('\t');
		buf.append(GrammarReport.avg(decisionMaxFixedLookaheads));
		buf.append('\t');
		buf.append(GrammarReport.stddev(decisionMaxFixedLookaheads));
		buf.append('\t');
		buf.append(numCyclicDecisions);
		buf.append('\t');
		buf.append(GrammarReport.min(decisionMaxCyclicLookaheads));
		buf.append('\t');
		buf.append(GrammarReport.max(decisionMaxCyclicLookaheads));
		buf.append('\t');
		buf.append(GrammarReport.avg(decisionMaxCyclicLookaheads));
		buf.append('\t');
		buf.append(GrammarReport.stddev(decisionMaxCyclicLookaheads));
		return buf.toString();
	}

	public String toString() {
        return toString(toNotifyString());
	}

	protected static String[] decodeReportData(String data) {
		String[] fields = new String[NUM_RUNTIME_STATS];
		StringTokenizer st = new StringTokenizer(data, "\t");
		int i = 0;
		while ( st.hasMoreTokens() ) {
			fields[i] = st.nextToken();
			i++;
		}
		if ( i!=NUM_RUNTIME_STATS ) {
			return null;
		}
		return fields;
	}

	public static String toString(String notifyDataLine) {
		String[] fields = decodeReportData(notifyDataLine);
		if ( fields==null ) {
			return null;
		}
		StringBuffer buf = new StringBuffer();
        buf.append("ANTLR Runtime Report; Profile Version ");
		buf.append(fields[0]);
		buf.append('\n');
		buf.append("Number of rule invocations ");
		buf.append(fields[1]);
		buf.append('\n');
		buf.append("number of fixed lookahead decisions ");
		buf.append(fields[2]);
		buf.append('\n');
		buf.append("min lookahead used in a fixed lookahead decision ");
		buf.append(fields[3]);
		buf.append('\n');
		buf.append("max lookahead used in a fixed lookahead decision ");
		buf.append(fields[4]);
		buf.append('\n');
		buf.append("average lookahead depth used in fixed lookahead decisions ");
		buf.append(fields[5]);
		buf.append('\n');
		buf.append("standard deviation of depth used in fixed lookahead decisions ");
		buf.append(fields[6]);
		buf.append('\n');
		buf.append("number of arbitrary lookahead decisions ");
		buf.append(fields[7]);
		buf.append('\n');
		buf.append("min lookahead used in an arbitrary lookahead decision ");
		buf.append(fields[8]);
		buf.append('\n');
		buf.append("max lookahead used in an arbitrary lookahead decision ");
		buf.append(fields[9]);
		buf.append('\n');
		buf.append("average lookahead depth used in arbitrary lookahead decisions ");
		buf.append(fields[10]);
		buf.append('\n');
		buf.append("standard deviation of depth used in arbitrary lookahead decisions ");
		buf.append(fields[11]);
		buf.append('\n');
		return buf.toString();
	}

	protected int[] trim(int[] X, int n) {
		if ( n<X.length ) {
			int[] trimmed = new int[n];
			System.arraycopy(X,0,trimmed,0,n);
			X = trimmed;
		}
		return X;
	}

}

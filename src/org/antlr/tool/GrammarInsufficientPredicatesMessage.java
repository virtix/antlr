package org.antlr.tool;

import org.antlr.stringtemplate.StringTemplate;
import org.antlr.analysis.DecisionProbe;
import org.antlr.analysis.DFAState;
import org.antlr.analysis.NFAState;
import org.antlr.analysis.SemanticContext;
import antlr.Token;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class GrammarInsufficientPredicatesMessage extends Message {
	public DecisionProbe probe;
    public List alts;

	public GrammarInsufficientPredicatesMessage(DecisionProbe probe,
												List alts)
	{
		super(ErrorManager.MSG_INSUFFICIENT_PREDICATES);
		this.probe = probe;
		this.alts = alts;
	}

	public String toString() {
		GrammarAST decisionASTNode = probe.dfa.getDecisionASTNode();
		int line = decisionASTNode.getLine();
		int col = decisionASTNode.getColumn();
		String fileName = probe.dfa.nfa.grammar.getFileName();
		StringTemplate st = getMessageTemplate();
		if ( fileName!=null ) {
			st.setAttribute("file", fileName);
		}
		st.setAttribute("line", new Integer(line));
		st.setAttribute("col", new Integer(col));

		st.setAttribute("alts", alts);

		return st.toString();
	}

}

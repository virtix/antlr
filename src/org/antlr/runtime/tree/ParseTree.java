package org.antlr.runtime.tree;

/** A record of the rules used to match a token sequence.  The tokens
 *  end up as the leaves of this tree and rule nodes are the interior nodes.
 *  This really adds no functionality, it is just an alias for CommonTree
 *  that is more meaningful (specific).
 */
public class ParseTree extends CommonTree {
	public ParseTree(Object label) {
		super(label);
	}
}

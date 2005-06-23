package org.antlr.test;

import org.antlr.test.unit.TestSuite;

public class TestAutoAST extends TestSuite {
	public void testTokenList() throws Exception {
		String grammar =
			"grammar foo;\n" +
			"options {output=AST;}\n" +
			"a : ID INT ;\n" +
			"ID : 'a'..'z'+ ;\n" +
			"INT : '0'..'9'+;\n" +
			"WS : (' '|'\n') {channel=99;} ;\n";
		String found =
			TestCompileAndExecSupport.execParser("foo.g", grammar, "foo", "fooLexer",
												 "a", "abc 34");
		String expecting = "abc 34\n";
		assertEqual(found, expecting);
	}

	public void testTokenListInSingleAltBlock() throws Exception {
		String grammar =
			"grammar foo;\n" +
			"options {output=AST;}\n" +
			"a : (ID INT) ;\n" +
			"ID : 'a'..'z'+ ;\n" +
			"INT : '0'..'9'+;\n" +
			"WS : (' '|'\n') {channel=99;} ;\n";
		String found =
			TestCompileAndExecSupport.execParser("foo.g", grammar, "foo", "fooLexer",
												 "a", "abc 34");
		String expecting = "abc 34\n";
		assertEqual(found, expecting);
	}

	public void testSimpleRootAtOuterLevel() throws Exception {
		String grammar =
			"grammar foo;\n" +
			"options {output=AST;}\n" +
			"a : ID^ INT ;\n" +
			"ID : 'a'..'z'+ ;\n" +
			"INT : '0'..'9'+;\n" +
			"WS : (' '|'\n') {channel=99;} ;\n";
		String found =
			TestCompileAndExecSupport.execParser("foo.g", grammar, "foo", "fooLexer",
												 "a", "abc 34");
		String expecting = "(abc 34)\n";
		assertEqual(found, expecting);
	}

	public void testSimpleRootAtOuterLevelReverse() throws Exception {
		String grammar =
			"grammar T;\n" +
			"options {output=AST;}\n" +
			"a : INT ID^ ;\n" +
			"ID : 'a'..'z'+ ;\n" +
			"INT : '0'..'9'+;\n" +
			"WS : (' '|'\n') {channel=99;} ;\n";
		String found =
			TestCompileAndExecSupport.execParser("t.g", grammar, "T", "TLexer",
												 "a", "34 abc");
		String expecting = "(abc 34)\n";
		assertEqual(found, expecting);
	}

	public void testBang() throws Exception {
		String grammar =
			"grammar T;\n" +
			"options {output=AST;}\n" +
			"a : ID INT! ID! INT ;\n" +
			"ID : 'a'..'z'+ ;\n" +
			"INT : '0'..'9'+;\n" +
			"WS : (' '|'\n') {channel=99;} ;\n";
		String found =
			TestCompileAndExecSupport.execParser("t.g", grammar, "T", "TLexer",
												 "a", "abc 34 dag 4532");
		String expecting = "abc 4532\n";
		assertEqual(found, expecting);
	}

	public void testLoopRoot() throws Exception {
		String grammar =
			"grammar T;\n" +
			"options {output=AST;}\n" +
			"a : ( ID^ INT )* ;\n" +
			"ID : 'a'..'z'+ ;\n" +
			"INT : '0'..'9'+;\n" +
			"WS : (' '|'\n') {channel=99;} ;\n";
		String found =
			TestCompileAndExecSupport.execParser("t.g", grammar, "T", "TLexer",
												 "a", "a 1 b 2 c 3");
		String expecting = "(a 1) (b 2) (c 3)\n";
		assertEqual(found, expecting);
	}

	public void testLoopRootReverse() throws Exception {
		String grammar =
			"grammar T;\n" +
			"options {output=AST;}\n" +
			"a : ( ID INT^ )* ;\n" +
			"ID : 'a'..'z'+ ;\n" +
			"INT : '0'..'9'+;\n" +
			"WS : (' '|'\n') {channel=99;} ;\n";
		String found =
			TestCompileAndExecSupport.execParser("t.g", grammar, "T", "TLexer",
												 "a", "a 1 b 2 c 3");
		String expecting = "(1 a) (2 b) (3 c)\n";
		assertEqual(found, expecting);
	}

	public void testPlusLoopRoot() throws Exception {
		String grammar =
			"grammar T;\n" +
			"options {output=AST;}\n" +
			"a : ( ID^ INT )+ ;\n" +
			"ID : 'a'..'z'+ ;\n" +
			"INT : '0'..'9'+;\n" +
			"WS : (' '|'\n') {channel=99;} ;\n";
		String found =
			TestCompileAndExecSupport.execParser("t.g", grammar, "T", "TLexer",
												 "a", "a 1 b 2 c 3");
		String expecting = "(a 1) (b 2) (c 3)\n";
		assertEqual(found, expecting);
	}

	public void testPlusLoopRootReverse() throws Exception {
		String grammar =
			"grammar T;\n" +
			"options {output=AST;}\n" +
			"a : ( ID^ INT )+ ;\n" +
			"ID : 'a'..'z'+ ;\n" +
			"INT : '0'..'9'+;\n" +
			"WS : (' '|'\n') {channel=99;} ;\n";
		String found =
			TestCompileAndExecSupport.execParser("t.g", grammar, "T", "TLexer",
												 "a", "a 1 b 2 c 3");
		String expecting = "(a 1) (b 2) (c 3)\n";
		assertEqual(found, expecting);
	}

	public void testOptionalThenRoot() throws Exception {
		String grammar =
			"grammar T;\n" +
			"options {output=AST;}\n" +
			"a : ( ID INT )? ID^ ;\n" +
			"ID : 'a'..'z'+ ;\n" +
			"INT : '0'..'9'+;\n" +
			"WS : (' '|'\n') {channel=99;} ;\n";
		String found =
			TestCompileAndExecSupport.execParser("t.g", grammar, "T", "TLexer",
												 "a", "a 1 b");
		String expecting = "(b a 1)\n";
		assertEqual(found, expecting);
	}

	public void testLabeledStringRoot() throws Exception {
		String grammar =
			"grammar T;\n" +
			"options {output=AST;}\n" +
			"a : v=\"void\"^ ID ';' ;\n" +
			"ID : 'a'..'z'+ ;\n" +
			"INT : '0'..'9'+;\n" +
			"WS : (' '|'\n') {channel=99;} ;\n";
		String found =
			TestCompileAndExecSupport.execParser("t.g", grammar, "T", "TLexer",
												 "a", "void foo;");
		String expecting = "(void foo ;)\n";
		assertEqual(found, expecting);
	}

	public void testWildcard() throws Exception {
		String grammar =
			"grammar T;\n" +
			"options {output=AST;}\n" +
			"a : v=\"void\"^ . ';' ;\n" +
			"ID : 'a'..'z'+ ;\n" +
			"INT : '0'..'9'+;\n" +
			"WS : (' '|'\n') {channel=99;} ;\n";
		String found =
			TestCompileAndExecSupport.execParser("t.g", grammar, "T", "TLexer",
												 "a", "void foo;");
		String expecting = "(void foo ;)\n";
		assertEqual(found, expecting);
	}

	public void testWildcardRoot() throws Exception {
		String grammar =
			"grammar T;\n" +
			"options {output=AST;}\n" +
			"a : v=\"void\" .^ ';' ;\n" +
			"ID : 'a'..'z'+ ;\n" +
			"INT : '0'..'9'+;\n" +
			"WS : (' '|'\n') {channel=99;} ;\n";
		String found =
			TestCompileAndExecSupport.execParser("t.g", grammar, "T", "TLexer",
												 "a", "void foo;");
		String expecting = "(foo void ;)\n";
		assertEqual(found, expecting);
	}

	public void testRootRoot() throws Exception {
		String grammar =
			"grammar T;\n" +
			"options {output=AST;}\n" +
			"a : ID^ INT^ ID ;\n" +
			"ID : 'a'..'z'+ ;\n" +
			"INT : '0'..'9'+;\n" +
			"WS : (' '|'\n') {channel=99;} ;\n";
		String found =
			TestCompileAndExecSupport.execParser("t.g", grammar, "T", "TLexer",
												 "a", "a 34 c");
		String expecting = "(34 a c)\n";
		assertEqual(found, expecting);
	}

	public void testRootRoot2() throws Exception {
		String grammar =
			"grammar T;\n" +
			"options {output=AST;}\n" +
			"a : ID INT^ ID^ ;\n" +
			"ID : 'a'..'z'+ ;\n" +
			"INT : '0'..'9'+;\n" +
			"WS : (' '|'\n') {channel=99;} ;\n";
		String found =
			TestCompileAndExecSupport.execParser("t.g", grammar, "T", "TLexer",
												 "a", "a 34 c");
		String expecting = "(c (34 a))\n";
		assertEqual(found, expecting);
	}

	public void testNestedSubrule() throws Exception {
		String grammar =
			"grammar T;\n" +
			"options {output=AST;}\n" +
			"a : \"void\" (({;}ID|INT) ID | \"null\" ) ';' ;\n" +
			"ID : 'a'..'z'+ ;\n" +
			"INT : '0'..'9'+;\n" +
			"WS : (' '|'\n') {channel=99;} ;\n";
		String found =
			TestCompileAndExecSupport.execParser("t.g", grammar, "T", "TLexer",
												 "a", "void a b;");
		String expecting = "void a b ;\n";
		assertEqual(found, expecting);
	}

	public void testInvokeRule() throws Exception {
		String grammar =
			"grammar T;\n" +
			"options {output=AST;}\n" +
			"a  : type ID ;\n" +
			"type : {;}\"int\" | \"float\" ;\n" +
			"ID : 'a'..'z'+ ;\n" +
			"INT : '0'..'9'+;\n" +
			"WS : (' '|'\n') {channel=99;} ;\n";
		String found =
			TestCompileAndExecSupport.execParser("t.g", grammar, "T", "TLexer",
												 "a", "int a");
		String expecting = "int a\n";
		assertEqual(found, expecting);
	}

	public void testInvokeRuleAsRoot() throws Exception {
		String grammar =
			"grammar T;\n" +
			"options {output=AST;}\n" +
			"a  : type^ ID ;\n" +
			"type : {;}\"int\" | \"float\" ;\n" +
			"ID : 'a'..'z'+ ;\n" +
			"INT : '0'..'9'+;\n" +
			"WS : (' '|'\n') {channel=99;} ;\n";
		String found =
			TestCompileAndExecSupport.execParser("t.g", grammar, "T", "TLexer",
												 "a", "int a");
		String expecting = "(int a)\n";
		assertEqual(found, expecting);
	}

	public void testRuleRootInLoop() throws Exception {
		String grammar =
			"grammar T;\n" +
			"options {output=AST;}\n" +
			"a : ID ('+'^^ ID)* ;\n" +
			"ID : 'a'..'z'+ ;\n" +
			"INT : '0'..'9'+;\n" +
			"WS : (' '|'\n') {channel=99;} ;\n";
		String found =
			TestCompileAndExecSupport.execParser("t.g", grammar, "T", "TLexer",
												 "a", "a+b+c+d");
		String expecting = "(+ (+ (+ a b) c) d)\n";
		assertEqual(found, expecting);
	}

	public void testRuleInvocationRuleRootInLoop() throws Exception {
		String grammar =
			"grammar T;\n" +
			"options {output=AST;}\n" +
			"a : ID (op^^ ID)* ;\n" +
			"op : {;}'+' | '-' ;\n" +
			"ID : 'a'..'z'+ ;\n" +
			"INT : '0'..'9'+;\n" +
			"WS : (' '|'\n') {channel=99;} ;\n";
		String found =
			TestCompileAndExecSupport.execParser("t.g", grammar, "T", "TLexer",
												 "a", "a+b+c-d");
		String expecting = "(- (+ (+ a b) c) d)\n";
		assertEqual(found, expecting);
	}

	public void testTailRecursion() throws Exception {
		String grammar =
			"grammar T;\n" +
			"options {output=AST;}\n" +
			"a : atom (\"exp\"^^ a)? ;\n" +
			"atom : INT ;\n" +
			"ID : 'a'..'z'+ ;\n" +
			"INT : '0'..'9'+;\n" +
			"WS : (' '|'\n') {channel=99;} ;\n";
		String found =
			TestCompileAndExecSupport.execParser("t.g", grammar, "T", "TLexer",
												 "a", "3 exp 4 exp 5");
		String expecting = "(exp 3 (exp 4 5))\n";
		assertEqual(found, expecting);
	}

	public void testSet() throws Exception {
		String grammar =
			"grammar T;\n" +
			"options {output=AST;}\n" +
			"a : ID|INT ;\n" +
			"ID : 'a'..'z'+ ;\n" +
			"INT : '0'..'9'+;\n" +
			"WS : (' '|'\n') {channel=99;} ;\n";
		String found =
			TestCompileAndExecSupport.execParser("t.g", grammar, "T", "TLexer",
												 "a", "abc");
		String expecting = "abc\n";
		assertEqual(found, expecting);
	}

	public void testSetRoot() throws Exception {
		String grammar =
			"grammar T;\n" +
			"options {output=AST;}\n" +
			"a : ('+' | '-')^ ID ;\n" +
			"ID : 'a'..'z'+ ;\n" +
			"INT : '0'..'9'+;\n" +
			"WS : (' '|'\n') {channel=99;} ;\n";
		String found =
			TestCompileAndExecSupport.execParser("t.g", grammar, "T", "TLexer",
												 "a", "+abc");
		String expecting = "(+ abc)\n";
		assertEqual(found, expecting);
	}

	public void testSetAsRuleRootInLoop() throws Exception {
		String grammar =
			"grammar T;\n" +
			"options {output=AST;}\n" +
			"a : ID (('+'|'-')^^ ID)* ;\n" +
			"ID : 'a'..'z'+ ;\n" +
			"INT : '0'..'9'+;\n" +
			"WS : (' '|'\n') {channel=99;} ;\n";
		String found =
			TestCompileAndExecSupport.execParser("t.g", grammar, "T", "TLexer",
												 "a", "a+b-c");
		String expecting = "(- (+ a b) c)\n";
		assertEqual(found, expecting);
	}

	public void testNotSet() throws Exception {
		String grammar =
			"grammar T;\n" +
			"options {output=AST;}\n" +
			"a : ~ID '+' INT ;\n" +
			"ID : 'a'..'z'+ ;\n" +
			"INT : '0'..'9'+;\n" +
			"WS : (' '|'\n') {channel=99;} ;\n";
		String found =
			TestCompileAndExecSupport.execParser("t.g", grammar, "T", "TLexer",
												 "a", "34+2");
		String expecting = "34 + 2\n";
		assertEqual(found, expecting);
	}

	public void testNotSetRoot() throws Exception {
		String grammar =
			"grammar T;\n" +
			"options {output=AST;}\n" +
			"a : ~'+'^ INT ;\n" +
			"ID : 'a'..'z'+ ;\n" +
			"INT : '0'..'9'+;\n" +
			"WS : (' '|'\n') {channel=99;} ;\n";
		String found =
			TestCompileAndExecSupport.execParser("t.g", grammar, "T", "TLexer",
												 "a", "34 55");
		String expecting = "(34 55)\n";
		assertEqual(found, expecting);
	}

	public void testNotSetRuleRootInLoop() throws Exception {
		String grammar =
			"grammar T;\n" +
			"options {output=AST;}\n" +
			"a : INT (~INT^^ INT)* ;\n" +
			"blort : '+' ;\n" +
			"ID : 'a'..'z'+ ;\n" +
			"INT : '0'..'9'+;\n" +
			"WS : (' '|'\n') {channel=99;} ;\n";
		String found =
			TestCompileAndExecSupport.execParser("t.g", grammar, "T", "TLexer",
												 "a", "3+4+5");
		String expecting = "(+ (+ 3 4) 5)\n";
		assertEqual(found, expecting);
	}

	public void testTokenLabelReuse() throws Exception {
		// check for compilation problem due to multiple defines
		String grammar =
			"grammar T;\n" +
			"options {output=AST;}\n" +
			"a : id=ID id=ID {System.out.print(\"2nd id=\"+$id.text+\";\");} ;\n" +
			"ID : 'a'..'z'+ ;\n" +
			"INT : '0'..'9'+;\n" +
			"WS : (' '|'\n') {channel=99;} ;\n";
		String found =
			TestCompileAndExecSupport.execParser("t.g", grammar, "T", "TLexer",
												 "a", "a b");
		String expecting = "2nd id=b;a b\n";
		assertEqual(found, expecting);
	}

	public void testTokenLabelReuse2() throws Exception {
		// check for compilation problem due to multiple defines
		String grammar =
			"grammar T;\n" +
			"options {output=AST;}\n" +
			"a : id=ID id=ID^ {System.out.print(\"2nd id=\"+$id.text+\";\");} ;\n" +
			"ID : 'a'..'z'+ ;\n" +
			"INT : '0'..'9'+;\n" +
			"WS : (' '|'\n') {channel=99;} ;\n";
		String found =
			TestCompileAndExecSupport.execParser("t.g", grammar, "T", "TLexer",
												 "a", "a b");
		String expecting = "2nd id=b;(b a)\n";
		assertEqual(found, expecting);
	}

	public void testTokenListLabelReuse() throws Exception {
		// check for compilation problem due to multiple defines
		// make sure ids has both ID tokens
		String grammar =
			"grammar T;\n" +
			"options {output=AST;}\n" +
			"a : ids+=ID ids+=ID {System.out.print(\"id list=\"+$ids+\";\");} ;\n" +
			"ID : 'a'..'z'+ ;\n" +
			"INT : '0'..'9'+;\n" +
			"WS : (' '|'\n') {channel=99;} ;\n";
		String found =
			TestCompileAndExecSupport.execParser("t.g", grammar, "T", "TLexer",
												 "a", "a b");
		String expecting = "id list=[[@0,0:0='a',<2>,1:0], [@2,2:2='b',<2>,1:2]];a b\n";
		assertEqual(found, expecting);
	}

	public void testTokenListLabelReuse2() throws Exception {
		// check for compilation problem due to multiple defines
		// make sure ids has both ID tokens
		String grammar =
			"grammar T;\n" +
			"options {output=AST;}\n" +
			"a : ids+=ID^ ids+=ID {System.out.print(\"id list=\"+$ids+\";\");} ;\n" +
			"ID : 'a'..'z'+ ;\n" +
			"INT : '0'..'9'+;\n" +
			"WS : (' '|'\n') {channel=99;} ;\n";
		String found =
			TestCompileAndExecSupport.execParser("t.g", grammar, "T", "TLexer",
												 "a", "a b");
		String expecting = "id list=[[@0,0:0='a',<2>,1:0], [@2,2:2='b',<2>,1:2]];(a b)\n";
		assertEqual(found, expecting);
	}

	public void testTokenListLabelRuleRoot() throws Exception {
		String grammar =
			"grammar T;\n" +
			"options {output=AST;}\n" +
			"a : id+=ID^^ ;\n" +
			"ID : 'a'..'z'+ ;\n" +
			"INT : '0'..'9'+;\n" +
			"WS : (' '|'\n') {channel=99;} ;\n";
		String found =
			TestCompileAndExecSupport.execParser("t.g", grammar, "T", "TLexer",
												 "a", "a");
		String expecting = "a\n";
		assertEqual(found, expecting);
	}

	public void testTokenListLabelBang() throws Exception {
		String grammar =
			"grammar T;\n" +
			"options {output=AST;}\n" +
			"a : id+=ID! ;\n" +
			"ID : 'a'..'z'+ ;\n" +
			"INT : '0'..'9'+;\n" +
			"WS : (' '|'\n') {channel=99;} ;\n";
		String found =
			TestCompileAndExecSupport.execParser("t.g", grammar, "T", "TLexer",
												 "a", "a");
		String expecting = "nil\n";
		assertEqual(found, expecting);
	}

	public void testRuleListLabel() throws Exception {
		String grammar =
			"grammar T;\n" +
			"options {output=AST;}\n" +
			"a : x+=b x+=b {System.out.print(\"2nd x=\"+((CommonTree)$x.get(1)).toStringTree()+\";\");} ;\n" +
			"b : ID;\n" +
			"ID : 'a'..'z'+ ;\n" +
			"INT : '0'..'9'+;\n" +
			"WS : (' '|'\n') {channel=99;} ;\n";
		String found =
			TestCompileAndExecSupport.execParser("t.g", grammar, "T", "TLexer",
												 "a", "a b");
		String expecting = "2nd x=b;a b\n";
		assertEqual(found, expecting);
	}

	public void testRuleListLabelRoot() throws Exception {
		String grammar =
			"grammar T;\n" +
			"options {output=AST;}\n" +
			"a : ( x+=b^ )+ {System.out.print(\"x=\"+((CommonTree)$x.get(1)).toStringTree()+\";\");} ;\n" +
			"b : ID;\n" +
			"ID : 'a'..'z'+ ;\n" +
			"INT : '0'..'9'+;\n" +
			"WS : (' '|'\n') {channel=99;} ;\n";
		String found =
			TestCompileAndExecSupport.execParser("t.g", grammar, "T", "TLexer",
												 "a", "a b");
		String expecting = "x=b;a b\n";
		assertEqual(found, expecting);
	}

	public void testRuleListLabelRuleRoot() throws Exception {
		String grammar =
			"grammar T;\n" +
			"options {output=AST;}\n" +
			"a : ( x+=b^^ )+ {System.out.print(\"x=\"+((CommonTree)$x.get(1)).toStringTree()+\";\");} ;\n" +
			"b : ID;\n" +
			"ID : 'a'..'z'+ ;\n" +
			"INT : '0'..'9'+;\n" +
			"WS : (' '|'\n') {channel=99;} ;\n";
		String found =
			TestCompileAndExecSupport.execParser("t.g", grammar, "T", "TLexer",
												 "a", "a b");
		String expecting = "x=(b a);(b a)\n";
		assertEqual(found, expecting);
	}

	public void testRuleListLabelBang() throws Exception {
		String grammar =
			"grammar T;\n" +
			"options {output=AST;}\n" +
			"a : x+=b! x+=b {System.out.print(\"x=\"+((CommonTree)$x.get(1)).toStringTree()+\";\");} ;\n" +
			"b : ID;\n" +
			"ID : 'a'..'z'+ ;\n" +
			"INT : '0'..'9'+;\n" +
			"WS : (' '|'\n') {channel=99;} ;\n";
		String found =
			TestCompileAndExecSupport.execParser("t.g", grammar, "T", "TLexer",
												 "a", "a b");
		String expecting = "x=b;a b\n";
		assertEqual(found, expecting);
	}

	public void testComplicatedMelange() throws Exception {
		// check for compilation problem
		String grammar =
			"grammar T;\n" +
			"options {output=AST;}\n" +
			"a : A b=B b=B c+=C c+=C D {$D.text;} ;\n" +
			"A : 'a' ;\n" +
			"B : 'b' ;\n" +
			"C : 'c' ;\n" +
			"D : 'd' ;\n" +
			"WS : (' '|'\n') {channel=99;} ;\n";
		String found =
			TestCompileAndExecSupport.execParser("t.g", grammar, "T", "TLexer",
												 "a", "a b b c c d");
		String expecting = "a b b c c d\n";
		assertEqual(found, expecting);
	}


	// S U P P O R T

	public void _test() throws Exception {
		String grammar =
			"grammar T;\n" +
			"options {output=AST;}\n" +
			"a :  ;\n" +
			"ID : 'a'..'z'+ ;\n" +
			"INT : '0'..'9'+;\n" +
			"WS : (' '|'\n') {channel=99;} ;\n";
		String found =
			TestCompileAndExecSupport.execParser("t.g", grammar, "T", "TLexer",
												 "a", "abc 34");
		String expecting = "\n";
		assertEqual(found, expecting);
	}

}

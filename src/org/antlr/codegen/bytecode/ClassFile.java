/*
 [The "BSD licence"]
 Copyright (c) 2004 Terence Parr
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
package org.antlr.codegen.bytecode;

import org.antlr.Tool;
import org.antlr.tool.ErrorManager;

import java.io.*;
import java.util.*;

/** This class knows how to write a restricted set of .class files
 *  I am writing bytecodes directly rather than Java source because I
 *  need gotos to avoid creating a DFA state object for each state in
 *  a DFA.  Slow to create, very big java source files, and slower to
 *  call virtual methods than jumping to a label.
 *
 *  This is like an "unparser" or template generator.  Andreas Rueckert
 *  built a nice Java class file parser for ANTLR; I made casual reference
 *  to that and a nice article by Bill Venners containing a "Getting Loaded"
 *  applet that illustrates .class file contents.  Naturally, the Java
 *  byte code documentation, javac, and <tt>hexdump</tt> were my main
 *  tools. ;)
 *
 *  I deviate from jasmin syntax in a few ways.  For example, my .method
 *  def has the maxStack value in it rather than using a separate instruction.
 *  Further, I use iconst for all sizes and then gen "ldc int" if necessary.
 *  I only allow single line ";..." comments; can't put a comment on end of
 *  an instruction.  Must be ';' in column 1 else conflicts with type names.
 *
 *  I don't intend this to be a general assembler mechanism as I do very
 *  little error checking.  I intend this to quickly and easily assemble
 *  bytecode assembly that is generated by another program; ANTLR, in this
 *  case.  This is about 1/5 the size of jasmin; like 1400 lines for
 *  ClassFile.java and MethodAssembler.java.  Not bad for a week's work, eh?
 *  No dependencies on foreign libraries. :)
 */
public class ClassFile {
    static final int MAGIC = 0xCAFEBABE;
    static final int VERSION = 0x0000002E;

	static final int PUBLIC_ACCESS = 0x0001;
	static final int STATIC_ACCESS = 0x0008;

    // Constant pool record identifiers
	static final int INTEGER = 0x03;
	static final int STRING = 0x08;
	static final int FIELD = 0x09;
    static final int METHOD = 0x0A;
    static final int INTERFACE_METHOD = 0x0B;
    static final int CLASS = 0x07;
    static final int UTF8 = 0x01;
    static final int NAME_AND_TYPE = 0x0C;

	private static final int INITIAL_LINES_PER_METHOD = 300;

	public static class Method {
		String name;
		String signature;
		List assemblyCodeLines;
		protected Map labels = new HashMap();
		int[] code;
		int maxStack = 0;
		int maxLocals = 2; // one local (int c) and one parameter (IntStream)
		public String toString() {
			return "method "+name+" "+signature;
		}
	}

	public static class ConstantPoolMethodDescriptor {
		String class_;
		String name;
		String signature;
		boolean interfaceMethod = false;
		public ConstantPoolMethodDescriptor(String class_,
											String name,
											String signature) {
			this.class_ = class_;
			this.name = name;
			this.signature = signature;
		}
	}

	public static class ConstantPoolInterfaceMethodDescriptor
		extends ConstantPoolMethodDescriptor
 	{
		public ConstantPoolInterfaceMethodDescriptor(String class_,
													 String name,
													 String signature) {
			super(class_,name,signature);
			interfaceMethod = true;
		}
	}

	public static class ConstantPoolFieldDescriptor {
		String class_;
		String name;
		String signature;
		public ConstantPoolFieldDescriptor(String class_,
										   String name,
										   String signature) {
			this.class_ = class_;
			this.name = name;
			this.signature = signature;
		}
	}

	protected String package_;
	protected String classFileName;
	protected String srcFileName;

	protected String className;
	protected String superClassName;
	protected String outputDirectory;

    protected DataOutputStream out;

	// track the list of all constant pool elements; order is important
	// as certain entries will refer to previous strings etc...

	protected List constantPoolClasses = new ArrayList();
	protected List constantPoolStrings = new ArrayList();
	protected List constantPoolMethods = new ArrayList();
	protected List constantPoolFields = new ArrayList();
	protected List constantPoolLiterals = new ArrayList();
	protected List constantPoolIntegers = new ArrayList();

    // want to be able to access constant pool later by name, string, ...
	// so, track item to it's index in a Map
	// These also prevent duplicate additions to constant pool

	protected Map stringToConstantPoolIndex = new HashMap();
	protected Map literalToConstantPoolIndex = new HashMap();
    protected Map classNameToConstantPoolIndex = new HashMap();
    protected Map methodNameToConstantPoolIndex = new HashMap();
	protected Map fieldNameToConstantPoolIndex = new HashMap();
	protected Map integerToConstantPoolIndex = new HashMap();

    /** Used to add elements to the constant pool while tracking
     *  their index so you can refer to it.
     */
    protected int poolIndex;

    /** Count how many constant pool entries so I can verify my
     *  hand computed number.
     */
    protected int poolCount = 0;

	/** entire assembly file full of methods */
	protected String assemblyCode;

	protected List dfaMethods = new LinkedList();

	public static void main(String[] args) throws IOException {
		String dfaAssemblyCode =
			".method <init> ()V 1\n" +
			"aload 0\n" +
			"invokespecial java/lang/Object.<init>()V\n"+
			"return\n"+
			".method DFA3 (Lorg/antlr/runtime/CharStream;)I 2\n" +
			"    iconst 0\n" +
			"    istore 1\n" +
			"    goto s0\n" +
			"errorState:\n" +
			"    getstatic java/lang/System.err Ljava/io/PrintStream;\n" +
			"        ldc \"DFA match error!\"\n" +
			"        invokevirtual java/io/PrintStream.println(Ljava/lang/String;)V\n" +
			"    iconst 0\n" +
			"    ireturn\n" +
			"s2:\n" +
			"    iconst 1\n" +
			"    ireturn\n" +
			"s3:\n" +
			"    iconst 2\n" +
			"    ireturn\n" +
			"s1:\n" +
			"    iconst 2\n" +
			"    ireturn\n" +
			"s0:\n" +
			"; load lookahead into a local in every state\n" +
			"    aload 0\n" +
			"    iconst 1\n" +
			"    invokeinterface org/antlr/runtime/CharStream.LA(I)I 2\n" +
			"    istore 1\n" +
			"s0e1:\n" +
			"    iload 1\n" +
			"    iconst 97\n" +
			"    if_icmpne s0e1_skip\n" +
			"s0e1_go:\n" +
			"    aload 0\n" +
			"    invokeinterface org/antlr/runtime/CharStream.consume()V 1\n" +
			"    goto s1\n" +
			"s0e1_skip:\n" +
			"s0e2:\n" +
			"    iload 1\n" +
			"    iconst 98\n" +
			"    if_icmpne s0e2_skip\n" +
			"s0e2_go:\n" +
			"    aload 0\n" +
			"    invokeinterface org/antlr/runtime/CharStream.consume()V 1\n" +
			"    goto s2\n" +
			"s0e2_skip:\n" +
			"s0e3:\n" +
			"    iload 1\n" +
			"    iconst 99\n" +
			"    if_icmpne s0e3_skip\n" +
			"s0e3_go:\n" +
			"    aload 0\n" +
			"    invokeinterface org/antlr/runtime/CharStream.consume()V 1\n" +
			"    goto s3\n" +
			"s0e3_skip:\n" +
			"    goto errorState\n"
		;
		dfaAssemblyCode =
			".method <init> ()V 1\n" +
			"aload 0\n" +
			"invokespecial java/lang/Object.<init>()V\n"+
			"return\n"+
			".method DFA3 (Lorg/antlr/runtime/CharStream;)I 2\n" +
			"back:" +
			"    iload 1\n" +
			"    iconst 48\n" +
			"    if_icmplt F\n" +
			"    iload 1\n" +
			"    iconst 57\n" +
			"    if_icmpgt F\n" +
			"    goto back\n" +
			"F:\n" +
			"\n" +
			"    return\n";
		ClassFile code = new ClassFile(null,
									   null,
									   "DFA",
									   "java/lang/Object",
									   "/tmp",
									   dfaAssemblyCode);
		code.write();
	}

	public ClassFile(Tool tool,
					 String package_,
					 String className,
					 String superClassName,
					 String outputDirectory,
					 String assemblyCode)
		throws IOException
	{
		this.package_ = package_;
		this.className = className;
		this.superClassName = superClassName;
		if ( package_!=null ) {
			this.className = package_+"/"+className;
		}
		this.outputDirectory = outputDirectory;
		classFileName = outputDirectory+"/"+className+".class";
		srcFileName = className+".java";
		this.assemblyCode = assemblyCode;
   }

	public void write() throws IOException {
		poolIndex = 0; // reset
		poolCount = 0;
 		predefinedConstantPoolElements();
		// pass 1 collects strings, finds methods, ...
		splitAssemblyIntoMethodsAndUpdateConstantPool();
		open();
		classFile();
		close();
	}

	/** Walk the assembly code, breaking up into lines.  Associate
	 *  lines with a particular Method object.
	 *
	 * 	Pull out any constants that need to go into the constant pool.  For
	 *  example:
	 *
	 *  .method name typesig 2
	 *  iconst anInt
	 *  ldc "a string"
	 *  invokevirtual java/io/PrintStream.println(LString;)V 2
	 *
	 *  would pull out name, typesig, anInt, "a string", and
	 *  java/io/PrintStream.
	 *
	 *  Strip blank lines and the comment lines here too.
     */
	protected void splitAssemblyIntoMethodsAndUpdateConstantPool() {
		if ( assemblyCode==null ) {
			return;
		}
		try {
			StringReader sr = new StringReader(assemblyCode);
			BufferedReader br = new BufferedReader(sr);
			List lines = null;
			// skip until first .method
			String line = br.readLine();
			while ( line!=null && !line.startsWith(".method ") ) {
				line = br.readLine();
			}
			while ( line!=null ) {
				// ignore blank lines and single-line comments
				if ( line.length()==0 || line.indexOf(';')==0 ) {
					line = br.readLine();
					continue;
				}
				String[] elements = MethodAssembler.getInstructionElements(line);
				String op = elements[0];
				if ( op.equals(".method") ) {
					Method method = new Method();
					methodDescriptor(line, method);
					lines = new ArrayList(INITIAL_LINES_PER_METHOD);
					method.assemblyCodeLines = lines;
					dfaMethods.add(method);
				}
				else {
					extractConstantPoolItems(elements);
				}
				lines.add(line);
				line = br.readLine();
			}
			br.close();
		}
		catch (IOException ioe) {
			// can't be called since strings aren't doing IO
			// better print a warning anyhoo
			ErrorManager.internalError("IO error in StringReader?", ioe);
		}
	}

	protected void open() throws IOException {
		FileOutputStream fout = new FileOutputStream(classFileName);
		BufferedOutputStream bufout = new BufferedOutputStream(fout);
		out = new DataOutputStream(bufout);
	}

    protected void close() throws IOException {
        out.close();
    }

    protected void classFile() throws IOException {
        magicNumber();
        versionNumber();
        constantPool();
        accessFlags();
        out.writeShort(indexOfClass(className));
        out.writeShort(indexOfClass("java/lang/Object"));
        out.writeShort(0x0000); // no interfaces
        out.writeShort(0x0000); // no fields
        methods();
        attributes();
    }

    protected void magicNumber() throws IOException {
        out.writeInt(MAGIC);
    }

    protected void versionNumber() throws IOException {
        out.writeInt(VERSION);
    }

    protected void predefinedConstantPoolElements() {
		constantPoolClasses.add(className);
		constantPoolClasses.add(superClassName);

		constantPoolStrings.add("Code");
		constantPoolStrings.add("SourceFile");
		constantPoolStrings.add(srcFileName);
	}

	protected void constantPool() throws IOException {
		ByteArrayOutputStream bout = new ByteArrayOutputStream(1024);
		DataOutputStream cpout = new DataOutputStream(bout);

        for (int i = 0; i < constantPoolClasses.size(); i++) {
			String cl = (String) constantPoolClasses.get(i);
			constantPoolClassDescriptor(cpout, cl);
		}

		for (int i = 0; i < constantPoolStrings.size(); i++) {
			String s = (String) constantPoolStrings.get(i);
			constantPoolString(cpout, s);
		}

        for (int i = 0; i < constantPoolMethods.size(); i++) {
			ConstantPoolMethodDescriptor m =
				(ConstantPoolMethodDescriptor) constantPoolMethods.get(i);
            if ( m.interfaceMethod ) {
				constantPoolInterfaceMethodDescriptor(cpout, m.class_,m.name, m.signature);
			}
			else {
				constantPoolMethodDescriptor(cpout, m.class_,m.name, m.signature);
			}
		}

		for (int i = 0; i < constantPoolFields.size(); i++) {
			ConstantPoolFieldDescriptor f =
				(ConstantPoolFieldDescriptor) constantPoolFields.get(i);
			constantPoolFieldDescriptor(cpout, f.class_,f.name,f.signature);
		}

		for (int i = 0; i < constantPoolLiterals.size(); i++) {
			String lit = (String) constantPoolLiterals.get(i);
			constantPoolLiteral(cpout, lit);
		}

		for (int i = 0; i < constantPoolIntegers.size(); i++) {
			Integer I = (Integer) constantPoolIntegers.get(i);
			constantPoolInteger(cpout, I.intValue());
		}

		cpout.close();
		byte[] constPool = bout.toByteArray();
		// now write to actual file now that we know how many constants
		// It's number + 1 according to spec
		int numConstants = poolCount;
        out.writeShort(numConstants+1);
		out.write(constPool);
		//System.out.println("wrote "+numConstants+" constants");
    }


    protected void accessFlags() throws IOException {
        out.writeShort(0x0031); // ACC_PUBLIC, ACC_SUPER, ACC_FINAL
    }

    protected void methods() throws IOException {
        out.writeShort(dfaMethods.size());

		for (int i = 0; i < dfaMethods.size(); i++) {
			Method m = (Method) dfaMethods.get(i);
			// now it's ok to assemble; constant pool is complete
			MethodAssembler assembler = new MethodAssembler(this, m);
			assembler.getCode();
			if ( m.name.equals("<init>") ) {
				ctor(m);
			}
			else {
				dfaEntryMethod(m);
			}
		}
    }

	protected void dfaEntryMethod(Method m) throws IOException {
		method(m, PUBLIC_ACCESS|STATIC_ACCESS); // public static access
	}

	protected void ctor(Method m) throws IOException {
		method(m, PUBLIC_ACCESS); // public access
	}

	protected void method(Method m, int access) throws IOException {
        out.writeShort(access);
        out.writeShort(indexOfString(m.name));
        out.writeShort(indexOfString(m.signature));
        out.writeShort(1); // 1 attribute: "Code"
        out.writeShort(indexOfString("Code"));
		int codeSize = m.code.length;
        out.writeInt(2+2+4+codeSize+2+2); // length of dfa1() code attribute
        out.writeShort(m.maxStack);
        out.writeShort(m.maxLocals);
        out.writeInt(codeSize); // length of dfa-entry() code block
        // here's actual code
        for (int i=0; i<codeSize; i++) {
            out.writeByte(m.code[i]);
        }
        out.writeShort(0); // exception table length
        out.writeShort(0); // additional info (line numbers)
    }

    protected void attributes() throws IOException {
        // just one attribute: e.g., SourceFile="DFA.java"
        out.writeShort(1);
        out.writeShort(indexOfString("SourceFile"));
        out.writeInt(2); // VM spec says this must be 2
        out.writeShort(indexOfString(srcFileName));
    }

    // C O N S T A N T  P O O L  S U P P O R T  C O D E

	/** A number of instructions reference static variables, methods, 32-bit
	 *  integers and so on that need to go in the constant pool.  Find all
	 *  these in this method.  This method is not particularly clean or clever,
	 *  but it should be clear. ;)
	 */
	protected void extractConstantPoolItems(String[] elements) {
		String op = elements[0];
		if ( op.equals("iconst") ) {
			int v = Integer.parseInt(elements[1]);
			if ( v>0 && v>0xFFFF ) {
				//System.out.println(v+" is too big for short");
				// must add to constant pool
				constantPoolIntegers.add(new Integer(v));
			}
		}
		else if ( op.equals("ldc") ) {
			if ( elements[1]!=null && elements[1].charAt(0)=='"' ) {
				String lit = elements[1].substring(1,elements[1].length());
				constantPoolLiterals.add(lit);
			}
		}
		else if ( op.equals("getstatic") ) {
			String fieldStr = elements[1];
			int dotIndex = fieldStr.indexOf('.');
			String class_ = fieldStr.substring(0,dotIndex);
			String name = fieldStr.substring(dotIndex+1,fieldStr.length());
			String sig = elements[2];
			ConstantPoolFieldDescriptor f =
				new ConstantPoolFieldDescriptor(class_,name,sig);
			constantPoolFields.add(f);
		}
		else if ( op.equals("new") ) {
			String className = elements[1];
			constantPoolClasses.add(className);
		}
		else if ( op.startsWith("invoke") ) {
			String methodStr = elements[1];
			int dotIndex = methodStr.indexOf('.');
			int lparenIndex = methodStr.indexOf('(');
			String class_ = methodStr.substring(0,dotIndex);
			String name = methodStr.substring(dotIndex+1,lparenIndex);
			String sig = methodStr.substring(lparenIndex,methodStr.length());
			if ( op.equals("invokeinterface") ) {
				ConstantPoolInterfaceMethodDescriptor m =
					new ConstantPoolInterfaceMethodDescriptor(
						class_, name, sig);
				constantPoolMethods.add(m);
			}
			else {
				ConstantPoolMethodDescriptor m =
					new ConstantPoolMethodDescriptor(
						class_, name, sig);
				constantPoolMethods.add(m);
			}
		}
	}

	/** Parse a method descriptor:
	 *  .method name signature maxStack maxLocals
	 */
	protected void methodDescriptor(String descriptor, Method method) {
		StringTokenizer st = new StringTokenizer(descriptor, " ", false);
		if ( !st.hasMoreTokens() ) {
			ErrorManager.error(ErrorManager.MSG_BYTECODE_MISSING_METHOD);
		}
		st.nextToken(); // skip .method
		if ( !st.hasMoreTokens() ) {
			ErrorManager.error(ErrorManager.MSG_BYTECODE_MISSING_METHOD_NAME);
		}
		method.name = st.nextToken();
		if ( !st.hasMoreTokens() ) {
			ErrorManager.error(ErrorManager.MSG_BYTECODE_MISSING_METHOD_SIG);
		}
		method.signature = st.nextToken();
		if ( !st.hasMoreTokens() ) {
			ErrorManager.error(ErrorManager.MSG_BYTECODE_MISSING_METHOD_MAXSTACK);
		}
		method.maxStack = Integer.parseInt(st.nextToken());
		if ( !st.hasMoreTokens() ) {
			ErrorManager.error(ErrorManager.MSG_BYTECODE_MISSING_METHOD_MAXLOCALS);
		}
		method.maxLocals = Integer.parseInt(st.nextToken());
		constantPoolStrings.add(method.name);
		constantPoolStrings.add(method.signature);
	}

	protected int constantPoolFieldDescriptor(DataOutputStream out,
											  String className,
											  String fieldName,
											  String sig)
		throws IOException
	{
		String fullyQualifiedFieldName = className+"."+fieldName;
		if ( indexOfField(fullyQualifiedFieldName)>0 ) {
			return indexOfField(fullyQualifiedFieldName);
		}
		int classIndex = constantPoolClassDescriptor(out,className);
		int sigIndex = constantPoolNameAndType(out,fieldName, sig);
		poolCount++;
		poolIndex++;
		out.writeByte(FIELD);
		out.writeShort(classIndex);
		out.writeShort(sigIndex);
		fieldNameToConstantPoolIndex.put(fullyQualifiedFieldName,
										 new Integer(poolIndex));
		return poolIndex;
	}

	protected int constantPoolNameAndType(DataOutputStream out,
										  String name,
										  String sig) throws IOException {
		int nameIndex = constantPoolString(out,name); // index of class name
		int sigIndex = constantPoolString(out,sig); // index of class name
		poolCount++;
		poolIndex++;
		out.writeByte(NAME_AND_TYPE);
		out.writeShort(nameIndex);
		out.writeShort(sigIndex);
		return poolIndex;
	}

	protected int constantPoolMethodDescriptor(DataOutputStream out,
											   String className,
											   String methodName,
											   String sig)
		throws IOException
	{
		String fullyQualifiedMethodName = className+"."+methodName+sig;
		if ( indexOfMethod(fullyQualifiedMethodName)>0 ) {
			return indexOfMethod(fullyQualifiedMethodName);
		}
		int classIndex = constantPoolClassDescriptor(out,className);
		int sigIndex = constantPoolNameAndType(out,methodName, sig);
		poolCount++;
		poolIndex++;
		out.writeByte(METHOD);
		out.writeShort(classIndex);
		out.writeShort(sigIndex);
		methodNameToConstantPoolIndex.put(fullyQualifiedMethodName, new Integer(poolIndex));
		return poolIndex;
	}

	protected int constantPoolInterfaceMethodDescriptor(
		DataOutputStream out,
		String className,
		String methodName,
		String sig)
		throws IOException
	{
		String fullyQualifiedMethodName = className+"."+methodName+sig;
		if ( indexOfMethod(fullyQualifiedMethodName)>0 ) {
			return indexOfMethod(fullyQualifiedMethodName);
		}
		int classIndex = constantPoolClassDescriptor(out,className);
		int sigIndex = constantPoolNameAndType(out,methodName, sig);
		poolCount++;
		poolIndex++;
		out.writeByte(INTERFACE_METHOD);
		out.writeShort(classIndex);
		out.writeShort(sigIndex);
		methodNameToConstantPoolIndex.put(fullyQualifiedMethodName, new Integer(poolIndex));
		return poolIndex;
	}

	protected int constantPoolClassDescriptor(DataOutputStream out,
											  String className) throws IOException {
		if ( indexOfClass(className)>0 ) {
			return indexOfClass(className);
		}
		int c = constantPoolString(out,className); // index of class name
		poolCount++;
		poolIndex++;
		out.writeByte(CLASS);
		out.writeShort(c);
		classNameToConstantPoolIndex.put(className,new Integer(poolIndex));
		return poolIndex; // return index of this CLASS
	}

	protected int constantPoolString(DataOutputStream out,
									 String s) throws IOException {
		// check first to see if string is there
		if ( indexOfString(s)>0 ) {
			return indexOfString(s);
		}
		poolCount++;
		poolIndex++;
		out.writeByte(UTF8);
		out.writeUTF(s);
		stringToConstantPoolIndex.put(s, new Integer(poolIndex));
		return poolIndex;
	}

	protected int constantPoolLiteral(DataOutputStream out,
									  String s) throws IOException {
		int i = constantPoolString(out,s);
		poolCount++;
		poolIndex++;
		out.writeByte(STRING);
		out.writeShort(i);
		literalToConstantPoolIndex.put(s, new Integer(poolIndex));
		return poolIndex;
	}

	protected int constantPoolInteger(DataOutputStream out,
									  int v) throws IOException {
		// check first to see if string is there
		if ( indexOfInteger(v)>0 ) {
			return indexOfInteger(v);
		}
		poolCount++;
		poolIndex++;
		out.writeByte(INTEGER);
		out.writeInt(v);
		integerToConstantPoolIndex.put(new Integer(v), new Integer(poolIndex));
		return poolIndex;
	}

	protected int indexOfString(String s) {
		Integer I = (Integer)stringToConstantPoolIndex.get(s);
		if ( I==null ) {
			return 0;
		}
		return I.intValue();
	}

	protected int indexOfLiteral(String s) {
		Integer I = (Integer)literalToConstantPoolIndex.get(s);
		if ( I==null ) {
			return 0;
		}
		return I.intValue();
	}

    protected int indexOfClass(String className) {
        Integer I = (Integer)classNameToConstantPoolIndex.get(className);
        if ( I==null ) {
            return 0;
        }
        return I.intValue();
    }

    protected int indexOfMethod(String methodName) {
        Integer I = (Integer)methodNameToConstantPoolIndex.get(methodName);
        if ( I==null ) {
            return 0;
        }
        return I.intValue();
    }

	protected int indexOfField(String fieldName) {
		Integer I = (Integer)fieldNameToConstantPoolIndex.get(fieldName);
		if ( I==null ) {
			return 0;
		}
		return I.intValue();
	}

	protected int indexOfInteger(int v) {
		Integer I = (Integer)integerToConstantPoolIndex.get(new Integer(v));
		if ( I==null ) {
			return 0;
		}
		return I.intValue();
	}

}

/**
 * File: IRGenerator.java
 * Pavlos Spanoudakis (sdi1800184)
 */

package Visitors;

import syntaxtree.*;
import visitor.*;
import ClassInformation.*;
import SymbolTable.*;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates the output {@code .ll} file out of the given MiniJava file.
 * Each {@code visit} method returns a {@code String}, which is
 * the name of the register where the evaluated value is stored,
 * or {@code null} if this is not needed.
 * <p>The second argument is also a {@code String} to be provided
 * to Identifier {@code visit} method, indicating whether an Identifier name, or
 * a variable address, or a variable value (which will be loaded to a new register) is required.
 */
public class IRGenerator extends GJDepthFirst<String, String> {

    /** The previously collected class names, fields & methods. */
    private Map<String, ClassInfo> classInfos;
    /** Used to indicate the class type of `this`, when referenced inside methods. */
    private ClassInfo currentClass;
    /** The local variables of each method are stored here,
     * and are removed after the end of the method body. */
    private SymbolTable symbolTable;
    /** Used to write to the output .ll file. */
    private FileWriter IRWriter;
    /** Used to generate new register names. */
    private int regCounter = -1;
    /** Used to generate new label names. */
    private int labelCounter = -1;
    /** Maps local register names (of objects) to class types, in order to figure out the vtable offset
     * when a method of the object in the register is called.
     * This is cleared after the function body has been generated. */
    private Map<String, ClassInfo> objectRegisters;

    public IRGenerator(String outputFileName, Map<String, ClassInfo>infos) throws IOException
    {
        classInfos = infos;
        symbolTable = new SymbolTable();
        IRWriter = new FileWriter(outputFileName, true);
        objectRegisters = new HashMap<String, ClassInfo>();
    }

    /**
     * Prints the virtual tables of all classes in the output {@code .ll} file.
     */
    public void emitVtables() throws IOException
    {
        // Iterate over the classes
        for(Map.Entry<String, ClassInfo>entry: this.classInfos.entrySet())
        {
            ClassInfo classInfo = entry.getValue();
            // Increase the field offsets +8 (vtable pointer will be stored at the beginning of the object)
            // This is irrelevant to the vtable creation, but needs to be done before IR for
            // method bodies is generated.
            classInfo.offsetIncrement();
            // Create virtual table
            classInfo.createVirtualTable();
            // Print it
            classInfo.emitVtable(this.IRWriter);
        }
        emit("\n");
    }

    /**
     * Prints all the contents of {@code ./Visitors/utils.ll}
     * in the output {@code .ll} file. This file should contain required functions
     * such as {@code print_int} and {@code throw_oob}, as well as needed imports.
     */
    public void emitUtils() throws FileNotFoundException, IOException
    {
        FileReader utilsReader = new FileReader("./Visitors/utils.ll");
        for (int c = utilsReader.read(); c != -1; c = utilsReader.read())
        {
            IRWriter.write(c);
        }
        IRWriter.write('\n');
        utilsReader.close();
    }

    /**
     * Prints the given string in the output {@code .ll} file (with a {@code "\t"} before
     * and a newline after it).
     */
    public void emit(String text) throws IOException
    {
        IRWriter.write("\t" + text + "\n");
    }

    /**
     * Prints the given string in the output {@code .ll} file, as a label start.
     */
    public void emitLabel(String text) throws IOException
    {
        IRWriter.write("\n" + text + ":" + "\n");
    }

    /**
     * Closes the writer to the output {@code .ll} file.
     */
    public void closeWriter() throws IOException
    {
        IRWriter.close();
    }

    /** Returns a new register name. */
    public String getNewRegister()
    {
        regCounter++;
        return "%_" + regCounter;
    }

    /** Returns a new label name, which includes the given string. */
    public String getNewLabel(String type)
    {
        labelCounter++;
        return type + labelCounter;
    }

    /**
     * class f1 -> Identifier() {
     * public static void main(String[], f11 -> Identifier(), ) {
     * f14 -> ( VarDeclaration() )*
     * f15 -> ( Statement() )*
     *     }
     * }
     */    
    @Override
    public String visit(MainClass n, String argu) throws Exception {
        this.symbolTable.addScope();
        IRWriter.write("define i32 @main() {\n");
        // VarDeclarations
        for (Node node: n.f14.nodes)
        {
            node.accept(this, null);
        }
        // Generate IR for main body
        for (Node node: n.f15.nodes)
        {
            node.accept(this, null);
        }
        // Return
        emit("ret i32 0");
        IRWriter.write("}\n");
        this.objectRegisters.clear();
        this.symbolTable.popScope();
        return null;
    }

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> ( VarDeclaration() )*
     * f4 -> ( MethodDeclaration() )*
     * f5 -> "}"
     */
    @Override
    public String visit(ClassDeclaration n, String argu) throws Exception {
        this.currentClass = this.classInfos.get(n.f1.accept(this, null));
        // Class fields are already stored, so just generate IR for the methods
        for (Node node: n.f4.nodes)
        {
            IRWriter.write('\n');
            node.accept(this, null);
        }
        return null;
    }

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "extends"
     * f3 -> Identifier()
     * f4 -> "{"
     * f5 -> ( VarDeclaration() )*
     * f6 -> ( MethodDeclaration() )*
     * f7 -> "}"
     */
    @Override
    public String visit(ClassExtendsDeclaration n, String argu) throws Exception {
        this.currentClass = this.classInfos.get(n.f1.accept(this, null));
        // Class fields (and superclass fields) are already stored, so just generate IR for the methods
        for (Node node: n.f6.nodes)
        {
            IRWriter.write('\n');
            node.accept(this, null);
        }
        return null;
    }

    /**
     * f0 -> "public"
     * f1 -> Type()
     * f2 -> Identifier()
     * f3 -> "("
     * f4 -> ( FormalParameterList() )?
     * f5 -> ")"
     * f6 -> "{"
     * f7 -> ( VarDeclaration() )*
     * f8 -> ( Statement() )*
     * f9 -> "return"
     * f10 -> Expression()
     * f11 -> ";"
     * f12 -> "}"
     */
    @Override
    public String visit(MethodDeclaration n, String argu) throws Exception {
        this.symbolTable.addScope();
        // Get method type and name
        String type = VariableInfo.getIRType(n.f1.accept(this, null));
        String name = n.f2.accept(this, null);
        // Printing method signature
        IRWriter.write("define " + type + " @" + this.currentClass.name + "." + name);
        // Printing parameter list
        // `this` is always first
        IRWriter.write("(i8* %this");
        // Printing the rest of the parameters
        if (n.f4.present())
        {
            // Split parameters string into an array.
            // Each element is a "<type> <name>" string.
            String[] argStr = n.f4.accept(this, null).split("\\s*,\\s*");
            // Store the created variables here
            List<VariableInfo> args = new ArrayList<VariableInfo>();
            for(String arg: argStr)
            {
                IRWriter.write(", ");
                // Split the parameter string into <type>[0] and <name>[1] strings
                String[] splitArg = arg.split("\\s* \\s*");
                // Create new variable for this parameter, store it and print the parameter
                args.add(new VariableInfo(splitArg[1], splitArg[0]));
                IRWriter.write(VariableInfo.getIRType(splitArg[0]) + " %." + splitArg[1]);
            }
            // No more parameters
            IRWriter.write(") {\n");
            for(VariableInfo arg: args)
            {
                // Allocate stack space for each parameter and store its value
                arg.register = "%" + arg.name;
                emit(arg.register + " = alloca " + arg.IRType);
                emit("store " + arg.IRType + " %." + arg.name + ", " + arg.IRType + "* " + arg.register);
                this.symbolTable.pushVariableEntry(arg);
            }
        }
        else
        {
            IRWriter.write(") {\n");
        }
        // Go over local variable declarations
        for (Node node: n.f7.nodes)
        {
            node.accept(this, null);
        }
        // Body
        for (Node node: n.f8.nodes)
        {
            node.accept(this, null);
        }
        // Get return expression value
        String retExpr = n.f10.accept(this, "rvalue");
        emit("ret " + type + " " + retExpr);
        IRWriter.write("}\n");
        this.symbolTable.popScope();
        // Clear local register-class mappings
        this.objectRegisters.clear();
        return null;
    }

    /**
     * f0 -> FormalParameter()
     * f1 -> FormalParameterTail()
     */
    @Override
    public String visit(FormalParameterList n, String argu) throws Exception {
        String ret = n.f0.accept(this, null);
        
        if (n.f1 != null) {
            ret += n.f1.accept(this, null);
        }
        
        return ret;
    }

    /**
     * Grammar production:
     * f0 -> Type()
     * f1 -> Identifier()
     */
    @Override
    public String visit(FormalParameter n, String argu) throws Exception {
        // Return "<type> + <name>"
        return n.f0.accept(this, null) + " " + n.f1.accept(this, null);
    }

    /**
     * f0 -> ( FormalParameterTerm() )*
     */
    @Override
    public String visit(FormalParameterTail n, String argu) throws Exception {
        String params = "";
        // Return the parameter strings concatenated, and splitted by ','
        for ( Node node: n.f0.nodes) {
            params += "," + node.accept(this, null);
        }
        return params;
    }

    /**
     * Grammar production:
     * f0 -> ","
     * f1 -> FormalParameter()
     */
    @Override
    public String visit(FormalParameterTerm n, String argu) throws Exception {
        return n.f1.accept(this, null);
    }

    /**
     * f0 -> "("
     * f1 -> Expression()
     * f2 -> ")"
     */
    @Override
    public String visit(BracketExpression n, String argu) throws Exception {
        return n.f1.accept(this, argu);
    }

    /**
     * f0 -> AndExpression()
     *       | CompareExpression()
     *       | PlusExpression()
     *       | MinusExpression()
     *       | TimesExpression()
     *       | ArrayLookup()
     *       | ArrayLength()
     *       | MessageSend()
     *       | PrimaryExpression()
     */
    @Override
    public String visit(Expression n, String argu) throws Exception {
        return n.f0.accept(this, argu);
    }

    /**
     * f0 -> IntegerLiteral()
     *       | TrueLiteral()
     *       | FalseLiteral()
     *       | Identifier()
     *       | ThisExpression()
     *       | ArrayAllocationExpression()
     *       | AllocationExpression()
     *       | NotExpression()
     *       | BracketExpression()
     */
    @Override
    public String visit(PrimaryExpression n, String argu) throws Exception {
        return n.f0.accept(this, argu);
    }

    /**
     * f0 -> "new"
     * f1 -> Identifier()
     * f2 -> "("
     * f3 -> ")"
     */
    @Override
    public String visit(AllocationExpression n, String argu) throws Exception {
        // Get Object class
        ClassInfo classInfo = this.classInfos.get(n.f1.accept(this, null));
        int vtableSize = classInfo.getVtableNumEntries();
        // Get required memory for the object of this class
        String allocated = getNewRegister();
        emit(allocated + " = call i8* @calloc(i32 1, i32 " + classInfo.getObjectSize() + ")");
        String casted = getNewRegister();
        emit(casted + " = bitcast i8* " + allocated + " to i8***");
        // Get address of the vtable of this class
        String vtable = getNewRegister();
        String line = vtable + " = getelementptr [" + vtableSize + " x i8*], [" + vtableSize + " x i8*]* ";
        line = line + "@." + classInfo.name + "_vtable, i32 0, i32 0";
        emit(line);
        // Store the address at the beginning of the object
        emit("store i8** " + vtable + ", i8*** " + casted);
        // Map the object register to this class
        this.objectRegisters.put(allocated, classInfo);
        // Return the object register
        return allocated;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "."
     * f2 -> Identifier()
     * f3 -> "("
     * f4 -> ( ExpressionList() )?
     * f5 -> ")"
     */
    @Override
    public String visit(MessageSend n, String argu) throws Exception {
        // Get Object register
        String object = n.f0.accept(this, "rvalue");
        // Get Method name
        String name = n.f2.accept(this, null);
        String type;
        ClassInfo objectClass;
        if (object.equals("%this"))
        {
            // Use current class
            objectClass = this.currentClass;
        }
        else
        {
            // Get Class type for the object register from register-class map
            // AllocationExpression, VarDeclaration and MessageSend (if an object is returned)
            // may store <String(reg), String(class)> pairs there
            objectClass = this.objectRegisters.get(object);
        }
        // Get Method from the class vtable. We just need the offset; the vtable were
        // the object actually points to may not be this class' vtable (if the object is
        // actually of a subclass type), but the method offset will still be the same.
        MethodInfo method = objectClass.getVtableMethod(name);
        type = VariableInfo.getIRType(method.method.type);

        // Cast vtable pointer properly
        String castObj = getNewRegister();
        emit(castObj + " = bitcast i8* " + object + " to i8***");
        // Get vtable start
        String vtable = getNewRegister();
        emit(vtable + " = load i8**, i8*** " + castObj);
        // Get method from its position in the vtable
        String methodPtr = getNewRegister();
        emit(methodPtr + " = getelementptr i8*, i8** " + vtable + ", i32 " + method.offset / 8);
        // Get method address
        String methodRaw = getNewRegister();
        emit(methodRaw + " = load i8*, i8** " + methodPtr);
        String methodReg = getNewRegister();
        // Create signature string for bitcast
        String signature = method.method.getIRsignature();
        // Get a "callable" register for this method
        emit(methodReg + " = bitcast i8* " + methodRaw + " to " + signature + "*");

        // Constructing method call
        String retVal = getNewRegister();
        String call = retVal + " = call " + type + " " + methodReg;

        call = call + "(i8* " + object;
        // Construct argument (registers) string
        if (n.f4.present())
        {
            String[] argRegisters = n.f4.accept(this, null).split("\\s*,\\s*");
            for (int i = 0; i < argRegisters.length; i++) {
                call = call + ", " + VariableInfo.getIRType(method.method.args[i]) + " " + argRegisters[i];
            }
        }
        call = call + ")";
        // Call the method
        emit(call);
        if( !VariableInfo.isPrimitiveType(method.method.type) )
        // If the method returns an object, map the return value register to the object class
        {
            this.objectRegisters.put(retVal, this.classInfos.get(method.method.type));
        }
        return retVal;
    }

    /**
     * f0 -> Expression()
     * f1 -> ExpressionTail()
     */
    @Override
    public String visit(ExpressionList n, String argu) throws Exception {
        // Return the expression registers seperated by ','
        return n.f0.accept(this, "rvalue") + n.f1.accept(this, null);
    }

    /**
     * f0 -> ( ExpressionTerm() )*
     */
    @Override
    public String visit(ExpressionTail n, String argu) throws Exception {
        // Return the expression registers seperated by ','
        String retStr = "";
        for (Node node: n.f0.nodes)
        {
            retStr = retStr + "," + node.accept(this, null);
        }
        return retStr;
    }

    /**
     * f0 -> ","
     * f1 -> Expression()
     */
    @Override
    public String visit(ExpressionTerm n, String argu) throws Exception {
        // return the register where the expression evaluated value has been stored
        return n.f1.accept(this, "rvalue");
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "."
     * f2 -> "length"
     */
    @Override
    public String visit(ArrayLength n, String argu) throws Exception {
        // Get the address of first element in the array
        String arrayStart = n.f0.accept(this, "rvalue");
        // Get value of int in the Array Start, the size
        String arraySize = getNewRegister();
        emit(arraySize + " = load i32, i32* " + arrayStart);
        return arraySize;
    }

    /**
     * f0 -> "new"
     * f1 -> "int"
     * f2 -> "["
     * f3 -> Expression()
     * f4 -> "]"
     */
    @Override
    public String visit(ArrayAllocationExpression n, String argu) throws Exception {
        // Evaluate size
        String size = n.f3.accept(this, "rvalue");
        String isNegativeReg = getNewRegister();
        // Check if it is < 0
        emit(isNegativeReg + " = icmp slt i32 " + size + ", 0" );
        String validArraySize = getNewLabel("validArraySize");
        String invalidArraySize = getNewLabel("invalidArraySize");
        // If so, throw OOB, else continue
        emit("br i1 " + isNegativeReg + ", label %" + invalidArraySize + ", label %" + validArraySize);
        // Throw OOB
        emitLabel(invalidArraySize);
        emit("call void @throw_oob()");
        emit("br label %" + validArraySize);
        // Continue
        emitLabel(validArraySize);
        String fullSizeReg = getNewRegister();
        // + 1 size (size will be stored before the elements)
        emit(fullSizeReg + " = add i32 " + size + ", 1");
        // Allocate memory
        String callocReg = getNewRegister();
        emit(callocReg + " = call i8* @calloc(i32 4, i32 " + fullSizeReg + ")");
        // Store size (number of elements) in the beginning
        String arrayStart = getNewRegister();
        emit(arrayStart + " = bitcast i8* " + callocReg + " to i32*");
        emit("store i32 " + size + ", i32* " + arrayStart);
        return arrayStart;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "["
     * f2 -> PrimaryExpression()
     * f3 -> "]"
     */
    @Override
    public String visit(ArrayLookup n, String argu) throws Exception {
        // Get and store index value
        String index = n.f2.accept(this, "rvalue");
        String invalidIndex = getNewLabel("arrayInvalidIndex");
        String getArray = getNewLabel("getArray");
        String indexNegative = getNewRegister();
        // Check if index is negative
        emit(indexNegative + " = icmp slt i32 " + index + ", 0");
        // If so, goto invalidIndex label (throw OOB), else continue
        emit("br i1 " + indexNegative + ", label %" + invalidIndex + ", label %" + getArray);

        emitLabel(getArray);
        // Get the address of the array
        String arrayStart = n.f0.accept(this, "rvalue");
        // Get value of int in the Array Start (the size)
        String arraySize = getNewRegister();
        emit(arraySize + " = load i32, i32* " + arrayStart);

        String indexCheck = getNewRegister();        
        // Check if the array size is less or equal to index
        emit(indexCheck + " = icmp ule i32 " + arraySize + ", " + index);
        String validIndex = getNewLabel("arrayValidIndex");        
        // If so, goto invalidIndex label, else to validIndexLabel
        emit("br i1 " + indexCheck + ", label %" + invalidIndex + ", label %" + validIndex);
        
        // invalidIndex, throw OOB
        emitLabel(invalidIndex);
        emit("call void @throw_oob()");
        emit("br label %" + validIndex);

        // validIndex      
        emitLabel(validIndex);
        // The real index is + 1, since the array size is actually element 0
        String realIndex = getNewRegister();
        emit(realIndex + " = add i32 " + index + ", 1");
        // Get address of element with the given index
        String elementAddress = getNewRegister();
        emit(elementAddress + " = getelementptr i32, i32* " + arrayStart +", i32 " + realIndex);
        // Load element value to a new register
        String elementValue = getNewRegister();
        emit(elementValue + " = load i32, i32* " + elementAddress);
        // Return the new register with the element value
        return elementValue;
    }

    /**
     * f0 -> Identifier()
     * f1 -> "["
     * f2 -> Expression()
     * f3 -> "]"
     * f4 -> "="
     * f5 -> Expression()
     * f6 -> ";"
     */
    @Override
    public String visit(ArrayAssignmentStatement n, String argu) throws Exception {
        // Get and store index value
        String index = n.f2.accept(this, "rvalue");
        String invalidIndex = getNewLabel("arrayInvalidIndex");
        String loadArray = getNewLabel("loadArray");
        String indexNegative = getNewRegister();
        // Check if index is negative
        emit(indexNegative + " = icmp slt i32 " + index + ", 0");
        // If so, goto invalidIndex label (throw OOB), else continue (load the array)
        emit("br i1 " + indexNegative + ", label %" + invalidIndex + ", label %" + loadArray);

        emitLabel(loadArray);
        // Get the register that points to the array
        String arrayAddress = n.f0.accept(this, "lvalue");
        // Get Array Start address
        String arrayStart = getNewRegister();
        emit(arrayStart + " = load i32*, i32** " + arrayAddress);
        // Get value of int in the Array Start (the size)
        String arraySize = getNewRegister();
        emit(arraySize + " = load i32, i32* " + arrayStart);

        String indexCheck = getNewRegister();
        // Check if the array size is less or equal to index
        emit(indexCheck + " = icmp ule i32 " + arraySize + ", " + index);
        String validIndex = getNewLabel("arrayValidIndex");
        // If so, goto invalidIndex label, else to validIndexLabel
        emit("br i1 " + indexCheck + ", label %" + invalidIndex + ", label %" + validIndex);

        // invalidIndex, throw OOB
        emitLabel(invalidIndex);
        emit("call void @throw_oob()");
        emit("br label %" + validIndex);

        // validIndex
        emitLabel(validIndex);
        // The real index is + 1, since the array size is actually element 0
        String realIndex = getNewRegister();
        emit(realIndex + " = add i32 " + index + ", 1");
        // Get address of element with the given index
        String elementAddress = getNewRegister();
        emit(elementAddress + " = getelementptr i32, i32* " + arrayStart +", i32 " + realIndex);
        // Evaluate rvalue expression value
        String rvalue = n.f5.accept(this, "rvalue");
        // Store value in the array element
        emit("store i32 " + rvalue +", i32* " + elementAddress);
        return null;
    }

    /**
     * f0 -> Identifier()
     * f1 -> "="
     * f2 -> Expression()
     * f3 -> ";"
     */
    @Override
    public String visit(AssignmentStatement n, String argu) throws Exception {
        // Get address of lvalue variable
        String lvalue = n.f0.accept(this, "lvalue");
        // get value of rvalue expression
        String rvalue = n.f2.accept(this, "rvalue");
        // Get lvalue name as well
        String name = n.f0.accept(this, null);
        // Find variable in symbol table
        VariableInfo var = this.symbolTable.getCurrentScopeVariable(name);
        // Get IR type
        String type;
        if (var == null)
        // Not a local variable, so it is a field
        {
            type = this.currentClass.getFieldRec(name).field.IRType;
        }
        else
        // Local variable
        {
            type = var.IRType;
        }
        // Store rvalue to lvalue
        emit("store " + type + " " + rvalue + ", " + type + "* " + lvalue);
        return null;
    }

    @Override
    public String visit(Identifier n, String whatValue) throws Exception {
        // Get identifier name
        String name = n.f0.toString();
        if (whatValue != null)
        // A register is requested
        {
            // Get local variable with this name
            VariableInfo var = this.symbolTable.getCurrentScopeVariable(name);
            if (var != null)
            // Found
            {
                if (whatValue.equals("rvalue"))
                // Variable value needs to be loaded and returned
                {
                    // Load value
                    String loadedVar = getNewRegister();
                    emit(loadedVar + " = load " + var.IRType + ", " + var.IRType + "* " + var.register);
                    if ( !VariableInfo.isPrimitiveType(var.typeName) )
                    // Variable is an object
                    {
                        // So map the new register to the object class
                        this.objectRegisters.put(loadedVar, classInfos.get(var.typeName));
                    }
                    return loadedVar;
                }
                // lvalue given, no need to load
                return var.register;
            }
            // No local variable with that name found, so the identifier represents a field of `this`
            // Get the field
            FieldInfo classField = this.currentClass.getFieldRec(name);
            int offset = classField.offset;
            // Get field address in object space
            String fieldRegister = getNewRegister();
            emit(fieldRegister + " = getelementptr i8, i8* %this, i32 " + offset);
            String type = classField.field.IRType;
            String castFieldReg = getNewRegister();
            // Cast field to its IR type
            emit(castFieldReg + " = bitcast i8* " + fieldRegister + " to " + type + "*");

            if (whatValue.equals("rvalue"))
            // The field value is needed
            {
                // load field value in new register
                String loadedField = getNewRegister();
                emit(loadedField + " = load " + type + ", " + type + "* " + castFieldReg);
                if (!VariableInfo.isPrimitiveType(classField.field.typeName))
                // Field is an object
                {
                    // So map the new register to the field class
                    this.objectRegisters.put(loadedField, this.classInfos.get(classField.field.typeName));
                }
                return loadedField;
            }
            // lvalue, no need to load field value
            return castFieldReg;
        }
        // just return the name
        return name;
    }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
     * f2 -> ";"
     * 
     * Note that this is called for local variable declaration only.
     */
    @Override
    public String visit(VarDeclaration n, String argu) throws Exception {
        // Get type/name
        String type = n.f0.accept(this, null);
        String name = n.f1.accept(this, null);
        String register = "%" + name;
        String llvmType = VariableInfo.getIRType(type);
        // Allocate space in the stack
        emit(register + " = alloca " + llvmType);
        // Add the local variable in the symbol table
        this.symbolTable.pushVariableEntry(new VariableInfo(name, type, register) );
        if ( !VariableInfo.isPrimitiveType(type) )
        // The local variable is an object, so map its register to the class
        {
            this.objectRegisters.put(register, this.classInfos.get(type));
        }
        return register;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "&&"
     * f2 -> PrimaryExpression()
     */
    @Override
    public String visit(AndExpression n, String argu) throws Exception {
        String resultReg = getNewRegister();
        // Evaluate expr1
        String exp1 = n.f0.accept(this, "rvalue");
        String exp2Label = getNewLabel("andExp2_");
        String trueLabel = getNewLabel("andTrue");
        String falseLabel = getNewLabel("andFalse");
        String resultLabel = getNewLabel("andResult");
        // goto False if expr1 == 0, else continue to expr2
        emit("br i1 " + exp1 + ", label %" + exp2Label + ", label %" + falseLabel);
        // Evaluate expr2
        emitLabel(exp2Label);
        String exp2 = n.f2.accept(this, "rvalue");
        // False if expr2 == 0, else goto True
        emit("br i1 " + exp2 + ", label %" + trueLabel + ", label %" + falseLabel);
        // True
        emitLabel(trueLabel);
        emit("br label %" + resultLabel);
        // False
        emitLabel(falseLabel);
        emit("br label %" + resultLabel);
        // Result
        emitLabel(resultLabel);
        emit(resultReg + " = phi i1 [" + 1 + ", %" + trueLabel + "], [" + 0 + ", %" + falseLabel + "]");
        return resultReg;
    }

    /**
     * f0 -> "while"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> Statement()
     */
    @Override
    public String visit(WhileStatement n, String argu) throws Exception {
        String check = getNewLabel("whileCheck");
        String body = getNewLabel("whileBody");
        String exit = getNewLabel("whileExit");
        // Condition check
        emit("br label %" + check);
        emitLabel(check);
        // Condition expression evaluation
        String condition = n.f2.accept(this, "rvalue");
        emit("br i1 " + condition + ", label %" + body + ", label %" + exit);
        // Loop body
        emitLabel(body);
        n.f4.accept(this, null);
        // Check condition again
        emit("br label %" + check);
        // Exit loop
        emitLabel(exit);
        return null;
    }

    /**
     * f0 -> "if"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> Statement()
     * f5 -> "else"
     * f6 -> Statement()
     */
    @Override
    public String visit(IfStatement n, String argu) throws Exception {
        // Evaluate expression
        String expResult = n.f2.accept(this, "rvalue");
        String trueLabel = getNewLabel("ifTrue");
        String falseLabel = getNewLabel("ifFalse");
        String end = getNewLabel("ifEnd");
        // Check if expression is true/false
        emit("br i1 " + expResult + ", label %" + trueLabel + ", label %" + falseLabel);
        // True
        emitLabel(trueLabel);
        // Statement
        n.f4.accept(this, null);
        // Skip false
        emit("br label %" + end);
        // False
        emitLabel(falseLabel);
        // Statement
        n.f6.accept(this, null);
        // Exit
        emit("br label %" + end);
        emitLabel(end);
        return null;
    }

    /**
     * Grammar production:
     * f0 -> PrimaryExpression()
     * f1 -> "<"
     * f2 -> PrimaryExpression()
     */
    @Override
    public String visit(CompareExpression n, String argu) throws Exception {
        String exp1 = n.f0.accept(this, "rvalue");
        String exp2 = n.f2.accept(this, "rvalue");
        String result = getNewRegister();
        emit(result + " = icmp slt i32 " + exp1 + ", " + exp2);
        return result;
    }

    /**
     * Grammar production:
     * f0 -> "!"
     * f1 -> PrimaryExpression()
     */
    @Override
    public String visit(NotExpression n, String argu) throws Exception {
        String exp = n.f1.accept(this, "rvalue");
        String result = getNewRegister();
        emit(result + " = icmp eq i1 0, " + exp);
        return result;
    }

    @Override
    public String visit(PrintStatement n, String argu) throws Exception {
        String expRegister = n.f2.accept(this, "rvalue");
        emit("call void (i32) @print_int(i32 " + expRegister + ")");
        return null;
    }

    @Override
    public String visit(ThisExpression n, String argu) throws Exception {
        return "%this";
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "+"
     * f2 -> PrimaryExpression()
     */
    @Override
    public String visit(PlusExpression n, String argu) throws Exception {
        String expr1 = n.f0.accept(this, "rvalue");
        String expr2 = n.f2.accept(this, "rvalue");
        String result = getNewRegister();
        emit(result + " = add i32 " + expr1 + ", " + expr2);
        return result;
    }
    @Override
    public String visit(MinusExpression n, String argu) throws Exception {
        String expr1 = n.f0.accept(this, "rvalue");
        String expr2 = n.f2.accept(this, "rvalue");
        String result = getNewRegister();
        emit(result + " = sub i32 " + expr1 + ", " + expr2);
        return result;
    }

    @Override
    public String visit(TimesExpression n, String argu) throws Exception {
        String expr1 = n.f0.accept(this, "rvalue");
        String expr2 = n.f2.accept(this, "rvalue");
        String result = getNewRegister();
        emit(result + " = mul i32 " + expr1 + ", " + expr2);
        return result;
    }

    /**
     * f0 -> ArrayType()
     *       | BooleanType()
     *       | IntegerType()
     *       | Identifier()
     */
    @Override
    public String visit(Type n, String argu) throws Exception {
        return n.f0.accept(this, null);
    }

    @Override
    public String visit(ArrayType n, String argu) throws Exception {
        return "int[]";
    }

    @Override
    public String visit(IntegerType n, String argu) throws Exception {
        return "int";
    }

    @Override
    public String visit(BooleanType n, String argu) throws Exception {
        return "boolean";
    }

    @Override
    public String visit(IntegerLiteral n, String argu) throws Exception {
        return n.f0.toString();
    }

    @Override
    public String visit(FalseLiteral n, String argu) throws Exception {
        return "0";
    }

    @Override
    public String visit(TrueLiteral n, String argu) throws Exception {
        return "1";
    }
}

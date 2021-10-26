/**
 * File: FunctionBodyAnalyzer.java
 * Pavlos Spanoudakis (sdi1800184)
 */

package Visitors;

import syntaxtree.*;
import visitor.*;
import ClassInformation.*;
import SymbolTable.*;

import java.util.Map;

/**
 * Analyzes the method bodies.
 * 
 * Each {@code visit} method returns a String that indicates the
 * type of the respective statement, or {@code null} if this is not needed.
 */
public class FunctionBodyAnalyzer extends GJDepthFirst<String, Boolean> {
    // The previously collected class names, fields & methods.
    Map<String, ClassInfo> classInfos;
    
    SymbolTable symbolTable;
    // This will be used in class methods, for instant access to current class fields/methods.
    ClassInfo currentClass;

    public FunctionBodyAnalyzer(Map<String, ClassInfo> classInfoMap) {
        classInfos = classInfoMap;
        symbolTable = new SymbolTable();
        currentClass = null;
    }

    public void printOffsets()
    {
        for (Map.Entry<String, ClassInfo> classInfo: classInfos.entrySet())
        {
            classInfo.getValue().printOffsets();
        }
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
    public String visit(MainClass n, Boolean argu) throws Exception {
        this.symbolTable.addScope();

        // Adding main argument to the scope, to avoid redeclaration
        this.symbolTable.pushVariableEntry(new VariableInfo(n.f11.accept(this, null), "String[]"));

        String[] varStr;

        // Iterate over the variable declarations and push the variables in the Symbol Table
        for (Node node: n.f14.nodes)
        {
            // Get a "<type> <name>" string" for each declaration
            varStr = node.accept(this, null).split("\\s* \\s*");
            if ( !this.symbolTable.pushVariableEntry(new VariableInfo(varStr[1], varStr[0])) )
            {
                throw new SemanticError("Variable '" + varStr[1] + "' in function 'main' is declared more than once");
            }
        }
        // Analyze all statements in main
        for (Node node: n.f15.nodes)
        {
            node.accept(this, null);
        }
        // Pop all variables declared above from the Symbol Table Stack
        this.symbolTable.popScope();
        return null;
    }

    @Override
    public String visit(VarDeclaration n, Boolean classInfo) throws Exception {
        String type = n.f0.accept(this, null);
        String name = n.f1.accept(this, null);

        // Checking if field type is valid
        if ( !VariableInfo.isPrimitiveType(type) && (this.classInfos.get(type) == null))
        {
            throw new SemanticError("Line " + n.f2.beginLine + ": Variable '" + name + "' has invalid type '" + type + "'");
        }

        return type + " " + name;
    }

    @Override
    public String visit(Block n, Boolean argu) throws Exception {
        
        for (Node node: n.f1.nodes)
        {
            node.accept(this, null);
        }
        return null;
    }

    /**
     * Grammar production:
     * f0 -> Identifier()
     * f1 -> "="
     * f2 -> Expression()
     * f3 -> ";"
     */    
    @Override
    public String visit(AssignmentStatement n, Boolean argu) throws Exception {
        String varType = n.f0.accept(this, true);
        String exprType = n.f2.accept(this, null);

        // If identifier and expression types are equal, the assignment is valid
        if (varType.equals(exprType))
        {
            return varType;
        }

        // If identifier and expression types are not equal, then
        // identifier type can only by a superclass of expression type.
        if ( !VariableInfo.isPrimitiveType(varType) )
        {
            ClassInfo exprClass = this.classInfos.get(exprType);
            if ( exprClass != null && exprClass.hasSuperClass(varType))
            {
                return varType;
            }
        }        

        throw new SemanticError("Line " + n.f1.beginLine + ": Type mismatch: cannot convert from '" + exprType + "' to '" + varType + "'");        
    }

    /**
     * Grammar production:
     * f0 -> IntegerLiteral()               // returns "int"
     *       | TrueLiteral()                // returns "boolean"
     *       | FalseLiteral()               // returns "boolean"
     *       | Identifier()                 // returns identifier (var) type
     *       | ThisExpression()             // returns current class
     *       | ArrayAllocationExpression()  // returns array element type
     *       | AllocationExpression()       // returns type
     *       | NotExpression()              // returns "boolean"
     *       | BracketExpression()          //  returns one of the above
     */
    @Override
    public String visit(PrimaryExpression n, Boolean argu) throws Exception {
        return n.f0.accept(this, true);
    }

    @Override
    public String visit(ArrayAllocationExpression n, Boolean argu) throws Exception {
        String indexExprType = n.f3.accept(this, null);
        // Index can only be of type int
        if ( !indexExprType.equals("int") )
        {
            throw new SemanticError("Line " + n.f0.beginLine + ": Cannot convert from '" + indexExprType + "' to 'int'");
        }
        return "int[]";
    }

    @Override
    public String visit(AllocationExpression n, Boolean argu) throws Exception {
        String constructorName = n.f1.accept(this, null);
        // Making sure this is a known Class constructor
        if (this.classInfos.get(constructorName) == null)
        {
            throw new SemanticError("Line " + n.f0.beginLine + ": '" + constructorName + "' cannot be resolved to a type");
        }
        return constructorName;
    }

    /**
     * Grammar production:
     * f0 -> AndExpression()            // returns "boolean"
     *       | CompareExpression()      // returns "boolean"
     *       | PlusExpression()         // returns "int"
     *       | MinusExpression()        // returns "int"
     *       | TimesExpression()        // returns "int"
     *       | ArrayLookup()            // returns array element type
     *       | ArrayLength()            // returns "int"
     *       | MessageSend()            // returns return type
     *       | PrimaryExpression()      // returns type
     */    
    @Override
    public String visit(Expression n, Boolean argu) throws Exception {
        return n.f0.accept(this, null);
    }

    @Override
    public String visit(BracketExpression n, Boolean argu) throws Exception {
        return n.f1.accept(this, null);
    }

    @Override
    public String visit(AndExpression n, Boolean argu) throws Exception {
        String leftExprType = n.f0.accept(this, null);
        String rightExprType = n.f2.accept(this, null);
        String strLine = "Line " + n.f1.beginLine + ": ";

        if ( !(leftExprType.equals("boolean") && rightExprType.equals("boolean")) )
        {
            throw new SemanticError(strLine + "Operator '&&' is undefined for argument types '" + leftExprType + "', '" + rightExprType + "'");
        }
        return "boolean";
    }

    @Override
    public String visit(CompareExpression n, Boolean argu) throws Exception {
        String leftExprType = n.f0.accept(this, null);
        String rightExprType = n.f2.accept(this, null);
        String strLine = "Line " + n.f1.beginLine + ": ";

        if ( !(leftExprType.equals("int") && rightExprType.equals("int")) )
        {
            throw new SemanticError(strLine + "Operator '<' is undefined for argument types '" + leftExprType + "', '" + rightExprType + "'");
        }
        return "boolean";
    }

    @Override
    public String visit(PlusExpression n, Boolean argu) throws Exception {
        String leftExprType = n.f0.accept(this, null);
        String rightExprType = n.f2.accept(this, null);
        String strLine = "Line " + n.f1.beginLine + ": ";

        if ( !(leftExprType.equals("int") && rightExprType.equals("int")) )
        {
            throw new SemanticError(strLine + "Operator '+' is undefined for argument types '" + leftExprType + "', '" + rightExprType + "'");
        }
        return "int";
    }

    @Override
    public String visit(MinusExpression n, Boolean argu) throws Exception {
        String leftExprType = n.f0.accept(this, null);
        String rightExprType = n.f2.accept(this, null);
        String strLine = "Line " + n.f1.beginLine + ": ";

        if ( !(leftExprType.equals("int") && rightExprType.equals("int")) )
        {
            throw new SemanticError(strLine + "Operator '-' is undefined for argument types '" + leftExprType + "', '" + rightExprType + "'");
        }
        return "int";
    }

    @Override
    public String visit(TimesExpression n, Boolean argu) throws Exception {
        String leftExprType = n.f0.accept(this, null);
        String rightExprType = n.f2.accept(this, null);
        String strLine = "Line " + n.f1.beginLine + ": ";

        if ( !(leftExprType.equals("int") && rightExprType.equals("int")) )
        {
            throw new SemanticError(strLine + "Operator '*' is undefined for argument types '" + leftExprType + "', '" + rightExprType + "'");
        }
        return "int";
    }

    @Override
    public String visit(NotExpression n, Boolean argu) throws Exception {
        String exprType = n.f1.accept(this, null);
        String strLine = "Line " + n.f0.beginLine + ": ";
        if ( !exprType.equals("boolean") )
        {
            throw new SemanticError(strLine + "Operator '!' is undefined for argument of type '" + exprType + "'");
        }
        return "boolean";
    }

    /**
     * f0 -> PrimaryExpression(), [ f2 -> PrimaryExpression() ]
     */
    @Override
    public String visit(ArrayLookup n, Boolean argu) throws Exception {
        String exprType = n.f0.accept(this, null);
        String strLine = "Line " + n.f1.beginLine + ": ";
        // PrimaryExpression must be of type int[]
        if (!exprType.equals("int[]"))
        {
            throw new SemanticError(strLine + "The type of expression must be 'int[]' but resolved to '" + exprType + "'");
        }
        // Index must be of type int
        String indexExprType = n.f2.accept(this, null);
        if (!indexExprType.equals("int"))
        {
            throw new SemanticError(strLine + "Type mismatch: cannot convert from '" + indexExprType + "' to 'int'");
        }
        return "int";
    }

    /**
     * f0 -> PrimaryExpression(), f1 -> ".", f2 -> "length"
     */
    @Override
    public String visit(ArrayLength n, Boolean argu) throws Exception {
        String exprType = n.f0.accept(this, null);
        String strLine = "Line " + n.f1.beginLine + ": ";
        // PrimaryExpression must be of type int[]
        if (!exprType.equals("int[]"))
        {
            throw new SemanticError(strLine + "The type of expression must be 'int[]' but resolved to '" + exprType + "'");
        } 
        return "int";
    }

    /**
     * Grammar production:
     * f0 -> Identifier()
     * f1 -> "["
     * f2 -> Expression()
     * f3 -> "]"
     * f4 -> "="
     * f5 -> Expression()
     * f6 -> ";"
     */
    @Override
    public String visit(ArrayAssignmentStatement n, Boolean argu) throws Exception {
        String idType = n.f0.accept(this, true);
        String lineStr = "Line " + n.f4.beginLine + ": ";

        // Making sure the variable is of type int[]
        if ( !idType.equals("int[]") )
        {
            throw new SemanticError(lineStr + "The type of expression must be 'int[]' but resolved to '" + idType + "'");
        }

        // Making sure index is int
        String indexExprType = n.f2.accept(this, null);
        if ( !indexExprType.equals("int") )
        {
            throw new SemanticError(lineStr + "Type mismatch: cannot convert from '" + indexExprType + "' to 'int'");
        }

        String exprType = n.f5.accept(this, null);
        // Making sure only ints are assigned in an int[] assignment
        if ( !exprType.equals("int") )
        {
            throw new SemanticError(lineStr + "Type mismatch: cannot convert from '" + exprType + "' to 'int'");
        }

        return "int";
    }    

    @Override
    public String visit(Statement n, Boolean argu) throws Exception {
        return n.f0.accept(this, null);
    }

    @Override
    public String visit(IfStatement n, Boolean argu) throws Exception {
        String exprType = n.f2.accept(this, null);

        if ( !exprType.equals("boolean") )
        {
            throw new SemanticError("Line " + n.f0.beginLine + ": Type mismatch: cannot convert from '" + exprType + "' to 'boolean'");
        }

        // Analyzing statements after "if" and "else"
        n.f4.accept(this, null);
        n.f6.accept(this, null);

        return null;
    }

    @Override
    public String visit(WhileStatement n, Boolean argu) throws Exception {
        String exprType = n.f2.accept(this, null);

        if ( !exprType.equals("boolean") )
        {
            throw new SemanticError("Line " + n.f0.beginLine + ": Type mismatch: cannot convert from '" + exprType + "' to 'boolean'");
        }
        // Analyzing statement(s) inside the loop
        n.f4.accept(this, null);

        return null;
    }

    @Override
    public String visit(PrintStatement n, Boolean argu) throws Exception {
        String exprType = n.f2.accept(this, null);
        // Only ints can be printed
        if ( !exprType.equals("int"))
        {
            throw new SemanticError("Line " + n.f0.beginLine + ": Incompatible type argument in 'println' function");
        }
        return null;
    }

    /**
     * f0 -> PrimaryExpression(), f1 -> ".", f2 -> Identifier(), ( f4 -> ( ExpressionList() )? )
     */
    @Override
    public String visit(MessageSend n, Boolean argu) throws Exception {
        String varType = n.f0.accept(this, null);
        ClassInfo classInfo = this.classInfos.get(varType);
        String lineStr = "Line " + n.f1.beginLine + ": ";

        // Checking if we are actually in a Class method (not main)
        if (classInfo == null)
        {
            throw new SemanticError(lineStr + "Cannot invoke method of type '" + varType + "'");
        }
        // Making sure the Class has a method with this name
        String methodName = n.f2.accept(this, null);
        FunctionInfo method = classInfo.getMethod(methodName);
        if (method == null)
        {
            throw new SemanticError(lineStr + "Method '" + methodName + "' is undefined for type '" + varType + "'");
        }

        if (n.f4.present())
        // Args were given in method call
        {
            // Getting a string "<type1>,<type2>,..."
            String argTypesStr = n.f4.accept(this, null);
            if ( !method.hasArgTypes(argTypesStr, this.classInfos) )
            // Argument type are incorrect
            {
                throw new SemanticError(lineStr + "Incompatible arguments given in method '" + methodName + "' call");
            }
        }
        else
        // No args given in method call
        {
            if (method.args.length != 0)
            // The method expects args, so throw error
            {
                throw new SemanticError(lineStr + "Insufficient arguments given in method '" + methodName + "' call");
            }
        }

        return method.type;
    }

    @Override
    public String visit(ExpressionList n, Boolean argu) throws Exception {
        return n.f0.accept(this, null) + n.f1.accept(this, null);
    }

    @Override
    public String visit(ExpressionTail n, Boolean argu) throws Exception {
        String retStr = "";

        for (Node node: n.f0.nodes)
        {
            retStr = retStr + "," + node.accept(this, null);
        }

        return retStr;
    }

    @Override
    public String visit(ExpressionTerm n, Boolean argu) throws Exception {
        return n.f1.accept(this, null);
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
    public String visit(ClassDeclaration n, Boolean argu) throws Exception {
        String classname = n.f1.accept(this, null);
        ClassInfo classInfo = this.classInfos.get(classname);
        this.currentClass = classInfo;
        // Pushing all Class fields in the Symbol Table
        this.symbolTable.addClassNames(classInfo);

        // Analyzing Class methods
        for(Node node: n.f4.nodes)
        {
            node.accept(this, null);
        }

        // Pop all fields from the Symbol Table
        this.symbolTable.popClassNames(classInfo);
        this.currentClass = null;
        
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
    public String visit(ClassExtendsDeclaration n, Boolean argu) throws Exception {
        String classname = n.f1.accept(this, null);
        ClassInfo classInfo = this.classInfos.get(classname);
        this.currentClass = classInfo;
        // Pushing all Class (and superclass) fields in the Symbol Table
        this.symbolTable.addClassNames(classInfo);

        // Analyzing Class methods
        for(Node node: n.f6.nodes)
        {
            node.accept(this, null);
        }

        // Pop all fields from the Symbol Table
        this.symbolTable.popClassNames(classInfo);
        this.currentClass = null;

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
    public String visit(MethodDeclaration n, Boolean argu) throws Exception {
        FunctionInfo method = this.currentClass.getMethod(n.f2.accept(this, null));
        this.symbolTable.addScope();

        // Getting parameter String: "<type> <var>, <type2> <var2>, ..."
        String temp = n.f4.present() ? n.f4.accept(this, null) : "";

        if ( !temp.equals("") )
        {
            // Spliting into strings: "<type> <var>", "<type2> <var2>", ...
            String[] argStr = temp.split("\\s*,\\s*");
            for (int i = 0; i < argStr.length; i++)
            {
                // Spliting each string: "<type>", "<var>"
                String[] currArgStr = argStr[i].split("\\s* \\s*");
                // Pushing each argument in the Symbol Table
                if ( !this.symbolTable.pushVariableEntry(new VariableInfo(currArgStr[1], currArgStr[0])) )
                {
                    throw new SemanticError("Duplicate argument '" + currArgStr[1] + "' in method '" + method.name + "'");
                }
            }
        }

        String[] varStr;
        for (Node node: n.f7.nodes)
        // Pushing all local variables in the Symbol Table
        {
            varStr = node.accept(this, null).split("\\s* \\s*");
            if ( !this.symbolTable.pushVariableEntry(new VariableInfo(varStr[1], varStr[0])) )
            {
                throw new SemanticError("Variable '" + varStr[1] + "' in method '" + method.name +"' is declared more than once");
            }
        }

        // Analyzing method body statements
        for (Node node: n.f8.nodes)
        {
            node.accept(this, null);
        }

        // Checking return expression type
        String returnExprType = n.f10.accept(this, null);
        // If the return expression type is not the same as the method type
        if ( !returnExprType.equals(method.type))
        {
            // Then the method type can only be a superclass of the return expression type
            ClassInfo returnTypeClass = this.classInfos.get(returnExprType);
            if (returnTypeClass == null || !returnTypeClass.hasSuperClass(method.type))
            {
                String message = "Type mismatch in method '" + method.name;
                message = message + "' return value: cannot convert from '" + returnExprType + "' to '" + method.type + "'";
                throw new SemanticError(message);
            }
        }

        // Pop all local variables from the Symbol Table
        this.symbolTable.popScope();
        return null;
    }

    @Override
    public String visit(FormalParameter n, Boolean argu) throws Exception {
        return n.f0.accept(this, null) + " " + n.f1.accept(this, null);
    }

    @Override
    public String visit(FormalParameterList n, Boolean argu) throws Exception {
        String ret = n.f0.accept(this, null);
        
        if (n.f1 != null) {
            ret += n.f1.accept(this, null);
        }
        
        return ret;
    }

    @Override
    public String visit(FormalParameterTail n, Boolean argu) throws Exception {
        String params = "";
        for ( Node node: n.f0.nodes) {
            params += "," + node.accept(this, null);
        }
        return params;
    }

    @Override
    public String visit(FormalParameterTerm n, Boolean argu) throws Exception {
        return n.f1.accept(this, null);
    }

    @Override
    public String visit(ArrayType n, Boolean argu) {
        return "int[]";
    }

    @Override
    public String visit(BooleanType n, Boolean argu) {
        return "boolean";
    }

    @Override
    public String visit(IntegerType n, Boolean argu) {
        return "int";
    }

    /**
     * This is a worth mentioning point: this method will be called in e.g. Variable Declarations,
     * where the variable **name** will be needed, but also in Primary Expressions, were
     * the variable **type** will be needed. So we take advantage of the optional argument here.
     * 
     * @param needType If this is {@code null}, then the identifier *name* will be returned. If not,
     * the *type* of the variable associated with this name will be returned, if stored in the Symbol Table,
     * or a Semantic Error exception will be thrown.
     */
    @Override
    public String visit(Identifier n, Boolean needType) throws Exception {
        String name = n.f0.toString();
        if (needType == null)
        {
            return name;
        }
        VariableInfo var = this.symbolTable.getVariableEntry(name);
        if (var == null)
        {
            throw new SemanticError("Line " + n.f0.beginLine + ": Identifier '" + name + "' is undefined");
        }
        return var.typeName;
    }

    @Override
    public String visit(IntegerLiteral n, Boolean argu) throws Exception {
        String str = n.f0.toString();
        /** A last-minute addition to prevent invalid integer literals.
            No checking for Integer.MIN_VALUE needed since negative literals 
            are rejected by the Parser. */
        if (Long.parseLong(str) > Integer.MAX_VALUE)
        {
            throw new SemanticError("Line " + n.f0.beginLine + ": The literal " + str + " of type 'int' is out of range");
        }
        return "int";
    }

    @Override
    public String visit(TrueLiteral n, Boolean argu) throws Exception {
        return "boolean";
    }

    @Override
    public String visit(FalseLiteral n, Boolean argu) throws Exception {
        return "boolean";
    }

    @Override
    public String visit(ThisExpression n, Boolean argu) throws Exception {
        // Making sure we are actually inside a class method (not main)
        if (this.currentClass == null)
        {
            throw new SemanticError("Cannot refer to 'this' outside of a class method");
        }
        return this.currentClass.name;
    }
}

/**
 * File: DeclarationCollector.java
 * Pavlos Spanoudakis (sdi1800184)
 */

package Visitors;

import syntaxtree.*;
import visitor.*;
import ClassInformation.*;
import java.util.Map;

/**
 * Collects and stores all Class Fields and Methods. 
 * 
 * Each {@code visit} method returns a String that indicates the type 
 * of the respective statement, or {@code null} if this is not needed.
 * The second argument is a {@code ClassInfo} to be provided to MethodDeclaration
 * and VarDecaration (field declaration) {@code visit} methods, to allow
 * for instant access to current class fields/methods up to that point.
 */
public class DeclarationCollector extends GJDepthFirst<String, ClassInfo>{
    // Classes will be stored here
    Map<String, ClassInfo> classInfos;
    public DeclarationCollector(Map<String, ClassInfo> classInfoMap) {
        // ClassNameCollector has already collected class names
        classInfos = classInfoMap;
    }

    @Override
    public String visit(MainClass n, ClassInfo argu) throws Exception {
        // Nothing to be done here (yet)
        ClassInfo classInfo = this.classInfos.get(n.f1.accept(this, null));
        classInfo.setScope();
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
    public String visit(ClassDeclaration n, ClassInfo argu) throws Exception {
        ClassInfo classInfo = this.classInfos.get(n.f1.accept(this, null));
        // Offset counters will both be set to 0
        classInfo.setOffsets();

        // Store and analyze class fields
        for (Node node: n.f3.nodes)
        {
            node.accept(this, classInfo);
        }

        // Store and analyze class methods
        for (Node node: n.f4.nodes)
        {
            node.accept(this, classInfo);
        }

        classInfo.setScope();
        return null;
    }

    @Override
    public String visit(VarDeclaration n, ClassInfo classInfo) throws Exception {
        String type = n.f0.accept(this, null);
        String name = n.f1.accept(this, null);

        // Prevent duplicate field names
        if (classInfo.getField(name) != null)
        {
            throw new SemanticError("Field '" + name + "' declared more than once in class '" + classInfo.name + "'");
        }
        else
        {
            // Checking if field type is valid
            if (VariableInfo.isPrimitiveType(type) || (classInfos.get(type) != null))
            {
                classInfo.addField(name, type);
            }
            else
            {
                throw new SemanticError("Field '" + name + "' in class '" + classInfo.name + "' has invalid type '" + type + "'");
            }
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
    public String visit(ClassExtendsDeclaration n, ClassInfo argu) throws Exception {
        ClassInfo classInfo = this.classInfos.get(n.f1.accept(this, null));
        // Set the offset counters using superclass counters
        classInfo.setOffsets();

        // Store and analyze class fields
        for (Node node: n.f5.nodes)
        {
            node.accept(this, classInfo);
        }

        // Store and analyze class methods
        for (Node node: n.f6.nodes)
        {
            node.accept(this, classInfo);
        }

        classInfo.setScope();
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
    public String visit(MethodDeclaration n, ClassInfo classInfo) throws Exception {
        
        String type = n.f1.accept(this, null);

        // Checking if the return type is valid
        if ( !VariableInfo.isPrimitiveType(type) && this.classInfos.get(type) == null)
        {
            throw new SemanticError("Method return type '" + type + "' is undefined");
        }
        String name = n.f2.accept(this, null);        
        // Prevent method overloading/duplicate declaration
        if (classInfo.hasDeclaredMethod(name))
        {
            throw new SemanticError("Method '" + name + "' declared more than once in class '" + classInfo.name + "'");
        }

        /*// In case a method with the same name as the class is not preferable
        if (name.equals(classInfo.name))
        {
            throw new SemanticError("Line " + n.f0.beginLine + ": This method has a constructor name");
        }
        */

        // Getting a string with the argument type, seperated by commas
        String argTypeStr = n.f4.present() ? n.f4.accept(this, null) : "";
        String[] argTypes = FunctionInfo.getArgTypes(argTypeStr);

        FunctionInfo superMethod;
        // If a method with the same name is declared in a superclass, it must be overriden
        if ( classInfo.superClass != null && (superMethod = classInfo.superClass.getMethod(name)) != null)
        {
            if ( !superMethod.isOverriden(type, argTypes) )
            {
                throw new SemanticError("Method '" + name + "' in class '" + classInfo.name + "' is incorrectly overriding a superclass method");
            }
            classInfo.addOverridenMethod(name, type, argTypes);
        }
        else
        {
            classInfo.addMethod(name, type, argTypes);
        }
        
        return null;
    }

    /**
     * f0 -> FormalParameter()
     * f1 -> FormalParameterTail()
     */
    @Override
    public String visit(FormalParameterList n, ClassInfo argu) throws Exception {
        String ret = n.f0.accept(this, null);
        
        if (n.f1 != null) {
            ret += n.f1.accept(this, null);
        }
        
        return ret;
    }

    @Override
    public String visit(FormalParameterTerm n, ClassInfo argu) throws Exception {
        return n.f1.accept(this, argu);
    }

    @Override
    public String visit(FormalParameterTail n, ClassInfo argu) throws Exception {
        String ret = "";
        for ( Node node: n.f0.nodes ) {
            ret += "," + node.accept(this, null);
        }

        return ret;
    }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
     */
    @Override
    public String visit(FormalParameter n, ClassInfo classInfo) throws Exception{
        String type = n.f0.accept(this, null);
        String name = n.f1.accept(this, null);

        if ( !VariableInfo.isPrimitiveType(type) && (classInfos.get(type) == null))
        {
            throw new SemanticError("Method parameter '" + name + "' has invalid type '" + type + "'");
        }

        return type;
    }

    @Override
    public String visit(ArrayType n, ClassInfo argu) {
        return "int[]";
    }

    @Override
    public String visit(BooleanType n, ClassInfo argu) {
        return "boolean";
    }

    @Override
    public String visit(IntegerType n, ClassInfo argu) {
        return "int";
    }

    @Override
    public String visit(Identifier n, ClassInfo argu) {
        return n.f0.toString();
    }
}

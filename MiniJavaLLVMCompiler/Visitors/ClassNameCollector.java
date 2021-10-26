/**
 * File: ClassNameCollector.java
 * Pavlos Spanoudakis (sdi1800184)
 */

package Visitors;

import syntaxtree.*;
import visitor.*;
import ClassInformation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Collects and stores all Class Names in the target file.
 */
public class ClassNameCollector extends GJDepthFirst<String, Void> {
    // Classes will be stored here
    public Map<String, ClassInfo> classInfos;
    public ClassNameCollector() {
        classInfos = new LinkedHashMap<String, ClassInfo>();
    }

    @Override
    public String visit(MainClass n, Void argu) throws Exception {
        // Treating MainClass as a regular class to avoid redeclaration
        String className = n.f1.accept(this, null);
        ClassInfo classInfo = new ClassInfo(className);
        this.classInfos.put(className, classInfo);
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
    public String visit(ClassDeclaration n, Void argu) throws Exception {
        String classname = n.f1.accept(this, null);
        // Store class if not already present
        if (this.classInfos.get(classname) != null)
        {
            throw new SemanticError("Class '" + classname + "' is declared more than once");
        }

        ClassInfo classInfo = new ClassInfo(classname);
        this.classInfos.put(classname, classInfo);

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
    public String visit(ClassExtendsDeclaration n, Void argu) throws Exception {
        String className = n.f1.accept(this, null);
        String superClassName = n.f3.accept(this, null);
        // Store class if not already present
        if (this.classInfos.get(className) != null)
        {
            throw new SemanticError("Class '" + className + "' is declared more than once");
        }
        ClassInfo superClass = this.classInfos.get(superClassName);
        // Checking if superclass has already been declared
        if (superClass == null)
        {
            throw new SemanticError("Class '" + superClassName + "' is being extended without having been declared");
        }

        ClassInfo classInfo = new ClassInfo(className, superClass);
        this.classInfos.put(className, classInfo);

        return null;
    }

    @Override
    public String visit(Identifier n, Void argu) {
        return n.f0.toString();
    }
}

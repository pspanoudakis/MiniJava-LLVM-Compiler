/**
 * File: ClassInfo.java
 * Pavlos Spanoudakis (sdi1800184)
 */

package ClassInformation;

import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import SymbolTable.ScopeSymbols;

/**
 * Contains information related to a specific Class.
 */
public class ClassInfo {
    
    /** The Class name. */
    public final String name;
    /** The super Class of the Class. */
    public final ClassInfo superClass;
    /** The Class fields. */
    private Map<String, FieldInfo> fields;
    /** The Class methods. */
    private Map<String, MethodInfo> methods;
    /** The fields known to the methods of the Class (to be used by the Symbol Table) */
    public ScopeSymbols scopeVars;
    /** Offset counters. */
    int fieldOffset;
    int methodOffset;    
    /** The class Virtual Table. */
    private VirtualTable vtable;

    /**
     * Creates a ClassInfo with the specified Class name.
     */
    public ClassInfo(String className) {
        this.name = className;
        this.superClass = null;
        this.fields = new LinkedHashMap<String, FieldInfo>();
        this.methods = new LinkedHashMap<String, MethodInfo>();
        this.scopeVars = null;
        this.vtable = new VirtualTable();
    }

    /**
     * Creates a ClassInfo with the specified Class name and 
     * the given superclass.
     */
    public ClassInfo(String className, ClassInfo superClassInfo)
    {
        this.name = className;
        this.superClass = superClassInfo;
        this.fields = new LinkedHashMap<String, FieldInfo>();
        this.methods = new LinkedHashMap<String, MethodInfo>();
        this.scopeVars = null;
        this.vtable = new VirtualTable();
    }

    /**
     * Returns {@code true} if the Class has a Method with the given name.
     * Superclass methods will not be checked.
     */
    public boolean hasDeclaredMethod(String methodName)
    {
        return this.methods.get(methodName) != null;
    }

    /**
     * Returns the Class method with the specified name.
     * If this Class does not have such method, returns {@code null}.
     */
    public FunctionInfo getMethod(String methodName)
    {
        // Check if the Class has a method with the given name
        MethodInfo methodInfo = methods.get(methodName);
        
        if (methodInfo == null)
        // If not, check if the superclass does.
        {
            if (this.superClass != null)
            {
                return this.superClass.getMethod(methodName);
            }
            return null;
        }

        return methodInfo.method;
    }

    /**
     * Returns the Class method with the specified name.
     * If this Class does not have such method, returns {@code null}.
     */
    public MethodInfo getMethod(String methodName, boolean needOffset)
    {
        // Check if the Class has a method with the given name
        MethodInfo methodInfo = methods.get(methodName);
        
        if (methodInfo == null)
        // If not, check if the superclass does.
        {
            if (this.superClass != null)
            {
                return this.superClass.getMethod(methodName, true);
            }
            return null;
        }

        return methodInfo;
    }

    public MethodInfo getVtableMethod(String name)
    {
        return this.vtable.getMethod(name);
    }

    public FieldInfo getFieldRec(String fieldName)
    {
        FieldInfo result = fields.get(fieldName);
        if (result == null && this.superClass != null)
        {
            return this.superClass.getFieldRec(fieldName);
        }
        return result;
    }

    /**
     * Returns the Class field with the specified name.
     * If this Class does not have such field, returns {@code null}.
     */
    public VariableInfo getField(String fieldName)
    {
        FieldInfo result = fields.get(fieldName);

        if (result == null)
        {
            return null;
        }
        return result.field;
    }

    // Probably won't be needed
    public FieldInfo getOffsetField(String fieldName)
    {
        FieldInfo result = fields.get(fieldName);

        if (result == null)
        {
            return null;
        }
        return result;
    }

    /**
     * Adds a new Class field with the given name and type.
     */
    public void addField(String fieldName, String fieldType)
    {
        // Store the field
        fields.put( fieldName, new FieldInfo(new VariableInfo(fieldName, fieldType), this.fieldOffset) );
        // set the field offset properly
        if (fieldType.equals("int"))
        {
            this.fieldOffset += 4;
        }
        else if (fieldType.equals("boolean"))
        {
            this.fieldOffset += 1;
        }
        else
        {
            this.fieldOffset += 8;
        }
    }

    /**
     * Adds a new Class method with the given name, return type and argument types.
     */
    public void addMethod(String methodName, String type, String[] argTypes)
    {
        methods.put( methodName, new MethodInfo(new FunctionInfo(methodName, type, argTypes), this.name, methodOffset) );
        this.methodOffset += 8;
    }

    /**
     * Adds a new Class method with the given name, return type and argument types,
     * which overrides a superclass method. The offset is set to -1 in order to be ignored.
     */
    public void addOverridenMethod(String methodName, String type, String[] argTypes)
    {
        methods.put( methodName, new MethodInfo(new FunctionInfo(methodName, type, argTypes), this.name, -1) );
    }

    /**
     * Returns {@code true} if the Class has a superclass
     *  with the given name, {@code false} otherwise.
     */
    public boolean hasSuperClass(String superName)
    {
        if (this.superClass == null)
        {
            return false;
        }
        else
        {
            if (this.superClass.name.equals(superName))
            {
                return true;
            }
            return this.superClass.hasSuperClass(superName);
        }
    }

    /**
     * Adds the Class fields to the Scope structure to be provided to the Symbol Table.
     */
    public void setScope()
    {
        this.scopeVars = new ScopeSymbols();
        for (Map.Entry<String, FieldInfo> entry: this.fields.entrySet())
        {
            scopeVars.addVariable(entry.getValue().field);
        }
    }

    /**
     * Setup the Class offset counters (to be used before adding fields/methods).
     */
    public void setOffsets()
    {
        if (this.superClass != null)
        {
            this.fieldOffset = this.superClass.fieldOffset;
            this.methodOffset = this.superClass.methodOffset;
        }
        else
        {
            this.fieldOffset = 0;
            this.methodOffset = 0;
        }        
    }

    /**
     * Prints the field/method offsets of the Class.
     */
    public void printOffsets()
    {
        for (Map.Entry<String, FieldInfo> entry: this.fields.entrySet())
        {
            System.out.println(this.name + "." + entry.getKey() + ": " + entry.getValue().offset);
        }

        for (Map.Entry<String, MethodInfo> entry: this.methods.entrySet())
        {
            if (entry.getValue().offset != -1)
            {
                System.out.println(this.name + "." + entry.getKey() + ": " + entry.getValue().offset);
            }
        }
    }

    /**
     * Stores the superclass (if there is one) methods and the class methods
     * into the given Virtual Table.
     * This is called by a subclass {@link #createVirtualTable()} call.
     */
    public void createVirtualTable(VirtualTable table)
    {
        if (this.superClass != null)
        {
            this.superClass.createVirtualTable(table);
        }
        for (Map.Entry<String, MethodInfo> entry: this.methods.entrySet())
        {
            table.insert(entry.getValue());
        }
    }

    /**
     * Creates the VirtualTable for this class.
     * The superclass(es) methods are stored first recursively, and this class
     * methods are stored in the end.
     */
    public void createVirtualTable()
    {
        if (this.superClass != null)
        {
            this.superClass.createVirtualTable(this.vtable);
        }
        for (Map.Entry<String, MethodInfo> entry: this.methods.entrySet())
        {
            this.vtable.insert(entry.getValue());
        }
    }
    
    /**
     * Increases all field offsets + 8, since the Virtual Table address is stored at offset 0.
     */
    public void offsetIncrement()
    {
        for (Map.Entry<String, FieldInfo> entry: this.fields.entrySet())
        {
            entry.getValue().offset += 8;
        }
        fieldOffset += 8;
    }

    /**
     * Returns the size of each instance of this class (in bytes).
     */
    public int getObjectSize()
    {
        return this.fieldOffset;
    }

    /**
     * Used for printing the class vtable at the beginning of the IR file, as a global table.
     * 
     * @param writer The {@code FileWriter} to write the vtable to.
     */
    public void emitVtable(FileWriter writer) throws IOException
    {
        writer.write("@." + name + "_vtable = global [" + vtable.getNumEntries() + " x i8*] [");
        vtable.emit(writer);
        writer.write("]\n");
    }

    /**
     * Returns the number of methods in the class vtable.
     */
    public int getVtableNumEntries()
    {
        return vtable.getNumEntries();
    }
}

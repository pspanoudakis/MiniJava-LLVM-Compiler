/**
 * File: VariableInfo.java
 * Pavlos Spanoudakis (sdi1800184)
 */

package ClassInformation;

/**
 * Contains information about a variable or class field.
 */
public class VariableInfo {
    
    public final String name;
    public final String typeName;
    /** The equivalent type of typeName. */
    public final String IRType;
    /** The local register that holds this variable (used for local variables). */
    public String register;

    /**
     * Creates a VariableInfo with the specified name and type.
     */
    public VariableInfo(String varName, String varTypeName) {
        this.name = varName;
        this.typeName = varTypeName;
        this.IRType = getIRType(varTypeName);
        this.register = null;
    }

    public VariableInfo(String varName, String varTypeName, String varRegister) {
        this.name = varName;
        this.typeName = varTypeName;
        this.IRType = getIRType(varTypeName);
        this.register = varRegister;
    }

    /**
     * Returns {@code true} if the given type name 
     * is a Primitive MiniJava Type, {@code} false otherwise.
     */
    public static boolean isPrimitiveType(String typeName)
    {
        return (typeName.equals("int") || typeName.equals("boolean") || typeName.equals("int[]"));
    }

    /**
     * Returns the equivalent IR type for the given MiniJava type.
     */
    public static String getIRType(String minijavaType)
    {
        if (minijavaType.equals("int"))
        {
            return "i32";
        }
        else if (minijavaType.equals("boolean"))
        {
            return "i1";
        }
        else if (minijavaType.equals("int[]"))
        {
            return "i32*";
        }
        return "i8*";
    }
}

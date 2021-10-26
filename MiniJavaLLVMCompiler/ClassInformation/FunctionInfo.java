/**
 * File: FunctionInfo.java
 * Pavlos Spanoudakis (sdi1800184)
 */

package ClassInformation;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

/**
 * Contains information related to a class Method.
 */
public class FunctionInfo {
    
    /** The Method return type. */
    public final String type;
    /** The Method name. */
    public final String name;
    /** The Method argument types. */
    public final String[] args;
    

    public FunctionInfo(String functionName, String typeName, String[] argTypes)
    {
        this.type = typeName;
        this.name = functionName;
        this.args = argTypes;
    }

    /**
     * Returns {@code true} if the Method has the specified return type and
     * argument types, and therefore can be overriden successfully, or {@code false} otherwise.
     */
    public boolean isOverriden(String typeName, String[] argTypes)
    {
        if (!this.type.equals(typeName))
        {
            return false;
        }
        if (this.args.length!= argTypes.length)
        {
            return false;
        }

        for(int i = 0; i < this.args.length; i++)
        {
            if ( !this.args[i].equals(argTypes[i]))
            {
                return false;
            }
        }
        
        return true;
    }

    /**
     * Splits the given argument types string into an array of strings.
     * The given string must be of the form: {@code "<type>(,<type>)*"},
     * or {@code ""} if there are no arguments.
     */
    public static String[] getArgTypes(String strArgTypes)
    {
        if (strArgTypes.equals(""))
        {
            return new String[0];
        }
        else
        {
            return strArgTypes.split("\\s*,\\s*");
        }
    }

    /**
     * To be called in Method call checking.
     * Returns {@code true} if the Method call has compatible argument types, {@code false} otherwise.
     * @param argTypesStr A string of the form {@code "<type>(,<type>)*"}, or {@code ""},
     * which contains the types of the arguments given in the method call.
     * @param classInfos The classes that the visitor is aware of.
     */
    public boolean hasArgTypes(String argTypesStr, Map<String, ClassInfo> classInfos)
    {
        String[] argTypes = argTypesStr.split("\\s*,\\s*");
        
        // If the number of agruments is different than exepected, the call is not valid.
        if (this.args.length != argTypes.length)
        {
            return false;
        }

        ClassInfo classInfo;

        // Checking the corresponding argument types
        for (int i = 0; i < this.args.length; i++)
        {
            if ( !this.args[i].equals(argTypes[i]) )
            // If the argument type is not the same as expected
            {
                // If the argument type is a class and the expected type is a superclass of that class,
                // then the argument is valid.
                classInfo = classInfos.get(argTypes[i]);
                if (classInfo == null || !classInfo.hasSuperClass(this.args[i]))
                {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Displays the IR type string of the Method (return type & parameter types).
     * To be used when displaying class Virtual Tables.
     * 
     * @param writer The {@code FileWriter} to write the signature to.
     */
    public void emit(FileWriter writer) throws IOException
    {
        writer.write(getIRsignature() + "*");
    }

    /**
     * Returns the IR signature of the Method (return type & parameter types).
     */
    public String getIRsignature()
    {
        String sig = VariableInfo.getIRType(type) + " (i8*";
        for (int i = 0; i < args.length; i++)
        {
            sig = sig + "," +  VariableInfo.getIRType(args[i]);
        }
        sig = sig + ")";
        return sig;
    }
}

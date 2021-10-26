/**
 * File: MethodInfo.java
 * Pavlos Spanoudakis (sdi1800184)
 */

package ClassInformation;

/**
 * FunctionInfo wrapper class to store Method offset.
 */
public class MethodInfo {
    
    // This is the Method name in the IR file ("@<class>.<name>"")
    public final String fullname;
    public FunctionInfo method;
    public int offset;

    public MethodInfo(FunctionInfo fun, String classname, int n)
    {
        fullname = "@" + classname + "." + fun.name;
        method = fun;
        offset = n;
    }
}

/**
 * File: FieldInfo.java
 * Pavlos Spanoudakis (sdi1800184)
 */

package ClassInformation;

/**
 * VariableInfo wrapper class to store field offset.
 */
public class FieldInfo {
    
    public VariableInfo field;
    public int offset;

    public FieldInfo(VariableInfo var, int n)
    {
        field = var;
        offset = n;
    }
}

/**
 * File: VirtualTable.java
 * Pavlos Spanoudakis (sdi1800184)
 */

package ClassInformation;

import java.util.Map;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;

/**
 * Represents the Virtual Table of a Class.
 */
public class VirtualTable {

    /** Mapped Method names to Method objects (which contain the offsets). */
    private Map<String, MethodInfo> entries;
    
    public VirtualTable()
    {
        entries = new LinkedHashMap<String, MethodInfo>();
    }

    /**
     * Inserts the given Method in the Virtual Table.
     * If a method with the same name already exists, it is replaced.
     */
    public void insert(MethodInfo method)
    {
        MethodInfo present = entries.get(method.method.name);
        if (present != null)
        // If a method with the same name is already present
        {
            // Get its offset
            method.offset = present.offset;
        }
        // Insert the given method to the Table. The existing method
        // will be replaced, if there was one.
        entries.put(method.method.name, method);
    }

    /**
     * Returns the method with the specified name, or {@code null}
     * if such method does not exist.
     */
    public MethodInfo getMethod(String name)
    {
        return this.entries.get(name);
    }

    /**
     * Used for printing the vtable at the beginning of the IR file, as a global table.
     * Only the contents will be printed here. The vtable name and size 
     * have already been printed by {@link ClassInfo#emitVtable}.
     * 
     * @param writer The {@code FileWriter} to write the vtable contents to.
     */
    public void emit(FileWriter writer) throws IOException
    {
        Iterator<MethodInfo> itr = this.entries.values().iterator();
        // Iterate over the methods
        if (itr.hasNext())
        {
            // Display the first method
            MethodInfo method = itr.next();
            writer.write("i8* bitcast (");
            // Display signature (return type & parameter types)
            method.method.emit(writer);
            writer.write(" " + method.fullname + " to i8*)");
            // Do same for the rest
            while(itr.hasNext())
            {
                // Seperate by ','
                writer.write(", ");
                method = itr.next();
                writer.write("i8* bitcast (");
                method.method.emit(writer);
                writer.write(" " + method.fullname + " to i8*)");
            }
        }
    }

    /**
     * Returns the number of Methods stored in the Virtual Table.
     */
    public int getNumEntries()
    {
        return this.entries.size();
    }
}

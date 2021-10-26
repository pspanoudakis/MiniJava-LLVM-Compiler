/**
 * File: ScopeSymbols.java
 * Pavlos Spanoudakis (sdi1800184)
 */

package SymbolTable;

import java.util.HashMap;
import java.util.Map;

import ClassInformation.VariableInfo;

/**
 * The structure to be contained in the Symbol Table.
 */
public class ScopeSymbols {
    
    /** Class fields and local variables can be included here. */
    private Map<String, VariableInfo> variables;

    public ScopeSymbols() {
        variables = new HashMap<String, VariableInfo>();
    }

    public void addVariable(VariableInfo v) {
        variables.put(v.name, v);
    }

    public boolean varIsPresent(VariableInfo v) {
        return variables.containsKey(v.name);
    }

    public VariableInfo getVariable(String name) {
        return variables.get(name);
    }
}

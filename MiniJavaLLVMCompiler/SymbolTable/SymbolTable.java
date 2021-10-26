/**
 * File: SymbolTable.java
 * Pavlos Spanoudakis (sdi1800184)
 */

package SymbolTable;

import ClassInformation.ClassInfo;
import ClassInformation.VariableInfo;

import java.util.NoSuchElementException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

public class SymbolTable {
    
    /** A Stack of ScopeSymbols is used.
     *  {@code java.util.Stack} is considered obsolete, so a {@code Deque} is prefered.
     */
    private Deque<ScopeSymbols> scopes;

    public SymbolTable()
    {
        scopes = new ArrayDeque<ScopeSymbols>();
    }

    /**
     * Pushes a new ScopeSymbols structure to the Stack.
     */
    public void addScope()
    {
        scopes.add(new ScopeSymbols());
    }

    /**
     * Adds a new Variable/Class Field in the ScopeSymbols structure that is
     * on top of the Stack.
     * @return {@code true} if the variable was not already present, and therefore
     * was successfully added, or {@code false} if it was already present.
     */
    public boolean pushVariableEntry(VariableInfo var)
    {
        if (scopes.peekLast().varIsPresent(var))
        {
            return false;
        }
        scopes.peekLast().addVariable(var);
        return true;
    }

    /**
     * Iterates over the Stack, starting from the top Scope, and returns the first occurence
     * of a variable with the specified name. If such variable is not found, {@code null} is returned.
     */
    public VariableInfo getVariableEntry(String symbolName)
    {
        Iterator<ScopeSymbols> itr = scopes.descendingIterator(); 
        VariableInfo entry = null;

        while (entry == null && itr.hasNext())
        {
            entry = itr.next().getVariable(symbolName);
        }
        return entry;
    }

    /**
     * Returns the Variable with the specified name, if found in the top Scope.
     * If such variable was not found, {@code null} is returned.
     */
    public VariableInfo getCurrentScopeVariable(String symbolName)
    {
        if (scopes.size() == 0)
        {
            return null;
        }
        return scopes.peekLast().getVariable(symbolName);
    }

    /**
     * Pop the top Scope names from the Symbol Table.
     */
    public void popScope()
    {
        try {
            scopes.removeLast();
        } catch (NoSuchElementException e) {
            return;
        }
    }

    /**
     * Adds all the fields known to the specified Class in the Symbol Table.
     */
    public void addClassNames(ClassInfo classInfo)
    {
        if (classInfo.superClass != null)
        // If the class has a superclass, add the superclass fields first
        {
            // Fields will be added for all the superclasses
            addClassNames(classInfo.superClass);
        }
        // Add the Class fields in the end
        scopes.add(classInfo.scopeVars);
    }

    /**
     * Removes all the fields known to the specified Class from the Symbol Table.
     */
    public void popClassNames(ClassInfo classInfo)
    {
        // Remove the Class fields first
        scopes.removeLast();
        if (classInfo.superClass != null)
        // If the class has a superclass, remove the superclass fields as well
        {
            // Fields will be removed for all the superclasses
            popClassNames(classInfo.superClass);
        }
    }
}

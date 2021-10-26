/**
 * File: SemanticError.java
 * Pavlos Spanoudakis (sdi1800184)
 */

package Visitors;

/**
 * Signals that a semantic error was detected by a Visitor,
 * therefore the semantic analysis has been aborted.
 */
public class SemanticError extends Exception {
    String message = null;
    public SemanticError() {
        this.message = "Semantic Error.";
    }
    public SemanticError(String s) {
        this.message = "Semantic Error: " + s + ".";
    }
    public String getMessage()
    {
        return message;
    }
}

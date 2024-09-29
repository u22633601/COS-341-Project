import java.util.HashSet;
import java.util.Set;

public class SymbolTable {
    private Set<String> variables; // Set to store variables in the current scope

    public SymbolTable() {
        variables = new HashSet<>();
    }

    // Adds a variable to the current scope
    public void add(String varName) {
        variables.add(varName);
    }

    // Checks if the current scope contains the variable
    public boolean contains(String varName) {
        return variables.contains(varName);
    }

    // Returns the list of all variables in the current scope
    public Set<String> getVariables() {
        return variables;
    }

    // Prints all variables in the current scope
    public void printTable() {
        System.out.println("Variables: " + variables);
    }
}

import java.util.*;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.File;

// Main class containing the ScopeAnalyzer logic and entry point
public class ScopeAnalyzerMain {

    // ScopeAnalyzer class
    static class ScopeAnalyzer {
        private Stack<SymbolTable> scopeStack; // Stack to hold the scopes
        private int varCounter;
        private int funcCounter;
        private Map<String, String> uniqueNames; // Maps user-defined names to unique internal names
        private boolean stopOnError; // Flag to indicate whether the analysis should stop on error

        public ScopeAnalyzer() {
            scopeStack = new Stack<>(); // Using Stack for scoped symbol tables
            scopeStack.push(new SymbolTable()); // Global scope is created at initialization and stays
            varCounter = 100; // Start unique variable names at v100
            funcCounter = 500; // Start unique function names at f500
            uniqueNames = new HashMap<>();
            stopOnError = false; // Set the default to not stop on error
        }

        public void enterScope() {
            System.out.println("Entering a new scope.");
            scopeStack.push(new SymbolTable());
            printCurrentSymbolTable();
        }

        public void exitScope() {
            if (!scopeStack.isEmpty() && scopeStack.size() > 1) { // Ensure we don't pop the global scope
                printCurrentSymbolTable();
                System.out.println("Exiting the current scope.");
                scopeStack.pop(); // Pop only non-global scopes
                printCurrentSymbolTable(); // Print after the scope has been exited
            } else {
                System.err.println("Error: Attempted to exit scope, but no non-global scope exists.");
            }
        }

        // Generates a unique variable name, e.g., "v136"
        private String generateUniqueVarName() {
            return "v" + (varCounter++);
        }

        // Generates a unique function name, e.g., "f561"
        private String generateUniqueFuncName() {
            return "f" + (funcCounter++);
        }

        // Declare a variable with a unique internal name
        public boolean declare(String varName) {
            SymbolTable currentScope = scopeStack.peek();

            // Check if the variable is already declared in the current or outer scopes
            if (currentScope.contains(varName)) {
                System.err.println("Error: Variable '" + varName + "' is already declared in this scope.");
                stopOnError = true; // Set the flag to stop the program
                return false;
            }

            // If it's already declared in an outer scope, don't redeclare, just use it
            if (containsInAnyScope(varName)) {
                System.out.println("Using existing variable from outer scope: " + varName + " as " + uniqueNames.get(varName));
                return true;
            }

            // Generate and assign a unique internal name
            String uniqueName = isFunction(varName) ? generateUniqueFuncName() : generateUniqueVarName();
            uniqueNames.put(varName, uniqueName);
            System.out.println("Declaring " + (isFunction(varName) ? "function" : "variable") + ": " + varName + " as " + uniqueName);
            
            currentScope.add(varName); // Add user-defined name to the current scope
            return true;
        }

        // Check if the variable is declared in the current or outer scopes
        public boolean containsInAnyScope(String varName) {
            for (SymbolTable scope : scopeStack) {
                if (scope.contains(varName)) {
                    return true;
                }
            }
            return false;
        }

        // Check if a variable or function is in use and replace with the internal unique name
        public boolean checkUsage(String varName) {
            for (SymbolTable scope : scopeStack) {
                if (scope.contains(varName)) {
                    String uniqueName = uniqueNames.get(varName);
                    System.out.println("Using " + (isFunction(varName) ? "function" : "variable") + ": " + varName + " as " + uniqueName);
                    return true;
                }
            }
            System.err.println("Error: Variable or function '" + varName + "' is not declared in the current scope.");
            stopOnError = true; // Stop the program if the variable is not declared
            return false;
        }

        // Analyze a node and assign unique names to variables and functions
        public void analyzeNode(Node node) {
            // Stop analysis if there's an error
            if (stopOnError) {
                return;
            }

            if (node.isBlockStart()) {
                enterScope();
            }
            if (node.isBlockEnd()) {
                exitScope();
            }

            if (node.getType().equals("TERMINAL") && !node.getVarName().isEmpty()) {
                if (isVariableOrFunction(node.getVarName())) {
                    if (node.isAssignment()) {
                        // Treat as variable usage for assignment
                        if (checkUsage(node.getVarName())) {
                            System.out.println("Assigning value to " + node.getVarName());
                        }
                    } else if (scopeStack.peek().contains(node.getVarName())) {
                        // Detect multiple declarations within the same scope
                        System.err.println("Error: Variable/Function '" + node.getVarName() + "' is already declared in this scope.");
                        stopOnError = true; // Stop the program
                    } else {
                        declare(node.getVarName()); // Treat as a declaration
                    }
                } else {
                    System.out.println("Ignoring terminal: " + node.getVarName());
                }
            }

            // Recursively analyze child nodes
            for (Node child : node.getChildren()) {
                analyzeNode(child);
                // Stop analysis if there's an error in child nodes
                if (stopOnError) {
                    break;
                }
            }
        }

        // Helper function to decide if a terminal is a variable or function (starts with "V_" or "F_")
        private boolean isVariableOrFunction(String varName) {
            return varName.startsWith("V_") || isFunction(varName);
        }

        // Helper function to check if it's a function (starts with "F_")
        private boolean isFunction(String varName) {
            return varName.startsWith("F_");
        }

        // Print the symbol table for the global scope
        public void printGlobalSymbolTable() {
            if (!scopeStack.isEmpty()) {
                SymbolTable globalScope = scopeStack.firstElement(); // Get the global (first) scope
                System.out.println("\n=== Final Global Symbol Table ===");
                for (String varName : globalScope.getVariables()) {
                    System.out.println(varName + " : " + uniqueNames.get(varName));
                }
            } else {
                System.err.println("Error: No scope to print the global symbol table.");
            }
        }

        // Parsing XML to custom Node structure
        public Node parseXML(org.w3c.dom.Node xmlNode) {
            String nodeName = xmlNode.getNodeName();
            String varName = "";

            if (nodeName.equals("TERMINAL")) {
                varName = xmlNode.getTextContent().trim();
            }

            List<Node> children = new ArrayList<>();
            NodeList xmlChildren = xmlNode.getChildNodes();
            for (int i = 0; i < xmlChildren.getLength(); i++) {
                if (xmlChildren.item(i).getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                    children.add(parseXML(xmlChildren.item(i)));
                }
            }

            return new Node(nodeName, varName, children);
        }

        // Print the current symbol table
        private void printCurrentSymbolTable() {
            if (!scopeStack.isEmpty()) {
                SymbolTable currentScope = scopeStack.peek();
                System.out.println("=== Symbol Table for current scope ===");
                currentScope.printTable();
            } else {
                System.err.println("Error: Attempted to print symbol table for an empty scope.");
            }
        }
    }

    // SymbolTable class
    static class SymbolTable {
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

    // Node class
    static class Node {
        private String nodeType;
        private String varName;
        private List<Node> children; // List of child nodes

        public Node(String nodeType, String varName, List<Node> children) {
            this.nodeType = nodeType;
            this.varName = varName;
            this.children = children;
        }

        public String getType() {
            return nodeType;
        }

        public String getVarName() {
            return varName;
        }

        public List<Node> getChildren() {
            return children;
        }

        // Example check to determine if the node is an assignment
        public boolean isAssignment() {
            return nodeType.equals("ASSIGN") || varName.equals("=");
        }

        // Block start/end methods can be defined as per your grammar
        public boolean isBlockStart() {
            return varName.equals("begin");
        }

        public boolean isBlockEnd() {
            return varName.equals("end");
        }
    }

    // Main method
    public static void main(String[] args) {
        try {
            // Parse the XML file
            System.out.println("Parsing XML syntax tree...");
            File inputFile = new File("parser.xml");
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();

            // Parse the XML into a custom node structure
            ScopeAnalyzer analyzer = new ScopeAnalyzer();
            Node root = analyzer.parseXML(doc.getDocumentElement());
            System.out.println("Parsing completed.");
            System.out.println("Root node parsed: " + root.getVarName());

            // Analyze the scope
            System.out.println("Analyzing scope...");
            analyzer.analyzeNode(root);
            
            // Print final global symbol table
            analyzer.printGlobalSymbolTable();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

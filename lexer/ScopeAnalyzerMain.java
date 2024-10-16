import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import java.util.Map;
import java.util.HashMap;
import org.w3c.dom.NodeList;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.File;

public class ScopeAnalyzerMain {
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
            NodeType root = analyzer.parseXML(doc.getDocumentElement());
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

class NodeType {
    private String nodeType;
    private String varName;
    private List<NodeType> children;

    public NodeType(String nodeType, String varName, List<NodeType> children) {
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

    public List<NodeType> getChildren() {
        return children;
    }

    public boolean isAssignment() {
        return nodeType.equals("ASSIGN") || varName.equals("=");
    }

    public boolean isBlockStart() {
        return varName.equals("begin");
    }

    public boolean isBlockEnd() {
        return varName.equals("end");
    }
}

class SymbolTable {
    private Set<String> variables;

    public SymbolTable() {
        variables = new HashSet<>();
    }

    public void add(String varName) {
        variables.add(varName);
    }

    public boolean contains(String varName) {
        return variables.contains(varName);
    }

    public Set<String> getVariables() {
        return variables;
    }

    public void printTable() {
        System.out.println("Variables: " + variables);
    }
}

class ScopeAnalyzer {
    private Stack<SymbolTable> scopeStack;
    private int varCounter;
    private int funcCounter;
    private Map<String, String> uniqueNames;
    private boolean stopOnError;
    private Set<String> declaredFunctions;
    private Set<String> usedFunctions;
    private Map<String, Set<String>> functionScopes;

    public ScopeAnalyzer() {
        scopeStack = new Stack<>();
        scopeStack.push(new SymbolTable()); // Global scope
        varCounter = 100;
        funcCounter = 500;
        uniqueNames = new HashMap<>();
        stopOnError = false;
        declaredFunctions = new HashSet<>();
        usedFunctions = new HashSet<>();
        functionScopes = new HashMap<>();
    }

    public void enterScope(String scopeName) {
        System.out.println("Entering scope: " + scopeName);
        scopeStack.push(new SymbolTable());
        if (scopeName.startsWith("F_")) {
            functionScopes.put(scopeName, new HashSet<>());
        }
        printCurrentSymbolTable();
    }

    public void exitScope() {
        if (!scopeStack.isEmpty() && scopeStack.size() > 1) {
            printCurrentSymbolTable();
            System.out.println("Exiting the current scope.");
            scopeStack.pop();
            printCurrentSymbolTable();
        } else {
            System.err.println("Finished all scopes.");
        }
    }

    private String generateUniqueVarName() {
        return "v" + (varCounter++);
    }

    private String generateUniqueFuncName() {
        return "f" + (funcCounter++);
    }

    public boolean declare(String varName, String currentFunction) {
        SymbolTable currentScope = scopeStack.peek();

        if (currentScope.contains(varName)) {
            System.err.println("Error: Variable '" + varName + "' is already declared in this scope.");
            stopOnError = true;
            return false;
        }

        String uniqueName = isFunction(varName) ? generateUniqueFuncName() : generateUniqueVarName();
        uniqueNames.put(varName, uniqueName);
        System.out.println(
                "Declaring " + (isFunction(varName) ? "function" : "variable") + ": " + varName + " as " + uniqueName);

        currentScope.add(varName);
        if (currentFunction != null && functionScopes.containsKey(currentFunction)) {
            functionScopes.get(currentFunction).add(varName);
        }
        if (isFunction(varName)) {
            declaredFunctions.add(varName);
        }
        return true;
    }

    public boolean containsInCurrentScope(String varName) {
        return scopeStack.peek().contains(varName);
    }

    public boolean containsInAnyScope(String varName) {
        for (SymbolTable scope : scopeStack) {
            if (scope.contains(varName)) {
                return true;
            }
        }
        return false;
    }

    public boolean checkUsage(String varName, String currentFunction) {
        for (SymbolTable scope : scopeStack) {
            if (scope.contains(varName)) {
                String uniqueName = uniqueNames.get(varName);
                System.out.println("Using " + (isFunction(varName) ? "function" : "variable") + ": " + varName + " as "
                        + uniqueName);
                if (isFunction(varName)) {
                    usedFunctions.add(varName);
                }
                return true;
            }
        }
        System.err.println("Error: Variable or function '" + varName + "' is not declared in the current scope.");
        stopOnError = true;
        return false;
    }

    public void analyzeNode(NodeType node) {
        analyzeNodeFirstPass(node, null);
        stopOnError = false; // Reset error flag for second pass
        analyzeNodeSecondPass(node, null);
    }

    private void analyzeNodeFirstPass(NodeType node, String currentFunction) {
        if (stopOnError)
            return;

        if (node.isBlockStart()) {
            enterScope(node.getVarName());
        }

        if (node.getType().equals("TERMINAL") && !node.getVarName().isEmpty()) {
            if (isVariableOrFunction(node.getVarName())) {
                if (!node.isAssignment() && !containsInCurrentScope(node.getVarName())) {
                    declare(node.getVarName(), currentFunction);
                }
            }
        }

        if (node.getType().equals("FNAME")) {
            currentFunction = node.getVarName();
            declare(currentFunction, null);
        }

        for (NodeType child : node.getChildren()) {
            analyzeNodeFirstPass(child, currentFunction);
        }

        if (node.isBlockEnd()) {
            exitScope();
        }
    }

    private void analyzeNodeSecondPass(NodeType node, String currentFunction) {
        if (stopOnError)
            return;

        if (node.isBlockStart()) {
            enterScope(node.getVarName());
        }

        if (node.getType().equals("TERMINAL") && !node.getVarName().isEmpty()) {
            if (isVariableOrFunction(node.getVarName())) {
                if (node.isAssignment() || !containsInCurrentScope(node.getVarName())) {
                    checkUsage(node.getVarName(), currentFunction);
                }
            }
        }

        if (node.getType().equals("FNAME")) {
            currentFunction = node.getVarName();
        }

        for (NodeType child : node.getChildren()) {
            analyzeNodeSecondPass(child, currentFunction);
        }

        if (node.isBlockEnd()) {
            exitScope();
        }
    }

    private boolean isVariableOrFunction(String varName) {
        return varName.startsWith("V_") || isFunction(varName);
    }

    private boolean isFunction(String varName) {
        return varName.startsWith("F_");
    }

    public void printGlobalSymbolTable() {
        if (!scopeStack.isEmpty()) {
            SymbolTable globalScope = scopeStack.firstElement();
            System.out.println("\n=== Final Global Symbol Table ===");
            for (String varName : globalScope.getVariables()) {
                System.out.println(varName + " : " + uniqueNames.get(varName));
            }
        } else {
            System.err.println("Error: No scope to print the global symbol table.");
        }
    }

    public NodeType parseXML(org.w3c.dom.Node xmlNode) {
        String nodeName = xmlNode.getNodeName();
        String varName = "";

        if (nodeName.equals("TERMINAL")) {
            varName = xmlNode.getTextContent().trim();
        }

        List<NodeType> children = new ArrayList<>();
        NodeList xmlChildren = xmlNode.getChildNodes();
        for (int i = 0; i < xmlChildren.getLength(); i++) {
            if (xmlChildren.item(i).getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                children.add(parseXML(xmlChildren.item(i)));
            }
        }

        return new NodeType(nodeName, varName, children);
    }

    private void printCurrentSymbolTable() {
        if (!scopeStack.isEmpty()) {
            SymbolTable currentScope = scopeStack.peek();
            System.out.println("=== Symbol Table for current scope ===");
            currentScope.printTable();
        } else {
            System.err.println("Error: Attempted to print symbol table for an empty scope.");
        }
    }

    public void checkFunctionUsage() {
        Set<String> undeclaredFunctions = new HashSet<>(usedFunctions);
        undeclaredFunctions.removeAll(declaredFunctions);

        if (!undeclaredFunctions.isEmpty()) {
            for (String func : undeclaredFunctions) {
                System.err.println("Error: Function '" + func + "' is used but not declared.");
            }
            stopOnError = true;
        }
    }

    public boolean hasError() {
        return stopOnError;
    }
}

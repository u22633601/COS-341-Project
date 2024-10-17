import java.io.File;
import java.util.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;

public class DualScopeAnalyzer {
    public static void main(String[] args) {
        try {
            // Parse the XML file
            //System.out.println("Parsing XML syntax tree...");
            File inputFile = new File("parser.xml");
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();

            // Run ScopeAnalyzer1
            System.out.println("\n=== Running ScopeAnalyzer1 ===");
            ScopeAnalyzer1 analyzer1 = new ScopeAnalyzer1();
            ScopeAnalyzer1.Node root1 = analyzer1.parseXML(doc.getDocumentElement());
            //System.out.println("Parsing completed for ScopeAnalyzer1.");
            //System.out.println("Root node parsed: " + root1.getVarName());
            //System.out.println("Analyzing scope with ScopeAnalyzer1...");
            if (analyzer1.analyzeNode(root1)) {
                analyzer1.printGlobalSymbolTable();
                // Run ScopeAnalyzer2
                System.out.println("\n=== Running ScopeAnalyzer2 ===");
                ScopeAnalyzer2 analyzer2 = new ScopeAnalyzer2();
                ScopeAnalyzer2.NodeType root2 = analyzer2.parseXML(doc.getDocumentElement());
                // System.out.println("Parsing completed for ScopeAnalyzer2.");
                // System.out.println("Root node parsed: " + root2.getVarName());
                // System.out.println("Analyzing scope with ScopeAnalyzer2...");
                if (analyzer2.analyzeNode(root2)) {
                    analyzer2.printGlobalSymbolTable();
                } else {
                    System.err.println("ScopeAnalyzer2 encountered an error and stopped execution.");
                }
            } else {
                System.err.println("ScopeAnalyzer1 encountered an error and stopped execution.");
            }

            

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ScopeAnalyzer1 implementation
    static class ScopeAnalyzer1 {
        private Stack<SymbolTable> scopeStack;
        private int varCounter;
        private int funcCounter;
        private Map<String, String> uniqueNames;
        private boolean stopOnError;

        public ScopeAnalyzer1() {
            scopeStack = new Stack<>();
            scopeStack.push(new SymbolTable());
            varCounter = 100;
            funcCounter = 500;
            uniqueNames = new HashMap<>();
            stopOnError = false;
        }

        public void enterScope() {
            //System.out.println("Entering a new scope.");
            scopeStack.push(new SymbolTable());
            printCurrentSymbolTable();
        }

        public void exitScope() {
            if (!scopeStack.isEmpty() && scopeStack.size() > 1) {
                printCurrentSymbolTable();
                //System.out.println("Exiting the current scope.");
                scopeStack.pop();
                printCurrentSymbolTable();
            } else {
                //System.err.println("Error: Attempted to exit scope, but no non-global scope exists.");
            }
        }

        private String generateUniqueVarName() {
            return "v" + (varCounter++);
        }

        private String generateUniqueFuncName() {
            return "f" + (funcCounter++);
        }

        public boolean declare(String varName) {
            SymbolTable currentScope = scopeStack.peek();

            if (currentScope.contains(varName)) {
                System.err.println("Error: Variable '" + varName + "' is already declared in this scope.");
                stopOnError = true;
                return false;
            }

            if (containsInAnyScope(varName)) {
                //System.out.println("Using existing variable from outer scope: " + varName + " as " + uniqueNames.get(varName));
                return true;
            }

            String uniqueName = isFunction(varName) ? generateUniqueFuncName() : generateUniqueVarName();
            uniqueNames.put(varName, uniqueName);
            //System.out.println("Declaring " + (isFunction(varName) ? "function" : "variable") + ": " + varName + " as " + uniqueName);
            
            currentScope.add(varName);
            return true;
        }

        public boolean containsInAnyScope(String varName) {
            for (SymbolTable scope : scopeStack) {
                if (scope.contains(varName)) {
                    return true;
                }
            }
            return false;
        }

        public boolean checkUsage(String varName) {
            for (SymbolTable scope : scopeStack) {
                if (scope.contains(varName)) {
                    String uniqueName = uniqueNames.get(varName);
                    System.out.println("Using " + (isFunction(varName) ? "function" : "variable") + ": " + varName + " as " + uniqueName);
                    return true;
                }
            }
            System.err.println("Error: Variable or function '" + varName + "' is not declared in the current scope.");
            stopOnError = true;
            return false;
        }

        public boolean analyzeNode(Node node) {
            if (node.isBlockStart()) {
                enterScope();
            }
            if (node.isBlockEnd()) {
                exitScope();
            }

            if (node.getType().equals("TERMINAL") && !node.getVarName().isEmpty()) {
                if (isVariableOrFunction(node.getVarName())) {
                    if (node.isAssignment()) {
                        if (!checkUsage(node.getVarName())) {
                            return false; // Stop execution on error
                        }
                        System.out.println("Assigning value to " + node.getVarName());
                    } else if (scopeStack.peek().contains(node.getVarName())) {
                        System.err.println("Error: Variable/Function '" + node.getVarName()
                                + "' is already declared in this scope.");
                        return false; // Stop execution on error
                    } else {
                        if (!declare(node.getVarName())) {
                            return false; // Stop execution on error
                        }
                    }
                } else {
                    //System.out.println("Ignoring terminal: " + node.getVarName());
                }
            }

            for (Node child : node.getChildren()) {
                if (!analyzeNode(child)) {
                    return false; // Stop execution if error in child node
                }
            }

            return true; // No errors encountered
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
                System.out.println("\n=== Final Global Symbol Table (ScopeAnalyzer1) ===");
                for (String varName : globalScope.getVariables()) {
                    System.out.println(varName + " : " + uniqueNames.get(varName));
                }
            } else {
                System.err.println("Error: No scope to print the global symbol table.");
            }
        }

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

        private void printCurrentSymbolTable() {
            if (!scopeStack.isEmpty()) {
                SymbolTable currentScope = scopeStack.peek();
                //System.out.println("=== Symbol Table for current scope ===");
                currentScope.printTable();
            } else {
                System.err.println("Error: Attempted to print symbol table for an empty scope.");
            }
        }

        static class SymbolTable {
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
                //System.out.println("Variables: " + variables);
            }
        }

        static class Node {
            private String nodeType;
            private String varName;
            private List<Node> children;

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
    }

    // ScopeAnalyzer2 implementation
    static class ScopeAnalyzer2 {
        private Stack<SymbolTable> scopeStack;
        private int varCounter;
        private int funcCounter;
        private Map<String, String> uniqueNames;
        private boolean stopOnError;
        private Set<String> declaredFunctions;
        private Set<String> usedFunctions;
        private Map<String, Set<String>> functionScopes;

        public ScopeAnalyzer2() {
            scopeStack = new Stack<>();
            scopeStack.push(new SymbolTable());
            varCounter = 100;
            funcCounter = 500;
            uniqueNames = new HashMap<>();
            stopOnError = false;
            declaredFunctions = new HashSet<>();
            usedFunctions = new HashSet<>();
            functionScopes = new HashMap<>();
        }

        public void enterScope(String scopeName) {
            //System.out.println("Entering scope: " + scopeName);
            scopeStack.push(new SymbolTable());
            if (scopeName.startsWith("F_")) {
                functionScopes.put(scopeName, new HashSet<>());
            }
            printCurrentSymbolTable();
        }

        public void exitScope() {
            if (!scopeStack.isEmpty() && scopeStack.size() > 1) {
                printCurrentSymbolTable();
                //System.out.println("Exiting the current scope.");
                scopeStack.pop();
                printCurrentSymbolTable();
            } else {
                //System.err.println("Finished all scopes.");
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
            //System.out.println("Declaring " + (isFunction(varName) ? "function" : "variable") + ": " + varName + " as " + uniqueName);

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
                    //System.out.println("Using " + (isFunction(varName) ? "function" : "variable") + ": " + varName + " as " + uniqueName);
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

        public boolean analyzeNode(NodeType node) {
            if (!analyzeNodeFirstPass(node, null)) {
                return false; // Stop if error in first pass
            }
            return analyzeNodeSecondPass(node, null);
        }

        private boolean analyzeNodeFirstPass(NodeType node, String currentFunction) {
            if (node.isBlockStart()) {
                enterScope(node.getVarName());
            }

            if (node.getType().equals("TERMINAL") && !node.getVarName().isEmpty()) {
                if (isVariableOrFunction(node.getVarName())) {
                    if (!node.isAssignment() && !containsInCurrentScope(node.getVarName())) {
                        if (!declare(node.getVarName(), currentFunction)) {
                            return false; // Stop execution on error
                        }
                    }
                }
            }

            if (node.getType().equals("FNAME")) {
                currentFunction = node.getVarName();
                if (!declare(currentFunction, null)) {
                    return false; // Stop execution on error
                }
            }

            for (NodeType child : node.getChildren()) {
                if (!analyzeNodeFirstPass(child, currentFunction)) {
                    return false; // Stop execution if error in child node
                }
            }

            if (node.isBlockEnd()) {
                exitScope();
            }

            return true; // No errors encountered
        }
        
        private boolean analyzeNodeSecondPass(NodeType node, String currentFunction) {
            if (node.isBlockStart()) {
                enterScope(node.getVarName());
            }

            if (node.getType().equals("TERMINAL") && !node.getVarName().isEmpty()) {
                if (isVariableOrFunction(node.getVarName())) {
                    if (node.isAssignment() || !containsInCurrentScope(node.getVarName())) {
                        if (!checkUsage(node.getVarName(), currentFunction)) {
                            return false; // Stop execution on error
                        }
                    }
                }
            }

            if (node.getType().equals("FNAME")) {
                currentFunction = node.getVarName();
            }

            for (NodeType child : node.getChildren()) {
                if (!analyzeNodeSecondPass(child, currentFunction)) {
                    return false; // Stop execution if error in child node
                }
            }

            if (node.isBlockEnd()) {
                exitScope();
            }

            return true; // No errors encountered
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
                System.out.println("\n=== Final Global Symbol Table (ScopeAnalyzer2) ===");
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
                //System.out.println("=== Symbol Table for current scope ===");
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

        static class SymbolTable {
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
                //System.out.println("Variables: " + variables);
            }
        }

        static class NodeType {
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
    }
}
import java.io.*;
import java.util.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;

class SymbolInfo {
    String name;
    String type;
    String kind;
    int scope;

    public SymbolInfo(String name, String type, String kind, int scope) {
        this.name = name;
        this.type = type;
        this.kind = kind;
        this.scope = scope;
    }

    @Override
    public String toString() {
        return String.format("Name: %s, Type: %s, Kind: %s, Scope: %d", name, type, kind, scope);
    }
}

class SymbolTable {
    private Map<Integer, Map<String, SymbolInfo>> scopes;
    private int currentScope;
    private Map<Integer, Set<String>> siblingScopes;

    public SymbolTable() {
        scopes = new HashMap<>();
        currentScope = 0;
        scopes.put(currentScope, new HashMap<>());
        siblingScopes = new HashMap<>();
        siblingScopes.put(currentScope, new HashSet<>());
    }

    public void enterScope() {
        currentScope++;
        scopes.put(currentScope, new HashMap<>());
        siblingScopes.put(currentScope, new HashSet<>());
        System.out.println("Entering scope: " + currentScope);
    }

    public void exitScope() {
        System.out.println("Exiting scope: " + currentScope);
        scopes.remove(currentScope);
        siblingScopes.remove(currentScope);
        currentScope--;
    }

    public void addSymbol(String name, String type, String kind) {
        SymbolInfo info = new SymbolInfo(name, type, kind, currentScope);
        scopes.get(currentScope).put(name, info);
        siblingScopes.get(currentScope).add(name);
        System.out.println("Adding symbol: " + info);
    }

    public SymbolInfo lookupSymbol(String name) {
        for (int i = currentScope; i >= 0; i--) {
            SymbolInfo info = scopes.get(i).get(name);
            if (info != null) {
                return info;
            }
        }
        return null;
    }

    public int getCurrentScope() {
        return currentScope;
    }

    public boolean isInCurrentScope(String name) {
        return scopes.get(currentScope).containsKey(name);
    }

    public boolean isSiblingScope(String name) {
        return siblingScopes.get(currentScope).contains(name);
    }
}

// SymbolInfo and TreeNode classes remain the same

public class SemanticAnalyzer {
    private SymbolTable symbolTable;
    private TreeNode root;
    private List<String> errors;
    private Set<String> reservedKeywords;
    private Set<String> declaredVariables;
    private Set<String> usedVariables;

    public SemanticAnalyzer(TreeNode root) {
        this.root = root;
        this.symbolTable = new SymbolTable();
        this.errors = new ArrayList<>();
        this.reservedKeywords = new HashSet<>(Arrays.asList(
                "main", "begin", "end", "if", "then", "else", "while", "do", "num", "text", "void",
                "halt", "return", "input", "output", "and", "or", "not", "eq", "grt", "add", "sub", "mul", "div"));
        this.declaredVariables = new HashSet<>();
        this.usedVariables = new HashSet<>();
    }

    public void analyze() {
        System.out.println("Starting semantic analysis...");
        analyzeNode(root);
        checkUndeclaredVariables();
        System.out.println("Semantic analysis completed.");
    }

    private void analyzeNode(TreeNode node) {
        System.out.println("Analyzing node: " + node.symbol);

        switch (node.symbol) {
            case "PROG":
                symbolTable.enterScope();
                symbolTable.addSymbol("main", "void", "function");
                break;
            case "GLOBVARS":
                analyzeGlobalVariables(node);
                break;
            case "FUNCTIONS":
                analyzeFunctions(node);
                break;
            case "VNAME":
                if (node.children.size() > 0 && node.children.get(0).symbol.startsWith("V_")) {
                    String varName = node.children.get(0).symbol;
                    checkVariableNameRules(varName);
                    usedVariables.add(varName);
                }
                break;
            case "FNAME":
                if (node.children.size() > 0 && node.children.get(0).symbol.startsWith("F_")) {
                    checkFunctionNameRules(node.children.get(0).symbol);
                }
                break;
        }

        for (TreeNode child : node.children) {
            analyzeNode(child);
        }

        if (node.symbol.equals("PROG") || node.symbol.equals("DECL")) {
            symbolTable.exitScope();
        }
    }

    private void analyzeGlobalVariables(TreeNode node) {
        System.out.println("Analyzing global variables...");
        String currentType = null;
        for (TreeNode child : node.children) {
            if (child.symbol.equals("VTYP")) {
                currentType = child.children.get(0).symbol;
            } else if (child.symbol.equals("VNAME")) {
                String varName = child.children.get(0).symbol;
                checkVariableNameRules(varName);
                symbolTable.addSymbol(varName, currentType, "variable");
                declaredVariables.add(varName);
            }
        }
    }

    private void analyzeFunctions(TreeNode node) {
        System.out.println("Analyzing functions...");
        for (TreeNode child : node.children) {
            if (child.symbol.equals("DECL")) {
                analyzeFunction(child);
            }
        }
    }

    private void analyzeFunction(TreeNode node) {
        TreeNode header = findChildBySymbol(node, "HEADER");
        if (header != null) {
            TreeNode ftyp = findChildBySymbol(header, "FTYP");
            TreeNode fname = findChildBySymbol(header, "FNAME");
            if (ftyp != null && fname != null) {
                String functionName = fname.children.get(0).symbol;
                String returnType = ftyp.children.get(0).symbol;
                checkFunctionNameRules(functionName);
                symbolTable.addSymbol(functionName, returnType, "function");
                symbolTable.enterScope();

                // Analyze parameters
                List<TreeNode> params = findChildrenBySymbol(header, "VNAME");
                for (TreeNode param : params) {
                    String paramName = param.children.get(0).symbol;
                    checkVariableNameRules(paramName);
                    symbolTable.addSymbol(paramName, "parameter", "variable");
                    declaredVariables.add(paramName);
                }

                // Analyze function body
                TreeNode body = findChildBySymbol(node, "BODY");
                if (body != null) {
                    analyzeNode(body);
                }
            }
        }
    }

    private void checkFunctionNameRules(String functionName) {
        System.out.println("Checking function name rules for: " + functionName);

        // Rule 3: A child scope may not have the same name as its immediate parent
        // scope
        if (symbolTable.lookupSymbol(functionName) != null) {
            errors.add("Error: Function '" + functionName + "' has the same name as a parent scope.");
            return;
        }

        // Rule 4: A child scope may not have the same name as any of its sibling scopes
        if (symbolTable.isSiblingScope(functionName)) {
            errors.add("Error: Function '" + functionName + "' has the same name as a sibling scope.");
            return;
        }

        // Rule 7: There may be no recursive call to main
        if (functionName.equals("main") && symbolTable.getCurrentScope() > 0) {
            errors.add("Error: Recursive call to 'main' is not allowed.");
            return;
        }

        symbolTable.addSymbol(functionName, "function", "function");
    }

    private void checkVariableNameRules(String variableName) {
        System.out.println("Checking variable name rules for: " + variableName);

        // Rule 9: No variable name may be double-declared in the same scope
        if (symbolTable.isInCurrentScope(variableName)) {
            errors.add("Error: Variable '" + variableName + "' is already declared in this scope.");
            return;
        }

        // Rule 13: No variable anywhere in the RecSPL program may have a name that is
        // also used as a Function name
        SymbolInfo existingSymbol = symbolTable.lookupSymbol(variableName);
        if (existingSymbol != null && existingSymbol.kind.equals("function")) {
            errors.add("Error: Variable name '" + variableName + "' conflicts with an existing function name.");
            return;
        }

        // Rule 14: No variable name anywhere in the RecSPL program may be identical
        // with any Reserved Keyword
        if (reservedKeywords.contains(variableName)) {
            errors.add("Error: Variable name '" + variableName + "' is a reserved keyword.");
            return;
        }

        // If all checks pass, add the variable to the symbol table
        // Note: We're not specifying the type here, as it would come from a VTYP node
        symbolTable.addSymbol(variableName, "unknown", "variable");
        declaredVariables.add(variableName);
    }

    private void checkUndeclaredVariables() {
        // Rule 12: Every used variable name must have a declaration
        for (String usedVar : usedVariables) {
            if (!declaredVariables.contains(usedVar)) {
                errors.add("Error: Variable '" + usedVar + "' is used but not declared.");
            }
        }
    }

    private TreeNode findChildBySymbol(TreeNode parent, String symbol) {
        for (TreeNode child : parent.children) {
            if (child.symbol.equals(symbol)) {
                return child;
            }
        }
        return null;
    }

    private List<TreeNode> findChildrenBySymbol(TreeNode parent, String symbol) {
        List<TreeNode> result = new ArrayList<>();
        for (TreeNode child : parent.children) {
            if (child.symbol.equals(symbol)) {
                result.add(child);
            }
        }
        return result;
    }

    public List<String> getErrors() {
        return errors;
    }

    public static void main(String[] args) {
        try {
            // Parse the XML file
            File inputFile = new File("parser.xml");
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();

            // Build the syntax tree
            TreeNode root = buildTreeFromXML(doc.getDocumentElement());

            // Create and run the semantic analyzer
            SemanticAnalyzer analyzer = new SemanticAnalyzer(root);
            analyzer.analyze();

            // Print any errors
            List<String> errors = analyzer.getErrors();
            if (errors.isEmpty()) {
                System.out.println("No semantic errors found.");
            } else {
                System.out.println("Semantic errors found:");
                for (String error : errors) {
                    System.out.println(error);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static TreeNode buildTreeFromXML(Element element) {
        int id = Integer.parseInt(element.getElementsByTagName("UNID").item(0).getTextContent());
        String symbol = element.getAttribute("symbol");
        if (symbol.isEmpty()) {
            symbol = element.getTagName();
        }

        TreeNode node = new TreeNode(id, symbol);

        NodeList children = element.getElementsByTagName("CHILDREN").item(0).getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i).getNodeType() == Node.ELEMENT_NODE) {
                Element childElement = (Element) children.item(i);
                int childId = Integer.parseInt(childElement.getTextContent());
                Element childNodeElement = findElementById(element.getOwnerDocument(), childId);
                if (childNodeElement != null) {
                    node.addChild(buildTreeFromXML(childNodeElement));
                }
            }
        }

        return node;
    }

    private static Element findElementById(Document doc, int id) {
        NodeList allElements = doc.getElementsByTagName("*");
        for (int i = 0; i < allElements.getLength(); i++) {
            Element element = (Element) allElements.item(i);
            NodeList unidNodes = element.getElementsByTagName("UNID");
            if (unidNodes.getLength() > 0) {
                int elementId = Integer.parseInt(unidNodes.item(0).getTextContent());
                if (elementId == id) {
                    return element;
                }
            }
        }
        return null;
    }
}
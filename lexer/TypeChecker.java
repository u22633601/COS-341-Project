import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.*;
import java.util.*;

public class TypeChecker {

    private static Map<String, String> symbolTable = new HashMap<>();

    private static void loadSymbolTable(String filePath) throws IOException {
        System.out.println("Loading symbol table...");
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(" : ");
                if (parts.length == 3) {
                    symbolTable.put(parts[0].trim(), parts[2].trim());
                    System.out.println("Loaded: " + parts[0].trim() + " -> " + parts[2].trim());
                }
            }
        }
    }

    private static String getType(String symbol) {
        String type = symbolTable.getOrDefault(symbol, "u");
        System.out.println("Getting type for '" + symbol + "': " + type);
        return type;
    }

    private static boolean typeCheck(Node node) {
        if (node == null) {
            System.out.println("Encountered a null node.");
            return true;
        }

        String nodeName = node.getNodeName();
        System.out.println("Visiting node: " + nodeName);

        switch (nodeName) {
            case "SYNTREE":
            case "ROOT":
            case "INNERNODES":
            case "LEAFNODES":
            case "PROG":
            case "GLOBVARS":
            case "ALGO":
            case "FUNCTIONS":
            case "INSTRUC":
                return typeCheckChildren(node);
            case "IN":
            case "LEAF":
            case "COMMAND":
            case "ASSIGN":
            case "CALL":
            case "BRANCH":
            case "TERM":
            case "OP":
            case "COND":
                return typeCheckNode(node);
            default:
                System.out.println("Unhandled node type: " + nodeName);
                return true;
        }
    }

    private static boolean typeCheckChildren(Node parent) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                if (!typeCheck(child)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean typeCheckNode(Node node) {
        Node symbNode = getChildByTag(node, "SYMB");
        if (symbNode == null) {
            symbNode = getChildByTag(node, "TERMINAL");
        }

        if (symbNode == null) {
            System.out.println("No SYMB or TERMINAL found for node: " + node.getNodeName());
            return true;
        }

        String symbol = symbNode.getTextContent().trim();
        System.out.println("Checking symbol: " + symbol);

        switch (symbol) {
            case "ASSIGN":
                return typeCheckAssign(node);
            case "CALL":
                return typeCheckCall(node);
            case "BRANCH":
                return typeCheckBranch(node);
            case "BINOP":
                return typeCheckBinOp(node);
            case "UNOP":
                return typeCheckUnOp(node);
            case "RETURN":
                return typeCheckReturn(node);
            default:
                System.out.println("Unhandled symbol: " + symbol);
                return true;
        }
    }

    private static boolean typeCheckAssign(Node node) {
        String varName = getVName(node);
        String varType = getType(varName);
        String termType = getTermType(getChildByTag(node, "TERM"));
        System.out.println("Checking assignment: " + varName + " (" + varType + ") = " + termType);
        return varType.equals(termType);
    }

    private static boolean typeCheckCall(Node node) {
        String funcName = getFName(node);
        String funcType = getType(funcName);
        System.out.println("Checking function call: " + funcName + " -> " + funcType);
        
        // Check if all arguments are numeric
        NodeList args = node.getChildNodes();
        for (int i = 0; i < args.getLength(); i++) {
            Node arg = args.item(i);
            if (arg.getNodeName().equals("ARG")) {
                String argType = getType(getVName(arg));
                if (!argType.equals("n")) {
                    return false;
                }
            }
        }
        
        return funcType.equals("v") || funcType.equals("n");
    }

    private static boolean typeCheckBranch(Node node) {
        Node cond = getChildByTag(node, "COND");
        String condType = getCondType(cond);
        System.out.println("Branch condition type: " + condType);
        return condType.equals("b") && typeCheckChildren(node);
    }

    private static boolean typeCheckBinOp(Node node) {
        Node arg1Node = getChildByTag(node, "ARG1");
        Node arg2Node = getChildByTag(node, "ARG2");

        if (arg1Node == null || arg2Node == null) {
            System.out.println("Argument nodes are null for binary operation.");
            return false;
        }

        String leftType = getArgType(arg1Node);
        String rightType = getArgType(arg2Node);
        String binop = getNodeSymbol(node);

        System.out.println("Checking binary operation: " + binop +
                " (left: " + leftType + ", right: " + rightType + ")");

        if (Arrays.asList("add", "sub", "mul", "div").contains(binop)) {
            return leftType.equals("n") && rightType.equals("n");
        } else if (Arrays.asList("grt", "eq").contains(binop)) {
            return leftType.equals("n") && rightType.equals("n");
        } else if (Arrays.asList("and", "or").contains(binop)) {
            return leftType.equals("b") && rightType.equals("b");
        }
        return false;
    }

    private static boolean typeCheckUnOp(Node node) {
        Node argNode = getChildByTag(node, "ARG");
        String argType = getArgType(argNode);
        String unop = getNodeSymbol(node);

        System.out.println("Checking unary operation: " + unop + " (arg: " + argType + ")");

        return (unop.equals("sqrt") && argType.equals("n")) ||
               (unop.equals("not") && argType.equals("b"));
    }

    private static boolean typeCheckReturn(Node node) {
        Node atomicNode = getChildByTag(node, "ATOMIC");
        String atomicType = getType(getVName(atomicNode));
        
        // Find the enclosing function's return type
        Node funcNode = findEnclosingFunction(node);
        if (funcNode != null) {
            Node headerNode = getChildByTag(funcNode, "HEADER");
            Node ftypNode = getChildByTag(headerNode, "FTYP");
            String funcReturnType = ftypNode.getTextContent().trim();
            
            System.out.println("Checking return: " + atomicType + " vs function return type: " + funcReturnType);
            return (atomicType.equals("n") && funcReturnType.equals("num")) ||
                   (atomicType.equals("v") && funcReturnType.equals("void"));
        }
        
        return false;
    }

    private static Node findEnclosingFunction(Node node) {
        while (node != null && !node.getNodeName().equals("DECL")) {
            node = node.getParentNode();
        }
        return node;
    }

    private static String getVName(Node node) {
        Node vnameNode = getChildByTag(node, "VNAME");
        if (vnameNode == null) {
            System.out.println("No VNAME node found.");
            return "";
        }
        return getLeafValue(vnameNode);
    }

    private static String getFName(Node node) {
        return getLeafValue(getChildByTag(node, "FNAME"));
    }

    private static Node getChildByTag(Node node, String tagName) {
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().equals(tagName)) {
                return child;
            }
        }
        return null;
    }

    private static String getLeafValue(Node node) {
        return node != null ? node.getTextContent().trim() : "";
    }

    private static String getNodeSymbol(Node node) {
        Node symbNode = getChildByTag(node, "SYMB");
        return symbNode != null ? symbNode.getTextContent().trim() : "";
    }

    private static String getTermType(Node termNode) {
        if (termNode == null) {
            return "u";
        }
        Node childNode = termNode.getFirstChild();
        if (childNode == null) {
            return "u";
        }
        switch (childNode.getNodeName()) {
            case "ATOMIC":
                return getType(getVName(childNode));
            case "CALL":
                return getType(getFName(childNode));
            case "OP":
                return getOpType(childNode);
            default:
                return "u";
        }
    }

    private static String getArgType(Node argNode) {
        if (argNode == null) {
            return "u";
        }
        Node childNode = argNode.getFirstChild();
        if (childNode == null) {
            return "u";
        }
        switch (childNode.getNodeName()) {
            case "ATOMIC":
                return getType(getVName(childNode));
            case "OP":
                return getOpType(childNode);
            default:
                return "u";
        }
    }

    private static String getOpType(Node opNode) {
        Node unopNode = getChildByTag(opNode, "UNOP");
        if (unopNode != null) {
            String unop = getNodeSymbol(unopNode);
            return unop.equals("not") ? "b" : "n";
        }
        Node binopNode = getChildByTag(opNode, "BINOP");
        if (binopNode != null) {
            String binop = getNodeSymbol(binopNode);
            if (Arrays.asList("and", "or").contains(binop)) {
                return "b";
            } else if (Arrays.asList("grt", "eq").contains(binop)) {
                return "b";
            } else {
                return "n";
            }
        }
        return "u";
    }

    private static String getCondType(Node condNode) {
        if (condNode == null) {
            return "u";
        }
        Node childNode = condNode.getFirstChild();
        if (childNode == null) {
            return "u";
        }
        switch (childNode.getNodeName()) {
            case "SIMPLE":
            case "COMPOSIT":
                return "b";
            default:
                return "u";
        }
    }

    public static void main(String[] args) {
        try {
            loadSymbolTable("Symbol.txt");
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new File("parser.xml"));
            Node root = doc.getDocumentElement();

            boolean result = typeCheck(root);
            System.out.println("Type Checking Result: " + (result ? "Valid" : "Invalid"));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
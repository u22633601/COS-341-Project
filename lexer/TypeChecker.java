import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.*;
import java.util.*;

public class TypeChecker {

    private static Map<String, String> symbolTable = new HashMap<>();

    // Load the symbol table from Symbol.txt
    private static void loadSymbolTable(String filePath) throws IOException {
        System.out.println("Loading symbol table...");
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        String line;
        while ((line = reader.readLine()) != null) {
            String[] parts = line.split(" : ");
            if (parts.length == 3) {
                symbolTable.put(parts[0].trim(), parts[2].trim());
                System.out.println("Loaded: " + parts[0].trim() + " -> " + parts[2].trim());
            }
        }
        reader.close();
    }

    // Get the type of a symbol from the symbol table
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
                return typeCheckChildren(node);
            case "IN":
            case "LEAF":
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
            return true; // Return true or handle as needed
        }

        String symbol = symbNode.getTextContent().trim();
        System.out.println("Checking symbol: " + symbol);

        switch (symbol) {
            case "ASSIGN":
                return typeCheckAssign(node);
            case "CALL":
                return typeCheckCall(node);
            case "COND":
                return typeCheckBranch(node);
            case "BINOP":
                return typeCheckBinOp(node);
            case "UNOP":
                return typeCheckUnOp(node);
            default:
                System.out.println("Unhandled symbol: " + symbol);
                return true; // Or return false based on your logic
        }
    }

    private static boolean typeCheckAssign(Node node) {
        String varName = getVName(node);
        String varType = getType(varName);
        String termType = getType(getTerm(node));
        System.out.println("Checking assignment: " + varName + " (" + varType + ") = " + termType);
        return varType.equals(termType);
    }

    private static boolean typeCheckCall(Node node) {
        String funcName = getFName(node);
        String funcType = getType(funcName);
        System.out.println("Checking function call: " + funcName + " -> " + funcType);
        return funcType.equals("v");
    }

    private static boolean typeCheckBranch(Node node) {
        Node cond = getCond(node);
        boolean condCheck = typeCheck(cond);
        System.out.println("Branch condition valid: " + condCheck);
        return condCheck;
    }

    private static boolean typeCheckBinOp(Node node) {
        Node arg1Node = getChildByTag(node, "ARG1");
        Node arg2Node = getChildByTag(node, "ARG2");

        if (arg1Node == null || arg2Node == null) {
            System.out.println("Argument nodes are null for binary operation.");
            return false; // or handle as needed
        }

        String leftName = getVName(arg1Node);
        String rightName = getVName(arg2Node);
        String leftType = getType(leftName);
        String rightType = getType(rightName);
        String binop = getNodeSymbol(node);

        System.out.println("Checking binary operation: " + binop +
                " (" + leftName + ": " + leftType +
                ", " + rightName + ": " + rightType + ")");

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
        String argName = getVName(getChildByTag(node, "ARG"));
        String argType = getType(argName);
        String unop = getNodeSymbol(node);

        System.out.println("Checking unary operation: " + unop +
                " (" + argName + ": " + argType + ")");

        return (unop.equals("sqrt") && argType.equals("n")) ||
               (unop.equals("not") && argType.equals("b"));
    }

    private static String getVName(Node node) {
        Node vnameNode = getChildByTag(node, "VNAME");
        if (vnameNode == null) {
            System.out.println("No VNAME node found.");
            return ""; // Return empty or some default value
        }
        return getLeafValue(vnameNode);
    }

    private static String getTerm(Node node) {
        return getLeafValue(getChildByTag(node, "TERM"));
    }

    private static String getFName(Node node) {
        return getLeafValue(getChildByTag(node, "FNAME"));
    }

    private static Node getCond(Node node) {
        return getChildByTag(node, "COND");
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

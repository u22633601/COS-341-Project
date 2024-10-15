import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TypeChecker {

    // Symbol table for storing variable types
    private Map<String, String> symbolTable = new HashMap<>();

    // Set to keep track of visited nodes to prevent infinite loops
    private Set<String> visitedNodes = new HashSet<>();

    public static void main(String[] args) {
        TypeChecker checker = new TypeChecker();
        checker.checkTypes("parser.xml");
    }

    public void checkTypes(String xmlFile) {
        try {
            // Parse the XML file
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(xmlFile);

            // Normalize XML structure
            doc.getDocumentElement().normalize();

            // Start type checking from the root node
            Node rootNode = doc.getElementsByTagName("ROOT").item(0);
            if (rootNode == null) {
                System.err.println("Error: Root node is missing.");
                return;
            }

            System.out.println("Root element: " + doc.getDocumentElement().getNodeName());
            typeCheckNode(rootNode);

            System.out.println("Type checking completed successfully.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Recursive type checking for each node
    private void typeCheckNode(Node node) {
        if (node == null) {
            System.err.println("Error: Null node encountered during type checking.");
            return;
        }

        if (node.getNodeType() == Node.ELEMENT_NODE) {
            Element elem = (Element) node;

            if (elem.getTagName().equals("ID")) {
                String idValue = elem.getTextContent();
                System.out.println("Processing node with ID: " + idValue);

                if (visitedNodes.contains(idValue)) {
                    System.out.println("Node with ID " + idValue + " has already been processed. Skipping to avoid infinite loop.");
                    return;
                }

                visitedNodes.add(idValue);

                NodeList innerNodes = elem.getOwnerDocument().getElementsByTagName("IN");
                for (int i = 0; i < innerNodes.getLength(); i++) {
                    Element innerNode = (Element) innerNodes.item(i);
                    if (innerNode.getElementsByTagName("UNID").item(0).getTextContent().equals(idValue)) {
                        typeCheckNode(innerNode);
                        return;
                    }
                }

                System.out.println("No inner node found for ID: " + idValue);
                return;
            }

            String symbol = getTextContentSafely(elem, "SYMB");
            if (symbol == null) {
                System.err.println("Error: SYMB tag is missing for node. Node details: ");
                printNodeDetails(elem);
                return;
            }

            System.out.println("Type checking node with symbol: " + symbol);

            switch (symbol) {
                case "GLOBVARS":
                    checkGlobalVars(elem);
                    break;
                case "ASSIGN":
                    checkAssignment(elem);
                    break;
                case "CALL":
                    checkFunctionCall(elem);
                    break;
                case "BINOP":
                    checkOperation(elem);
                    break;
                default:
                    break;
            }

            NodeList children = elem.getElementsByTagName("CHILDREN");
            if (children == null || children.getLength() == 0) {
                System.out.println("No children found for node with symbol: " + symbol);
            } else {
                NodeList childNodes = children.item(0).getChildNodes();
                for (int i = 0; i < childNodes.getLength(); i++) {
                    Node child = childNodes.item(i);
                    if (child != null && child.getNodeType() == Node.ELEMENT_NODE) {
                        typeCheckNode(child);
                    }
                }
            }
        }
    }

    // Check global variable declarations
    private void checkGlobalVars(Element elem) {
        NodeList children = elem.getElementsByTagName("CHILDREN");
        if (children != null && children.getLength() > 0) {
            NodeList childNodes = children.item(0).getChildNodes();
            for (int i = 0; i < childNodes.getLength(); i++) {
                if (childNodes.item(i).getNodeType() == Node.ELEMENT_NODE) {
                    Element childElem = (Element) childNodes.item(i);
                    String varType = getTextContentSafely(childElem, "VTYP");
                    String varName = getTextContentSafely(childElem, "VNAME");
                    if (varType != null && varName != null) {
                        System.out.println("Adding variable " + varName + " with type " + varType + " to symbol table");
                        symbolTable.put(varName, varType);
                    }
                }
            }
        }
    }

    // Check assignments for type consistency
    private void checkAssignment(Element elem) {
        String varName = getTextContentSafely(elem, "VNAME");
        String termType = checkTerm(elem);

        if (varName == null || termType == null) {
            System.err.println("Error: Assignment node is missing VNAME or TERMINAL.");
            return;
        }

        if (!symbolTable.containsKey(varName)) {
            throw new RuntimeException("Undeclared variable: " + varName);
        }

        String varType = symbolTable.get(varName);
        if (!varType.equals(termType)) {
            throw new RuntimeException("Type mismatch for variable: " + varName + ", expected: " + varType + ", found: " + termType);
        }
    }

    // Check function calls for type consistency
    private void checkFunctionCall(Element elem) {
        String funcName = getTextContentSafely(elem, "FNAME");
        NodeList args = elem.getElementsByTagName("ARG");

        if (funcName == null) {
            System.err.println("Error: Function call node is missing FNAME.");
            return;
        }

        if (args.getLength() != 3) {
            throw new RuntimeException("Function " + funcName + " expects 3 arguments.");
        }

        String arg1Type = checkTerm(args.item(0));
        String arg2Type = checkTerm(args.item(1));
        String arg3Type = checkTerm(args.item(2));

        if (!arg1Type.equals(arg2Type) || !arg2Type.equals(arg3Type)) {
            throw new RuntimeException("Type mismatch in function " + funcName + ". Arguments must have the same type.");
        }
    }

    // Check binary operations for type consistency and parent relationship
    private void checkOperation(Element elem) {
        String operation = getTextContentSafely(elem, "BINOP");
        NodeList args = elem.getElementsByTagName("ARG");

        if (args.getLength() < 2) {
            System.err.println("Error: Binary operation " + operation + " expects two arguments.");
            return;
        }

        String leftType = checkTerm(args.item(0));
        String rightType = checkTerm(args.item(1));

        System.out.println("Operation '" + operation + "' with left type: " + leftType 
                           + " and right type: " + rightType);

        // Ensure types are compatible (i.e., both operands must have the same type)
        if (!leftType.equals(rightType)) {
            throw new RuntimeException("Type mismatch in operation '" + operation 
                + "': left operand is " + leftType + ", right operand is " + rightType);
        }

        System.out.println("Operation '" + operation + "' passed type checking.");
    }

    // Helper method to check terms (variables, constants, function calls, etc.)
    private String checkTerm(Node termNode) {
        if (termNode == null) {
            return null;
        }

        Element elem = (Element) termNode;

        // Check if the term is a variable
        if (elem.getElementsByTagName("VNAME").getLength() > 0) {
            String varName = elem.getElementsByTagName("VNAME").item(0).getTextContent();
            if (symbolTable.containsKey(varName)) {
                String varType = symbolTable.get(varName);
                System.out.println("Variable " + varName + " is of type " + varType);
                return varType;
            } else {
                throw new RuntimeException("Undeclared variable: " + varName);
            }
        }

        // Check if the term is a constant
        if (elem.getElementsByTagName("CONST").getLength() > 0) {
            String constValue = elem.getElementsByTagName("CONST").item(0).getTextContent();
            try {
                Integer.parseInt(constValue);  // Try parsing the constant as a number
                return "num";
            } catch (NumberFormatException e) {
                return "text";  // If parsing fails, treat it as a text constant
            }
        }

        return null;  // Invalid term
    }

    // Helper method to safely get text content from an element
    private String getTextContentSafely(Element elem, String tagName) {
        NodeList nodeList = elem.getElementsByTagName(tagName);
        if (nodeList != null && nodeList.getLength() > 0) {
            return nodeList.item(0).getTextContent();
        } else {
            System.err.println("Warning: Missing tag <" + tagName + "> in element.");
            return null;
        }
    }

    // Helper method to print detailed node information for debugging
    private void printNodeDetails(Element elem) {
        System.out.println("Node details:");
        System.out.println("Tag name: " + elem.getTagName());
        NodeList children = elem.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            System.out.println("Child node: " + child.getNodeName() + ", Value: " + child.getTextContent());
        }
    }
}

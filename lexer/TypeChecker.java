import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.*;
import java.util.*;

public class TypeChecker {
    private final Map<Integer, Element> nodes = new HashMap<>();
    private final Map<Integer, String> terminalValues = new HashMap<>();
    private final Map<String, String> symbolTable = new HashMap<>();
    private final Set<Integer> visitedNodes = new HashSet<>();

    private Map<Integer, Element> nodeMap = new HashMap<>();
    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println("Usage: java TypeChecker <XML file> <Symbol table file>");
            return;
        }

        TypeChecker checker = new TypeChecker();
        checker.loadSymbolTable(args[1]);  // Load symbol table from file
        checker.loadTree(args[0]);         // Load XML tree
        boolean result = checker.traverseAndCheck();
        System.out.println("Type check result: " + result);
    }

    // Load the symbol table from the input file
    private void loadSymbolTable(String filename) {
        System.out.println("Loading Symbol Table from: " + filename);

        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && line.contains(":")) {
                    String[] parts = line.split(":");
                    if (parts.length >= 3) {
                        String variableName = parts[0].trim();
                        String type = parts[2].trim();
                        symbolTable.put(variableName, type);
                        System.out.println("Added to Symbol Table: " + variableName + " = " + type);
                    } else {
                        System.out.println("Skipping malformed line: " + line);
                    }
                }
            }
            System.out.println("Loaded Symbol Table: " + symbolTable);
        } catch (IOException e) {
            System.out.println("Error loading symbol table: " + e.getMessage());
        }
    }

    // Load the XML tree from the input file
    private void loadTree(String filename) throws Exception {
        System.out.println("Loading XML Tree from: " + filename);

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new File(filename));

        // Store IN nodes in the map
        NodeList inNodes = doc.getElementsByTagName("IN");
        for (int i = 0; i < inNodes.getLength(); i++) {
            Element element = (Element) inNodes.item(i);
            int id = Integer.parseInt(element.getElementsByTagName("UNID").item(0).getTextContent());
            nodes.put(id, element);
            System.out.println("Added IN Node: ID = " + id + ", Symbol = " + element.getAttribute("symbol"));
        }

        // Extract LEAF nodes to get terminal values
        NodeList leafNodes = doc.getElementsByTagName("LEAF");
        for (int i = 0; i < leafNodes.getLength(); i++) {
            Element element = (Element) leafNodes.item(i);
            int id = Integer.parseInt(element.getElementsByTagName("UNID").item(0).getTextContent());
            String terminalValue = element.getElementsByTagName("TERMINAL").item(0).getTextContent().trim();
            terminalValues.put(id, terminalValue);
            System.out.println("Added LEAF Node: ID = " + id + ", Terminal = " + terminalValue);
        }
    }

    // Traverse and check the XML tree
    // Traverse and check the XML tree, stopping at terminal symbols if necessary.
private boolean traverseAndCheck() {
    for (Element node : nodes.values()) {
        if (!checkNode(node)) {
            return false;  // Stop traversal if any node fails.
        }
    }
    return true;
}


private boolean checkNode(Element node) {
    int nodeId = getNodeID(node);

    if (visitedNodes.contains(nodeId)) {
        System.out.println("Node ID " + nodeId + " already visited. Skipping.");
        return true;  // Skip further processing.
    }

    visitedNodes.add(nodeId);  // Mark as visited.

    String symbol = node.getAttribute("symbol");
    System.out.println("Processing node: ID = " + nodeId + ", Symbol = " + symbol);

    // Stop if we encounter the terminal '$'.
    if ("$".equals(terminalValues.get(nodeId))) {
        System.out.println("Encountered terminal '$'. Stopping further processing.");
        return true;
    }

    // Process OP or ASSIGN nodes.
    if ("OP".equals(symbol)) {
        return checkOp(node);
    } else if ("ASSIGN".equals(symbol)) {
        return checkAssignment(node);
    }

    // Traverse children.
    List<Element> children = findChildren(node);
    for (Element child : children) {
        if (!checkNode(child)) {
            return false;  // Stop if any child fails.
        }
    }
    return true;
}


    // Check an OP node structure
private boolean checkOp(Element node) {
    List<Element> children = findChildren(node);

    if (children.size() < 2) {
        System.out.println("Invalid OP node structure: Not enough children");
        return false;
    }

    // Check type compatibility for all child nodes involved in the operation
    String firstType = inferType(children.get(0));
    for (int i = 1; i < children.size(); i++) {
        String childType = inferType(children.get(i));
        if (!firstType.equals(childType)) {
            System.out.println("Type mismatch in OP: Expected " + firstType + " but got " + childType);
            return false;
        }
    }

    System.out.println("Valid OP node with " + children.size() + " children.");
    return true;
}


    // Check an ASSIGN node structure
private boolean checkAssignment(Element node) {
    List<Element> children = findChildren(node);

    if (children.size() < 2) {
        System.out.println("Invalid ASSIGN node structure: Not enough children");
        return false;
    }

    // Get the variable name and type from the symbol table.
    String variableName = getNodeContent(children.get(0));
    String expectedType = symbolTable.get(variableName);

    if (expectedType == null) {
        System.out.println("Error: Variable '" + variableName + "' is not declared.");
        return false;
    }

    // Infer the type of the assigned value.
    String valueType = inferType(children.get(1));

    // Check if the types match.
    if (!expectedType.equals(valueType)) {
        System.out.println("Type mismatch in ASSIGN: Variable '" + variableName + "' expects " 
                           + expectedType + ", but got " + valueType);
        return false;
    }

    System.out.println("Assignment to '" + variableName + "' is valid.");
    return true;
}

    

    // Infer the type of a node based on its content
// Infer the type of a node based on its content
private String inferType(Element node) {
    String content = getNodeContent(node);

    if (content.equals("unknown")) {
        return "unknown";  // Gracefully handle unknown content.
    } else if (content.matches("\\d+")) {
        return "num";
    } else if (content.matches("\".*\"")) {
        return "text";
    } else if (symbolTable.containsKey(content)) {
        return symbolTable.get(content);
    }
    return "unknown";
}


private String getNodeContent(Element node) {
    int id = getNodeID(node);

    // Return cached content if available (avoid re-processing).
    if (terminalValues.containsKey(id)) {
        String content = terminalValues.get(id);
        System.out.println("Using cached content for node ID " + id + ": " + content);
        return content;
    }

    // If the node was visited before, use a fallback to prevent empty content.
    if (visitedNodes.contains(id)) {
        System.out.println("Node ID " + id + " already visited. Returning fallback content.");
        return terminalValues.getOrDefault(id, "unknown");  // Use a default fallback.
    }

    visitedNodes.add(id);  // Mark as visited.

    // Traverse child nodes to retrieve content.
    NodeList childIDs = node.getElementsByTagName("ID");
    for (int i = 0; i < childIDs.getLength(); i++) {
        int childID = Integer.parseInt(childIDs.item(i).getTextContent().trim());

        if (terminalValues.containsKey(childID)) {
            String content = terminalValues.get(childID);
            terminalValues.put(id, content);  // Cache the result.
            return content;
        }

        if (nodes.containsKey(childID)) {
            Element childNode = nodes.get(childID);
            String childContent = getNodeContent(childNode);
            if (!childContent.isEmpty()) {
                terminalValues.put(id, childContent);  // Cache the result.
                return childContent;
            }
        }
    }

    System.out.println("No valid content found for node ID " + id + ". Returning 'unknown'.");
    return "unknown";  // Ensure we return 'unknown' instead of an empty string.
}






    
    
    // Find children of a given node
    private List<Element> findChildren(Element node) {
        List<Element> children = new ArrayList<>();
        NodeList childNodes = node.getElementsByTagName("ID");

        for (int i = 0; i < childNodes.getLength(); i++) {
            int childID = Integer.parseInt(childNodes.item(i).getTextContent());
            if (nodes.containsKey(childID)) {
                children.add(nodes.get(childID));
                System.out.println("Found child node: ID = " + childID);
            }
        }
        return children;
    }

    // Get the unique ID of a node
    private int getNodeID(Element node) {
        return Integer.parseInt(node.getElementsByTagName("UNID").item(0).getTextContent());
    }
}

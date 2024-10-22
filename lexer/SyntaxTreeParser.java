import java.io.File;
import java.io.IOException;
import java.util.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;

public class SyntaxTreeParser {

    static class TreeNode {
        String symbol;
        int unid;
        int parent;
        String terminal;
        List<Integer> children;

        public TreeNode(String symbol, int unid, int parent) {
            this.symbol = symbol;
            this.unid = unid;
            this.parent = parent;
            this.children = new ArrayList<>();
        }

        @Override
        public String toString() {
            return terminal != null ? String.format("'%s'", terminal) : symbol;
        }
    }

    private Map<Integer, TreeNode> nodes = new HashMap<>();
    private Map<String, String> symbolTable = new HashMap<>();

    // Load the symbol table from Symbol.txt
    private void loadSymbolTable(String filename) throws IOException {
        Scanner scanner = new Scanner(new File(filename));
        while (scanner.hasNextLine()) {
            String[] parts = scanner.nextLine().split(" : ");
            symbolTable.put(parts[0], parts[1]); // e.g., V_x -> v110
        }
        scanner.close();
    }

    public void parseTree(String filename) {
        try {
            // Load XML tree
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new File(filename));
            doc.getDocumentElement().normalize();

            // Parse inner and leaf nodes
            parseNodes(doc);

            // Find the root node and start translation
            for (TreeNode node : nodes.values()) {
                if (node.symbol.equals("PROG")) { // PROG node is the root node
                    System.out.println("Starting translation for PROG node: " + node.unid);
                    String result = translateNode(node);
                    System.out.println("Translation result: " + result);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void parseNodes(Document doc) {
        // Parse inner nodes
        NodeList innerNodes = doc.getElementsByTagName("IN");
        for (int i = 0; i < innerNodes.getLength(); i++) {
            Element node = (Element) innerNodes.item(i);
            parseInnerNode(node);
        }

        // Parse leaf nodes
        NodeList leafNodes = doc.getElementsByTagName("LEAF");
        for (int i = 0; i < leafNodes.getLength(); i++) {
            Element node = (Element) leafNodes.item(i);
            parseLeafNode(node);
        }
    }

    private void parseInnerNode(Element node) {
        String symbol = node.getAttribute("symbol");
        int unid = Integer.parseInt(getElementText(node, "UNID"));
        int parent = Integer.parseInt(getElementText(node, "PARENT"));

        TreeNode treeNode = new TreeNode(symbol, unid, parent);

        // Parse children
        NodeList children = node.getElementsByTagName("ID");
        for (int i = 0; i < children.getLength(); i++) {
            Element child = (Element) children.item(i);
            treeNode.children.add(Integer.parseInt(child.getTextContent()));
        }

        nodes.put(unid, treeNode);
    }

    private void parseLeafNode(Element node) {
        int unid = Integer.parseInt(getElementText(node, "UNID"));
        int parent = Integer.parseInt(getElementText(node, "PARENT"));
        String terminal = getElementText(node, "TERMINAL");

        TreeNode treeNode = new TreeNode("LEAF", unid, parent);
        treeNode.terminal = terminal;
        nodes.put(unid, treeNode);
    }

    private String getElementText(Element parent, String tagName) {
        NodeList list = parent.getElementsByTagName(tagName);
        if (list.getLength() > 0) {
            return list.item(0).getTextContent();
        }
        return null;
    }

    private String translateNode(TreeNode node) {
        System.out.println("Translating node: " + node.symbol + " (UNID: " + node.unid + ")");
        switch (node.symbol) {
            case "PROG":
                return translatePROG(node);
            case "ALGO":
                return translateALGO(node);
            case "COMMAND":
                return translateCOMMAND(node);
            case "ASSIGN":
                return translateASSIGN(node);
            // Add more cases for each grammar symbol
            default:
                return "";
        }
    }

    // PROG ::= main GLOBVARS ALGO FUNCTIONS
    private String translatePROG(TreeNode node) {
        System.out.println("Translating PROG node...");
        TreeNode algoNode = nodes.get(node.children.get(1)); // Assuming GLOBVARS at 0, ALGO at 1
        TreeNode functionsNode = nodes.get(node.children.get(2));

        String algoCode = translateNode(algoNode);
        String functionsCode = translateNode(functionsNode);

        return algoCode + " STOP " + functionsCode;
    }

    // ALGO ::= begin INSTRUC end
    private String translateALGO(TreeNode node) {
        System.out.println("Translating ALGO node...");
        TreeNode instrucNode = nodes.get(node.children.get(0)); // INSTRUC
        return translateNode(instrucNode);
    }

    // COMMAND ::= VNAME < input
    private String translateASSIGN(TreeNode node) {
        System.out.println("Translating ASSIGN node...");
        TreeNode vnameNode = nodes.get(node.children.get(0)); // VNAME
        String vname = translateNode(vnameNode);
        return "INPUT " + vname;
    }

    // COMMAND ::= print ATOMIC
    private String translateCOMMAND(TreeNode node) {
        System.out.println("Translating COMMAND node...");
        TreeNode atomicNode = nodes.get(node.children.get(0)); // ATOMIC
        String atomicCode = translateNode(atomicNode);
        return "PRINT " + atomicCode;
    }

    // ATOMIC ::= VNAME (Lookup from symbol table)
    private String translateATOMIC(TreeNode node) {
        String vname = node.terminal;
        System.out.println("Translating ATOMIC node with terminal: " + vname);
        return symbolTable.getOrDefault(vname, vname); // Translate based on symbol table
    }

    // Helper function to translate VNAME from the symbol table
    private String translateVNAME(TreeNode node) {
        String vname = node.terminal;
        System.out.println("Translating VNAME node with terminal: " + vname);
        return symbolTable.get(vname);
    }

    public static void main(String[] args) {
        SyntaxTreeParser parser = new SyntaxTreeParser();
        try {
            parser.loadSymbolTable("Symbol.txt");
            parser.parseTree("parser.xml");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

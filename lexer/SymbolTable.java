import java.io.File;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class SymbolTable {
    private Map<String, Map<String, String>> table;

    public SymbolTable() {
        table = new HashMap<>();
    }

    public void populateFromXML(String filename) {
        try {
            File inputFile = new File(filename);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();

            Element root = doc.getDocumentElement();
            System.out.println("Root element: " + root.getNodeName());
            processNode(root);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void processNode(Element node) {
        System.out.println("Processing node: " + node.getTagName());

        if (node.getTagName().equals("ROOT") || node.getTagName().equals("IN") || node.getTagName().equals("LEAF")) {
            NodeList unidList = node.getElementsByTagName("UNID");
            if (unidList.getLength() > 0) {
                String nodeId = unidList.item(0).getTextContent();
                System.out.println("Found UNID: " + nodeId);
                addSymbol(nodeId, "type", node.getTagName());

                if (node.getTagName().equals("ROOT") || node.getTagName().equals("IN")) {
                    NodeList symbList = node.getElementsByTagName("SYMB");
                    if (symbList.getLength() > 0) {
                        String symbol = symbList.item(0).getTextContent();
                        System.out.println("Found SYMB: " + symbol);
                        addSymbol(nodeId, "symbol", symbol);
                    }
                } else if (node.getTagName().equals("LEAF")) {
                    NodeList terminalList = node.getElementsByTagName("TERMINAL");
                    if (terminalList.getLength() > 0) {
                        String terminal = terminalList.item(0).getTextContent();
                        System.out.println("Found TERMINAL: " + terminal);
                        addSymbol(nodeId, "terminal", terminal);
                    }
                }
            }
        }

        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                processNode((Element) child);
            }
        }
    }

    public void addSymbol(String nodeId, String attribute, String value) {
        table.putIfAbsent(nodeId, new HashMap<>());
        table.get(nodeId).put(attribute, value);
        System.out.println("Added to table - Node ID: " + nodeId + ", " + attribute + ": " + value);
    }

    public String getSymbolAttribute(String nodeId, String attribute) {
        Map<String, String> attributes = table.get(nodeId);
        return attributes != null ? attributes.get(attribute) : null;
    }

    public void printTable() {
        System.out.println("Symbol Table:");
        if (table.isEmpty()) {
            System.out.println("  (Empty)");
        } else {
            for (Map.Entry<String, Map<String, String>> entry : table.entrySet()) {
                System.out.println("Node ID: " + entry.getKey());
                for (Map.Entry<String, String> attr : entry.getValue().entrySet()) {
                    System.out.println("  " + attr.getKey() + ": " + attr.getValue());
                }
            }
        }
    }

    public static void main(String[] args) {
        SymbolTable symbolTable = new SymbolTable();
        symbolTable.populateFromXML("parser.xml");
        symbolTable.printTable();
    }
}
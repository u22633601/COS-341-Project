import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

class SymbolTable {
    private Map<String, SymbolInfo> table;

    public SymbolTable() {
        table = new HashMap<>();
    }

    public void addSymbol(String name, SymbolInfo info) {
        table.put(name, info);
    }

    public SymbolInfo getSymbol(String name) {
        return table.get(name);
    }

    public void display() {
        for (Map.Entry<String, SymbolInfo> entry : table.entrySet()) {
            System.out.println("Symbol: " + entry.getKey() + " -> " + entry.getValue());
        }
    }
}

class SymbolInfo {
    private String type;
    private String parent;
    private String children;

    public SymbolInfo(String type, String parent, String children) {
        this.type = type;
        this.parent = parent;
        this.children = children;
    }

    @Override
    public String toString() {
        return "Type: " + type + ", Parent: " + parent + ", Children: " + children;
    }
}

public class SymbolTableBuilder {

    public static void main(String[] args) {
        try {
            File inputFile = new File("parser.xml"); // Replace with your XML file path
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();

            SymbolTable symbolTable = new SymbolTable();

            // Parse INNERNODES
            NodeList innerNodes = doc.getElementsByTagName("IN");
            for (int i = 0; i < innerNodes.getLength(); i++) {
                Node node = innerNodes.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;
                    String symbol = element.getAttribute("symbol");
                    String unid = getTagValue("UNID", element);
                    String parent = getTagValue("PARENT", element);
                    String children = getChildrenString(element);

                    SymbolInfo symbolInfo = new SymbolInfo(symbol, parent, children);
                    symbolTable.addSymbol(unid, symbolInfo);
                }
            }

            // Parse LEAFNODES
            NodeList leafNodes = doc.getElementsByTagName("LEAF");
            for (int i = 0; i < leafNodes.getLength(); i++) {
                Node node = leafNodes.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;
                    String terminal = getTagValue("TERMINAL", element);
                    String unid = getTagValue("UNID", element);
                    String parent = getTagValue("PARENT", element);

                    SymbolInfo symbolInfo = new SymbolInfo("TERMINAL", parent, terminal);
                    symbolTable.addSymbol(unid, symbolInfo);
                }
            }

            // Display the symbol table
            symbolTable.display();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String getTagValue(String tag, Element element) {
        NodeList nodeList = element.getElementsByTagName(tag).item(0).getChildNodes();
        Node node = (Node) nodeList.item(0);
        return node.getNodeValue();
    }

    private static String getChildrenString(Element element) {
        NodeList childrenList = element.getElementsByTagName("ID");
        StringBuilder children = new StringBuilder();
        for (int i = 0; i < childrenList.getLength(); i++) {
            Node childNode = childrenList.item(i);
            if (i > 0) children.append(", ");
            children.append(childNode.getTextContent());
        }
        return children.toString();
    }
}

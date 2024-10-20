import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.*;
import java.util.*;

public class TypeChecker {
    private Document doc;
    private Map<String, String> symbolTable = new HashMap<>();
    private Map<String, Element> nodeMap = new HashMap<>();

    public TypeChecker(String xmlFile, String symbolTableFile) {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            doc = dBuilder.parse(new File(xmlFile));
            doc.getDocumentElement().normalize();

            loadSymbolTable(symbolTableFile);
            buildNodeMap();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadSymbolTable(String filename) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filename));
        String line;
        while ((line = reader.readLine()) != null) {
            String[] parts = line.split(":");
            if (parts.length == 3) {
                symbolTable.put(parts[0].trim(), parts[2].trim());
            }
        }
        reader.close();
        System.out.println("Symbol Table loaded: " + symbolTable);
    }

    private void buildNodeMap() {
        NodeList innerNodes = doc.getElementsByTagName("IN");
        for (int i = 0; i < innerNodes.getLength(); i++) {
            Element node = (Element) innerNodes.item(i);
            String unid = node.getElementsByTagName("UNID").item(0).getTextContent();
            nodeMap.put(unid, node);
        }

        NodeList leafNodes = doc.getElementsByTagName("LEAF");
        for (int i = 0; i < leafNodes.getLength(); i++) {
            Element node = (Element) leafNodes.item(i);
            String unid = node.getElementsByTagName("UNID").item(0).getTextContent();
            nodeMap.put(unid, node);
        }
    }

    public boolean typeCheck() {
        Element root = (Element) doc.getElementsByTagName("ROOT").item(0);
        if (root == null) {
            System.out.println("Error: ROOT node not found");
            return false;
        }
        System.out.println("Starting type check from root: " + root.getNodeName());
        return typeCheckPROG(root);
    }

    private boolean typeCheckPROG(Element progNode) {
        System.out.println("Checking PROG node");
        NodeList childrenList = progNode.getElementsByTagName("CHILDREN");
        if (childrenList.getLength() == 0) {
            System.out.println("Error: PROG node has no CHILDREN");
            return false;
        }
        String progUnid = childrenList.item(0).getTextContent().trim();
        Element progInnerNode = nodeMap.get(progUnid);
        if (progInnerNode == null) {
            System.out.println("Error: PROG inner node not found");
            return false;
        }
        
        boolean globvarsCheck = true;
        boolean algoCheck = true;
        boolean functionsCheck = true;

        NodeList children = progInnerNode.getElementsByTagName("CHILDREN").item(0).getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i).getNodeType() == Node.ELEMENT_NODE) {
                Element child = (Element) children.item(i);
                String childUnid = child.getTextContent();
                Element childNode = nodeMap.get(childUnid);
                if (childNode == null) {
                    System.out.println("Error: Child node not found for UNID " + childUnid);
                    continue;
                }
                NodeList symbList = childNode.getElementsByTagName("SYMB");
                if (symbList.getLength() == 0) {
                    System.out.println("Error: SYMB not found for node " + childUnid);
                    continue;
                }
                String symb = symbList.item(0).getTextContent();
                System.out.println("Checking child node: " + symb);

                switch (symb) {
                    case "GLOBVARS":
                        globvarsCheck = typeCheckGLOBVARS(childNode);
                        break;
                    case "ALGO":
                        algoCheck = typeCheckALGO(childNode);
                        break;
                    case "FUNCTIONS":
                        functionsCheck = typeCheckFUNCTIONS(childNode);
                        break;
                    default:
                        System.out.println("Unknown node type: " + symb);
                }
            }
        }

        System.out.println("PROG check result: GLOBVARS=" + globvarsCheck + ", ALGO=" + algoCheck + ", FUNCTIONS=" + functionsCheck);
        return globvarsCheck && algoCheck && functionsCheck;
    }

    private boolean typeCheckGLOBVARS(Element globvarsNode) {
        System.out.println("Checking GLOBVARS node");
        NodeList childrenList = globvarsNode.getElementsByTagName("CHILDREN");
        if (childrenList.getLength() == 0) {
            System.out.println("GLOBVARS node has no children");
            return true;  // Empty GLOBVARS is valid
        }
        NodeList children = childrenList.item(0).getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i).getNodeType() == Node.ELEMENT_NODE) {
                Element child = (Element) children.item(i);
                String childUnid = child.getTextContent();
                Element childNode = nodeMap.get(childUnid);
                if (childNode == null) {
                    System.out.println("Error: Child node not found for UNID " + childUnid);
                    continue;
                }
                String symb = childNode.getElementsByTagName("SYMB").item(0).getTextContent();
                if (symb.equals("VTYP")) {
                    String vtype = getTerminalValue(childUnid);
                    if (i + 1 < children.getLength()) {
                        String vnameUnid = children.item(i + 1).getTextContent();
                        String vname = getTerminalValue(vnameUnid);
                        System.out.println("Checking global variable: " + vname + " of type " + vtype);
                        if (!symbolTable.get(vname).equals(vtype)) {
                            System.out.println("Type mismatch in global variable declaration: " + vname);
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    private boolean typeCheckALGO(Element algoNode) {
        System.out.println("Checking ALGO node");
        NodeList childrenList = algoNode.getElementsByTagName("CHILDREN");
        if (childrenList.getLength() == 0) {
            System.out.println("ALGO node has no children");
            return true;
        }
        NodeList children = childrenList.item(0).getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i).getNodeType() == Node.ELEMENT_NODE) {
                Element child = (Element) children.item(i);
                String childUnid = child.getTextContent();
                Element childNode = nodeMap.get(childUnid);
                if (childNode == null) {
                    System.out.println("Error: Child node not found for UNID " + childUnid);
                    continue;
                }
                String symb = childNode.getElementsByTagName("SYMB").item(0).getTextContent();
                if (symb.equals("INSTRUC")) {
                    if (!typeCheckINSTRUC(childNode)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private boolean typeCheckINSTRUC(Element instrucNode) {
        System.out.println("Checking INSTRUC node");
        NodeList childrenList = instrucNode.getElementsByTagName("CHILDREN");
        if (childrenList.getLength() == 0) {
            System.out.println("INSTRUC node has no children");
            return true;
        }
        NodeList children = childrenList.item(0).getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i).getNodeType() == Node.ELEMENT_NODE) {
                Element child = (Element) children.item(i);
                String childUnid = child.getTextContent();
                Element childNode = nodeMap.get(childUnid);
                if (childNode == null) {
                    System.out.println("Error: Child node not found for UNID " + childUnid);
                    continue;
                }
                String symb = childNode.getElementsByTagName("SYMB").item(0).getTextContent();
                if (symb.equals("COMMAND")) {
                    if (!typeCheckCOMMAND(childNode)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private boolean typeCheckCOMMAND(Element commandNode) {
        System.out.println("Checking COMMAND node");
        NodeList childrenList = commandNode.getElementsByTagName("CHILDREN");
        if (childrenList.getLength() == 0) {
            System.out.println("COMMAND node has no children");
            return true;
        }
        NodeList children = childrenList.item(0).getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i).getNodeType() == Node.ELEMENT_NODE) {
                Element child = (Element) children.item(i);
                String childUnid = child.getTextContent();
                Element childNode = nodeMap.get(childUnid);
                if (childNode == null) {
                    System.out.println("Error: Child node not found for UNID " + childUnid);
                    continue;
                }
                String symb = childNode.getElementsByTagName("SYMB").item(0).getTextContent();
                if (symb.equals("ASSIGN")) {
                    return typeCheckASSIGN(childNode);
                }
            }
        }
        return true;
    }

    private boolean typeCheckASSIGN(Element assignNode) {
        System.out.println("Checking ASSIGN node");
        NodeList childrenList = assignNode.getElementsByTagName("CHILDREN");
        if (childrenList.getLength() == 0) {
            System.out.println("ASSIGN node has no children");
            return false;
        }
        NodeList children = childrenList.item(0).getChildNodes();
        String vname = "";
        String termType = "";

        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i).getNodeType() == Node.ELEMENT_NODE) {
                Element child = (Element) children.item(i);
                String childUnid = child.getTextContent();
                Element childNode = nodeMap.get(childUnid);
                if (childNode == null) {
                    System.out.println("Error: Child node not found for UNID " + childUnid);
                    continue;
                }
                String symb = childNode.getElementsByTagName("SYMB").item(0).getTextContent();
                if (symb.equals("VNAME")) {
                    vname = getTerminalValue(childUnid);
                } else if (symb.equals("TERM")) {
                    termType = typeOfTERM(childNode);
                }
            }
        }

        String vnameType = symbolTable.get(vname);
        System.out.println("Left-hand side: " + vname + " of type " + vnameType);
        System.out.println("Right-hand side type: " + termType);

        if (!vnameType.equals(termType)) {
            System.out.println("Type mismatch in assignment: " + vname + " (" + vnameType + ") = " + termType);
            return false;
        }
        System.out.println("Assignment type check passed");
        return true;
    }

    private String typeOfTERM(Element termNode) {
        System.out.println("Determining type of TERM");
        NodeList childrenList = termNode.getElementsByTagName("CHILDREN");
        if (childrenList.getLength() == 0) {
            System.out.println("TERM node has no children");
            return "unknown";
        }
        NodeList children = childrenList.item(0).getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i).getNodeType() == Node.ELEMENT_NODE) {
                Element child = (Element) children.item(i);
                String childUnid = child.getTextContent();
                Element childNode = nodeMap.get(childUnid);
                if (childNode == null) {
                    System.out.println("Error: Child node not found for UNID " + childUnid);
                    continue;
                }
                String symb = childNode.getElementsByTagName("SYMB").item(0).getTextContent();
                if (symb.equals("OP")) {
                    return typeOfOP(childNode);
                } else if (symb.equals("ATOMIC")) {
                    return typeOfATOMIC(childNode);
                }
            }
        }
        System.out.println("Unknown TERM type");
        return "unknown";
    }

    private String typeOfOP(Element opNode) {
        System.out.println("Determining type of OP");
        NodeList childrenList = opNode.getElementsByTagName("CHILDREN");
        if (childrenList.getLength() == 0) {
            System.out.println("OP node has no children");
            return "unknown";
        }
        NodeList children = childrenList.item(0).getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i).getNodeType() == Node.ELEMENT_NODE) {
                Element child = (Element) children.item(i);
                String childUnid = child.getTextContent();
                Element childNode = nodeMap.get(childUnid);
                if (childNode == null) {
                    System.out.println("Error: Child node not found for UNID " + childUnid);
                    continue;
                }
                String symb = childNode.getElementsByTagName("SYMB").item(0).getTextContent();
                if (symb.equals("BINOP")) {
                    String binop = getTerminalValue(childUnid);
                    System.out.println("BINOP: " + binop);
                    if (binop.equals("add") || binop.equals("sub") || binop.equals("mul") || binop.equals("div")) {
                        System.out.println("Arithmetic operation, type is num");
                        return "num";
                    }
                }
            }
        }
        System.out.println("Unknown OP type");
        return "unknown";
    }

    private String typeOfATOMIC(Element atomicNode) {
        System.out.println("Determining type of ATOMIC");
        NodeList childrenList = atomicNode.getElementsByTagName("CHILDREN");
        if (childrenList.getLength() == 0) {
            System.out.println("ATOMIC node has no children");
            return "unknown";
        }
        NodeList children = childrenList.item(0).getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i).getNodeType() == Node.ELEMENT_NODE) {
                Element child = (Element) children.item(i);
                String childUnid = child.getTextContent();
                Element childNode = nodeMap.get(childUnid);
                if (childNode == null) {
                    System.out.println("Error: Child node not found for UNID " + childUnid);
                    continue;
                }
                String symb = childNode.getElementsByTagName("SYMB").item(0).getTextContent();
                if (symb.equals("VNAME")) {
                    String vname = getTerminalValue(childUnid);
                    String type = symbolTable.get(vname);
                    System.out.println("ATOMIC is a variable: " + vname + " of type " + type);
                    return type;
                }
            }
        }
        System.out.println("Unknown ATOMIC type");
        return "unknown";
    }

    private boolean typeCheckFUNCTIONS(Element functionsNode) {
        System.out.println("Checking FUNCTIONS node (not implemented)");
        return true;
    }

    private String getTerminalValue(String unid) {
        Element node = nodeMap.get(unid);
        if (node != null) {
            NodeList terminals = node.getElementsByTagName("TERMINAL");
            if (terminals.getLength() > 0) {
                return terminals.item(0).getTextContent();
            }
        }
        System.out.println("Error: Terminal value not found for UNID " + unid);
        return "";
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java TypeChecker <xml_file> <symbol_table_file>");
            return;
        }

        TypeChecker checker = new TypeChecker(args[0], args[1]);
        boolean result = checker.typeCheck();
        System.out.println("Type checking result: " + (result ? "PASS" : "FAIL"));
    }
}
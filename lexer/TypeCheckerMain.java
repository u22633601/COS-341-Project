import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;

public class TypeCheckerMain {
    public static void main(String[] args) {
        try {
            // Parse the XML file
            File inputFile = new File("parser.xml");
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();

            // Load the symbol table
            SymbolTable symbolTable = loadSymbolTable("Symbol.txt");

            // Run the type checker
            TypeCheckerImpl typeChecker = new TypeCheckerImpl(symbolTable);
            boolean isValid = typeChecker.typeCheck(doc.getDocumentElement());

            if (isValid) {
                System.out.println("Type checking completed successfully.");
            } else {
                System.err.println("Type checking encountered errors.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static SymbolTable loadSymbolTable(String fileName) throws IOException {
        SymbolTable symbolTable = new SymbolTable();
        File file = new File(fileName);
        Scanner scanner = new Scanner(file);

        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            String[] parts = line.split(" : ");
            if (parts.length == 3) {
                String varName = parts[0].trim();
                String uniqueName = parts[1].trim();
                String type = parts[2].trim();
                symbolTable.add(varName, uniqueName, type);
            }
        }

        scanner.close();
        return symbolTable;
    }

    private static class SymbolTable {
        private Map<String, String> variables;
        private Map<String, String> uniqueNames;

        public SymbolTable() {
            variables = new HashMap<>();
            uniqueNames = new HashMap<>();
        }

        public void add(String varName, String uniqueName, String type) {
            variables.put(varName, type);
            uniqueNames.put(varName, uniqueName);
        }

        public String getType(String varName) {
            return variables.get(varName);
        }

        public String getUniqueName(String varName) {
            return uniqueNames.get(varName);
        }
    }

    private static class TypeCheckerImpl {
        private SymbolTable symbolTable;
        private boolean stopOnError;

        public TypeCheckerImpl(SymbolTable symbolTable) {
            this.symbolTable = symbolTable;
            this.stopOnError = false;
        }

        public boolean typeCheck(Node node) {
            String nodeName = node.getNodeName();
            switch (nodeName) {
                case "PROG":
                    return typeCheckPROG(node);
                case "GLOBVARS":
                    return typeCheckGLOBVARS(node);
                case "ALGO":
                    return typeCheckALGO(node);
                case "INSTRUC":
                    return typeCheckINSTRUC(node);
                case "COMMAND":
                    return typeCheckCOMMAND(node);
                case "ASSIGN":
                    return typeCheckASSIGN(node);
                case "ATOMIC":
                    return typeCheckATOMIC(node) != null;
                case "CALL":
                    return typeCheckCALL(node) != null;
                case "OP":
                    return typeCheckOP(node) != null;
                case "BRANCH":
                    return typeCheckBRANCH(node);
                case "SIMPLE":
                    return typeCheckSIMPLE(node);
                case "COMPOSIT":
                    return typeCheckCOMPOSIT(node);
                case "FUNCTIONS":
                    return typeCheckFUNCTIONS(node);
                case "DECL":
                    return typeCheckDECL(node);
                case "HEADER":
                    return typeCheckHEADER(node);
                case "BODY":
                    return typeCheckBODY(node);
                case "LOCVARS":
                    return typeCheckLOCVARS(node);
                case "SUBFUNCS":
                    return typeCheckSUBFUNCS(node);
                default:
                    return true; // Default case for unknown nodes
            }
        }

        private boolean typeCheckPROG(Node node) {
            NodeList children = node.getChildNodes();
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

        private boolean typeCheckGLOBVARS(Node node) {
            // Base case for GLOBVARS
            return true;
        }

        private boolean typeCheckALGO(Node node) {
            NodeList children = node.getChildNodes();
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

        private boolean typeCheckINSTRUC(Node node) {
            NodeList children = node.getChildNodes();
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

        private boolean typeCheckCOMMAND(Node node) {
            NodeList children = node.getChildNodes();
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

        private boolean typeCheckASSIGN(Node node) {
            NodeList children = node.getChildNodes();
            String varName = null;
            String type = null;

            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (child.getNodeType() == Node.ELEMENT_NODE) {
                    if (child.getNodeName().equals("VNAME")) {
                        varName = child.getTextContent().trim();
                        type = symbolTable.getType(varName);
                    } else if (child.getNodeName().equals("TERM")) {
                        String termType = typeCheckTERM(child);
                        if (type != null && !type.equals(termType)) {
                            System.err.println("Type error: Assigning " + termType + " to " + type);
                            return false;
                        }
                    }
                }
            }
            return true;
        }

        private String typeCheckATOMIC(Node node) {
            NodeList children = node.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (child.getNodeType() == Node.ELEMENT_NODE) {
                    if (child.getNodeName().equals("VNAME")) {
                        String varName = child.getTextContent().trim();
                        return symbolTable.getType(varName);
                    } else if (child.getNodeName().equals("CONST")) {
                        return child.getTextContent().trim();
                    }
                }
            }
            return null;
        }

        private String typeCheckCALL(Node node) {
            NodeList children = node.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (child.getNodeType() == Node.ELEMENT_NODE) {
                    if (child.getNodeName().equals("FNAME")) {
                        String funcName = child.getTextContent().trim();
                        return symbolTable.getType(funcName);
                    }
                }
            }
            return null;
        }

        private String typeCheckOP(Node node) {
            NodeList children = node.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (child.getNodeType() == Node.ELEMENT_NODE) {
                    if (child.getNodeName().equals("UNOP")) {
                        return typeCheckUNOP(child);
                    } else if (child.getNodeName().equals("BINOP")) {
                        return typeCheckBINOP(child);
                    }
                }
            }
            return null;
        }

        private String typeCheckUNOP(Node node) {
            NodeList children = node.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (child.getNodeType() == Node.ELEMENT_NODE) {
                    if (child.getNodeName().equals("ARG")) {
                        return typeCheckARG(child);
                    }
                }
            }
            return null;
        }

        private String typeCheckBINOP(Node node) {
            NodeList children = node.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (child.getNodeType() == Node.ELEMENT_NODE) {
                    if (child.getNodeName().equals("ARG1") || child.getNodeName().equals("ARG2")) {
                        return typeCheckARG(child);
                    }
                }
            }
            return null;
        }

        private String typeCheckARG(Node node) {
            NodeList children = node.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (child.getNodeType() == Node.ELEMENT_NODE) {
                    if (child.getNodeName().equals("ATOMIC")) {
                        return typeCheckATOMIC(child);
                    } else if (child.getNodeName().equals("OP")) {
                        return typeCheckOP(child);
                    }
                }
            }
            return null;
        }

        private boolean typeCheckBRANCH(Node node) {
            NodeList children = node.getChildNodes();
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

        private boolean typeCheckSIMPLE(Node node) {
            NodeList children = node.getChildNodes();
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

        private boolean typeCheckCOMPOSIT(Node node) {
            NodeList children = node.getChildNodes();
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

        private boolean typeCheckFUNCTIONS(Node node) {
            NodeList children = node.getChildNodes();
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

        private boolean typeCheckDECL(Node node) {
            NodeList children = node.getChildNodes();
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

        private boolean typeCheckHEADER(Node node) {
            NodeList children = node.getChildNodes();
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

        private boolean typeCheckBODY(Node node) {
            NodeList children = node.getChildNodes();
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

        private boolean typeCheckLOCVARS(Node node) {
            NodeList children = node.getChildNodes();
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

        private boolean typeCheckSUBFUNCS(Node node) {
            NodeList children = node.getChildNodes();
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

        private String typeCheckTERM(Node node) {
            NodeList children = node.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (child.getNodeType() == Node.ELEMENT_NODE) {
                    if (child.getNodeName().equals("ATOMIC")) {
                        return typeCheckATOMIC(child);
                    } else if (child.getNodeName().equals("CALL")) {
                        return typeCheckCALL(child);
                    } else if (child.getNodeName().equals("OP")) {
                        return typeCheckOP(child);
                    }
                }
            }
            return null;
        }
    }
}

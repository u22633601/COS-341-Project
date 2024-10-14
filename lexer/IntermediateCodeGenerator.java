import java.util.*;

public class IntermediateCodeGenerator {
    private int tempCounter = 0;
    private int labelCounter = 0;
    private StringBuilder code = new StringBuilder();
    private Map<String, String> symbolTable;

    public IntermediateCodeGenerator(Map<String, String> symbolTable) {
        this.symbolTable = symbolTable;
        System.out.println("IntermediateCodeGenerator initialized with symbol table: " + symbolTable);
    }

    public String generateCode(NodeType root) {
        System.out.println("Starting code generation for root node: " + root.getType());
        traverseTree(root);
        return code.toString();
    }

    private void traverseTree(NodeType node) {
        System.out.println("Traversing node: " + node.getType());
        if (node.getType().equals("LEAFNODES")) {
            processLeafNodes(node);
        } else {
            for (NodeType child : node.getChildren()) {
                traverseTree(child);
            }
        }
    }

    private void processLeafNodes(NodeType leafNodesNode) {
        System.out.println("Processing LEAFNODES");
        List<NodeType> leaves = leafNodesNode.getChildren();
        for (int i = 0; i < leaves.size(); i++) {
            NodeType leaf = leaves.get(i);
            String terminal = leaf.getChildren().get(2).getVarName();
            System.out.println("Processing terminal: " + terminal);

            if (terminal.equals("V_a") || terminal.equals("V_b")) {
                if (i + 2 < leaves.size() && leaves.get(i + 1).getChildren().get(2).getVarName().equals("lst")) {
                    // Input operation
                    String varName = symbolTable.get(terminal);
                    code.append("INPUT ").append(varName).append("\n");
                    System.out.println("Generated INPUT instruction for " + terminal);
                    i += 2; // Skip "lst" and "input"
                } else if (i + 2 < leaves.size() && leaves.get(i + 1).getChildren().get(2).getVarName().equals("=")) {
                    // Assignment operation
                    String varName = symbolTable.get(terminal);
                    String value = leaves.get(i + 2).getChildren().get(2).getVarName();
                    code.append("ASSIGN ").append(varName).append(" ").append(value).append("\n");
                    System.out.println("Generated ASSIGN instruction for " + terminal);
                    i += 2; // Skip "=" and the value
                }
            } else if (terminal.equals("return")) {
                // Return operation
                if (i + 1 < leaves.size()) {
                    String returnValue = leaves.get(i + 1).getChildren().get(2).getVarName();
                    String varName = symbolTable.get(returnValue);
                    code.append("RETURN ").append(varName).append("\n");
                    System.out.println("Generated RETURN instruction for " + returnValue);
                    i++; // Skip the return value
                }
            }
        }
        code.append("HALT\n");
        System.out.println("Generated HALT instruction");
    }

    private String generateTempVar() {
        return "t" + (tempCounter++);
    }

    private String generateLabel() {
        return "L" + (labelCounter++);
    }

    // Utility method to get the variable name from the symbol table
    private String getVarName(String originalName) {
        return symbolTable.getOrDefault(originalName, originalName);
    }
}
import java.util.*;

public class IntermediateCodeGenerator {
    private int tempCounter = 0;
    private int labelCounter = 0;
    private StringBuilder code = new StringBuilder();
    private Map<String, String> symbolTable;
    private Stack<String> labelStack = new Stack<>();

    public IntermediateCodeGenerator(Map<String, String> symbolTable) {
        this.symbolTable = symbolTable;
    }

    public String generateCode(NodeType root) {
        traverseTree(root);
        return code.toString();
    }

    private void traverseTree(NodeType node) {
        if (node.getType().equals("LEAFNODES")) {
            processLeafNodes(node);
        } else {
            for (NodeType child : node.getChildren()) {
                traverseTree(child);
            }
        }
    }

    private void processLeafNodes(NodeType leafNodesNode) {
        List<NodeType> leaves = leafNodesNode.getChildren();
        for (int i = 0; i < leaves.size(); i++) {
            NodeType leaf = leaves.get(i);
            String terminal = leaf.getChildren().get(2).getVarName();

            switch (terminal) {
                case "V_x":
                case "V_y":
                case "V_result":
                case "V_a":
                case "V_b":
                case "V_sum":
                case "V_count":
                case "V_resul":
                    if (i + 2 < leaves.size() && leaves.get(i + 1).getChildren().get(2).getVarName().equals("lst")) {
                        String varName = getVarName(terminal);
                        code.append("INPUT ").append(varName).append("\n");
                        i += 2; // Skip "lst" and "input"
                    } else if (i + 2 < leaves.size()
                            && leaves.get(i + 1).getChildren().get(2).getVarName().equals("=")) {
                        String varName = getVarName(terminal);
                        String value = processExpression(leaves, i + 2);
                        code.append(varName).append(" := ").append(value).append("\n");
                        i = skipExpression(leaves, i + 2);
                    }
                    break;
                case "if":
                    String condition = processCondition(leaves, i + 1);
                    String thenLabel = generateLabel();
                    String elseLabel = generateLabel();
                    String endLabel = generateLabel();
                    labelStack.push(elseLabel);
                    labelStack.push(endLabel);
                    code.append("IF ").append(condition).append(" GOTO ").append(thenLabel).append("\n");
                    code.append("GOTO ").append(elseLabel).append("\n");
                    code.append(thenLabel).append(":\n");
                    i = skipToThen(leaves, i);
                    break;
                case "else":
                    code.append("GOTO ").append(labelStack.peek()).append("\n");
                    code.append(labelStack.pop()).append(":\n"); // Pop and print else label
                    break;
                case "end":
                    if (!labelStack.isEmpty()) {
                        code.append(labelStack.pop()).append(":\n"); // Pop and print end label
                    }
                    break;
                case "return":
                    if (i + 1 < leaves.size()) {
                        String returnValue = processExpression(leaves, i + 1);
                        code.append("RETURN ").append(returnValue).append("\n");
                        i = skipExpression(leaves, i + 1);
                    }
                    break;
                case "F_average":
                    code.append("FUNC ").append(getVarName(terminal)).append("\n");
                    break;
            }
        }
    }

    private String processExpression(List<NodeType> leaves, int start) {
        StringBuilder exp = new StringBuilder();
        int i = start;
        while (i < leaves.size() && !leaves.get(i).getChildren().get(2).getVarName().equals(";")) {
            String term = leaves.get(i).getChildren().get(2).getVarName();
            if (term.startsWith("V_") || term.startsWith("F_")) {
                exp.append(getVarName(term));
            } else if (term.equals("(") || term.equals(")") || term.equals(",")) {
                exp.append(term);
            } else if (isOperator(term)) {
                exp.append(" ").append(translateOperator(term)).append(" ");
            } else {
                exp.append(term);
            }
            i++;
        }
        return exp.toString();
    }

    private int skipExpression(List<NodeType> leaves, int start) {
        int i = start;
        while (i < leaves.size() && !leaves.get(i).getChildren().get(2).getVarName().equals(";")) {
            i++;
        }
        return i;
    }

    private String processCondition(List<NodeType> leaves, int start) {
        StringBuilder condition = new StringBuilder();
        int i = start;
        while (i < leaves.size() && !leaves.get(i).getChildren().get(2).getVarName().equals("then")) {
            String term = leaves.get(i).getChildren().get(2).getVarName();
            if (term.startsWith("V_")) {
                condition.append(getVarName(term));
            } else if (isOperator(term)) {
                condition.append(" ").append(translateOperator(term)).append(" ");
            } else {
                condition.append(term);
            }
            i++;
        }
        return condition.toString();
    }

    private int skipToThen(List<NodeType> leaves, int start) {
        int i = start;
        while (i < leaves.size() && !leaves.get(i).getChildren().get(2).getVarName().equals("then")) {
            i++;
        }
        return i;
    }

    private boolean isOperator(String term) {
        return term.equals("and") || term.equals("or") || term.equals("eq") || term.equals("grt") ||
                term.equals("add") || term.equals("sub") || term.equals("mul") || term.equals("div");
    }

    private String translateOperator(String op) {
        switch (op) {
            case "and":
                return "AND";
            case "or":
                return "OR";
            case "eq":
                return "=";
            case "grt":
                return ">";
            case "add":
                return "+";
            case "sub":
                return "-";
            case "mul":
                return "*";
            case "div":
                return "/";
            default:
                return op;
        }
    }

    private String getVarName(String originalName) {
        return symbolTable.getOrDefault(originalName, originalName);
    }

    private String generateTempVar() {
        return "t" + (tempCounter++);
    }

    private String generateLabel() {
        return "L" + (labelCounter++);
    }
}
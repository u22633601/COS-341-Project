import java.util.*;

public class IntermediateCodeGenerator {
    private int tempCounter = 0;
    private int labelCounter = 0;
    private StringBuilder code = new StringBuilder();
    private Map<String, String> symbolTable;
    private boolean debug = true;
    private int indentLevel = 0;

    public IntermediateCodeGenerator(Map<String, String> symbolTable) {
        this.symbolTable = symbolTable;
    }

    public String generateCode(NodeType root) {
        traverseTree(root, 0);
        return code.toString();
    }

    private void traverseTree(NodeType node, int depth) {
        debugPrint(depth, "Traversing node: " + node.getType());

        if (node.getType().equals("LEAFNODES")) {
            handleLEAFNODES(node, depth + 1);
        } else {
            for (NodeType child : node.getChildren()) {
                traverseTree(child, depth + 1);
            }
        }
    }

    private void handleLEAFNODES(NodeType node, int depth) {
        debugPrint(depth, "Handling LEAFNODES");
        List<NodeType> leaves = node.getChildren();
        for (int i = 0; i < leaves.size(); i++) {
            NodeType leaf = leaves.get(i);
            String terminal = leaf.getChildren().get(2).getVarName();
            debugPrint(depth + 1, "Processing terminal: " + terminal);

            switch (terminal) {
                case "main":
                    appendCode("FUNC main");
                    indentLevel++;
                    break;
                case "begin":
                    // Ignore begin
                    break;
                case "end":
                    indentLevel--;
                    appendCode("END_FUNC");
                    break;
                case "if":
                    handleIfStatement(leaves, i, depth + 1);
                    break;
                case "skip":
                    appendCode("REM DO NOTHING");
                    break;
                case "halt":
                    appendCode("STOP");
                    break;
                case "print":
                    handlePrint(leaves, i, depth + 1);
                    i++; // Skip the next token which should be the thing to print
                    break;
                case "return":
                    handleReturn(leaves, i, depth + 1);
                    i++; // Skip the next token which should be the thing to return
                    break;
                default:
                    if (terminal.startsWith("V_")) {
                        handleVariable(leaves, i, depth + 1);
                        i++; // Skip the next token which should be '=' or 'lst'
                    } else if (terminal.startsWith("F_")) {
                        handleFunctionCall(leaves, i, depth + 1);
                        i += 3; // Skip the next 3 tokens which should be the parameters
                    }
                    break;
            }
        }
    }

    private void handleIfStatement(List<NodeType> leaves, int index, int depth) {
        debugPrint(depth, "Handling if statement");
        String condition = processCondition(leaves, index + 1);
        String thenLabel = generateLabel();
        String elseLabel = generateLabel();
        String endLabel = generateLabel();

        appendCode("IF " + condition + " GOTO " + thenLabel);
        appendCode("GOTO " + elseLabel);
        appendCode(thenLabel + ":");
        indentLevel++;

        // Process 'then' part
        int i = index + 1;
        while (i < leaves.size() && !leaves.get(i).getChildren().get(2).getVarName().equals("else")) {
            i++;
        }

        indentLevel--;
        appendCode("GOTO " + endLabel);
        appendCode(elseLabel + ":");
        indentLevel++;

        // Process 'else' part
        if (i < leaves.size() && leaves.get(i).getChildren().get(2).getVarName().equals("else")) {
            i++;
        }

        indentLevel--;
        appendCode(endLabel + ":");
    }

    private void handlePrint(List<NodeType> leaves, int index, int depth) {
        debugPrint(depth, "Handling print");
        String value = leaves.get(index + 1).getChildren().get(2).getVarName();
        appendCode("PRINT " + getVarName(value));
    }

    private void handleReturn(List<NodeType> leaves, int index, int depth) {
        debugPrint(depth, "Handling return");
        String value = leaves.get(index + 1).getChildren().get(2).getVarName();
        if (value.startsWith("\"")) {
            appendCode("RETURN " + value);
        } else {
            appendCode("RETURN " + getVarName(value));
        }
    }

    private void handleVariable(List<NodeType> leaves, int index, int depth) {
        debugPrint(depth, "Handling variable");
        String varName = getVarName(leaves.get(index).getChildren().get(2).getVarName());
        String nextToken = leaves.get(index + 1).getChildren().get(2).getVarName();
        if (nextToken.equals("lst")) {
            appendCode("INPUT " + varName);
        } else if (nextToken.equals("=")) {
            String value = processExpression(leaves, index + 2);
            appendCode(varName + " := " + value);
        }
    }

    private void handleFunctionCall(List<NodeType> leaves, int index, int depth) {
        debugPrint(depth, "Handling function call");
        String funcName = getVarName(leaves.get(index).getChildren().get(2).getVarName());
        String param1 = getVarName(leaves.get(index + 1).getChildren().get(2).getVarName());
        String param2 = getVarName(leaves.get(index + 2).getChildren().get(2).getVarName());
        String param3 = getVarName(leaves.get(index + 3).getChildren().get(2).getVarName());
        appendCode("CALL_" + funcName + "(" + param1 + "," + param2 + "," + param3 + ")");
    }

    private String processCondition(List<NodeType> leaves, int start) {
        StringBuilder condition = new StringBuilder();
        int i = start;
        String operator = "";
        while (i < leaves.size() && !leaves.get(i).getChildren().get(2).getVarName().equals("then")) {
            String term = leaves.get(i).getChildren().get(2).getVarName();
            if (term.equals("and") || term.equals("or")) {
                operator = term.toUpperCase();
                condition.insert(0, operator + "(");
                condition.append(")");
            } else if (isOperator(term)) {
                condition.append(translateOperator(term));
            } else if (term.startsWith("V_")) {
                condition.append(getVarName(term));
            } else if (term.equals("(") || term.equals(")") || term.equals(",")) {
                condition.append(term);
            } else {
                condition.append(term);
            }
            i++;
        }
        return condition.toString();
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
                exp.append(translateOperator(term));
            } else {
                exp.append(term);
            }
            i++;
        }
        return exp.toString();
    }

    private boolean isOperator(String term) {
        return term.equals("and") || term.equals("or") || term.equals("eq") || term.equals("grt") ||
                term.equals("add") || term.equals("sub") || term.equals("mul") || term.equals("div");
    }

    private String translateOperator(String op) {
        switch (op) {
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
            case "sqrt":
                return "SQR";
            case "and":
                return "AND";
            case "or":
                return "OR";
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

    private void appendCode(String line) {
        for (int i = 0; i < indentLevel; i++) {
            code.append("  ");
        }
        code.append(line).append("\n");
    }

    private void debugPrint(int depth, String message) {
        if (debug) {
            StringBuilder indent = new StringBuilder();
            for (int i = 0; i < depth; i++) {
                indent.append("  ");
            }
            System.out.println(indent + message);
        }
    }
}
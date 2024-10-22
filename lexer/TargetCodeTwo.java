
import java.io.*;
import java.util.*;

public class TargetCodeTwo {
    private static final int STACK_SIZE = 30;
    private static int lineNumber = 10;
    private static Map<String, String> variableMap = new HashMap<>();
    private static Map<String, Integer> labelMap = new HashMap<>();
    private static List<String> basicCode = new ArrayList<>();
    private static int labelCounter = 0;

    public static void main(String[] args) {
        loadSymbolTable();
        List<String> inputCode = readInputCode();
        processInputCode(inputCode);
        updateGotoLines();
        printBasicCode();
    }

    private static void loadSymbolTable() {
        try (BufferedReader reader = new BufferedReader(new FileReader("Symbol.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(" : ");
                if (parts.length == 3) {
                    String originalName = parts[0];
                    String generatedName = parts[1];
                    String type = parts[2].trim();
                    
                    // Map variables based on their type
                    if (type.equals("num")) {
                        variableMap.put(originalName, "V" + (variableMap.size() + 1) + "%");
                    } else if (type.equals("text")) {
                        variableMap.put(originalName, ((char)('A' + variableMap.size())) + "$");
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static List<String> readInputCode() {
        List<String> code = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader("input.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                code.add(line.trim());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return code;
    }

    private static void processInputCode(List<String> inputCode) {
        addLine("DIM M(7," + STACK_SIZE + ")");
        addLine("SP = 0");
        addLine("REM main");

        boolean inBlock = false;
        StringBuilder currentBlock = new StringBuilder();

        for (String line : inputCode) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("num") || line.startsWith("text")) continue;

            if (line.equals("begin")) {
                inBlock = true;
                continue;
            }

            if (line.equals("end")) {
                if (inBlock) {
                    processBlock(currentBlock.toString());
                    currentBlock = new StringBuilder();
                    inBlock = false;
                }
                continue;
            }

            if (inBlock) {
                currentBlock.append(line).append("\n");
            }
        }
        addLine("END");
    }

    private static void processBlock(String block) {
        String[] statements = block.split(";");
        for (String stmt : statements) {
            stmt = stmt.trim();
            if (stmt.isEmpty()) continue;

            if (stmt.contains("<")) {
                // Input statement
                String var = stmt.split("<")[0].trim();
                addLine("INPUT " + translateVariable(var));
            } else if (stmt.startsWith("if")) {
                processIfStatement(stmt);
            } else if (stmt.contains("=")) {
                // Assignment
                String[] parts = stmt.split("=");
                String left = parts[0].trim();
                String right = processExpression(parts[1].trim());
                addLine("LET " + translateVariable(left) + " = " + right);
            } else if (stmt.startsWith("return")) {
                String retVal = stmt.substring(7).trim();
                if (retVal.startsWith("\"")) {
                    // String return
                    addLine("LET M(0,SP) = " + retVal);
                } else {
                    // Variable return
                    addLine("LET M(0,SP) = " + translateVariable(retVal));
                }
            } else if (stmt.equals("skip")) {
                addLine("REM SKIP");
            }
        }
    }

    private static String processExpression(String expr) {
        expr = expr.trim();
        if (expr.contains("add (")) {
            int start = expr.indexOf("(") + 1;
            int end = findClosingParenthesis(expr, start - 1);
            String innerContent = expr.substring(start, end).trim();
            String[] parts = innerContent.split(",");
            return translateVariable(parts[0].trim()) + " + " + translateVariable(parts[1].trim());
        } else if (expr.contains("mul (")) {
            int start = expr.indexOf("(") + 1;
            int end = findClosingParenthesis(expr, start - 1);
            String innerContent = expr.substring(start, end).trim();
            String[] parts = innerContent.split(",");
            return translateVariable(parts[0].trim()) + " * " + translateVariable(parts[1].trim());
        }
        return translateVariable(expr);
    }

    private static void processIfStatement(String stmt) {
        try {
            if (stmt.contains("and (")) {
                // Handle AND condition
                String trueLabel = "LABEL" + (++labelCounter);
                String falseLabel = "LABEL" + (++labelCounter);
                String endLabel = "LABEL" + (++labelCounter);
    
                String[] conditions = extractAndConditions(stmt);
                if (conditions.length >= 2) {
                    String firstCond = processCondition(conditions[0].trim());
                    String secondCond = processCondition(conditions[1].trim());
    
                    addLine("IF " + firstCond + " THEN GOTO " + trueLabel);
                    addLine("GOTO " + falseLabel);
                    addLine("REM " + trueLabel);
                    addLine("IF " + secondCond + " THEN GOTO " + endLabel);
                    addLine("GOTO " + falseLabel);
                    
                    // Process then block
                    String thenBlock = extractThenBlock(stmt);
                    if (!thenBlock.isEmpty()) {
                        processBlock(thenBlock);
                    }
                    addLine("GOTO " + endLabel);
                    
                    addLine("REM " + falseLabel);
                    // Process else block
                    String elseBlock = extractElseBlock(stmt);
                    if (!elseBlock.isEmpty()) {
                        processBlock(elseBlock);
                    }
                    addLine("REM " + endLabel);
                }
            } else if (stmt.contains("or (")) {
                // Handle OR condition
                String trueLabel = "LABEL" + (++labelCounter);
                String falseLabel = "LABEL" + (++labelCounter);
                String endLabel = "LABEL" + (++labelCounter);
    
                String[] conditions = extractOrConditions(stmt);
                if (conditions.length >= 2) {
                    String firstCond = processCondition(conditions[0].trim());
                    String secondCond = processCondition(conditions[1].trim());
    
                    addLine("IF " + firstCond + " THEN GOTO " + trueLabel);
                    addLine("IF " + secondCond + " THEN GOTO " + trueLabel);
                    addLine("GOTO " + falseLabel);
                    
                    addLine("REM " + trueLabel);
                    // Process then block
                    String thenBlock = extractThenBlock(stmt);
                    if (!thenBlock.isEmpty()) {
                        processBlock(thenBlock);
                    }
                    addLine("GOTO " + endLabel);
                    
                    addLine("REM " + falseLabel);
                    // Process else block
                    String elseBlock = extractElseBlock(stmt);
                    if (!elseBlock.isEmpty()) {
                        processBlock(elseBlock);
                    }
                    addLine("REM " + endLabel);
                }
            }
        } catch (Exception e) {
            System.err.println("Error processing if statement: " + stmt);
        }
    }

    private static String[] extractAndConditions(String stmt) {
        try {
            String content = extractParenthesesContent(stmt.substring(stmt.indexOf("and")));
            if (content != null) {
                return content.split(",");
            }
        } catch (Exception e) {
            System.err.println("Error extracting AND conditions from: " + stmt);
        }
        return new String[0];
    }
    
    private static String[] extractOrConditions(String stmt) {
        try {
            String content = extractParenthesesContent(stmt.substring(stmt.indexOf("or")));
            if (content != null) {
                return content.split(",");
            }
        } catch (Exception e) {
            System.err.println("Error extracting OR conditions from: " + stmt);
        }
        return new String[0];
    }

    private static String processCondition(String cond) {
        try {
            cond = cond.trim();
            if (cond.contains("grt (")) {
                // Find the content between outermost parentheses
                String innerContent = extractParenthesesContent(cond);
                if (innerContent != null) {
                    String[] parts = innerContent.split(",");
                    if (parts.length == 2) {
                        return translateVariable(parts[0].trim()) + " > " + translateVariable(parts[1].trim());
                    }
                }
            } else if (cond.contains("eq (")) {
                // Find the content between outermost parentheses
                String innerContent = extractParenthesesContent(cond);
                if (innerContent != null) {
                    String[] parts = innerContent.split(",");
                    if (parts.length == 2) {
                        return translateVariable(parts[0].trim()) + " = " + translateVariable(parts[1].trim());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error processing condition: " + cond);
        }
        return cond;
    }

    private static String extractParenthesesContent(String expr) {
        try {
            int start = expr.indexOf("(");
            if (start != -1) {
                int end = findClosingParenthesis(expr, start);
                if (end != -1) {
                    return expr.substring(start + 1, end).trim();
                }
            }
        } catch (Exception e) {
            System.err.println("Error extracting parentheses content from: " + expr);
        }
        return null;
    }

    private static int findClosingParenthesis(String str, int startIndex) {
        int count = 1;
        for (int i = startIndex + 1; i < str.length(); i++) {
            if (str.charAt(i) == '(') {
                count++;
            } else if (str.charAt(i) == ')') {
                count--;
                if (count == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static String extractThenBlock(String stmt) {
        int thenIndex = stmt.indexOf("then") + 4;
        int elseIndex = stmt.indexOf("else");
        
        if (elseIndex == -1) {
            return "";
        }
        
        String block = stmt.substring(thenIndex, elseIndex).trim();
        if (block.startsWith("begin")) {
            block = block.substring(5);
        }
        if (block.endsWith("end")) {
            block = block.substring(0, block.length() - 3);
        }
        return block.trim();
    }

    private static String extractElseBlock(String stmt) {
        int elseIndex = stmt.indexOf("else") + 4;
        
        String block = stmt.substring(elseIndex).trim();
        if (block.startsWith("begin")) {
            block = block.substring(5);
        }
        if (block.endsWith("end")) {
            block = block.substring(0, block.length() - 3);
        }
        return block.trim();
    }

    private static String translateVariable(String var) {
        var = var.trim();
        if (var.equals("0")) return "0";
        return variableMap.getOrDefault(var, var);
    }

    private static void addLine(String code) {
        basicCode.add(lineNumber + " " + code);
        lineNumber += 10;
    }

    private static void updateGotoLines() {
        for (int i = 0; i < basicCode.size(); i++) {
            String line = basicCode.get(i);
            if (line.contains("GOTO") || line.contains("THEN")) {
                for (String label : labelMap.keySet()) {
                    if (line.contains(label)) {
                        line = line.replace(label, labelMap.get(label).toString());
                    }
                }
                basicCode.set(i, line);
            }
        }
    }

    private static void printBasicCode() {
        for (String line : basicCode) {
            System.out.println(line);
        }
    }
}
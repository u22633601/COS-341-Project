import java.io.*;
import java.util.*;

public class TargetCode {
    private static final int STACK_SIZE = 30;
    private static int lineNumber = 10;
    private static Map<String, String> variableMap = new HashMap<>();
    private static Map<String, Integer> labelMap = new HashMap<>();
    private static List<String> basicCode = new ArrayList<>();
    private static char nextVarName = 'A';

    public static void main(String[] args) {
        List<String> intermediateCode = readIntermediateCode();
        generateBasicCode(intermediateCode);
        updateGotoLines();
        printBasicCode();
    }

    private static List<String> readIntermediateCode() {
        List<String> code = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader("intermediateCode.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                code.add(line.trim());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return code;
    }

    private static void generateBasicCode(List<String> intermediateCode) {
        addLine("DIM M(7," + STACK_SIZE + ")");
        addLine("SP = 0");
    
        for (String line : intermediateCode) {
            if (line.startsWith("LABEL")) {
                String label = line.split(" ")[1];
                labelMap.put(label, lineNumber);
                addLine("REM " + label);
            } else if (line.startsWith("IF")) {
                String[] parts = line.split(" ");
                String condition = translateCondition(parts[1]);
                String thenLabel = parts[3];
                String elseLabel = parts[5];
                addLine("IF " + condition + " THEN GOTO " + thenLabel);
                addLine("GOTO " + elseLabel);
            } else if (line.startsWith("GOTO")) {
                String label = line.split(" ")[1];
                addLine("GOTO " + label);
            } else if (line.contains(":=")) {
                String[] parts = line.split(":=");
                String left = translateVariable(parts[0].trim());
                String right = translateExpression(parts[1].trim());
                addLine("LET " + left + " = " + right);
            } else if (line.startsWith("PRINT")) {
                String var = translateVariable(line.split(" ")[1]);
                addLine("PRINT " + var);
            } else if (line.startsWith("CALL")) {
                String function = line.split("_")[1].split("\\(")[0];
                addLine("GOSUB " + function);
            } else if (line.equals("RETURN")) {
                addLine("RETURN");
            } else if (line.equals("HALT")) {
                addLine("END");
            } else if (line.startsWith("INPUT")) {
                String var = translateVariable(line.split(" ")[1]);
                addLine("INPUT " + var);
            }
        }
    }
    
    private static void updateGotoLines() {
        for (int i = 0; i < basicCode.size(); i++) {
            String line = basicCode.get(i);
            if (line.contains("GOTO ") || line.contains("GOSUB ")) {
                String[] parts = line.split(" ");
                String label = parts[parts.length - 1];
                if (labelMap.containsKey(label)) {
                    int targetLine = labelMap.get(label);
                    basicCode.set(i, line.substring(0, line.lastIndexOf(" ") + 1) + targetLine);
                }
            }
        }
    }

    private static void addLine(String code) {
        basicCode.add(lineNumber + " " + code);
        lineNumber += 10;
    }

    private static void printBasicCode() {
        for (String line : basicCode) {
            System.out.println(line);
        }
    }

    private static String translateCondition(String condition) {
        String[] parts = condition.split("\\(|,|\\)");
        String op = parts[0];
        String left = translateVariable(parts[1]);
        String right = translateVariable(parts[2]);
        
        switch (op) {
            case "grt": return left + " > " + right;
            case "eq": return left + " = " + right;
            default: return left + " " + op + " " + right;
        }
    }

    private static String translateExpression(String expr) {
        if (expr.contains("(")) {
            String[] parts = expr.split("\\(|,|\\)");
            String operation = parts[0];
            String left = translateVariable(parts[1]);
            String right = translateVariable(parts[2]);
            
            switch (operation) {
                case "add":
                    return left + " + " + right;
                case "sub":
                    return left + " - " + right;
                case "mul":
                    return left + " * " + right;
                case "div":
                    return left + " / " + right;
                default:
                    return expr; // If it's an unknown operation, return as is
            }
        }
        return translateVariable(expr);
    }

    private static String translateVariable(String var) {
        if (var == null || var.isEmpty()) {
            return "";
        }
        if (var.matches("v\\d+")) {
            return variableMap.computeIfAbsent(var, k -> String.valueOf(nextVarName++));
        } else if (var.startsWith("M[")) {
            String[] parts = var.split("\\[|\\]");
            return "M(" + parts[1] + ",SP)";
        } else {
            return var;
        }
    }

    
}
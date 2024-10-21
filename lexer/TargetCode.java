import java.io.*;
import java.util.*;

public class TargetCode {
    private static final int STACK_SIZE = 30;
    private static int lineNumber = 10;
    private static Map<String, String> symbolTable = new HashMap<>();
    private static Map<String, Integer> labelMap = new HashMap<>();
    private static List<String> basicCode = new ArrayList<>();
    private static char nextVar = 'A';
    private static int endProgramLine = 0;

    public static void main(String[] args) {
        loadSymbolTable();
        List<String> intermediateCode = readIntermediateCode();
        generateBasicCode(intermediateCode);
        printBasicCode();
    }

    private static void loadSymbolTable() {
        try (BufferedReader reader = new BufferedReader(new FileReader("Symbol.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(" : ");
                String varType = parts[2].trim();
                String basicVar = (varType.equals("text")) ? (nextVar++ + "$") : String.valueOf(nextVar++);
                symbolTable.put(parts[1], basicVar);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        
        // First pass: identify labels and their line numbers
        int tempLineNumber = lineNumber;
        for (String line : intermediateCode) {
            if (line.startsWith("LABEL")) {
                String label = line.split(" ")[1];
                labelMap.put(label, tempLineNumber);
            }
            tempLineNumber += 10;
        }
        
        // Second pass: generate actual code
        for (String line : intermediateCode) {
            if (line.startsWith("LABEL")) {
                String label = line.split(" ")[1];
                addLine("REM " + label);
                if (label.equals("end_program")) {
                    endProgramLine = lineNumber - 10;
                }
            } else if (line.startsWith("GOTO")) {
                String label = line.split(" ")[1];
                if (label.equals("end_program")) {
                    addLine("GOTO " + endProgramLine);
                } else {
                    addLine("GOTO " + labelMap.getOrDefault(label, endProgramLine));
                }
            } else if (line.startsWith("IF")) {
                String[] parts = line.split(" ");
                String condition = translateCondition(parts[1]);
                String thenLabel = parts[3];
                String elseLabel = parts[5];
                int thenLine = labelMap.getOrDefault(thenLabel, endProgramLine);
                addLine("IF " + condition + " THEN GOTO " + thenLine);
                addLine("GOTO " + labelMap.getOrDefault(elseLabel, endProgramLine));
            } else if (line.contains(":=")) {
                String[] parts = line.split(":=");
                String left = translateVariable(parts[0].trim());
                String right = translateExpression(parts[1].trim());
                addLine("LET " + left + " = " + right);
            } else if (line.startsWith("CALL")) {
                String function = line.split("_")[1].split("\\(")[0];
                addLine("GOSUB " + labelMap.getOrDefault(function, endProgramLine));
            } else if (line.equals("RETURN")) {
                addLine("RETURN");
            } else if (line.startsWith("PRINT")) {
                String var = translateVariable(line.split(" ")[1]);
                addLine("PRINT " + var);
            } else if (line.startsWith("INPUT")) {
                String var = translateVariable(line.split(" ")[1]);
                addLine("INPUT " + var);
            } else if (line.equals("HALT")) {
                addLine("END");
            }
        }
    }

    private static String translateCondition(String condition) {
        String[] parts = condition.split("\\(|,|\\)");
        String op = parts[0];
        String left = translateVariable(parts[1]);
        String right = translateVariable(parts[2]);
        
        switch (op) {
            case "eq": return left + " = " + right;
            case "grt": return left + " > " + right;
            default: return left + " " + op + " " + right;
        }
    }

    private static String translateExpression(String expr) {
        if (expr.startsWith("CALL")) {
            return "M(0,SP)";
        } else if (expr.contains("(")) {
            String[] parts = expr.split("\\(|,|\\)");
            String op = parts[0];
            String left = translateVariable(parts[1]);
            String right = parts.length > 2 ? translateVariable(parts[2]) : "";
            
            switch (op) {
                case "add": return left + " + " + right;
                case "sub": return left + " - " + right;
                case "mul": return left + " * " + right;
                case "div": return left + " / " + right;
                default: return expr;
            }
        } else {
            return translateVariable(expr);
        }
    }

    private static Map<String, String> variableMap = new HashMap<>();
    private static char nextVarName = 'A';
    
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

    private static void addLine(String code) {
        basicCode.add(lineNumber + " " + code);
        lineNumber += 10;
    }

    private static void printBasicCode() {
        for (String line : basicCode) {
            System.out.println(line);
        }
    }
}
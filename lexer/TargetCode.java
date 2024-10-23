import java.io.*;
import java.util.*;

public class TargetCode {
    private static final int STACK_SIZE = 30;
    private static int lineNumber = 10;
    private static Map<String, String> variableMap = new HashMap<>();
    private static Map<String, Integer> labelMap = new HashMap<>();
    private static List<String> basicCode = new ArrayList<>();
    private static Map<String, String> tempVarMap = new HashMap<>();
    private static int tempNumVarCounter = 1;

    public static void main(String[] args) {
        loadSymbolTable();  // Add this call to main
        List<String> intermediateCode = readIntermediateCode();
        generateBasicCode(intermediateCode);
        updateGotoLines();
        printBasicCode();
    }

    // private static void loadSymbolTable() {
    //     try (BufferedReader reader = new BufferedReader(new FileReader("Symbol.txt"))) {
    //         String line;
    //         int numVarCounter = 1;  // Counter for numeric variables (V1%, V2%, etc.)
    //         char textVarCounter = 'A';  // Counter for text variables (A$, B$, etc.)
            
    //         while ((line = reader.readLine()) != null) {
    //             String[] parts = line.split(" : ");
    //             if (parts.length == 3) {
    //                 String originalName = parts[0];  // V_x, V_y, etc.
    //                 String generatedName = parts[1]; // v101, v102, etc.
    //                 String type = parts[2].trim();   // num or text
                    
    //                 if (type.equals("num")) {
    //                     // Create numeric variable (V1%, V2%, etc.)
    //                     variableMap.put(generatedName, "V" + numVarCounter + "%");
    //                     numVarCounter++;
    //                 } else if (type.equals("text")) {
    //                     // Create text variable (A$, B$, etc.)
    //                     variableMap.put(generatedName, textVarCounter + "$");
    //                     textVarCounter++;
    //                 }
    //             }
    //         }
    //     } catch (IOException e) {
    //         e.printStackTrace();
    //     }
    // }

    private static void loadSymbolTable() {
        try (BufferedReader reader = new BufferedReader(new FileReader("Symbol.txt"))) {
            String line;
            int numVarCounter = 1;  // Counter for numeric variables (V1%, V2%, etc.)
            char textVarCounter = 'A';  // Counter for text variables (A$, B$, etc.)
                
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(" : ");
                if (parts.length == 3) {
                    String originalName = parts[0];  // V_x, V_y, etc.
                    String generatedName = parts[1]; // v101, v102, etc.
                    String type = parts[2].trim();   // num or text
                        
                    if (type.equals("num")) {
                        // Create numeric variable (V1%, V2%, etc.)
                        variableMap.put(generatedName, "V" + numVarCounter + "%");
                        numVarCounter++;
                    } else if (type.equals("text")) {
                        // Create text variable (A$, B$, etc.)
                        variableMap.put(generatedName, textVarCounter + "$");
                        textVarCounter++;
                    }
                }
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
        addLine("DIM R$");
    
        String endLabel = findEndLabel(intermediateCode);
        tempVarMap.clear();
        tempNumVarCounter = 4;  // Start after the main variables (V1%, V2%, V3%)
    
        // First pass: map all temporary variables in order of appearance
        for (String line : intermediateCode) {
            line = line.trim();
            if (line.contains(":=")) {
                String[] parts = line.split(":=");
                String left = parts[0].trim();
                String right = parts[1].trim();
                
                // Map temporary variable
                if (left.startsWith("t")) {
                    String newVar = "V" + tempNumVarCounter++ + "%";
                    tempVarMap.putIfAbsent(left, newVar);
                }
            }
        }
    
        // Second pass: generate code
        for (String line : intermediateCode) {
            line = line.trim();
            
            if (line.isEmpty()) continue;
            
            if (line.startsWith("LABEL") || line.matches("L\\d+:")) {
                String label = line.startsWith("LABEL") ? line.split(" ")[1] : line.split(":")[0];
                labelMap.put(label, lineNumber);
                addLine("REM " + label);
            }
            else if (line.startsWith("IF")) {
                String[] parts = line.split(" ");
                String condition = parts[1];
                String thenLabel = parts[3];
                String elseLabel = parts[6];
                addLine("IF " + translateVariable(condition) + " THEN GOTO " + thenLabel);
                addLine("GOTO " + elseLabel);
            }
            else if (line.startsWith("GOTO")) {
                String label = line.split(" ")[1];
                addLine("GOTO " + label);
            }
            else if (line.contains(":=")) {
                String[] parts = line.split(":=");
                String left = parts[0].trim();
                String right = parts[1].trim();
                
                if (right.contains("AND") || right.contains("OR")) {
                    String operator = right.contains("AND") ? "AND" : "OR";
                    String[] operands = right.split(operator);
                    String leftOp = translateVariable(operands[0].trim());
                    String rightOp = translateVariable(operands[1].trim());
                    addLine("LET " + translateVariable(left) + " = " + leftOp + " " + operator + " " + rightOp);
                } else if (right.contains(">") || right.contains("=")) {
                    addLine("LET " + translateVariable(left) + " = " + translateExpression(right));
                } else if (right.contains("*")) {
                    String[] mulParts = right.split("\\*");
                    String leftOp = translateVariable(mulParts[0].trim());
                    String rightOp = translateVariable(mulParts[1].trim());
                    addLine("LET " + translateVariable(left) + " = " + leftOp + " * " + rightOp);
                } else {
                    addLine("LET " + translateVariable(left) + " = " + translateVariable(right));
                }
            }
            else if (line.startsWith("PRINT")) {
                String content = line.substring(6).trim();
                addLine("PRINT " + (content.startsWith("\"") ? content : translateVariable(content)));
            }
            else if (line.startsWith("INPUT")) {
                String var = translateVariable(line.split(" ")[1]);
                addLine("INPUT " + var);
            }
            else if (line.equals("STOP")) {
                addLine("END");
            }
        }
    }
    
    private static String translateVariable(String var) {
        if (var == null || var.isEmpty()) {
            return "";
        }
        
        // Handle temporary variables
        if (var.matches("t\\d+")) {
            return tempVarMap.getOrDefault(var, var);
        }
        
        // Handle regular variables
        if (var.matches("v\\d+")) {
            return variableMap.getOrDefault(var, var);
        } else if (var.startsWith("M[")) {
            String[] parts = var.split("\\[|\\]");
            return "M(" + parts[1] + ",SP)";
        } else if (var.startsWith("\"")) {
            return var;
        } else {
            try {
                Integer.parseInt(var);
                return var;
            } catch (NumberFormatException e) {
                return variableMap.getOrDefault(var, var);
            }
        }
    }
    
    
    
    private static String translateExpression(String expr) {
        expr = expr.trim();
        
        if (expr.contains("=")) {
            String[] parts = expr.split("=");
            String left = translateVariable(parts[0].trim());
            String right = translateVariable(parts[1].trim());
            return "(" + left + " = " + right + ")";
        }
        
        if (expr.contains(">")) {
            String[] parts = expr.split(">");
            String left = translateVariable(parts[0].trim());
            String right = translateVariable(parts[1].trim());
            return "(" + left + " > " + right + ")";
        }
        
        return translateVariable(expr);
    }
    
    private static String findEndLabel(List<String> intermediateCode) {
        for (String line : intermediateCode) {
            if (line.matches("L\\d+:") && intermediateCode.indexOf(line) == intermediateCode.size() - 2) {
                return line.split(":")[0];
            }
        }
        return null;
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
        // Check if condition is just a variable
        if (!condition.contains("(") && !condition.contains(",")) {
            return translateVariable(condition);
        }
    
        // Handle complex conditions
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

    
}
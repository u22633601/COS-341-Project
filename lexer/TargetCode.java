import java.io.*;
import java.util.*;

public class TargetCode {
    private static final int STACK_SIZE = 30;
    private static int lineNumber = 10;
    private static Map<String, String> variableMap = new HashMap<>();
    private static Map<String, Integer> labelMap = new HashMap<>();
    private static List<String> basicCode = new ArrayList<>();
    private static Map<String, String> tempVarMap = new HashMap<>();  // Add this field for temporary variables
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

    // The translateVariable method needs to be updated to use the new variable format
    // private static String translateVariable(String var) {
    //     if (var == null || var.isEmpty()) {
    //         return "";
    //     }
    //     if (var.matches("v\\d+")) {
    //         return variableMap.getOrDefault(var, var);
    //     } else if (var.startsWith("M[")) {
    //         String[] parts = var.split("\\[|\\]");
    //         return "M(" + parts[1] + ",SP)";
    //     } else {
    //         return var;
    //     }
    // }

    private static String translateVariable(String var) {
        if (var == null || var.isEmpty()) {
            return "";
        }
        
        // Handle temporary variables (t1, t2, t3, etc.)
        if (var.startsWith("t")) {
            return tempVarMap.computeIfAbsent(var, k -> "V" + tempNumVarCounter++ + "%");
        }
        
        // Handle regular variables
        if (var.matches("v\\d+")) {
            return variableMap.getOrDefault(var, var);
        } else if (var.startsWith("M[")) {
            String[] parts = var.split("\\[|\\]");
            return "M(" + parts[1] + ",SP)";
        } else if (var.startsWith("\"")) {  // Handle string literals
            return var;  // Return string literals as-is
        } else {
            // Handle numeric literals
            try {
                Integer.parseInt(var);
                return var;
            } catch (NumberFormatException e) {
                return variableMap.getOrDefault(var, var);
            }
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
        addLine("DIM R$");  // String variable for return values
    
        // First pass: find end label
        String endLabel = findEndLabel(intermediateCode);
    
        for (String line : intermediateCode) {
            line = line.trim();
            
            if (line.isEmpty()) continue;
            
            if (line.startsWith("LABEL") || line.matches("L\\d+:")) {
                String label;
                if (line.startsWith("LABEL")) {
                    label = line.split(" ")[1];
                } else {
                    label = line.split(":")[0];
                }
                labelMap.put(label, lineNumber);
                addLine("REM " + label);
            }
            else if (line.startsWith("IF")) {
                String[] parts = line.split(" ");
                String condition = parts[1];
                String thenLabel = parts[3];
                String elseLabel = parts[6];
                addLine("IF " + translateCondition(condition) + " THEN GOTO " + thenLabel);
                addLine("GOTO " + elseLabel);
            }
            else if (line.startsWith("GOTO")) {
                String label = line.split(" ")[1];
                addLine("GOTO " + label);
            }
            else if (line.contains("=")) {
                String[] parts = line.split("=");
                String left = parts[0].trim();
                String right = parts[1].trim();
                
                if (right.startsWith("\"")) {
                    // For string literals, use R$ directly
                    addLine("LET R$ = " + right);
                } else {
                    addLine("LET " + translateVariable(left) + " = " + translateExpression(right));
                }
            }
            else if (line.startsWith("PRINT")) {
                String var = translateVariable(line.split(" ")[1]);
                addLine("PRINT " + var);
            }
            else if (line.startsWith("INPUT")) {
                String var = translateVariable(line.split(" ")[1]);
                addLine("INPUT " + var);
            }
            else if (line.startsWith("RETURN")) {
                // Instead of RETURN, use GOTO to end label
                if (line.length() > 6) {  // Has a return value
                    String retVal = line.substring(7).trim();
                    if (retVal.startsWith("\"")) {
                        addLine("LET R$ = " + retVal);
                    } else {
                        addLine("LET M(0,SP) = " + translateVariable(retVal));
                    }
                }
                if (endLabel != null) {
                    addLine("GOTO " + endLabel);
                }
            }
            else if (line.equals("STOP")) {
                addLine("END");
            }
        }
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

    private static String translateExpression(String expr) {
        // Handle simple comparison expressions (like t1 > t2)
        if (expr.contains(">")) {
            String[] parts = expr.split(">");
            String left = translateVariable(parts[0].trim());
            String right = translateVariable(parts[1].trim());
            return left + " > " + right;
        }
        
        // Handle existing cases
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
                    return expr; 
            }
        }
        return translateVariable(expr);
    }

    

    
}
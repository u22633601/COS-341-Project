import java.io.*;
import java.util.*;

public class TargetCode {
    private static final int STACK_SIZE = 30;
    private static int lineNumber = 10;
    private static Map<String, String> variableMap = new HashMap<>();
    private static Map<String, Integer> labelMap = new HashMap<>();
    private static List<String> basicCode = new ArrayList<>();
    private static Map<String, String> tempVarMap = new HashMap<>();
    private static int tempNumVarCounter = 50;
    private static final String SYMBOL_FILE = "Symbol.txt";
    private static final String INTERMEDIATE_FILE = "intermediateCode.txt";

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java TargetCode <output-file>");
            System.exit(1);
        }

        String outputFile = args[0];

        try {
            // Check if input files exist
            if (!new File(SYMBOL_FILE).exists()) {
                System.err.println("Error: Symbol table file '" + SYMBOL_FILE + "' not found");
                System.exit(1);
            }
            if (!new File(INTERMEDIATE_FILE).exists()) {
                System.err.println("Error: Intermediate code file '" + INTERMEDIATE_FILE + "' not found");
                System.exit(1);
            }

            // Load symbol table
            System.out.println("Loading symbol table from " + SYMBOL_FILE + "...");
            loadSymbolTable();

            // Read intermediate code
            System.out.println("Reading intermediate code from " + INTERMEDIATE_FILE + "...");
            List<String> intermediateCode = readIntermediateCode();

            // Generate BASIC code
            System.out.println("Generating BASIC code...");
            generateBasicCode(intermediateCode);
            updateGotoLines();

            // Write output
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
                for (String line : basicCode) {
                    writer.write(line);
                    writer.newLine();
                }
                System.out.println("BASIC code has been written to " + outputFile);
            } catch (IOException e) {
                System.err.println("Error writing to output file: " + e.getMessage());
                System.exit(1);
            }

        } catch (Exception e) {
            System.err.println("Error during code generation: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void loadSymbolTable() {
        try (BufferedReader reader = new BufferedReader(new FileReader("Symbol.txt"))) {
            String line;
            int numVarCounter = 1;
            char textVarCounter = 'A';

            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(" : ");
                if (parts.length == 3) {
                    String originalName = parts[0];
                    String generatedName = parts[1];
                    String type = parts[2].trim();

                    if (type.equals("num")) {
                        variableMap.put(generatedName, "V" + numVarCounter + "%");
                        numVarCounter++;
                    } else if (type.equals("text")) {
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
        addLine("DIM RT%");

        tempVarMap.clear();
        tempNumVarCounter = 50;

        for (String line : intermediateCode) {
            if (line.contains(":=")) {
                String[] parts = line.split(":=");
                String left = parts[0].trim();
                if (left.startsWith("t")) {
                    String newVar = "V" + tempNumVarCounter++ + "%";
                    tempVarMap.putIfAbsent(left, newVar);
                }
            }
        }

        Map<String, Integer> functionPositions = new HashMap<>();
        boolean inFunction = false;
        String currentFunc = null;

        for (int i = 0; i < intermediateCode.size(); i++) {
            String line = intermediateCode.get(i).trim();

            if (line.isEmpty())
                continue;

            if (line.equals("REM BEGIN")) {
                inFunction = true;
                i++;
                currentFunc = intermediateCode.get(i).trim();
                addLine("END");
                addLine("REM Function " + currentFunc);
                functionPositions.put(currentFunc, lineNumber - 10);
                continue;
            }

            if (line.equals("REM END")) {
                if (inFunction) {
                    inFunction = false;
                    currentFunc = null;
                    if (!basicCode.get(basicCode.size() - 1).contains("RETURN")) {
                        addLine("RETURN");
                    }
                }
                continue;
            }

            if (line.startsWith("LABEL") || line.matches("L\\d+:")) {
                String label = line.startsWith("LABEL") ? line.split(" ")[1] : line.split(":")[0];
                labelMap.put(label, lineNumber);
                addLine("REM " + label);
            } else if (line.startsWith("IF")) {
                String[] parts = line.split(" ");
                String condition = parts[1];
                String thenLabel = parts[3];
                String elseLabel = parts[6];
                addLine("IF " + translateVariable(condition) + " THEN GOTO " + thenLabel);
                addLine("GOTO " + elseLabel);
            } else if (line.startsWith("GOTO")) {
                String label = line.split(" ")[1];
                addLine("GOTO " + label);
            } else if (line.startsWith("RETURN")) {
                String returnVar = line.split(" ")[1];
                addLine("LET RT% = " + translateVariable(returnVar));
                if (inFunction) {
                    addLine("RETURN");
                }
            } else if (line.contains("CALL_")) {
                String[] parts = line.split(":=|\\(|\\)");
                String resultVar = parts[0].trim();
                String functionName = parts[1].trim().substring(5);
                String[] args = parts[2].split(",");

                addLine("LET M(0,SP) = " + (lineNumber + 20));
                for (int j = 0; j < args.length; j++) {
                    addLine("LET SP = SP + 1");
                    addLine("LET M(" + j + ",SP) = " + translateVariable(args[j].trim()));
                }
                addLine("GOSUB " + functionName + "_PLACEHOLDER");
                addLine("LET SP = SP - " + args.length);
                addLine("LET " + translateVariable(resultVar) + " = RT%");
            } else if (line.contains(":=")) {
                String[] parts = line.split(":=");
                String left = parts[0].trim();
                String right = parts[1].trim();

                if (right.startsWith("NOT ")) {

                    String operand = right.substring(4).trim();
                    addLine("LET " + translateVariable(left) + " = NOT(" + translateVariable(operand) + ")");
                } else if (right.startsWith("SQR ")) {

                    String operand = right.substring(4).trim();
                    addLine("LET " + translateVariable(left) + " = SQR(" + translateVariable(operand) + ")");
                } else if (right.contains("AND") || right.contains("OR")) {
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
                } else if (right.contains("+")) {
                    String[] addParts = right.split("\\+");
                    String leftOp = translateVariable(addParts[0].trim());
                    String rightOp = translateVariable(addParts[1].trim());
                    addLine("LET " + translateVariable(left) + " = " + leftOp + " + " + rightOp);
                } else if (right.contains("-")) {
                    String[] subParts = right.split("-");
                    String leftOp = translateVariable(subParts[0].trim());
                    String rightOp = translateVariable(subParts[1].trim());
                    addLine("LET " + translateVariable(left) + " = " + leftOp + " - " + rightOp);
                } else if (right.contains("/")) {
                    String[] divParts = right.split("/");
                    String leftOp = translateVariable(divParts[0].trim());
                    String rightOp = translateVariable(divParts[1].trim());
                    addLine("LET " + translateVariable(left) + " = " + leftOp + " / " + rightOp);
                } else {
                    addLine("LET " + translateVariable(left) + " = " + translateVariable(right));
                }
            } else if (line.startsWith("PRINT")) {
                String content = line.substring(6).trim();
                addLine("PRINT " + (content.startsWith("\"") ? content : translateVariable(content)));
            } else if (line.startsWith("INPUT")) {
                String var = translateVariable(line.split(" ")[1]);
                addLine("INPUT " + var);
            } else if (line.equals("STOP") && !inFunction) {
                addLine("END");
            }
        }

        for (int i = 0; i < basicCode.size(); i++) {
            String line = basicCode.get(i);
            if (line.contains("GOSUB") && line.contains("_PLACEHOLDER")) {
                String functionName = line.split("GOSUB ")[1].replace("_PLACEHOLDER", "");
                Integer functionLine = functionPositions.get(functionName);
                if (functionLine != null) {
                    String newLine = line.substring(0, line.indexOf("GOSUB")) + "GOSUB " + functionLine;
                    basicCode.set(i, newLine);
                }
            }
        }
    }

    private static String translateVariable(String var) {
        if (var == null || var.isEmpty()) {
            return "";
        }

        if (var.matches("t\\d+")) {
            return tempVarMap.getOrDefault(var, var);
        }

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

    private static void addLine(String code) {
        basicCode.add(lineNumber + " " + code);
        lineNumber += 10;
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

    private static void writeBasicCodeToFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("TargetCode.bas"))) {
            for (String line : basicCode) {
                writer.write(line);
                writer.newLine();
            }
            System.out.println("BASIC code has been written to TargetCode.bas");
        } catch (IOException e) {
            System.err.println("Error writing to TargetCode.bas: " + e.getMessage());
        }
    }
}
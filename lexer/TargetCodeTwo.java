import java.io.*;
import java.util.*;

public class TargetCodeTwo {
    private Map<String, String> symbolTable = new HashMap<>();
    private List<String> intermediateCode = new ArrayList<>();
    private List<String> basicCode = new ArrayList<>();
    private Map<String, Integer> labelMap = new HashMap<>();
    private Map<String, String> tempVarMap = new HashMap<>();
    private Map<String, String> valueTracker = new HashMap<>();
    private int lineNumber = 10;
    private int tempCounter = 4; // Start temp variable counter after the first few

    public static void main(String[] args) {
        TargetCodeTwo generator = new TargetCodeTwo();
        generator.loadSymbolTable("Symbol.txt");
        generator.loadIntermediateCode("intermediateCode.txt");
        generator.generateBasicCode();
        generator.writeBasicCode("BasicOutput.bas");
    }

    private void loadSymbolTable(String filename) {
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            int numVarCounter = 1;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(" : ");
                if (parts.length == 3) {
                    String originalName = parts[0].trim();
                    String generatedName = parts[1].trim();
                    String type = parts[2].trim();
                    if (type.equals("num")) {
                        symbolTable.put(generatedName, "V" + numVarCounter++ + "%");
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadIntermediateCode(String filename) {
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                intermediateCode.add(line.trim());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void generateBasicCode() {
        // First pass: identify labels and map to line numbers
        for (String line : intermediateCode) {
            if (line.matches("L\\d+:")) {
                String label = line.split(":")[0];
                labelMap.put(label, lineNumber);
                addLine("REM " + label);
            }
        }

        // Second pass: generate code
        for (String line : intermediateCode) {
            if (line.contains(":=")) {
                handleAssignment(line);
            } else if (line.startsWith("IF")) {
                handleIfStatement(line);
            } else if (line.startsWith("GOTO")) {
                String label = line.split(" ")[1];
                addLine("GOTO " + labelMap.get(label));
            } else if (line.startsWith("PRINT")) {
                String content = line.substring(6).trim();
                addLine("PRINT " + translateVariable(content));
            } else if (line.equals("STOP")) {
                addLine("END");
            }
        }
    }

    private void handleAssignment(String line) {
        String[] parts = line.split(":=");
        String left = parts[0].trim();
        String right = parts[1].trim();

        String translatedRight = translateExpression(right);
        addAssignmentIfNeeded(left, translatedRight);
    }

    private void handleIfStatement(String line) {
        String[] parts = line.split(" ");
        String condition = translateExpression(parts[1]);
        String trueLabel = parts[3];
        String falseLabel = parts[6];

        addLine("IF " + condition + " THEN GOTO " + labelMap.get(trueLabel));
        addLine("GOTO " + labelMap.get(falseLabel));
    }

    private void addAssignmentIfNeeded(String left, String right) {
        String translatedLeft = translateVariable(left);

        // Avoid redundant assignments
        if (!right.equals(valueTracker.get(translatedLeft))) {
            addLine("LET " + translatedLeft + " = " + right);
            valueTracker.put(translatedLeft, right);
        }
    }

    private String translateVariable(String var) {
        if (symbolTable.containsKey(var)) {
            return symbolTable.get(var);
        }
        if (tempVarMap.containsKey(var)) {
            return tempVarMap.get(var);
        }
        if (var.matches("\\d+")) {
            return var; // Return constants as-is
        }
        return var; // Default fallback
    }

    private String translateExpression(String expr) {
        if (expr.contains(">")) {
            String[] parts = expr.split(">");
            return translateVariable(parts[0].trim()) + " > " + translateVariable(parts[1].trim());
        } else if (expr.contains("=")) {
            String[] parts = expr.split("=");
            return translateVariable(parts[0].trim()) + " = " + translateVariable(parts[1].trim());
        }
        return translateVariable(expr);
    }

    private void addLine(String code) {
        basicCode.add(lineNumber + " " + code);
        lineNumber += 10;
    }

    private void writeBasicCode(String filename) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filename))) {
            for (String line : basicCode) {
                bw.write(line);
                bw.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

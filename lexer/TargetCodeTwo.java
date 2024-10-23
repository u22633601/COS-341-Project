import java.io.*;
import java.util.*;

public class TargetCodeTwo {
    private int currentLineNumber;
    private Map<String, String> symbolTable;
    private Map<String, Integer> labelToLineNumber;
    private List<String> basicCode;
    private Set<String> stringVariables;
    private Map<String, String> tempVarMap;
    private int tempVarCounter;

    public TargetCodeTwo() {
        currentLineNumber = 10;
        symbolTable = new HashMap<>();
        labelToLineNumber = new HashMap<>();
        basicCode = new ArrayList<>();
        stringVariables = new HashSet<>();
        tempVarMap = new HashMap<>();
        tempVarCounter = 1;
    }

    private void addLine(String code) {
        basicCode.add(currentLineNumber + " " + code);
        currentLineNumber += 10;
    }

    private void loadSymbolTable(String filename) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.trim().split("\\s*:\\s*");
                if (parts.length == 3) {
                    symbolTable.put(parts[1], parts[2]); // Map ID to type
                    if (parts[2].equals("text")) {
                        stringVariables.add(parts[1]);
                    }
                }
            }
        }
    }

    private String getBasicVariable(String intermediateVar) {
        // Check if we've already mapped this temporary variable
        if (tempVarMap.containsKey(intermediateVar)) {
            return tempVarMap.get(intermediateVar);
        }

        // Handle source variables (v103, v104, etc.)
        if (intermediateVar.startsWith("v")) {
            if (stringVariables.contains(intermediateVar)) {
                char letterVar = (char)('A' + (Integer.parseInt(intermediateVar.substring(1)) % 26));
                return letterVar + "$";
            } else {
                // Keep original number for source variables
                return "V" + intermediateVar.substring(1);
            }
        }
        // Handle temporary variables (t1, t2, etc.)
        else if (intermediateVar.startsWith("t")) {
            String newVar = "V" + tempVarCounter + "%";
            tempVarMap.put(intermediateVar, newVar);
            tempVarCounter++;
            return newVar;
        }
        return intermediateVar;
    }

    

    private Map<String, Integer> labelNextLineMap;


    private void processIntermediateCode(String filename) throws IOException {
        // Initialize
        addLine("DIM M(7,30)");
        addLine("SP = 0");
        addLine("DIM R$");

        // First pass: collect all labels and their line numbers
        int tempLine = 40;
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                
                if (line.matches("L\\d+:")) {
                    labelToLineNumber.put(line.substring(0, line.length()-1), tempLine + 10); // Point to line after REM
                }
                tempLine += 10;
            }
        }

        // Reset line number for second pass
        currentLineNumber = 40;

        // Second pass: generate code with corrected GOTO targets
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                processLine(line.trim());
            }
        }
    }

    private void processLine(String line) {
        if (line.isEmpty()) return;

        if (line.startsWith("REM")) {
            addLine("REM " + line.substring(4));
        }
        else if (line.matches("L\\d+:")) {
            addLine("REM " + line.substring(0, line.length()-1));
        }
        else if (line.startsWith("GOTO")) {
            String label = line.substring(5).trim();
            Integer targetLine = labelToLineNumber.get(label);
            if (targetLine != null) {
                addLine("GOTO " + targetLine);
            }
        }
        else if (line.startsWith("IF")) {
            processIfStatement(line);
        }
        else if (line.startsWith("PRINT")) {
            processPrintStatement(line);
        }
        else if (line.startsWith("INPUT")) {
            processInputStatement(line);
        }
        else if (line.contains(":=")) {
            processAssignment(line);
        }
        else if (line.equals("STOP")) {
            addLine("END");
        }
    }

    private void processIfStatement(String line) {
        // IF t3 GOTO L1 ELSE GOTO L2
        String[] parts = line.split("\\s+");
        String condition = parts[1];
        String trueLabel = parts[3];
        String falseLabel = parts[6];

        Integer trueLine = labelToLineNumber.get(trueLabel);
        Integer falseLine = labelToLineNumber.get(falseLabel);
        
        if (trueLine == null) {
            System.err.println("Warning: Label not found: " + trueLabel);
            trueLine = currentLineNumber + 10;
        }
        if (falseLine == null) {
            System.err.println("Warning: Label not found: " + falseLabel);
            falseLine = currentLineNumber + 20;
        }

        addLine("IF (" + getBasicVariable(condition) + ") THEN GOTO " + trueLine);
        addLine("GOTO " + falseLine);
    }

    private void processAssignment(String line) {
        String[] parts = line.split(":=");
        String target = parts[0].trim();
        String expression = parts[1].trim();

        if (!expression.contains(" ")) {
            // Simple assignment
            if (expression.matches("\\d+")) {
                addLine("LET " + getBasicVariable(target) + " = " + expression);
            } else {
                addLine("LET " + getBasicVariable(target) + " = " + getBasicVariable(expression));
            }
        } else {
            // Operation
            String[] exprParts = expression.split("\\s+");
            if (exprParts.length == 3) {
                String left = getBasicVariable(exprParts[0]);
                String operator = translateOperator(exprParts[1]);
                String right = getBasicVariable(exprParts[2]);
                addLine("LET " + getBasicVariable(target) + " = (" + left + " " + operator + " " + right + ")");
            }
        }
    }

 

    private void processPrintStatement(String line) {
        String content = line.substring(6).trim();
        addLine("PRINT " + content);
    }

    private void processInputStatement(String line) {
        String var = line.substring(6).trim();
        addLine("INPUT " + getBasicVariable(var));
    }

    

    private String translateOperator(String op) {
        switch (op) {
            case "AND": return "AND";
            case "OR": return "OR";
            case ">": return ">";
            case "=": return "=";
            case "+": return "+";
            case "-": return "-";
            case "*": return "*";
            case "/": return "/";
            default: return op;
        }
    }

    public void generateTargetCode(String symbolTableFile, String intermediateCodeFile, String outputFile) {
        try {
            loadSymbolTable(symbolTableFile);
            processIntermediateCode(intermediateCodeFile);
            
            try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile))) {
                for (String line : basicCode) {
                    writer.println(line);
                }
            }
        } catch (IOException e) {
            System.err.println("Error generating target code: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Usage: java TargetCodeTwo symbolTable.txt intermediateCode.txt output.bas");
            return;
        }

        TargetCodeTwo generator = new TargetCodeTwo();
        generator.generateTargetCode(args[0], args[1], args[2]);
        System.out.println("BASIC target code generation completed successfully.");
    }
}
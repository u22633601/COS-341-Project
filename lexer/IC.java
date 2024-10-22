import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class IC {
    private static Map<String, String> symbolTable;
    private static int labelCounter = 0;
    private static int tempCounter = 0;

    public static void main(String[] args) {
        // Read the symbol table from file
        symbolTable = readSymbolTable("Symbol.txt");

        // Read the input program from file
        String inputProgram = readInputProgram("input.txt");

        // Generate the intermediate code
        String intermediateCode = generateIntermediateCode(inputProgram);

        // Write the intermediate code to file
        writeIntermediateCode(intermediateCode, "ic.txt");
    }

    private static Map<String, String> readSymbolTable(String filename) {
        Map<String, String> symbolTable = new HashMap<>();
        try {
            String content = new String(Files.readAllBytes(Paths.get(filename)));
            String[] lines = content.split("\\r?\\n");
            for (String line : lines) {
                String[] parts = line.split("\\s+");
                if (parts.length >= 3) {
                    String name = parts[0];
                    String newName = parts[2];
                    symbolTable.put(name, newName);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return symbolTable;
    }

    private static String readInputProgram(String filename) {
        try {
            return new String(Files.readAllBytes(Paths.get(filename)));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    private static void writeIntermediateCode(String intermediateCode, String filename) {
        try {
            Files.write(Paths.get(filename), intermediateCode.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String generateIntermediateCode(String inputProgram) {
        StringBuilder code = new StringBuilder();
        String[] tokens = inputProgram.split("\\s+");
        int index = 0;

        // Translation for PROG
        code.append(translateALGO(tokens, index));
        code.append("STOP\n");
        code.append("REM END\n");
        code.append(translateFUNCTIONS(tokens, index));

        return code.toString();
    }

    private static String translateALGO(String[] tokens, int index) {
        StringBuilder code = new StringBuilder();

        // Skip "begin"
        index++;

        // Translation for INSTRUC
        code.append(translateINSTRUC(tokens, index));

        // Skip "end"
        index++;

        return code.toString();
    }

    private static String translateINSTRUC(String[] tokens, int index) {
        StringBuilder code = new StringBuilder();
    
        while (index < tokens.length && !tokens[index].equals("end")) {
            // Translation for COMMAND
            String command = translateCOMMAND(tokens, index);
            code.append(command);
    
            // Append a newline character if the command is not "REM DO NOTHING"
            if (!command.equals("REM DO NOTHING")) {
                code.append("\n");
            }
    
            // Skip ";"
            index++;
        }
    
        return code.toString();
    }

    private static String translateCOMMAND(String[] tokens, int index) {
        String command = tokens[index];

        switch (command) {
            case "skip":
                return "REM DO NOTHING";
            case "halt":
                return "STOP";
            case "print":
                return "PRINT " + translateATOMIC(tokens, ++index);
            case "return":
                return "RETURN " + translateATOMIC(tokens, ++index);
            default:
                if (isAssignment(tokens, index)) {
                    return translateASSIGN(tokens, index);
                } else if (isCall(tokens, index)) {
                    return translateCALL(tokens, index);
                } else if (isBranch(tokens, index)) {
                    return translateBRANCH(tokens, index);
                }
        }

        return "";
    }

    private static boolean isAssignment(String[] tokens, int index) {
        return tokens[index + 1].equals("<") || tokens[index + 1].equals("=");
    }

    private static boolean isCall(String[] tokens, int index) {
        return tokens[index].startsWith("F_");
    }

    private static boolean isBranch(String[] tokens, int index) {
        return tokens[index].equals("if");
    }

    private static String translateATOMIC(String[] tokens, int index) {
        String atomic = tokens[index];

        if (atomic.startsWith("V_")) {
            return symbolTable.get(atomic);
        } else if (atomic.startsWith("\"")) {
            return atomic;
        } else {
            return atomic;
        }
    }

    private static String translateASSIGN(String[] tokens, int index) {
        String variable = symbolTable.get(tokens[index]);

        if (tokens[index + 1].equals("<")) {
            return "INPUT " + variable;
        } else {
            String term = translateTERM(tokens, index + 2);
            return variable + " := " + term;
        }
    }

    private static String translateTERM(String[] tokens, int index) {
        String term = tokens[index];

        if (term.startsWith("V_") || term.startsWith("\"") || isNumeric(term)) {
            return translateATOMIC(tokens, index);
        } else if (term.startsWith("F_")) {
            return translateCALL(tokens, index);
        } else {
            return translateOP(tokens, index);
        }
    }

    private static boolean isNumeric(String str) {
        return str.matches("-?\\d+(\\.\\d+)?");
    }

    private static String translateCALL(String[] tokens, int index) {
        String function = symbolTable.get(tokens[index]);
        String arg1 = translateATOMIC(tokens, index + 2);
        String arg2 = translateATOMIC(tokens, index + 4);
        String arg3 = translateATOMIC(tokens, index + 6);

        return "CALL_" + function + "(" + arg1 + "," + arg2 + "," + arg3 + ")";
    }

    private static String translateOP(String[] tokens, int index) {
        String operator = tokens[index];

        if (operator.equals("not")) {
            return translateUNOP(tokens, index);
        } else {
            return translateBINOP(tokens, index);
        }
    }

    private static String translateUNOP(String[] tokens, int index) {
        String operator = tokens[index];
        String arg = translateARG(tokens, index + 2);

        switch (operator) {
            case "not":
                return "!" + arg;
            case "sqrt":
                return "SQR(" + arg + ")";
            default:
                return "";
        }
    }

    private static String translateBINOP(String[] tokens, int index) {
        String operator = tokens[index];
        String arg1 = translateARG(tokens, index + 2);
        String arg2 = translateARG(tokens, index + 4);

        switch (operator) {
            case "or":
                return arg1 + " || " + arg2;
            case "and":
                return arg1 + " && " + arg2;
            case "eq":
                return arg1 + " = " + arg2;
            case "grt":
                return arg1 + " > " + arg2;
            case "add":
                return arg1 + " + " + arg2;
            case "sub":
                return arg1 + " - " + arg2;
            case "mul":
                return arg1 + " * " + arg2;
            case "div":
                return arg1 + " / " + arg2;
            default:
                return "";
        }
    }

    private static String translateARG(String[] tokens, int index) {
        String arg = tokens[index];

        if (arg.startsWith("V_") || arg.startsWith("\"") || isNumeric(arg)) {
            return translateATOMIC(tokens, index);
        } else {
            return translateOP(tokens, index);
        }
    }

    private static String translateBRANCH(String[] tokens, int index) {
        String condition = translateCOND(tokens, index + 1);
        String thenCode = translateALGO(tokens, index + 4);
        String elseCode = translateALGO(tokens, index + 7);
    
        String labelElse = generateLabel();
        String labelEnd = generateLabel();
    
        StringBuilder code = new StringBuilder();
        code.append("IF ").append(condition).append(" GOTO ").append(labelElse).append("\n");
        code.append(thenCode.trim().replace("\n", "\n    "));
        code.append("\nGOTO ").append(labelEnd).append("\n");
        code.append(labelElse).append(":\n");
        code.append(elseCode.trim().replace("\n", "\n    "));
        code.append("\n");
        code.append(labelEnd).append(":\n");
    
        return code.toString();
    }

    private static String translateCOND(String[] tokens, int index) {
        String condition = tokens[index];

        if (condition.equals("and")) {
            String simple1 = translateSIMPLE(tokens, index + 2);
            String simple2 = translateSIMPLE(tokens, index + 6);
            return simple1 + " && " + simple2;
        } else if (condition.equals("or")) {
            String simple1 = translateSIMPLE(tokens, index + 2);
            String simple2 = translateSIMPLE(tokens, index + 6);
            return simple1 + " || " + simple2;
        } else {
            return translateSIMPLE(tokens, index);
        }
    }

    private static String translateSIMPLE(String[] tokens, int index) {
        String operator = tokens[index];
        String arg1 = translateATOMIC(tokens, index + 2);
        String arg2 = translateATOMIC(tokens, index + 4);

        switch (operator) {
            case "eq":
                return arg1 + " = " + arg2;
            case "grt":
                return arg1 + " > " + arg2;
            default:
                return "";
        }
    }

    private static String translateFUNCTIONS(String[] tokens, int index) {
        StringBuilder code = new StringBuilder();

        while (index < tokens.length) {
            if (tokens[index].equals("num") || tokens[index].equals("void")) {
                code.append(translateDECL(tokens, index));
                code.append("STOP\n");
            } else {
                break;
            }
        }

        return code.toString();
    }

    private static String translateDECL(String[] tokens, int index) {
        // Skip function header
        index += 5;

        return translateBODY(tokens, index);
    }

    private static String translateBODY(String[] tokens, int index) {
        StringBuilder code = new StringBuilder();

        // Translation for PROLOG
        code.append(translatePROLOG(tokens, index));

        // Skip local variable declarations
        index += 7;

        // Translation for ALGO
        code.append(translateALGO(tokens, index));

        // Translation for EPILOG
        code.append(translateEPILOG(tokens, index));

        // Translation for SUBFUNCS
        code.append(translateFUNCTIONS(tokens, index));

        return code.toString();
    }

    private static String translatePROLOG(String[] tokens, int index) {
        // For now, assuming INLINING method
        return "REM BEGIN\n";
    }

    private static String translateEPILOG(String[] tokens, int index) {
        // For now, assuming INLINING method
        return "REM END\n";
    }

    private static String generateLabel() {
        return "L" + (labelCounter++);
    }

    private static String generateTemp() {
        return "t" + (tempCounter++);
    }
}
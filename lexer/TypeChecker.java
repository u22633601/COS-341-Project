import java.io.*;
import java.util.*;
import java.util.regex.*;

public class TypeChecker {
    private static Map<String, String> symbolTable = new HashMap<>();
    private static List<String> errors = new ArrayList<>();
    private static int currentLine = 0;

    public static void main(String[] args) {
        loadSymbolTable("Symbol.txt");
        checkProgram("input.txt");
        printErrors();
    }

    private static void loadSymbolTable(String filename) {
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length == 3) {
                    symbolTable.put(parts[0].trim(), parts[2].trim());
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading symbol table: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void checkProgram(String filename) {
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                currentLine++;
                checkLine(line.trim());
            }
        } catch (IOException e) {
            System.err.println("Error reading program file: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void checkLine(String line) {
        if (line.startsWith("main") || line.startsWith("end") || line.startsWith("begin") || line.equals("skip ;")) {
            return; // These lines don't require type checking
        }

        if (line.contains("<")) {
            checkInput(line);
        } else if (line.contains("=")) {
            checkAssignment(line);
        } else if (line.startsWith("if")) {
            checkCondition(line);
        } else if (line.startsWith("return")) {
            checkReturn(line);
        } else if (line.contains("(")) {
            checkFunctionCall(line);
        } else if (line.startsWith("num") || line.startsWith("text") || line.startsWith("void")) {
            checkDeclaration(line);
        }
    }

    private static void checkInput(String line) {
        String[] parts = line.split("<");
        String var = parts[0].trim();
        checkVariableType(var);
    }

    private static void checkAssignment(String line) {
        String[] parts = line.split("=");
        String left = parts[0].trim();
        String right = parts[1].trim().replace(";", "");

        String leftType = getVariableType(left);
        String rightType = inferType(right);

        if (!leftType.equals(rightType)) {
            addError("Type mismatch in assignment: " + left + " (" + leftType + ") = " + right + " (" + rightType + ")");
        }
    }

    private static void checkCondition(String line) {
        String condition = line.substring(line.indexOf("(") + 1, line.lastIndexOf(")"));
        checkExpression(condition);
    }

    private static void checkReturn(String line) {
        String value = line.substring(line.indexOf(" ") + 1, line.indexOf(";")).trim();
        if (value.startsWith("\"") && value.endsWith("\"")) {
            // It's a string literal, which is fine
        } else {
            checkExpression(value);
        }
    }

    private static void checkFunctionCall(String line) {
        String funcName = line.substring(0, line.indexOf("(")).trim();
        String params = line.substring(line.indexOf("(") + 1, line.indexOf(")"));
        String[] paramList = params.split(",");

        if (!funcName.startsWith("F_")) {
            addError("Invalid function name: " + funcName);
            return;
        }

        String funcType = symbolTable.getOrDefault(funcName, "unknown");
        if (funcType.equals("unknown")) {
            addError("Undefined function: " + funcName);
            return;
        }

        if (paramList.length != 3) {
            addError("Function " + funcName + " must have exactly 3 parameters");
            return;
        }

        for (String param : paramList) {
            String paramType = inferType(param.trim());
            if (!paramType.equals("num")) {
                addError("Parameter " + param.trim() + " must be of type num for function " + funcName);
            }
        }
    }

    private static void checkDeclaration(String line) {
        String[] parts = line.split(" ");
        String type = parts[0];
        String name = parts[1].replace(",", "").trim();

        if (!name.startsWith("V_") && !name.startsWith("F_")) {
            addError("Invalid variable or function name: " + name);
        }

        if (symbolTable.containsKey(name)) {
            String declaredType = symbolTable.get(name);
            if (!declaredType.equals(type)) {
                addError("Type mismatch in declaration: " + name + " declared as " + type + ", but symbol table says " + declaredType);
            }
        }
    }

    private static void checkExpression(String expression) {
        if (expression.contains("(")) {
            String op = expression.substring(0, expression.indexOf("(")).trim();
            String args = expression.substring(expression.indexOf("(") + 1, expression.lastIndexOf(")"));
            String[] argList = args.split(",");

            switch (op) {
                case "not":
                case "sqrt":
                    if (argList.length != 1) {
                        addError("Operator " + op + " requires exactly 1 argument");
                    } else {
                        checkNumericArg(argList[0].trim());
                    }
                    break;
                case "or":
                case "and":
                case "eq":
                case "grt":
                case "add":
                case "sub":
                case "mul":
                case "div":
                    if (argList.length != 2) {
                        addError("Operator " + op + " requires exactly 2 arguments");
                    } else {
                        checkNumericArg(argList[0].trim());
                        checkNumericArg(argList[1].trim());
                    }
                    break;
                default:
                    addError("Unknown operator: " + op);
            }
        } else {
            // It's a simple variable or constant
            inferType(expression);
        }
    }

    private static void checkNumericArg(String arg) {
        String type = inferType(arg);
        if (!type.equals("num")) {
            addError("Argument " + arg + " must be of type num");
        }
    }

    private static String inferType(String expression) {
        if (expression.startsWith("\"") && expression.endsWith("\"")) {
            return "text";
        } else if (isNumeric(expression)) {
            return "num";
        } else if (expression.startsWith("V_") || expression.startsWith("F_")) {
            return getVariableType(expression);
        } else if (expression.contains("(")) {
            // It's a function call or operation, which should return a num
            return "num";
        } else {
            addError("Cannot infer type of: " + expression);
            return "unknown";
        }
    }

    private static String getVariableType(String var) {
        String type = symbolTable.getOrDefault(var, "unknown");
        if (type.equals("unknown")) {
            addError("Undefined variable: " + var);
        }
        return type;
    }

    private static void checkVariableType(String var) {
        if (!symbolTable.containsKey(var)) {
            addError("Undefined variable: " + var);
        }
    }

    private static boolean isNumeric(String str) {
        return str.matches("-?\\d+(\\.\\d+)?");
    }

    private static void addError(String message) {
        errors.add("Line " + currentLine + ": Type Error: " + message);
    }

    private static void printErrors() {
        if (errors.isEmpty()) {
            System.out.println("No type errors found.");
        } else {
            for (String error : errors) {
                System.out.println(error);
            }
        }
    }
}
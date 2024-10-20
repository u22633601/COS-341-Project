import java.io.*;
import java.util.*;
import java.util.ArrayList;

public class TypeChecker {
    private static Map<String, String> symbolTable = new HashMap<>();
    private static List<String> errors = new ArrayList<>();
    private static int currentLine = 0;
    private static boolean debug = true; // Set to true for detailed output

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
        if (debug) {
            System.out.println("Symbol Table: " + symbolTable);
        }
    }

    private static void checkProgram(String filename) {
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                currentLine++;
                if (debug) {
                    System.out.println("Checking line " + currentLine + ": " + line);
                }
                checkLine(line.trim());
            }
        } catch (IOException e) {
            System.err.println("Error reading program file: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void checkLine(String line) {
        line = line.trim();
        if (line.isEmpty() || line.startsWith("main") || line.equals("end") || line.startsWith("begin")) {
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
        } else if (line.matches("^(num|text|void)\\s+F_.*\\(.*\\).*")) {
            checkFunctionDeclaration(line);
        } else if (line.startsWith("num") || line.startsWith("text") || line.startsWith("void")) {
            checkDeclaration(line);
        } else if (line.contains("(")) {
            checkFunctionCall(line);
        }
    }

    private static void checkFunctionDeclaration(String line) {
        line = line.replace("{", "").trim(); // Remove opening brace if present
        String[] parts = line.split("\\(", 2);
        if (parts.length != 2) {
            addError("Invalid function declaration");
            return;
        }
        
        String[] returnTypeAndName = parts[0].trim().split("\\s+");
        if (returnTypeAndName.length != 2) {
            addError("Invalid function declaration format");
            return;
        }
        
        String returnType = returnTypeAndName[0];
        String funcName = returnTypeAndName[1];
        
        if (!funcName.startsWith("F_")) {
            addError("Function name must start with F_: " + funcName);
        }
        
        String params = parts[1].substring(0, parts[1].lastIndexOf(")")).trim();
        String[] paramList = params.split(",");
        if (paramList.length != 3) {
            addError("Function " + funcName + " must have exactly 3 parameters");
        }
        
        for (String param : paramList) {
            String paramName = param.trim();
            if (!paramName.startsWith("V_")) {
                addError("Function parameter must start with V_: " + paramName);
            }
            String paramType = symbolTable.get(paramName);
            if (paramType == null || !paramType.equals("num")) {
                addError("Function parameter must be of type num: " + paramName);
            }
        }
        
        String declaredType = symbolTable.get(funcName);
        if (declaredType == null) {
            addError("Function " + funcName + " is not declared in the symbol table");
        } else if (!declaredType.equals(returnType)) {
            addError("Return type mismatch for function " + funcName + ": declared as " + declaredType + ", but defined as " + returnType);
        }
    }

    private static void checkInput(String line) {
        String[] parts = line.split("<");
        String var = parts[0].trim();
        checkVariableType(var);
    }

    private static void checkCondition(String line) {
        int startIndex = line.indexOf("(");
        int endIndex = line.lastIndexOf(")");
        if (startIndex != -1 && endIndex != -1 && startIndex < endIndex) {
            String condition = line.substring(startIndex + 1, endIndex).trim();
            String conditionType = checkExpression(condition);
            if (!conditionType.equals("bool")) {
                addError("Condition must be a boolean-like expression");
            }
        } else {
            addError("Invalid if statement structure");
        }
    }

    private static void checkAssignment(String line) {
        String[] parts = line.split("=");
        String left = parts[0].trim();
        String right = parts[1].trim().replace(";", "");

        String leftType = getVariableType(left);
        String rightType = checkExpression(right);

        if (debug) {
            System.out.println("Checking assignment: " + left + " (" + leftType + ") = " + right + " (" + rightType + ")");
        }

        if (!leftType.equals(rightType)) {
            addError("Type mismatch in assignment: " + left + " (" + leftType + ") = " + right + " (" + rightType + ")");
        }
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
        String params = line.substring(line.indexOf("(") + 1, line.lastIndexOf(")"));
        String[] paramList = params.split(",");

        if (debug) {
            System.out.println("Checking function call: " + funcName + " with params: " + Arrays.toString(paramList));
        }

        if (isBuiltInFunction(funcName)) {
            checkBuiltInFunction(funcName, paramList);
        } else if (funcName.startsWith("F_")) {
            checkUserDefinedFunction(funcName, paramList);
        } else {
            addError("Unknown function: " + funcName);
        }
    }

    private static void checkDeclaration(String line) {
        String[] parts = line.split(",");
        
        for (String part : parts) {
            String[] subParts = part.trim().split("\\s+");
            if (subParts.length < 2) continue;
            
            String type = subParts[0];
            String name = subParts[subParts.length - 1].trim();
            
            if (!name.startsWith("V_")) {
                addError("Invalid variable name: " + name);
            } else if (symbolTable.containsKey(name)) {
                String declaredType = symbolTable.get(name);
                if (!declaredType.equals(type)) {
                    addError("Type mismatch in declaration: " + name + " declared as " + type + ", but symbol table says " + declaredType);
                }
            } else {
                // Variable is not in the symbol table, which is fine for local variables
                if (debug) {
                    System.out.println("Declared local variable: " + name + " of type " + type);
                }
            }
        }
    }

    private static String checkExpression(String expression) {
        expression = expression.trim();
        if (expression.isEmpty()) {
            addError("Empty expression");
            return "unknown";
        }
        
        if (expression.contains("(")) {
            int openParenIndex = expression.indexOf("(");
            String funcName = expression.substring(0, openParenIndex).trim();
            String argsString = getArguments(expression.substring(openParenIndex));
            
            String[] args = splitArguments(argsString);
            
            if (isBuiltInFunction(funcName)) {
                checkBuiltInFunction(funcName, args);
                if (funcName.equals("eq") || funcName.equals("grt") || funcName.equals("and") || funcName.equals("or") || funcName.equals("not")) {
                    return "bool";
                } else {
                    return "num";
                }
            } else if (funcName.startsWith("F_")) {
                return checkUserDefinedFunction(funcName, args);
            } else {
                addError("Unknown function: " + funcName);
                return "unknown";
            }
        } else {
            return inferType(expression);
        }
    }
    
    private static String getArguments(String expression) {
        int parenthesesCount = 0;
        int start = expression.indexOf("(");
        for (int i = start; i < expression.length(); i++) {
            if (expression.charAt(i) == '(') {
                parenthesesCount++;
            } else if (expression.charAt(i) == ')') {
                parenthesesCount--;
                if (parenthesesCount == 0) {
                    return expression.substring(start + 1, i);
                }
            }
        }
        addError("Mismatched parentheses in expression: " + expression);
        return "";
    }
    
    private static String[] splitArguments(String argsString) {
        List<String> args = new ArrayList<>();
        int parenthesesCount = 0;
        StringBuilder currentArg = new StringBuilder();
        
        for (char c : argsString.toCharArray()) {
            if (c == '(') {
                parenthesesCount++;
            } else if (c == ')') {
                parenthesesCount--;
            }
            
            if (c == ',' && parenthesesCount == 0) {
                args.add(currentArg.toString().trim());
                currentArg = new StringBuilder();
            } else {
                currentArg.append(c);
            }
        }
        if (currentArg.length() > 0) {
            args.add(currentArg.toString().trim());
        }
        
        return args.toArray(new String[0]);
    }

    private static String inferType(String expression) {
        expression = expression.trim();
        if (expression.startsWith("\"") && expression.endsWith("\"")) {
            return "text";
        } else if (isNumeric(expression)) {
            return "num";
        } else if (expression.startsWith("V_") || expression.startsWith("F_")) {
            return getVariableType(expression);
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

    private static String checkUserDefinedFunction(String funcName, String[] paramList) {
        if (!symbolTable.containsKey(funcName)) {
            addError("Undefined function: " + funcName);
            return "unknown";
        }

        String funcType = symbolTable.get(funcName);

        if (paramList.length != 3) {
            addError("Function " + funcName + " must have exactly 3 parameters");
            return funcType;
        }

        for (String param : paramList) {
            String paramType = inferType(param.trim());
            if (!paramType.equals("num")) {
                addError("Parameter " + param.trim() + " must be of type num for function " + funcName);
            }
        }

        return funcType;
    }

    private static void checkUnaryOperation(String op, String[] args) {
        if (args.length != 1) {
            addError("Operator " + op + " requires exactly 1 argument");
            return;
        }

        String argType = inferType(args[0].trim());
        if (!argType.equals("num")) {
            addError("Argument " + args[0].trim() + " must be of type num for operator " + op);
        }
    }

    private static void checkBinaryOperation(String op, String[] args) {
        if (args.length != 2) {
            addError("Operator " + op + " requires exactly 2 arguments");
            return;
        }
    
        for (String arg : args) {
            String argType;
            if (arg.contains("(")) {
                // This is a nested function call, which has already been checked
                argType = "num";
            } else {
                argType = inferType(arg.trim());
            }
            if (!argType.equals("num")) {
                addError("Argument " + arg.trim() + " must be of type num for operator " + op);
            }
        }
    }

    private static void checkBuiltInFunction(String funcName, String[] args) {
        switch (funcName) {
            case "add":
            case "sub":
            case "mul":
            case "div":
            case "grt":
                if (args.length != 2) {
                    addError("Operator " + funcName + " requires exactly 2 arguments");
                } else {
                    for (String arg : args) {
                        String argType = checkExpression(arg.trim());
                        if (!argType.equals("num")) {
                            addError("Argument " + arg.trim() + " must be of type num for operator " + funcName);
                        }
                    }
                }
                break;
            case "and":
            case "or":
                if (args.length != 2) {
                    addError("Operator " + funcName + " requires exactly 2 arguments");
                } else {
                    for (String arg : args) {
                        String argType = checkExpression(arg.trim());
                        if (!argType.equals("bool")) {
                            addError("Argument " + arg.trim() + " must be a boolean-like expression for operator " + funcName);
                        }
                    }
                }
                break;
            case "eq":
                if (args.length != 2) {
                    addError("Operator " + funcName + " requires exactly 2 arguments");
                } else {
                    String type1 = checkExpression(args[0].trim());
                    String type2 = checkExpression(args[1].trim());
                    if (!type1.equals(type2)) {
                        addError("Arguments of eq must be of the same type");
                    }
                }
                break;
            case "not":
                if (args.length != 1) {
                    addError("Operator " + funcName + " requires exactly 1 argument");
                } else {
                    String argType = checkExpression(args[0].trim());
                    if (!argType.equals("bool")) {
                        addError("Argument " + args[0].trim() + " must be a boolean-like expression for operator " + funcName);
                    }
                }
                break;
            case "sqrt":
                if (args.length != 1) {
                    addError("Operator " + funcName + " requires exactly 1 argument");
                } else {
                    String argType = checkExpression(args[0].trim());
                    if (!argType.equals("num")) {
                        addError("Argument " + args[0].trim() + " must be of type num for operator " + funcName);
                    }
                }
                break;
            default:
                addError("Unknown built-in function: " + funcName);
        }
    }

    private static boolean isBooleanLikeExpression(String expression) {
        if (expression.contains("(")) {
            String funcName = expression.substring(0, expression.indexOf("(")).trim();
            return funcName.equals("eq") || funcName.equals("grt") || funcName.equals("and") || funcName.equals("or") || funcName.equals("not");
        }
        return false;
    }

    private static boolean isBuiltInFunction(String funcName) {
        return Arrays.asList("add", "sub", "mul", "div", "and", "or", "not", "eq", "grt", "sqrt").contains(funcName);
    }
}
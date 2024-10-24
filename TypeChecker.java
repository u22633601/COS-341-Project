import java.io.*;
import java.util.*;

public class TypeChecker {
    private static Map<String, String> symbolTable = new HashMap<>();
    private static List<String> errors = new ArrayList<>();
    private static int currentLine = 0;
    private static boolean debug = true; //set false for less detailed output
    private static String currentFunction = null;
    private static String currentFunctionType = null;
    private static boolean hasReturnStatement = false;
    private static Set<String> currentFunctionCalls = new HashSet<>();
    private static final String SYMBOL_FILE = "Symbol.txt";

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java TypeChecker <input-file>");
            System.exit(1);
        }

        String inputFile = args[0];

        //check input file
        if (!new File(inputFile).exists()) {
            System.err.println("Error: Input file '" + inputFile + "' not found");
            System.exit(1);
        }

        // load st and type checker
        if (loadSymbolTable(SYMBOL_FILE)) {
            checkProgram(inputFile);
            printErrors();
        } else {
            System.exit(1);
        }
    }

    private static boolean loadSymbolTable(String filename) { //load the symbol table
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            int lineCount = 0;
            while ((line = br.readLine()) != null) {
                lineCount++;
                String[] parts = line.split(":");
                if (parts.length == 3) {
                    symbolTable.put(parts[0].trim(), parts[2].trim());
                } else {
                    System.err.println("Error in " + filename + " line " + lineCount + ": Invalid format");
                    System.err.println("Expected format: identifier:scope:type");
                    return false;
                }
            }
            if (symbolTable.isEmpty()) {
                System.err.println("Error: Symbol table is empty");
                return false;
            }
            return true;
        } catch (FileNotFoundException e) {
            System.err.println("Error: Could not find " + filename);
            return false;
        } catch (IOException e) {
            System.err.println("Error reading " + filename + ": " + e.getMessage());
            return false;
        }
    }

    private static void checkReturn(String line) {
        if (currentFunction == null || currentFunctionType == null) {
            return; 
        }

        String value;
        if (line.startsWith("return")) {
            value = line.substring(6).trim();
        // } else if (line.startsWith("retrun")) { //
        //     value = line.substring(6).trim();
        } else {
            return;
        }
    
        if (value.endsWith(";")) {
            value = value.substring(0, value.length() - 1).trim();
        }
    
        if (currentFunctionType.equals("void")) {
            if (!value.isEmpty()) {
                addError("Void function cannot return a value"); //no return value for void functions
            }
            return;
        }
    
        
        if (currentFunctionType.equals("num")) {
            hasReturnStatement = true;  //function must have numeric return
            if (value.startsWith("\"")) {
                addError("Function " + currentFunction + " has return type num but is returning text");
                return;
            }
            String returnType = checkExpression(value); //num
            if (!returnType.equals("num")) {
                addError("Function " + currentFunction + " has return type num but is returning " + returnType);
            }
        }
    }
    
   
    private static void checkFunctionBody(String line) {
        if (line.equals("{")) {
            hasReturnStatement = false; 
        } else if (line.equals("}")) {
            
            if (currentFunctionType != null && currentFunctionType.equals("num") && !hasReturnStatement) {
                addError("Function " + currentFunction + " must have a return statement"); //num function must have a return 
            }
            currentFunction = null;
            currentFunctionType = null;
            hasReturnStatement = false;
        } else if (line.startsWith("return") ) { //check num fuction has a return statement
            hasReturnStatement = true;
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
        
        checkFunctionBody(line);
        
        if (line.isEmpty() || line.startsWith("main") || line.equals("end") || line.startsWith("begin")) {
            return;
        }
    
        if (line.matches("^(num|text|void)\\s+F_.*\\(.*\\).*")) {
            String[] parts = line.split("\\s+|\\(");
            if (parts.length >= 2) {
                currentFunction = parts[1];
                currentFunctionType = parts[0];
            }
            checkFunctionDeclaration(line);
        } else if (line.contains("<")) { //input
            checkInput(line);
        } else if (line.contains("=")) { //asignment
            checkAssignment(line);
        } else if (line.startsWith("if")) { // if else 
            checkCondition(line);
        } else if ((line.startsWith("return")) || (line.startsWith("print"))) {
            checkReturn(line);
        } else if (line.startsWith("num") || line.startsWith("text") || line.startsWith("void")) { //decl
            checkDeclaration(line);
        } else if (line.contains("(")) {
            String funcName = line.substring(0, line.indexOf("(")).trim();
            if (funcName.startsWith("F_")) { //user def function
                String funcType = symbolTable.get(funcName);
                if (funcType != null && funcType.equals("num")) {
                    addError("Function " + funcName + " has return type num and must be used in an assignment");
                }
            }
            checkFunctionCall(line);
        }
    }

    private static void checkFunctionDeclaration(String line) {
        line = line.replace("{", "").trim(); 
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
                addError("Function parameter must be of type num: " + paramName);//all user defined function parameters are num
            }
        }
        
        String declaredType = symbolTable.get(funcName);
        if (declaredType == null) {
            addError("Function " + funcName + " is not declared in the symbol table");
        } else if (!declaredType.equals(returnType)) {
            addError("return type mismatch for function " + funcName + ": declared as " + declaredType + ", but defined as " + returnType);
        }
    }

    private static void checkInput(String line) {
        String[] parts = line.split("<");
        String var = parts[0].trim();
        checkVariableType(var);
    }

    private static void checkCondition(String line) {
        
        String conditionPart = line.substring(2).trim();
        
        int thenIndex = conditionPart.lastIndexOf("then");
        if (thenIndex == -1) {
            addError("Invalid if statement: missing 'then' keyword");
            return;
        }
        
        String condition = conditionPart.substring(0, thenIndex).trim();
        
        if (debug) {
            System.out.println("Checking condition: " + condition);
        }
        
        String conditionType = checkExpression(condition);
        if (!conditionType.equals("bool")) {
            addError("condition must be a boolean expression (grt, eq, and, or)");
        }
    }

    private static void checkAssignment(String line) {
        String[] parts = line.split("=");
        String left = parts[0].trim();
        String right = parts[1].trim().replace(";", "");

        String leftType = getVariableType(left);
        String rightType = checkExpression(right);

        if (debug) {
            System.out.println("checking assignment: " + left + " (" + leftType + ") = " + right + " (" + rightType + ")");
        }

        if (!leftType.equals(rightType)) {
            addError("type mismatch in assignment: " + left + " (" + leftType + ") = " + right + " (" + rightType + ")");
        }
    }

    // private static void checkReturn(String line) {
    //     String value = line.substring(line.indexOf(" ") + 1, line.indexOf(";")).trim();
    //     if (value.startsWith("\"") && value.endsWith("\"")) {
    //         // It's a string literal, which is fine
    //     } else {
    //         checkExpression(value);
    //     }
    // }

    

    private static void checkFunctionCall(String line) {
        String funcName = line.substring(0, line.indexOf("(")).trim();
        String params = line.substring(line.indexOf("(") + 1, line.lastIndexOf(")"));
        String[] paramList = params.split(",");

        if (debug) {
            System.out.println("checking function call: " + funcName + " with params: " + Arrays.toString(paramList));
        }

        if (isBuiltInFunction(funcName)) {
            checkBuiltInFunction(funcName, paramList);
        } else if (funcName.startsWith("F_")) {
            checkUserDefinedFunction(funcName, paramList);
        } else {
            addError("unknown function: " + funcName);
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
                    addError("type mismatch in declaration: " + name + " declared as " + type + ", but symbol table says " + declaredType);
                }
            } else {
                if (debug) {
                    System.out.println("declared local variable: " + name + " of type " + type);
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
            String funcName = expression.substring(0, expression.indexOf("(")).trim();
            String argsString = getArguments(expression);
            String[] args = splitArguments(argsString);
            
            if (isBuiltInFunction(funcName)) {
                checkBuiltInFunction(funcName, args);
                if (funcName.equals("grt") || funcName.equals("eq") || 
                    funcName.equals("and") || funcName.equals("or") || funcName.equals("not")) {
                    return "bool";
                }
                return "num";
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

    private static boolean isBooleanOperation(String expression) {
        expression = expression.trim();
        if (!expression.contains("(")) {
            return false;
        }
        
        String outerFunc = expression.substring(0, expression.indexOf("(")).trim();
        return outerFunc.equals("grt") || outerFunc.equals("eq") || 
               outerFunc.equals("and") || outerFunc.equals("or");
    }
    
    private static String getArguments(String expression) {
        int parenthesesCount = 1;
        int startIndex = expression.indexOf("(") + 1;
        
        for (int i = startIndex; i < expression.length(); i++) {
            if (expression.charAt(i) == '(') parenthesesCount++;
            if (expression.charAt(i) == ')') parenthesesCount--;
            if (parenthesesCount == 0) {
                return expression.substring(startIndex, i);
            }
        }
        
        addError("Mismatched parentheses in expression: " + expression);
        return "";
    }
    
    private static String[] splitArguments(String argsString) {
        if (!argsString.contains(",")) {
            return new String[]{argsString.trim()};
        }
        
        List<String> args = new ArrayList<>();
        int parenthesesCount = 0;
        StringBuilder currentArg = new StringBuilder();
        
        for (char c : argsString.toCharArray()) {
            if (c == '(') {
                parenthesesCount++;
            }
            if (c == ')') {
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
            addError("Function " + funcName + " must have exactly 3 parameters"); //user def functions have 3 parameters
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
            addError("Operator " + op + " needs exactly 1 argument");
            return;
        }

        String argType = inferType(args[0].trim());
        if (!argType.equals("num")) {
            addError("Argument " + args[0].trim() + " must be of type num for operator " + op);
        }
    }

    private static void checkBinaryOperation(String op, String[] args) {
        if (args.length != 2) {
            addError("Operator " + op + " needs exactly 2 arguments");
            return;
        }
    
        for (String arg : args) {
            String argType;
            if (arg.contains("(")) {
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
            case "grt":
                if (args.length != 2) {
                    addError("operator GRT needs exactly 2 arguments");
                    return;
                }
                for (String arg : args) {
                    String type = checkExpression(arg.trim());
                    if (!type.equals("num")) {
                        addError("Arguments of GRT must be numeric, found type: " + type);
                    }
                }
                break;
                    
            case "add":
            case "sub":
            case "mul":
            case "div":
                if (args.length != 2) {
                    addError("operator " + funcName + " needs exactly 2 arguments");
                    return;
                }
                for (String arg : args) {
                    String type = checkExpression(arg.trim());
                    if (!type.equals("num")) {
                        addError("arguments of " + funcName + " must be numeric, found type: " + type);
                    }
                }
                break;
                    
            case "and":
            case "or":
                if (args.length != 2) {
                    addError("operator " + funcName + " needs exactly 2 arguments");
                    return;
                }
                for (String arg : args) {
                    String type = checkExpression(arg.trim());
                    if (!type.equals("bool")) {
                        addError("arguments of " + funcName + " must be boolean expressions (grt, eq, and, or)");
                    }
                }
                break;
                    
            case "eq":
                if (args.length != 2) {
                    addError("Operator EQ needs exactly 2 arguments");
                    return;
                }
                String type1 = checkExpression(args[0].trim());
                String type2 = checkExpression(args[1].trim());
                if (!type1.equals(type2)) {
                    addError("Arguments of EQ must be of the same type"); //must bot be num or both text
                }
                break;
                    
            case "not":
                if (args.length != 1) {
                    addError("operator NOT needs exactly 1 argument");
                    return;
                }
                String type = checkExpression(args[0].trim());
                if (!type.equals("bool")) {
                    addError("Argument of NOT must be a boolean expression"); //
                }
                break;
        }
    }
    
    private static boolean isNumeric(String str) { //check if a value is numeric
        if (str == null || str.trim().isEmpty()) {
            return false;
        }
        try {
            Double.parseDouble(str.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    private static String inferType(String expression) { //get num/text type
        expression = expression.trim();
        if (expression.isEmpty()) {
            return "unknown";
        }
        if (expression.startsWith("\"") && expression.endsWith("\"")) {
            return "text";
        }
        if (isNumeric(expression)) {
            return "num";
        }
        if (expression.startsWith("V_")) {
            return symbolTable.getOrDefault(expression, "bool"); //default 
        }
        return "unknown";
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

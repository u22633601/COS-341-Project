import java.io.*;
import java.util.*;

public class IntermediateCodeGenerator {
    private static Map<String, String> symbolTable = new HashMap<>();
    private static List<String> tokens = new ArrayList<>();
    private static int currentToken = 0;
    private static int tempCounter = 1;
    private static int labelCounter = 1;
    private static List<String> code = new ArrayList<>();
    private static final String SYMBOL_FILE = "Symbol.txt";
    private static final String OUTPUT_FILE = "intermediateCode.txt";

    static class Expression {
        String code;
        String place;

        Expression(String code, String place) {
            this.code = code;
            this.place = place;
        }
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java IntermediateCodeGenerator <input-file>");
            System.exit(1);
        }

        String inputFile = args[0];

        // Verify input file exists
        if (!new File(inputFile).exists()) {
            System.err.println("Error: Input file '" + inputFile + "' not found");
            System.exit(1);
        }

        try {
            // Load symbol table first
            try {
                loadSymbolTable(SYMBOL_FILE);
            } catch (IOException e) {
                System.err.println("Error: Could not read symbol table file '" + SYMBOL_FILE + "'");
                System.err.println("Make sure " + SYMBOL_FILE + " exists in the current directory");
                System.exit(1);
            }

            // Read and process input file
            String input;
            try {
                input = readInputFile(inputFile);
            } catch (IOException e) {
                System.err.println("Error reading input file: " + e.getMessage());
                System.exit(1);
                return;
            }

            // Generate code
            tokenize(input);
            parseProg();

            // Write output
            try {
                writeOutput(OUTPUT_FILE);
                System.out.println("Code generated successfully to " + OUTPUT_FILE);
                printGeneratedCode();
            } catch (IOException e) {
                System.err.println("Error writing output file: " + e.getMessage());
                System.exit(1);
            }

        } catch (Exception e) {
            System.err.println("Error during code generation: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void parseReturnStatement() {
        advance(); // Skip return
        // Expression returnExp = parseExpression();
        StringBuilder returnCode = new StringBuilder();
        // First add any code generated by the expression

        // Then add the return statement with the place
        code.add("RETURN " + returnCode);
        if (getCurrentToken().equals(";")) {
            advance();
        }
    }

    private static void parsePrintStatement() {
        advance(); // Skip return
        // Expression returnExp = parseExpression();
        StringBuilder returnCode = new StringBuilder();
        // First add any code generated by the expression

        // Then add the return statement with the place
        code.add("PRINT " + returnCode);
        if (getCurrentToken().equals(";")) {
            advance();
        }
    }

    private static String parseStatement() {
        String token = getCurrentToken();
        StringBuilder statement = new StringBuilder();
        System.out.println("Parsing statement: " + token);

        switch (token) {
            case "if":
                statement.append(parseIfStatement());
                break;
            case "return":
                advance();
                Expression returnExp = parseExpression();
                if (!returnExp.code.isEmpty()) {
                    statement.append(returnExp.code).append("\n");
                }
                statement.append("RETURN ").append(returnExp.place).append("\n");
                if (getCurrentToken().equals(";")) {
                    advance();
                }
                break;
            case "print":
                advance();
                Expression printExp = parseExpression();
                if (!printExp.code.isEmpty()) {
                    statement.append(printExp.code).append("\n");
                }
                statement.append("PRINT ").append(printExp.place).append("\n");
                if (getCurrentToken().equals(";")) {
                    advance();
                }
                break;
            case "skip":
                statement.append("REM DO NOTHING\n");
                advance();
                if (getCurrentToken().equals(";")) {
                    advance();
                }
                break;
            default:
                if (token.startsWith("V_")) {
                    statement.append(parseAssignment());
                } else {
                    System.out.println("Unexpected token: " + token);
                    advance(); // Skip unknown token
                }
                break;
        }
        return statement.toString();
    }

    private static String parseAssignment() {
        StringBuilder assignCode = new StringBuilder();
        String varName = translateVar(getCurrentToken());
        advance();
        String nextToken = getCurrentToken();

        if (nextToken.equals("<")) {
            advance(); // Skip
            advance(); // Skip input
            assignCode.append("INPUT ").append(varName).append("\n");
            if (getCurrentToken().equals(";")) {
                advance();
            }
        } else if (nextToken.equals("=")) {
            advance(); // Skip =
            Expression rightSideExp = parseExpression();
            if (!rightSideExp.code.isEmpty()) {
                assignCode.append(rightSideExp.code).append("\n");
            }
            assignCode.append(varName).append(" := ").append(rightSideExp.place).append("\n");
            if (getCurrentToken().equals(";")) {
                advance();
            }
        }
        return assignCode.toString();
    }

    private static String parseIfStatement() {
        StringBuilder ifCode = new StringBuilder();
        advance(); // Skip if
        Expression conditionExp = parseExpression();

        // Skip then
        while (!getCurrentToken().equals("then") && !getCurrentToken().equals("EOF")) {
            advance();
        }
        advance();

        String labelTrue = "L" + labelCounter++;
        String labelFalse = "L" + labelCounter++;
        String labelEnd = "L" + labelCounter++;

        // Add any code needed to evaluate the condition
        if (!conditionExp.code.isEmpty()) {
            ifCode.append(conditionExp.code).append("\n");
        }
        ifCode.append("IF ").append(conditionExp.place)
                .append(" GOTO ").append(labelTrue)
                .append(" ELSE GOTO ").append(labelFalse)
                .append("\n");

        ifCode.append(labelTrue).append(":\n");

        // Parse then block
        while (!getCurrentToken().equals("end") && !getCurrentToken().equals("EOF")) {
            ifCode.append(parseStatement());
        }
        advance(); // Skip end

        ifCode.append("GOTO ").append(labelEnd).append("\n");
        ifCode.append(labelFalse).append(":\n");

        if (getCurrentToken().equals("else")) {
            advance(); // Skip else
            if (getCurrentToken().equals("begin")) {
                advance(); // Skip begin
                while (!getCurrentToken().equals("end") && !getCurrentToken().equals("EOF")) {
                    ifCode.append(parseStatement());
                }
                advance(); // Skip end
            }
        }

        ifCode.append(labelEnd).append(":\n");

        return ifCode.toString();
    }
    // private static void parseIfStatement() {
    // advance(); // Skip if
    // Expression conditionExp = parseExpression();

    // // Skip then
    // while (!getCurrentToken().equals("then") && !getCurrentToken().equals("EOF"))
    // {
    // advance();
    // }
    // advance();

    // String labelTrue = "L" + labelCounter++;
    // String labelFalse = "L" + labelCounter++;
    // String labelEnd = "L" + labelCounter++;

    // StringBuilder ifCode = new StringBuilder();
    // // Add any code needed to evaluate the condition
    // if (!conditionExp.code.isEmpty()) {
    // ifCode.append(conditionExp.code).append("\n");
    // }
    // ifCode.append("IF ").append(conditionExp.place)
    // .append(" GOTO ").append(labelTrue)
    // .append(" ELSE GOTO ").append(labelFalse);
    // code.add(ifCode.toString());
    // code.add(labelTrue + ":");

    // // Parse then block
    // while (!getCurrentToken().equals("end") && !getCurrentToken().equals("EOF"))
    // {
    // parseStatement();
    // }
    // advance(); // Skip end

    // code.add("GOTO " + labelEnd);
    // code.add(labelFalse + ":");

    // if (getCurrentToken().equals("else")) {
    // advance(); // Skip else
    // while (!getCurrentToken().equals("end") && !getCurrentToken().equals("EOF"))
    // {
    // parseStatement();
    // }
    // advance(); // Skip end
    // }

    // code.add(labelEnd + ":");
    // }

    private static void printGeneratedCode() {
        System.out.println("\nGenerated Code:");
        for (String line : code) {
            // Split the code into lines and print each line separately
            for (String subline : line.split("\n")) {
                if (!subline.trim().isEmpty()) {
                    System.out.println(subline.trim());
                }
            }
        }
    }

    private static void writeOutput(String filename) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            for (String line : code) {
                // Split the code into lines and write each line separately
                for (String subline : line.split("\n")) {
                    if (!subline.trim().isEmpty()) {
                        writer.write(subline.trim());
                        writer.newLine();
                    }
                }
            }
        }
    }

    private static Expression parseExpression() {
    String token = getCurrentToken();
    System.out.println("DEBUG: parseExpression - Starting with token: " + token);

    if (isOperator(token)) {
        System.out.println("DEBUG: parseExpression - Found operator: " + token);
        if (token.equals("grt") || token.equals("eq") || token.equals("and") || token.equals("or")) {
            return parseBinaryOp(); // Don't advance here as parseBinaryOp handles it
        } else if (token.equals("not") || token.equals("sqrt")) {
            // Handle unary operators
            advance(); // Skip operator
            String opSymbol = translateOperator(token);

            advance(); // Skip (
            Expression exp = parseExpression();
            advance(); // Skip )

            String place = "t" + tempCounter++;
            StringBuilder codeBuilder = new StringBuilder();

            if (!exp.code.isEmpty()) {
                codeBuilder.append(exp.code).append("\n");
            }

            // Add the unary operation with operator at front
            codeBuilder.append(place).append(" := ")
                    .append(opSymbol).append(" ")
                    .append(exp.place);

            return new Expression(codeBuilder.toString(), place);
        } else {
            // For arithmetic operators (add, sub, mul, div)
            advance(); // Advance here for arithmetic operators
            if (getCurrentToken().equals("(")) {
                advance(); // Skip (
                String place1 = "t" + tempCounter++;
                Expression exp1 = parseExpression();
                advance(); // Skip ,
                String place2 = "t" + tempCounter++;
                Expression exp2 = parseExpression();
                advance(); // Skip )

                String place = "t" + tempCounter++;
                String opSymbol = translateOperator(token);

                StringBuilder codeBuilder = new StringBuilder();
                codeBuilder.append(place1).append(" := ").append(exp1.place).append("\n");
                codeBuilder.append(place2).append(" := ").append(exp2.place).append("\n");
                codeBuilder.append(place).append(" := ").append(place1)
                        .append(" ").append(opSymbol).append(" ")
                        .append(place2);

                System.out.println("DEBUG: parseExpression - arithmetic operation code:\n" + codeBuilder.toString());
                return new Expression(codeBuilder.toString(), place);
            }
        }
    } else if (token.startsWith("F_")) {
        String fnName = translateVar(token);
        advance(); // Skip function name
        advance(); // Skip (
        List<String> params = new ArrayList<>();
        for (int i = 0; i < 3; i++) { // Always 3 parameters
            if (i > 0) {
                advance(); // Skip comma
            }
            Expression param = parseExpression();
            params.add(param.place);
        }
        advance(); // Skip )

        // Generate temp variable for result
        String resultPlace = "t" + tempCounter++;
        String callCode = resultPlace + " := CALL_" + fnName + "(" + String.join(",", params) + ")";
        System.out.println("DEBUG: Function call generated: " + callCode);
        return new Expression(callCode, resultPlace);
    } else if (token.startsWith("V_")) {
        advance(); 
        return new Expression("", translateVar(token));
    } else {
        advance(); 
        return new Expression("", token); 
    }
    return new Expression("", "");
}



    private static String translateOperator(String op) {
        return switch (op) {
            case "add" -> "+";
            case "sub" -> "-";
            case "mul" -> "*";
            case "div" -> "/";
            case "and" -> "AND";
            case "or" -> "OR";
            case "eq" -> "=";
            case "grt" -> ">";
            case "not" -> "NOT";
            case "sqrt" -> "SQR";
            default -> op;
        };
    }

    private static String translateVar(String varName) {
        return symbolTable.getOrDefault(varName.trim(), varName.trim());
    }

    private static boolean isOperator(String token) {
        return token.equals("and") || token.equals("or") || token.equals("eq") ||
                token.equals("grt") || token.equals("add") || token.equals("sub") ||
                token.equals("mul") || token.equals("div") || token.equals("not") ||
                token.equals("sqrt");
    }

    private static boolean isEOF() {
        return currentToken >= tokens.size();
    }

    private static void parseProg() {
        StringBuilder mainCode = new StringBuilder();
        StringBuilder functionCode = new StringBuilder();

        // Skip main declarations until begin
        while (!getCurrentToken().equals("begin") && !isEOF()) {
            advance();
        }

        if (getCurrentToken().equals("begin")) {
            advance();
            System.out.println("DEBUG: Processing main code");
            while (!getCurrentToken().equals("end") && !isEOF()) {
                String statement = parseStatement();
                mainCode.append(statement);
            }
            code.add(mainCode.toString());
            code.add("STOP");

            advance(); // Skip end

            System.out.println("DEBUG: Processing functions");
            // Process functions after main
            while (!isEOF()) {
                String token = getCurrentToken();
                if (token.equals("num") || token.equals("void")) {
                    functionCode.append(parseFunctionDeclaration());
                }
                advance();
            }
            // Add REM END after all functions
            functionCode.append("REM END\n");
            code.add(functionCode.toString());
        }
    }

    private static String parseFunctionDeclaration() {
        StringBuilder funcCode = new StringBuilder();
        advance(); // Skip num/void

        // Get function name and parameters (HEADER)
        String funcName = translateVar(getCurrentToken());
        advance(); // Skip function name
        advance(); // Skip (

        List<String> params = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            if (i > 0)
                advance(); // Skip comma
            params.add(translateVar(getCurrentToken()));
            advance();
        }

        // Start BODY
        funcCode.append("\n"); // Spacing before function
        funcCode.append("REM BEGIN\n"); // PROLOG
        funcCode.append(funcName).append(" ").append("\n");

        // Skip to function body (ALGO)
        while (!getCurrentToken().equals("begin") && !isEOF()) {
            advance();
        }

        if (getCurrentToken().equals("begin")) {
            advance();
            while (!getCurrentToken().equals("end") && !isEOF()) {
                String statement = parseStatement();
                funcCode.append(statement);
            }
        }

        funcCode.append("REM END\n"); // EPILOG
        funcCode.append("STOP\n"); // Stop before next function

        return funcCode.toString();
    }
    private static String parseFunctions() {
        StringBuilder functionCode = new StringBuilder();
        while (!isEOF()) {
            String token = getCurrentToken();
            if (token.startsWith("F_")) {
                functionCode.append(parseFunction());
            }
            advance();
        }
        return functionCode.toString();
    }

    private static String parseFunction() {
        StringBuilder funcCode = new StringBuilder();
        String funcName = translateVar(getCurrentToken());
        advance(); // Skip function name

        // Skip parameters and type declarations until begin
        while (!getCurrentToken().equals("begin") && !isEOF()) {
            advance();
        }

        if (getCurrentToken().equals("begin")) {
            advance();
            funcCode.append("FUNCTION ").append(funcName).append("\n");
            funcCode.append(parseAlgo());
            funcCode.append("END FUNCTION\n");
        }

        return funcCode.toString();
    }

    private static String parseCommand() {
        String token = getCurrentToken();
        switch (token) {
            case "skip":
                advance();
                return "REM DO NOTHING\n";
            case "halt":
                advance();
                return "STOP\n";
            case "if":
                return parseBranch();
            case "return":
                advance();
                Expression returnExp = parseExpression();
                return (returnExp.code.isEmpty() ? "" : returnExp.code + "\n") +
                        "RETURN " + returnExp.place + "\n";
            case "print":
                advance();
                Expression returnExps = parseExpression();
                return (returnExps.code.isEmpty() ? "" : returnExps.code + "\n") +
                        "PRINT " + returnExps.place + "\n";
            default:
                if (token.startsWith("V_")) {
                    return parseAssign();
                } else if (token.startsWith("F_")) {
                    return parseCall();
                }
                advance();
                return "";
        }
    }

    private static String parseAssign() {
        String varName = translateVar(getCurrentToken());
        advance();
        String token = getCurrentToken();

        if (token.equals("<")) {
            advance(); // Skip
            advance(); // Skip input
            return "INPUT " + varName + "\n";
        } else if (token.equals("=")) {
            advance();
            Expression exp = parseExpression();
            StringBuilder assignCode = new StringBuilder();

            // Add the arithmetic operation code if it exists
            if (!exp.code.isEmpty()) {
                assignCode.append(exp.code).append("\n");
            }

            // Add the assignment
            assignCode.append(varName).append(" := ").append(exp.place).append("\n");

            return assignCode.toString();
        }

        return "";
    }

    private static Expression parseCondition() {
        if (getCurrentToken().equals("not")) {
            return parseUnaryOp();
        } else if (isBinaryOp(getCurrentToken())) {
            return parseBinaryOp();
        }
        return parseExpression();
    }

    private static String parseBranch() {
        advance(); // Skip if
        Expression cond = parseCondition();
        String labelTrue = "L" + labelCounter++;
        String labelFalse = "L" + labelCounter++;
        String labelEnd = "L" + labelCounter++;

        StringBuilder branchCode = new StringBuilder();

        // First add condition evaluation code if any
        if (!cond.code.isEmpty()) {
            branchCode.append(cond.code).append("\n");
        }

        // Add the if condition and goto statements
        branchCode.append("IF ").append(cond.place)
                .append(" GOTO ").append(labelTrue)
                .append(" ELSE GOTO ").append(labelFalse)
                .append("\n");

        // Add true branch label and code
        branchCode.append(labelTrue).append(":\n");
        advance(); // Skip then
        if (getCurrentToken().equals("begin")) {
            advance(); // Skip begin
            while (!getCurrentToken().equals("end") && !getCurrentToken().equals("EOF")) {
                branchCode.append(parseCommand());
            }
            advance(); // Skip end
        }

        // Add goto to skip else part
        branchCode.append("GOTO ").append(labelEnd).append("\n");

        // Add false branch label and code
        branchCode.append(labelFalse).append(":\n");
        if (getCurrentToken().equals("else")) {
            advance(); // Skip else
            if (getCurrentToken().equals("begin")) {
                advance(); // Skip begin
                while (!getCurrentToken().equals("end") && !getCurrentToken().equals("EOF")) {
                    branchCode.append(parseCommand());
                }
                advance(); // Skip end
            }
        }

        // Add end label
        branchCode.append(labelEnd).append(":\n");

        return branchCode.toString();
    }

    // Update tokenize method to handle all tokens properly
    private static void tokenize(String input) {
        String processed = input.replaceAll("([(){},;])", " $1 ")
                .replaceAll("<", " < ")
                .replaceAll("=", " = ")
                .replaceAll("\"([^\"]*)\"", " \"$1\" ")
                .replaceAll("\n", " ")
                .trim();

        tokens = new ArrayList<>();
        for (String token : processed.split("\\s+")) {
            if (!token.isEmpty()) {
                tokens.add(token);
                System.out.println("Added token: " + token);
            }
        }
    }

    private static String getCurrentToken() {
        return currentToken < tokens.size() ? tokens.get(currentToken) : "EOF";
    }

    private static void advance() {
        if (currentToken < tokens.size()) {
            currentToken++;
        }
    }

    private static String parseAlgo() {
        StringBuilder algoCode = new StringBuilder();
        while (!getCurrentToken().equals("end") && !isEOF()) {
            algoCode.append(parseInstruc());
        }
        advance(); // Skip 'end'
        return algoCode.toString();
    }

    private static String parseMainBlock() {
        StringBuilder mainCode = new StringBuilder();
        while (!getCurrentToken().equals("end") && !isEOF()) {
            if (getCurrentToken().equals("begin")) {
                advance();
                continue;
            }
            String statement = parseStatement();
            mainCode.append(statement);
            if (getCurrentToken().equals(";")) {
                advance();
            }
        }
        return mainCode.toString();
    }

    private static String parseFunctionBlock() {
        StringBuilder funcCode = new StringBuilder();
        advance(); // Skip num/void

        String funcName = getCurrentToken();
        String translatedName = translateVar(funcName);
        System.out.println("DEBUG: Processing function: " + translatedName);

        advance(); // Skip function name
        advance(); // Skip (

        // Get parameters
        List<String> params = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            if (i > 0)
                advance(); // Skip comma
            params.add(translateVar(getCurrentToken()));
            advance();
        }

        funcCode.append("\n").append(translatedName).append(" ")
                .append(String.join(",", params)).append("\n");

        // Skip to function body
        while (!getCurrentToken().equals("begin") && !isEOF()) {
            advance();
        }

        if (getCurrentToken().equals("begin")) {
            advance();
            // Parse function body
            while (!getCurrentToken().equals("end") && !isEOF()) {
                String statement = parseStatement();
                funcCode.append(statement);
                if (getCurrentToken().equals(";")) {
                    advance();
                }
            }
        }

        return funcCode.toString();
    }

    private static String handleFunctionDeclaration(String funcName) {
        StringBuilder funcCode = new StringBuilder();
        String fName = translateVar(funcName);
        System.out.println("DEBUG: Translating function: " + funcName + " to " + fName);

        // Build function header
        funcCode.append("\n").append(fName).append(" ");

        advance(); // Skip (
        // Get parameters
        List<String> params = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            if (i > 0) {
                advance(); // Skip comma
            }
            String param = translateVar(getCurrentToken());
            params.add(param);
            advance();
        }
        funcCode.append(String.join(",", params)).append("\n");

        // Skip until begin
        System.out.println("DEBUG: Skipping to function body");
        while (!getCurrentToken().equals("begin") && !isEOF()) {
            advance();
        }

        if (getCurrentToken().equals("begin")) {
            advance();
            System.out.println("DEBUG: Processing function body");
            String body = parseAlgo();
            funcCode.append(body);
        }

        return funcCode.toString();
    }

    // Helper method to peek next token
    private static String peekNextToken() {
        if (currentToken + 1 < tokens.size()) {
            return tokens.get(currentToken + 1);
        }
        return "EOF";
    }

    private static String parseInstruc() {
        String commandCode = parseCommand();
        if (getCurrentToken().equals(";")) {
            advance();
            String instrucCode = parseInstruc();
            return commandCode + instrucCode;
        }
        return commandCode;
    }

    private static Expression parseUnaryOp() {
        String op = getCurrentToken();
        advance(); // Skip operator
        advance(); // Skip (
        Expression arg = parseExpression();
        advance(); // Skip )

        String place = "t" + tempCounter++;
        String opCode = op.equals("sqrt") ? "SQR" : op;

        return new Expression(
                arg.code + place + " := " + opCode + "(" + arg.place + ")\n",
                place);
    }

    private static Expression parseBinaryOp() {
        String op = getCurrentToken();
        System.out.println("DEBUG: parseBinaryOp - operator: " + op);
        advance(); // Skip operator

        if (getCurrentToken().equals("(")) {
            advance(); // Skip (
        }

        // Handle first expression
        Expression exp1;
        if (isOperator(getCurrentToken())) {
            exp1 = parseBinaryOp();
        } else {
            exp1 = parseExpression();
        }

        if (getCurrentToken().equals(",")) {
            advance(); // Skip ,
        }

        // Handle second expression
        Expression exp2;
        if (isOperator(getCurrentToken())) {
            exp2 = parseBinaryOp();
        } else {
            exp2 = parseExpression();
        }

        if (getCurrentToken().equals(")")) {
            advance(); // Skip )
        }

        // Generate temporary variables and code for both operands
        StringBuilder codeBuilder = new StringBuilder();
        if (!exp1.code.isEmpty()) {
            codeBuilder.append(exp1.code).append("\n");
        }
        if (!exp2.code.isEmpty()) {
            codeBuilder.append(exp2.code).append("\n");
        }

        String place1 = "t" + tempCounter++;
        String place2 = "t" + tempCounter++;
        String resultPlace = "t" + tempCounter++;

        // Add assignments to temp variables
        codeBuilder.append(place1).append(" := ").append(exp1.place).append("\n");
        codeBuilder.append(place2).append(" := ").append(exp2.place).append("\n");

        // Use proper operator translation
        String opSymbol = translateOperator(op);
        codeBuilder.append(resultPlace).append(" := ").append(place1)
                .append(" ").append(opSymbol).append(" ")
                .append(place2);

        return new Expression(codeBuilder.toString(), resultPlace);
    }

    private static void parseAssignmentOrInput() {
        String varName = translateVar(getCurrentToken());
        advance();
        String nextToken = getCurrentToken();
        System.out.println("Processing assignment: " + varName + " " + nextToken);

        if (nextToken.equals("<")) {
            advance(); // Skip
            advance(); // Skip input
            code.add("INPUT " + varName);
            if (getCurrentToken().equals(";")) {
                advance();
            }
        } else if (nextToken.equals("=")) {
            advance(); // Skip =
            System.out.println("Parsing right side expression");
            Expression rightSideExp = parseExpression();

            // Add arithmetic operation code before the assignment
            if (!rightSideExp.code.isEmpty()) {
                String[] lines = rightSideExp.code.split("\n");
                for (String line : lines) {
                    if (!line.trim().isEmpty()) {
                        code.add(line.trim()); // This adds the t1 = 2, t2 = 4, t3 = t1 + t2
                    }
                }
            }

            // Now add the assignment
            code.add(varName + " = " + rightSideExp.place);

            if (getCurrentToken().equals(";")) {
                advance();
            }
        }
    }

    private static String parseCall() {
        String fnName = translateVar(getCurrentToken());
        advance(); // Skip function name
        advance(); // Skip (

        List<String> params = new ArrayList<>();
        while (!getCurrentToken().equals(")")) {
            if (getCurrentToken().equals(",")) {
                advance();
                continue;
            }
            Expression param = parseExpression();
            params.add(param.place);
        }
        advance(); // Skip )

        return "CALL_" + fnName + "(" + String.join(",", params) + ")\n";
    }

    // Helper methods
    private static boolean isUnaryOp(String token) {
        return token.equals("not") || token.equals("sqrt");
    }

    private static boolean isBinaryOp(String token) {
        return token.equals("and") || token.equals("or") || token.equals("eq") ||
                token.equals("grt") || token.equals("add") || token.equals("sub") ||
                token.equals("mul") || token.equals("div");
    }

    private static void loadSymbolTable(String filename) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filename));
        String line;
        while ((line = reader.readLine()) != null) {
            String[] parts = line.split(":");
            if (parts.length >= 2) {
                symbolTable.put(parts[0].trim(), parts[1].trim());
                System.out.println("Loaded symbol: " + parts[0].trim() + " -> " + parts[1].trim());
            }
        }
        reader.close();
    }

    private static String readInputFile(String filename) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filename));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        return sb.toString();
    }

    private static void generateCode() {
        // Skip until main
        while (!getCurrentToken().equals("main") && !getCurrentToken().equals("EOF")) {
            advance();
        }

        if (getCurrentToken().equals("EOF")) {
            System.out.println("Error: Reached end of file without finding main");
            return;
        }

        // Skip main
        advance();

        // Skip variable declarations until begin
        while (!getCurrentToken().equals("begin") && !getCurrentToken().equals("EOF")) {
            advance();
        }

        if (getCurrentToken().equals("EOF")) {
            System.out.println("Error: Reached end of file without finding begin");
            return;
        }

        // Skip begin
        advance();

        // Process statements until end
        while (!getCurrentToken().equals("end") && !getCurrentToken().equals("EOF")) {
            System.out.println("Processing statement starting with: " + getCurrentToken());
            parseStatement();
        }

        code.add("STOP");
    }
}

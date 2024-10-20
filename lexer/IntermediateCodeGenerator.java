import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Arrays;

class SymbolInfo {
    String newName;
    String type;

    SymbolInfo(String newName, String type) {
        this.newName = newName;
        this.type = type;
    }
}

public class IntermediateCodeGenerator {
    private Map<String, SymbolInfo> symbolTable;
    private int labelCounter;
    private int tempCounter;

    public IntermediateCodeGenerator() {
        symbolTable = new HashMap<>();
        labelCounter = 0;
        tempCounter = 0;
        loadSymbolTable();
    }

    private void loadSymbolTable() {
        try (BufferedReader br = new BufferedReader(new FileReader("Symbol.txt"))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length == 3) {
                    symbolTable.put(parts[0].trim(), new SymbolInfo(parts[1].trim(), parts[2].trim()));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    public String translate(Map<String, Object> node) {
        String nodeType = (String) node.get("type");

        switch (nodeType) {
            case "PROG":
                String algoCode = translate(getMapFromNode(node, "ALGO"));
                String functionsCode = translate(getMapFromNode(node, "FUNCTIONS"));
                return algoCode + "STOP\n" + functionsCode;

            case "ALGO":
                return translate(getMapFromNode(node, "INSTRUC"));

            case "INSTRUC":
                if (node.containsKey("COMMAND")) {
                    String commandCode = translate(getMapFromNode(node, "COMMAND"));
                    String instrucCode = node.containsKey("INSTRUC") ? translate(getMapFromNode(node, "INSTRUC")) : "";
                    return commandCode + "\n" + instrucCode;
                } else {
                    return "REM END";
                }

            case "COMMAND":
                if (node.containsKey("skip")) {
                    return "REM DO NOTHING";
                } else if (node.containsKey("halt")) {
                    return "STOP";
                } else if (node.containsKey("print")) {
                    String atomicCode = translate(getMapFromNode(node, "ATOMIC"));
                    return "PRINT " + atomicCode;
                } else if (node.containsKey("ASSIGN")) {
                    return translate(getMapFromNode(node, "ASSIGN"));
                } else if (node.containsKey("CALL")) {
                    return translate(getMapFromNode(node, "CALL"));
                } else if (node.containsKey("BRANCH")) {
                    return translate(getMapFromNode(node, "BRANCH"));
                } else if (node.containsKey("RETURN")) {
                    return translate(getMapFromNode(node, "RETURN"));
                }
                break;

            case "ATOMIC":
                if (node.containsKey("VNAME")) {
                    String vname = (String) node.get("VNAME");
                    SymbolInfo info = symbolTable.get(vname);
                    return (info != null) ? info.newName : vname;
                } else if (node.containsKey("CONST")) {
                    String constValue = (String) node.get("CONST");
                    return constValue.startsWith("\"") ? constValue : " " + constValue + " ";
                }
                break;

            case "ASSIGN":
                if (node.containsKey("input")) {
                    String vnameCode = translate(createAtomicNode((String) node.get("VNAME")));
                    return "INPUT " + vnameCode;
                } else {
                    String vnameCode = translate(createAtomicNode((String) node.get("VNAME")));
                    String termCode = translate(getMapFromNode(node, "TERM"));
                    return vnameCode + " := " + termCode;
                }

            case "TERM":
                if (node.containsKey("ATOMIC")) {
                    return translate(createAtomicNode((String) node.get("ATOMIC")));
                } else if (node.containsKey("CALL")) {
                    return translate(getMapFromNode(node, "CALL"));
                } else if (node.containsKey("OP")) {
                    return translate(getMapFromNode(node, "OP"));
                }
                break;

            case "CALL":
                String fname = (String) node.get("FNAME");
                SymbolInfo fnameInfo = symbolTable.get(fname);
                String fnameCode = (fnameInfo != null) ? fnameInfo.newName : fname;
                String p1 = translate(createAtomicNode((String) node.get("ATOMIC1")));
                String p2 = translate(createAtomicNode((String) node.get("ATOMIC2")));
                String p3 = translate(createAtomicNode((String) node.get("ATOMIC3")));
                return "CALL_" + fnameCode + "(" + p1 + "," + p2 + "," + p3 + ")";

            case "OP":
                if (node.containsKey("UNOP")) {
                    String opName = translate(createUnopNode((String) node.get("UNOP")));
                    String argCode = translate(getMapFromNode(node, "ARG"));
                    return opName + "(" + argCode + ")";
                } else if (node.containsKey("BINOP")) {
                    String opName = translate(createBinopNode((String) node.get("BINOP")));
                    String arg1Code = translate(getMapFromNode(node, "ARG1"));
                    String arg2Code = translate(getMapFromNode(node, "ARG2"));
                    return arg1Code + " " + opName + " " + arg2Code;
                }
                break;

            case "UNOP":
                String unop = (String) node.get("name");
                if (unop.equals("not")) {
                    return "NOT";
                } else if (unop.equals("sqrt")) {
                    return "SQR";
                }
                return unop;

            case "BINOP":
                String binop = (String) node.get("name");
                Map<String, String> binopMap = new HashMap<String, String>() {
                    {
                        put("eq", "=");
                        put("grt", ">");
                        put("add", "+");
                        put("sub", "-");
                        put("mul", "*");
                        put("div", "/");
                    }
                };
                return binopMap.getOrDefault(binop, binop);

            case "BRANCH":
                return translateBranch(node);

            case "COND":
                if (node.containsKey("SIMPLE")) {
                    return translate(getMapFromNode(node, "SIMPLE"));
                } else if (node.containsKey("COMPOSIT")) {
                    return translateComposit(getMapFromNode(node, "COMPOSIT"));
                }
                break;

            case "SIMPLE":
                String simpleBinopCode = translate(createBinopNode((String) node.get("BINOP")));
                String simpleAtomic1Code = translate(createAtomicNode((String) node.get("ATOMIC1")));
                String simpleAtomic2Code = translate(createAtomicNode((String) node.get("ATOMIC2")));
                return simpleAtomic1Code + " " + simpleBinopCode + " " + simpleAtomic2Code;

            case "RETURN":
                String returnValue = translate(getMapFromNode(node, "ATOMIC"));
                return "RETURN " + returnValue;

            case "FUNCTIONS":
                StringBuilder functionsBuilder = new StringBuilder();
                for (Object value : node.values()) {
                    if (value instanceof Map) {
                        functionsBuilder.append(translate((Map<String, Object>) value)).append("\n");
                    }
                }
                return functionsBuilder.toString();

            case "FUNCTION":
                String functionName = symbolTable.get(node.get("FNAME")).newName;
                String functionBody = translate(getMapFromNode(node, "BODY"));
                return "FUNC " + functionName + "\n" + functionBody + "END_FUNC\n";
        }

        return "";
    }

    private String translateBranch(Map<String, Object> node) {
        String condCode = translate(getMapFromNode(node, "COND"));
        String algo1Code = translate(getMapFromNode(node, "ALGO1"));
        String algo2Code = translate(getMapFromNode(node, "ALGO2"));
        String labelTrue = newLabel();
        String labelFalse = newLabel();
        String labelEnd = newLabel();

        StringBuilder result = new StringBuilder();
        result.append(condCode).append("\n");
        result.append("GOTO ").append(labelTrue).append("\n");
        result.append(labelFalse).append(":\n");
        result.append(algo2Code).append("\n");
        result.append("GOTO ").append(labelEnd).append("\n");
        result.append(labelTrue).append(":\n");
        result.append(algo1Code).append("\n");
        result.append(labelEnd).append(":\n");

        return result.toString();
    }

    private String translateComposit(Map<String, Object> node) {
        if (node.containsKey("BINOP")) {
            String binop = (String) node.get("BINOP");
            String simple1Code = translate(getMapFromNode(node, "SIMPLE1"));
            String simple2Code = translate(getMapFromNode(node, "SIMPLE2"));
            if (binop.equals("and")) {
                String labelFalse = newLabel();
                return "IF NOT(" + simple1Code + ") GOTO " + labelFalse + "\n" +
                        "IF NOT(" + simple2Code + ") GOTO " + labelFalse + "\n" +
                        "GOTO L_TRUE\n" +
                        labelFalse + ":\n" +
                        "GOTO L_FALSE";
            } else if (binop.equals("or")) {
                String labelTrue = newLabel();
                return "IF " + simple1Code + " GOTO " + labelTrue + "\n" +
                        "IF " + simple2Code + " GOTO " + labelTrue + "\n" +
                        "GOTO L_FALSE\n" +
                        labelTrue + ":\n" +
                        "GOTO L_TRUE";
            }
        } else if (node.containsKey("UNOP")) {
            String unop = (String) node.get("UNOP");
            String simpleCode = translate(getMapFromNode(node, "SIMPLE"));
            if (unop.equals("not")) {
                return "IF " + simpleCode + " GOTO L_FALSE\n" +
                        "GOTO L_TRUE";
            }
        }
        return "";
    }

    private String newLabel() {
        return "L" + (++labelCounter);
    }

    private String newTemp() {
        return "t" + (++tempCounter);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getMapFromNode(Map<String, Object> node, String key) {
        Object value = node.get(key);
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        throw new IllegalArgumentException("Expected a Map for key: " + key);
    }

    
    private Map<String, Object> createAtomicNode(String vname) {
        Map<String, Object> node = new HashMap<>();
        node.put("type", "ATOMIC");
        node.put("VNAME", vname);
        return node;
    }


    
    private Map<String, Object> createUnopNode(String name) {
        Map<String, Object> node = new HashMap<>();
        node.put("type", "UNOP");
        node.put("name", name);
        return node;
    }

    private Map<String, Object> createBinopNode(String name) {
        Map<String, Object> node = new HashMap<>();
        node.put("type", "BINOP");
        node.put("name", name);
        return node;
    }

    public static void main(String[] args) {
        IntermediateCodeGenerator generator = new IntermediateCodeGenerator();

        // Print the symbol table for debugging
        System.out.println("Symbol Table:");
        for (Map.Entry<String, SymbolInfo> entry : generator.symbolTable.entrySet()) {
            System.out.println(entry.getKey() + " -> " + entry.getValue().newName + " (" + entry.getValue().type + ")");
        }
        System.out.println();
        // Create the syntax tree based on the given input
        Map<String, Object> syntaxTree = new HashMap<>();
        syntaxTree.put("type", "PROG");

        Map<String, Object> algo = new HashMap<>();
        algo.put("type", "ALGO");

        Map<String, Object> instruc = new HashMap<>();
        instruc.put("type", "INSTRUC");

        // Input statements
        Map<String, Object> inputX = new HashMap<>();
        inputX.put("type", "COMMAND");
        inputX.put("ASSIGN", createAssign("V_x", "input"));

        Map<String, Object> inputY = new HashMap<>();
        inputY.put("type", "COMMAND");
        inputY.put("ASSIGN", createAssign("V_y", "input"));

        // If statement
        Map<String, Object> ifStatement = new HashMap<>();
        ifStatement.put("type", "COMMAND");
        ifStatement.put("BRANCH", createIfStatement());

        // Combine instructions
        instruc.put("COMMAND", inputX);
        instruc.put("INSTRUC", createInstruc(inputY, createInstruc(ifStatement, null)));

        algo.put("INSTRUC", instruc);
        syntaxTree.put("ALGO", algo);

        // Add functions
        Map<String, Object> functions = new HashMap<>();
        functions.put("type", "FUNCTIONS");
        functions.put("F_average",
                createFunction("F_average", "num", Arrays.asList("V_a", "V_b", "V_dummy"), createFAverageBody()));
        functions.put("F_ave", createFunction("F_ave", "void", Arrays.asList("V_e", "V_f", "V_g"), createFAveBody()));
        syntaxTree.put("FUNCTIONS", functions);

        String intermediateCode = generator.translate(syntaxTree);
        System.out.println("Generated Intermediate Code:");
        System.out.println(intermediateCode);
    }

    private static Map<String, Object> createAssign(String varName, String value) {
        Map<String, Object> assign = new HashMap<>();
        assign.put("type", "ASSIGN");
        assign.put("VNAME", varName);
        if (value.equals("input")) {
            assign.put("input", true);
        } else {
            Map<String, Object> term = new HashMap<>();
            term.put("type", "TERM");
            term.put("ATOMIC", value);
            assign.put("TERM", term);
        }
        return assign;
    }

    private static Map<String, Object> createInstruc(Map<String, Object> command, Map<String, Object> nextInstruc) {
        Map<String, Object> instruc = new HashMap<>();
        instruc.put("type", "INSTRUC");
        instruc.put("COMMAND", command);
        if (nextInstruc != null) {
            instruc.put("INSTRUC", nextInstruc);
        }
        return instruc;
    }

    private static Map<String, Object> createIfStatement() {
        Map<String, Object> ifStatement = new HashMap<>();
        ifStatement.put("type", "BRANCH");
        ifStatement.put("COND", createCondition());
        ifStatement.put("ALGO1", createThenAlgo());
        ifStatement.put("ALGO2", createElseAlgo());
        return ifStatement;
    }

    private static Map<String, Object> createCondition() {
        Map<String, Object> condition = new HashMap<>();
        condition.put("type", "COND");
        condition.put("COMPOSIT", createCompositeCondition());
        return condition;
    }

    private static Map<String, Object> createCompositeCondition() {
        Map<String, Object> composite = new HashMap<>();
        composite.put("type", "COMPOSIT");
        composite.put("BINOP", "and");
        composite.put("SIMPLE1", createSimpleCondition("V_x", "0", "grt"));
        composite.put("SIMPLE2", createSimpleCondition("V_y", "0", "grt"));
        return composite;
    }

    private static Map<String, Object> createSimpleCondition(String var, String value, String op) {
        Map<String, Object> simple = new HashMap<>();
        simple.put("type", "SIMPLE");
        simple.put("BINOP", op);
        simple.put("ATOMIC1", var);
        simple.put("ATOMIC2", value);
        return simple;
    }

    private static Map<String, Object> createThenAlgo() {
        Map<String, Object> algo = new HashMap<>();
        algo.put("type", "ALGO");
        algo.put("INSTRUC", createInstruc(
                createAssignCall("V_result", "F_average", Arrays.asList("V_x", "V_y", "0")),
                createInstruc(createReturn("V_result"), createInstruc(createSkip(), null))));
        return algo;
    }

    private static Map<String, Object> createElseAlgo() {
        Map<String, Object> algo = new HashMap<>();
        algo.put("type", "ALGO");
        Map<String, Object> nestedIf = new HashMap<>();
        nestedIf.put("type", "COMMAND");
        nestedIf.put("BRANCH", createNestedIfStatement());
        algo.put("INSTRUC", createInstruc(nestedIf, null));
        return algo;
    }

    private static Map<String, Object> createNestedIfStatement() {
        Map<String, Object> ifStatement = new HashMap<>();
        ifStatement.put("type", "BRANCH");
        ifStatement.put("COND", createNestedCondition());
        ifStatement.put("ALGO1", createNestedThenAlgo());
        ifStatement.put("ALGO2", createNestedElseAlgo());
        return ifStatement;
    }

    private static Map<String, Object> createNestedCondition() {
        Map<String, Object> condition = new HashMap<>();
        condition.put("type", "COND");
        condition.put("COMPOSIT", createNestedCompositeCondition());
        return condition;
    }

    private static Map<String, Object> createNestedCompositeCondition() {
        Map<String, Object> composite = new HashMap<>();
        composite.put("type", "COMPOSIT");
        composite.put("BINOP", "or");
        composite.put("SIMPLE1", createSimpleCondition("V_x", "0", "eq"));
        composite.put("SIMPLE2", createSimpleCondition("V_y", "0", "eq"));
        return composite;
    }

    private static Map<String, Object> createNestedThenAlgo() {
        Map<String, Object> algo = new HashMap<>();
        algo.put("type", "ALGO");
        algo.put("INSTRUC", createInstruc(createReturn("\"Zero\""), null));
        return algo;
    }

    private static Map<String, Object> createNestedElseAlgo() {
        Map<String, Object> algo = new HashMap<>();
        algo.put("type", "ALGO");
        algo.put("INSTRUC", createInstruc(
                createAssignOp("V_result", "mul", "V_x", "V_y"),
                createInstruc(createReturn("V_result"), null)));
        return algo;
    }

    private static Map<String, Object> createAssignCall(String resultVar, String funcName, List<String> args) {
        Map<String, Object> assign = new HashMap<>();
        assign.put("type", "COMMAND");
        Map<String, Object> assignInner = new HashMap<>();
        assignInner.put("type", "ASSIGN");
        assignInner.put("VNAME", resultVar);
        Map<String, Object> term = new HashMap<>();
        term.put("type", "TERM");
        Map<String, Object> call = new HashMap<>();
        call.put("type", "CALL");
        call.put("FNAME", funcName);
        call.put("ATOMIC1", args.get(0));
        call.put("ATOMIC2", args.get(1));
        call.put("ATOMIC3", args.get(2));
        term.put("CALL", call);
        assignInner.put("TERM", term);
        assign.put("ASSIGN", assignInner);
        return assign;
    }

    private static Map<String, Object> createAssignOp(String resultVar, String op, String arg1, String arg2) {
        Map<String, Object> assign = new HashMap<>();
        assign.put("type", "COMMAND");
        Map<String, Object> assignInner = new HashMap<>();
        assignInner.put("type", "ASSIGN");
        assignInner.put("VNAME", resultVar);
        Map<String, Object> term = new HashMap<>();
        term.put("type", "TERM");
        Map<String, Object> opMap = new HashMap<>();
        opMap.put("type", "OP");
        opMap.put("BINOP", op);
        Map<String, Object> arg1Map = new HashMap<>();
        arg1Map.put("type", "ATOMIC");
        arg1Map.put("VNAME", arg1);
        Map<String, Object> arg2Map = new HashMap<>();
        arg2Map.put("type", "ATOMIC");
        arg2Map.put("VNAME", arg2);
        opMap.put("ARG1", arg1Map);
        opMap.put("ARG2", arg2Map);
        term.put("OP", opMap);
        assignInner.put("TERM", term);
        assign.put("ASSIGN", assignInner);
        return assign;
    }

    private static Map<String, Object> createReturn(String value) {
        Map<String, Object> returnCommand = new HashMap<>();
        returnCommand.put("type", "COMMAND");
        Map<String, Object> returnInner = new HashMap<>();
        returnInner.put("type", "RETURN");
        Map<String, Object> atomic = new HashMap<>();
        atomic.put("type", "ATOMIC");
        if (value.startsWith("\"")) {
            atomic.put("CONST", value);
        } else {
            atomic.put("VNAME", value);
        }
        returnInner.put("ATOMIC", atomic);
        returnCommand.put("RETURN", returnInner);
        return returnCommand;
    }

    private static Map<String, Object> createSkip() {
        Map<String, Object> skip = new HashMap<>();
        skip.put("type", "COMMAND");
        skip.put("skip", true);
        return skip;
    }

    private static Map<String, Object> createFunction(String name, String returnType, List<String> params,
            Map<String, Object> body) {
        Map<String, Object> function = new HashMap<>();
        function.put("type", "FUNCTION");
        function.put("FNAME", name);
        function.put("FTYP", returnType);
        function.put("PARAMS", params);
        function.put("BODY", body);
        return function;
    }

    private static Map<String, Object> createFAverageBody() {
        Map<String, Object> body = new HashMap<>();
        body.put("type", "ALGO");
        body.put("INSTRUC", createInstruc(
                createAssignCall("V_res", "F_ave", Arrays.asList("V_x", "V_y", "0")),
                createInstruc(
                        createAssignOp("V_sum", "add", "V_a", "V_b"),
                        createInstruc(
                                createAssign("V_count", "2"),
                                createInstruc(
                                        createAssignOp("V_res", "div", "V_sum", "V_count"),
                                        null)))));
        return body;
    }

    private static Map<String, Object> createFAveBody() {
        Map<String, Object> body = new HashMap<>();
        body.put("type", "ALGO");
        body.put("INSTRUC", createInstruc(
                createAssignCall("V_rese", "F_average", Arrays.asList("V_x", "V_y", "0")),
                createInstruc(
                        createAssignOp("V_sume", "add", "V_a", "V_b"),
                        createInstruc(
                                createAssign("V_counte", "2"),
                                createInstruc(
                                        createAssignOp("V_rese", "div", "V_sume", "V_counte"),
                                        null)))));
        return body;
    }
}
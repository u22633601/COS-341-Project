import java.io.*;
import java.util.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import org.w3c.dom.*;

public class SLRParser {
    private static final int MAX_STEPS = 1000;
    private Stack<Integer> stack = new Stack<>();
    private List<Token> tokens = new ArrayList<>();
    private int currentTokenIndex = 0;
    private int nodeId = 1;
    private Document doc;
    private static final String INPUT_FILE = "lexer.xml";
    private static final String OUTPUT_FILE = "parser.xml";

    private static class Token {
        int id;
        String type;
        String value;

        Token(int id, String type, String value) {
            this.id = id;
            this.type = type;
            this.value = value;
        }
    }

    public void parse() {
        try {
            System.out.println("Starting parsing process...");
            System.out.println("Reading tokens from " + INPUT_FILE);
            readTokens(INPUT_FILE);

            System.out.println("Initializing parser...");
            initializeParser();

            System.out.println("Parsing tokens...");
            Element root = parseTokens();

            System.out.println("Writing output to " + OUTPUT_FILE);
            writeOutput(root, OUTPUT_FILE);

            System.out.println("Parsing completed successfully!");

        } catch (Exception e) {
            System.err.println("\nParsing failed:");
            System.err.println("→ " + e.getMessage());
            System.exit(1);
        }
    }

    private void readTokens(String inputFile) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new File(inputFile));

        NodeList tokenNodes = document.getElementsByTagName("TOK");
        for (int i = 0; i < tokenNodes.getLength(); i++) {
            Element tokenElement = (Element) tokenNodes.item(i);
            int id = Integer.parseInt(tokenElement.getElementsByTagName("ID").item(0).getTextContent());
            String type = tokenElement.getElementsByTagName("CLASS").item(0).getTextContent();
            String value = tokenElement.getElementsByTagName("WORD").item(0).getTextContent();
            tokens.add(new Token(id, type, value));
        }
    }

    private void initializeParser() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        doc = builder.newDocument();
        stack.push(0);
    }

    private Element parseTokens() throws Exception {
        Element syntree = doc.createElement("SYNTREE");
        doc.appendChild(syntree);

        Element root = null;
        Element innerNodes = doc.createElement("INNERNODES");
        Element leafNodes = doc.createElement("LEAFNODES");

        int steps = 0;
        while (steps < MAX_STEPS) {
            int state = stack.peek();
            Token token = currentTokenIndex < tokens.size() ? tokens.get(currentTokenIndex) : new Token(-1, "EOF", "$");
            String action = getAction(state, token);

            System.out.println("Step " + steps + ":");
            System.out.println("  Current state: " + state);
            System.out.println("  Current token: " + token.value + " (Type: " + token.type + ")");
            System.out.println("  Action: " + action);

            if (action.startsWith("s")) {
                int nextState = Integer.parseInt(action.substring(1));
                stack.push(nextState);
                Element leaf = createLeafNode(token);
                leafNodes.appendChild(leaf);
                currentTokenIndex++;
                System.out.println("  Shift to state " + nextState);
            } else if (action.startsWith("r")) {
                int ruleNumber = Integer.parseInt(action.substring(1));
                Reduction reduction = reduce(ruleNumber);
                Element node = createInnerNode(reduction);
                innerNodes.appendChild(node);
                if (root == null && reduction.lhs.equals("PROG")) {
                    root = createRootNode(node);
                    syntree.appendChild(root);
                }
                for (int i = 0; i < reduction.rhsLength; i++) {
                    stack.pop();
                }
                int gotoState = getGotoState(stack.peek(), reduction.lhs);
                if (gotoState == -1) {
                    //throw new Exception("Invalid GOTO state for " + reduction.lhs + " in state " + stack.peek());
                    if (gotoState == -1) {
                        throw new Exception("Invalid GOTO state for non-terminal '" + reduction.lhs + "' after reducing using rule " + ruleNumber + " in state " + stack.peek());
                    }
                    
                }
                stack.push(gotoState);
                System.out.println("  Reduce using rule " + ruleNumber + ": " + reduction.lhs + " -> "
                        + reduction.rhsLength + " symbols");
                System.out.println("  Goto state " + gotoState);
            } else if (action.equals("acc")) {
                System.out.println("  Accept");
                break;
            } else {
                // System.out.println("  Error: Unexpected action");
                // throw new Exception("Parsing error at token: " + token.value + " in state " + state);
                System.out.println(" Error: Unexpected action " + action + "' for token '" + token.value + "' (Type: " + token.type + ") at position " + currentTokenIndex + " in state " + state);
                throw new Exception("Parsing error at token '" + token.value + "' (Type: " + token.type + ") at position " + currentTokenIndex + " in state " + state + ". No valid action found.");
            }

            System.out.println("  Stack: " + stack);
            System.out.println();

            steps++;
        }

        if (steps == MAX_STEPS) {
            throw new Exception("Maximum number of steps reached. Parsing incomplete.");
        }

        syntree.appendChild(innerNodes);
        syntree.appendChild(leafNodes);
        return syntree;
    }

private String getAction(int state, Token token) {
        String tokenValue = token.value;
        String tokenType = token.type;

        switch (state) {
            case 0:
                if (tokenValue.equals("main")) return "s2";
                break;
            case 1:
                if (tokenValue.equals("$"))
                    return "s3";
                break;
            case 2:
                if (tokenValue.equals("num")) return "s6";
                if (tokenValue.equals("text")) return "s7";
                if (tokenValue.equals("begin")) return "r2";
                break;
            case 3:
                if (tokenValue.equals("$"))
                    return "acc";
                break;
            case 4:
                if (tokenValue.equals("begin")) return "s9";
                break;
            case 5:
                if (tokenType.equals("v") && tokenValue.startsWith("V_"))
                    return "s11";
                break;
            case 6:
                if (tokenType.equals("v") && tokenValue.startsWith("V_"))
                    return "r4";
                break;
            case 7:
                if (tokenType.equals("v") && tokenValue.startsWith("V_"))
                    return "r5";

                break;
            case 8:
                if (tokenValue.equals("$")) return "r47";
                if (tokenValue.equals("num"))
                    return "s16";
                if (tokenValue.equals("end"))
                    return "r47";
                if (tokenValue.equals("void")) return "s17";
                break;
            case 9:
                if (tokenType.equals("v") && tokenValue.startsWith("V_"))
                    return "s11";
                if (tokenValue.equals("end")) return "r8";
                if (tokenValue.equals("skip")) return "s20";
                if (tokenValue.equals("halt")) return "s21";
                if ((tokenValue.equals("return")) || (tokenValue.equals("print")))
                    return "s22";
                if (tokenValue.equals("if"))
                    return "s28";
                if (tokenType.equals("f") && tokenValue.startsWith("F_"))
                    return "s29";
                break;
            case 10:
                if (tokenValue.equals(","))
                    return "s30";
                break;
            case 11:
                if (tokenValue.equals(","))
                    return "r6";
                if (tokenValue.equals(";"))
                    return "r6";
                if (tokenValue.equals("lst"))
                    return "r6";
                if (tokenValue.equals("="))
                    return "r6";
                if (tokenValue.equals(")"))
                    return "r6";
                break;
            case 12:
                if (tokenValue.equals("$")) return "r1";
                break;
            case 13:
                if (tokenValue.equals("$")) return "r47";
                if (tokenValue.equals("num"))
                    return "s16";
                if (tokenValue.equals("end")) return "r47";
                if (tokenValue.equals("void")) return "s17";
                break;
            case 14:
                if (tokenValue.equals("{")) return "s34";
                break;
            case 15:
                if (tokenType.equals("f") && tokenValue.startsWith("F_"))
                    return "s29";
                break;
            case 16:
                if (tokenType.equals("f") && tokenValue.startsWith("F_"))
                    return "r51";
            case 17:
                if (tokenType.equals("f") && tokenValue.startsWith("F_"))
                    return "r52";
                break;
            case 18:
                if (tokenValue.equals("end")) return "s36";
                break;
            case 19:
                if (tokenValue.equals(";")) return "s37";
                break;
            case 20:
                if (tokenValue.equals(";"))
                    return "r10";
                break;
            case 21:
                if (tokenValue.equals(";")) return "r11";
                break;
            case 22:
                if (tokenType.equals("v") && tokenValue.startsWith("V_"))
                    return "s11";
                if (tokenType.equals("n"))
                    return "s41";
                if (tokenType.equals("t"))
                    return "s42";
                break;
            case 23:
                if (tokenValue.equals(";"))
                    return "r13";
            case 24:
                if (tokenValue.equals(";"))
                    return "r14";
            case 25:
                if (tokenValue.equals(";"))
                    return "r15";
                break;
            case 26:
                if (tokenValue.equals("lst")) return "s43";
                if (tokenValue.equals("=")) return "s44";
                break;
            case 27:
                if (tokenValue.equals("("))
                    return "s45";
                break;
            case 28:
                if (tokenValue.equals("not")) return "s59";
                if (tokenValue.equals("sqrt"))
                    return "s60";
                if (tokenValue.equals("or"))
                    return "s51";
                if (tokenValue.equals("and"))
                    return "s52";
                if (tokenValue.equals("eq"))
                        return "s53";
                if (tokenValue.equals("grt"))
                        return "s54";
                if (tokenValue.equals("add"))
                        return "s55";
                if (tokenValue.equals("sub"))
                        return "s56";
                if (tokenValue.equals("mul"))
                        return "s57";
                if (tokenValue.equals("div"))
                        return "s58";
                break;
            case 29:
                if (tokenValue.equals("(")) // was )
                    return "r46";
                break;
            case 30:
                if (tokenValue.equals("num"))
                    return "s6";
                if (tokenValue.equals("text"))
                    return "s7";
                if (tokenValue.equals("begin"))
                    return "r2";
                break;
            case 31:
                if (tokenValue.equals("$"))
                    return "r48";
                if (tokenValue.equals("end"))
                    return "r48";
                break;
            case 32:
                if (tokenValue.equals("$"))
                    return "r49";
                if (tokenValue.equals("num"))
                    return "r49";
                if (tokenValue.equals("end"))
                    return "r49";
                if (tokenValue.equals("void"))
                    return "r49";
                break;
            case 33:
                if (tokenValue.equals("num")) return "s6";
                if (tokenValue.equals("text")) return "s7";
                break;
            case 34:
                if (tokenValue.equals("num"))
                    return "r54";
                if (tokenValue.equals("text"))
                    return "r54";
                break;
            case 35:
                if (tokenValue.equals("(")) //was )
                    return "s64";
                break;
            case 36:
                if (tokenValue.equals("$"))
                    return "r7";
                if (tokenValue.equals("num"))
                    return "r7";
                if (tokenValue.equals("else"))
                    return "r7";
                if (tokenValue.equals(";"))
                    return "r7";
                if (tokenValue.equals("void"))
                    return "r7";
                if (tokenValue.equals("}"))
                    return "r7";
                break;
            case 37:
                if (tokenType.equals("v") && tokenValue.startsWith("V_"))
                    return "s11";
                if (tokenValue.equals("end"))
                    return "r8";
                if (tokenValue.equals("skip"))
                    return "s20";
                if (tokenValue.equals("halt"))
                    return "s21";
                if ((tokenValue.equals("return")) || (tokenValue.equals("print")))
                    return "s22";
                if (tokenValue.equals("if"))
                    return "s28";
                if (tokenType.equals("f") && tokenValue.startsWith("F_"))
                    return "s29";
                break;
            case 38:
                if (tokenValue.equals(";"))
                    return "r12";
                break;
            case 39:
                if (tokenValue.equals(","))
                    return "r16";
                if (tokenValue.equals(";"))
                    return "r16";
                if (tokenValue.equals(")"))
                    return "r16";
                break;
            case 40:
                if (tokenValue.equals(","))
                    return "r17";
                if (tokenValue.equals(";"))
                    return "r17";
                if (tokenValue.equals(")"))
                    return "r17";
                break;
            case 41:
                if (tokenValue.equals(","))
                    return "r18";
                if (tokenValue.equals(";"))
                    return "r18";
                if (tokenValue.equals(")"))
                    return "r18";
                break;
            case 42:
                if (tokenValue.equals(","))
                    return "r19";
                if (tokenValue.equals(";"))
                    return "r19";
                if (tokenValue.equals(")"))
                    return "r19";
                break;
            case 43:
                if (tokenValue.equals("input"))
                    return "s66";
                break;
            case 44:
                if (tokenType.equals("v") && tokenValue.startsWith("V_"))
                    return "s11";
                if (tokenType.equals("n"))
                    return "s41";
                if (tokenType.equals("t"))
                    return "s42";
                if (tokenValue.equals("not"))
                    return "s59";
                if (tokenValue.equals("sqrt"))
                    return "s60";
                if (tokenValue.equals("or"))
                    return "s51";
                if (tokenValue.equals("and"))
                    return "s52";
                if (tokenValue.equals("eq"))
                    return "s53";
                if (tokenValue.equals("grt"))
                    return "s54";
                if (tokenValue.equals("add"))
                    return "s55";
                if (tokenValue.equals("sub"))
                    return "s56";
                if (tokenValue.equals("mul"))
                    return "s57";
                if (tokenValue.equals("div"))
                    return "s58";
                if (tokenType.equals("f") && tokenValue.startsWith("F_"))
                    return "s29";
                break;
            case 45:
                if (tokenType.equals("v") && tokenValue.startsWith("V_"))
                    return "s11";
                if (tokenType.equals("n"))
                    return "s41";
                if (tokenType.equals("t"))
                    return "s42";
                break;
            case 46:
                if (tokenValue.equals("then"))
                    return "s74";
            case 47:
                if (tokenValue.equals("then")) return "r31";
                break;
            case 48:
                if (tokenValue.equals("then"))
                    return "r32";
                break;
            case 49:
                if (tokenValue.equals("(")) return "s75";
                break;
            case 50:
                if (tokenValue.equals("("))
                    return "s76";
            case 51:
                if (tokenValue.equals("("))
                    return "r38";
                break;
            case 52:
                if (tokenValue.equals("("))
                    return "r39";
                break;
            case 53:
                if (tokenValue.equals("("))
                    return "r40";
                break;
            case 54:
                if (tokenValue.equals("("))
                    return "r41";
                break;
            case 55:
                if (tokenValue.equals("("))
                    return "r42";
                break;
            case 56:
                if (tokenValue.equals("("))
                    return "r43";
                break;
            case 57:
                if (tokenValue.equals("("))
                    return "r44";
                break;
            case 58:
                if (tokenValue.equals("("))
                    return "r45";
                break;
            case 59:
                if (tokenValue.equals("("))
                    return "r36";
                break;
            case 60:
                if (tokenValue.equals("("))
                    return "r37";
                break;
            case 61:
                if (tokenValue.equals("begin"))
                    return "r3";
                break;
            case 62:
                if (tokenValue.equals("begin"))
                    return "s9";
            case 63:
                if (tokenType.equals("v") && tokenValue.startsWith("V_")) return "s11";
                break;
            case 64:
                if (tokenType.equals("v") && tokenValue.startsWith("V_"))
                    return "s11";
                break;
            case 65:
                if (tokenValue.equals("end")) return "r9";
                break;
            case 66:
                if (tokenValue.equals(";")) return "r20";
                break;
            case 67:
                if (tokenValue.equals(";"))
                    return "r21";
                break;
            case 68:
                if (tokenValue.equals(";"))
                    return "r24";
                break;
            case 69:
                if (tokenValue.equals(";"))
                    return "r25";
                break;
            case 70:
                if (tokenValue.equals(";"))
                    return "r26";
                break;
            case 71:
                if (tokenValue.equals("(")) return "s80"; //tess [was ) ]
                break;
            case 72:
                if (tokenValue.equals("(")) //tess [was ) ]
                    return "s81";
                break;
            case 73:
                if (tokenValue.equals(","))
                    return "s82";
                break;
            case 74:
                if (tokenValue.equals("begin"))
                    return "s9";
            case 75:
                if (tokenType.equals("v") && tokenValue.startsWith("V_"))
                    return "s11";
                if (tokenType.equals("n"))
                    return "s41";
                if (tokenType.equals("t"))
                    return "s42";
                if (tokenValue.equals("or"))
                    return "s51";
                if (tokenValue.equals("and"))
                    return "s52";
                if (tokenValue.equals("eq"))
                    return "s53";
                if (tokenValue.equals("grt"))
                    return "s54";
                if (tokenValue.equals("add"))
                    return "s55";
                if (tokenValue.equals("sub"))
                    return "s56";
                if (tokenValue.equals("mul"))
                    return "s57";
                if (tokenValue.equals("div"))
                    return "s58";
                break;
            case 76:
                if (tokenValue.equals("or"))
                    return "s51";
                if (tokenValue.equals("and"))
                    return "s52";
                if (tokenValue.equals("eq"))
                    return "s53";
                if (tokenValue.equals("grt"))
                    return "s54";
                if (tokenValue.equals("add"))
                    return "s55";
                if (tokenValue.equals("sub"))
                    return "s56";
                if (tokenValue.equals("mul"))
                    return "s57";
                if (tokenValue.equals("div"))
                    return "s58";
                break;
            case 77:
                if (tokenValue.equals("}"))
                    return "s89";
                break;
            case 78:
                if (tokenValue.equals(","))
                    return "s90";
                break;
            case 79:
                if (tokenValue.equals(","))
                    return "s91"; // Comma between function arguments
                break;
            case 80:
                if (tokenType.equals("v") && tokenValue.startsWith("V_"))
                    return "s11";
                if (tokenType.equals("n"))
                    return "s41";
                if (tokenType.equals("t"))
                    return "s42";
                if (tokenValue.equals("not"))
                    return "s59";
                if (tokenValue.equals("sqrt"))
                    return "s60";
                if (tokenValue.equals("or"))
                    return "s51";
                if (tokenValue.equals("and"))
                    return "s52";
                if (tokenValue.equals("eq"))
                    return "s53";
                if (tokenValue.equals("grt"))
                    return "s54";
                if (tokenValue.equals("add"))
                    return "s55";
                if (tokenValue.equals("sub"))
                    return "s56";
                if (tokenValue.equals("mul"))
                    return "s57";
                if (tokenValue.equals("div"))
                    return "s58";
                break;
            case 81:
                if (tokenType.equals("v") && tokenValue.startsWith("V_"))
                    return "s11";
                if (tokenType.equals("n"))
                    return "s41";
                if (tokenType.equals("t"))
                    return "s42";
                if (tokenValue.equals("not"))
                    return "s59";
                if (tokenValue.equals("sqrt"))
                    return "s60";
                if (tokenValue.equals("or"))
                    return "s51";
                if (tokenValue.equals("and"))
                    return "s52";
                if (tokenValue.equals("eq"))
                    return "s53";
                if (tokenValue.equals("grt"))
                    return "s54";
                if (tokenValue.equals("add"))
                    return "s55";
                if (tokenValue.equals("sub"))
                    return "s56";
                if (tokenValue.equals("mul"))
                    return "s57";
                if (tokenValue.equals("div"))
                    return "s58";
                break;
            case 82:
                if (tokenType.equals("v") && tokenValue.startsWith("V_"))
                    return "s11";
                if (tokenType.equals("n"))
                    return "s41";
                if (tokenType.equals("t"))
                    return "s42";
                break;
            case 83:
                if (tokenValue.equals("else"))
                    return "s97";
                break;
            case 84:
                if (tokenValue.equals(","))
                    return "s98";
                break;
            case 85:
                if (tokenValue.equals(",")) return "s99";
                break;
            case 86:
                if (tokenValue.equals("(")) return "s100";
                break;
            case 87:
                if (tokenValue.equals(")")) return "s101";
                break;
            case 88:
                if (tokenValue.equals("$"))
                    return "r47";
                if (tokenValue.equals("num"))
                    return "s16";
                if (tokenValue.equals("end"))
                    return "r47";
                if (tokenValue.equals("void"))
                    return "s17";
                break;
            case 89:
                if (tokenValue.equals("$"))
                    return "r55";
                if (tokenValue.equals("num"))
                    return "r55";
                if (tokenValue.equals("end"))
                    return "r55";
                if (tokenValue.equals("void"))
                    return "r55";
                break;
            case 90:
                if (tokenValue.equals("num"))
                    return "s6";
                if (tokenValue.equals("text"))
                    return "s7";
                break;
            case 91:
                if (tokenType.equals("v") && tokenValue.startsWith("V_"))
                    return "s11";
                break;
            case 92:
                if (tokenValue.equals(")")) return "s106";
                break;
            case 93:
                if (tokenValue.equals(","))
                    return "r29";
                if (tokenValue.equals(")"))
                    return "r29";
                break;
            case 94:
                if (tokenValue.equals(","))
                    return "r30";
                if (tokenValue.equals(")"))
                    return "r30";
                break;
            case 95:
                if (tokenValue.equals(","))
                    return "s107";
                break;
            case 96:
                if (tokenValue.equals(","))
                    return "s108";
                break;
            case 97:
                if (tokenValue.equals("begin"))
                    return "s9";
                break;
            case 98:
                if (tokenType.equals("v") && tokenValue.startsWith("V_"))
                    return "s11";
                if (tokenType.equals("n"))
                    return "s41";
                if (tokenType.equals("t"))
                    return "s42";
                break;
            case 99:
                if (tokenValue.equals("or"))
                    return "s51";
                if (tokenValue.equals("and"))
                    return "s52";
                if (tokenValue.equals("eq"))
                    return "s53";
                if (tokenValue.equals("grt"))
                    return "s54";
                if (tokenValue.equals("add"))
                    return "s55";
                if (tokenValue.equals("sub"))
                    return "s56";
                if (tokenValue.equals("mul"))
                    return "s57";
                if (tokenValue.equals("div"))
                    return "s58";
                break;
            case 100:
                if (tokenType.equals("v") && tokenValue.startsWith("V_"))
                    return "s11";
                if (tokenType.equals("n"))
                    return "s41";
                if (tokenType.equals("t"))
                    return "s42";
                break;
            case 101:
                if (tokenValue.equals("then"))
                    return "r35";
                break;
            case 102:
                if (tokenValue.equals("end"))
                    return "s112";
                break;
            case 103:
                if (tokenValue.equals("end"))
                    return "r57";
                break;
            case 104:
                if (tokenType.equals("v") && tokenValue.startsWith("V_"))
                    return "s11";
                break;
            case 105:
                if (tokenValue.equals(","))
                    return "s114";
                break;
            case 106:
                if (tokenValue.equals(","))
                    return "r27";
                if (tokenValue.equals(";"))
                    return "r27";
                if (tokenValue.equals(")"))
                    return "r27";
                break;
            case 107:
                if (tokenType.equals("v") && tokenValue.startsWith("V_"))
                    return "s11";
                if (tokenType.equals("n"))
                    return "s41";
                if (tokenType.equals("t"))
                    return "s42";
                if (tokenValue.equals("not"))
                    return "s59";
                if (tokenValue.equals("sqrt"))
                    return "s60";
                if (tokenValue.equals("or"))
                    return "s51";
                if (tokenValue.equals("and"))
                    return "s52";
                if (tokenValue.equals("eq"))
                    return "s53";
                if (tokenValue.equals("grt"))
                    return "s54";
                if (tokenValue.equals("add"))
                    return "s55";
                if (tokenValue.equals("sub"))
                    return "s56";
                if (tokenValue.equals("mul"))
                    return "s57";
                if (tokenValue.equals("div"))
                    return "s58";
                break;
            case 108:
                if (tokenType.equals("v") && tokenValue.startsWith("V_"))
                    return "s11";
                if (tokenType.equals("n"))
                    return "s41";
                if (tokenType.equals("t"))
                    return "s42";
                break;
            case 109:
                if (tokenValue.equals(";"))
                    return "r23";
                break;
            case 110:
                if (tokenValue.equals(")"))
                    return "s117";
                break;
            case 111:
                if (tokenValue.equals(")"))
                    return "s118";
                break;
            case 112:
                if (tokenValue.equals("$"))
                    return "r53";
                if (tokenValue.equals("num"))
                    return "r53";
                if (tokenValue.equals("end"))
                    return "r53";
                if (tokenValue.equals("void"))
                    return "r53";
                break;
            case 113:
                if (tokenValue.equals(","))
                    return "s119";
                break;
            case 114:
                if (tokenType.equals("v") && tokenValue.startsWith("V_"))
                    return "s11";
                break;
            case 115:
                if (tokenValue.equals(")"))
                    return "s121";
                break;
            case 116:
                if (tokenValue.equals(")"))
                    return "s122";
                break;
            case 117:
                if (tokenValue.equals(","))
                    return "r33";
                if (tokenValue.equals(")"))
                    return "r33";
                if (tokenValue.equals("then"))
                    return "r33";
                break;
            case 118:
                if (tokenValue.equals("then"))
                    return "r34";
                break;
            case 119:
                if (tokenValue.equals("num"))
                    return "s6";
                if (tokenValue.equals("text"))
                    return "s7";
                break;
            case 120:
                if (tokenValue.equals(")"))
                    return "s124";
                break;
            case 121:
                if (tokenValue.equals(","))
                    return "r28";
                if (tokenValue.equals(";"))
                    return "r28";
                if (tokenValue.equals(")")) // was ;
                    return "r28";
                break;
            case 122:
                if (tokenValue.equals(";"))
                    return "r22";
                break;
            case 123:
                if (tokenType.equals("v") && tokenValue.startsWith("V_"))
                    return "s11";
                break;
            case 124:
                if (tokenValue.equals("{"))
                    return "r50";
                break;
            case 125:
                if (tokenValue.equals(","))
                    return "s126";
                break;
            case 126:
                if (tokenValue.equals("begin"))
                    return "r56";
                break;
                default:
                return "\n Error: Unexpected token '" + token.value + "' (Type: " + token.type + ") at position " + currentTokenIndex + " in state " + state;
        }
        return "\n Error: No valid action found for token '" + token.value + "' (Type: " + token.type + ") at position " + currentTokenIndex + " in state " + state;
    }

    private int getGotoState(int state, String nonTerminal) {
        switch (state) {
            case 0:
                if (nonTerminal.equals("PROG"))
                    return 1;
                break;
            case 2:
                if (nonTerminal.equals("GLOBVARS"))
                    return 4;
                if (nonTerminal.equals("VTYP"))
                    return 5;
                break;
            case 4:
                if (nonTerminal.equals("ALGO"))
                    return 8;
                break;
            case 5:
                if (nonTerminal.equals("VNAME"))
                    return 10;
                break;
            case 8:
                if (nonTerminal.equals("FUNCTIONS"))
                    return 12;
                if (nonTerminal.equals("DECL"))
                    return 13;
                if (nonTerminal.equals("HEADER"))
                    return 14;
                if (nonTerminal.equals("FTYP"))
                    return 15;
                break;
            case 9:
                if (nonTerminal.equals("VNAME"))
                    return 26;
                if (nonTerminal.equals("INSTRUC"))
                    return 18;
                if (nonTerminal.equals("COMMAND"))
                    return 19;
                if (nonTerminal.equals("ASSIGN"))
                    return 23;
                if (nonTerminal.equals("CALL"))
                    return 24;
                if (nonTerminal.equals("BRANCH"))
                    return 25;
                break;
            case 13:
                if (nonTerminal.equals("FUNCTIONS"))
                    return 31;
                if (nonTerminal.equals("DECL"))
                    return 13;
                if (nonTerminal.equals("HEADER"))
                    return 14;
                if (nonTerminal.equals("FTYP"))
                    return 15;
                break;
            case 14:
                if (nonTerminal.equals("BODY"))
                    return 32;
                if (nonTerminal.equals("PROLOG"))
                    return 33;
                break;
            case 15:
                if (nonTerminal.equals("FNAME"))
                    return 35;
                break;
            case 22:
                if (nonTerminal.equals("ATOMIC"))
                    return 38;
                if (nonTerminal.equals("CONST"))
                    return 40;
                if (nonTerminal.equals("VNAME"))
                    return 39;
                break;
            // case 27:
            //     if (nonTerminal.equals("ATOMIC"))
            //         return 45;
            //     if (nonTerminal.equals("CONST"))
            //         return 40;
            //     if (nonTerminal.equals("VNAME"))
            //         return 39;
            //     break;
            case 28:
                if (nonTerminal.equals("COND"))
                    return 46;
                if (nonTerminal.equals("SIMPLE"))
                    return 47;
                if (nonTerminal.equals("COMPOSIT"))
                    return 48;
                if (nonTerminal.equals("UNOP"))
                    return 50;
                if (nonTerminal.equals("BINOP"))
                    return 49;
                break;
            case 30:
                if (nonTerminal.equals("GLOBVARS"))
                    return 61;
                if (nonTerminal.equals("VTYP"))
                    return 5;
                break;
            case 33:
                if (nonTerminal.equals("VTYP"))
                    return 63;
                if (nonTerminal.equals("LOCVARS"))
                    return 62;
                break;
            case 37:
                if (nonTerminal.equals("VNAME"))
                    return 26;
                if (nonTerminal.equals("INSTRUC"))
                    return 65;
                if (nonTerminal.equals("COMMAND"))
                    return 19;
                if (nonTerminal.equals("ASSIGN"))
                    return 23;
                if (nonTerminal.equals("CALL"))
                    return 24;
                if (nonTerminal.equals("BRANCH"))
                    return 25;
                if (nonTerminal.equals("FNAME"))
                    return 27;
                break;
            case 44:
                if (nonTerminal.equals("ATOMIC"))
                    return 68;
                if (nonTerminal.equals("CONST"))
                    return 40;
                if (nonTerminal.equals("VNAME"))
                    return 39;
                if (nonTerminal.equals("CALL"))
                    return 69;
                if (nonTerminal.equals("TERM"))
                    return 67;
                if (nonTerminal.equals("OP"))
                    return 70;
                if (nonTerminal.equals("UNOP"))
                    return 71;
                if (nonTerminal.equals("BINOP"))
                    return 72;
                if (nonTerminal.equals("FNAME"))
                    return 27;
                break;
            case 45:
                if (nonTerminal.equals("ATOMIC"))
                    return 73;
                if (nonTerminal.equals("CONST"))
                    return 40;
                if (nonTerminal.equals("VNAME"))
                    return 39;
                break;
            case 62:
                if (nonTerminal.equals("ALGO"))
                    return 77;
                break;
            case 63:
                if (nonTerminal.equals("VNAME"))
                    return 78;
                break;
            case 64:
                if (nonTerminal.equals("VNAME"))
                    return 79;
                break;
            // case 72:
            //     if (nonTerminal.equals("VNAME"))
            //         return 39;
            //     if (nonTerminal.equals("ATOMIC"))
            //         return 83;
            //     if (nonTerminal.equals("CONST"))
            //         return 40;
            //     if (nonTerminal.equals("OP"))
            //         return 84;
            //     if (nonTerminal.equals("ARG"))
            //         return 82;
            //     break;
            // case 73:
            //     if (nonTerminal.equals("VNAME"))
            //         return 39;
            //     if (nonTerminal.equals("ATOMIC"))
            //         return 83;
            //     if (nonTerminal.equals("CONST"))
            //         return 40;
            //     if (nonTerminal.equals("OP"))
            //         return 84;
            //     if (nonTerminal.equals("ARG"))
            //         return 85;
            //     break;
            // case 74:
            //     if (nonTerminal.equals("VNAME"))
            //         return 39;
            //     if (nonTerminal.equals("ATOMIC"))
            //         return 83;
            //     if (nonTerminal.equals("CONST"))
            //         return 40;
            //     break;
            case 74:
                if (nonTerminal.equals("ALGO"))
                    return 83;
                break;
            case 75:
                if (nonTerminal.equals("VNAME"))
                    return 39;
                if (nonTerminal.equals("ATOMIC"))
                    return 84;
                if (nonTerminal.equals("CONST"))
                    return 40;
                if (nonTerminal.equals("SIMPLE"))
                    return 85;
                if (nonTerminal.equals("BINOP"))
                    return 86;
                break;
            case 76:
                if (nonTerminal.equals("SIMPLE"))
                    return 87;
                if (nonTerminal.equals("BINOP"))
                    return 86;
                break;
            case 77:
                if (nonTerminal.equals("EPILOG"))
                    return 88;
                break;
            case 80:
                if (nonTerminal.equals("VNAME"))
                    return 39;
                if (nonTerminal.equals("ATOMIC"))
                    return 93;
                if (nonTerminal.equals("CONST"))
                    return 40;
                if (nonTerminal.equals("OP"))
                    return 94;
                if (nonTerminal.equals("ARG"))
                    return 92;
                if (nonTerminal.equals("UNOP"))
                    return 71;
                if (nonTerminal.equals("BINOP"))
                    return 72;
                break;
            case 81:
                if (nonTerminal.equals("VNAME"))
                    return 39;
                if (nonTerminal.equals("ATOMIC"))
                    return 93;
                if (nonTerminal.equals("CONST"))
                    return 40;
                if (nonTerminal.equals("OP"))
                    return 94;
                if (nonTerminal.equals("ARG"))
                    return 95;
                if (nonTerminal.equals("UNOP"))
                    return 71;
                if (nonTerminal.equals("BINOP"))
                    return 72;
                break;
            case 82:
                if (nonTerminal.equals("VNAME"))
                    return 39;
                if (nonTerminal.equals("ATOMIC"))
                    return 96;
                if (nonTerminal.equals("CONST"))
                    return 40;
                break;
            // case 88:
            //     if (nonTerminal.equals("ATOMIC"))
            //         return 99;
            //     if (nonTerminal.equals("CONST"))
            //         return 40;
            //     if (nonTerminal.equals("VNAME"))
            //         return 39;
            //     break;
            case 88:
                if (nonTerminal.equals("FUNCTIONS"))
                    return 103;
                if (nonTerminal.equals("DECL"))
                    return 13;
                if (nonTerminal.equals("HEADER"))
                    return 14;
                if (nonTerminal.equals("FTYP"))
                    return 15;
                if (nonTerminal.equals("SUBFUNCS"))
                    return 102;
                break;
            case 90:
                if (nonTerminal.equals("VTYP"))
                    return 104;
                break;
            case 91:
                if (nonTerminal.equals("VNAME"))
                    return 105;
                break;
            // case 96:
            //     if (nonTerminal.equals("VNAME"))
            //         return 39;
            //     if (nonTerminal.equals("ATOMIC"))
            //         return 83;
            //     if (nonTerminal.equals("CONST"))
            //         return 40;
            //     if (nonTerminal.equals("OP"))
            //         return 84;
            //     if (nonTerminal.equals("ARG"))
            //         return 106;
            //     break;
            // case 97:
            //     if (nonTerminal.equals("VNAME"))
            //         return 39;
            //     if (nonTerminal.equals("ATOMIC"))
            //         return 107;
            //     if (nonTerminal.equals("CONST"))
            //         return 40;
            //     break;
            case 97:
                if (nonTerminal.equals("ALGO"))
                    return 109;
                break;
            case 98:
                if (nonTerminal.equals("VNAME"))
                    return 39;
                if (nonTerminal.equals("ATOMIC"))
                    return 110;
                if (nonTerminal.equals("CONST"))
                    return 40;
                break;
            case 99:
                if (nonTerminal.equals("SIMPLE"))
                    return 111;
                if (nonTerminal.equals("BINOP"))
                    return 86;
                break;
            case 100:
                if (nonTerminal.equals("VNAME"))
                    return 39;
                if (nonTerminal.equals("ATOMIC"))
                    return 84;
                if (nonTerminal.equals("CONST"))
                    return 40;
                break;
            case 104:
                if (nonTerminal.equals("VNAME"))
                    return 113;
                break;
            case 107:
                if (nonTerminal.equals("VNAME"))
                    return 39;
                if (nonTerminal.equals("ATOMIC"))
                    return 93;
                if (nonTerminal.equals("CONST"))
                    return 40;
                if (nonTerminal.equals("OP"))
                    return 94;
                if (nonTerminal.equals("ARG"))
                    return 115;
                if (nonTerminal.equals("UNOP"))
                    return 71;
                if (nonTerminal.equals("BINOP"))
                    return 72;
                break;
            case 108:
                if (nonTerminal.equals("VNAME"))
                    return 39;
                if (nonTerminal.equals("ATOMIC"))
                    return 116;
                if (nonTerminal.equals("CONST"))
                    return 40;
                break;
            case 114:
                if (nonTerminal.equals("VNAME"))
                    return 120;
                break;
            case 119:
                if (nonTerminal.equals("VTYP"))
                    return 123;
                break;
            case 123:
                if (nonTerminal.equals("VNAME"))
                    return 125;
                break;
            default:
                break;

        }

        // If no GOTO state is found, return an error state
        return -1;
    }

    private Reduction reduce(int ruleNumber) {
        switch (ruleNumber) {
            case 0:
                return new Reduction("S", 2);////is it really 2????????????
            case 1:
                return new Reduction("PROG", 4); // PROG -> main GLOBVARS ALGO FUNCTIONS
            case 2:
                return new Reduction("GLOBVARS", 0); // GLOBVARS -> ε
            case 3:
                return new Reduction("GLOBVARS", 4); // GLOBVARS -> VTYP VNAME , GLOBVARS
            case 4:
                return new Reduction("VTYP", 1); // VTYP -> num
            case 5:
                return new Reduction("VTYP", 1); // VTYP -> text
            case 6:
                return new Reduction("VNAME", 1); // VNAME -> v
            case 7:
                return new Reduction("ALGO", 3); // ALGO -> begin INSTRUC end
            case 8:
                return new Reduction("INSTRUC", 0); // INSTRUC -> ε
            case 9:
                return new Reduction("INSTRUC", 3); // INSTRUC -> COMMAND ; INSTRUC
            case 10:
                return new Reduction("COMMAND", 1); // COMMAND -> skip //here
            case 11:
                return new Reduction("COMMAND", 1); // COMMAND -> halt
            case 12:
                return new Reduction("COMMAND", 2); // COMMAND -> return ATOMIC
            case 13:
                return new Reduction("COMMAND", 1); // COMMAND -> ASSIGN
            case 14:
                return new Reduction("COMMAND", 1); // COMMAND -> CALL
            case 15:
                return new Reduction("COMMAND", 1); // COMMAND -> BRANCH
            case 16:
                return new Reduction("ATOMIC", 1); // ATOMIC -> VNAME
            case 17:
                return new Reduction("ATOMIC", 1); // ATOMIC -> CONST
            case 18:
                return new Reduction("CONST", 1); // CONST -> N
            case 19:
                return new Reduction("CONST", 1); // CONST -> T
            case 20:
                return new Reduction("ASSIGN", 3); // ASSIGN -> VNAME input ( )
            case 21:
                return new Reduction("ASSIGN", 3); // ASSIGN -> VNAME = OP
            case 22:
                return new Reduction("CALL", 8); // CALL -> FNAME ( ATOMIC , ATOMIC, ATOMIC )
            case 23:
                return new Reduction("BRANCH", 6); // BRANCH -> if COND then ALGO else ALGO
            case 24:
                return new Reduction("TERM", 1); // TERM -> ATOMIC
            case 25:
                return new Reduction("TERM", 1); // TERM -> CALL
            case 26:
                return new Reduction("TERM", 1); // TERM -> OP
            case 27:
                return new Reduction("OP", 4); // OP -> UNOP (ARG)
            case 28:
                return new Reduction("OP", 6); // OP -> BINOP (ARG , ARG )
            case 29:
                return new Reduction("ARG", 1); // ARG -> ATOMIC
            case 30:
                return new Reduction("ARG", 1); // ARG -> OP
            case 31:
                return new Reduction("COND", 1); // COND -> SIMPLE
            case 32:
                return new Reduction("COND", 1); // COND -> COMPOSIT
            case 33:
                return new Reduction("SIMPLE", 6); // SIMPLE -> BINOP ( ATOMIC , ATOMIC)
            case 34:
                return new Reduction("COMPOSIT", 6); // COMPOSIT -> BINOP ( SIMPLE , SIMPLE)
            case 35:
                return new Reduction("COMPOSIT", 4); // COMPOSIT -> UNOP (SIMPLE)
            case 36:
                return new Reduction("UNOP", 1); // UNOP -> not
            case 37:
                return new Reduction("UNOP", 1); // UNOP -> sqrt
            case 38:
                return new Reduction("BINOP", 1); // BINOP -> or
            case 39:
                return new Reduction("BINOP", 1); // BINOP -> and
            case 40:
                return new Reduction("BINOP", 1); // BINOP -> eq
            case 41:
                return new Reduction("BINOP", 1); // BINOP -> grt
            case 42:
                return new Reduction("BINOP", 1); // BINOP -> add
            case 43:
                return new Reduction("BINOP", 1); // BINOP -> sub
            case 44:
                return new Reduction("BINOP", 1); // BINOP -> mul
            case 45:
                return new Reduction("BINOP", 1); // BINOP -> div
            case 46:
                return new Reduction("FNAME", 1); // FNAME -> F
            case 47:
                return new Reduction("FUNCTIONS", 0); // FUNCTIONS -> ε
            case 48:
                return new Reduction("FUNCTIONS", 2); // FUNCTIONS -> FUNCTIONS DECL
            case 49:
                return new Reduction("DECL", 2); // DECL -> HEADER BODY
            case 50:
                return new Reduction("HEADER", 9); // HEADER -> FTYP FNAME (VNAME , VNAME , VNAME)
            case 51:
                return new Reduction("FTYP", 1); // FTYP -> num
            case 52:
                return new Reduction("FTYP", 1); // FTYP -> void
            case 53:
                return new Reduction("BODY", 6); // BODY -> PROLOG LOCVARS ALGO EPILOG SUBFUNCS end
            case 54:
                return new Reduction("PROLOG", 1); // PROLOG -> {
            case 55:
                return new Reduction("EPILOG", 1); // EPILOG -> }
            case 56:
                return new Reduction("LOCVARS", 9); // LOCVARS -> VTYP VNAME , VTYP VNAME , VTYP VNAME
            case 57:
                return new Reduction("SUBFUNCS", 1); //SUBFUNCS -> FUNCTIONS
            default:
                throw new RuntimeException("Unknown rule number: " + ruleNumber);
        }
    }

    private static class Reduction {
        String lhs;
        int rhsLength;

        Reduction(String lhs, int rhsLength) {
            this.lhs = lhs;
            this.rhsLength = rhsLength;
        }
    }

    private Element createRootNode(Element innerNode) {
        Element root = doc.createElement("ROOT");
        root.appendChild(createUNID());
        root.appendChild(createSymb(innerNode.getAttribute("symbol")));
        root.appendChild(createChildren(innerNode));
        return root;
    }

    private Element createInnerNode(Reduction reduction) {
        Element node = doc.createElement("IN");
        node.setAttribute("symbol", reduction.lhs);
        node.appendChild(createParent());
        node.appendChild(createUNID());
        node.appendChild(createSymb(reduction.lhs));
        node.appendChild(createChildren(reduction.rhsLength));
        return node;
    }

    private Element createLeafNode(Token token) {
        Element leaf = doc.createElement("LEAF");
        leaf.appendChild(createParent());
        leaf.appendChild(createUNID());
        Element terminal = doc.createElement("TERMINAL");
        terminal.setTextContent(token.value);
        leaf.appendChild(terminal);
        return leaf;
    }

    private Element createParent() {
        Element parent = doc.createElement("PARENT");
        parent.setTextContent(String.valueOf(nodeId - 1));
        return parent;
    }

    private Element createUNID() {
        Element unid = doc.createElement("UNID");
        unid.setTextContent(String.valueOf(nodeId++));
        return unid;
    }

    private Element createSymb(String symbol) {
        Element symb = doc.createElement("SYMB");
        symb.setTextContent(symbol);
        return symb;
    }

    private Element createChildren(int count) {
        Element children = doc.createElement("CHILDREN");
        for (int i = 0; i < count; i++) {
            Element id = doc.createElement("ID");
            id.setTextContent(String.valueOf(nodeId - count + i));
            children.appendChild(id);
        }
        return children;
    }

    private Element createChildren(Element node) {
        Element children = doc.createElement("CHILDREN");
        Element id = doc.createElement("ID");
        id.setTextContent(node.getElementsByTagName("UNID").item(0).getTextContent());
        children.appendChild(id);
        return children;
    }

    private void writeOutput(Element root, String outputFile) throws Exception {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(new File(outputFile));
        transformer.transform(source, result);
    }

    public static void main(String[] args) {
        SLRParser parser = new SLRParser();
        parser.parse();
    }
}
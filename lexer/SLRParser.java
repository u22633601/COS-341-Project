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

    public void parse(String inputFile, String outputFile) throws Exception {
        readTokens(inputFile);
        initializeParser();
        Element root = parseTokens();
        writeOutput(root, outputFile);
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
                    throw new Exception("Invalid GOTO state for " + reduction.lhs + " in state " + stack.peek());
                }
                stack.push(gotoState);
                System.out.println("  Reduce using rule " + ruleNumber + ": " + reduction.lhs + " -> "
                        + reduction.rhsLength + " symbols");
                System.out.println("  Goto state " + gotoState);
            } else if (action.equals("acc")) {
                System.out.println("  Accept");
                break;
            } else {
                System.out.println("  Error: Unexpected action");
                throw new Exception("Parsing error at token: " + token.value + " in state " + state);
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
                if (tokenValue.equals("return"))
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
                if (tokenValue.equals("<"))
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
                if (tokenValue.equals("end")) return "s35";
                break;
            case 19:
                if (tokenValue.equals(";")) return "s36";
                break;
            case 20:
            case 21:
                if (tokenValue.equals(";")) return "r10";
                if (tokenValue.equals(";")) return "r11";
                break;
            case 22:
                if (tokenType.equals("text") && tokenValue.startsWith("V"))
                    return "s11";
                if (tokenType.equals("text") && (tokenValue.startsWith("N") || tokenValue.startsWith("T")))
                    return "s40";
                if (tokenType.equals("T"))
                    return "s41";
                break;
            case 23:
            case 24:
            case 25:
                if (tokenValue.equals(";"))
                    return "s36";
                if (tokenValue.equals(";")) return "r13";
                if (tokenValue.equals(";")) return "r14";
                if (tokenValue.equals(";")) return "r15";
                break;
            case 26:
                if (tokenValue.equals("input")) return "s42";
                if (tokenValue.equals("=")) return "s43";
                break;
            case 27:
                if (tokenType.equals("text") && tokenValue.startsWith("V"))
                    return "s11";
                if (tokenType.equals("text") && (tokenValue.startsWith("N") || tokenValue.startsWith("T")))
                    return "s40";
                if (tokenType.equals("T"))
                    return "s41";
                if (tokenValue.equals("("))
                    return "s34";
                break;
            case 28:
                if (tokenValue.equals("not")) return "s50";
                if (tokenValue.equals("sqrt")) return "s51";
                if (tokenValue.equals("(")) return "s48";
                break;
            case 29:
                if (tokenValue.equals("num")) return "s6";
                if (tokenValue.equals("text"))
                    return "s7";
                if (tokenValue.equals("begin"))
                    return "r2";
                break;
            case 30:
                if (tokenValue.equals("$")) return "r48";
                if (tokenValue.equals("}")) return "r48";
                break;
            case 31:
                if (tokenValue.equals("$")) return "r49";
                if (tokenValue.equals("num")) return "r49";
                if (tokenValue.equals("text")) return "r49";
                if (tokenValue.equals("void")) return "r49";
                if (tokenValue.equals("}")) return "r49";
                break;
            case 32:
                if (tokenValue.equals("num")) return "s6";
                if (tokenValue.equals("text")) return "s7";
                break;
            case 33:
                if (tokenValue.equals("num")) return "r54";
                if (tokenValue.equals("text")) return "r54";
                break;
            case 34:
                if (tokenType.equals("text") && tokenValue.startsWith("V"))
                    return "s11";
                break;
            case 35:
                if (tokenValue.equals("$")) return "r7";
                if (tokenValue.equals("num")) return "r7";
                if (tokenValue.equals("text")) return "r7";
                if (tokenValue.equals(";")) return "r7";
                if (tokenValue.equals("void")) return "r7";
                if (tokenValue.equals("}")) return "r7";
                break;
            case 36:
                if (tokenValue.equals("end"))
                    return "r8";
                if (tokenType.equals("text") && tokenValue.startsWith("V")) return "s11";
                if (tokenValue.equals("skip")) return "s20";
                if (tokenValue.equals("halt")) return "s21";
                if (tokenValue.equals("return")) return "s22";
                if (tokenValue.equals("if")) return "s28";
                break;
            case 37:
                if (tokenValue.equals(";")) return "r12";
                break;
            case 40:
                if (tokenValue.equals(","))
                    return "r19"; // CONST -> T
                if (tokenValue.equals(")"))
                    return "r19";
                if (tokenValue.equals(","))
                    return "r16";
                if (tokenValue.equals(";"))
                    return "r16";
                if (tokenValue.equals(")"))
                    return "r16";
                if (tokenValue.equals(","))
                    return "r17";
                if (tokenValue.equals(";"))
                    return "r17";
                if (tokenValue.equals(")"))
                    return "r17";
                if (tokenValue.equals(","))
                    return "r18";
                if (tokenValue.equals(";"))
                    return "r18";
                if (tokenValue.equals(")"))
                    return "r18";
                if (tokenValue.equals(","))
                    return "r19";
                if (tokenValue.equals(";"))
                    return "r19";
                if (tokenValue.equals(")"))
                    return "r19";
                break;
            case 38:
            case 39:
            case 41:
                if (tokenValue.equals(",")) return "r16";
                if (tokenValue.equals(";")) return "r16";
                if (tokenValue.equals(")")) return "r16";
                if (tokenValue.equals(",")) return "r17";
                if (tokenValue.equals(";")) return "r17";
                if (tokenValue.equals(")")) return "r17";
                if (tokenValue.equals(",")) return "r18";
                if (tokenValue.equals(";")) return "r18";
                if (tokenValue.equals(")")) return "r18";
                if (tokenValue.equals(",")) return "r19";
                if (tokenValue.equals(";")) return "r19";
                if (tokenValue.equals(")")) return "r19";
                break;
            case 42:
                if (tokenValue.equals("(")) return "s57";
                break;
            case 43:
                if (tokenType.equals("text") && tokenValue.startsWith("V")) return "s11";
                if (tokenType.equals("text") && (tokenValue.startsWith("N") || tokenValue.startsWith("T")))
                    return "s40";
                if (tokenType.equals("T")) return "s41";
                if (tokenValue.equals("(")) return "s27";
                if (tokenValue.equals("not")) return "s62";
                if (tokenValue.equals("sqrt")) return "s63";
                break;
            case 44:
                if (tokenValue.equals(",")) return "s64";
                break;
            case 45:
                if (tokenValue.equals("then")) return "s65";
                break;
            case 46:
            case 47:
                if (tokenValue.equals("then")) return "r31";
                if (tokenValue.equals("then")) return "r32";
                break;
            case 48:
                if (tokenType.equals("text") && tokenValue.startsWith("V")) return "s11";
                if (tokenType.equals("text") && (tokenValue.startsWith("N") || tokenValue.startsWith("T")))
                    return "s40";
                if (tokenType.equals("T")) return "s41";
                if (tokenValue.equals("not")) return "s68";
                break;
            case 49:
                if (tokenValue.equals(")")) return "s69";
                break;
            case 50:
            case 51:
                if (tokenValue.equals(")")) return "r36";
                if (tokenValue.equals(")")) return "r37";
                break;
            case 52:
                if (tokenValue.equals("begin")) return "r3";
                break;
            case 53:
                if (tokenValue.equals("begin")) return "s9";
                break;
            case 54:
                if (tokenType.equals("text") && tokenValue.startsWith("V")) return "s11";
                break;
            case 55:
                if (tokenValue.equals(",")) return "s72";
                break;
            case 56:
                if (tokenValue.equals("end")) return "r9";
                break;
            case 57:
                if (tokenValue.equals(";")) return "r20";
                break;
            case 58:
                if (tokenValue.equals(";")) return "r21";
                break;
            case 59:
                if (tokenValue.equals(";")) return "r24";
                break;
            case 60:
                if (tokenValue.equals(";")) return "r25";
                break;
            case 61:
                if (tokenValue.equals(";")) return "r26";
                break;
            case 62:
            case 63:
                if (tokenType.equals("text") && tokenValue.startsWith("V")) return "s11";
                if (tokenType.equals("text") && (tokenValue.startsWith("N") || tokenValue.startsWith("T")))
                    return "s40";
                if (tokenType.equals("T")) return "s41";
                if (tokenValue.equals("(")) return "s62";
                if (tokenValue.equals("not")) return "s62";
                if (tokenValue.equals("sqrt")) return "s63";
                break;
            case 64:
                if (tokenType.equals("text") && tokenValue.startsWith("V")) return "s11";
                if (tokenType.equals("text") && (tokenValue.startsWith("N") || tokenValue.startsWith("T")))
                    return "s40";
                if (tokenType.equals("T"))
                    return "s41";
                if (tokenValue.equals(")"))
                    return "s88";
                break;
            case 65:
                if (tokenValue.equals("begin")) return "s9";
                break;
            case 66:
                if (tokenValue.equals(",")) return "s79";
                break;
            case 67:
                if (tokenValue.equals(",")) return "s80";
                break;
            case 68:
                if (tokenType.equals("text") && tokenValue.startsWith("V")) return "s11";
                if (tokenType.equals("text") && (tokenValue.startsWith("N") || tokenValue.startsWith("T")))
                    return "s40";
                if (tokenType.equals("T")) return "s41";
                break;
            case 69:
                if (tokenValue.equals("not")) return "s68";
                break;
            case 70:
                if (tokenValue.equals("{")) return "s83";
                break;
            case 71:
                if (tokenValue.equals(",")) return "s84";
                break;
            case 72:
                if (tokenType.equals("text") && tokenValue.startsWith("V")) return "s11";
                break;
            case 73:
                if (tokenValue.equals(")")) return "s86";
                break;
            case 74:
            case 75:
                if (tokenValue.equals(",")) return "r29";
                if (tokenValue.equals(";")) return "r29";
                if (tokenValue.equals(")")) return "r29";
                if (tokenValue.equals(",")) return "r30";
                if (tokenValue.equals(";")) return "r30";
                if (tokenValue.equals(")")) return "r30";
                break;
            case 76:
                if (tokenValue.equals(",")) return "s87";
                break;
            case 77:
                if (tokenValue.equals(",")) return "s88";
                break;
            case 78:
                if (tokenValue.equals("else"))
                    return "s89";
                if (tokenValue.equals(","))
                    return "s87"; // Comma between function arguments
                if (tokenValue.equals(")"))
                    return "r27";
                break;
            case 79:
                if (tokenType.equals("text") && tokenValue.startsWith("V")) return "s11";
                if (tokenType.equals("text") && (tokenValue.startsWith("N") || tokenValue.startsWith("T")))
                    return "s40";
                if (tokenType.equals("T"))
                    return "s41";
                if (tokenValue.equals(","))
                    return "s87"; // Comma between function arguments
                if (tokenValue.equals(")"))
                    return "r27";
                break;
            case 80:
                if (tokenValue.equals("not")) return "s68";
                break;
            case 81:
                if (tokenValue.equals(")")) return "s92";
                break;
            case 82:
                if (tokenValue.equals("$")) return "r47";
                if (tokenValue.equals("num")) return "s16";
                if (tokenValue.equals("void")) return "s17";
                if (tokenValue.equals("}")) return "r47";
                break;
            case 83:
                if (tokenValue.equals("num")) return "r55";
                if (tokenValue.equals("text")) return "r55";
                if (tokenValue.equals("$")) return "r55";
                if (tokenValue.equals("void")) return "r55";
                if (tokenValue.equals("}")) return "r55";
                break;
            case 84:
                if (tokenValue.equals("num")) return "s6";
                if (tokenValue.equals("text")) return "s7";
                break;
            case 85:
                if (tokenValue.equals(",")) return "s96";
                break;
            case 86:
                if (tokenValue.equals(",")) return "r27";
                if (tokenValue.equals(";")) return "r27";
                if (tokenValue.equals(")")) return "r27";
                break;
            case 87:
                if (tokenType.equals("text") && tokenValue.startsWith("V")) return "s11";
                if (tokenType.equals("text") && (tokenValue.startsWith("N") || tokenValue.startsWith("T")))
                    return "s40";
                if (tokenType.equals("T")) return "s41";
                if (tokenValue.equals("(")) return "s62";
                if (tokenValue.equals("not")) return "s62";
                if (tokenValue.equals("sqrt")) return "s63";
                break;
            case 88:
                if (tokenType.equals("text") && tokenValue.startsWith("V")) return "s11";
                if (tokenType.equals("text") && (tokenValue.startsWith("N") || tokenValue.startsWith("T")))
                    return "s40";
                if (tokenType.equals("T"))
                    return "s41";
                if (tokenValue.equals(";"))
                    return "r22";
                break;
            case 89:
                if (tokenValue.equals("begin")) return "s9";
                break;
            case 90:
                if (tokenValue.equals(")")) return "s100";
                break;
            case 91:
                if (tokenValue.equals(")")) return "s101";
                break;
            case 92:
                if (tokenValue.equals("then")) return "r35";
                break;
            case 93:
                if (tokenValue.equals("}")) return "s102";
                break;
            case 94:
                if (tokenValue.equals("}")) return "r57";
                break;
            case 95:
                if (tokenType.equals("text") && tokenValue.startsWith("V"))
                    return "s11";
                break;
            case 96:
                if (tokenType.equals("text") && tokenValue.startsWith("V"))
                    return "s11";
                break;
            case 97:
                if (tokenValue.equals(","))
                    return "s87"; // Comma between function arguments
                if (tokenValue.equals(")"))
                    return "r27";
                if (tokenValue.equals(")"))
                    return "s105";
                break;
            case 98:
                if (tokenValue.equals(")"))
                    return "s106";
                break;
            case 99:
                if (tokenValue.equals(";"))
                    return "r23";
                break;
            case 100:
                if (tokenValue.equals(","))
                    return "r33";
                if (tokenValue.equals(";"))
                    return "r33";
                if (tokenValue.equals(")"))
                    return "r33";
                if (tokenValue.equals("then"))
                    return "r33";
                break;
            case 101:
                if (tokenValue.equals("then"))
                    return "r34";
                break;
            case 102:
                if (tokenValue.equals("$"))
                    return "r53";
                if (tokenValue.equals("num"))
                    return "r53";
                if (tokenValue.equals("text"))
                    return "r53";
                if (tokenValue.equals("void"))
                    return "r53";
                if (tokenValue.equals("}"))
                    return "r53";
                break;
            case 103:
                if (tokenValue.equals(","))
                    return "s107";
                break;
            case 104:
                if (tokenValue.equals(")"))
                    return "s108";
                break;
            case 105:
                if (tokenValue.equals(","))
                    return "r28";
                if (tokenValue.equals(";"))
                    return "r28";
                if (tokenValue.equals(")"))
                    return "r28";
                break;
            case 106:
                if (tokenValue.equals(";"))
                    return "r22";
                break;
            case 107:
                if (tokenValue.equals("num"))
                    return "s6";
                if (tokenValue.equals("text"))
                    return "s7";
                break;
            case 108:
                if (tokenValue.equals("{"))
                    return "r50";
                break;
            case 109:
                if (tokenType.equals("text") && tokenValue.startsWith("V"))
                    return "s11";
                break;
            case 110:
                if (tokenValue.equals(","))
                    return "s111";
                break;
            case 111:
                if (tokenValue.equals("}"))
                    return "r56";
                break;
            default:
                break;
        }

        // If no action is found, return an error
        return "error";
    }

    private int getGotoState(int state, String nonTerminal) {
        switch (state) {
            case 0:
                if (nonTerminal.equals("S"))
                    return 1;
                if (nonTerminal.equals("PROG"))
                    return 1;
                if (nonTerminal.equals("GLOBVARS"))
                    return 4;
                break;
            case 2:
                if (nonTerminal.equals("GLOBVARS"))
                    return 4;
                if (nonTerminal.equals("VTYP"))
                    return 5;
                if (nonTerminal.equals("PROG"))
                    return 1;
                break;
            case 4:
                if (nonTerminal.equals("ALGO"))
                    return 8;
                if (nonTerminal.equals("PROG"))
                    return 1;
                break;
            case 5:
                if (nonTerminal.equals("VNAME"))
                    return 10;
                if (nonTerminal.equals("GLOBVARS"))
                    return 52;
                break;
            case 6:
            case 7:
                if (nonTerminal.equals("VNAME"))
                    return 10;
                if (nonTerminal.equals("GLOBVARS"))
                    return 4;
                if (nonTerminal.equals("PROG"))
                    return 1;
                break;
            case 8:
                if (nonTerminal.equals("FUNCTIONS"))
                    return 12;
                if (nonTerminal.equals("DECL"))
                    return 13;
                if (nonTerminal.equals("HEADER"))
                    return 14;
                if (nonTerminal.equals("FNAME"))
                    return 15;
                if (nonTerminal.equals("ALGO"))
                    return 70;
                if (nonTerminal.equals("PROG"))
                    return 1;
                break;
            case 9:
                if (nonTerminal.equals("INSTRUC"))
                    return 18;
                if (nonTerminal.equals("COMMAND"))
                    return 19;
                if (nonTerminal.equals("ATOMIC"))
                    return 23;
                if (nonTerminal.equals("ASSIGN"))
                    return 24;
                if (nonTerminal.equals("CALL"))
                    return 25;
                if (nonTerminal.equals("BRANCH"))
                    return 26;
                if (nonTerminal.equals("FNAME"))
                    return 27;
                if (nonTerminal.equals("ARG"))
                    return 64;
                if (nonTerminal.equals("VNAME"))
                    return 78;
                if (nonTerminal.equals("CONST"))
                    return 79;
                if (nonTerminal.equals("TERM"))
                    return 97;
                break;
            case 10:
                if (nonTerminal.equals("GLOBVARS"))
                    return 52;
                break;
            case 12:
                if (nonTerminal.equals("PROG"))
                    return 1;
                break;
            case 13:
                if (nonTerminal.equals("FUNCTIONS"))
                    return 30;
                if (nonTerminal.equals("DECL"))
                    return 13;
                if (nonTerminal.equals("HEADER"))
                    return 14;
                if (nonTerminal.equals("FNAME"))
                    return 15;
                break;
            case 19:
                if (nonTerminal.equals("INSTRUC"))
                    return 56;
                break;
            case 22:
                if (nonTerminal.equals("TERM"))
                    return 37;
                if (nonTerminal.equals("CONST"))
                    return 38;
                if (nonTerminal.equals("VNAME"))
                    return 39;
                break;
            case 27:
                if (nonTerminal.equals("TERM"))
                    return 44;
                if (nonTerminal.equals("CONST"))
                    return 38;
                if (nonTerminal.equals("VNAME"))
                    return 39;
                break;
            case 28:
                if (nonTerminal.equals("COND"))
                    return 45;
                if (nonTerminal.equals("SIMPLE"))
                    return 46;
                if (nonTerminal.equals("COMPOSIT"))
                    return 47;
                if (nonTerminal.equals("UNOP"))
                    return 49;
                break;
            case 29:
                if (nonTerminal.equals("GLOBVARS"))
                    return 52;
                if (nonTerminal.equals("VTYP"))
                    return 5;
                break;
            case 32:
                if (nonTerminal.equals("VTYP"))
                    return 54;
                break;
            case 34:
                if (nonTerminal.equals("ARG"))
                    return 64;
                if (nonTerminal.equals("ATOMIC"))
                    return 77;
                if (nonTerminal.equals("VNAME"))
                    return 78;
                if (nonTerminal.equals("CONST"))
                    return 79;
                if (nonTerminal.equals("TERM"))
                    return 97;
                if (nonTerminal.equals("CALL"))
                    return 25;
                break;
            case 36:
                if (nonTerminal.equals("INSTRUC"))
                    return 56;
                if (nonTerminal.equals("COMMAND"))
                    return 19;
                if (nonTerminal.equals("ATOMIC"))
                    return 23;
                if (nonTerminal.equals("ASSIGN"))
                    return 24;
                if (nonTerminal.equals("CALL"))
                    return 25;
                if (nonTerminal.equals("BRANCH"))
                    return 26;
                break;
            case 43:
                if (nonTerminal.equals("TERM"))
                    return 59;
                if (nonTerminal.equals("CONST"))
                    return 38;
                if (nonTerminal.equals("VNAME"))
                    return 39;
                if (nonTerminal.equals("OP"))
                    return 58;
                if (nonTerminal.equals("UNOP"))
                    return 60;
                if (nonTerminal.equals("BINOP"))
                    return 61;
                break;
            case 48:
                if (nonTerminal.equals("COND"))
                    return 66;
                if (nonTerminal.equals("SIMPLE"))
                    return 46;
                if (nonTerminal.equals("COMPOSIT"))
                    return 47;
                if (nonTerminal.equals("TERM"))
                    return 67;
                if (nonTerminal.equals("CONST"))
                    return 38;
                if (nonTerminal.equals("VNAME"))
                    return 39;
                break;
            case 53:
                if (nonTerminal.equals("ALGO"))
                    return 70;
                break;
            case 62:
            case 63:
                if (nonTerminal.equals("TERM"))
                    return 74;
                if (nonTerminal.equals("CONST"))
                    return 38;
                if (nonTerminal.equals("VNAME"))
                    return 39;
                if (nonTerminal.equals("OP"))
                    return 73;
                if (nonTerminal.equals("UNOP"))
                    return 60;
                if (nonTerminal.equals("BINOP"))
                    return 61;
                break;
            case 64:
                if (nonTerminal.equals("TERM"))
                    return 77;
                if (nonTerminal.equals("CONST"))
                    return 38;
                if (nonTerminal.equals("VNAME"))
                    return 39;
                if (nonTerminal.equals("ARG"))
                    return 64;
                if (nonTerminal.equals("ATOMIC"))
                    return 77;
                if (nonTerminal.equals("CALL"))
                    return 25;
                break;
            case 65:
                if (nonTerminal.equals("ALGO"))
                    return 78;
                break;
            case 68:
                if (nonTerminal.equals("TERM"))
                    return 90;
                if (nonTerminal.equals("CONST"))
                    return 38;
                if (nonTerminal.equals("VNAME"))
                    return 39;
                break;
            case 69:
                if (nonTerminal.equals("COND"))
                    return 91;
                if (nonTerminal.equals("SIMPLE"))
                    return 46;
                if (nonTerminal.equals("COMPOSIT"))
                    return 47;
                break;
            case 79:
                if (nonTerminal.equals("TERM"))
                    return 98;
                if (nonTerminal.equals("CONST"))
                    return 38;
                if (nonTerminal.equals("VNAME"))
                    return 39;
                break;
            case 80:
                if (nonTerminal.equals("COND"))
                    return 91;
                if (nonTerminal.equals("SIMPLE"))
                    return 46;
                if (nonTerminal.equals("COMPOSIT"))
                    return 47;
                break;
            case 82:
                if (nonTerminal.equals("FUNCTIONS"))
                    return 94;
                if (nonTerminal.equals("DECL"))
                    return 13;
                if (nonTerminal.equals("HEADER"))
                    return 14;
                if (nonTerminal.equals("FNAME"))
                    return 15;
                break;
            case 84:
                if (nonTerminal.equals("VTYP"))
                    return 95;
                break;
            case 87:
                if (nonTerminal.equals("TERM"))
                    return 97;
                if (nonTerminal.equals("CONST"))
                    return 38;
                if (nonTerminal.equals("VNAME"))
                    return 39;
                if (nonTerminal.equals("OP"))
                    return 73;
                if (nonTerminal.equals("UNOP"))
                    return 60;
                if (nonTerminal.equals("BINOP"))
                    return 61;
                if (nonTerminal.equals("ARG"))
                    return 64;
                if (nonTerminal.equals("ATOMIC"))
                    return 77;
                if (nonTerminal.equals("CALL"))
                    return 25;
                break;
            case 88:
                if (nonTerminal.equals("TERM"))
                    return 98;
                if (nonTerminal.equals("CONST"))
                    return 38;
                if (nonTerminal.equals("VNAME"))
                    return 39;
                break;
            case 89:
                if (nonTerminal.equals("ALGO"))
                    return 99;
                break;
            case 95:
                if (nonTerminal.equals("VNAME"))
                    return 103;
                break;
            case 96:
                if (nonTerminal.equals("VNAME"))
                    return 104;
                break;
            case 107:
                if (nonTerminal.equals("VTYP"))
                    return 109;
                break;
            case 109:
                if (nonTerminal.equals("VNAME"))
                    return 110;
                break;
            default:
                break;

        }

        // If no GOTO state is found, return an error state
        return -1;
    }

    private Reduction reduce(int ruleNumber) {
        switch (ruleNumber) {
            case 1:
                return new Reduction("PROG", 3); // PROG -> main GLOBVARS ALGO
            case 2:
                return new Reduction("GLOBVARS", 0); // GLOBVARS -> ε
            case 3:
                return new Reduction("GLOBVARS", 3); // GLOBVARS -> GLOBVARS VTYP VNAME ,
            case 4:
                return new Reduction("VTYP", 1); // VTYP -> num
            case 5:
                return new Reduction("VTYP", 1); // VTYP -> text
            case 6:
                return new Reduction("VNAME", 1); // VNAME -> variable
            case 7:
                return new Reduction("ALGO", 3); // ALGO -> begin INSTRUC end
            case 8:
                return new Reduction("INSTRUC", 0); // INSTRUC -> ε
            case 9:
                return new Reduction("INSTRUC", 2); // INSTRUC -> INSTRUC COMMAND ;
            case 10:
                return new Reduction("COMMAND", 1); // COMMAND -> skip //here
            case 11:
                return new Reduction("COMMAND", 1); // COMMAND -> halt
            case 12:
                return new Reduction("COMMAND", 1); // COMMAND -> ATOMIC
            case 13:
                return new Reduction("ATOMIC", 1); // ATOMIC -> ASSIGN
            case 14:
                return new Reduction("ATOMIC", 1); // ATOMIC -> CALL
            case 15:
                return new Reduction("ATOMIC", 1); // ATOMIC -> BRANCH
            case 16:
                return new Reduction("TERM", 1); // TERM -> CONST
            case 17:
                return new Reduction("TERM", 1); // TERM -> VNAME
            case 18:
                return new Reduction("CONST", 1); // CONST -> N
            case 19:
                return new Reduction("CONST", 1); // CONST -> T
            case 20:
                return new Reduction("ASSIGN", 3); // ASSIGN -> VNAME input ( )
            case 21:
                return new Reduction("ASSIGN", 3); // ASSIGN -> VNAME = OP
            case 22:
                return new Reduction("CALL", 4); // CALL -> FNAME ( ARG )
            case 23:
                return new Reduction("BRANCH", 5); // BRANCH -> if COND then ALGO else ALGO
            case 24:
                return new Reduction("OP", 1); // OP -> TERM
            case 25:
                return new Reduction("OP", 1); // OP -> UNOP
            case 26:
                return new Reduction("OP", 1); // OP -> BINOP
            case 27:
                return new Reduction("ARG", 1); // ARG -> TERM
            case 28:
                return new Reduction("ARG", 3); // ARG -> ARG , TERM
            case 29:
                return new Reduction("UNOP", 4); // UNOP -> not ( TERM )
            case 30:
                return new Reduction("UNOP", 4); // UNOP -> sqrt ( TERM )
            case 31:
                return new Reduction("COND", 1); // COND -> SIMPLE
            case 32:
                return new Reduction("COND", 1); // COND -> COMPOSIT
            case 33:
                return new Reduction("SIMPLE", 3); // SIMPLE -> TERM < TERM
            case 34:
                return new Reduction("COMPOSIT", 3); // COMPOSIT -> ( COND )
            case 35:
                return new Reduction("COMPOSIT", 3); // COMPOSIT -> not COND
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
                return new Reduction("HEADER", 4); // HEADER -> FNAME ( FTYP )
            case 51:
                return new Reduction("FTYP", 1); // FTYP -> num
            case 52:
                return new Reduction("FTYP", 1); // FTYP -> void
            case 53:
                return new Reduction("BODY", 3); // BODY -> { PROLOG EPILOG }
            case 54:
                return new Reduction("PROLOG", 0); // PROLOG -> ε
            case 55:
                return new Reduction("PROLOG", 2); // PROLOG -> PROLOG LOCVARS
            case 56:
                return new Reduction("LOCVARS", 3); // LOCVARS -> VTYP VNAME ,
            case 57:
                return new Reduction("EPILOG", 1); // EPILOG -> SUBFUNCS
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
        if (args.length != 2) {
            System.out.println("Usage: java SLRParser <input_file> <output_file>");
            return;
        }

        try {
            SLRParser parser = new SLRParser();
            parser.parse(args[0], args[1]);
            System.out.println("Parsing completed successfully. Output written to " + args[1]);
        } catch (Exception e) {
            System.err.println("Error during parsing: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
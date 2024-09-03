
import java.io.*;
import java.util.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import org.w3c.dom.*;

public class Parser {
    private List<Token> tokens;
    private int currentTokenIndex;
    private Stack<Integer> stateStack;
    private Stack<Node> nodeStack;
    private Map<Integer, Map<String, Action>> actionTable;
    private Map<Integer, Map<String, Integer>> gotoTable;
    private int nextNodeId;

    public Parser() {
        tokens = new ArrayList<>();
        stateStack = new Stack<>();
        nodeStack = new Stack<>();
        actionTable = new HashMap<>();
        gotoTable = new HashMap<>();
        nextNodeId = 1;
        initializeTables();
    }

    private void initializeTables() {
        // Initialize action table
        for (int i = 0; i < 200; i++) { // Assume we have up to 100 states
            actionTable.put(i, new HashMap<>());
        }

        // Initialize goto table
        for (int i = 0; i < 200; i++) {
            gotoTable.put(i, new HashMap<>());
        }

        // Fill in the action table
        // Action table entries
        actionTable.get(0).put("main", new Action(ActionType.SHIFT, 2));
        //actionTable.get(2).put(",", new Action(ActionType.SHIFT, 4));
        actionTable.get(2).put("num", new Action(ActionType.SHIFT, 6));
        actionTable.get(2).put("text", new Action(ActionType.SHIFT, 7));
        actionTable.get(2).put("VARIABLE", new Action(ActionType.SHIFT, 7));
        actionTable.get(2).put("begin", new Action(ActionType.REDUCE, 2));///////////////////////
        actionTable.get(3).put("end", new Action(ActionType.SHIFT, 9));
        actionTable.get(4).put("begin", new Action(ActionType.REDUCE, 2));
        actionTable.get(5).put("begin", new Action(ActionType.SHIFT, 11));
        actionTable.get(6).put("begin", new Action(ActionType.REDUCE, 4));
        actionTable.get(7).put("begin", new Action(ActionType.REDUCE, 5));
        //actionTable.get(8).put(",", new Action(ActionType.SHIFT, 13));
        actionTable.get(8).put("num", new Action(ActionType.SHIFT, 17));
        actionTable.get(8).put("void", new Action(ActionType.SHIFT, 18));
        //actionTable.get(9).put(",", new Action(ActionType.SHIFT, 20));
        actionTable.get(9).put("begin", new Action(ActionType.SHIFT, 11));
        actionTable.get(9).put("skip", new Action(ActionType.SHIFT, 22));
        actionTable.get(9).put("halt", new Action(ActionType.SHIFT, 23));
        actionTable.get(9).put("return", new Action(ActionType.SHIFT, 24));
        actionTable.get(9).put("VARIABLE", new Action(ActionType.SHIFT, 28));
        actionTable.get(9).put("if", new Action(ActionType.SHIFT, 30));
        actionTable.get(9).put("FUNCTION", new Action(ActionType.SHIFT, 29));
        actionTable.get(10).put("num", new Action(ActionType.SHIFT, 31));
        actionTable.get(11).put("end", new Action(ActionType.REDUCE, 6));
        actionTable.get(11).put("skip", new Action(ActionType.REDUCE, 6));
        actionTable.get(11).put("halt", new Action(ActionType.REDUCE, 6));
        actionTable.get(11).put("return", new Action(ActionType.REDUCE, 6));
        actionTable.get(11).put("VARIABLE", new Action(ActionType.REDUCE, 6));
        actionTable.get(11).put("if", new Action(ActionType.REDUCE, 6));
        actionTable.get(11).put("FUNCTION", new Action(ActionType.REDUCE, 6));
        actionTable.get(12).put("$", new Action(ActionType.REDUCE, 1));
        actionTable.get(13).put("end", new Action(ActionType.REDUCE, 47));
        actionTable.get(13).put("}", new Action(ActionType.REDUCE, 47));
       // actionTable.get(14).put(",", new Action(ActionType.SHIFT, 13));
        actionTable.get(14).put("num", new Action(ActionType.SHIFT, 17));
        actionTable.get(14).put("void", new Action(ActionType.SHIFT, 18));
        actionTable.get(15).put("end", new Action(ActionType.SHIFT, 35));
        actionTable.get(15).put("}", new Action(ActionType.SHIFT, 34));
        actionTable.get(16).put(")", new Action(ActionType.SHIFT, 36));
        actionTable.get(17).put(")", new Action(ActionType.REDUCE, 51));
        actionTable.get(18).put(")", new Action(ActionType.REDUCE, 52));
        actionTable.get(19).put(";", new Action(ActionType.SHIFT, 37));
        actionTable.get(20).put("end", new Action(ActionType.REDUCE, 8));
        actionTable.get(21).put(";", new Action(ActionType.SHIFT, 38));
        actionTable.get(22).put(";", new Action(ActionType.REDUCE, 10));
        actionTable.get(23).put(";", new Action(ActionType.REDUCE, 11));
        actionTable.get(24).put("VARIABLE", new Action(ActionType.SHIFT, 40));
        actionTable.get(24).put("NUMBER", new Action(ActionType.SHIFT, 42));
        actionTable.get(24).put("TEXT", new Action(ActionType.SHIFT, 43));
        actionTable.get(25).put(";", new Action(ActionType.REDUCE, 13));
        actionTable.get(26).put(";", new Action(ActionType.REDUCE, 14));
        actionTable.get(27).put(";", new Action(ActionType.REDUCE, 15));
        actionTable.get(28).put("<", new Action(ActionType.SHIFT, 44));
        actionTable.get(28).put("=", new Action(ActionType.SHIFT, 45));
        actionTable.get(29).put("VARIABLE", new Action(ActionType.SHIFT, 40));
        actionTable.get(29).put("NUMBER", new Action(ActionType.SHIFT, 42));
        actionTable.get(29).put("TEXT", new Action(ActionType.SHIFT, 43));
        actionTable.get(30).put("VARIABLE", new Action(ActionType.SHIFT, 40));
        actionTable.get(30).put("NUMBER", new Action(ActionType.SHIFT, 42));
        actionTable.get(30).put("TEXT", new Action(ActionType.SHIFT, 43));
        actionTable.get(30).put("not", new Action(ActionType.SHIFT, 52));
        actionTable.get(30).put("sqrt", new Action(ActionType.SHIFT, 53));
        actionTable.get(31).put(",", new Action(ActionType.SHIFT, 4));
        actionTable.get(31).put("num", new Action(ActionType.SHIFT, 6));
        actionTable.get(31).put("text", new Action(ActionType.SHIFT, 7));
        actionTable.get(31).put("VARIABLE", new Action(ActionType.SHIFT, 7));
        actionTable.get(32).put("end", new Action(ActionType.REDUCE, 48));
        actionTable.get(32).put("}", new Action(ActionType.REDUCE, 48));
        actionTable.get(33).put("end", new Action(ActionType.REDUCE, 49));
        actionTable.get(33).put("num", new Action(ActionType.REDUCE, 49));
        actionTable.get(33).put("void", new Action(ActionType.REDUCE, 49));
        actionTable.get(33).put("}", new Action(ActionType.REDUCE, 49));
        actionTable.get(34).put("num", new Action(ActionType.SHIFT, 6));
        actionTable.get(34).put("text", new Action(ActionType.SHIFT, 7));
        actionTable.get(34).put("VARIABLE", new Action(ActionType.SHIFT, 7));
        actionTable.get(35).put("num", new Action(ActionType.REDUCE, 54));
        actionTable.get(35).put("text", new Action(ActionType.REDUCE, 54));
        actionTable.get(35).put("VARIABLE", new Action(ActionType.REDUCE, 54));
        actionTable.get(36).put("num", new Action(ActionType.SHIFT, 6));
        actionTable.get(36).put("text", new Action(ActionType.SHIFT, 7));
        actionTable.get(36).put("VARIABLE", new Action(ActionType.SHIFT, 7));
        actionTable.get(37).put("end", new Action(ActionType.REDUCE, 7));
        actionTable.get(37).put("skip", new Action(ActionType.REDUCE, 7));
        actionTable.get(37).put("halt", new Action(ActionType.REDUCE, 7));
        actionTable.get(37).put("return", new Action(ActionType.REDUCE, 7));
        actionTable.get(37).put("VARIABLE", new Action(ActionType.REDUCE, 7));
        actionTable.get(37).put("if", new Action(ActionType.REDUCE, 7));
        actionTable.get(37).put("FUNCTION", new Action(ActionType.REDUCE, 7));
        actionTable.get(37).put("}", new Action(ActionType.REDUCE, 7));
        actionTable.get(38).put(",", new Action(ActionType.SHIFT, 20));
        actionTable.get(38).put("begin", new Action(ActionType.SHIFT, 11));
        actionTable.get(38).put("skip", new Action(ActionType.SHIFT, 22));
        actionTable.get(38).put("halt", new Action(ActionType.SHIFT, 23));
        actionTable.get(38).put("return", new Action(ActionType.SHIFT, 24));
        actionTable.get(38).put("VARIABLE", new Action(ActionType.SHIFT, 28));
        actionTable.get(38).put("if", new Action(ActionType.SHIFT, 30));
        actionTable.get(38).put("FUNCTION", new Action(ActionType.SHIFT, 29));
        actionTable.get(39).put(";", new Action(ActionType.REDUCE, 12));
        actionTable.get(40).put("end", new Action(ActionType.REDUCE, 16));
        actionTable.get(40).put("skip", new Action(ActionType.REDUCE, 16));
        actionTable.get(40).put("halt", new Action(ActionType.REDUCE, 16));
        actionTable.get(40).put("return", new Action(ActionType.REDUCE, 16));
        actionTable.get(40).put("VARIABLE", new Action(ActionType.REDUCE, 16));
        actionTable.get(40).put("if", new Action(ActionType.REDUCE, 16));
        actionTable.get(40).put("else", new Action(ActionType.REDUCE, 16));
        actionTable.get(41).put("end", new Action(ActionType.REDUCE, 17));
        actionTable.get(41).put("skip", new Action(ActionType.REDUCE, 17));
        actionTable.get(41).put("halt", new Action(ActionType.REDUCE, 17));
        actionTable.get(41).put("return", new Action(ActionType.REDUCE, 17));
        actionTable.get(41).put("VARIABLE", new Action(ActionType.REDUCE, 17));
        actionTable.get(41).put("if", new Action(ActionType.REDUCE, 17));
        actionTable.get(41).put("else", new Action(ActionType.REDUCE, 17));
        actionTable.get(42).put("end", new Action(ActionType.REDUCE, 18));
        actionTable.get(42).put("skip", new Action(ActionType.REDUCE, 18));
        actionTable.get(42).put("halt", new Action(ActionType.REDUCE, 18));
        actionTable.get(42).put("return", new Action(ActionType.REDUCE, 18));
        actionTable.get(42).put("VARIABLE", new Action(ActionType.REDUCE, 18));
        actionTable.get(42).put("if", new Action(ActionType.REDUCE, 18));
        actionTable.get(42).put("else", new Action(ActionType.REDUCE, 18));
        actionTable.get(43).put("end", new Action(ActionType.REDUCE, 19));
        actionTable.get(43).put("skip", new Action(ActionType.REDUCE, 19));
        actionTable.get(43).put("halt", new Action(ActionType.REDUCE, 19));
        actionTable.get(43).put("return", new Action(ActionType.REDUCE, 19));
        actionTable.get(43).put("VARIABLE", new Action(ActionType.REDUCE, 19));
        actionTable.get(43).put("if", new Action(ActionType.REDUCE, 19));
        actionTable.get(43).put("else", new Action(ActionType.REDUCE, 19));
        actionTable.get(44).put("input", new Action(ActionType.SHIFT, 59));
        actionTable.get(45).put("VARIABLE", new Action(ActionType.SHIFT, 40));
        actionTable.get(45).put("NUMBER", new Action(ActionType.SHIFT, 42));
        actionTable.get(45).put("TEXT", new Action(ActionType.SHIFT, 43));
        actionTable.get(45).put("FUNCTION", new Action(ActionType.SHIFT, 29));
        actionTable.get(45).put("UNOP(", new Action(ActionType.SHIFT, 64));
        actionTable.get(45).put("BINOP(", new Action(ActionType.SHIFT, 65));
        actionTable.get(46).put("then", new Action(ActionType.SHIFT, 81));
        actionTable.get(47).put("else", new Action(ActionType.SHIFT, 82));
        actionTable.get(48).put("else", new Action(ActionType.REDUCE, 31));
        actionTable.get(49).put("else", new Action(ActionType.REDUCE, 32));
        actionTable.get(50).put("VARIABLE", new Action(ActionType.SHIFT, 40));
        actionTable.get(50).put("NUMBER", new Action(ActionType.SHIFT, 42));
        actionTable.get(50).put("TEXT", new Action(ActionType.SHIFT, 43));
        actionTable.get(51).put("(", new Action(ActionType.SHIFT, 71));
        actionTable.get(52).put("(", new Action(ActionType.REDUCE, 36));
        actionTable.get(53).put("(", new Action(ActionType.REDUCE, 37));
        actionTable.get(54).put("begin", new Action(ActionType.REDUCE, 3));
        actionTable.get(55).put("end", new Action(ActionType.SHIFT, 9));
        actionTable.get(56).put("num", new Action(ActionType.SHIFT, 6));
        actionTable.get(56).put("text", new Action(ActionType.SHIFT, 7));
        actionTable.get(56).put("VARIABLE", new Action(ActionType.SHIFT, 7));
        actionTable.get(57).put("num", new Action(ActionType.SHIFT, 74));
        actionTable.get(58).put(";", new Action(ActionType.REDUCE, 9));
        actionTable.get(59).put(";", new Action(ActionType.REDUCE, 20));
        actionTable.get(60).put(";", new Action(ActionType.REDUCE, 21));
        actionTable.get(61).put(";", new Action(ActionType.REDUCE, 24));
        actionTable.get(62).put(";", new Action(ActionType.REDUCE, 25));
        actionTable.get(63).put(";", new Action(ActionType.REDUCE, 26));
        actionTable.get(64).put("VARIABLE", new Action(ActionType.SHIFT, 40));
        actionTable.get(64).put("NUMBER", new Action(ActionType.SHIFT, 42));
        actionTable.get(64).put("TEXT", new Action(ActionType.SHIFT, 43));
        actionTable.get(64).put("UNOP(", new Action(ActionType.SHIFT, 64));
        actionTable.get(64).put("BINOP(", new Action(ActionType.SHIFT, 65));
        actionTable.get(65).put("VARIABLE", new Action(ActionType.SHIFT, 40));
        actionTable.get(65).put("NUMBER", new Action(ActionType.SHIFT, 42));
        actionTable.get(65).put("TEXT", new Action(ActionType.SHIFT, 43));
        actionTable.get(65).put("UNOP(", new Action(ActionType.SHIFT, 64));
        actionTable.get(65).put("BINOP(", new Action(ActionType.SHIFT, 65));
        actionTable.get(66).put("VARIABLE", new Action(ActionType.SHIFT, 40));
        actionTable.get(66).put("NUMBER", new Action(ActionType.SHIFT, 42));
        actionTable.get(66).put("TEXT", new Action(ActionType.SHIFT, 43));
        actionTable.get(67).put("end", new Action(ActionType.SHIFT, 9));
        actionTable.get(68).put("then", new Action(ActionType.SHIFT, 81));
        actionTable.get(69).put("then", new Action(ActionType.SHIFT, 82));
        actionTable.get(70).put("VARIABLE", new Action(ActionType.SHIFT, 40));
        actionTable.get(70).put("NUMBER", new Action(ActionType.SHIFT, 42));
        actionTable.get(70).put("TEXT", new Action(ActionType.SHIFT, 43));
        actionTable.get(71).put("VARIABLE", new Action(ActionType.SHIFT, 40));
        actionTable.get(71).put("NUMBER", new Action(ActionType.SHIFT, 42));
        actionTable.get(71).put("TEXT", new Action(ActionType.SHIFT, 43));
        actionTable.get(71).put("not", new Action(ActionType.SHIFT, 52));
        actionTable.get(71).put("sqrt", new Action(ActionType.SHIFT, 53));
        actionTable.get(72).put("end", new Action(ActionType.SHIFT, 85));
        actionTable.get(72).put("}", new Action(ActionType.SHIFT, 85));
        actionTable.get(73).put("num", new Action(ActionType.SHIFT, 86));
        actionTable.get(74).put("num", new Action(ActionType.SHIFT, 6));
        actionTable.get(74).put("text", new Action(ActionType.SHIFT, 7));
        actionTable.get(74).put("VARIABLE", new Action(ActionType.SHIFT, 7));
        actionTable.get(75).put(")", new Action(ActionType.SHIFT, 88));
        actionTable.get(76).put("end", new Action(ActionType.REDUCE, 29));
        actionTable.get(76).put("skip", new Action(ActionType.REDUCE, 29));
        actionTable.get(76).put("halt", new Action(ActionType.REDUCE, 29));
        actionTable.get(76).put("return", new Action(ActionType.REDUCE, 29));
        actionTable.get(76).put("VARIABLE", new Action(ActionType.REDUCE, 29));
        actionTable.get(76).put("if", new Action(ActionType.REDUCE, 29));
        actionTable.get(76).put("else", new Action(ActionType.REDUCE, 29));
        actionTable.get(77).put("end", new Action(ActionType.REDUCE, 30));
        actionTable.get(77).put("skip", new Action(ActionType.REDUCE, 30));
        actionTable.get(77).put("halt", new Action(ActionType.REDUCE, 30));
        actionTable.get(77).put("return", new Action(ActionType.REDUCE, 30));
        actionTable.get(77).put("VARIABLE", new Action(ActionType.REDUCE, 30));
        actionTable.get(77).put("if", new Action(ActionType.REDUCE, 30));
        actionTable.get(77).put("else", new Action(ActionType.REDUCE, 30));
        actionTable.get(78).put(",", new Action(ActionType.SHIFT, 89));
        actionTable.get(79).put(",", new Action(ActionType.SHIFT, 90));
        actionTable.get(80).put("else", new Action(ActionType.SHIFT, 91));
        actionTable.get(81).put("VARIABLE", new Action(ActionType.SHIFT, 40));
        actionTable.get(81).put("NUMBER", new Action(ActionType.SHIFT, 42));
        actionTable.get(81).put("TEXT", new Action(ActionType.SHIFT, 43));
        actionTable.get(82).put("VARIABLE", new Action(ActionType.SHIFT, 40));
        actionTable.get(82).put("NUMBER", new Action(ActionType.SHIFT, 42));
        actionTable.get(82).put("TEXT", new Action(ActionType.SHIFT, 43));
        actionTable.get(82).put("not", new Action(ActionType.SHIFT, 52));
        actionTable.get(82).put("sqrt", new Action(ActionType.SHIFT, 53));
        actionTable.get(83).put(")", new Action(ActionType.SHIFT, 94));
        actionTable.get(84).put("end", new Action(ActionType.SHIFT, 104));
        actionTable.get(85).put("end", new Action(ActionType.REDUCE, 55));
        actionTable.get(85).put("num", new Action(ActionType.REDUCE, 55));
        actionTable.get(85).put("void", new Action(ActionType.REDUCE, 55));
        actionTable.get(85).put("}", new Action(ActionType.REDUCE, 55));
        actionTable.get(86).put("text", new Action(ActionType.SHIFT, 6));
        actionTable.get(86).put("VARIABLE", new Action(ActionType.SHIFT, 7));
        actionTable.get(87).put("num", new Action(ActionType.SHIFT, 98));
        actionTable.get(88).put("end", new Action(ActionType.REDUCE, 27));
        actionTable.get(88).put("skip", new Action(ActionType.REDUCE, 27));
        actionTable.get(88).put("halt", new Action(ActionType.REDUCE, 27));
        actionTable.get(88).put("return", new Action(ActionType.REDUCE, 27));
        actionTable.get(88).put("VARIABLE", new Action(ActionType.REDUCE, 27));
        actionTable.get(88).put("if", new Action(ActionType.REDUCE, 27));
        actionTable.get(88).put("else", new Action(ActionType.REDUCE, 27));
        actionTable.get(89).put("VARIABLE", new Action(ActionType.SHIFT, 40));
        actionTable.get(89).put("NUMBER", new Action(ActionType.SHIFT, 42));
        actionTable.get(89).put("TEXT", new Action(ActionType.SHIFT, 43));
        actionTable.get(89).put("UNOP(", new Action(ActionType.SHIFT, 64));
        actionTable.get(89).put("BINOP(", new Action(ActionType.SHIFT, 65));
        actionTable.get(90).put("VARIABLE", new Action(ActionType.SHIFT, 40));
        actionTable.get(90).put("NUMBER", new Action(ActionType.SHIFT, 42));
        actionTable.get(90).put("TEXT", new Action(ActionType.SHIFT, 43));
        actionTable.get(91).put("end", new Action(ActionType.SHIFT, 9));
        actionTable.get(92).put(")", new Action(ActionType.SHIFT, 102));
        actionTable.get(93).put(")", new Action(ActionType.SHIFT, 103));
        actionTable.get(94).put("else", new Action(ActionType.REDUCE, 35));
        actionTable.get(95).put("end", new Action(ActionType.SHIFT, 104));
        actionTable.get(96).put("end", new Action(ActionType.REDUCE, 57));
        actionTable.get(97).put("num", new Action(ActionType.SHIFT, 6));
        actionTable.get(97).put("text", new Action(ActionType.SHIFT, 7));
        actionTable.get(97).put("VARIABLE", new Action(ActionType.SHIFT, 7));
        actionTable.get(98).put("num", new Action(ActionType.SHIFT, 6));
        actionTable.get(98).put("text", new Action(ActionType.SHIFT, 7));
        actionTable.get(98).put("VARIABLE", new Action(ActionType.SHIFT, 7));
        actionTable.get(99).put(")", new Action(ActionType.SHIFT, 107));
        actionTable.get(100).put(")", new Action(ActionType.SHIFT, 108));
        actionTable.get(101).put(";", new Action(ActionType.REDUCE, 23));
        actionTable.get(102).put("end", new Action(ActionType.REDUCE, 33));
        actionTable.get(102).put("skip", new Action(ActionType.REDUCE, 33));
        actionTable.get(102).put("halt", new Action(ActionType.REDUCE, 33));
        actionTable.get(102).put("return", new Action(ActionType.REDUCE, 33));
        actionTable.get(102).put("VARIABLE", new Action(ActionType.REDUCE, 33));
        actionTable.get(102).put("if", new Action(ActionType.REDUCE, 33));
        actionTable.get(102).put("else", new Action(ActionType.REDUCE, 33));
        actionTable.get(103).put("else", new Action(ActionType.REDUCE, 34));
        actionTable.get(104).put("end", new Action(ActionType.REDUCE, 53));
        actionTable.get(104).put("num", new Action(ActionType.REDUCE, 53));
        actionTable.get(104).put("void", new Action(ActionType.REDUCE, 53));
        actionTable.get(104).put("}", new Action(ActionType.REDUCE, 53));
        actionTable.get(105).put("num", new Action(ActionType.SHIFT, 109));
        actionTable.get(106).put(")", new Action(ActionType.SHIFT, 110));
        actionTable.get(107).put("end", new Action(ActionType.REDUCE, 28));
        actionTable.get(107).put("skip", new Action(ActionType.REDUCE, 28));
        actionTable.get(107).put("halt", new Action(ActionType.REDUCE, 28));
        actionTable.get(107).put("return", new Action(ActionType.REDUCE, 28));
        actionTable.get(107).put("VARIABLE", new Action(ActionType.REDUCE, 28));
        actionTable.get(107).put("if", new Action(ActionType.REDUCE, 28));
        actionTable.get(107).put("else", new Action(ActionType.REDUCE, 28));
        actionTable.get(108).put(";", new Action(ActionType.REDUCE, 22));
        actionTable.get(109).put("text", new Action(ActionType.SHIFT, 6));
        actionTable.get(109).put("VARIABLE", new Action(ActionType.SHIFT, 7));
        actionTable.get(110).put("end", new Action(ActionType.REDUCE, 50));
        actionTable.get(110).put("}", new Action(ActionType.REDUCE, 50));
        actionTable.get(111).put("num", new Action(ActionType.SHIFT, 6));
        actionTable.get(111).put("text", new Action(ActionType.SHIFT, 7));
        actionTable.get(111).put("VARIABLE", new Action(ActionType.SHIFT, 7));
        actionTable.get(112).put("num", new Action(ActionType.SHIFT, 113));
        actionTable.get(113).put("begin", new Action(ActionType.REDUCE, 56));

        // Goto table entries
        gotoTable.get(0).put("S", 1);
        gotoTable.get(1).put("PROG", 2);
        gotoTable.get(2).put("GLOBVARS", 3);
        gotoTable.get(2).put("VTYP", 5);
        gotoTable.get(3).put("ALGO", 8);
        gotoTable.get(5).put("ALGO", 10);
        gotoTable.get(8).put("FUNCTIONS", 12);
        gotoTable.get(8).put("DECL", 14);
        gotoTable.get(8).put("HEADER", 15);
        gotoTable.get(8).put("FTYP", 16);
        gotoTable.get(9).put("INSTRUC", 19);
        gotoTable.get(9).put("COMMAND", 21);
        gotoTable.get(9).put("ATOMIC", 25);
        gotoTable.get(9).put("CONST", 26);
        gotoTable.get(9).put("ASSIGN", 27);
        gotoTable.get(9).put("CALL", 27);
        gotoTable.get(9).put("BRANCH", 27);
        gotoTable.get(14).put("FUNCTIONS", 32);
        gotoTable.get(14).put("DECL", 14);
        gotoTable.get(14).put("HEADER", 15);
        gotoTable.get(14).put("FTYP", 16);
        gotoTable.get(15).put("BODY", 33);
        gotoTable.get(15).put("PROLOG", 34);
        gotoTable.get(15).put("EPILOG", 34);
        gotoTable.get(24).put("ATOMIC", 39);
        gotoTable.get(24).put("CONST", 41);
        gotoTable.get(29).put("ATOMIC", 46);
        gotoTable.get(29).put("CONST", 41);
        gotoTable.get(30).put("COND", 47);
        gotoTable.get(30).put("SIMPLE", 48);
        gotoTable.get(30).put("COMPOSIT", 49);
        gotoTable.get(30).put("UNOP", 51);
        gotoTable.get(30).put("BINOP", 50);
        gotoTable.get(31).put("GLOBVARS", 54);
        gotoTable.get(31).put("VTYP", 5);
        gotoTable.get(34).put("VTYP", 56);
        gotoTable.get(34).put("VNAME", 55);
        gotoTable.get(36).put("VTYP", 57);
        gotoTable.get(36).put("VNAME", 57);
        gotoTable.get(38).put("INSTRUC", 58);
        gotoTable.get(38).put("INSTRUC", 58);
        gotoTable.get(38).put("COMMAND", 21);
        gotoTable.get(38).put("ATOMIC", 25);
        gotoTable.get(38).put("CONST", 26);
        gotoTable.get(38).put("ASSIGN", 27);
        gotoTable.get(38).put("CALL", 27);
        gotoTable.get(38).put("BRANCH", 27);
        gotoTable.get(45).put("ATOMIC", 60);
        gotoTable.get(45).put("CONST", 41);
        gotoTable.get(45).put("CALL", 61);
        gotoTable.get(45).put("TERM", 62);
        gotoTable.get(45).put("OP", 63);
        gotoTable.get(50).put("ATOMIC", 68);
        gotoTable.get(50).put("CONST", 41);
        gotoTable.get(50).put("SIMPLE", 69);
        gotoTable.get(55).put("ALGO", 72);
        gotoTable.get(56).put("VNAME", 73);
        // Corrected gotoTable entries for states 64-66
        gotoTable.get(64).put("ATOMIC", 76);
        gotoTable.get(64).put("CONST", 41);
        gotoTable.get(64).put("ARG", 75);
        gotoTable.get(64).put("OP", 77);
        gotoTable.get(65).put("ATOMIC", 76);
        gotoTable.get(65).put("CONST", 41);
        gotoTable.get(65).put("ARG", 78);
        gotoTable.get(65).put("OP", 77);
        gotoTable.get(66).put("ATOMIC", 79);
        gotoTable.get(66).put("CONST", 41);
        gotoTable.get(66).put("ARG", 100);
        gotoTable.get(66).put("OP", 77);
        gotoTable.get(67).put("ALGO", 80);
        gotoTable.get(70).put("ATOMIC", 68);
        gotoTable.get(70).put("CONST", 41);
        gotoTable.get(70).put("SIMPLE", 69);
        gotoTable.get(71).put("COND", 83);
        gotoTable.get(71).put("SIMPLE", 48);
        gotoTable.get(71).put("COMPOSIT", 49);
        gotoTable.get(71).put("UNOP", 51);
        gotoTable.get(71).put("BINOP", 50);
        gotoTable.get(72).put("SUBFUNCS", 84);
        gotoTable.get(72).put("FUNCTIONS", 84);
        gotoTable.get(72).put("DECL", 14);
        gotoTable.get(72).put("HEADER", 15);
        gotoTable.get(72).put("FTYP", 16);
        gotoTable.get(74).put("VTYP", 87);
        gotoTable.get(74).put("VNAME", 87);
        gotoTable.get(81).put("ALGO", 92);
        gotoTable.get(82).put("ALGO", 93);
        gotoTable.get(86).put("VNAME", 97);
        gotoTable.get(89).put("ATOMIC", 76);
        gotoTable.get(89).put("CONST", 41);
        gotoTable.get(89).put("ARG", 99);
        gotoTable.get(89).put("OP", 77);
        gotoTable.get(90).put("ATOMIC", 100);
        gotoTable.get(90).put("CONST", 41);
        gotoTable.get(91).put("ALGO", 101);
        gotoTable.get(97).put("VNAME", 105);
        gotoTable.get(98).put("VNAME", 106);
        gotoTable.get(109).put("VNAME", 111);
        gotoTable.get(111).put("VNAME", 112);

        // Fill in the goto table
        // Example: gotoTable.get(1).put("PROG", 2);
        // TODO: Add all goto table entries here
    }

    public void parseInputXML(String inputFile) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new File(inputFile));

        NodeList tokenNodes = doc.getElementsByTagName("TOK");
        for (int i = 0; i < tokenNodes.getLength(); i++) {
            Element tokenElement = (Element) tokenNodes.item(i);
            int id = Integer.parseInt(tokenElement.getElementsByTagName("ID").item(0).getTextContent());
            String classStr = tokenElement.getElementsByTagName("CLASS").item(0).getTextContent();
            String word = tokenElement.getElementsByTagName("WORD").item(0).getTextContent();
            tokens.add(new Token(id, TokenType.valueOf(classStr.toUpperCase()), word));
        }
    }

    public void parse() {
        stateStack.push(0);
        int tokenIndex = 0;
        while (true) {
            int state = stateStack.peek();
            Token token = tokenIndex < tokens.size() ? tokens.get(tokenIndex) : new Token(0, TokenType.EOF, "$");
            Action action = getAction(state, token);

            System.out.println("Current state: " + state + ", Token: " + token.getWord()); // Debug output

            if (action == null) {
                throw new RuntimeException("No action defined for state " + state + " and token " + token.getWord());
            }

            switch (action.type) {
                case SHIFT:
                    System.out.println("Shifting to state " + action.value); // Debug output
                    nodeStack.push(new Node(token));
                    stateStack.push(action.value);
                    tokenIndex++;
                    break;
                case REDUCE:
                    System.out.println("Reducing by rule " + action.value); // Debug output
                    Production prod = getProduction(action.value);
                    List<Node> children = new ArrayList<>();
                    for (int i = 0; i < prod.rightSide.size(); i++) {
                        if (stateStack.isEmpty() || nodeStack.isEmpty()) {
                            throw new RuntimeException(
                                    "Stack underflow during reduce operation. Rule: " + action.value);
                        }
                        stateStack.pop();
                        children.add(0, nodeStack.pop());
                    }
                    Node parent = new Node(prod.leftSide);
                    parent.children.addAll(children);
                    nodeStack.push(parent);

                    if (stateStack.isEmpty()) {
                        throw new RuntimeException(
                                "State stack is empty after reduce operation. Rule: " + action.value);
                    }
                    int gotoState = getGoto(stateStack.peek(), prod.leftSide);
                    if (gotoState == -1) {
                        throw new RuntimeException("No goto defined for state " + stateStack.peek()
                                + " and non-terminal " + prod.leftSide);
                    }
                    stateStack.push(gotoState);
                    break;
                case ACCEPT:
                    System.out.println("Accepting the input"); // Debug output
                    return; // Parsing completed successfully
                default:
                    throw new RuntimeException("Parsing error at token: " + token.getWord() +
                            " in state " + state +
                            " (TokenType: " + token.getType() + ")");
            }
        }
    }

    private void shift(int nextState) {
        Node node = new Node(getCurrentToken());
        nodeStack.push(node);
        stateStack.push(nextState);
        currentTokenIndex++;
    }

    private void reduce(int ruleNumber) {
        Production production = getProduction(ruleNumber);
        List<Node> children = new ArrayList<>();
        for (int i = 0; i < production.rightSide.size(); i++) {
            stateStack.pop();
            children.add(0, nodeStack.pop());
        }
        Node parent = new Node(production.leftSide);
        parent.children.addAll(children);
        nodeStack.push(parent);

        int state = stateStack.peek();
        int nextState = gotoTable.get(state).get(production.leftSide);
        stateStack.push(nextState);
    }

    private Action getAction(int state, Token token) {
        Map<String, Action> actions = actionTable.get(state);
        if (actions == null) {
            return null;
        }
        Action action = actions.get(token.getWord());
        if (action == null) {
            action = actions.get(token.getType().toString());
        }
        return action;
    }
    
    private int getGoto(int state, String nonTerminal) {
        Map<String, Integer> gotos = gotoTable.get(state);
        if (gotos == null) {
            return -1;
        }
        Integer gotoState = gotos.get(nonTerminal);
        return gotoState != null ? gotoState : -1;
    }

    private Token getCurrentToken() {
        return currentTokenIndex < tokens.size() ? tokens.get(currentTokenIndex) : new Token(0, TokenType.EOF, "$");
    }

    public void writeOutputXML(String outputFile) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.newDocument();

        Element rootElement = doc.createElement("SYNTREE");
        doc.appendChild(rootElement);

        Node parseTreeRoot = nodeStack.peek();
        Element rootNode = createNodeElement(doc, parseTreeRoot, null);
        rootElement.appendChild(rootNode);

        Element innerNodesElement = doc.createElement("INNERNODES");
        Element leafNodesElement = doc.createElement("LEAFNODES");
        rootElement.appendChild(innerNodesElement);
        rootElement.appendChild(leafNodesElement);

        Queue<Node> queue = new LinkedList<>();
        queue.offer(parseTreeRoot);
        while (!queue.isEmpty()) {
            Node node = queue.poll();
            if (node.token == null) {
                Element innerNode = createNodeElement(doc, node, parseTreeRoot);
                innerNodesElement.appendChild(innerNode);
                queue.addAll(node.children);
            } else {
                Element leafNode = createLeafElement(doc, node, parseTreeRoot);
                leafNodesElement.appendChild(leafNode);
            }
        }

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(new File(outputFile));
        transformer.transform(source, result);
    }

    private Element createNodeElement(Document doc, Node node, Node root) {
        Element element = doc.createElement(root == node ? "ROOT" : "IN");
        Element unidElement = doc.createElement("UNID");
        unidElement.setTextContent(String.valueOf(node.id));
        element.appendChild(unidElement);

        Element symbElement = doc.createElement("SYMB");
        symbElement.setTextContent(node.symbol);
        element.appendChild(symbElement);

        if (root != node) {
            Element parentElement = doc.createElement("PARENT");
            parentElement.setTextContent(String.valueOf(findParentId(node)));
            element.appendChild(parentElement);
        }

        Element childrenElement = doc.createElement("CHILDREN");
        for (Node child : node.children) {
            Element idElement = doc.createElement("ID");
            idElement.setTextContent(String.valueOf(child.id));
            childrenElement.appendChild(idElement);
        }
        element.appendChild(childrenElement);

        return element;
    }

    private Element createLeafElement(Document doc, Node node, Node root) {
        Element element = doc.createElement("LEAF");
        Element parentElement = doc.createElement("PARENT");
        parentElement.setTextContent(String.valueOf(findParentId(node)));
        element.appendChild(parentElement);

        Element unidElement = doc.createElement("UNID");
        unidElement.setTextContent(String.valueOf(node.id));
        element.appendChild(unidElement);

        Element terminalElement = doc.createElement("TERMINAL");
        Element tokenElement = doc.createElement("TOK");
        Element idElement = doc.createElement("ID");
        idElement.setTextContent(String.valueOf(node.token.getId()));
        Element classElement = doc.createElement("CLASS");
        classElement.setTextContent(node.token.getType().toString().toLowerCase());
        Element wordElement = doc.createElement("WORD");
        wordElement.setTextContent(node.token.getWord());

        tokenElement.appendChild(idElement);
        tokenElement.appendChild(classElement);
        tokenElement.appendChild(wordElement);
        terminalElement.appendChild(tokenElement);
        element.appendChild(terminalElement);

        return element;
    }

    private int findParentId(Node child) {
        for (Node node : nodeStack) {
            if (node.children.contains(child)) {
                return node.id;
            }
        }
        return -1; // Should never happen
    }

    private Production getProduction(int ruleNumber) {
        // TODO: Define your grammar productions here
        // Example:
        // if (ruleNumber == 1) return new Production("S", Arrays.asList("PROG", "$"));
        switch (ruleNumber) {
            case 1:
                return new Production("S", Arrays.asList("PROG", "$"));
            case 2:
                return new Production("PROG", Arrays.asList("main", "GLOBVARS", "ALGO", "FUNCTIONS"));
            case 3:
                return new Production("GLOBVARS", Collections.emptyList()); // epsilon production
            case 4:
                return new Production("GLOBVARS", Arrays.asList("VTYP", "VNAME", ",", "GLOBVARS"));
            case 5:
                return new Production("VTYP", Arrays.asList("num"));
            case 6:
                return new Production("VTYP", Arrays.asList("text"));
            case 7:
                return new Production("VNAME", Arrays.asList("VARIABLE"));
            case 8:
                return new Production("ALGO", Arrays.asList("begin", "INSTRUC", "end"));
            case 9:
                return new Production("INSTRUC", Collections.emptyList()); // epsilon production
            case 10:
                return new Production("INSTRUC", Arrays.asList("COMMAND", ";", "INSTRUC"));
            case 11:
                return new Production("COMMAND", Arrays.asList("skip"));
            case 12:
                return new Production("COMMAND", Arrays.asList("halt"));
            case 13:
                return new Production("COMMAND", Arrays.asList("return", "ATOMIC"));
            case 14:
                return new Production("COMMAND", Arrays.asList("ASSIGN"));
            case 15:
                return new Production("COMMAND", Arrays.asList("CALL"));
            case 16:
                return new Production("COMMAND", Arrays.asList("BRANCH"));
            case 17:
                return new Production("ATOMIC", Arrays.asList("VNAME"));
            case 18:
                return new Production("ATOMIC", Arrays.asList("CONST"));
            case 19:
                return new Production("CONST", Arrays.asList("NUMBER"));
            case 20:
                return new Production("CONST", Arrays.asList("TEXT"));
            case 21:
                return new Production("ASSIGN", Arrays.asList("VNAME", "<", "input"));
            case 22:
                return new Production("ASSIGN", Arrays.asList("VNAME", "=", "TERM"));
            case 23:
                return new Production("CALL", Arrays.asList("FNAME", "(", "ATOMIC", ",", "ATOMIC", ",", "ATOMIC", ")"));
            case 24:
                return new Production("BRANCH", Arrays.asList("if", "COND", "then", "ALGO", "else", "ALGO"));
            case 25:
                return new Production("TERM", Arrays.asList("ATOMIC"));
            case 26:
                return new Production("TERM", Arrays.asList("CALL"));
            case 27:
                return new Production("TERM", Arrays.asList("OP"));
            case 28:
                return new Production("OP", Arrays.asList("UNOP", "(", "ARG", ")"));
            case 29:
                return new Production("OP", Arrays.asList("BINOP", "(", "ARG", ",", "ARG", ")"));
            case 30:
                return new Production("ARG", Arrays.asList("ATOMIC"));
            case 31:
                return new Production("ARG", Arrays.asList("OP"));
            case 32:
                return new Production("COND", Arrays.asList("SIMPLE"));
            case 33:
                return new Production("COND", Arrays.asList("COMPOSIT"));
            case 34:
                return new Production("SIMPLE", Arrays.asList("BINOP", "(", "ATOMIC", ",", "ATOMIC", ")"));
            case 35:
                return new Production("COMPOSIT", Arrays.asList("BINOP", "(", "SIMPLE", ",", "SIMPLE", ")"));
            case 36:
                return new Production("COMPOSIT", Arrays.asList("UNOP", "(", "SIMPLE", ")"));
            case 37:
                return new Production("UNOP", Arrays.asList("not"));
            case 38:
                return new Production("UNOP", Arrays.asList("sqrt"));
            case 39:
                return new Production("BINOP", Arrays.asList("or"));
            case 40:
                return new Production("BINOP", Arrays.asList("and"));
            case 41:
                return new Production("BINOP", Arrays.asList("eq"));
            case 42:
                return new Production("BINOP", Arrays.asList("grt"));
            case 43:
                return new Production("BINOP", Arrays.asList("add"));
            case 44:
                return new Production("BINOP", Arrays.asList("sub"));
            case 45:
                return new Production("BINOP", Arrays.asList("mul"));
            case 46:
                return new Production("BINOP", Arrays.asList("div"));
            case 47:
                return new Production("FNAME", Arrays.asList("FUNCTION"));
            case 48:
                return new Production("FUNCTIONS", Collections.emptyList()); // epsilon production
            case 49:
                return new Production("FUNCTIONS", Arrays.asList("DECL", "FUNCTIONS"));
            case 50:
                return new Production("DECL", Arrays.asList("HEADER", "BODY"));
            case 51:
                return new Production("HEADER",
                        Arrays.asList("FTYP", "FNAME", "(", "VNAME", ",", "VNAME", ",", "VNAME", ")"));
            case 52:
                return new Production("FTYP", Arrays.asList("num"));
            case 53:
                return new Production("FTYP", Arrays.asList("void"));
            case 54:
                return new Production("BODY", Arrays.asList("PROLOG", "LOCVARS", "ALGO", "EPILOG", "SUBFUNCS", "end"));
            case 55:
                return new Production("PROLOG", Arrays.asList("{"));
            case 56:
                return new Production("EPILOG", Arrays.asList("}"));
            case 57:
                return new Production("LOCVARS",
                        Arrays.asList("VTYP", "VNAME", ",", "VTYP", "VNAME", ",", "VTYP", "VNAME", ","));
            case 58:
                return new Production("SUBFUNCS", Arrays.asList("FUNCTIONS"));
            default:
                throw new RuntimeException("Invalid rule number: " + ruleNumber);
        }
    }

    private static class Node {
        public int id;
        String symbol;
        Token token;
        List<Node> children;

        Node(String symbol) {
            this.symbol = symbol;
            this.children = new ArrayList<>();
        }

        Node(Token token) {
            this.token = token;
            this.symbol = token.getWord();
            this.children = new ArrayList<>();
        }
    }

    private static class Action {
        ActionType type;
        int value;

        Action(ActionType type, int value) {
            this.type = type;
            this.value = value;
        }
    }

    private enum ActionType {
        SHIFT, REDUCE, ACCEPT, ERROR
    }

    private static class Production {
        String leftSide;
        List<String> rightSide;

        Production(String leftSide, List<String> rightSide) {
            this.leftSide = leftSide;
            this.rightSide = rightSide;
        }
    }
    
    public List<Token> getTokens() {
        return new ArrayList<>(tokens); // Return a copy to preserve encapsulation
    }
} 

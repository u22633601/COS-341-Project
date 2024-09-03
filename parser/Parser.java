import java.io.*;
import java.util.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;

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
        for (int i = 0; i < 100; i++) { // Assume we have up to 100 states
            actionTable.put(i, new HashMap<>());
        }

        // Initialize goto table
        for (int i = 0; i < 100; i++) {
            gotoTable.put(i, new HashMap<>());
        }

        // Fill in the action table
        // Example: actionTable.get(0).put("main", new Action(ActionType.SHIFT, 1));
        // TODO: Add all action table entries here

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
        while (true) {
            int state = stateStack.peek();
            Token token = getCurrentToken();
            Action action = getAction(state, token);

            if (action.type == ActionType.SHIFT) {
                shift(action.value);
            } else if (action.type == ActionType.REDUCE) {
                reduce(action.value);
            } else if (action.type == ActionType.ACCEPT) {
                break;
            } else {
                throw new RuntimeException("Parsing error at token: " + token.getWord());
            }
        }
    }

    private void shift(int nextState) {
        Node node = new Node(nextNodeId++, getCurrentToken());
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
        Node parent = new Node(nextNodeId++, production.leftSide);
        parent.children.addAll(children);
        nodeStack.push(parent);

        int state = stateStack.peek();
        int nextState = gotoTable.get(state).get(production.leftSide);
        stateStack.push(nextState);
    }

    private Action getAction(int state, Token token) {
        return actionTable.get(state).getOrDefault(token.getWord(), new Action(ActionType.ERROR, 0));
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
        // Add all your grammar productions here
        throw new RuntimeException("Invalid rule number: " + ruleNumber);
    }

    private static class Node {
        int id;
        String symbol;
        List<Node> children;
        Token token;

        Node(int id, String symbol) {
            this.id = id;
            this.symbol = symbol;
            this.children = new ArrayList<>();
        }

        Node(int id, Token token) {
            this.id = id;
            this.symbol = token.getWord();
            this.children = new ArrayList<>();
            this.token = token;
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
} {
    
}

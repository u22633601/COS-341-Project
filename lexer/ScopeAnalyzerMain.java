import java.io.File;
import javax.xml.parsers.*;
import org.w3c.dom.*;

public class ScopeAnalyzerMain {
    public static void main(String[] args) {
        try {
            // Parse the XML file
            File inputFile = new File("parser.xml");
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();

            // Parse the XML into a custom node structure
            ScopeAnalyzer analyzer = new ScopeAnalyzer();
            NodeType root = analyzer.parseXML(doc.getDocumentElement());

            // Print the structure of the parsed syntax tree
            System.out.println("Parsed Syntax Tree Structure:");
            printNodeStructure(root, 0);

            // Analyze the scope
            analyzer.analyzeNode(root);

            // Generate intermediate code
            IntermediateCodeGenerator codeGen = new IntermediateCodeGenerator(analyzer.getUniqueNames());
            String intermediateCode = codeGen.generateCode(root);
            System.out.println("Intermediate Code:");
            System.out.println(intermediateCode);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void printNodeStructure(NodeType node, int depth) {
        StringBuilder indent = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            indent.append("  ");
        }
        System.out.println(indent + node.getType() + (node.getVarName().isEmpty() ? "" : ": " + node.getVarName()));
        for (NodeType child : node.getChildren()) {
            printNodeStructure(child, depth + 1);
        }
    }
}
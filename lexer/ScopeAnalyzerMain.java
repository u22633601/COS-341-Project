import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.File;

public class ScopeAnalyzerMain {
    public static void main(String[] args) {
        try {
            // Parse the XML file
            System.out.println("Parsing XML syntax tree...");
            File inputFile = new File("parser.xml");
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();

            // Parse the XML into a custom node structure
            ScopeAnalyzer analyzer = new ScopeAnalyzer();
            Node root = analyzer.parseXML(doc.getDocumentElement());
            System.out.println("Parsing completed.");
            System.out.println("Root node parsed: " + root.getVarName());

            // Analyze the scope
            System.out.println("Analyzing scope...");
            analyzer.analyzeNode(root);
            
            // Print final global symbol table
            analyzer.printGlobalSymbolTable();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}


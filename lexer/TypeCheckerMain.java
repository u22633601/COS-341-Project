import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.File;

public class TypeCheckerMain {
    public static void main(String[] args) {
        try {
            // Parse the XML file
            System.out.println("Parsing XML syntax tree for type checking...");
            File inputFile = new File("parser.xml");
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();

            // Parse the XML into a custom node structure
            TypeChecker typeChecker = new TypeChecker();
            Node root = typeChecker.parseXML(doc.getDocumentElement());
            System.out.println("Parsing completed.");
            System.out.println("Root node parsed for type checking: " + root.getVarName());

            // Perform type checking
            System.out.println("Type checking...");
            boolean success = typeChecker.typeCheck(root);
            if (success) {
                System.out.println("Type checking was successful.");
            } else {
                System.out.println("Type checking failed.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

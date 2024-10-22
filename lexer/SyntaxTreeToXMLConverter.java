import java.io.File;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class SyntaxTreeToXMLConverter {

    public void convertToXML(ParseNode rootNode, String outputFilePath) throws Exception {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        Document document = documentBuilder.newDocument();

        Element rootElement = convertNodeToXMLElement(rootNode, document);
        document.appendChild(rootElement);

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource domSource = new DOMSource(document);
        StreamResult streamResult = new StreamResult(new File(outputFilePath));
        transformer.transform(domSource, streamResult);
    }

    private Element convertNodeToXMLElement(ParseNode node, Document document) {
        Element element = document.createElement("NODE");
        element.setAttribute("value", node.getValue());

        for (ParseNode child : node.getChildren()) {
            Element childElement = convertNodeToXMLElement(child, document);
            element.appendChild(childElement);
        }

        return element;
    }
}

import java.util.List;

public class ParserMain {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java ParserMain <input-xml-file> <output-xml-file>");
            return;
        }

        Parser parser = new Parser();
        try {
            parser.parseInputXML(args[0]);

            // Debug: Print the first few tokens
            List<Token> tokens = parser.getTokens();
            System.out.println("First 10 tokens:");
            for (int i = 0; i < Math.min(10, tokens.size()); i++) {
                Token token = tokens.get(i);
                System.out.println(token.getType() + ": " + token.getWord());
            }

            parser.parse();
            parser.writeOutputXML(args[1]);
            System.out.println("Parsing complete. Output written to " + args[1]);
        } catch (Exception e) {
            System.err.println("Error during parsing: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
}
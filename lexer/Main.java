import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java Main <input-file>");
            return;
        }

        Lexer lexer = new Lexer();
        try {
            lexer.tokenizeFile(args[0]);
            lexer.writeTokensToXML("lexer.xml");
            System.out.println("Tokenization complete. Output written to output.xml.");
        } catch (IOException e) {
            System.err.println("Error processing file: " + e.getMessage());
        }
    }
}

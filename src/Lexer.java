import java.io.*;
import java.util.*;

public class Lexer {
    private List<Token> tokens = new ArrayList<>();
    private int tokenId = 1;

    private static final String VARIABLE_REGEX = "V_[a-z]([a-z]|[0-9])*";
    private static final String FUNCTION_REGEX = "F_[a-z]([a-z]|[0-9])*";
    private static final String TEXT_REGEX = "\"[A-Z][a-z]{0,7}\"";
    private static final String NUMBER_REGEX = "-?\\d+(\\.\\d+)?";
    private static final String RESERVED_KEYWORDS_REGEX = "\\b(main|begin|end|skip|halt|print|if|then|else|input|num|text|void|=|<|>|<=|>=|==|!=|\\+|\\-|\\*|\\/|\\(|\\)|,|;)\\b";
    private static final String OPERATORS_REGEX = "[=+\\-*/<>!]";
    
    public void tokenizeFile(String filePath) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        String line;
        while ((line = reader.readLine()) != null) {
            tokenizeLine(line);
        }
        reader.close();
    }

    private void tokenizeLine(String line) {
        String[] words = line.trim().split("\\s+"); // Split by whitespace but ignore extra spaces
        for (String word : words) {
            if (word.isEmpty()) continue; // Skip empty tokens that might occur due to extra spaces
            TokenType type = identifyTokenType(word);
            if (type != TokenType.INVALID) {
                tokens.add(new Token(tokenId++, type, word));
            } else {
                System.err.println("Invalid token encountered: " + word);
                System.exit(1);
            }
        }
    }
    

    private TokenType identifyTokenType(String word) {
        if (word.matches(VARIABLE_REGEX)) return TokenType.VARIABLE;
        if (word.matches(FUNCTION_REGEX)) return TokenType.FUNCTION;
        if (word.matches(TEXT_REGEX)) return TokenType.TEXT;
        if (word.matches(NUMBER_REGEX)) return TokenType.NUMBER;
        if (word.matches(RESERVED_KEYWORDS_REGEX)) return TokenType.RESERVED_KEYWORD;
        if (word.matches(OPERATORS_REGEX)) return TokenType.RESERVED_KEYWORD; // Operators treated as reserved keywords
        return TokenType.INVALID;
    }
       

    public void writeTokensToXML(String outputFilePath) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath));
        writer.write("<TOKENSTREAM>\n");
        for (Token token : tokens) {
            writer.write(token.toXML() + "\n");
        }
        writer.write("</TOKENSTREAM>");
        writer.close();
    }
}

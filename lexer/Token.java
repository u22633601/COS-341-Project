public class Token {
    private int id;
    private TokenType type;
    private String word;

    public Token(int id, TokenType type, String word) {
        this.id = id;
        this.type = type;
        this.word = word;
    }

    public int getId() {
        return id;
    }

    public TokenType getType() {
        return type;
    }

    public String getWord() {
        return word;
    }

    public String toXML() {
        String sanitizedWord = word.replace("<", "lst");
        return "<TOK>\n" +
               "  <ID>" + id + "</ID>\n" +
               "  <CLASS>" + type.toString().toLowerCase() + "</CLASS>\n" +
               "  <WORD>" + sanitizedWord + "</WORD>\n" +
               "</TOK>";
    }
}

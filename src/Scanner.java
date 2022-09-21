import token.Tokens.Token;

import java.util.ArrayList;

public class Scanner {
    /** A list of all tokens.
     */
    private ArrayList<Token> tokens = new ArrayList<>();

    /** The token, set by nextToken().
     */
    private Token token;

    public Scanner() {}

    public boolean isEmpty() { return tokens.isEmpty(); }

    public void saveToken(Token token) { tokens.add(token); }

    public void nextToken() {
        if (!tokens.isEmpty()) token = tokens.remove(0);
    }

    public Token lookAheadToken(int pos) {
        if (tokens.size() > pos) {
            return tokens.get(pos);
        } else return null;
    }

    public Token getToken() { return token; }

    public ArrayList<Token> getTokens() { return tokens; }
}

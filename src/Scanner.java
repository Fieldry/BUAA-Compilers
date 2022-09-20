import token.Tokens;

import java.util.ArrayList;

public class Scanner {
    /** A list of all tokens.
     */
    private ArrayList<Tokens.Token> tokens = new ArrayList<>();

    /** The token, set by nextToken().
     */
    private Tokens.Token token;

    /** The previous token, set by nextToken().
     */
    private Tokens.Token prevToken;

    public Scanner() {}

    public void saveToken(Tokens.Token token) { tokens.add(token); }

    public void nextToken() {
        prevToken = token;
        if (!tokens.isEmpty()) token = tokens.remove(0);
    }

    public Tokens.Token lookAheadToken(int pos) {
        if (tokens.size() > pos) {
            return tokens.get(pos);
        } else return null;
    }

    public Tokens.Token getToken() { return token; }

    public Tokens.Token getPrevToken() { return prevToken; }

    public ArrayList<Tokens.Token> getTokens() { return tokens; }
}

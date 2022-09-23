package token;

import java.util.HashMap;

public class Tokens {
    private static final HashMap<String, TokenKind> keywords = new HashMap<>();

    public Tokens() {
        for (TokenKind t : TokenKind.values()) {
            keywords.put(t.name, t);
        }
    }

    public enum TokenKind {
        IDENT("identifier", "IDENFR"),
        INTC("int const", "INTCON"),
        FORMATS("format string", "STRCON"),

        // Keywords
        VOID("void"),
        INT("int"),
        CONST("const"),
        MAIN("main"),
        IF("if"),
        ELSE("else"),
        WHILE("while"),
        BREAK("break"),
        CONTINUE("continue"),
        RETURN("return"),

        // IO functions
        PRINTF("printf"),
        GETINT("getint"),

        // Operators
        PLUS("+", "PLUS"),
        MINUS("-", "MINU"),
        STAR("*", "MULT"),
        DIV("/", "DIV"),
        MOD("%", "MOD"),

        LPAR("(", "LPARENT"),
        RPAR(")", "RPARENT"),
        LBRACE("{", "LBRACE"),
        RBRACE("}", "RBRACE"),
        LSQU("[", "LBRACK"),
        RSQU("]", "RBRACK"),

        SEMI(";", "SEMICN"),
        COMMA(",", "COMMA"),

        ASSIGN("=", "ASSIGN"),
        NOT("!", "NOT"),
        LSS("<", "LSS"),
        GRE(">", "GRE"),

        EQL("==", "EQL"),
        NEQ("!=", "NEQ"),
        LEQ("<=", "LEQ"),
        GEQ(">=", "GEQ"),

        AND("&&", "AND"),
        OR("||", "OR"),

        // One-line comment
        OLC("//", "OLC");

        public final String name;
        public final String code;

        TokenKind(String name) {
            this.name = name;
            this.code = this.name.toUpperCase() + "TK";
        }

        TokenKind(String name, String code) {
            this.name = name;
            this.code = code;
        }

        @Override
        public String toString() { return this.code; }
    }

    public static class Token {
        /** Kind of token.
         */
        public TokenKind tokenKind;

        /** Which line.
         */
        public int line;

        /** Real value for IntConst, FormatString and Identifier.
         */
        public String value;

        public Token(TokenKind tokenKind) {
            this.tokenKind = tokenKind;
            this.line = 0;
            this.value = null;
        }
        public Token(TokenKind tokenKind, int line) {
            this.tokenKind = tokenKind;
            this.line = line;
            this.value = null;
        }

        public Token(TokenKind tokenKind, int line, String value) {
            this.tokenKind = tokenKind;
            this.line = line;
            this.value = value;
        }

        @Override
        public String toString() {
            return this.tokenKind.code + " " + (value == null ? "" : value);
        }

        public TokenKind getTokenKind() {
            return tokenKind;
        }

        public int getLine() {
            return line;
        }

        public String getValue() {
            return value;
        }
    }

    public TokenKind lookupKeywords(String name) {
        TokenKind t = keywords.get(name);
        return t != null ? t : TokenKind.IDENT;
    }

}

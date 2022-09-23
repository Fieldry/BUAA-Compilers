import token.Tokens;
import exception.SysYException;
import exception.SysYException.*;

import java.util.ArrayList;
import java.util.List;

public class Tokenizer {
    private Tokens tokens;
    private Reader reader;
    private Scanner scanner;

    protected final List<SysYException> errors = new ArrayList<>();

    boolean commentsFlag = false;
    int line = 0;
    Tokens.TokenKind tokenKind;

    public Tokenizer(Tokens tokens, Reader reader, Scanner scanner) {
        this.tokens = tokens;
        this.reader = reader;
        this.scanner = scanner;
    }

    /** Read token.
     */
    public Tokens.Token readToken() {
        reader.sp = 0;
        while (true) {
            if (reader.isEnd()) {
                return null;
            } else if (commentsFlag) {
                if (reader.isStar()) {
                    reader.readChar();
                    if (reader.isDiv()) {
                        // Multiline comments end.
                        commentsFlag = false;
                        reader.readChar();
                    }
                } else reader.readChar();
            } else if (reader.isWhitespace()) {
                reader.readChar();
            } else if (reader.isUnder() || reader.isAlpha()) {
                while (reader.isUnder() || reader.isAlpha() || reader.isDigit()) {
                    reader.saveChar();
                    reader.readChar();
                }
                tokenKind = tokens.lookupKeywords(reader.savedToken());
                break;
            } else if (reader.isDigit()) {
                while (reader.isDigit()) {
                    reader.saveChar();
                    reader.readChar();
                }
                tokenKind = Tokens.TokenKind.INTC;
                break;
            } else if (reader.isDiv()) {
                reader.readChar();
                if (reader.isDiv()) {
                    // One-line comments.
                    return null;
                } else if (reader.isStar()) {
                    // Multiline comments.
                    commentsFlag = true;
                    reader.readChar();
                } else {
                    // A single division sign
                    tokenKind = Tokens.TokenKind.DIV;
                    break;
                }
            } else if (reader.isQuotes()) {
                // Handle format string.
                reader.saveChar();
                reader.readChar();
                while (!reader.isQuotes()) {
                    reader.saveChar();
                    reader.readChar();
                }
                reader.saveChar();
                reader.readChar();
                tokenKind = Tokens.TokenKind.FORMATS;
                break;
            } else if (reader.isEqual()) {
                reader.readChar();
                if (reader.isEqual()) {
                    // Equals
                    tokenKind = Tokens.TokenKind.EQL;
                    reader.readChar();
                } else {
                    // Assignment
                    tokenKind = Tokens.TokenKind.ASSIGN;
                }
                break;
            } else if (reader.isLess()) {
                reader.readChar();
                if (reader.isEqual()) {
                    // Less Or Equals
                    tokenKind = Tokens.TokenKind.LEQ;
                    reader.readChar();
                } else {
                    // Less
                    tokenKind = Tokens.TokenKind.LSS;
                }
                break;
            } else if (reader.isGreater()) {
                reader.readChar();
                if (reader.isEqual()) {
                    // Greater Or Equals
                    tokenKind = Tokens.TokenKind.GEQ;
                    reader.readChar();
                } else {
                    // Greater
                    tokenKind = Tokens.TokenKind.GRE;
                }
                break;
            } else if (reader.isNot()) {
                reader.readChar();
                if (reader.isEqual()) {
                    // Not equals
                    tokenKind = Tokens.TokenKind.NEQ;
                    reader.readChar();
                } else {
                    // Not
                    tokenKind = Tokens.TokenKind.NOT;
                }
                break;
            } else if (reader.isAnd()) {
                reader.readChar();
                if (reader.isAnd()) {
                    tokenKind = Tokens.TokenKind.AND;
                    reader.readChar();
                } else {
                    // Error
                }
                break;
            } else if (reader.isOr()) {
                reader.readChar();
                if (reader.isOr()) {
                    tokenKind = Tokens.TokenKind.OR;
                    reader.readChar();
                } else {
                    // Error
                }
                break;
            } else if (reader.isPlus() || reader.isMinus() || reader.isStar() || reader.isMod()
                    || reader.isLPar() || reader.isLSqu() || reader.isLBrace()
                    || reader.isRPar() || reader.isRSqu() || reader.isRBrace()
                    || reader.isComma() || reader.isSemi()) {
                tokenKind = tokens.lookupKeywords(String.valueOf(reader.getChar()));
                reader.readChar();
                break;
            } else {
                System.out.println("Unexpected char:" + reader.getChar());
                break;
            }
        }

        return new Tokens.Token(tokenKind);
    }

    public void tokenAnalyse() throws SysYException {
        Tokens.Token token;
        while (reader.readNextLine()) {
            line++;
            while ((token = readToken()) != null) {
                token.line = line;
                if (token.tokenKind == Tokens.TokenKind.IDENT || token.tokenKind == Tokens.TokenKind.FORMATS
                        || token.tokenKind == Tokens.TokenKind.INTC) {
                    token.value = reader.savedToken();
                    if (token.tokenKind == Tokens.TokenKind.FORMATS) {
                        for (int i = 1; i < token.value.length() - 1; i++) {
                            char c = token.value.charAt(i);
                            if (c == '%') {
                                c = token.value.charAt(++i);
                                if (c != 'd') errors.add(new SysYException(EKind.a, line));
                            } else if (c == '\\') {
                                c = token.value.charAt(++i);
                                if (c != 'n') errors.add(new SysYException(EKind.a, line));
                            } else if (!(c == ' ' || c == 33 || c >= 40 && c <= 126)){
                                errors.add(new SysYException(EKind.a, line));
                            }
                        }
                    }
                }
                scanner.saveToken(token);
            }
        }
        throw new SysYException(EKind.o, line);
    }
}

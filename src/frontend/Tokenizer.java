package frontend;

import frontend.token.Tokens;
import frontend.exception.SysYException;
import frontend.exception.SysYException.*;
import frontend.token.Tokens.*;
import utils.Reader;

import java.util.ArrayList;
import java.util.List;

public class Tokenizer {
    private final Tokens tokens;
    private final Reader reader;
    private final Scanner scanner;

    public final List<SysYException> errors = new ArrayList<>();

    private boolean commentsFlag = false;
    private int line = 0;
    private TokenKind tokenKind;

    public Tokenizer(Tokens tokens, Reader reader, Scanner scanner) {
        this.tokens = tokens;
        this.reader = reader;
        this.scanner = scanner;
    }

    /** Read frontend.token.
     */
    public Token readToken() {
        reader.resetSp();
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
                tokenKind = TokenKind.INTC;
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
                    tokenKind = TokenKind.DIV;
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
                tokenKind = TokenKind.FORMATS;
                break;
            } else if (reader.isEqual()) {
                reader.readChar();
                if (reader.isEqual()) {
                    // Equals
                    tokenKind = TokenKind.EQL;
                    reader.readChar();
                } else {
                    // Assignment
                    tokenKind = TokenKind.ASSIGN;
                }
                break;
            } else if (reader.isLess()) {
                reader.readChar();
                if (reader.isEqual()) {
                    // Less Or Equals
                    tokenKind = TokenKind.LEQ;
                    reader.readChar();
                } else {
                    // Less
                    tokenKind = TokenKind.LSS;
                }
                break;
            } else if (reader.isGreater()) {
                reader.readChar();
                if (reader.isEqual()) {
                    // Greater Or Equals
                    tokenKind = TokenKind.GEQ;
                    reader.readChar();
                } else {
                    // Greater
                    tokenKind = TokenKind.GRE;
                }
                break;
            } else if (reader.isNot()) {
                reader.readChar();
                if (reader.isEqual()) {
                    // Not equals
                    tokenKind = TokenKind.NEQ;
                    reader.readChar();
                } else {
                    // Not
                    tokenKind = TokenKind.NOT;
                }
                break;
            } else if (reader.isAnd()) {
                reader.readChar();
                if (reader.isAnd()) {
                    tokenKind = TokenKind.AND;
                    reader.readChar();
                } else {
                    // Error
                }
                break;
            } else if (reader.isOr()) {
                reader.readChar();
                if (reader.isOr()) {
                    tokenKind = TokenKind.OR;
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
                System.exit(1);
                break;
            }
        }

        return new Token(tokenKind);
    }

    public void tokenAnalyse() throws SysYException {
        Token token;
        while (reader.readNextLine()) {
            line++;
            while ((token = readToken()) != null) {
                token.setLine(line);
                if (token.getTokenKind() == TokenKind.IDENT || token.getTokenKind() == TokenKind.FORMATS
                        || token.getTokenKind() == TokenKind.INTC) {
                    token.setValue(reader.savedToken());
                    if (token.getTokenKind() == TokenKind.FORMATS) {
                        String string = token.getValue();
                        for (int i = 1, size = string.length() - 1; i < size; i++) {
                            char c = string.charAt(i);
                            if (c == '%') {
                                c = string.charAt(++i);
                                if (c != 'd') errors.add(new SysYException(EKind.a, line));
                            } else if (c == '\\') {
                                c = string.charAt(++i);
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

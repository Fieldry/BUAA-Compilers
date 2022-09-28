package io;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class Reader {
    /** The current character.
     */
    private char ch;

    /** The input buffer,
     * the index of next char to be read
     * and the length of the buffer.
     */
    private char[] buf;
    private int bp;
    private int len;

    /** A character buffer for saved chars.
     */
    char[] sbuf = new char[1<<20];
    int sp;

    /** The file to be read.
     */
    String filename;
    BufferedReader br;

    public Reader(String filename) {
        this.filename = filename;
        try {
            this.br = new BufferedReader(new FileReader(filename));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Read a new line of the file and assign it to buf.
     * @return true if reading succeeds, or false if reaches the end of file.
     */
    public boolean readNextLine() {
        String s = null;
        try {
            s = br.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (s != null) {
            buf = s.toCharArray();
            bp = 0;
            len = buf.length;
            readChar();
            return true;
        } else return false;
    }

    /**
     * Read next char in buffer.
     */
    public void readChar() {
        if (bp < len) {
            ch = buf[bp++];
        } else {
            ch = 0;
        }
    }

    public char getChar() { return ch; }

    public void saveChar() { sbuf[sp++] = ch; }

    public String savedToken() { return String.valueOf(sbuf, 0, sp); }

    public void resetSp() { sp = 0; }

    public boolean isDigit() { return ch <= '9' && ch >= '0'; }

    public boolean isAlpha() { return ch <= 'Z' && ch >= 'A' || ch <= 'z' && ch >= 'a'; }

    public boolean isUnder() { return ch == '_'; }

    public boolean isPlus() { return ch == '+'; }

    public boolean isMinus() { return ch == '-'; }

    public boolean isStar() { return ch == '*'; }

    public boolean isDiv() { return ch == '/'; }

    public boolean isMod() { return ch == '%'; }

    // parentheses
    public boolean isLPar() { return ch == '('; }

    public boolean isRPar() { return ch == ')'; }

    // square brackets
    public boolean isLSqu() { return ch == '['; }

    public boolean isRSqu() { return ch == ']'; }

    // braces
    public boolean isLBrace() { return ch == '{'; }

    public boolean isRBrace() { return ch == '}'; }

    public boolean isLess() { return ch == '<'; }

    public boolean isGreater() { return ch == '>'; }

    public boolean isEqual() { return ch == '='; }

    public boolean isNot() { return ch == '!'; }

    public boolean isAnd() { return ch == '&'; }

    public boolean isOr() { return ch == '|'; }

    public boolean isComma() { return ch == ','; }

    public boolean isSemi() { return ch == ';'; }

    public boolean isQuotes() { return ch == '"'; }

    public boolean isWhitespace() { return ch <= ' '; }

    public boolean isEnd() { return ch == 0; }
}

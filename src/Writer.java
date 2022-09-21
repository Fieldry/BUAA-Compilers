import token.Tokens.Token;
import exception.SysYException;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class Writer {

    /** The file to output.
     */
    String filename;
    BufferedWriter bw;

    /** The file to output error.
     */
    String errFilename;
    BufferedWriter errBw;

    public Writer(String filename, String errFilename) {
        this.filename = filename;
        this.errFilename = errFilename;
        try {
            this.bw = new BufferedWriter(new FileWriter(filename));
            this.errBw = new BufferedWriter(new FileWriter(errFilename));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeToken(Token token) {
        try {
            bw.write(token.tokenKind.code + " "
                    + (token.value != null ? token.value : token.tokenKind.name) + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeTokens(List<Token> tokens) {
        try {
            for (Token token : tokens) {
                writeToken(token);
            }
            bw.flush();
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void write(String string) {
        try {
            bw.write(string + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeError(SysYException exception) {
        try {
            errBw.write(exception.line + " " + exception.kind + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            bw.flush();
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

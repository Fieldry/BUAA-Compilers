import token.Tokens;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Writer {

    /** The file to output.
     */
    String filename;
    BufferedWriter bw;

    public Writer(String filename) {
        this.filename = filename;
        try {
            this.bw = new BufferedWriter(new FileWriter(filename));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeToken(Tokens.Token token) {
        try {
            bw.write(token.tokenKind.code + " "
                    + (token.value != null ? token.value : token.tokenKind.name) + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeTokens(List<Tokens.Token> tokens) {
        try {
            for (Tokens.Token token : tokens) {
                writeToken(token);
            }
            bw.flush();
            bw.close();
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

    public void write(String string) {
        try {
            bw.write(string + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

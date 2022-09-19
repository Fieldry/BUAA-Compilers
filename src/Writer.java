import token.Tokens;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class Writer {
    /** A list of all tokens.
     */
    ArrayList<Tokens.Token> tokens;

    /** The file to output.
     */
    String filename;
    BufferedWriter bw;

    public Writer(String filename, ArrayList<Tokens.Token> tokens) {
        this.tokens = tokens;

        this.filename = filename;
        try {
            this.bw = new BufferedWriter(new FileWriter(filename));
        } catch (IOException e) {
            System.out.println(e);
        }
    }

    public void writeToken() {
        try {
            for (Tokens.Token token : tokens) {
                bw.write(token.tokenKind.code + " "
                        + (token.value != null ? token.value : token.tokenKind.name) + "\n");
            }
            bw.flush();
            bw.close();
        } catch (IOException e) {
            System.out.println(e);
        }
    }
}

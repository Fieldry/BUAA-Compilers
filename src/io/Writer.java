package io;

import frontend.token.Tokens.Token;
import frontend.exception.SysYException;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class Writer {

    /** The file to output.
     */
    private final String filename;
    private BufferedWriter bw;

    /** The file to output error.
     */
    private final String errFilename;
    private BufferedWriter errBw;

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
            bw.write(token.getTokenKind().getCode() + " "
                    + (token.getValue() != null ? token.getValue() : token.getTokenKind().getName()) + "\n");
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
            bw.write(string);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeln(String string) {
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

    public void writeErrors(List<SysYException> errors) {
        for (SysYException error : errors) {
            writeError(error);
        }
    }

    public void close() {
        try {
            bw.flush();
            bw.close();
            errBw.flush();
            errBw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

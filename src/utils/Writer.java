package utils;

import frontend.token.Tokens.Token;
import frontend.exception.SysYException;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

public class Writer {
    /** The file to output.
     */
    private BufferedWriter bw;

    /** The file to output token and syntax.
     */
    private BufferedWriter outBw;

    /** The file to output error.
     */
    private BufferedWriter errBw;

    /** The file to output llvm ir.
     */
    private BufferedWriter llvmBw;

    /** The file to output mips.
     */
    private BufferedWriter mipsBw;

    public Writer(String output, String err, String llvm, String mips) {
        try {
            this.bw = this.outBw = new BufferedWriter(new FileWriter(output));
            this.errBw = new BufferedWriter(new FileWriter(err));
            this.llvmBw = new BufferedWriter(new FileWriter(llvm));
            this.mipsBw = new BufferedWriter(new FileWriter(mips));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeToken(Token token) {
        try {
            outBw.write(token.getTokenKind().getCode() + " "
                    + (token.getValue() != null ? token.getValue() : token.getTokenKind().getName()) + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeTokens(List<Token> tokens) {
        for (Token token : tokens)
            writeToken(token);
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

    public void setLlvmBw() { bw = llvmBw; }

    public void setMipsBw() {
        bw = mipsBw;
        try {
            llvmBw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setStdOut() { bw = new BufferedWriter(new PrintWriter(System.out)); }

    public void write(String string) {
        try {
            bw.write(string);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeln(Object o) {
        try {
            bw.write(o.toString() + "\n");
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

    public void close() {
        try {
            outBw.flush();
            outBw.close();
            errBw.flush();
            errBw.close();
            llvmBw.flush();
            llvmBw.close();
            mipsBw.flush();
            mipsBw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

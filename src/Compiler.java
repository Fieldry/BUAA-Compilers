import exception.SysYException;
import token.Tokens;
import tree.SysYTree.*;

public class Compiler {
    public static Reader reader;
    public static Writer writer;
    public static Tokenizer tokenizer;
    public static Parser parser;

    public static Tokens tokens = new Tokens();
    public static Scanner scanner = new Scanner();

    public static void main(String[] args) {
        String input = "testfile.txt", output = "output.txt", error = "error.txt";
        reader = new Reader(input);
        writer = new Writer(output, error);
        tokenizer = new Tokenizer(tokens, reader, scanner);
        try {
            tokenizer.tokenAnalyse();
        } catch (SysYException e) {
            e.printStackTrace();
        }
        parser = new Parser(scanner, writer, true);
        try {
            SysYCompilationUnit compUnit = parser.syntaxAnalyse();
        } catch (SysYException e) {
            e.printStackTrace();
        }
        writer.close();
    }
}

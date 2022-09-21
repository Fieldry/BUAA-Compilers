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
        String input = "testfile.txt", output = "output.txt";
        reader = new Reader(input);
        writer = new Writer(output);
        tokenizer = new Tokenizer(tokens, reader, scanner);
        tokenizer.tokenAnalyse();
        parser = new Parser(scanner, writer, true);
        SysYCompilationUnit compUnit = parser.syntaxAnalyse();
        writer.close();
    }
}

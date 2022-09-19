import token.Tokens;

public class Compiler {
    public static Reader reader;
    public static Writer writer;
    public static Tokenizer tokenizer;

    public static Tokens tokens = new Tokens();
    public static Scanner scanner = new Scanner();

    public static void main(String[] args) {
        String input = "testfile.txt", output = "output.txt";
        reader = new Reader(input);
        tokenizer = new Tokenizer(tokens, reader, scanner);
        tokenizer.tokenAnalyse();

        writer = new Writer(output, scanner.getTokens());
        writer.writeToken();
    }
}

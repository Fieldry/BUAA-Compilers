import exception.SysYException;
import symbolTable.SymbolTable;
import token.Tokens;
import tree.SysYTree;
import tree.SysYTree.*;

import java.util.*;

public class Compiler {
    public static Reader reader;
    public static Writer writer;
    public static Tokenizer tokenizer;
    public static Parser parser;

    public static Tokens tokens = new Tokens();
    public static Scanner scanner = new Scanner();

    public static void main(String[] args) {
        String input = "testfile.txt", output = "output.txt", error = "error.txt";
        SysYCompilationUnit compUnit = null;

        reader = new Reader(input);
        writer = new Writer(output, error);
        tokenizer = new Tokenizer(tokens, reader, scanner);
        try {
            tokenizer.tokenAnalyse();
        } catch (SysYException e) {
            for (SysYException err: tokenizer.errors) {
                if (err.kind != SysYException.EKind.o) writer.writeError(err);
            }
        }
        parser = new Parser(scanner, writer, true);
        try {
            compUnit = parser.syntaxAnalyse();
        } catch (SysYException e) {
            for (SysYException err: parser.errors) {
                if (err.kind != SysYException.EKind.o) writer.writeError(err);
            }
        }

        if (compUnit != null) {
            compUnit.check(new SymbolTable(null), false);
        }

        List<SysYException> errors = new ArrayList<>(tokenizer.errors) {{
            addAll(parser.errors);
            addAll(SysYTree.errors);
            sort(Comparator.comparingInt(SysYException::getLine));
        }};
        writer.writeErrors(errors);
        writer.close();
    }
}

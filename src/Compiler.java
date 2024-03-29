import backend.MIPSBuilder;
import midend.mir.AssemblyBuilder;
import midend.mir.Module;
import frontend.exception.SysYException;
import frontend.token.Tokens;
import frontend.tree.SysYTree.*;
import frontend.Parser;
import frontend.Scanner;
import frontend.Tokenizer;
import utils.Reader;
import utils.Writer;

public class Compiler {
    public static Reader reader;
    public static Writer writer;
    public static Tokenizer tokenizer;
    public static Parser parser;
    public static AssemblyBuilder builder;

    public static MIPSBuilder mipsBuilder;

    public static Tokens tokens = new Tokens();
    public static Scanner scanner = new Scanner();
    public static Module module = new Module();

    public static void main(String[] args) {
        String input = "testfile.txt", output = "output.txt", error = "error.txt",
            llvm = "llvm_ir.txt", mips = "mips.txt";
        SysYCompilationUnit compUnit = null;

        reader = new Reader(input);
        writer = new Writer(output, error, llvm, mips);
        builder = new AssemblyBuilder(writer, module);
        mipsBuilder = new MIPSBuilder(writer, module);

        tokenizer = new Tokenizer(tokens, reader, scanner);
        try {
            tokenizer.tokenAnalyse();
        } catch (SysYException e) {
            if (e.getKind() != SysYException.EKind.o) e.printStackTrace();
        }

        parser = new Parser(scanner);
        try {
            compUnit = parser.syntaxAnalyse();
        } catch (SysYException e) {
            if (e.getKind() != SysYException.EKind.o) e.printStackTrace();
        }

        /*
        if (compUnit != null) {
            try {
                compUnit.check(new SymbolSysYTable(null), false);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        List<SysYException> errors = new ArrayList<SysYException>() {{
            addAll(tokenizer.errors);
            addAll(parser.errors);
            addAll(SysYTree.errors);
            sort(Comparator.comparingInt(SysYException::getLine));
        }};
        writer.writeErrors(errors);
         */

        builder.generateLLVM(compUnit);
        mipsBuilder.genModule();

        writer.close();
    }
}

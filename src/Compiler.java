import backend.MIPSBuilder;
import frontend.symbolTable.SymbolSysYTable;
import frontend.tree.SysYTree;
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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

        boolean errorHandle = true;

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


        if (errorHandle) {
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
        } else {
            builder.generateLLVM(compUnit);
            mipsBuilder.genModule();
        }
        writer.close();
    }
}

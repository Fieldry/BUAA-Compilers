package backend;

import java.util.*;

import midend.mir.*;
import midend.mir.Module;
import backend.MIPSCode.*;
import backend.Address.*;
import utils.Writer;
import utils.inodelist.INode;

public class MIPSBuilder {
    private final Module mirModule;
    private final Module lirModule = new Module();

    private final Writer writer;

    private final ArrayList<FunctionBuilder> functionBuilders = new ArrayList<>();
    private final LinkedHashMap<String, Address> globalMem = new LinkedHashMap<>();

    public MIPSBuilder(Writer writer, Module module) {
        mirModule = module;
        this.writer = writer;
    }

    public void genModule() {
        writer.setMipsBw();
        writer.writeln(".data:");
        for (GlobalVariable value : mirModule.getGlobalList()) {
            {
                String name = value.getName().replace("@", "");
                writer.write("\t" + name);
                writer.writeln(": " + value.getInitValue().toMIPS(true));
                globalMem.put(value.getName(), new LabelAddress(new Label(name), Registers.Register.R0));
            }
        }

        writer.writeln("");
        writer.writeln(".text:");


        boolean isMain;
        for (Function function : mirModule.getFunctionList()) {
            isMain = function.getName().equals("@main");
            FunctionBuilder functionBuilder = new FunctionBuilder(function, isMain, globalMem);
            lirModule.addFunction(functionBuilder.firstPass(lirModule));
            functionBuilders.add(functionBuilder);
        }
        for (FunctionBuilder functionBuilder : functionBuilders) {
            functionBuilder.secondPass();
        }

        for (Function function : lirModule.getFunctionList()) {
            writer.writeln("Function_" + function.getName() + ":");
            for (BasicBlock block : function.getBBlockList()) {
                writer.writeln(block + ":");
                for (INode iNode : block.getInstList()) {
                    if (iNode instanceof NopCode) {
                        iNode.remove();
                    } else {
                        writer.writeln("\t" + iNode);
                    }
                }
            }
            writer.writeln("");
        }
    }
}

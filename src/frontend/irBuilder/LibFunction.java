package frontend.irBuilder;

import frontend.irBuilder.Type.*;

import java.util.Arrays;
import java.util.Objects;

public class LibFunction extends Function {
    public static LibFunction GET_INT = new LibFunction(IntType.INT32_TYPE, "getint");
    public static LibFunction PUT_CH = new LibFunction(VoidType.VOID_TYPE, "putch", IntType.INT32_TYPE);
    public static LibFunction PUT_INT = new LibFunction(VoidType.VOID_TYPE, "putint", IntType.INT32_TYPE);
    public static LibFunction PUT_ARRAY = new LibFunction(VoidType.VOID_TYPE, "putarray",
            IntType.INT32_TYPE, new PointerType(IntType.INT32_TYPE));

    private final Type[] params;

    private LibFunction(Type type, String name, Type... params) {
        this.type = type;
        this.name = "@" + name;
        this.params = params;
    }

    @Override
    public String toString() {
        return "declare " + type + " " + name + "(" +
                Arrays.stream(params).map(Objects::toString).reduce((x, y) -> x + ", " + y).orElse("") + ")";
    }
}

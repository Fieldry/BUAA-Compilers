package exception;

public class SysYException extends Exception {
    public enum EKind {
        a("非法符号"),
        b("名字重定义"),
        c("未定义的名字"),
        d("函数参数个数不匹配"),
        e("函数参数类型不匹配"),
        f("无返回值函数有不匹配return"),
        g("有返回值函数无return"),
        h("不能改变常量的值"),
        i("缺少分号"),
        j("缺少右小括号"),
        k("缺少右中括号"),
        l("格式字符串与表达式个数不匹配"),
        m("非循环语句中break和continue"),
        o("other");

        public final String name;

        EKind(String name) {
            this.name = name;
        }

        @Override
        public String toString() { return this.name(); }
    }

    public EKind kind;
    public int line;

    public SysYException(EKind kind, int line) {
        this.kind = kind;
        this.line = line;
    }

    public SysYException(EKind kind) {
        this.kind = kind;
    }

    public void setLine(int line) {
        this.line = line;
    }

    public int getLine() { return this.line; }

    public EKind getKind() { return kind; }

    @Override
    public String toString() {
        return kind + " " + line;
    }
}

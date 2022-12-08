package frontend.tree;

import frontend.exception.SysYException;
import frontend.exception.SysYException.EKind;
import frontend.symbolTable.SymbolSysYTable;
import frontend.symbolTable.SymbolSysYTable.STKind;
import frontend.token.Tokens.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Root class for abstract syntax frontend.tree nodes. It provides definitions
 * for specific frontend.tree nodes as subclasses nested inside.
 */

public abstract class SysYTree {
    public static final List<SysYException> errors = new ArrayList<>();

    public SymbolSysYTable check(SymbolSysYTable table, boolean inLoop) {
        return table;
    }

    public static class SysYCompilationUnit extends SysYTree {
        private final List<SysYBlockItem> decls;
        private final List<SysYSymbol> funcDefs;
        private SysYSymbol mainFuncDef;

        public SysYCompilationUnit() {
            decls = new ArrayList<>();
            funcDefs = new ArrayList<>();
            mainFuncDef = null;
        }

        public List<SysYBlockItem> getDecls() { return decls; }

        public List<SysYSymbol> getFuncDefs() { return funcDefs; }

        public SysYSymbol getMainFuncDef() { return mainFuncDef;}

        public void addDecl(SysYBlockItem decl) { decls.add(decl); }
        public void addFuncDef(SysYSymbol funcDef) { funcDefs.add(funcDef); }
        public void setMainFuncDef(SysYSymbol mainFuncDef) { this.mainFuncDef = mainFuncDef; }

        @Override
        public SymbolSysYTable check(SymbolSysYTable table, boolean inLoop) {
            for (SysYBlockItem decl : decls) {
                if (decl != null) table = decl.check(table, inLoop);
            }
            for (SysYSymbol symbol : funcDefs) {
                if (symbol != null) table = symbol.check(table, inLoop);
            }
            return mainFuncDef.check(table, inLoop);
        }
    }

    /** Base class for declarations and statements. */
    public static class SysYBlockItem extends SysYTree {}

    /** Base class for statements. */
    public static class SysYStatement extends SysYBlockItem {}

    /** Variables declarations including const var and var. */
    public static class SysYDecl extends SysYBlockItem {
        private final List<SysYSymbol> defs;

        public SysYDecl(boolean isConst, List<SysYSymbol> defs) {
            this.defs = defs;
        }

        public List<SysYSymbol> getDefs() {
            return defs;
        }

        @Override
        public SymbolSysYTable check(SymbolSysYTable table, boolean inLoop) {
            for (SysYSymbol def : defs) {
                if (def != null) table = def.check(table, inLoop);
            }
            return table;
        }
    }

    /** Base class for variables and function definitions, built for symbol table. */
    public static abstract class SysYSymbol extends SysYTree {
        public enum SymbolKind {
            CONST,
            VARIABLE,
            FUNCTION,
            PARAMETER,
            MAIN_FUNCTION
        }

        protected SysYIdentifier ident;

        public String getName() { return ident.getValue(); }

        @Override
        public String toString() { return ident.getLine() + " " + ident.getValue(); }

        public abstract SymbolKind getKind();
    }

    /** Variables definitions including const var and var. */
    public static class SysYDef extends SysYSymbol {
        private final boolean isConst;
        private final int dimensions;
        private final SysYExpression firstExp;
        private final SysYExpression secondExp;
        private final SysYExpression init;

        public SysYDef(boolean isConst, SysYIdentifier ident, int dimensions,
                        SysYExpression firstExp, SysYExpression secondExp, SysYExpression init) {
            this.isConst = isConst;
            this.ident = ident;
            this.dimensions = dimensions;
            this.firstExp = firstExp;
            this.secondExp = secondExp;
            this.init = init;
        }

        public boolean isConst() { return isConst; }

        public int getDimensions() {
            return dimensions;
        }

        public SysYExpression getFirstExp() {
            return firstExp;
        }

        public SysYExpression getSecondExp() {
            return secondExp;
        }

        public SysYExpression getInit() {
            return init;
        }

        @Override
        public SymbolKind getKind() {
            return isConst ? SymbolKind.CONST : SymbolKind.VARIABLE;
        }

        @Override
        public SymbolSysYTable check(SymbolSysYTable table, boolean inLoop) {
            try {
                table.addSymbol(ident.getValue(), this);
            } catch (SysYException e) {
                e.setLine(ident.getLine());
                errors.add(e);
            }
            return table;
        }
    }

    public static class SysYInit extends SysYExpression {
        private final List<SysYExpression> expression;

        public SysYInit(boolean isConst, List<SysYExpression> expression) {
            this.expression = expression;
        }

        public List<SysYExpression> getExpression() {
            return expression;
        }

        @Override
        public ReturnKind getReturnKind(SymbolSysYTable table) {
            return null;
        }
    }

    /** Function definitions including void and int. */
    public static class SysYFuncDef extends SysYSymbol {
        private final boolean returnInt;
        private final List<SysYSymbol> funcParams;
        private final SysYStatement block;

        public SysYFuncDef(boolean returnInt, SysYIdentifier ident, List<SysYSymbol> funcParams, SysYStatement block) {
            this.returnInt = returnInt;
            this.ident = ident;
            this.funcParams = funcParams == null ? new ArrayList<>() : funcParams;
            this.block = block;
        }

        public boolean isReturnInt() {
            return returnInt;
        }

        public List<SysYSymbol> getFuncParams() {
            return funcParams;
        }

        public SysYStatement getBlock() {
            return block;
        }

        @Override
        public SymbolKind getKind() {
            return SymbolKind.FUNCTION;
        }

        @Override
        public SymbolSysYTable check(SymbolSysYTable table, boolean inLoop) {
            try {
                table.addSymbol(ident.getValue(), this);
            } catch (SysYException e) {
                e.setLine(ident.getLine());
                errors.add(e);
            }
            SymbolSysYTable sub = new SymbolSysYTable(table,
                    returnInt ? STKind.INT_FUNC : STKind.VOID_FUNC);
            for (SysYSymbol param : funcParams) {
                if (param != null) sub = param.check(sub, inLoop);
            }
            if (block != null) block.check(sub, inLoop);
            return table;
        }
    }

    public static class SysYFuncParam extends SysYSymbol {
        private final int dimensions;
        private SysYExpression secondExp;

        public SysYFuncParam(SysYIdentifier ident, int dimensions) {
            this.ident = ident;
            this.dimensions = dimensions;
        }

        public SysYFuncParam(SysYIdentifier ident, int dimensions, SysYExpression secondExp) {
            this.ident = ident;
            this.dimensions = dimensions;
            this.secondExp = secondExp;
        }

        public int getDimensions() {
            return dimensions;
        }

        public SysYExpression getSecondExp() {
            return secondExp;
        }

        @Override
        public SymbolKind getKind() {
            return SymbolKind.PARAMETER;
        }

        @Override
        public SymbolSysYTable check(SymbolSysYTable table, boolean inLoop) {
            try {
                table.addSymbol(ident.getValue(), this);
            } catch (SysYException e) {
                e.setLine(ident.getLine());
                errors.add(e);
            }
            return table;
        }
    }

    public static class SysYMainFuncDef extends SysYSymbol {
        private final SysYStatement block;

        public SysYMainFuncDef(SysYStatement block) {
            this.block = block;
        }

        public SysYStatement getBlock() { return block; }

        @Override
        public SymbolKind getKind() {
            return SymbolKind.MAIN_FUNCTION;
        }

        @Override
        public SymbolSysYTable check(SymbolSysYTable table, boolean inLoop) {
            if (block != null) block.check(new SymbolSysYTable(table, STKind.INT_FUNC), inLoop);
            return table;
        }
    }

    public static class SysYAssign extends SysYStatement {
        private final SysYLVal lVal;
        private final SysYExpression expression;

        public SysYAssign(SysYLVal lVal, SysYExpression expression) {
            this.lVal = lVal;
            this.expression = expression;
        }

        public SysYLVal getlVal() { return lVal; }

        public SysYExpression getExpression() { return expression; }

        @Override
        public SymbolSysYTable check(SymbolSysYTable table, boolean inLoop) {
            if (lVal != null) {
                lVal.check(table, inLoop);
                SysYSymbol symbol = table.findSymbolInAll(lVal.getName());
                if (symbol != null && symbol.getKind() == SysYSymbol.SymbolKind.CONST) {
                    errors.add(new SysYException(EKind.h, lVal.getLine()));
                }
            }
            return table;
        }
    }

    public static class SysYBlock extends SysYStatement {
        private final List<SysYBlockItem> block;
        private final int endLine;

        public SysYBlock(List<SysYBlockItem> block, int endLine) {
            this.block = block == null ? new ArrayList<>() : block;
            this.endLine = endLine;
        }

        public List<SysYBlockItem> getBlock() { return block; }

        @Override
        public SymbolSysYTable check(SymbolSysYTable table, boolean inLoop) {
            SymbolSysYTable sub = new SymbolSysYTable(table);
            for (SysYBlockItem item : block) {
                if (item != null) {
                    sub = item.check(sub, inLoop);
                    if (table.getKind() == STKind.VOID_FUNC && (item instanceof SysYReturn) &&
                            ((SysYReturn) item).expression != null ) {
                        errors.add(new SysYException(EKind.f, ((SysYReturn) item).getLine()));
                    }
                }
            }
            if (table.getKind() == STKind.INT_FUNC &&
                    (block.isEmpty() || !(block.get(block.size() - 1) instanceof SysYReturn))) {
                    errors.add(new SysYException(EKind.g, endLine));
                }

            return table;
        }
    }

    public static class SysYIf extends SysYStatement {
        private final SysYExpression cond;
        private final SysYStatement thenStmt;
        private final SysYStatement elseStmt;

        public SysYIf(SysYExpression cond, SysYStatement thenStmt, SysYStatement elseStmt) {
            this.cond = cond;
            this.thenStmt = thenStmt;
            this.elseStmt = elseStmt;
        }

        @Override
        public SymbolSysYTable check(SymbolSysYTable table, boolean inLoop) {
            if (thenStmt != null) table = thenStmt.check(table, inLoop);
            if (elseStmt != null) table = elseStmt.check(table, inLoop);
            return table;
        }

        public SysYExpression getCond() {
            return cond;
        }

        public SysYStatement getThenStmt() {
            return thenStmt;
        }

        public SysYStatement getElseStmt() {
            return elseStmt;
        }
    }

    public static class SysYWhile extends SysYStatement {
        private final SysYExpression cond;
        private final SysYStatement stmt;

        public SysYWhile(SysYExpression cond, SysYStatement stmt) {
            this.cond = cond;
            this.stmt = stmt;
        }

        public SysYExpression getCond() {
            return cond;
        }

        public SysYStatement getStmt() {
            return stmt;
        }

        @Override
        public SymbolSysYTable check(SymbolSysYTable table, boolean inLoop) {
            if (stmt != null) table = stmt.check(table, true);
            return table;
        }
    }

    public static class SysYBreak extends SysYStatement {
        private int line;

        public SysYBreak() {}

        public SysYBreak(int line) {
            this.line = line;
        }

        @Override
        public SymbolSysYTable check(SymbolSysYTable table, boolean inLoop) {
            if (!inLoop) {
                errors.add(new SysYException(EKind.m, line));
            }
            return table;
        }
    }

    public static class SysYContinue extends SysYStatement {
        public int line;

        public SysYContinue(int line) {
            this.line = line;
        }

        @Override
        public SymbolSysYTable check(SymbolSysYTable table, boolean inLoop) {
            if (!inLoop) {
                errors.add(new SysYException(EKind.m, line));
            }
            return table;
        }
    }

    public static class SysYReturn extends SysYStatement {
        private final int line;
        private final SysYExpression expression;

        public SysYReturn(int line, SysYExpression expression) {
            this.line = line;
            this.expression = expression;
        }

        public SysYExpression getExpression() {
            return expression;
        }

        public int getLine() {
            return line;
        }

        @Override
        public SymbolSysYTable check(SymbolSysYTable table, boolean inLoop) {
            return table;
        }
    }

    /** Base class for expressions. */
    public abstract static class SysYExpression extends SysYStatement {
        public enum ReturnKind {
            VOID,
            INT,
            ONE_DIM,
            TWO_DIM
        }

        protected boolean isFuncCall = false;

        public abstract ReturnKind getReturnKind(SymbolSysYTable table) throws SysYException;
        public boolean isFuncCall() { return isFuncCall; }
    }

    public static class SysYExpressionStatement extends SysYExpression {
        private final SysYExpression exp;
        private final boolean isEmpty;

        public SysYExpressionStatement() {
            exp = null;
            isEmpty = true;
        }

        public SysYExpressionStatement(SysYExpression exp) {
            this.exp = exp;
            isEmpty = false;
        }

        public boolean isEmpty() { return isEmpty; }

        public SysYExpression getExp() { return exp; }

        @Override
        public SymbolSysYTable check(SymbolSysYTable table, boolean inLoop) {
            if (!isEmpty) {
                assert exp != null;
                table = exp.check(table, inLoop);
            }
            return table;
        }

        @Override
        public ReturnKind getReturnKind(SymbolSysYTable table) throws SysYException {
            if (isEmpty) return null;
            else {
                assert exp != null;
                return exp.getReturnKind(table);
            }
        }
    }

    public static class SysYGetInt extends SysYExpression {
        @Override
        public ReturnKind getReturnKind(SymbolSysYTable table) {
            return ReturnKind.INT;
        }
    }

    public static class SysYPrintf extends SysYExpression {
        public int line;
        private final String format;
        private final List<SysYExpression> exps;

        public SysYPrintf(int line, String format, List<SysYExpression> exps) {
            this.line = line;
            this.format = format;
            this.exps = exps;
        }

        public String getFormat() {
            return format;
        }

        public List<SysYExpression> getExps() {
            return exps;
        }

        @Override
        public SymbolSysYTable check(SymbolSysYTable table, boolean inLoop) {
            if (format.split("%d").length - 1 != exps.size()) {
                errors.add(new SysYException(EKind.l, line));
            }
            return table;
        }

        @Override
        public ReturnKind getReturnKind(SymbolSysYTable table) {
            return ReturnKind.VOID;
        }
    }

    public static class SysYIdentifier extends SysYTree {
        private final Token ident;

        public SysYIdentifier(Token token) {
            ident = token;
        }

        public String getValue() {
            return ident.getValue();
        }

        public int getLine() { return ident.getLine(); }
    }

    public static class SysYIntC extends SysYExpression  {
        private final int value;

        public SysYIntC(int value) {
            this.value = value;
        }

        public int getValue() { return value; }

        @Override
        public ReturnKind getReturnKind(SymbolSysYTable table) {
            return ReturnKind.INT;
        }
    }

    public static class SysYLVal extends SysYExpression {
        private final SysYIdentifier ident;
        private final int dimensions;
        private SysYExpression firstExp;
        private SysYExpression secondExp;

        public SysYLVal(SysYIdentifier ident, int dimensions) {
            this.ident = ident;
            this.dimensions = dimensions;
        }

        public SysYLVal(SysYIdentifier ident, int dimensions, SysYExpression firstExp) {
            this.ident = ident;
            this.dimensions = dimensions;
            this.firstExp = firstExp;
        }

        public SysYLVal(SysYIdentifier ident, int dimensions, SysYExpression firstExp, SysYExpression secondExp) {
            this.ident = ident;
            this.dimensions = dimensions;
            this.firstExp = firstExp;
            this.secondExp = secondExp;
        }

        public SysYExpression getFirstExp() {
            return firstExp;
        }

        public SysYExpression getSecondExp() {
            return secondExp;
        }

        public int getLine() { return ident.getLine(); }

        public String getName() { return ident.getValue(); }

        @Override
        public SymbolSysYTable check(SymbolSysYTable table, boolean inLoop) {
            if (ident == null) return table;
            SysYSymbol symbol = table.findSymbolInAll(ident.getValue());
            if (symbol == null) {
                errors.add(new SysYException(EKind.c, ident.getLine()));
            }
            return table;
        }

        @Override
        public ReturnKind getReturnKind(SymbolSysYTable table) throws SysYException {
            SysYSymbol symbol = table.findSymbolInAll(ident.getValue());
            if (symbol != null) {
                int calDim;
                if (symbol instanceof SysYDef) calDim = ((SysYDef) symbol).getDimensions();
                else if (symbol instanceof SysYFuncParam) calDim = ((SysYFuncParam) symbol).getDimensions();
                else throw new SysYException(EKind.o);

                switch (this.dimensions - calDim) {
                    case 0:
                        return ReturnKind.INT;
                    case 1:
                        return ReturnKind.ONE_DIM;
                    case 2:
                        return ReturnKind.TWO_DIM;
                    default:
                        throw new SysYException(EKind.e);
                }
            } else throw new SysYException(EKind.c);
        }
    }

    public static class SysYFuncCall extends SysYExpression {
        private final SysYIdentifier ident;
        private final List<SysYExpression> funcRParams;

        public SysYFuncCall(SysYIdentifier ident, List<SysYExpression> funcParams) {
            this.ident = ident;
            this.funcRParams = funcParams == null ? new ArrayList<>() : funcParams;
            this.isFuncCall = true;
        }

        public String getName() {
            return ident.getValue();
        }

        public List<SysYExpression> getFuncRParams() {
            return funcRParams;
        }

        @Override
        public SymbolSysYTable check(SymbolSysYTable table, boolean inLoop) {
            SysYSymbol symbol = table.findSymbolInAll(ident.getValue());
            if (symbol == null) {
                errors.add(new SysYException(EKind.c, ident.getLine()));
            } else {
                SysYFuncDef funcDef = (SysYFuncDef) symbol;
                if (funcDef.funcParams.size() != funcRParams.size()) {
                    errors.add(new SysYException(EKind.d, ident.getLine()));
                } else
                for (int i = 0, size = funcRParams.size(); i < size; i++ ) {
                    SysYFuncParam funcParam = (SysYFuncParam) funcDef.funcParams.get(i);
                    switch (funcParam.dimensions) {
                        case 2: {
                            try {
                                if (funcRParams.get(i) == null ||
                                        funcRParams.get(i).getReturnKind(table) != ReturnKind.TWO_DIM) {
                                    errors.add(new SysYException(EKind.e, ident.getLine()));
                                }
                            } catch (SysYException e) {
                                errors.add(new SysYException(EKind.e, ident.getLine()));
                            }
                            break;
                        }
                        case 1: {
                            try {
                                if (funcRParams.get(i) == null ||
                                        funcRParams.get(i).getReturnKind(table) != ReturnKind.ONE_DIM) {
                                    errors.add(new SysYException(EKind.e, ident.getLine()));
                                }
                            } catch (SysYException e) {
                                errors.add(new SysYException(EKind.e, ident.getLine()));
                            }
                            break;
                        }
                        case 0: {
                            try {
                                if (funcRParams.get(i) == null ||
                                        funcRParams.get(i).getReturnKind(table) != ReturnKind.INT) {
                                    errors.add(new SysYException(EKind.e, ident.getLine()));
                                }
                            } catch (SysYException e) {
                                errors.add(new SysYException(EKind.e, ident.getLine()));
                            }
                            break;
                        }
                    }
                }
            }

            return table;
        }

        @Override
        public ReturnKind getReturnKind(SymbolSysYTable table) throws SysYException {
            SysYSymbol symbol = table.findSymbolInAll(ident.getValue());
            if (symbol != null && symbol.getKind() == SysYSymbol.SymbolKind.FUNCTION) {
                return ((SysYFuncDef) symbol).returnInt ? ReturnKind.INT : ReturnKind.VOID;
            }
            throw new SysYException(EKind.c);
        }
    }

    public static class SysYUnaryExp extends SysYExpression {
        private final Token unaryOp;
        private final SysYExpression unaryExp;

        public SysYUnaryExp(Token unaryOp, SysYExpression unaryExp) {
            this.unaryOp = unaryOp;
            this.unaryExp = unaryExp;
            this.isFuncCall = unaryExp.isFuncCall;
        }

        public Token getUnaryOp() { return unaryOp; }

        public SysYExpression getUnaryExp() { return unaryExp; }

        @Override
        public ReturnKind getReturnKind(SymbolSysYTable table) throws SysYException {
            if (unaryExp != null && unaryExp.getReturnKind(table) == ReturnKind.INT) return ReturnKind.INT;
            else throw new SysYException(EKind.e);
        }

        @Override
        public SymbolSysYTable check(SymbolSysYTable table, boolean inLoop) {
            if (unaryExp != null)
                return unaryExp.check(table, inLoop);
            else return table;
        }
    }

    public static class SysYBinaryExp extends SysYExpression {
        protected SysYExpression leftExp;
        protected SysYExpression rightExp;
        protected Token token;

        public SysYBinaryExp(SysYExpression leftExp) {
            this.leftExp = leftExp;
            this.isFuncCall = leftExp.isFuncCall;
        }

        public SysYBinaryExp(Token token, SysYExpression leftExp, SysYExpression rightExp) {
            this.leftExp = leftExp;
            this.rightExp = rightExp;
            this.token = token;
            this.isFuncCall = leftExp.isFuncCall | rightExp.isFuncCall;
        }

        public Token getToken() {
            return token;
        }

        public SysYExpression getLeftExp() {
            return leftExp;
        }

        public SysYExpression getRightExp() {
            return rightExp;
        }

        @Override
        public ReturnKind getReturnKind(SymbolSysYTable table) throws SysYException {
            ReturnKind leftRet = leftExp.getReturnKind(table);
            ReturnKind rightRet = rightExp == null ? null : rightExp.getReturnKind(table);
            if (rightRet != null && leftRet != rightRet) throw new SysYException(EKind.e);
            else if (leftRet == ReturnKind.INT) return ReturnKind.INT;
            else throw new SysYException(EKind.e);
        }

        @Override
        public SymbolSysYTable check(SymbolSysYTable table, boolean inLoop) {
            if (leftExp != null) table = leftExp.check(table, inLoop);
            if (rightExp != null) table = rightExp.check(table, inLoop);
            return table;
        }
    }

    public static class SysYMulExp extends SysYBinaryExp {
        public SysYMulExp(SysYExpression leftExp) {
            super(leftExp);
        }

        public SysYMulExp(Token token, SysYExpression leftExp, SysYExpression rightExp) {
            super(token, leftExp, rightExp);
        }
    }

    public static class SysYAddExp extends SysYBinaryExp {
        public SysYAddExp(SysYExpression leftExp) {
            super(leftExp);
        }

        public SysYAddExp(Token token, SysYExpression leftExp, SysYExpression rightExp) {
            super(token, leftExp, rightExp);
        }
    }

    public static class SysYRelExp extends SysYBinaryExp {
        public SysYRelExp(SysYExpression leftExp) {
            super(leftExp);
        }

        public SysYRelExp(Token token, SysYExpression leftExp, SysYExpression rightExp) {
            super(token, leftExp, rightExp);
        }
    }

    public static class SysYEqExp extends SysYBinaryExp {
        public SysYEqExp(SysYExpression leftExp) {
            super(leftExp);
        }

        public SysYEqExp(Token token, SysYExpression leftExp, SysYExpression rightExp) {
            super(token, leftExp, rightExp);
        }
    }

    public static class SysYLAndExp extends SysYBinaryExp {
        public SysYLAndExp(SysYExpression leftExp) {
            super(leftExp);
        }

        public SysYLAndExp(SysYExpression leftExp, SysYExpression rightExp) {
            super(new Token(TokenKind.AND), leftExp, rightExp);
        }
    }

    public static class SysYLOrExp extends SysYBinaryExp {
        public SysYLOrExp(SysYExpression leftExp) {
            super(leftExp);
        }

        public SysYLOrExp(SysYExpression leftExp, SysYExpression rightExp) {
            super(new Token(TokenKind.OR), leftExp, rightExp);
        }
    }

    public static class SysYCond extends SysYExpression {
        private final SysYExpression cond;

        public SysYCond(SysYExpression cond) { this.cond = cond; }

        public SysYExpression getCond() {
            return cond;
        }

        @Override
        public ReturnKind getReturnKind(SymbolSysYTable table) throws SysYException {
            if (cond != null && cond.getReturnKind(table) == ReturnKind.INT) return ReturnKind.INT;
            else throw new SysYException(EKind.e);
        }

        @Override
        public SymbolSysYTable check(SymbolSysYTable table, boolean inLoop) {
            if (cond != null)
                return cond.check(table, inLoop);
            else return table;
        }
    }
}

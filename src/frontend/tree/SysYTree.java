package frontend.tree;

import frontend.exception.SysYException;
import frontend.exception.SysYException.EKind;
import frontend.irBuilder.Module;
import frontend.symbolTable.SymbolTable;
import frontend.symbolTable.SymbolTable.STKind;
import frontend.token.Tokens.*;
import frontend.irBuilder.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Root class for abstract syntax frontend.tree nodes. It provides definitions
 * for specific frontend.tree nodes as subclasses nested inside.
 */

public abstract class SysYTree {
    public static final List<SysYException> errors = new ArrayList<>();

    public SymbolTable check(SymbolTable table, boolean inLoop) {
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

        public void addDecl(SysYBlockItem decl) { decls.add(decl); }
        public void addFuncDef(SysYSymbol funcDef) { funcDefs.add(funcDef); }
        public void setMainFuncDef(SysYSymbol mainFuncDef) { this.mainFuncDef = mainFuncDef; }

        @Override
        public SymbolTable check(SymbolTable table, boolean inLoop) {
            for (SysYBlockItem decl : decls) {
                table = decl.check(table, inLoop);
            }
            for (SysYSymbol symbol : funcDefs) {
                table = symbol.check(table, inLoop);
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
        public boolean isConst;
        List<SysYSymbol> defs;

        public SysYDecl(boolean isConst, List<SysYSymbol> defs) {
            this.isConst = isConst;
            this.defs = defs;
        }

        @Override
        public SymbolTable check(SymbolTable table, boolean inLoop) {
            for (SysYSymbol def : defs) {
                table = def.check(table, inLoop);
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

        public SysYIdentifier ident;

        public int getLine() { return ident.getLine(); }

        @Override
        public String toString() { return ident.getLine() + " " + ident.getName(); }

        public abstract SymbolKind getKind();
    }

    /** Variables definitions including const var and var. */
    public static class SysYDef extends SysYSymbol {
        public boolean isConst;
        public int dimensions;
        public SysYExpression firstExp;
        public SysYExpression secondExp;
        public SysYExpression init;

        public SysYDef(boolean isConst, SysYIdentifier ident, int dimensions,
                        SysYExpression firstExp, SysYExpression secondExp, SysYExpression init) {
            this.isConst = isConst;
            this.ident = ident;
            this.dimensions = dimensions;
            this.firstExp = firstExp;
            this.secondExp = secondExp;
            this.init = init;
        }

        public int getDimensions() {
            return dimensions;
        }

        @Override
        public SymbolKind getKind() {
            return isConst ? SymbolKind.CONST : SymbolKind.VARIABLE;
        }

        @Override
        public SymbolTable check(SymbolTable table, boolean inLoop) {
            try {
                table.addSymbol(ident.getName(), this);
            } catch (SysYException e) {
                e.setLine(ident.getLine());
                errors.add(e);
            }
            return table;
        }
    }

    public static class SysYInit extends SysYExpression {
        public boolean isConst;
        public List<SysYExpression> expression;

        public SysYInit(boolean isConst, List<SysYExpression> expression) {
            this.isConst = isConst;
            this.expression = expression;
        }

        @Override
        public ReturnKind getReturnKind(SymbolTable table) {
            return null;
        }
    }

    /** Function definitions including void and int. */
    public static class SysYFuncDef extends SysYSymbol {
        public boolean returnInt;
        public List<SysYSymbol> funcParams;
        public SysYStatement block;

        public SysYFuncDef(boolean returnInt, SysYIdentifier ident, List<SysYSymbol> funcParams, SysYStatement block) {
            this.returnInt = returnInt;
            this.ident = ident;
            this.funcParams = funcParams;
            this.block = block;
        }

        @Override
        public SymbolKind getKind() {
            return SymbolKind.FUNCTION;
        }

        @Override
        public SymbolTable check(SymbolTable table, boolean inLoop) {
            try {
                table.addSymbol(ident.getName(), this);
            } catch (SysYException e) {
                e.setLine(ident.getLine());
                errors.add(e);
            }
            SymbolTable sub = new SymbolTable(table,
                    returnInt ? STKind.INT_FUNC : STKind.VOID_FUNC);
            if (funcParams != null)
                for (SysYSymbol param : funcParams) {
                    sub = param.check(sub, inLoop);
                }
            block.check(sub, inLoop);
            return table;
        }
    }

    public static class SysYFuncParam extends SysYSymbol {
        public int dimensions;
        public SysYExpression secondExp;

        public SysYFuncParam(SysYIdentifier ident, int dimensions) {
            this.ident = ident;
            this.dimensions = dimensions;
        }

        public SysYFuncParam(SysYIdentifier ident, int dimensions, SysYExpression secondExp) {
            this.ident = ident;
            this.dimensions = dimensions;
            this.secondExp = secondExp;
        }

        @Override
        public SymbolKind getKind() {
            return SymbolKind.PARAMETER;
        }

        @Override
        public SymbolTable check(SymbolTable table, boolean inLoop) {
            try {
                table.addSymbol(ident.getName(), this);
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
        public SymbolTable check(SymbolTable table, boolean inLoop) {
            block.check(new SymbolTable(table, STKind.INT_FUNC), inLoop);
            return table;
        }
    }

    public static class SysYAssign extends SysYStatement {
        public SysYLVal lVal;
        public SysYExpression expression;

        public SysYAssign(SysYLVal lVal, SysYExpression expression) {
            this.lVal = lVal;
            this.expression = expression;
        }

        @Override
        public SymbolTable check(SymbolTable table, boolean inLoop) {
            lVal.check(table, inLoop);
            SysYSymbol symbol = table.findSymbolInAll(lVal.getName());
            if (symbol != null && symbol.getKind() == SysYSymbol.SymbolKind.CONST) {
                errors.add(new SysYException(EKind.h, lVal.getLine()));
            }
            return table;
        }
    }

    public static class SysYBlock extends SysYStatement {
        private final List<SysYBlockItem> block;
        private final int endLine;

        public SysYBlock(List<SysYBlockItem> block, int endLine) {
            this.block = block;
            this.endLine = endLine;
        }

        public List<SysYBlockItem> getBlock() { return block; }

        @Override
        public SymbolTable check(SymbolTable table, boolean inLoop) {
            SymbolTable sub = new SymbolTable(table);
            for (SysYBlockItem item : block) {
                sub = item.check(sub, inLoop);
                if (table.kind == STKind.VOID_FUNC
                    && (item instanceof SysYReturn) && ((SysYReturn) item).expression != null) {
                    errors.add(new SysYException(EKind.f, ((SysYReturn) item).getLine()));
                }
            }
            if (table.kind == STKind.INT_FUNC
                && (block.isEmpty() || !(block.get(block.size() - 1) instanceof SysYReturn))) {
                errors.add(new SysYException(EKind.g, endLine));
            }
            return table;
        }
    }

    public static class SysYIf extends SysYStatement {
        public SysYExpression cond;
        public SysYStatement thenStmt;
        public SysYStatement elseStmt;

        public SysYIf(SysYExpression cond, SysYStatement thenStmt, SysYStatement elseStmt) {
            this.cond = cond;
            this.thenStmt = thenStmt;
            this.elseStmt = elseStmt;
        }

        @Override
        public SymbolTable check(SymbolTable table, boolean inLoop) {
            table = cond.check(table, inLoop);
            table = thenStmt.check(table, inLoop);
            if (elseStmt != null) table = elseStmt.check(table, inLoop);
            return table;
        }
    }

    public static class SysYWhile extends SysYStatement {
        public SysYExpression cond;
        public SysYStatement stmt;

        public SysYWhile(SysYExpression cond, SysYStatement stmt) {
            this.cond = cond;
            this.stmt = stmt;
        }

        @Override
        public SymbolTable check(SymbolTable table, boolean inLoop) {
            table = cond.check(table, inLoop);
            table = stmt.check(table, true);
            return table;
        }
    }

    public static class SysYBreak extends SysYStatement {
        public int line;

        public SysYBreak(int line) {
            this.line = line;
        }

        @Override
        public SymbolTable check(SymbolTable table, boolean inLoop) {
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
        public SymbolTable check(SymbolTable table, boolean inLoop) {
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
    }

    /** Base class for expressions. */
    public abstract static class SysYExpression extends SysYStatement {
        public enum ReturnKind {
            VOID,
            INT,
            ONE_DIM,
            TWO_DIM
        }

        public abstract ReturnKind getReturnKind(SymbolTable table) throws SysYException;
    }

    public static class SysYExpressionStatement extends SysYExpression {
        public SysYExpression exp;
        public boolean isEmpty;

        public SysYExpressionStatement() { isEmpty = true; }
        public SysYExpressionStatement(SysYExpression exp) {
            this.exp = exp;
            isEmpty = false;
        }

        @Override
        public SymbolTable check(SymbolTable table, boolean inLoop) {
            if (!isEmpty) table = exp.check(table, inLoop);
            return table;
        }

        @Override
        public ReturnKind getReturnKind(SymbolTable table) throws SysYException {
            return isEmpty ? null : exp.getReturnKind(table);
        }
    }

    public static class SysYGetInt extends SysYExpression {
        @Override
        public ReturnKind getReturnKind(SymbolTable table) {
            return ReturnKind.INT;
        }
    }

    public static class SysYPrintf extends SysYExpression {
        public int line;
        public String format;
        public List<SysYExpression> exps;

        public SysYPrintf(int line, String format, List<SysYExpression> exps) {
            this.line = line;
            this.format = format;
            this.exps = exps;
        }

        @Override
        public SymbolTable check(SymbolTable table, boolean inLoop) {
            if (format.split("%d").length - 1 != exps.size()) {
                errors.add(new SysYException(EKind.l, line));
            }
            return table;
        }

        @Override
        public ReturnKind getReturnKind(SymbolTable table) {
            return ReturnKind.VOID;
        }
    }

    public static class SysYIdentifier extends SysYTree {
        public Token name;

        public SysYIdentifier(Token token) {
            name = token;
        }

        public String getName() {
            return name.value;
        }

        public int getLine() { return name.line; }
    }

    public static class SysYIntC extends SysYExpression  {
        private final int value;

        public SysYIntC(int value) {
            this.value = value;
        }

        public int getValue() { return value; }

        @Override
        public ReturnKind getReturnKind(SymbolTable table) {
            return ReturnKind.INT;
        }
    }

    public static class SysYLVal extends SysYExpression {
        public SysYIdentifier ident;
        public int dimensions;
        public SysYExpression firstExp;
        public SysYExpression secondExp;

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

        public SysYIdentifier getIdent() {
            return ident;
        }

        public int getLine() { return ident.getLine(); }

        public String getName() { return ident.getName(); }

        @Override
        public SymbolTable check(SymbolTable table, boolean inLoop) {
            SysYSymbol symbol = table.findSymbolInAll(ident.getName());
            if (symbol == null) {
                errors.add(new SysYException(EKind.c, ident.getLine()));
            }
            return table;
        }

        @Override
        public ReturnKind getReturnKind(SymbolTable table) throws SysYException {
            SysYSymbol symbol = table.findSymbolInAll(ident.getName());
            if (symbol != null) {
                int calDim = ((SysYDef) symbol).getDimensions();
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
        public SysYIdentifier ident;
        public List<SysYExpression> funcRParams;

        public SysYFuncCall(SysYIdentifier ident, List<SysYExpression> funcParams) {
            this.ident = ident;
            this.funcRParams = funcParams;
        }

        @Override
        public SymbolTable check(SymbolTable table, boolean inLoop) {
            SysYSymbol symbol = table.findSymbolInAll(ident.getName());
            if (symbol == null) {
                errors.add(new SysYException(EKind.c, ident.getLine()));
            } else {
                SysYFuncDef funcDef = (SysYFuncDef) symbol;
                if (funcDef.funcParams.size() != funcRParams.size()) {
                    errors.add(new SysYException(EKind.d, ident.getLine()));
                }
                for (int i = 0, size = Integer.min(funcDef.funcParams.size(), funcRParams.size()); i < size; i++ ) {
                    SysYFuncParam funcParam = (SysYFuncParam) funcDef.funcParams.get(i);
                    switch (funcParam.dimensions) {
                        case 2: {
                            try {
                                if (funcRParams.get(i).getReturnKind(table) != ReturnKind.TWO_DIM) {
                                    errors.add(new SysYException(EKind.e, ident.getLine()));
                                }
                            } catch (SysYException e) {
                                errors.add(new SysYException(EKind.e, ident.getLine()));
                            }
                        }
                        case 1: {
                            try {
                                if (funcRParams.get(i).getReturnKind(table) != ReturnKind.ONE_DIM) {
                                    errors.add(new SysYException(EKind.e, ident.getLine()));
                                }
                            } catch (SysYException e) {
                                errors.add(new SysYException(EKind.e, ident.getLine()));
                            }
                        }
                        case 0: {
                            try {
                                if (funcRParams.get(i).getReturnKind(table) != ReturnKind.INT) {
                                    errors.add(new SysYException(EKind.e, ident.getLine()));
                                }
                            } catch (SysYException e) {
                                errors.add(new SysYException(EKind.e, ident.getLine()));
                            }
                        }
                    }
                }
            }

            return table;
        }

        @Override
        public ReturnKind getReturnKind(SymbolTable table) throws SysYException {
            SysYSymbol symbol = table.findSymbolInAll(ident.getName());
            if (symbol != null && symbol.getKind() == SysYSymbol.SymbolKind.FUNCTION) {
                return ((SysYFuncDef) symbol).returnInt ? ReturnKind.INT : ReturnKind.VOID;
            }
            throw new SysYException(EKind.c);
        }
    }

    public static class SysYUnaryExp extends SysYExpression {
        public Token unaryOp;
        public SysYExpression unaryExp;

        public SysYUnaryExp(Token unaryOp, SysYExpression unaryExp) {
            this.unaryOp = unaryOp;
            this.unaryExp = unaryExp;
        }

        @Override
        public ReturnKind getReturnKind(SymbolTable table) throws SysYException {
            if (unaryExp.getReturnKind(table) == ReturnKind.INT) return ReturnKind.INT;
            else throw new SysYException(EKind.e);
        }
    }

    public static class SysYBinaryExp extends SysYExpression {
        protected SysYExpression leftExp;
        protected SysYExpression rightExp;
        protected Token token;

        public SysYBinaryExp() {}

        public SysYBinaryExp(Token token, SysYExpression leftExp, SysYExpression rightExp) {
            this.leftExp = leftExp;
            this.rightExp = rightExp;
            this.token = token;
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
        public ReturnKind getReturnKind(SymbolTable table) throws SysYException {
            ReturnKind leftRet = leftExp.getReturnKind(table);
            ReturnKind rightRet = rightExp.getReturnKind(table);
            if (leftRet != rightRet) throw new SysYException(EKind.e);
            if (leftRet == ReturnKind.INT) return ReturnKind.INT;
            throw new SysYException(EKind.e);
        }
    }

    public static class SysYMulExp extends SysYBinaryExp {
        public SysYMulExp(SysYExpression leftExp) {
            this.leftExp = leftExp;
        }

        public SysYMulExp(Token token, SysYExpression leftExp, SysYExpression rightExp) {
            super(token, leftExp, rightExp);
        }
    }

    public static class SysYAddExp extends SysYBinaryExp {
        public Token token;

        public SysYAddExp(SysYExpression leftExp) {
            this.leftExp = leftExp;
        }

        public SysYAddExp(Token token, SysYExpression leftExp, SysYExpression rightExp) {
            this.token = token;
            this.leftExp = leftExp;
            this.rightExp = rightExp;
        }
    }

    public static class SysYRelExp extends SysYBinaryExp {
        public Token token;

        public SysYRelExp(SysYExpression leftExp) {
            this.leftExp = leftExp;
        }

        public SysYRelExp(Token token, SysYExpression leftExp, SysYExpression rightExp) {
            this.token = token;
            this.leftExp = leftExp;
            this.rightExp = rightExp;
        }
    }

    public static class SysYEqExp extends SysYBinaryExp {
        public Token token;

        public SysYEqExp(SysYExpression leftExp) {
            this.leftExp = leftExp;
        }

        public SysYEqExp(Token token, SysYExpression leftExp, SysYExpression rightExp) {
            this.token = token;
            this.leftExp = leftExp;
            this.rightExp = rightExp;
        }
    }

    public static class SysYLAndExp extends SysYBinaryExp {

        public SysYLAndExp(SysYExpression leftExp) {
            this.leftExp = leftExp;
        }

        public SysYLAndExp(SysYExpression leftExp, SysYExpression rightExp) {
            this.token = new Token(TokenKind.AND);
            this.leftExp = leftExp;
            this.rightExp = rightExp;
        }
    }

    public static class SysYLOrExp extends SysYBinaryExp {

        public SysYLOrExp(SysYExpression leftExp) {
            this.leftExp = leftExp;
        }

        public SysYLOrExp(SysYExpression leftExp, SysYExpression rightExp) {
            this.token = new Token(TokenKind.OR);
            this.leftExp = leftExp;
            this.rightExp = rightExp;
        }
    }

    public static class SysYCond extends SysYExpression {
        public SysYExpression cond;

        public SysYCond(SysYExpression cond) { this.cond = cond; }

        @Override
        public ReturnKind getReturnKind(SymbolTable table) throws SysYException {
            if (cond.getReturnKind(table) == ReturnKind.INT) return ReturnKind.INT;
            else throw new SysYException(EKind.e);
        }
    }
}
